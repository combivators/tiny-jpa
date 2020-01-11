package net.tiny.dao;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Transient;
import javax.validation.constraints.Pattern;

public class EntityParserTest {

    @Test
    public void testParseEntity() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("id", "12345");
        params.put("name", "Tom");
        params.put("value", "hoge");
        params.put("type", "ipv6");

        EntityParser parser = new EntityParser();
        TestBean1 bean = parser.parse(TestBean1.class, params);
        assertNotNull(bean);
        assertEquals(new Integer(12345), bean.getId());
        assertEquals("Tom", bean.getName());
        assertEquals("hoge", bean.getValue());
        assertEquals(BeanType.ipv6, bean.getType());
    }

    public static enum BeanType {
        ipv4,
        ipv6
    }

    public static abstract class BaseBean implements Serializable {
        @Transient
        private static final long serialVersionUID = 1L;

        private Integer id;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }
    }
    public static abstract class AbstractBean extends BaseBean {
        @Transient
        private static final long serialVersionUID = 1L;

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class TestBean1 extends AbstractBean {
        @Transient
        private static final long serialVersionUID = 1L;

        @Pattern(regexp = "\\b(?:value|hoge)\\b")
        private String value;
        private BeanType type;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public BeanType getType() {
            return type;
        }

        public void setType(BeanType type) {
            this.type = type;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("" + getId());
            sb.append("," + getName());
            sb.append("," + getValue());
            if(null != type)
                sb.append("," + type.name());
            return sb.toString();
        }
    }


}
