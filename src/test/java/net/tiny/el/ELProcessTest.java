package net.tiny.el;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.ELClass;
import javax.el.ELProcessor;

/*
 * EL2.2 : JSR245 (On JavaEE6, Tomcat7)
 *    implement :
 *       org.glassfish.web  el-impl-2.2.jar
 *       com.sun.el           javax.el-2.2.6.jar
 * EL3.0 : JSR341 (JavaEE7, 2013/6Release)
 *       com.sun.el           javax.el-3.0.0.jar
 *
 */
public class ELProcessTest {

    @Test
    public void testEL3Expression() throws Exception {
        //Java EL3
        String expression = "quantity*price";
        ELProcessor elProc = ELParser.getELProcessor();
        elProc.defineBean("quantity", 15);
        elProc.defineBean("price", 25.5);
        Double result = (Double)elProc.eval(expression);
        assertEquals(382.5d,  result);

        expression = "x*y/3-1";
        elProc = ELParser.getELProcessor();
        elProc.defineBean("x", 15);
        elProc.defineBean("y", 25.5);
        result = (Double)elProc.eval(expression);
        assertEquals(126.5d,  result);

        ELProcessor el = ELParser.getELProcessor();
        assert ((Long)el.eval("a = [1, 2, 3]; a[1]")) == 2L;
        assert ((Long)(el.eval("((x,y) -> x+y)(4, 5)"))) == 9L;

        //el.getELManager().importStatic("com.company.Class.STATIC_FIELD");
        assert (el.eval("Math.random()") instanceof Double);
        for (int i = 0; i < 10; i++) {
            System.out.println("random: " + el.eval("Math.random()") );
        }
        //System.out.println(el.eval("STATIC_FIELD"));

        List<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }
        el.defineBean("list", list);
        List<?> processedList = (List<?>)el.eval("list.stream().filter( x -> x <= 3).map(x -> x * 5).toList()");
        assert Integer.valueOf(processedList.get(0).toString()) == list.get(0);
        assert Integer.valueOf(processedList.get(1).toString()) == list.get(5);
        assert Integer.valueOf(processedList.get(2).toString()) == list.get(10);
        assert Integer.valueOf(processedList.get(3).toString()) == list.get(15);
    }

    @Test
    public void testEL3() throws Exception {
        //Java EL3
        String expression = "#{(x -> x + 1)(10)}";
        Map<String, Object> args = new HashMap<String, Object>();
        //args.put("x", 15);
        long ret = ELParser.eval(expression, Long.class, args);
        assertEquals(11L, ret);

        args.clear();
        expression =  "#{((x,y) -> x + y)(3, 4)}";
        ret = ELParser.eval(expression, Long.class, args);
        assertEquals(7L, ret);

        args.clear();
        expression =  "#{[1,2,3,4].stream().sum()}";
        ret = ELParser.eval(expression, Long.class, args);
        assertEquals(10L, ret);

        args.clear();
        expression =  "#{fact = n -> n == 0 ? 1 : n * fact(n-1); fact(5)}";
        ret = ELParser.eval(expression, Long.class, args);
        assertEquals(120L, ret);
    }

    @Test
    public void testEL3All() throws Exception {
        //ELProcessor elProc = new ELProcessor();
        ELProcessor elProc = ELParser.getELProcessor();
        // 设置变量
        elProc.defineBean("foo", new BigDecimal("123"));
        elProc.defineBean("bar", "brabrabra");

        // 获取表达式的值getValue()
        String expression = "bar += '☆' += foo"; // 不需要使用${}或#{}
        String ret1 = (String)elProc.getValue(expression, String.class);
        System.out.println("ret=" + ret1);// ret=brabrabra☆123

        // 获取表达式的值eval()
        Number ret2 = (Number)elProc.eval("foo + 1");
        System.out.println("ret=" + ret2);// ret=124

        // 变量的嵌套
        elProc.setVariable("v1", "foo * 2");
        Number ret3 = (Number)elProc.eval("v1 + 1");
        System.out.println("ret=" + ret3);// ret=247

        Number ret4 = (Number)elProc.eval("v1 + foo");
        System.out.println("ret=" + ret4);// ret=369

        // 给不存在的变量设置值
        elProc.setValue("baz", "1234");
        Number ret5 = (Number)elProc.eval("baz + 1");
        System.out.println("ret=" + ret5);// ret=1235

        // 设置其他类型的变量
        elProc.eval("qux = [1,2,3,4]");
        elProc.eval("map = {'a': 111, 'b': 222}");
        System.out.println("qux=" + elProc.getValue("qux", Object.class));// qux=[1, 2, 3, 4]
        System.out.println("map=" + elProc.getValue("map", Object.class));// map={b=222, a=111}

        // 计算多个表达式
        Integer ret6 = (Integer) elProc.getValue("x=1;y=2;z=x+y", Integer.class);
        System.out.println("ret=" + ret6);// ret=3

        // 静态变量
        elProc.defineBean("Color", new ELClass(java.awt.Color.class));
        Color color = (Color) elProc.eval("Color.red");
        System.out.println("ret=" + color);// ret=java.awt.Color[r=255,g=0,b=0]

        // 默认导入了「java.lang.*」
        System.out.println("random=" + elProc.eval("Math.random()"));

        // 自定义函数
        Method method = MyClass.class.getMethod("strJoin", new Class[] {String.class, List.class});
        elProc.defineFunction("myFn", "join", method);
        Object ret = elProc.eval("myFn:join(',', ['aaa', 'bbb', 'ccc', 123])");
        System.out.println("ret=" + ret);// ret=aaa,bbb,ccc,123

        // 计算lambda表达式
        List<Integer> listdata = Arrays.asList(1, 2, 3, 4, 5, 6);
        elProc.defineBean("list", listdata);

        String lambda1 = "sum=0;list.stream().forEach(x->(sum=sum+x));sum";
        Number ret8 = (Number) elProc.eval(lambda1);
        System.out.println("ret=" + ret8.intValue());// ret=21

        String lambda1b = "list.stream().sum()";
        Number ret9 = (Number) elProc.eval(lambda1b);
        System.out.println("ret=" + ret9.intValue());// ret=21

        String lambda1c = "list.stream().reduce(0,(a,b)->a+b)";
        Number ret10 = (Number) elProc.eval(lambda1c);
        System.out.println("ret=" + ret10.intValue());// ret=21
    }

    @Test
    public void testCoustomFunc() throws Exception {
        ELProcessor elProc = ELParser.getELProcessor();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 自定义函数
        Method method = MyClass.class.getMethod("string", new Class[] {Date.class, String.class});
        elProc.defineFunction("", "string", method);
        elProc.defineBean("now", sdf.parse("2016-12-31 12:45:59"));
        Object ret = elProc.eval("string(now, 'yyyy-MM-dd HH:mm:ss')");
        System.out.println("ret=" + ret);
        assertEquals("2016-12-31 12:45:59", ret);
    }

    public static class MyClass {
        public static String strJoin(String sep, List<Object> args) {
            StringBuilder buf = new StringBuilder();
            if (args != null) {
                for (Object arg : args) {
                    if (buf.length() > 0) {
                        buf.append(sep);
                    }
                    buf.append(arg != null ? arg.toString() : "");
                }
            }
            return buf.toString();
        }

        public static String string(Date date, String pattern) {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            return sdf.format(date);
        }
    }
}
