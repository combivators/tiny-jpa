package net.tiny.el;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import net.tiny.service.RuntimeVersion;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;

public class EL3Test {

    @Test
    public void testEL3Eval() throws Exception {
        assertTrue(ELParser.isSupportEL3());

        Map<String, Object> args = new HashMap<String, Object>();
        MyBean myBean = new MyBean();
        myBean.setX(1);
        myBean.setY(2);
        args.clear();
        args.put("foo", "FOO");
        args.put("bar", myBean);
        String expression = "hello, ${foo}! ${bar.x + 234}";
        String ret = ELParser.eval(expression, String.class, args);
        assertEquals("hello, FOO! 235",  ret);

        args.clear();
        args.put("x", 12);
        args.put("y", 5.0);
        expression = "${x*y/3-1}";
        Double retValue = ELParser.eval(expression, Double.class, args);
        assertEquals(19.0d,  retValue);

        args.clear();
        args.put("sys", System.getProperties());
        expression = "${sys['java.version']}";
        ret = ELParser.eval(expression, String.class, args);
        System.out.println("java.version=" + ret);

        args.clear();
        args.put("a", 15.1d);
        args.put("b", 20.2d);
        expression = "max=${Math:max(a,b)}, sin=${Math:sin(b)}";
        ret = ELParser.process(expression, args, Math.class);
        if (RuntimeVersion.higher(11)) {
            assertEquals("max=20.2, sin=0.9758205177669755", ret);
        } else {
            assertEquals("max=20, sin=0.9758205177669755", ret);
        }

        ResourceBundle res = ResourceBundle.getBundle(getClass().getSimpleName());
        args.clear();
        args.put("res", res);
        expression = "${res['foo.bar.baz']}";
        ret = ELParser.process(expression, args);
        assertEquals("EL2TestSample", ret);

        //call a function
        args.clear();
        args.put("name", "Dave");
        expression = "#{MyFunc:hello(name)}";
        ret = ELParser.process(expression, args, MyFunc.class);
        assertEquals("Hello Dave!", ret);
    }

    @Test
    public void testEL3Method() throws Exception {
        String expression = "#{member.msg}";
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("x", 15);
        Member member = new Member();
        member.setAmount(1000L);
        args.put("member", member);

        ELContext context = ELParser.createELContext(args);
        ExpressionFactory factory = ELParser.getExpressionFactory();
        MethodExpression me = factory.createMethodExpression(context, expression, String.class, new Class[]{String.class});
        Object ret  = me.invoke(context, new Object[] {"tom"});
        assertEquals("Hello tom" , ret.toString());
    }

    @Test
    public void testEL3FromatDate() throws Exception {
        String expression = "/product/content/${Format:string(createDate, 'yyyyMM')}/1234.html";
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("id", 1234L);
        args.put("createDate", new Date(System.currentTimeMillis()));
        String ret = ELParser.eval(expression, String.class, args, Format.class);
        assertTrue(ret.startsWith("/product/content/"));
        assertTrue(ret.endsWith("/1234.html"));
    }

    public static class MessageMethod {
        public static String message(String key) {
            return "Hello " + key + "!";
        }
    }

    public static class CurrencyMethod {
        public static String currency(double amount, boolean showSign, boolean showUnit) {
            StringBuilder sb = new StringBuilder();
            if(showSign) {
                sb.append("\\");
            }
            sb.append(amount);
            if(showUnit) {
                sb.append("EN");
            }
            return sb.toString();
        }
    }

    public static class Member {
        private long amount;
        public long getAmount() {
            return amount;
        }

        public void setAmount(long amount) {
            this.amount = amount;
        }

        public String msg(String name) {
            return "Hello " + name;
        }
    }

    public static class MyFunc {
        public static String hello(String name) {
            return "Hello " + name +"!";
        }
    }

    public static class Format {
        public static String string(Date date, String format) {
            //String format = "yyyy-MM-dd";
            SimpleDateFormat  sdf = new SimpleDateFormat(format);
            return sdf.format(date);
        }
    }

    public static class MyBean {
        private int x;
        private int y;
        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public void mes(String mes) {
           System.out.println("mes=" + mes);
        }

        @Override
        public String toString() {
            return "(x: " + x + ", y:" + y + ")";
        }
    }
}
