package net.tiny.dao;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import net.tiny.dao.reference.Column;
import net.tiny.dao.reference.Schema;
import net.tiny.dao.reference.SchemaParser;
import net.tiny.dao.reference.Table;
import net.tiny.dao.test.entity.Log;
import net.tiny.unit.db.Database;


@Database(persistence="persistence-eclipselink.properties"
  ,trace=true
  ,before= {"create sequence xx_log_sequence increment by 1 start with 1;"
          ,"create sequence id_seq increment by 1 start with 0 NOCYCLE;"
  }
  )
public class JpaBasicTest {

    @PersistenceContext(unitName = "persistenceUnit")
    private EntityManager em;

    @Resource
    private DataSource ds;

    @Test
    public void testVaildEntityManager() throws Exception {
        //javax.persistence.validation.mode = auto
        //javax.persistence.schema-generation.database.action = create
        //javax.persistence.schema-generation.create-source = metadata
        assertNotNull(em);

        EntityTransaction trx = em.getTransaction();
        trx.begin();
        Log log = em.find(Log.class, 1L);
        assertNull(log);

        log = new Log();
        log.setContent("contentx1024");
        log.setOperation("push");
        log.setOperator("admin");
        log.setParameter("123");
        log.setIp("192.168.100.200");
        em.persist(log);
        em.flush();
        em.clear();

        log = em.find(Log.class, log.getId());
        assertNotNull(log);
        assertNotNull(log.getCreateDate());
        assertNotNull(log.getModifyDate());

        trx.commit();
    }

    @Test
    public void testSimpleEntity() throws Exception {
        Connection conn = ds.getConnection();
        assertNotNull(conn);

        SchemaParser parser = new SchemaParser(conn);

        Schema schema = parser.parse("PUBLIC");
        assertNotNull(schema);
        List<Table> tables = schema.getTables();
        assertEquals(7, tables.size());

        Table table = parser.getTable("PUBLIC", "XX_LOG");
        assertEquals("XX_LOG", table.getName());
        List<Column>  columns = table.getColumns();
        assertEquals(8, columns.size());

        Column column = columns.get(0);
        assertEquals("ID", column.getColumnName());
        assertEquals("BIGINT", column.getTypeName());
        assertEquals(19, column.getColumnSize());
        assertTrue(column.isPrimaryKey());
        conn.commit();
        conn.close();
    }

/*
    @ParameterizedTest
    @ArgumentsSource(JpaHelperProvider.class)
    public void testDropAllTablesAndSequence(JpaHelper helper) throws Exception {
        String[] tables = helper.getTableNames();
        System.out.println("tables: " + tables.length);
        Connection conn = helper.getJdbcConnection();
        Statement stmt =conn.createStatement();

        for(String table : tables) {
            String sql = "DROP TABLE " + table;
            System.out.println(sql);
            stmt.execute(sql);
        }
        stmt.close();
        conn.commit();
        conn.close();

        conn = helper.getJdbcConnection();
        stmt =conn.createStatement();
        for(String table : tables) {
            String sql = "DROP SEQUENCE " + table + "_SEQUENCE";
            try {
                stmt.execute(sql);
                System.out.println(sql);
            } catch (SQLException ex) {}
        }
        stmt.close();
        conn.commit();
        conn.close();
    }

    @ParameterizedTest
    @ArgumentsSource(JpaHelperProvider.class)
    public void testMaxId(JpaHelper helper) throws Exception {
        String table = "machine";
        Long id = helper.max(table, "id", Long.class);
        System.out.println(table + " max id : " + id);
        try {
             table = "maker";
             id = helper.max(table, "id", Long.class);
            System.out.println(table + " max id : " + id);
        }catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @ParameterizedTest
    @ArgumentsSource(JpaHelperProvider.class)
    public void testShowAllTables(JpaHelper helper) throws Exception {
        String[] schemas = helper.getSchemas();
        assertTrue(schemas.length > 0);
        System.out.println("Schema");
        for(String schema : schemas) {
            System.out.println("\t" + schema);
        }
        System.out.println();

        String[] types = helper.getTableTypes();
        assertTrue(types.length > 0);
        System.out.println("Types");
        for(String type : types) {
            System.out.println("\t" + type);
        }
        System.out.println();

        List<String>  list = helper.getTables("PUBLIC");
        //assertTrue(list.size() > 0);
        System.out.println("PUBLIC Tables");
        for(String tab : list) {
            System.out.println("\t" + tab);
        }
        System.out.println();

        String[]  tables = helper.getTableNames();
        //assertTrue(tables.length > 0);
        System.out.println("All Tables");
        for(String tab : tables) {
            System.out.println("\t" + tab);
        }
        System.out.println();

        //All Index
        System.out.println("All Index");
        for(String tab : tables) {
            List<Index>  indexes = helper.getSchemaParser().getIndexs("PUBLIC", tab);
            for(Index idx : indexes) {
                System.out.println("\t" + tab + " " + idx.toString());
            }
        }
        System.out.println();

        //Count up all
        Map<String, Integer>  counts = helper.countAll();
        //assertTrue(counts.size() > 0);
        Set<String> keys = counts.keySet();
        System.out.println("Count : ");
        for(String key : keys) {
            System.out.println("\t" + key + " " + counts.get(key));
        }
        System.out.println();


        SchemaParser parser = helper.getSchemaParser();
        assertNotNull(parser);

        String tableName = "xx_admin";
        List<Column> columns = parser.getColumns("PUBLIC", tableName);
        //assertTrue(columns.size() > 0);
        //Column column = columns.get(0);
        for(Column column : columns) {
            System.out.println(tableName + " -> " + column.getColumnName());
        }

        List<PrimaryKey> pkeys = parser.getPrimaryKeys("PUBLIC", tableName);
        //assertTrue(pkeys.size() > 0);
        for(PrimaryKey key : pkeys) {
            System.out.println(tableName + " -> " + key.getName() + " , " + key.getPkName() + " ,  " + key.getColumnName());
        }

        List<Index> indexs = parser.getIndexs("PUBLIC", tableName);
        //assertTrue(indexs.size() > 0);
        for(Index index : indexs) {
            System.out.println(tableName + " -> " + index.getIndexName() +  " ,  " + index.getColumnName());
        }

        Schema schema = helper.getSchema("PUBLIC");
        assertNotNull(schema);

        Table table = helper.getTable("PUBLIC", tableName);
        assertNotNull(table);
        System.out.println(table.toString());

        //assertTrue(table.hasColumn("id"));
    }
*/
}
