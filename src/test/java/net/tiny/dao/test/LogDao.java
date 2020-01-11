package net.tiny.dao.test;

import java.util.List;

import javax.persistence.NoResultException;

import net.tiny.dao.BaseDao;
import net.tiny.dao.test.entity.Log;

/**
 * Dao - 日志
 *
 */
public class LogDao extends BaseDao<Log, Long> {

    /**
     * 删除所有日志
     */
    public void clearAll() {
        //String jpql = "delete from Log log";
        //entityManager.createQuery(jpql).setFlushMode(FlushModeType.COMMIT).executeUpdate();
        super.removeAll();
    }

    public List<Log> findByOperator(String operator) {
        if (operator == null) {
            return null;
        }
        try {
             return getNamedQuery("Log.findByOperator").setParameter("operator", operator).getResultList();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<Log> findByIp(String ip) {
        if (ip == null) {
            return null;
        }
        try {
             return getNamedQuery("Log.findByIp").setParameter("ip", ip).getResultList();
        } catch (NoResultException e) {
            return null;
        }
    }
}
