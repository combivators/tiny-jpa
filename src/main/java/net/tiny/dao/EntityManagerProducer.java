package net.tiny.dao;

import java.net.URL;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;

import net.tiny.ws.auth.Crypt;

public class EntityManagerProducer implements Constants {
    private static Logger LOGGER = Logger.getLogger(EntityManagerProducer.class.getName());

    private static ConcurrentHashMap<String, EntityManagerFactory> FACTORIES = new ConcurrentHashMap<>();

    private static ThreadLocal<EntityManager> localContext = new ThreadLocal<>();

    public static String getPropertiesFile(final String unitName, final String profile) {
        String propertiesFilename = "persistence";
        if(null != profile && !profile.isEmpty()) {
            propertiesFilename = propertiesFilename.concat("-").concat(profile);
        }
        if (!DEFAULT_UNIT.equals(unitName)) {
            propertiesFilename = propertiesFilename.concat("-").concat(unitName);
        }
        propertiesFilename = propertiesFilename.concat(".properties");
        return propertiesFilename;
    }

    public static EntityManagerFactory createEntityManagerFactory(final String unitName, final String profile) {
        EntityManagerFactory factory = FACTORIES.get(unitName);
        if (null == factory) {
            final Properties properties = new Properties();
            final String propertiesFilename = getPropertiesFile(unitName, profile);
            final URL url = Thread.currentThread().getContextClassLoader().getResource(propertiesFilename);
            try {
                factory = Persistence.createEntityManagerFactory(unitName);
                if (null != url) {
                    // Load JPA Properties
                    properties.load(url.openStream());
                    String  jdbcPassword = properties.getProperty("javax.persistence.jdbc.password");
                    if (null != jdbcPassword && !jdbcPassword.isEmpty()) {
                        jdbcPassword = Crypt.decryptPassword(jdbcPassword);
                    } else {
                        jdbcPassword = "";
                    }
                    properties.setProperty("javax.persistence.jdbc.password", jdbcPassword);
                    factory = Persistence.createEntityManagerFactory(unitName, properties);
                    LOGGER.info(String.format("[JPA] EntityManagerFactory '%s' is created. Load properties from '%s'.", unitName, propertiesFilename));
                } else {
                    // Lookup persistence.xml
                    LOGGER.info(String.format("[JPA] EntityManagerFactory '%s' is created by 'persistence.xml'.", unitName));
                }
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "Failed to setup persistence unit '"+unitName+"'.",  t);
                throw new PersistenceException(t.getMessage(), t);
            }
            FACTORIES.put(unitName, factory);
        }
        return factory;
    }

    public static EntityManager create(final String unitName) {
        return createEntityManagerFactory(unitName, System.getProperty("profile")).createEntityManager();
    }

    public EntityManager create() {
        return create(DEFAULT_UNIT);
    }

    public EntityManager getScopedEntityManager(boolean createable) {
        EntityManager em = localContext.get();
        boolean local = (em != null);
        if(createable) {
            em = create();
            if(!local) {
                localContext.set(em);
            }
        } else if(!local) {
            em = create();
            localContext.set(em);
        }
        return em;
    }


    public void dispose(EntityManager entityManager) {
        EntityManager em = localContext.get();
        if(null != em &&
                null != entityManager &&
                em.hashCode() == entityManager.hashCode()) {
            localContext.remove();
        }
        closeEntityManager(entityManager);
    }

    private void closeEntityManager(EntityManager entityManager) {
        if (entityManager == null) {
            return;
        }
        if (entityManager.isOpen()) {
            try {
                EntityTransaction transaction = entityManager.getTransaction();
                // In case a transaction is still open.
                if (transaction.isActive()) {
                    if(transaction.getRollbackOnly()) {
                        LOGGER.fine(String.format("[JPA] tr-%d rollback", transaction.hashCode()));
                        transaction.rollback();
                    } else {
                        LOGGER.fine(String.format("[JPA] tr-%d commit", transaction.hashCode()));
                        transaction.commit();
                    }
                }
            } finally {
                LOGGER.fine("[JPA] Closing entity manager");
                entityManager.close();
            }

        }
    }

}