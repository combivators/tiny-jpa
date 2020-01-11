package net.tiny.dao;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Transient;
import javax.validation.ConstraintViolation;
import javax.validation.constraints.Pattern;

public class SeparatedValuesTest {

    @Test
    public void testEscape() throws Exception {
        assertEquals("abc_12", SeparatedValues.escape("abc_12"));
        assertEquals("abc 12", SeparatedValues.escape("abc 12"));
        assertEquals("\"abc\"\"12\"", SeparatedValues.escape("abc\"12"));
        assertEquals("\"abc,12\"", SeparatedValues.escape("abc,12"));
        assertEquals("\"abc\t12\"", SeparatedValues.escape("abc\t12"));
        assertEquals("\"abc\r\n12\"", SeparatedValues.escape("abc\r\n12"));
    }

    @Test
    public void testUnescape() throws Exception {
        assertEquals("abc_12", SeparatedValues.unescape("abc_12"));
        assertEquals("abc 12", SeparatedValues.unescape("abc 12"));
        assertEquals("abc\"12", SeparatedValues.unescape("\"abc\"\"12\""));
        assertEquals("abc,12", SeparatedValues.unescape("\"abc,12\""));
        assertEquals("abc\t12", SeparatedValues.unescape("\"abc\t12\""));
        assertEquals("abc\r\n12", SeparatedValues.unescape("\"abc\r\n12\""));
    }
//"1","2013-07-14 00:03:51.0","2013-07-14 00:03:51.0",null,"中国","中国",",",null

    @Test
    public void testParseLine() throws Exception {
        SeparatedValues csv;
        String line;
        line = "\"1\",\"2013-07-14 00:03:51.0\",\"2013-07-14 00:03:51.0\",null,\"China\",\"中国\",\",\",null";
        csv = new SeparatedValues(line);
        assertEquals(line, csv.toString());
        assertEquals(8, csv.size());
        assertEquals("1", csv.get(0));
        assertEquals("China", csv.get(4));
        assertEquals(",", csv.get(6));
        assertEquals("null", csv.get(7));

        line = "\"2\",\"2013-07-14 00:03:51.0\",\"2013-07-14 00:03:51.0\",null,\"China Beijing\",\"北京市\",\",1,\",\"1\"";
        csv = new SeparatedValues(line);
        assertEquals(8, csv.size());
        assertEquals("2", csv.get(0));
        assertEquals("China Beijing", csv.get(4));
        assertEquals(",1,", csv.get(6));
        assertEquals("1", csv.get(7));
    }

    @Test
    public void testParseCsv() throws Exception {
        SeparatedValues csv;
        String line;
        line = "1,152,";
        csv = new SeparatedValues(line);
        assertEquals(line, csv.toString());
        assertEquals(3, csv.size());
        assertEquals("1", csv.get(0));
        assertEquals("152", csv.get(1));
        assertEquals("", csv.get(2));


        line = "ab,cd,ef,gh";
        csv = new SeparatedValues(line);
        assertEquals(4, csv.size());
        assertEquals("ab", csv.get(0));
        assertEquals("cd", csv.get(1));
        assertEquals("ef", csv.get(2));
        assertEquals("gh", csv.get(3));

        line = "";
        csv = new SeparatedValues(line);
        assertEquals(1, csv.size());
        assertEquals("", csv.get(0));

        line = ",";
        csv = new SeparatedValues(line);
        assertEquals(2, csv.size());
        assertEquals("", csv.get(0));
        assertEquals("", csv.get(1));

        line = ",,,";
        csv = new SeparatedValues(line);
        assertEquals(4, csv.size());
        assertEquals("", csv.get(0));
        assertEquals("", csv.get(1));
        assertEquals("", csv.get(3));

        line = "a";
        csv = new SeparatedValues(line);
        assertEquals(1, csv.size());
        assertEquals("a", csv.get(0));

        line = "a,";
        csv = new SeparatedValues(line);
        assertEquals(2, csv.size());
        assertEquals("a", csv.get(0));
        assertEquals("", csv.get(1));


        line = "a,b";
        csv = new SeparatedValues(line);
        assertEquals(2, csv.size());
        assertEquals("a", csv.get(0));
        assertEquals("b", csv.get(1));

        line = "a,b,";
        csv = new SeparatedValues(line);
        assertEquals(3, csv.size());
        assertEquals("a", csv.get(0));
        assertEquals("b", csv.get(1));
        assertEquals("", csv.get(2));

        line = "a,,c";
        csv = new SeparatedValues(line);
        assertEquals(3, csv.size());
        assertEquals("a", csv.get(0));
        assertEquals("", csv.get(1));
        assertEquals("c", csv.get(2));

        line = "null,\"cd\",,12";
        csv = new SeparatedValues(line);
        assertEquals(4, csv.size());
        assertEquals("null", csv.get(0));
        assertEquals("cd", csv.get(1));
        assertEquals("", csv.get(2));
        assertEquals("12", csv.get(3));

        line = "ab,\"c\r\nd\",,12";
        csv = new SeparatedValues(line);
        assertEquals(4, csv.size());
        assertEquals("ab", csv.get(0));
        assertEquals("c\r\nd", csv.get(1));
        assertEquals("", csv.get(2));
        assertEquals("12", csv.get(3));

        line = "ab,\"c,d\",,12";
        csv = new SeparatedValues(line);
        assertEquals(4, csv.size());
        assertEquals("ab", csv.get(0));
        assertEquals("c,d", csv.get(1));
        assertEquals("", csv.get(2));
        assertEquals("12", csv.get(3));

        line = "foo,\"bar,baz\"";
        String[] array = SeparatedValues.csv(line);
        assertEquals(2, array.length);
        assertEquals("foo", array[0]);
        assertEquals("bar,baz", array[1]);

        line = "foo,\"bar\\\"baz\",ab";
        array = SeparatedValues.csv(line);
        assertEquals(3, array.length);
        assertEquals("foo", array[0]);
        assertEquals("bar\"baz", array[1]);
        assertEquals("ab", array[2]);

    }

