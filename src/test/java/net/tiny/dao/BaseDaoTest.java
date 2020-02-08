package net.tiny.dao;

import static org.junit.jupiter.api.Assertions.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;

import net.tiny.config.JsonParser;
import net.tiny.dao.test.LogDao;
import net.tiny.dao.test.entity.Log;
import net.tiny.unit.db.Database;

@Database(persistence="persistence-eclipselink.properties"
  ,trace=true
  ,before= {"create sequence xx_log_sequence increment by 1 start with 1;"
       ,"create sequence id_seq increment by 1 start with 0 NOCYCLE;"
  }
)
public class BaseDaoTest {

    @PersistenceContext(unitName = "persistenceUnit")
    protected EntityManager entityManager;


    @Test
    public void testInsertUpdateDelete() throws Exception {
        assertNotNull(entityManager);
        entityManager.getTransaction().begin();

        LogDao dao = new LogDao();
        dao.setEntityManager(entityManager);

        Log log = new Log();
        log.setContent("content");
        log.setOperation("用户设置");
        log.setOperator("user");
        log.setParameter("phone=88888888");
        log.setIp("192.168.80.180");
        dao.insert(log);
        assertNotNull(log.getId());
        final String response = JsonParser.marshal(log);
        System.out.println(response);
        entityManager.getTransaction().commit();
    }
}
