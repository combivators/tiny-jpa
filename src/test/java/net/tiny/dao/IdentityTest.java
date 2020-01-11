package net.tiny.dao;

import static org.junit.jupiter.api.Assertions.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;

import net.tiny.dao.test.entity.Id1;
import net.tiny.dao.test.entity.Id2;
import net.tiny.dao.test.entity.Id3;
import net.tiny.dao.test.entity.Id4;
import net.tiny.dao.test.entity.Id5;

import net.tiny.unit.db.Database;

@Database(persistence="persistence-eclipselink.properties"
  ,trace=true
  ,before= {
     "create sequence xx_log_sequence increment by 1 start with 0;"
     ,"create sequence id_seq increment by 1 start with 0 NOCYCLE;"
     //,"ALTER SEQUENCE id_seq RESTART START WITH 1"
   }
)
public class IdentityTest {

    @PersistenceContext(unitName = "persistenceUnit")
    private EntityManager em;

    @Test
    public void testIdentity() throws Exception {
        assertNotNull(em);

        em.getTransaction().begin();

        testId1();
        em.flush();

        testId2();
        em.flush();

        testId3();
        em.flush();

        testId4();
        em.flush();

        testId5();
        em.flush();

        em.getTransaction().commit();
    }

    void testId1() throws Exception {
        BaseDao<Id1, Long> id1Dao = new BaseDao<Id1, Long>() {};
        id1Dao.setEntityManager(em);
        Id1 id1 = new Id1();
        id1Dao.insert(id1);
        System.out.println("id1: " + id1.getId());
        assertEquals(1L, id1.getId());

        id1 = new Id1();
        id1Dao.insert(id1);
        assertEquals(2L, id1.getId());
    }

    void testId2() throws Exception {
        BaseDao<Id2, Long> id2Dao = new BaseDao<Id2, Long>() {};
        id2Dao.setEntityManager(em);
        Id2 id2 = new Id2();
        id2Dao.insert(id2);
        System.out.println("id2: " + id2.getId());
        assertEquals(3L, id2.getId()); // Id1,Id2 share a Identity
        id2 = new Id2();
        id2Dao.insert(id2);
        assertEquals(4L, id2.getId());
    }

    void testId3() throws Exception {
        BaseDao<Id3, Long> id3Dao = new BaseDao<Id3, Long>() {};
        id3Dao.setEntityManager(em);
        Id3 id3 = new Id3();
        id3Dao.insert(id3);
        System.out.println("id3: " + id3.getId());
        assertNull(id3.getId()); //Eclipse-Link JPA: strategy=GenerationType.IDENTITY NG!
    }

    void testId4() throws Exception {
        BaseDao<Id4, Long> id4Dao = new BaseDao<Id4, Long>() {};
        id4Dao.setEntityManager(em);
        Id4 id4 = new Id4();
        id4Dao.insert(id4);
        System.out.println("id4: " + id4.getId());
        assertEquals(1L, id4.getId());

        id4 = new Id4();
        id4Dao.insert(id4);
        assertEquals(2L, id4.getId());
    }

    void testId5() throws Exception {
        BaseDao<Id5, Long> id5Dao = new BaseDao<Id5, Long>() {};
        id5Dao.setEntityManager(em);
        Id5 id5 = new Id5();
        id5Dao.insert(id5);
        System.out.println("id5: " + id5.getId());
        assertEquals(1L, id5.getId());
    }
}
