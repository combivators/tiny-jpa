package net.tiny.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.jupiter.api.Test;

import net.tiny.ws.client.SimpleClient;

import net.tiny.unit.ws.Server;
import net.tiny.config.JsonParser;
import net.tiny.dao.test.entity.Log;

@Server(web=8080,rdb=9001,trace=true
  ,config="src/test/resources/config/test-handlers.yml"
  ,persistence="persistence-eclipselink.properties"
  ,db="h2"
  ,before= {"create sequence xx_log_sequence increment by 1 start with 0;"}
)
public class EntityServiceTest {

    @Test
    public void testGetPostPutDeleteEntity() throws Exception {
        int port = 8080;
        SimpleClient client = new SimpleClient.Builder().build();

        byte[] res = client.doGet(new URL("http://localhost:" + port +"/v1/dao/log/99"), callback -> {
            if(callback.success()) {
                fail("Ee? How did find it!");
            } else {
                assertEquals("404 Not Found", callback.cause().getMessage());
            }
        });
        assertEquals(0, res.length);

        //POST
        Log log = new Log();
        log.setContent("content");
        log.setOperation("用户设置");
        log.setOperator("user");
        log.setParameter("phone=88888888");
        log.setIp("192.168.80.180");
        String req = JsonParser.marshal(log);
        res = client.doPost(new URL("http://localhost:" + port +"/v1/dao/log"), req.getBytes(), callback -> {
            if(callback.success()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
                assertEquals("application/json; charset=utf-8", client.getHeader("Content-Type"));
            } else {
                fail(callback.cause().getMessage());
            }
        });
        log = JsonParser.unmarshal(new String(res), Log.class);
        assertEquals(1L, log.getId());
        Thread.sleep(100L);

        // GET
        String json = client.doGet("http://localhost:" + port +"/v1/dao/log/" + log.getId());
        System.out.println(json);
        assertTrue(json.contains("\"id\" : 1,"));

        // PUT
        client.request()
            .port(port)
            .path("/v1/dao/log/" + log.getId())
            .type(SimpleClient.MIME_TYPE_JSON)
            .accept("application/json")
            .doPut("{\"id\": 1, \"operation\":\"用户设置\",\"operator\":\"user\",\"parameter\":\"phone=12345678\",\"ip\":\"192.168.100.123\"}".getBytes(),
              callback -> {
                if(callback.success()) {
                  assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
              } else {
                  Throwable err = callback.cause();
                  fail(err.getMessage());
              }
          });

        // DELETE
        json = client.doDelete("http://localhost:" + port +"/v1/dao/log/" + log.getId());

        client.close();
    }

}
