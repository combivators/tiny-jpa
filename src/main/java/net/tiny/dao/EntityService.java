package net.tiny.dao;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Table;

import com.sun.net.httpserver.HttpExchange;

import net.tiny.config.JsonParser;
import net.tiny.service.ClassFinder;
import net.tiny.service.ClassHelper;
import net.tiny.service.Patterns;
import net.tiny.ws.BaseWebService;
import net.tiny.ws.HttpHandlerHelper;
import net.tiny.ws.RequestHelper;
import net.tiny.ws.ResponseHeaderHelper;

/**
 * JPA Entity web service
 *
 * GET    /dao/v1/{entity}/{id}
 * GET    /dao/v1/{entity}/list?size=99
 * GET    /dao/v1/{entity}/count
 * POST   /dao/v1/{entity}
 * PUT    /dao/v1/{entity}/{id}
 * DELETE /dao/v1/{entity}/{id}
 *
 * @param <T>
 * @param <ID>
 */
public class EntityService extends BaseWebService {

    private static Logger LOGGER = Logger.getLogger(EntityService.class.getName());

    static final String REQ_LIST_REGEX = "list[?]size=\\d+";

    private Level level = Level.FINE;
    private String entities = null;
    private String pattern = null;

    private transient Patterns patterns;
    //private transient Hashtable<String, Class<?>> entities;
    private transient Hashtable<String, Class<?>> index;

    public void setPattern(String p) {
        pattern = p;
        if (null != entities) {
            index = loadEntityClasses();
        }
    }

    public void setEntities(String e) {
        entities = e;
        patterns = Patterns.valueOf(entities);
        if (null != pattern) {
            index = loadEntityClasses();
        }
    }

