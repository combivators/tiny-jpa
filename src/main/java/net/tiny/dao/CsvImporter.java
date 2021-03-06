package net.tiny.dao;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.validation.ConstraintViolation;

import net.tiny.dao.reference.Column;
import net.tiny.dao.reference.SchemaParser;

public class CsvImporter {

    private static Logger LOGGER = Logger.getLogger(CsvImporter.class.getName());

    private static String TABLE_ORDERING_FILE = "table-ordering.txt";

    static final String LOAD_DATA_FORMAT = "LOAD DATA LOCAL INFILE '%s' INTO TABLE %s FIELDS TERMINATED BY '%s'"
                                         + " OPTIONALLY ENCLOSED BY '%s'"
                                         + " ESCAPED BY '\\\\' "
                                         + " LINES TERMINATED BY '\r\n'"
                                         + " (%s) IGNORE %d LINES";

    private final Builder builder;

    private CsvImporter(Builder builder) {
        this.builder = builder;
    }

    /**
     * Parse CSV file and load in given database table.
     *
     * @throws SQLException
     */
    public long load() throws SQLException {
        long s = builder.dao.count();
        load(builder.dao, builder.options);
        return builder.dao.count() - s;
    }

    /**
     * Parse CSV file and load in given database table.
     *
     * @param dao JPA Data access object
     * @param options Input source option of CSV or TSV file
     * @throws SQLException
     */
    public static void load(IDao<?,?> dao, Options options) throws SQLException {
        EntityReader<?> reader = null;
        try {
            Class<?> type = dao.getEntityType();
            reader = new EntityReader<>(type, options);

            if(options.truncated) {
                // Delete data from table before loading csv
                dao.removeAll();
                dao.commitAndContinue();
            }

            Connection connection = dao.getJdbcConnection();
            final boolean header = (options.skip == 1);
            final String[] columns;
            if (header) {
                columns = columns(options);
            } else {
                SchemaParser parser = new SchemaParser(connection);
                List<Column> list = parser.getColumns(options.schema, options.table);
                columns = new String[list.size()];
                for (int i=0; i<list.size(); i++) {
                    columns[i] = list.get(i).getColumnName();
                }
            }

            String sql;
            if (connection.toString().contains("h2")) {
                sql = generateLoadDataSql(options, columns, true);
            } else {
                sql = generateLoadDataSql(options, columns, false);
            }
            dao.executeNativeSQL(sql);
            if (options.verbose) {
                LOGGER.info(String.format("[CSV] import sql '%s'", sql));
            }
            dao.commitAndContinue();
        } catch (IOException e) {
            throw new SQLException("Error occured while executing file. Cause: " + e.getMessage(), e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                throw new SQLException("Error occured while closing file. Cause: " + e.getMessage(), e);
            }
        }
    }


    private static String generateLoadDataSql(Options options, String[] columns, boolean h2) {
        String fields = String.join(",", columns);
        if (h2) {
            // Call H2Database CSVREAD
            return String.format("INSERT INTO %s (%s) SELECT * FROM CSVREAD('%s', null, 'UTF-8', '%s')",
                    options.table, fields, options.path.toFile().getAbsolutePath(), options.delimiter);
        } else {
            // Call General 'LOAD DATA ...'
            return String.format(LOAD_DATA_FORMAT,
                    options.path.toFile().getAbsolutePath(),
                    options.table, options.delimiter, options.quotation, fields, options.skip);
        }
    }


    private static String[] columns(Options options) throws SQLException {
        String line = null;
        LineNumberReader reader = null;
        try {
            reader = new LineNumberReader(Files.newBufferedReader(options.path));
            line = reader.readLine();
        } catch (IOException e) {
            throw new SQLException("Error occured while loading data from file to database. Cause: " + e.getMessage(), e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                throw new SQLException("Error occured while closing data from file to database. Cause: " + e.getMessage(), e);
            }
        }
        final String[] columns = line.split(options.delimiter);
        for (int i=0; i<columns.length; i++) {
            columns[i] = columns[i].trim();
        }
        return columns;
    }

