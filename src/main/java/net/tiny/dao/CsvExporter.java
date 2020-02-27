package net.tiny.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.tiny.dao.reference.Column;
import net.tiny.dao.reference.SchemaParser;


public class CsvExporter {

    private static Logger LOGGER = Logger.getLogger(CsvExporter.class.getName());

    private static String TABLE_ORDERING_FILE = "table-ordering.txt";
    static final String SAVE_DATA_H2_FORMAT = "CALL CSVWRITE('%s', 'SELECT %s FROM %s', 'charset=UTF-8');";
    static final String SAVE_DATA_FORMAT = "SELECT * UNION ALL SELECT %s FROM %s INTO OUTFILE '%s' fields"
                                         + " TERMINATED BY '%s'"
                                         + " ENCLOSED BY '%s'"
                                         + " ESCAPED BY '\"' "
                                         + " LINES TERMINATED BY '\r\n'";

    private final Builder builder;

    private CsvExporter(Builder builder) {
        this.builder = builder;
    }

    /**
     * Export CSV file from given database table.
     *
     * @throws SQLException
     */
    public long save() throws SQLException {
        long s = builder.dao.count();
        save(builder.dao, builder.options);
        return builder.dao.count() - s;
    }

    /**
     * Export CSV file from given database table.
     *
     * @param dao JPA Data access object
     * @param options output option of CSV or TSV file
     * @throws SQLException
     */
    public static void save(IDao<?,?> dao, Options options) throws SQLException {
        final Connection connection = dao.getJdbcConnection();
        final SchemaParser parser = new SchemaParser(connection);
        final List<Column> list = parser.getColumns(options.schema, options.table);
        final String[] columns = new String[list.size()];
        for (int i=0; i<list.size(); i++) {
            columns[i] = list.get(i).getColumnName();
        }

        String sql;
        if (connection.toString().contains("h2")) {
            sql = generateSelectSql(options, columns, true);
        } else {
            sql = generateSelectSql(options, columns, false);
        }
        dao.executeNativeSQL(sql);
        if (options.verbose) {
            LOGGER.info(String.format("[CSV] export sql '%s'", sql));
        }
        dao.commitAndContinue();
    }

    private static String generateSelectSql(Options options, String[] columns, boolean h2) {
        String fields = String.join(",", columns);
        if (h2) {
            // Call H2Database CSVWRITE
            return String.format(SAVE_DATA_H2_FORMAT,
                    options.path.toFile().getAbsolutePath(), fields, options.table);
        } else {
            // Call General 'LOAD DATA ...'
            return String.format(SAVE_DATA_FORMAT,
                    fields, options.table,
                    options.delimiter, options.quotation, options.path.toFile().getAbsolutePath());
        }
    }

    /**
     * Export CSV data by given general database table name.
     *
     * @param connection Database connection
     * @param options Input source option of CSV or TSV file
     * @param sql The native SQL for load CSV file
     * @throws SQLException
     */
    private static void export(Connection connection, Options options, String sql) throws SQLException {
        Statement stmt = null;
        try {
            connection.setAutoCommit(false);
            stmt = connection.createStatement();
            if (options.verbose) {
                LOGGER.info(String.format("[CSV] export sql '%s'", sql));
            }
            stmt.execute(sql);

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException(String.format("Error '%s' occured while output data to file. Cause: ", sql, e.getMessage()), e);
        } finally {
            if (null != stmt)
                stmt.close();
        }
    }

    /**
     * Export CSV data by given general database table name.
     *
     * @param connection Database connection
     * @param options Output option of CSV or TSV file
     * @throws SQLException
     */
    public static void save(Connection connection, Options options) throws SQLException {
        if (null == connection || connection.isClosed()) {
            throw new SQLException("Not a valid connection.");
        }
        final SchemaParser parser = new SchemaParser(connection);
        final List<Column> list = parser.getColumns(options.schema, options.table);
        final String[] columns = new String[list.size()];
        for (int i=0; i<list.size(); i++) {
            columns[i] = list.get(i).getColumnName();
        }

        String sql;
        if (connection.toString().contains("h2")) {
            sql = generateSelectSql(options, columns, true);
        } else {
            sql = generateSelectSql(options, columns, false);
        }
        export(connection, options, sql);
    }

    /**
     * Export CSV data form the table name defined by the file 'table-ordering.txt'
     *
     * @param connection JDBC Connection
     * @param path The path of table ordering file.
     * @throws SQLException
     */
    public static void save(Connection connection, String path) throws SQLException {
        try {
            List<Options> options = options(path);
            for (Options op : options) {
                save(connection, op);
            }
        } catch (IOException e) {
            throw new SQLException(e.getMessage(), e);
        }
    }

    static List<Options> options(String path) throws IOException {
        List<Options> options = new ArrayList<>();
        Path table = Paths.get(path, TABLE_ORDERING_FILE);
        if (!Files.exists(table)) {
            throw new IllegalArgumentException(String.format("Not found '%s' file in '%s'.", TABLE_ORDERING_FILE, path));
        }
        List<String> lines = Files.readAllLines(table);
        for (String tab : lines) {
            Options op = new Options(String.format("%s/%s.csv", path, tab), tab);
            options.add(op);
        }
        return options;
    }

    public static class Options {
        Path path = null;
        String schema = "PUBLIC";
        String table = null;
        SeparatedValues.Type type = SeparatedValues.Type.CSV;
        String delimiter = ",";
        String quotation = "\"";
        boolean verbose = false;

        public Options() {}
        public Options(String p, String t) {
            path = Paths.get(p);
            table = t;
        }

        public Options path(String p) {
            path = Paths.get(p);
            return this;
        }

        public Options schema(String name) {
            schema = name;
            return this;
        }

        public Options table(String name) {
            table = name;
            return this;
        }
        public Options verbose(boolean enable) {
            verbose = enable;
            return this;
        }

        public Options type(String name) {
            if (null != name) {
                if ("csv".equalsIgnoreCase(name)) {
                    type = SeparatedValues.Type.CSV;
                    delimiter = ",";
                } else if ("tsv".equalsIgnoreCase(name)) {
                    type = SeparatedValues.Type.TSV;
                    delimiter = "\t";
                } else
                    throw new IllegalArgumentException(String.format("Unknow type '%s'", name));
            }
            return this;
        }


        public Options quotation(String q) {
            quotation = q;
            return this;
        }
    }

    public static class Builder {
        final IDao<?,?> dao;
        Options options;

        public Builder(IDao<?,?> d) {
            dao = d;
            options = new Options();
        }

        public Builder path(String p) {
            options.path(p);
            return this;
        }

        public Builder schema(String name) {
            options.schema(name);
            return this;
        }

        public Builder table(String name) {
            options.table(name);
            return this;
        }

        public Builder type(String name) {
            options.type(name);
            return this;
        }
        public Builder verbose(boolean enable) {
            options.verbose(enable);
            return this;
        }
        public CsvExporter build() {
            if (options.path == null || options.table == null) {
                throw new IllegalArgumentException("Path or Table name not set.");
            }
            return new CsvExporter(this);
        }
    }
}
