package net.tiny.validation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.constraints.Future;

public class FutureValidator implements ConstraintValidator<Future, Temporal> {

    @Override
    public void initialize(Future constraintAnnotation) {
    }

    @Override
    public boolean isValid(Temporal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if(value instanceof LocalDateTime) {
        	LocalDateTime lt = LocalDateTime.from(value);
        	return lt.isAfter(LocalDateTime.now());
        } else if(value instanceof LocalDate) {
        	LocalDate ld = LocalDate.from(value);
        	return ld.isAfter(LocalDate.now());
        }
        return false;
    }

}