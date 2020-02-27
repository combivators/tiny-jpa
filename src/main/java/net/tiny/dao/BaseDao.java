package net.tiny.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.Table;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Metamodel;

import net.tiny.dao.entity.OrderEntity;


/**
 * Dao - 数据层实装继承基类
 *
 */
public abstract class BaseDao<T, ID extends Serializable> extends AbstractDao<T, ID> {

    /** 别名数 */
    private static final long MAX_ALIAS_COUNT = 1000L;

    private static volatile long aliasCount = 0L;

    public BaseDao() {
        super();
    }

    public BaseDao(final Class<ID> keyClass, final Class<T> entityClass) {
        super(keyClass,   entityClass);
    }

    @Override
    public int save(Iterator<T> entities) {
        int count = 0;
        EntityManager em = getEntityManager();
        while(entities.hasNext()) {
            T e = entities.next();
            if (isNullPrimaryKey(e)) {
                insert(e);
            } else {
                e = update(e);
            }
            count++;
            if (count % getBatchSize() == 0) {
                // Flush a batch of inserts and release memory.
                em.flush();
                em.clear();
                commitAndContinue();
            }
        }
        return count;
    }

    @Override
    public Collection<T> save(Collection<T> entities) {
        final List<T> savedEntities = new ArrayList<T>(entities.size());
        int count = 0;
        EntityManager em = getEntityManager();
        for (T e : entities) {
            if (isNullPrimaryKey(e)) {
                insert(e);
            } else {
                e = update(e);
            }
            savedEntities.add(e);
            count++;
            if (count % getBatchSize() == 0) {
                // Flush a batch of inserts and release memory.
                em.flush();
                em.clear();
            }
        }
        return savedEntities;
    }

    @Override
    public int delete(String name, final Map<String, Object> where) {
        TypedQuery<T> query = getNamedQuery(name, where);
        return query.setFlushMode(FlushModeType.COMMIT).executeUpdate();
    }

    @Override
    public int delete(String name, final Object[] args) {
        TypedQuery<T> query = getNamedQuery(name, args);
        return query.setFlushMode(FlushModeType.COMMIT).executeUpdate();
    }

    @Override
    public int deleteAll(Iterator<T> entities) {
        int count = 0;
        EntityManager em = getEntityManager();
        while(entities.hasNext()) {
            T e = entities.next();
            em.remove(e);
            count++;
            if (count % getBatchSize() == 0) {
                em.flush();
                em.clear();
            }
        }
        return count;
    }

    @Override
    public int remove(List<T> entities) {
        int count = 0;
        EntityManager em = getEntityManager();
        for(T e : entities) {
            em.remove(e);
            count++;
        }
        return count;
    }

    @Override
    public Optional<T> find(String name, final Map<String, Object> where) {
        TypedQuery<T> query = getNamedQuery(name, where);
        return Optional.of(query.getSingleResult());
    }

    @Override
    public Optional<T> find(String name, final Object[] args) {
        TypedQuery<T> query = getNamedQuery(name, args);
        return Optional.of(query.getSingleResult());
    }