    @Test
    public void testParseTsv() throws Exception {
        String line = "foo\t\"bar\tbaz\"";
        String[] array = SeparatedValues.tsv(line);
        assertEquals(2, array.length);
        assertEquals("foo", array[0]);
        assertEquals("bar\tbaz", array[1]);
    }

    @Test
    public void testParseRecords() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("foo,hoge,\r\n");
        sb.append("foo,,\"bar,baz\"\r\n");
        sb.append("foo,\"bar\r\nbaz\",hoge\r\n");
        sb.append("foo,\"bar\\\"baz\",hoge\r\n");
        StringReader reader = new StringReader(sb.toString());
        Iterator<SeparatedValues> it = SeparatedValues.parse(reader, ',');
        assertTrue(it.hasNext());
        SeparatedValues list = it.next();
        assertEquals(3, list.size());
        assertEquals("foo", list.get(0));
        assertEquals("hoge", list.get(1));
        assertEquals("", list.get(2));

        assertTrue(it.hasNext());
        list = it.next();
        assertEquals(3, list.size());
        assertEquals("foo", list.get(0));
        assertEquals("", list.get(1));
        assertEquals("bar,baz", list.get(2));

        assertTrue(it.hasNext());
        list = it.next();
        assertEquals(3, list.size());
        assertEquals("foo", list.get(0));
        assertEquals("bar\nbaz", list.get(1));
        assertEquals("hoge", list.get(2));

        assertTrue(it.hasNext());
        list = it.next();
        assertEquals(3, list.size());
        assertEquals("foo", list.get(0));
        assertEquals("bar\"baz", list.get(1));
        assertEquals("hoge", list.get(2));
    }

    @Test
    public void testParseEntities() throws Exception {
        assertTrue(java.util.regex.Pattern.matches("\\b(?:value|hoge)\\b", "value"));
        assertTrue(java.util.regex.Pattern.matches("\\b(?:value|hoge)\\b", "hoge"));

        Class<?> classType = TestBean1.class;
        List<EntityParser.ColumnField> list = EntityParser.getColumnFields(classType);
        for(EntityParser.ColumnField f : list) {
            System.out.println(f.getField().getName());
        }
        System.out.println();

        StringBuilder sb = new StringBuilder();
        sb.append("id,name,value,type\r\n");
        sb.append("1,,hoge,\r\n");
        sb.append("2,\"bar,baz\",,\"ipv6\"\r\n");
        sb.append("3,\"bar\r\nbaz\",hoge,\"ipv4\"\r\n");
        sb.append("4,\"bar\\\"baz\",hoge,\"ipv4\"\r\n");
        StringReader reader = new StringReader(sb.toString());
        Map<String, Set<ConstraintViolation<?>>> error = new HashMap<>();
        String[] fields = new String[] {"id", "name", "value", "type"};
        Iterator<TestBean1> it = SeparatedIterator.parse(reader, TestBean1.class, SeparatedValues.Type.CSV, fields, 1, error);
        while(it.hasNext()) {
            System.out.println(" - '" + it.next().toString() + "'");
        }
        System.out.println();
        assertEquals(1, error.size());
        String errorLine = error.keySet().iterator().next();
        System.out.println(errorLine);
        Set<ConstraintViolation<?>> violations = error.get(errorLine);
        assertEquals(1, violations.size());
        ConstraintViolation<?> violation = violations.iterator().next();
        System.out.println(violation.getMessage());

        fields = new String[] {"id", "value", "name", "type"};
        reader = new StringReader(sb.toString());
        it = SeparatedIterator.parse(reader, TestBean1.class, SeparatedValues.Type.CSV, fields, 1, null);

        while(it.hasNext()) {
            System.out.println(" - '" + it.next().toString() + "'");
        }
        System.out.println();

        error = new HashMap<>();
        reader = new StringReader(sb.toString());
        it = SeparatedIterator.parse(reader, TestBean1.class, error);
        while(it.hasNext()) {
            System.out.println(" - '" + it.next().toString() + "'");
        }

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
