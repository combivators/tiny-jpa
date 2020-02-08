package net.tiny.dao;

import java.io.Serializable;

import javax.persistence.EntityManager;

/**
 * Dao - 凡用业务层实装类
 *
 */
public class Dao<T, ID extends Serializable> extends BaseDao<T, ID>{

    public Dao(final Class<ID> keyClass, final Class<T> entityClass) {
        super(keyClass, entityClass);
    }

    //For online
    public static <T, ID extends Serializable> IDao<T, ID> getDao(EntityManagerProducer producer, Class<ID> keyType, Class<T> entityType) {
        return getDao(producer.getScoped(false), keyType, entityType);
    }

    public static <T, ID extends Serializable> IDao<T, ID> getDao(EntityManager em, Class<ID> keyType, Class<T> entityType) {
        Dao<T, ID> dao = new Dao<T, ID>(keyType, entityType);
        dao.setEntityManager(em);
        return dao;
    }

    public static <T> IDao<T, Long> getDao(EntityManagerProducer producer, Class<T> type) {
        return getDao(producer.getScoped(false), type);
    }

    public static <T> IDao<T, Long> getDao(EntityManager em, Class<T> type) {
        Dao<T, Long> dao = new Dao<T, Long>(Long.class, type);
        dao.setEntityManager(em);
        return dao;
    }

    //For batch
    public static <T, ID extends Serializable> IDao<T, ID> getDao(Class<ID> keyType, Class<T> entityType) {
        return getDao(new EntityManagerProducer(), keyType, entityType);
    }
}
