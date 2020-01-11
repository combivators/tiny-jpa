package net.tiny.dao;

import static org.junit.jupiter.api.Assertions.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;

import net.tiny.dao.test.LogDao;
import net.tiny.unit.ws.Server;

@Server(web=8080,rdb=9001,trace=true
  ,config="src/test/resources/config/test-handlers.yml"
  ,persistence="persistence-eclipselink.properties"
  ,db="h2"
  ,before= {"create sequence xx_log_sequence increment by 1 start with 0;"
          ,"create sequence id_seq increment by 1 start with 0 NOCYCLE;"
  }
  ,imports = "src/test/resources/data/imports"
)
public class EntityImportTest {

    @PersistenceContext(unitName = "persistenceUnit")
    private EntityManager em;

    @Test
    public void testDaoCount() throws Exception {
        assertNotNull(em);
        LogDao dao = new LogDao();
        dao.setEntityManager(em);
        assertEquals(4L, dao.count());
    }
}
