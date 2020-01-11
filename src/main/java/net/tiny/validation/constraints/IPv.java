package net.tiny.validation.constraints;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;
import javax.validation.ReportAsSingleViolation;

import net.tiny.validation.InetAddressValidator;

@Documented
@ReportAsSingleViolation
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = InetAddressValidator.class)
public @interface IPv {
    public static enum Type {
        IPv4, IPv6, ALL
    }
    String message() default "{net.tiny.validation.IPv.message}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    Type type() default Type.ALL;
}