    /**
     * Parse CSV file and load in given general database table.
     *
     * @param connection Database connection
     * @param options Input source option of CSV or TSV file
     * @param sql The native SQL for load CSV file
     * @throws SQLException
     */
    private static void loadGeneral(Connection connection, Options options, String sql) throws SQLException {
        Statement stmt = null;
        try {
            connection.setAutoCommit(false);
            if(options.truncated) {
                // Delete data from table before loading csv
                stmt = connection.createStatement();
                String del = "DELETE FROM " + options.table;
                stmt.execute(del);
                stmt.close();
                //TODO Rest sequence
                if (options.verbose) {
                    LOGGER.info(String.format("[CSV] import options.truncated '%s' : '%s'", options.truncated, del));
                }
            }
            stmt = connection.createStatement();
            stmt.execute(sql);
            if (options.verbose) {
                LOGGER.info(String.format("[CSV] import sql '%s'", sql));
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException("Error occured while loading data from file to database. Cause: " + e.getMessage(), e);
        } finally {
            if (null != stmt)
                stmt.close();
        }
    }

    /**
     * Parse CSV file and load in given general database table.
     *
     * @param connection Database connection
     * @param options Input source option of CSV or TSV file
     * @throws SQLException
     */
    public static void load(Connection connection, Options options) throws SQLException {
        if (null == connection || connection.isClosed()) {
            throw new SQLException("Not a valid connection.");
        }
        final boolean header = (options.skip == 1);
        final String[] columns;
        if (header) {
            columns = columns(options);
        } else {
            SchemaParser parser = new SchemaParser(connection);
            List<Column> list = parser.getColumns(options.schema, options.table);
            columns = new String[list.size()];
            for (int i=0; i<list.size(); i++) {
                columns[i] = list.get(i).getColumnName();
            }
        }
        String sql;
        if (connection.toString().contains("h2")) {
            sql = generateLoadDataSql(options, columns, true);
        } else {
            sql = generateLoadDataSql(options, columns, false);
        }
        loadGeneral(connection, options, sql);
    }

    static List<Options> options(String path) throws IOException {
        List<Options> options = new ArrayList<>();
        Path table = Paths.get(path, TABLE_ORDERING_FILE);
        if (!Files.exists(table)) {
            throw new IllegalArgumentException(String.format("Not found '%s' file in '%s'.", TABLE_ORDERING_FILE, path));
        }
        List<String> lines = Files.readAllLines(table);
        for (String tab : lines) {
            Options op = new Options(String.format("%s/%s.csv", path, tab), tab)
                    .truncated(true)
                    .skip(1);
            options.add(op);
        }
        return options;
    }

    /**
     * Load all CSV data form file 'table-ordering.txt' to database
     *
     * @param connection JDBC Connection
     * @param path The path of table ordering file.
     * @throws SQLException
     */
    public static void load(Connection connection, String path) throws SQLException {
        try {
            List<Options> options = options(path);
            for (Options op : options) {
                load(connection, op);
            }
        } catch (IOException e) {
            throw new SQLException(e.getMessage(), e);
        }
    }

    public static class Options {
        Path path = null;
        String schema = "PUBLIC";
        String table = null;
        SeparatedValues.Type type = SeparatedValues.Type.CSV;
        String delimiter = ",";
        String quotation = "\"";
        //int batchSize = Constants.DEFAULT_BATCH_SIZE;
        int skip = 0;
        boolean truncated = false;
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

        public Options skip(int s) {
            skip = s;
            return this;
        }

        public Options truncated(boolean enable) {
            truncated = enable;
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

        public Builder skip(int s) {
            options.skip(s);
            return this;
        }

        public Builder truncated(boolean enable) {
            options.truncated(enable);
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

        public CsvImporter build() {
            if (options.path == null || options.table == null) {
                throw new IllegalArgumentException("Path or Table name not set.");
            }
            return new CsvImporter(this);
        }
    }

    static class EntityReader<T> implements Closeable {
        final BufferedReader reader;
        final Iterator<T> entities;
        final Map<String, Set<ConstraintViolation<?>>> error = new HashMap<>();

        EntityReader(Class<T> type, Options options) throws IOException {
            reader = Files.newBufferedReader(options.path);
            entities = SeparatedIterator.parse(reader, type, options.type, options.skip, error);
        }

        public T readNext() {
            return entities.next();
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }
}
