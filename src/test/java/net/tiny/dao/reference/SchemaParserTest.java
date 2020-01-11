package net.tiny.dao.reference;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import javax.annotation.Resource;
import javax.sql.DataSource;

import net.tiny.unit.db.Database;

@Database(db="jpa-test"
  ,before={"CREATE TABLE APPLICATION_MESSAGE ( MESSAGE_ID CHAR(8) NOT NULL,MESSAGE VARCHAR(1024) NOT NULL,CLIENT_ENABLED CHAR(1) DEFAULT '0' NOT NULL,SUBSYSTEM_ID CHAR(4) NOT NULL,LAST_UPDATE TIMESTAMP NOT NULL,CONSTRAINT APPLICATION_MESSAGE_PK PRIMARY KEY (MESSAGE_ID))"}
  ,after= {"DROP TABLE APPLICATION_MESSAGE"})
public class SchemaParserTest {

    @Resource
    private DataSource ds;

    @Test
    public void testSchemaParser() throws Exception {
        Connection conn = ds.getConnection();
        assertNotNull(conn);

        SchemaParser parser = new SchemaParser(conn);

        Schema schema = parser.parse("PUBLIC");
        assertNotNull(schema);
        List<Table> tables = schema.getTables();
        assertEquals(1, tables.size());

        Table table = tables.get(0);
        assertEquals("APPLICATION_MESSAGE", table.getName());
        List<Column>  columns = table.getColumns();
        assertEquals(5, columns.size());

        Column column = columns.get(0);
        assertEquals("MESSAGE_ID", column.getColumnName());
        assertEquals("CHAR", column.getTypeName());
        assertEquals(8, column.getColumnSize());
        assertTrue(column.isPrimaryKey());

        PrimaryKey primaryKey = column.getPrimaryKey();
        assertEquals("APPLICATION_MESSAGE_PK", primaryKey.getPkName());

        conn.close();
    }
}
