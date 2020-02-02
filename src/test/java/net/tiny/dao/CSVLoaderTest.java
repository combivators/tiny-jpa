package net.tiny.dao;


import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.util.List;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import net.tiny.dao.test.LogDao;
import net.tiny.unit.db.Database;

@Database(persistence="persistence-eclipselink.properties"
 ,trace=true
 ,before= {"create sequence xx_log_sequence increment by 1 start with 1;"
        ,"create sequence id_seq increment by 1 start with 0 NOCYCLE;"
 }
)
public class CSVLoaderTest {

    @PersistenceContext(unitName = "persistenceUnit")
    protected EntityManager entityManager;

    @Resource
    private DataSource ds;

    @Test
    public void testLoadCSV() throws Exception {
        assertNotNull(ds);
        CSVLoader.Options options = new CSVLoader.Options("src/test/resources/data/imports/XX_LOG.csv", "xx_log")
                .truncated(true)
                .skip(1);
        Connection conn = ds.getConnection();
        CSVLoader.load(conn, options);
        conn.close();
    }

    @Test
    public void testTableOrdering() throws Exception {
    	List<CSVLoader.Options> options = CSVLoader.options("src/test/resources/data/imports");
    	assertEquals(1, options.size());
        Connection conn = ds.getConnection();
    	CSVLoader.tableOrdering(conn, "src/test/resources/data/imports");
        conn.commit();
        conn.close();
    }

    @Test
    public void testLoadLogCSV() throws Exception {
        assertNotNull(entityManager);
        entityManager.getTransaction().begin();

        LogDao dao = new LogDao();
        dao.setEntityManager(entityManager);
        CSVLoader loader = new CSVLoader.Builder(dao)
                .path("src/test/resources/data/imports/XX_LOG.csv")
                .table("xx_log")
                .truncated(true)
                .skip(1)
                .build();

        long num = loader.load();
        assertEquals(4, num, "Load CSV ");
        System.out.println("Load CSV " + num);
        entityManager.getTransaction().commit();
    }
}
