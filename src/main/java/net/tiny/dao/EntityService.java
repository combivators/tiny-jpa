package net.tiny.dao;

import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

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

    private String entity;
    private Class<ID> keyType;
    private Class<T> entityType;

    @SuppressWarnings("unchecked")
    public void setEntity(String type) {
        entity = type;
        try {
            entityType = (Class<T>)Class.forName(entity);
            keyType = (Class<ID>)Long.class;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(String.format("Not fount '%s' entity class.", entity));
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
            he.sendResponseHeaders(status, NO_RESPONSE_LENGTH);
            return;
        }

        final IDao<T, ID> baseDao = createDao(he);
        final Class<T> entityType = baseDao.getEntityType();
        final Class<ID> keyType = baseDao.getKeyType();
        ID id;

        switch (method) {
        case GET:
            id = JsonParser.unmarshal(request.getParameter(0), keyType);
            Optional<T> entity = baseDao.find(id);
            if (entity.isPresent()) {
                final String response = JsonParser.marshal(entity.get());
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
            T obj = (T)JsonParser.unmarshal(new String(request.getRequestContent()), entityType);
            baseDao.insert(obj);
            baseDao.flush();
            final String response = JsonParser.marshal(obj);
            final byte[] rawResponse = response.getBytes(StandardCharsets.UTF_8);
            final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
            header.setContentType(MIME_TYPE.JSON);
            he.sendResponseHeaders(HttpURLConnection.HTTP_CREATED, rawResponse.length);
            he.getResponseBody().write(rawResponse);
            break;
        case PUT:
            id = JsonParser.unmarshal(request.getParameter(0), keyType);
            T t = (T)JsonParser.unmarshal(new String(request.getRequestContent()), entityType);
            baseDao.update(t);
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

    private IDao<T, ID> createDao(HttpExchange he) {
        final Object em = he.getAttribute(EntityManager.class.getName());
        if (null == em || !(em instanceof EntityManager)) {
            throw new RuntimeException("Not setting a JPA http transaction filter.");
        }
        if (keyType == null || entityType == null) {
            throw new RuntimeException("Not setting handler entity properties.");
        }
        return Dao.getDao((EntityManager)em, keyType, entityType);
    }

}
