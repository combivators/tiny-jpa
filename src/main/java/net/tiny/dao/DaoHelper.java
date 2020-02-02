package net.tiny.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import net.tiny.dao.reference.Schema;
import net.tiny.dao.reference.SchemaParser;
import net.tiny.dao.reference.Table;

public class DaoHelper {

	public static Logger LOGGER = Logger.getLogger(DaoHelper.class.getName());

    public static Connection getConnection(EntityManager em) {
        return em.unwrap(Connection.class);
    }

	public static void load(Connection conn, String path) throws SQLException {
		CSVLoader.tableOrdering(conn, path);
		resetSequence(conn);
	}

	public static void resetSequence(Connection conn) throws SQLException {
		final String format = "alter sequence %s_sequence restart with %d increment by 1";
		final List<String> sequences = new ArrayList<String>();
		final Map<String, Long> ids = maxIds(conn);
		for(String key : ids.keySet()) {
			if(ids.get(key) > 0L) {
				long next = ids.get(key) + 1L;
				sequences.add(String.format(format, key, next).toLowerCase());
			}
		}
		batchScript(conn, sequences);
	}

    public static void executeScript(Connection conn, List<String> scripts) throws SQLException {
        if(null == scripts || scripts.isEmpty())
            return;
        int count = 0;
        long st  = System.currentTimeMillis();
        for(int i=0; i<scripts.size(); i++) {
        	String sql = scripts.get(i);
        	if (sql.startsWith("-"))
        		continue;
        	try {
        		PreparedStatement ps = conn.prepareStatement(sql);
        		ps.executeUpdate();
        		ps.close();
        		count++;
        		if (LOGGER.isLoggable(Level.FINE)) {
        			LOGGER.fine(String.format("[JDBC] Run script(%d) - '%s'", (i+1), sql));
        		}
        	} catch (SQLException e) {
         		LOGGER.log(Level.WARNING, String.format("[JDBC] Run script(%d) - '%s' error : %s", i, sql, e.getMessage()));
            }
        }
        conn.commit();
        LOGGER.info(String.format("[JDBC] Run %d scripts - %dms", count, (System.currentTimeMillis() - st)));
    }

    public static void batchScript(Connection conn, List<String> scripts) throws SQLException {
        if(null == scripts || scripts.isEmpty())
            return;
        long st  = System.currentTimeMillis();
		Statement s  = conn.createStatement();
        for(int i=0; i<scripts.size(); i++) {
        	String sql = scripts.get(i);
        	if (sql.startsWith("-"))
        		continue;
       		s.addBatch(sql);
        }
        s.executeBatch();
        close(s);
        conn.commit();
        LOGGER.info(String.format("[JDBC] Run batch scripts - %dms", (System.currentTimeMillis() - st)));
    }

	public static Map<String, Long> maxIds(Connection conn) throws SQLException {
        final SchemaParser parser = new SchemaParser(conn);
        final Schema schema = parser.parse("PUBLIC");
        List<Table> tables = schema.getTables();
		Map<String, Long> map = new LinkedHashMap<String, Long>();
		for(Table table : tables) {
			if(table.hasColumn("id")) {
				Long maxId = max(conn, table.getName(), "id", Long.class);
				if(null != maxId) {
					map.put(table.getName(), maxId);
				} else {
					map.put(table.getName(), 0L);
				}
			}
		}
		return map;
	}

	public static Map<String, Integer> countAll(Connection conn) throws SQLException {
		Map<String, Integer> map = new LinkedHashMap<String, Integer>();
        List<String> tables = tableNames(conn);
		for(String table : tables) {
			Integer num = count(conn, table);
			map.put(table, num);
		}
		return map;
	}

	public static int count(Connection conn, String table) throws SQLException {
		int num = 0;
		Statement stmt =conn.createStatement();
		String sql = "SELECT COUNT(*) FROM " + table;
		ResultSet res = stmt.executeQuery(sql);
		if(res.next()) {
			num = res.getInt(1);
		}
		close(res);
		return num;
	}

	static List<String> tableNames(Connection conn) throws SQLException {
        final SchemaParser parser = new SchemaParser(conn);
        final Schema schema = parser.parse("PUBLIC");
        List<Table> tables = schema.getTables();
        return tables.stream()
        	.map( t -> t.getName())
        	.collect(Collectors.toList());
	}

	static <T> T max(Connection conn, String table, String column, Class<T> type) throws SQLException {
		Object maxValue = null;
		Statement stmt =conn.createStatement();
		String sql = "SELECT max(" + column + ") FROM " + table;
		try {
			ResultSet res = stmt.executeQuery(sql);
			if(res.next()) {
				maxValue = res.getObject(1);
			}
			close(res);
			return  type.cast(maxValue);
		} catch(SQLException ex) {
			//Not found column 'ID'
			return null;
		}
	}

	/**
	 * Closes a ResultSet returned by {@link #executeQuery(String)}.
	 */
	static void close(ResultSet resultSet) {
		if (resultSet == null) {
			// nothing to do
			return;
		}
		Statement statement = null;
		try {
			statement = resultSet.getStatement();
		} catch (SQLException ignore) {
			// unable to get statement
		}
		try {
			resultSet.close();
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, "[JDBC] Unable to close result set!", e);
		} finally {
			close(statement);
		}
	}

	static void close(Statement statement) {
		if (statement != null) {
			try {
				statement.close();
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE, "[JDBC] Unable to close statement!", e);
			}
		}
	}
}
