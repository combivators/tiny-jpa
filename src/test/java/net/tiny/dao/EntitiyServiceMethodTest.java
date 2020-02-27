package net.tiny.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class EntitiyServiceMethodTest {

    @Test
    public void testListRegex() throws Exception {
        String param = "list?size=10";
        assertTrue((Pattern.matches(EntityService.REQ_LIST_REGEX, param)));
        int pos = param.indexOf("=");
        int page = Integer.parseInt(param.substring(pos+1));
        assertEquals(10, page);

        assertFalse((Pattern.matches(EntityService.REQ_LIST_REGEX, "1212")));
        assertFalse((Pattern.matches(EntityService.REQ_LIST_REGEX, "list?size=ab")));
    }

    @Test
    public void testLoadEntityClasses() throws Exception {
        EntityService service = new EntityService();
        service.setEntities("net.tiny.dao.*");
        service.setPattern(".*/classes/, .*/test-classes/, .*/tiny-.*[.]jar");

        assertTrue(service.hasEntity("xx_log"));
        assertEquals(net.tiny.dao.test.entity.Log.class, service.getEntityType("xx_log"));
        assertEquals(Long.class, service.getKeyType("xx_log"));
    }
}
