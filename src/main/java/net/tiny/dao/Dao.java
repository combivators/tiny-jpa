package net.tiny.dao;

import java.io.Serializable;

/**
 * Dao - 凡用业务层实装类
 *
 */
public class Dao<T extends Serializable, ID extends Serializable> extends BaseDao<T, ID>{

    public Dao(final Class<ID> keyClass, final Class<T> entityClass) {
        super(keyClass, entityClass);
    }

    //For online
    public static <T extends Serializable, ID extends Serializable> IDao<T, ID> getDao(EntityManagerProducer producer, Class<ID> keyClass, Class<T> entityClass) {
        Dao<T, ID> dao = new Dao<T, ID>(keyClass, entityClass);
        dao.setEntityManager(producer.getScopedEntityManager(true));
        return dao;
    }

    //For batch
    public static <T extends Serializable, ID extends Serializable> IDao<T, ID> getDao(Class<ID> keyClass, Class<T> entityClass) {
        return getDao(new EntityManagerProducer(), keyClass, entityClass);
    }
}
