package net.tiny.dao.reference;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SchemaParser {
    private static final Logger LOGGER = Logger.getLogger(SchemaParser.class.getName());

    public static final String TABLE_SCHEM   = "TABLE_SCHEM";
    public static final String TABLE_CATALOG = "TABLE_CAT";
    public static final String TABLE_TYPE    = "TABLE_TYPE";
    public static final String TABLE_NAME    = "TABLE_NAME";

    private final Connection jdbcConnection;
    private DatabaseMetaData databaseMetaData = null;

    public SchemaParser(Connection connection) throws SQLException {
        this.jdbcConnection = connection;
    }

    public Schema parse(String name)  throws SQLException {
        String[] schemas = getSchemas();
        boolean found = false;
        for (String schema : schemas) {
            if(schema.equalsIgnoreCase(name)) {
                found = true;
                break;
            }
        }
        if(!found) {
            // Not found this schema
            throw new SQLException("Not found schema '" + name +"'");
        }
        List<String> tableNames = getTables(name);
        Schema schema = new Schema(name);
        List<Table> tables = new ArrayList<Table>();
        for(String tableName : tableNames) {
            tables.add( getTable(name, tableName) );
        }
        schema.setTables(tables);
        return schema;
    }

    public String[] getSchemas() throws SQLException {
        ResultSet rs = null;
        List<String> list = new ArrayList<String>();
        try {
            DatabaseMetaData dmd = getDatabaseMetaData();
            rs = dmd.getSchemas();
            while (rs.next()) {
                list.add(rs.getString(TABLE_SCHEM));
            }
        } finally {
            close (rs);
        }
        return list. toArray(new String[list.size()]);
    }

    public String[] getCatalogs() throws SQLException {
        ResultSet rs = null;
        List<String> list = new ArrayList<String>();
        try {
            DatabaseMetaData dmd = getDatabaseMetaData();
            rs = dmd.getCatalogs();
            while (rs.next()) {
                list.add(rs.getString(TABLE_CATALOG));
            }
        } finally {
            close (rs);
        }
        return list. toArray(new String[list.size()]);
    }

    public String[] getTableTypes() throws SQLException {
        ResultSet rs = null;
        List<String> list = new ArrayList<String>();
        try {
            DatabaseMetaData dmd = getDatabaseMetaData();
            rs = dmd.getTableTypes();
            while (rs.next()) {
                list.add(rs.getString(TABLE_TYPE));
            }
        } finally {
            close (rs);
        }
        return list. toArray(new String[list.size()]);
    }

    public List<Column> getColumns(String schema, String table) throws SQLException {
        ResultSet rs = null;
        List<Column> list = new ArrayList<Column>();
        try {
            DatabaseMetaData dmd = getDatabaseMetaData();
            rs = dmd.getColumns(null, schema.toUpperCase(), table.toUpperCase(), null);
            while (rs.next()) {
                list.add(setAttributes(Column.class, rs));
            }
        } finally {
            close (rs);
        }
        return list;
    }

    public List<PrimaryKey> getPrimaryKeys(String schema, String table) throws SQLException {
        ResultSet rs = null;
        List<PrimaryKey> list = new ArrayList<PrimaryKey>();
        try {
            DatabaseMetaData dmd = getDatabaseMetaData();
            rs = dmd.getPrimaryKeys(null, schema.toUpperCase(), table.toUpperCase());
            while (rs.next()) {
                list.add(setAttributes(PrimaryKey.class, rs));
            }
        } finally {
            close (rs);
        }
        return list;
    }

    public List<Index> getIndexs(String schema, String table) throws SQLException {
        ResultSet rs = null;
        List<Index> list = new ArrayList<Index>();
        try {
            DatabaseMetaData dmd = getDatabaseMetaData();
            rs = dmd.getIndexInfo(null, schema.toUpperCase(), table.toUpperCase(), false, true);
            while (rs.next()) {
                list.add(setAttributes(Index.class, rs));
            }
        } finally {
            close (rs);
        }
        return list;
    }

    public Table getTable(String schema, String table) throws SQLException {
        List<Column> columns = getColumns(schema,  table);
        Map<String, Column> map = new HashMap<String, Column>();
        for(Column column : columns) {
            map.put(column.getColumnName(), column);
        }
        List<PrimaryKey> pkeys = getPrimaryKeys(schema,  table);
        for(PrimaryKey pkey : pkeys) {
            Column column = map.get(pkey.getColumnName());
            column.setPrimaryKey(pkey);
        }
        List<Index> indexs = getIndexs(schema,  table);
        for(Index idx : indexs) {
            Column column = map.get(idx.getColumnName());
            column.setIndex(idx);
        }
        return new Table(table, columns);
    }

    public List<String> getTables(String schema) throws SQLException {
        ResultSet rs = null;
        List<String> list = new ArrayList<String>();
        try {
            DatabaseMetaData dmd = getDatabaseMetaData();
            String[] tps = new String[] {"TABLE"};
            rs = dmd.getTables(null, schema.toUpperCase(), "%", tps);
            while (rs.next()) {
                list.add(rs.getString(TABLE_NAME).toUpperCase());
            }
        } finally {
            close (rs);
        }
        return list;
    }

    public DatabaseMetaData getDatabaseMetaData() throws SQLException {
        if(null == databaseMetaData) {
            databaseMetaData = jdbcConnection.getMetaData();
        }
        return databaseMetaData;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> T setAttributes(Class<T>classType, ResultSet rs) throws SQLException {
        try {
             ResultSetMetaData metaData = rs.getMetaData();
             int count = metaData.getColumnCount();
            List<String> columns = new ArrayList<String>();
            boolean underscore = false;
            for(int i=1; i<=count; i++) {
                String cn = metaData.getColumnName(i);
                if(!underscore && cn.indexOf("_") > 0) {
                    underscore = true;
                }
                columns.add(cn);
            }
            T t = (T)classType.newInstance();
            BeanInfo javaBean = Introspector.getBeanInfo(classType);
            List<PropertyDescriptor> descriptors =
                    getPropertyDescriptors(javaBean.getPropertyDescriptors(), columns, underscore);

            for(PropertyDescriptor property : descriptors) {
                Method getter = property.getReadMethod();
                Method setter = property.getWriteMethod();

                String columnName = property.getName();
                Class type = getter.getReturnType();
                if(underscore) {
                    columnName = addUnderscores(columnName).toUpperCase();
                }
                Object arg = rs.getObject(columnName);
                if (arg != null) {
                    if(arg instanceof java.sql.Blob) {
                        // BLOB
                        arg = toObject(((java.sql.Blob)arg).getBinaryStream());
                    } else if(arg instanceof BigDecimal) {
                        BigDecimal decimal = (BigDecimal)arg;
                        if(int.class.equals(type) || Integer.class.equals(type)) {
                            arg = decimal.intValue();
                        } else if(short.class.equals(type) || Short.class.equals(type)) {
                                arg = decimal.shortValue();
                        } else if(long.class.equals(type) || Long.class.equals(type)) {
                            arg = decimal.longValue();
                        } else if(float.class.equals(type) || Float.class.equals(type)) {
                            arg = decimal.floatValue();
                        } else if(double.class.equals(type) || Double.class.equals(type)) {
                            arg = decimal.doubleValue();
                        }
                    } else if(type.isEnum()) {
                        arg = Enum.valueOf(type, arg.toString());
                    }
                }
                if(null != arg) {
                    invoke(t, setter, arg);
                }
            }
            return t;
        } catch (InstantiationException | IllegalAccessException | IntrospectionException ex) {
            throw new IllegalArgumentException("Can not support property class type '"
                    + classType.getName() +"'", ex);
        }
    }


    /**
     * Closes a ResultSet returned by {@link #executeQuery(String)}.
     */
    protected void close(ResultSet resultSet) {
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
            LOGGER.log(Level.SEVERE, "Unable to close result set!", e);
        } finally {
            close(statement);
        }
    }

    void close(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Unable to close statement!", e);
            }
        }
    }

    private static List<PropertyDescriptor> getPropertyDescriptors(PropertyDescriptor[] descriptors, List<String> columns, boolean underscore) throws SQLException {
        List<PropertyDescriptor> list = new ArrayList<PropertyDescriptor>();
        for(PropertyDescriptor property : descriptors) {
            Method getter = property.getReadMethod();
            Method setter = property.getWriteMethod();
            if (null == getter || null == setter)
                continue;
            String columnName = property.getName();
            if(underscore) {
                columnName = addUnderscores(columnName).toUpperCase();
            }
            if(columns.contains(columnName)) {
                list.add(property);
            }
        }
        return list;
    }

    private static void invoke(Object instance, Method setter, Object arg) {
        try {
            setter.invoke(instance, arg);
        } catch (Exception ex) {
            StringBuilder sb = new StringBuilder();
            sb.append(instance.getClass().getName());
            sb.append("#");
            sb.append(setter.getName());
            sb.append("( ");
            sb.append(arg.getClass().getName());
            sb.append(":'");
            sb.append(arg.toString());
            sb.append("'");
            sb.append(" )  Error: ");
            sb.append(ex.getMessage());
            throw new RuntimeException(sb.toString(), ex);
        }
    }

    private static final Object toObject(InputStream in) {
        if(in == null) return null;;
        Object obj = null;
        try {
            ObjectInputStream oin = new ObjectInputStream(in);
            obj = oin.readObject();
            oin.close();
        } catch(Exception ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
        return obj;
    }

       protected static String addUnderscores(String name) {
            if (name == null)
                return null;
            StringBuffer buf = new StringBuffer(name.replace('.', '_'));
            for (int i = 1; i < buf.length() - 1; i++) {
                if ((isLowerToUpper(buf, i)) || (isMultipleUpperToLower(buf, i))

                ) {
                    buf.insert(i++, '_');
                }
            }
            return buf.toString().toLowerCase();
        }

        private  static boolean isMultipleUpperToLower(StringBuffer buf, int i) {
            return i > 1 && Character.isUpperCase(buf.charAt(i - 1))
                    && Character.isUpperCase(buf.charAt(i - 2))
                    && Character.isLowerCase(buf.charAt(i));
        }

        private static boolean isLowerToUpper(StringBuffer buf, int i) {
            return Character.isLowerCase(buf.charAt(i - 1))
                    && Character.isUpperCase(buf.charAt(i));
        }
}
