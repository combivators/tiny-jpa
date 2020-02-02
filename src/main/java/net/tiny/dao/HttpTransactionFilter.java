package net.tiny.dao;

import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

public class HttpTransactionFilter extends Filter {

    private EntityManagerProducer producer;
    private EntityManagerFactory factory;

    public HttpTransactionFilter factory(EntityManagerFactory factory) {
        this.factory = factory;
        if (null == producer) {
        	producer = new EntityManagerProducer();
        	producer.setEntityManagerFactory(this.factory);
        }
        return this;
    }

    public HttpTransactionFilter producer(EntityManagerProducer producer) {
        this.producer = producer;
        return this;
    }

    @Override
    public String description() {
        return "HTTP Web service transaction filter";
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        EntityManager entityManager = null;
        try {
            entityManager = producer.create();
            exchange.setAttribute(EntityManagerProducer.class.getName(), producer);
            exchange.setAttribute(EntityManager.class.getName(), entityManager);
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
}
