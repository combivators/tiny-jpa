package net.tiny.validation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.constraints.Past;

public class PastValidator implements ConstraintValidator<Past, Temporal> {

    @Override
    public void initialize(Past constraintAnnotation) {
    }

    @Override
    public boolean isValid(Temporal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if(value instanceof LocalDateTime) {
        	LocalDateTime lt = LocalDateTime.from(value);
        	return lt.isBefore(LocalDateTime.now());
        } else if(value instanceof LocalDate) {
        	LocalDate ld = LocalDate.from(value);
        	return ld.isBefore(LocalDate.now());
        }
        return false;
    }

}