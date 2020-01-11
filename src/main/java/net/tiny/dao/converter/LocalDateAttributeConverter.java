package net.tiny.dao.converter;

import java.time.LocalDate;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class LocalDateAttributeConverter implements AttributeConverter<LocalDate, java.sql.Date> {
    @Override
    public java.sql.Date convertToDatabaseColumn(LocalDate localDate) {
    	return (localDate == null ? null : java.sql.Date.valueOf(localDate));
    }

    @Override
    public LocalDate convertToEntityAttribute(java.sql.Date sqlDate) {
    	return (sqlDate == null ? null : sqlDate.toLocalDate());
    }
}