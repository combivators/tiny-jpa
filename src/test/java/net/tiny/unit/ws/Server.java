package net.tiny.unit.ws;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Tag("Server")
@Test
@ExtendWith(ServerExtension.class)
public @interface Server {
    int web() default 18080;
    int backlog() default 2;
    int rdb() default 19001;
    String db()  default "test_h2";
    boolean clear() default true;
    boolean trace() default false;
    String persistence() default "";
    String unit() default "persistenceUnit";
    String config();
    String createScript() default "";
    String dropScript() default "";
    String[] before() default {};
    String[] after() default {};
    String imports() default "";
    String logging() default "logging.properties";
}
