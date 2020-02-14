package net.tiny.dao;

import java.io.Serializable;
import java.util.Optional;

import javax.annotation.Resource;

import net.tiny.service.ServiceContext;

public class AbstractService<T, ID extends Serializable> {

    @Resource
    protected final ServiceContext context;
    protected final IDao<T, ID> dao;

    public AbstractService(final ServiceContext c, final Class<ID> keyClass, final Class<T> entityClass) {
        context = c;
        dao = Dao.getDao(service(EntityManagerProducer.class), keyClass, entityClass);
    }

    public AbstractService(final AbstractService<?,?> base, final Class<ID> keyClass, final Class<T> entityClass) {
        context = base.context;
        dao = Dao.getDao(service(EntityManagerProducer.class), keyClass, entityClass);
    }

    public ServiceContext getContext() {
        return context;
    }

    public boolean exists(ID id) {
        return dao.find(id).isPresent();
    }

    public Optional<T> get(ID id) {
        return dao.find(id);
    }

    public void post(T entity) {
        dao.insert(entity);
        dao.flush();
    }

    public void put(T entity) {
        dao.update(entity);
        dao.flush();
    }

    public void delete(T entity) {
        dao.delete(entity);
    }

    public <S> S service(Class<S> service) {
        return context.lookup(service);
    }

    protected BaseDao<T, ID> dao() {
        return (BaseDao<T, ID>)dao;
    }


}
