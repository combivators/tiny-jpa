package net.tiny.dao;

import net.tiny.service.ServiceContext;

public abstract class BaseService<T> extends AbstractService <T, Long>{

    public BaseService(ServiceContext c, Class<T> type) {
        super(c, Long.class, type);
    }

    public BaseService(BaseService<?> base, Class<T> type) {
        super(base, Long.class, type);
    }
}
