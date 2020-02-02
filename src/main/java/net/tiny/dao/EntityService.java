package net.tiny.dao;

import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

import javax.persistence.EntityManager;

import com.sun.net.httpserver.HttpExchange;

import net.tiny.config.JsonParser;
import net.tiny.ws.BaseWebService;
import net.tiny.ws.HttpHandlerHelper;
import net.tiny.ws.RequestHelper;
import net.tiny.ws.ResponseHeaderHelper;

/**
 * JPA Entity web service
 *
 * GET    /v1/dao/entity/{id}
 * GET    /v1/dao/entity/list?size=9
 * POST   /v1/dao/entity
 * PUT    /v1/dao/entity/{id}
 * DELETE /v1/dao/entity/{id}
 *
 * @param <T>
 * @param <ID>
 */
public class EntityService<T, ID extends Serializable> extends BaseWebService {

    private String dao;
    private Class<?> daoType;

    public void setDao(String dao) {
        try {
            daoType = Class.forName(dao);
            if (!BaseDao.class.isAssignableFrom(daoType)) {
                throw new RuntimeException(String.format("'%s' is invaild dao class.", dao));
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(String.format("Not fount '%s' class.", dao));
        }
        this.dao = dao;
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
            he.sendResponseHeaders(status, NO_RESPONSE_LENGTH);
            return;
        }

        final BaseDao<T, ID> baseDao = createDao(he);
        final Class<T> entityType = baseDao.getEntityType();
        final Class<ID> keyType = baseDao.getKeyType();
        ID id;
        T entity = null;
        switch (method) {
        case GET:
            id = JsonParser.unmarshal(request.getParameter(0), keyType);
            entity = baseDao.find(id);

            if (null != entity) {
                final String response = JsonParser.marshal(entity);
                final byte[] rawResponse = response.getBytes(StandardCharsets.UTF_8);
                final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
                header.setContentType(MIME_TYPE.JSON);
                he.sendResponseHeaders(HttpURLConnection.HTTP_OK, rawResponse.length);
                he.getResponseBody().write(rawResponse);
            } else {
                he.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, NO_RESPONSE_LENGTH);
            }
            break;
        case POST:
            entity = (T)JsonParser.unmarshal(new String(request.getRequestContent()), entityType);
            baseDao.insert(entity);
            baseDao.flush();
            final String response = JsonParser.marshal(entity);
            final byte[] rawResponse = response.getBytes(StandardCharsets.UTF_8);
            final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
            header.setContentType(MIME_TYPE.JSON);
            he.sendResponseHeaders(HttpURLConnection.HTTP_CREATED, rawResponse.length);
            he.getResponseBody().write(rawResponse);
            break;
        case PUT:
            id = JsonParser.unmarshal(request.getParameter(0), keyType);
            entity = (T)JsonParser.unmarshal(new String(request.getRequestContent()), entityType);
            baseDao.update(entity);
            baseDao.flush();
            he.sendResponseHeaders(HttpURLConnection.HTTP_OK, NO_RESPONSE_LENGTH);
            break;
        case DELETE:
            id = JsonParser.unmarshal(request.getParameter(0), keyType);
            boolean ret = baseDao.deleteById(id);
            if (ret) {
                he.sendResponseHeaders(HttpURLConnection.HTTP_OK, NO_RESPONSE_LENGTH);
            } else {
                he.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, NO_RESPONSE_LENGTH);
            }
            break;
        default:
            he.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, NO_RESPONSE_LENGTH);
            break;
        }
    }

    private EntityManager getEntityManager(HttpExchange he) {
        final Object em = he.getAttribute(EntityManager.class.getName());
        if (null == em || !(em instanceof EntityManager) || dao == null || daoType == null) {
            throw new RuntimeException("Not setting a JPA http transaction filter.");
        }
        return (EntityManager)em;
    }

    @SuppressWarnings("unchecked")
    private BaseDao<T, ID> createDao(HttpExchange he) {
        try {
            final EntityManager em = getEntityManager(he);
            final BaseDao<T, ID> baseDao = (BaseDao<T, ID>)daoType.newInstance();
            baseDao.setEntityManager(em);
            return baseDao;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(String.format("Create '%s' instance error.", dao), e);
        }
    }

}
