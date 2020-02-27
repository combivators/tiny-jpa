package net.tiny.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
  ,imports="src/test/resources/data/imports"
)
public class CsvExporterTest {

    @PersistenceContext(unitName = "persistenceUnit")
    protected EntityManager entityManager;

    @Resource
    private DataSource ds;

    @Test
    public void testLoadedLogCSV() throws Exception {
        assertNotNull(entityManager);
        entityManager.getTransaction().begin();

        LogDao dao = new LogDao();
        dao.setEntityManager(entityManager);
        long num = dao.count();
        assertEquals(4, num, "Load CSV ");
        System.out.println("Load CSV " + num);
        entityManager.getTransaction().commit();
    }

    @Test
    public void testExportCSV() throws Exception {
        assertNotNull(ds);
        Path csv = Paths.get("src/test/resources/data/exports/XX_LOG.csv");
        assertFalse(Files.exists(csv));

        CsvExporter.Options options = new CsvExporter.Options("src/test/resources/data/exports/XX_LOG.csv", "xx_log")
                .verbose(true);
        Connection conn = ds.getConnection();
        CsvExporter.save(conn, options);
        conn.close();

        assertTrue(Files.exists(csv));
        List<String> lines = Files.readAllLines(csv);
        assertEquals(5, lines.size());
        Files.deleteIfExists(csv);
    }
}