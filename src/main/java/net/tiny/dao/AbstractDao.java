package net.tiny.dao;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

//import javax.inject.Inject;
import javax.persistence.EmbeddedId;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Id;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.PersistenceException;
/**
 * Dao - 基类
 *
 */
public  abstract class AbstractDao<T, ID extends Serializable> implements IDao<T, ID> {
    /** 实体类类型 */
    protected transient Class<T> entityClass;
    /** 键值类类型 */
    private transient Class<ID> keyClass;

    @PersistenceContext(unitName = DEFAULT_UNIT, type = PersistenceContextType.EXTENDED)
    //@Inject
    protected EntityManager entityManager;

    private int batchSize = DEFAULT_BATCH_SIZE;

    protected AbstractDao(final Class<ID> keyClass, final Class<T> entityClass) {
        this.keyClass    = keyClass;
        this.entityClass = entityClass;
    }

    @SuppressWarnings("unchecked")
    protected AbstractDao() {
        Class<?> ownerClass = getClass();
        Type classType = getClass().getGenericSuperclass();
        ParameterizedType genericSuperclass = null;
        if(classType instanceof ParameterizedType) {
            genericSuperclass = (ParameterizedType)classType;
        } else if(classType instanceof Class) {
            //maybe is proxy by CDI
            ownerClass = (Class<?>)classType;
            genericSuperclass = (ParameterizedType) ownerClass.getGenericSuperclass();
        }

        Type[] types = genericSuperclass.getActualTypeArguments();
        if(types[0] instanceof Class) {
            this.entityClass = (Class<T>) genericSuperclass.getActualTypeArguments()[0];
            this.keyClass = (Class<ID>) genericSuperclass.getActualTypeArguments()[1];
        } else {
            throw new ClassCastException("Unknow entity class type");
        }
    }

    @Override
    public Class<T> getEntityType() {
        return entityClass;
    }

    @Override
    public Class<ID> getKeyType() {
        return keyClass;
    }

    @Override
    public EntityManager getEntityManager() {
        return this.entityManager;
        //return getSession().getEntityManager();
    }

    @Override
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void commitAndContinue() {
        EntityManager em = getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.commit();
        transaction.begin();
    }

    @Override
    public void rollbackAndContinue() {
        EntityManager em = getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        transaction.rollback();
        transaction.begin();
    }

    @Override
    public Pageable getPageable(int pageNumber) {
        return new Pageable(pageNumber, PAGE_SIZE);
    }

    public Query createQuery(String sql) {
        return getEntityManager().createQuery(sql);
    }

    public TypedQuery<T> createTypedQuery(String sql) {
        return getEntityManager().createQuery(sql, entityClass);
    }

    public Query createNamedQuery(String name) {
        return getEntityManager().createNamedQuery(name);
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return getConnection().getMetaData();
    }

    @Override
    public Connection getJdbcConnection() {
        return getConnection();
    }

    protected Connection getConnection() {
        return getEntityManager().unwrap(Connection.class);
    }

    public boolean executeNativeSQL(String sql) {
        Connection conn = getConnection();
        Statement stmt = null;
        boolean ret = false;
        try {
            stmt = conn.createStatement();
            ret = stmt.execute(sql);
        } catch (SQLException e) {
            throw new PersistenceException(String.format("Error(%s) occured while executing a native SQL : %s",
                    e.getMessage(), sql), e);
        } finally {
            if (null != stmt) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    throw new PersistenceException("Error occured while closing Statement. Cause: " + e.getMessage(), e);
                }
            }
        }
        return ret;
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }

    @Override
    public void setBatchSize(int size) {
        this.batchSize = size;
    }

    @Override
    public void refresh(T entity) {
        if (entity != null) {
            getEntityManager().refresh(entity);
        }
    }

    @Override
    public void refresh(T entity, LockModeType lockModeType) {
        if (entity != null) {
            if (lockModeType != null) {
                getEntityManager().refresh(entity, lockModeType);
            } else {
                getEntityManager().refresh(entity);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public ID getIdentifier(T entity) {
        return (ID) getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
    }

    @Override
    public boolean isManaged(T entity) {
        return getEntityManager().contains(entity);
    }

    @Override
    public void detach(T entity) {
        getEntityManager().detach(entity);
    }

    @Override
    public void lock(T entity, LockModeType lockModeType) {
        if (entity != null && lockModeType != null) {
            getEntityManager().lock(entity, lockModeType);
        }
    }

    @Override
    public void clear() {
        getEntityManager().clear();
    }

    @Override
    public void flush() {
        getEntityManager().flush();
    }

    @Override
    public void insert(final T entity) {
        persist(entity);
    }

    protected void persist(final T entity) {
        getEntityManager().persist(entity);
    }

    @Override
    public  T update(final T e) {
        return merge(e);
    }

    protected T merge(final T e) {
        return getEntityManager().merge(e);
    }

    @Override
    public void remove(T entity) {
        if (entity != null) {
            getEntityManager().remove(entity);
        }
    }

    @Override
    public int removeAll() {
        final StringBuffer queryString =
                new StringBuffer("DELETE  FROM ");
        queryString.append(entityClass.getSimpleName());
        queryString.append(" e");
        TypedQuery<T> query = getEntityManager().createQuery(queryString.toString(), entityClass);
        return query.executeUpdate();
    }

    @Override
    public boolean deleteById(final ID id) {
        final EntityManager em = getEntityManager();
        T e = em.getReference(entityClass, id);
        if(null != e) {
            em.remove(e);
            return true;
        }
        return false;
    }

    @Override
    public void delete(final T e) {
        getEntityManager().remove(e);
    }

    @Override
    public Optional<T> find(ID id) {
        return find(id, null);
    }

    @Override
    public Optional<T> find(ID id, LockModeType lockModeType) {
        T t = null;
        if (id != null) {
            if (lockModeType != null) {
                t = getEntityManager().find(entityClass, id, lockModeType);
            } else {
                t = getEntityManager().find(entityClass, id);
            }
        }
        if (t != null) {
            return Optional.of(t);
        }
        return Optional.empty();
    }

    protected <E> TypedQuery<E> getNamedQuery(String name, Class<E> classType) {
        return getEntityManager().createNamedQuery(name, classType);
    }

   protected boolean isNullPrimaryKey(final T entity) {
        return (getIdentifier(entity) != null);
    }

    protected String getIDName() {
        Field field = findIDField();
        if(null != field) {
            return field.getName();
        }
        return null;
    }

    private Field findIDField() {
        for(Field field : entityClass.getDeclaredFields()){
              if(field.isAnnotationPresent(Id.class)
                      || field.isAnnotationPresent(EmbeddedId.class)) {
                  // Is a primary key field
                  return field;
              }
        }
        return null;
    }
}
