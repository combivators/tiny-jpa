package net.tiny.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;

/**
 * Dao - 基类接口
 *
 */
public interface IDao<T, ID extends Serializable> extends Constants {

    /** 每页记录数 */
    int PAGE_SIZE = 10;

    /**
     * 获取实体对象键的Java类
     *
     * @return 键的Java类
     */
    public Class<ID> getKeyType();
    /**
     * 获取实体对象的Java类
     *
     * @return 对象Java类
     */
    public Class<T> getEntityType();

    /**
     * 获取持久化容器管理
     *
     * @return EntityManager
     */
    EntityManager getEntityManager();

    /**
     * 设置批处理件数持久化容器管理
     *
     * @param entityManager
     *            持久化容器管理
     *
     */
    void setEntityManager(EntityManager entityManager);

    /**
     * 数据库交互对话结束，并开始新的对话
     *
     */
    void commitAndContinue();

    /**
     * 数据库交互对话卷回，并开始新的对话
     *
     */
    void rollbackAndContinue();

    /**
     * 获取分页信息
     *
     * @param pageNumber
     *            分页号
     *
     * @return 分页信息
     */
    Pageable getPageable(int pageNumber);

    /**
     * 批处理件数
     *
     * @return 一次批处理件数
     */
    int getBatchSize();

    /**
     * 设置批处理件数
     *
     * @param size
     *            批处理件数
     */
    void setBatchSize(int size);

    /**
     * 刷新实体对象
     *
     * @param entity
     *            实体对象
     */
    void refresh(T entity);

    /**
     * 刷新实体对象
     *
     * @param entity
     *            实体对象
     * @param lockModeType
     *            锁定方式
     */
    void refresh(T entity, LockModeType lockModeType);

    /**
     * 获取实体对象ID
     *
     * @param entity
     *            实体对象
     * @return 实体对象ID
     */
    ID getIdentifier(T entity);

    /**
     * 判断是否为托管状态
     *
     * @param entity
     *            实体对象
     * @return 是否为托管状态
     */
    boolean isManaged(T entity);

    /**
     * 设置为游离状态
     *
     * @param entity
     *            实体对象
     */
    void detach(T entity);

    /**
     * 锁定实体对象
     *
     * @param entity
     *            实体对象
     * @param lockModeType
     *            锁定方式
     */
    void lock(T entity, LockModeType lockModeType);

    /**
     * 清除缓存
     */
    void clear();

    /**
     * 同步数据
     */
    void flush();

    /**
     * 持久化实体对象
     *
     * @param entity
     *            实体对象
     */
    void insert(T entity);
    //void persist(T entity);



    /**
     * 合并实体对象
     *
     * @param entity
     *            实体对象
     * @return 实体对象
     */
    T update(T entity);
    //T merge(T entity);

    /**
     * 移除实体对象
     *
     * @param entity
     *            实体对象
     */
    void remove(T entity);

    //bulk Save
    Collection<T> save(Collection<T> entities);
    int save(Iterator<T> entities);

    /**
     * 查找实体对象
     *
     * @param id
     *            ID
     * @param lockModeType
     *            锁定方式
     * @return 实体对象，若不存在则返回null
     */
    T find(ID id, LockModeType lockModeType);

    /**
     * 查找实体对象
     *
     * @param id
     *            ID
     * @return 实体对象，若不存在则返回null
     */
    T find(ID id);

    /**
     * 查找实体对象
     *
     *	@param name
     *            查询对象关系映射名
     * @param where
     *            查询条件
     * @return 实体对象，若不存在则返回null
     */
    T find(String name, final Map<String, Object> where);

    /**
     * 查找实体对象
     *
     *	@param name
     *            查询对象关系映射名
     * @param args
     *            查询条件
     * @return 实体对象，若不存在则返回null
     */
    T find(String name, final Object[] args);

    /**
     * 查找实体对象集合
     *
     * @param first
     *            起始记录
     * @param count
     *            数量
     * @param filters
     *            筛选
     * @param orders
     *            排序
     * @return 实体对象集合
     */
    List<T> findList(Integer first, Integer count, List<Filter> filters, List<Order> orders);

    /**
     * 查找实体对象分页
     *
     * @param pageable
     *            分页信息
     * @return 实体对象分页
     */
    Page<T> findPage(Pageable pageable);

    /**
     * 查询实体对象存在
     *
     * @param whereParams
     *            查询条件
     * @return 是否对象存在
     */
    boolean exists(final Map<String, Object> whereParams);

    /**
     * 查询实体对象存在
     *
     * @param filters
     *            筛选
     * @return 是否对象存在
     */
    boolean exists(final Filter... filters);

    /**
     * 查询实体对象存在
     *
     *	@param name
     *            查询对象关系映射名
     *
     * @param where
     *            查询条件
     * @return 是否对象存在
     */
    boolean exists(String name, final Map<String, Object> whereParams);

    /**
     * 查询实体对象数量
     *
     * @param filters
     *            筛选
     * @return 实体对象数量
     */
    long count(Filter... filters);

    /**
     * 查询实体对象数量
     *
     * @param where
     *            查询条件
     * @return 实体对象数量
     */
    long count(final Map<String, Object> where);

    /**
     * 查询实体对象数量
     *
     *	@param name
     *            查询对象关系映射名
     *
     * @param where
     *            查询条件
     * @return 实体对象数量
     */
    long count(String name, final Map<String, Object> where);

    /**
     * 查询实体对象数量
     *
     *	@param name
     *            查询对象关系映射名
     *
     * @param args
     *            查询条件参数
     * @return 实体对象数量
     */
    long count(String name, final Object[] args);

    /**
     * 查询实体对象全体数量
     *
     * @return 实体对象数量
     */
    long count();

    /**
     * 删除实体对象
     *
     * @param id
     *            ID
     * @return 存在返回true，若不存在则返回false
     */
    boolean deleteById(ID id);

    /**
     * 删除实体对象
     *
     *	@param name
     *            查询对象关系映射名
     * @param where
     *            查询条件
     * @return 删除件数
     */
    int delete(String name, final Map<String, Object> where);

    /**
     * 删除实体对象
     *
     *	@param name
     *            查询对象关系映射名
     * @param args
     *            查询条件
     * @return 删除件数
     */
    int delete(String name, final Object[] args);

    void delete(T entity);
    int deleteAll(Iterator<T> entities);
    int remove(List<T> entities);
    int removeAll();

    Iterator<T> findAll();
    Iterator<T> find(final Map<String, Object> where, final Map<String, Object> order);
    Iterator<T> find(final Map<String, Object> where, final Map<String, Object> order, int offset, int max);

    List<T> selectAll() ;
    List<T> select(final Map<String, Object> whereParams, final Map<String, Object> orderParams);
    List<T> select(final Map<String, Object> whereParams, final Map<String, Object> orderParams, int offset, int max);

    TypedQuery<T> getNamedQuery(String name, Map<String, Object> args);
    TypedQuery<T> getNamedQuery(String name, final Object[] args);
    List<T> queryNamed(String name);
    List<T> queryNamed(String name, final Map<String, Object> where);
    List<T> queryNamed(String name, final Map<String, Object> where, int offset, int max);
    Iterator<T> findNamed(String name, final Map<String, Object> where);
    Iterator<T> findNamed(String name, final Map<String, Object> where, int offset, int max);

    int updateNamed(String name, final Map<String, Object> args);
    T updateNamed(String name, ID id);

    Connection getJdbcConnection();
    boolean executeNativeSQL(String sql);
}
