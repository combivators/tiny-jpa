package net.tiny.unit.ws;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpHandler;

import net.tiny.config.Config;
import net.tiny.config.Configuration;
import net.tiny.config.ConfigurationHandler;
import net.tiny.config.Reflections;
import net.tiny.dao.HttpTransactionFilter;
import net.tiny.dbcp.SimpleDataSource;
import net.tiny.unit.db.H2Engine;
import net.tiny.unit.db.JpaHelper;
import net.tiny.unit.db.ScriptRunner;
import net.tiny.ws.AccessLogger;
import net.tiny.ws.EmbeddedServer;
import net.tiny.ws.WebServiceHandler;

public class ServerExtension implements BeforeAllCallback, AfterAllCallback, BeforeTestExecutionCallback {

    protected static enum StoreKeyType {
        WEB, RDB, DATASOURCE, JPA
    }

    protected static final Namespace NAMESPACE = Namespace.create("net", "tiny", "unit", "ServerExtension");

    private static String lastLogging = "";
    private static boolean logged = false;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Server server = getServerAnnotation(context);
        if(null == server)
            return;
        logging(server.logging());
        //启动H2数据库
        H2Engine engine = new H2Engine.Builder()
                .port(server.rdb())
                .name(server.db())
                .clear(server.clear())
                .before(server.before())
                .after(server.after())
                .build();
        context.getStore(NAMESPACE).put(getStoreKey(context, StoreKeyType.RDB), engine);
        engine.start();

        SimpleDataSource datasource = new SimpleDataSource();
        datasource.getBuilder()
            .driver("org.h2.Driver")
            .url(String.format("jdbc:h2:tcp://localhost:%d/%s", server.rdb(), server.db()))
            .username("sa")
            .password("");
        context.getStore(NAMESPACE).put(getStoreKey(context, StoreKeyType.DATASOURCE), datasource);

        if (!server.createScript().isEmpty()) {
            runScript(new File(server.createScript()), datasource.getConnection());
        }

        //JPA
        JpaHelper helper = new JpaHelper(server.persistence(),
                server.unit(), server.rdb(), server.db(), server.trace());
        if (!server.persistence().isEmpty()) {
            context.getStore(NAMESPACE).put(getStoreKey(context, StoreKeyType.JPA), helper);
            helper.start();
        }
        if (!server.imports().isEmpty()) {
            helper.importCsv(new File(server.imports()));
        }

        //启动内置WEB服务器
        EmbeddedServer embedded = new EmbeddedServer.Builder()
                .port(server.web())
                .backlog(server.backlog())
                .handlers(handlers(server, helper.getEntityManagerFactory()))
                .build();
        context.getStore(NAMESPACE).put(getStoreKey(context, StoreKeyType.WEB), embedded);

        embedded.listen(callback -> {
            if(callback.success()) {
                System.out.println("Server listen on port: " + server.web());
            } else {
                callback.cause().printStackTrace();
            }
        });
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        Object testCase = context.getRequiredTestInstance();
        // Find a field with @Resource
        List<Field> withResouceAnnotatedFields = findAnnotatedFields(testCase.getClass(), Resource.class);
        SimpleDataSource datasource = context.getStore(NAMESPACE)
                .get(getStoreKey(context, StoreKeyType.DATASOURCE), SimpleDataSource.class);
        for(Field field : withResouceAnnotatedFields) {
            final Class<?> resourceType = Class.forName(field.getGenericType().getTypeName());
            if (DataSource.class.equals(resourceType)) {
                field.setAccessible(true);
                // Set DataSource instance
                field.set(testCase, datasource);
            }
        }