    @Override
    public List<T> findList(Integer first, Integer count, List<Filter> filters,	List<Order> orders) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(entityClass);
        criteriaQuery.select(criteriaQuery.from(entityClass));
        return findList(criteriaQuery, first, count, filters, orders);
    }

    public List<T> findList(Collection<ID> ids, List<Filter> filters, List<Order> orders) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(entityClass);
        Root<T> root = criteriaQuery.from(entityClass);
        criteriaQuery.select(root);
        Expression<ID> exp = root.get("id");
        Predicate predicate = exp.in(ids);
        criteriaQuery.where(predicate);
        criteriaQuery.orderBy(criteriaBuilder.asc(exp));
        return findList(criteriaQuery, 0, ids.size(), filters, orders);
    }

    @Override
    public Page<T> findPage(Pageable pageable) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(entityClass);
        criteriaQuery.select(criteriaQuery.from(entityClass));
        return findPage(criteriaQuery, pageable);
    }

    @Override
    public Iterator<T> findAll() {
        return selectAll().iterator();
    }

    @Override
    public Iterator<T> find(final Map<String, Object> whereParams, final Map<String, Object> orderParams) {
        return select(whereParams, orderParams).iterator();
    }

    @Override
    public Iterator<T> find(final Map<String, Object> whereParams, final Map<String, Object> orderParams, int offset, int max) {
        return select(whereParams, orderParams, offset, max).iterator();
    }

    @Override
    public Stream<T> finds() {
        final EntityManager em = getEntityManager();
        final CriteriaQuery<T> query = em.getCriteriaBuilder().createQuery(entityClass);
        query.from(entityClass);
        return em.createQuery(query).getResultStream();
    }

    @Override
    public List<T> selectAll() {
        final EntityManager em = getEntityManager();
        final CriteriaQuery<T> query = em.getCriteriaBuilder().createQuery(entityClass);
        query.from(entityClass);
        return em.createQuery(query).getResultList();
    }

    @Override
     public List<T> select(final Map<String, Object> whereParams, final Map<String, Object> orderParams) {
        return select(whereParams, orderParams, -1, -1);
    }
    @Override
    public List<T> select(final Map<String, Object> whereParams, final Map<String, Object> orderParams, int offset, int max) {
        final StringBuffer queryString =
                new StringBuffer("SELECT e FROM ");
        queryString.append(entityClass.getSimpleName());
        queryString.append(" e");
        if(null != whereParams || null != orderParams) {
            queryString.append(" ");
            queryString.append(getQueryClauses(whereParams, orderParams));
        }
        TypedQuery<T> query = getEntityManager().createQuery(queryString.toString(), entityClass);
        if(offset > 0) {
            query.setFirstResult(offset);
        }
        if(max > 0) {
            query.setMaxResults(max);
        }
        //query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        query.setFlushMode(FlushModeType.COMMIT);
        return query.getResultList();
    }

    @Override
    public boolean exists(final Map<String, Object> whereParams) {
        return (count(whereParams) > 0);
    }

    @Override
    public boolean exists(String name, final Map<String, Object> whereParams) {
        return (count(name, whereParams) > 0);
    }
    @Override
    public boolean exists(final Filter... filters) {
        return (count(filters) > 0);
    }

    @Override
    public long count(Filter... filters) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(entityClass);
        criteriaQuery.select(criteriaQuery.from(entityClass));
        return count(criteriaQuery, filters != null ? Arrays.asList(filters) : null);
    }

    @Override
    public long count() {
        return count((Map<String, Object>)null);
    }

    @Override
    public long count(final Map<String, Object> whereParams) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        criteriaQuery.select(criteriaBuilder.count(criteriaQuery.from(entityClass)));
        // criteriaQuery.where(/*your stuff*/);
        return getEntityManager().createQuery(criteriaQuery).getSingleResult();
    }

    @Override
    public long count(String name, final Map<String, Object> where) {
        TypedQuery<T> query = getNamedQuery(name, where);
        return (Long) query.getSingleResult();
    }

    @Override
    public long count(String name, final Object[] args) {
        TypedQuery<T> query = getNamedQuery(name, args);
        return (Long) query.getSingleResult();
    }

    private String getQueryClauses(final Map<String, Object> whereParams, final Map<String, Object> orderParams) {
        final StringBuffer queryString = new StringBuffer();
        if (whereParams != null && !whereParams.isEmpty()) {
            queryString.append(" where ");
            final Iterator<Map.Entry<String, Object>> it = whereParams.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry<String, Object> entry = it.next();
                if (entry.getValue() instanceof Boolean) {
                    queryString.append(entry.getKey());
                    queryString.append(" is ");
                    queryString.append(entry.getValue());
                    queryString.append(" ");
                } else {
                    if (entry.getValue() instanceof Number) {
                        queryString.append(entry.getKey());
                        queryString.append(" = ");
                        queryString.append(entry.getValue());
                    } else {
                        // string equality
                        queryString.append(entry.getKey());
                        queryString.append(" = '");
                        queryString.append(entry.getValue());
                        queryString.append("'");
                    }
                }
                if (it.hasNext()) {
                    queryString.append(" and ");
                }
            }
        }
        if ((orderParams != null) && !orderParams.isEmpty()) {
            queryString.append(" order by ");
            final Iterator<Map.Entry<String, Object>> it = orderParams.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry<String, Object> entry = it.next();
                queryString.append(entry.getKey());
                queryString.append(" ");
                if (entry.getValue() != null) {
                    queryString.append(entry.getValue());
                }
                if (it.hasNext()) {
                    queryString.append(", ");
                }
            }
        }
        return queryString.toString();
    }

    // Other common operations
    public TypedQuery<T> getNamedQuery(String name) {
        return getNamedQuery(name, entityClass);
    }

    @Override
    public TypedQuery<T> getNamedQuery(String name, Map<String, Object> args) {
        TypedQuery<T> query = getNamedQuery(name, entityClass);
        if(args != null && !args.isEmpty()) {
            for(String key : args.keySet()) {
                query.setParameter(key, args.get(key));
            }
        }
        return query;
    }

    @Override
    public TypedQuery<T> getNamedQuery(String name, final Object[] args) {
        TypedQuery<T> query = getNamedQuery(name, entityClass);
        if(args != null && args.length>0) {
            for(int i=0; i<args.length; i++) {
                query.setParameter(i, args[i]);
            }
        }
        return query;
    }

    @Override
    public List<T> queryNamed(String name) {
        TypedQuery<T> query = getNamedQuery(name, new Object[0]);
        return query.getResultList();
    }

    @Override
    public List<T> queryNamed(String name, final Map<String, Object> where) {
        return queryNamed(name, where, 0, 0);
    }

    @Override
    public List<T> queryNamed(String name, final Map<String, Object> where, int offset, int max) {
        TypedQuery<T> query = getNamedQuery(name, where);
        if(offset > 0) {
            query.setFirstResult(offset);
        }
        if(max > 0) {
            query.setMaxResults(max);
        }
        return query.getResultList();
    }

    @Override
    public Iterator<T> findNamed(String name, final Map<String, Object> where) {
        return queryNamed(name, where, -1, -1).iterator();
    }

    @Override
    public Iterator<T> findNamed(String name, final Map<String, Object> where, int offset, int max) {
        return queryNamed(name, where, offset, max).iterator();
    }

    @Override
    public int updateNamed(String name, final Map<String, Object> args) {
        TypedQuery<T> query = getNamedQuery(name, args);
        return query.executeUpdate();
    }

    @Override
    public Optional<T> updateNamed(String name, ID id) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put(getIDName(),  id);
        if(1 == updateNamed(name, args)) {
            return find(id);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String getTableName() {
        final Metamodel meta = getEntityManager().getMetamodel();
        final EntityType<T> entityType = meta.entity(entityClass);
        //Check whether @Table annotation is present on the class.
        final Table t = entityClass.getAnnotation(Table.class);
        return (t == null) ? entityType.getName().toUpperCase() : t.name();
    }


    protected List<T> findList(CriteriaQuery<T> criteriaQuery, Integer first, Integer count, List<Filter> filters, List<Order> orders) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        Root<T> root = getRoot(criteriaQuery);
        addRestrictions(criteriaQuery, filters);
        addOrders(criteriaQuery, orders);
        if (criteriaQuery.getOrderList() == null || criteriaQuery.getOrderList().isEmpty()) {
            if (OrderEntity.class.isAssignableFrom(entityClass)) {
                criteriaQuery.orderBy(criteriaBuilder.asc(root.get(OrderEntity.ORDER_PROPERTY_NAME)));
            } else {
                criteriaQuery.orderBy(criteriaBuilder.desc(root.get(OrderEntity.CREATE_DATE_PROPERTY_NAME)));
            }
        }
        TypedQuery<T> query = getEntityManager().createQuery(criteriaQuery).setFlushMode(FlushModeType.COMMIT);
        if (first != null) {
            query.setFirstResult(first);
        }
        if (count != null) {
            query.setMaxResults(count);
        }
        return query.getResultList();
    }

    protected Page<T> findPage(CriteriaQuery<T> criteriaQuery, Pageable pageable) {
        if (pageable == null) {
            pageable = new Pageable();
        }
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        Root<T> root = getRoot(criteriaQuery);
        addRestrictions(criteriaQuery, pageable);
        addOrders(criteriaQuery, pageable);
        if (criteriaQuery.getOrderList() == null || criteriaQuery.getOrderList().isEmpty()) {
            if (OrderEntity.class.isAssignableFrom(entityClass)) {
                criteriaQuery.orderBy(criteriaBuilder.asc(root.get(OrderEntity.ORDER_PROPERTY_NAME)));
            } else {
                criteriaQuery.orderBy(criteriaBuilder.desc(root.get(OrderEntity.CREATE_DATE_PROPERTY_NAME)));
            }
        }
        long total = count(criteriaQuery, null);
        int totalPages = (int) Math.ceil((double) total / (double) pageable.getPageSize());
        if (totalPages < pageable.getPageNumber()) {
            pageable.setPageNumber(totalPages);
        }
        TypedQuery<T> query = getEntityManager().createQuery(criteriaQuery).setFlushMode(FlushModeType.COMMIT);
        query.setFirstResult((pageable.getPageNumber() - 1) * pageable.getPageSize());
        query.setMaxResults(pageable.getPageSize());
        return new Page<T>(query.getResultList(), total, pageable);
    }

    protected Long count(CriteriaQuery<T> criteriaQuery, List<Filter> filters) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        addRestrictions(criteriaQuery, filters);

        CriteriaQuery<Long> countCriteriaQuery = criteriaBuilder.createQuery(Long.class);
        for (Root<?> root : criteriaQuery.getRoots()) {
            Root<?> dest = countCriteriaQuery.from(root.getJavaType());
            dest.alias(getAlias(root));
            copyJoins(root, dest);
        }

        Root<?> countRoot = getRoot(countCriteriaQuery, criteriaQuery.getResultType());
        countCriteriaQuery.select(criteriaBuilder.count(countRoot));

        if (criteriaQuery.getGroupList() != null) {
            countCriteriaQuery.groupBy(criteriaQuery.getGroupList());
        }
        if (criteriaQuery.getGroupRestriction() != null) {
            countCriteriaQuery.having(criteriaQuery.getGroupRestriction());
        }
        if (criteriaQuery.getRestriction() != null) {
            countCriteriaQuery.where(criteriaQuery.getRestriction());
        }
        return getEntityManager().createQuery(countCriteriaQuery).setFlushMode(FlushModeType.COMMIT).getSingleResult();
    }

    private synchronized String getAlias(Selection<?> selection) {
        if (selection != null) {
            String alias = selection.getAlias();
            if (alias == null) {
                if (aliasCount >= MAX_ALIAS_COUNT) {
                    aliasCount = 0;
                }
                alias = "generatedAlias" + aliasCount++;
                selection.alias(alias);
            }
            return alias;
        }
        return null;
    }

    private Root<T> getRoot(CriteriaQuery<T> criteriaQuery) {
        if (criteriaQuery != null) {
            return getRoot(criteriaQuery, criteriaQuery.getResultType());
        }
        return null;
    }

    private Root<T> getRoot(CriteriaQuery<?> criteriaQuery, Class<T> clazz) {
        if (criteriaQuery != null && criteriaQuery.getRoots() != null && clazz != null) {
            for (Root<?> root : criteriaQuery.getRoots()) {
                if (clazz.equals(root.getJavaType())) {
                    return (Root<T>) root.as(clazz);
                }
            }
        }
        return null;
    }

    private void copyJoins(From<?, ?> from, From<?, ?> to) {
        for (Join<?, ?> join : from.getJoins()) {
            Join<?, ?> toJoin = to.join(join.getAttribute().getName(), join.getJoinType());
            toJoin.alias(getAlias(join));
            copyJoins(join, toJoin);
        }
        for (Fetch<?, ?> fetch : from.getFetches()) {
            Fetch<?, ?> toFetch = to.fetch(fetch.getAttribute().getName());
            copyFetches(fetch, toFetch);
        }
    }

    private void copyFetches(Fetch<?, ?> from, Fetch<?, ?> to) {
        for (Fetch<?, ?> fetch : from.getFetches()) {
            Fetch<?, ?> toFetch = to.fetch(fetch.getAttribute().getName());
            copyFetches(fetch, toFetch);
        }
    }

    private void addRestrictions(CriteriaQuery<T> criteriaQuery, List<Filter> filters) {
        if (criteriaQuery == null || filters == null || filters.isEmpty()) {
            return;
        }
        Root<T> root = getRoot(criteriaQuery);
        if (root == null) {
            return;
        }
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        Predicate restrictions = criteriaQuery.getRestriction() != null ? criteriaQuery.getRestriction() : criteriaBuilder.conjunction();
        for (Filter filter : filters) {
            if (filter == null || isEmpty(filter.getProperty())) {
                continue;
            }
            if (filter.getOperator() == Filter.Operator.eq && filter.getValue() != null) {
                if (filter.getIgnoreCase() != null && filter.getIgnoreCase() && filter.getValue() instanceof String) {
                    restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(criteriaBuilder.lower(root.<String> get(filter.getProperty())), ((String) filter.getValue()).toLowerCase()));
                } else {
                    restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(root.get(filter.getProperty()), filter.getValue()));
                }
            } else if (filter.getOperator() == Filter.Operator.ne && filter.getValue() != null) {
                if (filter.getIgnoreCase() != null && filter.getIgnoreCase() && filter.getValue() instanceof String) {
                    restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.notEqual(criteriaBuilder.lower(root.<String> get(filter.getProperty())), ((String) filter.getValue()).toLowerCase()));
                } else {
                    restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.notEqual(root.get(filter.getProperty()), filter.getValue()));
                }
            } else if (filter.getOperator() == Filter.Operator.gt && filter.getValue() != null) {
                restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.gt(root.<Number> get(filter.getProperty()), (Number) filter.getValue()));
            } else if (filter.getOperator() == Filter.Operator.lt && filter.getValue() != null) {
                restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.lt(root.<Number> get(filter.getProperty()), (Number) filter.getValue()));
            } else if (filter.getOperator() == Filter.Operator.ge && filter.getValue() != null) {
                restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.ge(root.<Number> get(filter.getProperty()), (Number) filter.getValue()));
            } else if (filter.getOperator() == Filter.Operator.le && filter.getValue() != null) {
                restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.le(root.<Number> get(filter.getProperty()), (Number) filter.getValue()));
            } else if (filter.getOperator() == Filter.Operator.like && filter.getValue() != null && filter.getValue() instanceof String) {
                restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.like(root.<String> get(filter.getProperty()), (String) filter.getValue()));
            } else if (filter.getOperator() == Filter.Operator.in && filter.getValue() != null) {
                restrictions = criteriaBuilder.and(restrictions, root.get(filter.getProperty()).in(filter.getValue()));
            } else if (filter.getOperator() == Filter.Operator.isNull) {
                restrictions = criteriaBuilder.and(restrictions, root.get(filter.getProperty()).isNull());
            } else if (filter.getOperator() == Filter.Operator.isNotNull) {
                restrictions = criteriaBuilder.and(restrictions, root.get(filter.getProperty()).isNotNull());
            }
        }
        criteriaQuery.where(restrictions);
    }

    private void addRestrictions(CriteriaQuery<T> criteriaQuery, Pageable pageable) {
        if (criteriaQuery == null || pageable == null) {
            return;
        }
        Root<T> root = getRoot(criteriaQuery);
        if (root == null) {
            return;
        }
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        Predicate restrictions = criteriaQuery.getRestriction() != null ? criteriaQuery.getRestriction() : criteriaBuilder.conjunction();
        if (isNotEmpty(pageable.getSearchProperty()) && isNotEmpty(pageable.getSearchValue())) {
            restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.like(root.<String> get(pageable.getSearchProperty()), "%" + pageable.getSearchValue() + "%"));
        }
        if (pageable.getFilters() != null) {
            for (Filter filter : pageable.getFilters()) {
                if (filter == null || isEmpty(filter.getProperty())) {
                    continue;
                }
                if (filter.getOperator() == Filter.Operator.eq && filter.getValue() != null) {
                    if (filter.getIgnoreCase() != null && filter.getIgnoreCase() && filter.getValue() instanceof String) {
                        restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(criteriaBuilder.lower(root.<String> get(filter.getProperty())), ((String) filter.getValue()).toLowerCase()));
                    } else {
                        restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.equal(root.get(filter.getProperty()), filter.getValue()));
                    }
                } else if (filter.getOperator() == Filter.Operator.ne && filter.getValue() != null) {
                    if (filter.getIgnoreCase() != null && filter.getIgnoreCase() && filter.getValue() instanceof String) {
                        restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.notEqual(criteriaBuilder.lower(root.<String> get(filter.getProperty())), ((String) filter.getValue()).toLowerCase()));
                    } else {
                        restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.notEqual(root.get(filter.getProperty()), filter.getValue()));
                    }
                } else if (filter.getOperator() == Filter.Operator.gt && filter.getValue() != null) {
                    restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.gt(root.<Number> get(filter.getProperty()), (Number) filter.getValue()));
                } else if (filter.getOperator() == Filter.Operator.lt && filter.getValue() != null) {
                    restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.lt(root.<Number> get(filter.getProperty()), (Number) filter.getValue()));
                } else if (filter.getOperator() == Filter.Operator.ge && filter.getValue() != null) {
                    restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.ge(root.<Number> get(filter.getProperty()), (Number) filter.getValue()));
                } else if (filter.getOperator() == Filter.Operator.le && filter.getValue() != null) {
                    restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.le(root.<Number> get(filter.getProperty()), (Number) filter.getValue()));
                } else if (filter.getOperator() == Filter.Operator.like && filter.getValue() != null && filter.getValue() instanceof String) {
                    restrictions = criteriaBuilder.and(restrictions, criteriaBuilder.like(root.<String> get(filter.getProperty()), (String) filter.getValue()));
                } else if (filter.getOperator() == Filter.Operator.in && filter.getValue() != null) {
                    restrictions = criteriaBuilder.and(restrictions, root.get(filter.getProperty()).in(filter.getValue()));
                } else if (filter.getOperator() == Filter.Operator.isNull) {
                    restrictions = criteriaBuilder.and(restrictions, root.get(filter.getProperty()).isNull());
                } else if (filter.getOperator() == Filter.Operator.isNotNull) {
                    restrictions = criteriaBuilder.and(restrictions, root.get(filter.getProperty()).isNotNull());
                }
            }
        }
        criteriaQuery.where(restrictions);
    }

    private void addOrders(CriteriaQuery<T> criteriaQuery, List<Order> orders) {
        if (criteriaQuery == null || orders == null || orders.isEmpty()) {
            return;
        }
        Root<T> root = getRoot(criteriaQuery);
        if (root == null) {
            return;
        }
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        List<javax.persistence.criteria.Order> orderList = new ArrayList<javax.persistence.criteria.Order>();
        if (criteriaQuery.getOrderList() != null && !criteriaQuery.getOrderList().isEmpty()) {
            orderList.addAll(criteriaQuery.getOrderList());
        }
        for (Order order : orders) {
            if (order.getDirection() == Order.Direction.asc) {
                orderList.add(criteriaBuilder.asc(root.get(order.getProperty())));
            } else if (order.getDirection() == Order.Direction.desc) {
                orderList.add(criteriaBuilder.desc(root.get(order.getProperty())));
            }
        }
        criteriaQuery.orderBy(orderList);
    }

    private void addOrders(CriteriaQuery<T> criteriaQuery, Pageable pageable) {
        if (criteriaQuery == null || pageable == null) {
            return;
        }
        Root<T> root = getRoot(criteriaQuery);
        if (root == null) {
            return;
        }
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        List<javax.persistence.criteria.Order> orderList = new ArrayList<javax.persistence.criteria.Order>();
        if (criteriaQuery.getOrderList() != null && !criteriaQuery.getOrderList().isEmpty()) {
            orderList.addAll(criteriaQuery.getOrderList());
        }
        if (isNotEmpty(pageable.getOrderProperty()) && pageable.getOrderDirection() != null) {
            if (pageable.getOrderDirection() == Order.Direction.asc) {
                orderList.add(criteriaBuilder.asc(root.get(pageable.getOrderProperty())));
            } else if (pageable.getOrderDirection() == Order.Direction.desc) {
                orderList.add(criteriaBuilder.desc(root.get(pageable.getOrderProperty())));
            }
        }
        if (pageable.getOrders() != null) {
            for (Order order : pageable.getOrders()) {
                if (order.getDirection() == Order.Direction.asc) {
                    orderList.add(criteriaBuilder.asc(root.get(order.getProperty())));
                } else if (order.getDirection() == Order.Direction.desc) {
                    orderList.add(criteriaBuilder.desc(root.get(order.getProperty())));
                }
            }
        }
        criteriaQuery.orderBy(orderList);
    }

    boolean isEmpty(String str) {
        return (str == null || str.trim().isEmpty());
    }

    boolean isNotEmpty(String str) {
        return (str != null && !str.trim().isEmpty());
    }



}