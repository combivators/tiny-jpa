package net.tiny.dao;

import java.io.IOException;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

public class HttpTransactionFilter extends Filter {

    private static Logger LOGGER = Logger.getLogger(HttpTransactionFilter.class.getName());

    private EntityManagerProducer producer = new EntityManagerProducer();
    private EntityManagerFactory factory = null;
    private String unitName = Constants.DEFAULT_UNIT;
    private String profile = Constants.DEFAULT_PROFILE;

    public HttpTransactionFilter unitName(String unitName) {
        this.unitName = unitName;
        return this;
    }

    public HttpTransactionFilter profile(String profile) {
        this.profile = profile;
        return this;
    }

    public HttpTransactionFilter factory(EntityManagerFactory factory) {
        this.factory = factory;
        return this;
    }

    @Override
    public String description() {
        return "HTTP Web service transaction filter";
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        EntityManager entityManager = null;
        EntityTransaction trx = null;
        try {
            entityManager = createEntityManager();
            exchange.setAttribute(EntityManagerProducer.class.getName(), producer);
            exchange.setAttribute(EntityManager.class.getName(), entityManager);
            trx = entityManager.getTransaction();
            trx.begin();
            LOGGER.fine(String.format("[JPA] tr-%d begin", trx.hashCode()));
            // We don't start the database transaction here, but when first needed
            if (null != chain) {
                chain.doFilter(exchange);
            }
        } catch (Throwable ex) {
            if (ex instanceof IOException) {
                throw (IOException)ex;
            } else if (ex instanceof RuntimeException) {
                throw (RuntimeException)ex;
            } else {
                throw new RuntimeException(ex.getMessage(), ex);
            }
        } finally {
            // No matter what happens, close the EntityManager.
            producer.dispose(entityManager);
        }
    }

    private EntityManager createEntityManager() {
        if (factory == null) {
            factory = EntityManagerProducer.createEntityManagerFactory(unitName, profile);
        }
        return factory.createEntityManager();
    }
}