        JpaHelper helper = context.getStore(NAMESPACE)
                .get(getStoreKey(context, StoreKeyType.JPA), JpaHelper.class);
        if (null == helper)
            return;
        // Find a field with @PersistenceContext
        List<Field> withPersistenceAnnotatedFields = findAnnotatedFields(testCase.getClass(), PersistenceContext.class);
        for(Field field : withPersistenceAnnotatedFields) {
            final String unitName = field.getAnnotation(PersistenceContext.class).unitName();
            final Class<?> resourceType = Class.forName(field.getGenericType().getTypeName());
            if (EntityManager.class.equals(resourceType) && (unitName.isEmpty() || helper.getUnitName().equals(unitName))) {
                field.setAccessible(true);
                // Set EntityManager instance
                field.set(testCase, helper.getEntityManager());
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        Server server = getServerAnnotation(context);
        if(null == server)
            return;
        //停止WEB服务器
        EmbeddedServer embedded = context.getStore(NAMESPACE).remove(getStoreKey(context, StoreKeyType.WEB), EmbeddedServer.class);
        embedded.close();
        embedded.awaitTermination();

        //停止JPA
        if (!server.persistence().isEmpty()) {
            JpaHelper helper = context.getStore(NAMESPACE).remove(getStoreKey(context, StoreKeyType.JPA), JpaHelper.class);
            helper.stop();
        }

        //关闭数据源
        SimpleDataSource datasource = context.getStore(NAMESPACE)
                .remove(getStoreKey(context, StoreKeyType.DATASOURCE), SimpleDataSource.class);
        if (!server.dropScript().isEmpty()) {
            runScript(new File(server.dropScript()), datasource.getConnection());
        }
        datasource.close();

        //停止2数据库
        H2Engine engine = context.getStore(NAMESPACE).remove(getStoreKey(context, StoreKeyType.RDB), H2Engine.class);
        engine.stop();
    }

    static void logging(String file) {
        if (logged && lastLogging.equals(file))
            return;
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final URL url = loader.getResource(file);
        if (null != url) {
            try {
                LogManager.getLogManager().readConfiguration(url.openStream());
                logged = true;
                lastLogging = file;
            } catch (Exception ignore) {
            }
        }
    }

    protected static String getStoreKey(ExtensionContext context, StoreKeyType type) {
        String storedKey = context.getRequiredTestClass().getName();
        switch(type) {
        case WEB:
            storedKey = storedKey.concat(".ws");
            break;
        case RDB:
            storedKey = storedKey.concat(".db");
            break;
        case DATASOURCE:
            storedKey = storedKey.concat(".datasource");
            break;
        case JPA:
            storedKey = storedKey.concat(".jpa");
            break;
        }
        return storedKey;
    }

    protected static Server getServerAnnotation(ExtensionContext context) {
        try {
            return context.getElement()
                    .filter(el -> el.isAnnotationPresent(Server.class))
                    .get()
                    .getAnnotation(Server.class);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /**
     * 按测试配置文件动态生成测试微服句柄
     * @param server
     * @return
     */
    static List<HttpHandler> handlers(Server server, EntityManagerFactory factory) {
        final List<Filter> filters = new ArrayList<>();
        filters.add(new AccessLogger());

        final HttpTransactionFilter trx = new HttpTransactionFilter()
           .factory(factory);
        filters.add(trx);

        final ConfigurationHandler handler = new ConfigurationHandler();
        handler.setResource(server.config());
        handler.parse();
        final Configuration config = handler.getConfiguration();
        final HandlerConfig hc = config.getAs(HandlerConfig.class);
        final List<HttpHandler> handlers = new ArrayList<>();
        for (Object h : hc.handlers) {
            if (h instanceof WebServiceHandler) {
                handlers.add(((WebServiceHandler)h).filters(filters));
            } else if (h instanceof HttpHandler) {
                handlers.add((HttpHandler)h);
                try {
                    Reflections.setFieldValue(h, "filters", filters);
                } catch (RuntimeException e) {}
            }
        }
        return handlers;
    }

    static List<Field> findAnnotatedFields(Class<?> type, Class<? extends Annotation> annotation) {
        return Reflections.getFieldStream(type)
                .filter(f -> f.isAnnotationPresent(annotation))
                .collect(Collectors.toList());
    }

    static void runScript(File file, Connection conn) throws IOException, SQLException {
        // Give the input file to Reader
        Reader reader = new BufferedReader(new FileReader(file));
        try {
            // Initialize object for ScripRunner
            ScriptRunner scriptRunner = new ScriptRunner(conn, true, true);
            // Exctute script
            scriptRunner.runScript(reader);
        } finally {
            reader.close();
            conn.close();
        }
    }

    static void runScript(String script, Connection conn) throws IOException, SQLException {
        // Give the input file to Reader
        Reader reader = new StringReader(script);
        try {
            // Initialize object for ScripRunner
            ScriptRunner scriptRunner = new ScriptRunner(conn, true, true);
            // Exctute script
            scriptRunner.runScript(reader);
        } finally {
            reader.close();
            conn.close();
        }
    }

    @Config("config")
    public static class HandlerConfig {
        List<WebServiceHandler> handlers;

    }
}