    @Override
    protected void execute(HTTP_METHOD method, HttpExchange he) throws IOException {

        final RequestHelper request = HttpHandlerHelper.getRequestHelper(he);
        int status = HttpURLConnection.HTTP_OK;
        // Check http method and parameter
        switch (method) {
        case GET:
        case PUT:
        case DELETE:
            if (request.hasParameters()) {
                status = HttpURLConnection.HTTP_NOT_FOUND;
            }
            break;
        case POST:
            break;
        default:
            status = HttpURLConnection.HTTP_BAD_METHOD;
            break;
        }
        if (status != HttpURLConnection.HTTP_OK) {
            he.sendResponseHeaders(status, -1);
            return;
        }

        switch (method) {
        case GET:
            doGetEntity(he, request);
            break;
        case POST:
            doPostEntity(he, request);
            break;
        case PUT:
            doPutEntity(he, request);
            break;
        case DELETE:
            doDeleteEntity(he, request);
            break;
        default:
            he.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, -1);
            break;
        }
    }

    boolean hasEntity(String entity) {
        return index.containsKey(entity);
    }

    @SuppressWarnings("unchecked")
    <ID extends Serializable> Class<ID> getKeyType(String entity) {
        Class<?> entityClass = findEntityClass(entity);
        List<Field> withIdAnnotatedFields = ClassHelper.findAnnotatedFields(entityClass, javax.persistence.Id.class);
        Field field = withIdAnnotatedFields.get(0);
        return (Class<ID>) field.getType();
    }

    @SuppressWarnings("unchecked")
    <T> Class<T> getEntityType(String entity) {
        return (Class<T>)findEntityClass(entity);
    }

    private Class<?> findEntityClass(String table) {
        return index.get(table);
    }

    private Hashtable<String, Class<?>> loadEntityClasses() {
        ClassFinder.setLoggingLevel(level.getName());
        ClassFinder finder = new ClassFinder(pattern, new EntityFilter());
        List<Class<?>> list = finder.findAnnotatedClasses(javax.persistence.Entity.class);
        LOGGER.info(String.format("[JPA] Found %d entity classe(s) with pattern '%s'", list.size(), patterns.toString()));
        Hashtable<String, Class<?>> table = new Hashtable<>();
        for (Class<?> ec : list) {
            final Table t = ec.getAnnotation(Table.class);
            String tableName = (t != null) ? t.name() : ec.getSimpleName();
            table.put(tableName, ec);
        }
        if (table.isEmpty()) {
            LOGGER.warning("[JPA] Not found entity classe(s).");
        }
        return table;
    }


    private <T, ID extends Serializable> IDao<T, ID> createDao(HttpExchange he, Class<ID> keyType, Class<T> entityType) {
        final Object em = he.getAttribute(EntityManager.class.getName());
        if (null == em || !(em instanceof EntityManager)) {
            throw new RuntimeException("Not setting a JPA http transaction filter.");
        }
        if (keyType == null || entityType == null) {
            throw new RuntimeException("Not setting handler entity properties.");
        }
        return Dao.getDao((EntityManager)em, keyType, entityType);
    }

    private <T, ID extends Serializable> void doGetEntity(final HttpExchange he, final RequestHelper request) throws IOException {
        final String target = request.getParameter(0);
        if (!hasEntity(target)) {
            he.sendResponseHeaders(HttpURLConnection.HTTP_NOT_ACCEPTABLE, -1);
            return;
        }
        final String param = request.getParameter(1);
        final Class<ID> keyType = getKeyType(target);
        final Class<T> entityType = getEntityType(target);
        final IDao<T, ID> baseDao = createDao(he, keyType, entityType);
        String response = "";
        if ("count".equals(param)) {
            //Return entity count
            response = String.format("{\"count\":\"%d\"}", baseDao.count());
        } else
        if (Pattern.matches(REQ_LIST_REGEX, param)) {
            //Return entity list
            final int pos = param.indexOf("=");
            final long limit = Long.parseLong(param.substring(pos+1));
            if (limit>100L) {
                he.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, -1);
                return;
            }
            final List<T> list = baseDao.finds().limit(limit).collect(Collectors.toList());
            if (list.isEmpty()) {
                he.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
                return;
            }
            response = JsonParser.marshal(list);

        } else {
            //Return entity content
            final ID id = JsonParser.unmarshal(param, keyType);
            Optional<T> entity = baseDao.find(id);
            if (!entity.isPresent()) {
                he.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
                return;
            }
            response = JsonParser.marshal(entity.get());
        }
        final byte[] rawResponse = response.getBytes(StandardCharsets.UTF_8);
        final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
        header.setContentType(MIME_TYPE.JSON);
        he.sendResponseHeaders(HttpURLConnection.HTTP_OK, rawResponse.length);
        he.getResponseBody().write(rawResponse);
    }

    private <T, ID extends Serializable> void doPostEntity(final HttpExchange he, final RequestHelper request) throws IOException {
        final String target = request.getParameter(0);
        if (!hasEntity(target)) {
            he.sendResponseHeaders(HttpURLConnection.HTTP_NOT_ACCEPTABLE, -1);
            return;
        }
        final Class<ID> keyType = getKeyType(target);
        final Class<T> entityType = getEntityType(target);
        final IDao<T, ID> baseDao = createDao(he, keyType, entityType);
        T obj = (T)JsonParser.unmarshal(new String(request.getRequestContent()), entityType);
        baseDao.insert(obj);
        baseDao.flush();
        final String response = JsonParser.marshal(obj);
        final byte[] rawResponse = response.getBytes(StandardCharsets.UTF_8);
        final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
        header.setContentType(MIME_TYPE.JSON);
        he.sendResponseHeaders(HttpURLConnection.HTTP_CREATED, rawResponse.length);
        he.getResponseBody().write(rawResponse);
    }

    private <T, ID extends Serializable> void doPutEntity(final HttpExchange he, final RequestHelper request) throws IOException {
        final String target = request.getParameter(0);
        if (!hasEntity(target)) {
            he.sendResponseHeaders(HttpURLConnection.HTTP_NOT_ACCEPTABLE, -1);
            return;
        }
        final Class<ID> keyType = getKeyType(target);
        final Class<T> entityType = getEntityType(target);
        final IDao<T, ID> baseDao = createDao(he, keyType, entityType);
        ID id = JsonParser.unmarshal(request.getParameter(1), keyType);
        Optional<T> entity = baseDao.find(id);
        if (!entity.isPresent()) {
            he.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
            return;
        }
        T t = (T)JsonParser.unmarshal(new String(request.getRequestContent()), entityType);
        baseDao.update(t);
        baseDao.flush();
        he.sendResponseHeaders(HttpURLConnection.HTTP_OK, -1);
    }

    private <T, ID extends Serializable> void doDeleteEntity(final HttpExchange he, final RequestHelper request) throws IOException {
        final String target = request.getParameter(0);
        if (!hasEntity(target)) {
            he.sendResponseHeaders(HttpURLConnection.HTTP_NOT_ACCEPTABLE, -1);
            return;
        }
        final String param = request.getParameter(1);
        final Class<ID> keyType = getKeyType(target);
        final Class<T> entityType = getEntityType(target);
        final IDao<T, ID> baseDao = createDao(he, keyType, entityType);
        ID id = JsonParser.unmarshal(param, keyType);
        boolean ret = baseDao.deleteById(id);
        if (ret) {
            he.sendResponseHeaders(HttpURLConnection.HTTP_OK, -1);
        } else {
            he.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
        }
    }

    class EntityFilter implements ClassFinder.Filter {
        @Override
        public boolean isTarget(Class<?> targetClass) {
            return true;
        }

        @Override
        public boolean isTarget(String className) {
            return patterns.vaild(className);
        }
    }
}
