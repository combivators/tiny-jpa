package net.tiny.dao;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

public class EntityParser {

	private static final Logger LOGGER = Logger.getLogger(EntityParser.class.getName());

	private ColumnField[] columnFields;
	private boolean isNull = true;
	private Map<Alias, String> fieldAlias = new HashMap<>();

	public EntityParser() {
	}

	protected EntityParser(final Class<?> entityType) {
		List<ColumnField> list = getColumnFields(entityType);
		this.columnFields = list.toArray(new ColumnField[list.size()]);
	}

	protected EntityParser(Class<?> classType, String[] names, boolean isFieldName) {
		if(isFieldName) {
			this.columnFields = new ColumnField[names.length];
			for(int i=0; i<this.columnFields.length; i++) {
				try {
					this.columnFields[i] = getColumnField(classType, names[i]);
				} catch (NoSuchFieldException ex) {
					throw new IllegalArgumentException(ex.getMessage(), ex);
				}
			}
		} else {
			this.columnFields = findColumnFields(classType, names);
		}
	}

	protected EntityParser(Class<?> classType, String[] columns) {
		this(classType, columns, false);
	}

	protected <T> T parse(Class<T> type, List<String> params, Validator validator) throws Exception {
		if( params.size() != this.columnFields.length) {
			String msg = String.format("Illegal field size (%1$d) not equals column size (%2$d).", params.size(), this.columnFields.length);
			throw new IllegalArgumentException(msg);
		}
		String[] values = params.toArray(new String[params.size()]);
		return parseEntity(type, this.columnFields, values, validator);
	}

	public <T> T parse(Class<T> type, Map<String, String> params) throws Exception {
		return parse(type, params, null);
	}

	public <T> T parse(Class<T> type, Map<String, String> params, Validator validator) throws Exception {
		List<ColumnField> list = getColumnFields(type);
		ColumnField[] fields = list.toArray(new ColumnField[list.size()]);
		String[] values = new String[fields.length];
		for(int i=0; i < fields.length; i++) {
			String key = fields[i].getField().getName();
			if(params.containsKey(key)) {
				values[i] = params.get(key);
			} else {
				values[i] = null;
			}
		}
		return parseEntity(type, fields, values, validator);
	}

	protected <T> T parseEntity(Class<T> type, ColumnField[] fields, String[] values, Validator validator) throws Exception {
		T entity = type.cast(type.newInstance());
		for(int i=0; i < fields.length; i++) {
			ColumnField field = fields[i];
			String value = values[i];
			setFieldValue(entity, field.field, value);
		}
		if(null != validator) {
	        Set<ConstraintViolation<T>> violations = validator.validate(entity);
	        if(!violations.isEmpty()) {
        		throw new ConstraintViolationException(violations);
	        }
		}
		return entity;
	}

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Object convert(String value, Class<?> type, boolean nullable) throws ParseException {
    	if(null == value)
    		return null;
		if(nullable && "null".equalsIgnoreCase(value)) {
			return null;
		}
        Object data = null;
		if(String.class.equals(type)) {
			data = value;
		} else if(char.class.equals(type)) {
			data = value.isEmpty() ? null : value.charAt(0);
		} else if(Character.class.equals(type)) {
			data = value.isEmpty() ? null : new Character(value.charAt(0));
		} else if(int.class.equals(type) || Integer.class.equals(type)) {
			data = Integer.parseInt(value);
		} else if(short.class.equals(type) || Short.class.equals(type)) {
			data = Long.parseLong(value);
		} else if(long.class.equals(type) || Long.class.equals(type)) {
			data = Long.parseLong(value);
		} else if(float.class.equals(type) || Float.class.equals(type)) {
			data = Float.parseFloat(value);
		} else if(double.class.equals(type) || Double.class.equals(type)) {
			data = Double.parseDouble(value);
		} else if(boolean.class.equals(type) || Boolean.class.equals(type)) {
			data = Boolean.parseBoolean(value);
		} else if(type.isArray() && type.isAssignableFrom(byte[].class)) {
			data = Base64.getDecoder().decode(value);
		} else if(BigDecimal.class.equals(type)) {
			data = new BigDecimal(value);
		} else if(BigInteger.class.equals(type)) {
			data = new BigInteger(value);
		} else if (type.isEnum()) {
			if(Pattern.matches("[0-9]+", value)) {
				// Digit value by Enum.value
				data = value.isEmpty() ? null : lookupEnum((Class<? extends Enum>)type, Integer.parseInt(value));
			} else {
				// String value by Enum.name
				data = value.isEmpty() ? null : type.cast(Enum.valueOf((Class<? extends Enum>)type, value));
			}
		} else if(java.util.Calendar.class.equals(type)) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(parseDate(value));
			data = calendar;
		} else if(java.time.LocalDate.class.equals(type)) {
			data = parseLocalDate(value);
		} else if(java.time.LocalDateTime.class.equals(type)) {
			data = parseLocalDateTime(value);
		} else if(java.time.LocalTime.class.equals(type)) {
			data = parseLocalTime(value);
		} else if(java.util.Date.class.equals(type)) {
			data = parseDate(value);
		} else if(java.sql.Date.class.equals(type)) {
			data = new java.sql.Date(parseDate(value).getTime());
		} else if(java.sql.Time.class.equals(type)) {
			data = new java.sql.Time(parseDate(value).getTime());
		} else if(java.sql.Timestamp.class.equals(type)) {
			data = new java.sql.Timestamp(parseDate(value).getTime());
		} else {
			throw new TypeNotPresentException(type.getName(), null);
		}
		return data;
    }

    private Object convert(String value, boolean nullable, Field field) throws Exception {
    	Class<?> type = field.getType();
    	try {
    		return convert(value, type, nullable);
    	} catch (TypeNotPresentException ex) {
    		return convertEntity(field, value);
    	}
    }

    private Object convertEntity(Field field, String value) throws Exception {
    	Class<?> type = field.getType();
    	Object data = type.newInstance();
		Alias alias = new Alias(type, field.getName());
		String key = this.fieldAlias.get(alias);
		if(null == key) {
			JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
			if(null != joinColumn) {
				key = joinColumn.referencedColumnName();
			}
			if(null == key || key.isEmpty()) {
				ColumnField cf = getIdColumnField(type);
				if(null != cf) {
					key = cf.getField().getName();
				}
			}
			if(null != key) {
				alias.alias = key;
				this.fieldAlias.put(alias, key);
			}
			LOGGER.fine(String.format("The field '%1$s' alias to '%2$s#%3s'", field.getName(), type.getName(), key));
		}
		if(null != key) {
			ColumnField f = getColumnField(type, key);
			setFieldValue(data, f.field, value);
		}
		return data;
    }

    protected void setFieldValue(Object bean, Field field, String value) throws Exception  {
    	if(bean == null || value == null) {
    		return;
    	}
        field.setAccessible(true);
       	Object data = convert(value, this.isNull, field);
		if(null != data) {
			field.set(bean, data);
		}
	}

	protected void setFieldMultivalue(Object bean, Field field, List<String> values) throws Exception  {
    	if(bean == null || values.isEmpty()) {
    		return;
    	}

		Class<?> classType = null;
		Type type = field.getGenericType();
		boolean listed = false;
		if(type instanceof Class) {
			 classType = field.getType();
		} else if(type instanceof ParameterizedType) {
			//Is List(Set) type
			ParameterizedType listType = (ParameterizedType) field.getGenericType();
			classType = (Class<?>) listType.getActualTypeArguments()[0];
			listed = true;
		}

	    field.setAccessible(true);
        Object data = null;
        if(listed && isRelationshipField(field)) {
            if(field.getType().equals(List.class)) {
            	List<Object> list = new ArrayList<>();
            	for(String value : values) {
            		list.add(convert(value, classType, field.getAnnotations()));
            	}
            	data = list;
            } else if(field.getType().equals(Set.class)) {
            	Set<Object> set = new HashSet<>();
            	for(String value : values) {
            		set.add(convert(value, classType, field.getAnnotations()));
            	}
             	data = set;
            }
        } else {
        	String value = values.get(0);
        	try {
        		data = convert(value, classType, this.isNull);
           	} catch (TypeNotPresentException ex) {
           		data = convertEntity(field, value);
           	}
        }
		if(null != data) {
			field.set(bean, data);
		}
	}

	private Object convert(String value, Class<?> classType, Annotation[] annotations) throws Exception {
    	try {
    		return convert(value, classType, this.isNull);
    	} catch (TypeNotPresentException ex) {
    		return convertEntity(value, classType, annotations);
    	}
	}

    private Object convertEntity(String value, Class<?> type, Annotation[] annotations) throws Exception {
    	Object data = type.newInstance();
    	String key = null;
    	for(Annotation annotation : annotations) {
    		if(annotation.annotationType().equals(JoinTable.class)) {
    			JoinTable joinTable = (JoinTable)annotation;
    			JoinColumn[] joinColumns = joinTable.inverseJoinColumns();
    			for(JoinColumn joinColumn : joinColumns) {
    				key = joinColumn.referencedColumnName();
    				ColumnField f = getColumnField(type, key);
    				setFieldValue(data, f.field, value);
    			}
    		}
    	}
 		return data;
    }

	/**
	 * This field with JPA annotation @OneToOne @OneToMany @ManyToOne @ManyToMany
	 * @param field
	 * @return
	 */
	private boolean isRelationshipField(Field field) {
		return field.isAnnotationPresent(OneToMany.class) ||
				field.isAnnotationPresent(OneToOne.class)  ||
				field.isAnnotationPresent(ManyToOne.class)  ||
				field.isAnnotationPresent(ManyToMany.class);
	}

	public static ColumnField[] findColumnFields(Class<?> classType, String[] columns) {
		List<ColumnField> list = getColumnFields(classType);
		Map<String, ColumnField> index = new HashMap<>();
		for(ColumnField field : list) {
			index.put(field.getField().getName(), field);
		}
		ColumnField[] fields = new ColumnField[columns.length];
		for(int i=0; i<columns.length; i++) {
			columns[i] = columns[i].trim();
			//Step.1 Find @Column(name = "column_name")
			fields[i] = findFieldByColumn(list, columns[i]);
			if(null != fields[i]) {
				continue;
			}
			//Step.2. Find this class and superclass field name
			String name = columnToField(columns[i]);
			fields[i] = index.get(name);
			if(null != fields[i]) {
				continue;
			}
			//Step.3 Find @Embedded or @JoinColumn
			fields[i] = findFieldByEmbedded(list, columns[i]);
			if(null == fields[i]) {
				throw new IllegalArgumentException(
						String.format("Can not found field by column '%1$s' on class '%2$s'.", columns[i], classType.getName()));
			}
		}
		return fields;
	}

	private static ColumnField findFieldByColumn(List<ColumnField> fields, String columnName) {
		for(ColumnField field : fields) {
			Column column = field.getField().getAnnotation(Column.class);
			if(null != column && columnName.equalsIgnoreCase(column.name())) {
				return field;
			}
		}
		return null;
	}

	// Convert column name 'ad_position' to 'adPosition'
	private static String columnToField(String column) {
		StringBuilder sb = new StringBuilder();
		boolean underline = false;
		for(char c : column.toCharArray()) {
			if(c == '_') {
				underline = true;
				continue;
			}
			if(underline) {
				sb.append(Character.toUpperCase(c));
				underline = false;
			} else {
				sb.append(Character.toLowerCase(c));
			}
		}
		return sb.toString();
	}

	private static ColumnField findFieldByEmbedded(List<ColumnField> fields, String columnName) {
		for(ColumnField field : fields) {
			Embedded embedded = field.getField().getAnnotation(Embedded.class);
			if(null != embedded) {
				ColumnField[] columFields = findColumnFields(field.getField().getType(), new String[] {columnName});
				if(null != columFields && columFields.length == 1) {
					return columFields[0];
				}
			}
			JoinColumn joinColumn = field.getField().getAnnotation(JoinColumn.class);
			if(null != joinColumn && columnName.equalsIgnoreCase(joinColumn.name())) {
				return field;
			}
		}
		return null;
	}

	private static ColumnField getColumnField(Class<?> classType, String name) throws NoSuchFieldException {
		try {
			Field field = classType.getDeclaredField(name);
			return new ColumnField(classType, field);
		} catch (NoSuchFieldException ex) {
			Class<?> type = classType.getSuperclass();
			if(null == type) {
				throw ex;
			}
			return getColumnField(type, name);
		}
	}

	public static List<ColumnField> getColumnFields(Class<?> classType) {
		List<ColumnField> list = new ArrayList<>();
		Class<?> type = classType.getSuperclass();
		if(null != type) {
			list = getColumnFields(type);
		}
		Field[] fields = classType.getDeclaredFields();
		if(fields != null) {
			for(Field field : fields) {
				if((field.getModifiers() & Modifier.STATIC) != 0) {
					continue;
				}
				if((field.getModifiers() & Modifier.TRANSIENT) != 0) {
					continue;
				}
				list.add(new ColumnField(classType, field));
			}
		}
		return list;
	}

	protected static ColumnField getIdColumnField(Class<?> classType) {
		List<ColumnField> fields = getColumnFields(classType);
		for(ColumnField field : fields) {
			Id id = field.getField().getAnnotation(Id.class);
			if(null != id) {
				return field;
			}
		}
		return null;
	}

    /**
     * 获取枚举
     *
     * @param classType 枚举类
     * @param ordinal 输入参数
     * @return 枚举
     */
	public static <E extends Enum<E> > E lookupEnum(Class<E> classType, int ordinal) {
	    EnumSet<E> set = EnumSet.allOf(classType);
	    if (ordinal < set.size()) {
	        Iterator<E> iter = set.iterator();
	        for (int i = 0; i < ordinal; i++) {
	            iter.next();
	        }
	        E rval = iter.next();
	        assert(rval.ordinal() == ordinal);
	        return rval;
	    }
	    throw new IllegalArgumentException("Invalid value " + ordinal + " for " + classType.getName( ) + ", must be < " + set.size());
	}

    /** 日期格式配比 */
    static final String[] DATE_PATTERNS =
    		new String[] {
    				"yyyy-MM-dd",
    				"yyyy/MM/dd",
    				"yyyyMMdd",
    				"yyyy-MM-dd HH:mm:ss.S",
    				"yyyy-MM-dd HH:mm:ss",
    				"yyyy/MM/dd HH:mm:ss",
    				"yyyyMMddHHmmss",
    				"yyyy-MM-dd'T'HH:mm:ss.SSSX" };

    /**
     * 获取日期
     *
     * @param parameter
     *          输入参数
     * @return 日期
     */
    static LocalDateTime parseLocalDateTime(String parameter) throws DateTimeParseException {
    	DateTimeParseException error = null;
        for(String pattern : DATE_PATTERNS) {
        	DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            try {
                return LocalDateTime.parse(parameter, formatter);
            } catch (DateTimeParseException ex) {
                error = ex;
                continue;
            }
        }
        throw error;
    }

    static LocalDate parseLocalDate(String parameter) throws DateTimeParseException {
    	DateTimeParseException error = null;
        for(String pattern : DATE_PATTERNS) {
        	DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            try {
                return LocalDate.parse(parameter, formatter);
            } catch (DateTimeParseException ex) {
                error = ex;
                continue;
            }
        }
        throw error;
    }

    static LocalTime parseLocalTime(String parameter) throws DateTimeParseException {
    	DateTimeParseException error = null;
        for(String pattern : DATE_PATTERNS) {
        	DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            try {
                return LocalTime.parse(parameter, formatter);
            } catch (DateTimeParseException ex) {
                error = ex;
                continue;
            }
        }
        throw error;
    }

    static Date parseDate(String parameter) throws ParseException {
        ParseException error = null;
        for(String pattern : DATE_PATTERNS) {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            try {
                return sdf.parse(parameter);
            } catch (ParseException ex) {
                error = ex;
                continue;
            }
        }
        throw error;
    }

	public static class ColumnField {
		final Class<?> type;
		final Field field;

		public ColumnField(Class<?> type, Field field) {
			this.type = type;
			this.field = field;
		}
		public Class<?> getType() {
			return type;
		}
		public Field getField() {
			return field;
		}
		@Override
		public String toString() {
			return String.format("%1$s#%2$s", type.getName(), field.getName());
		}
	}

	static class Alias implements Comparable<Alias> {
		final Class<?> type;
		final String field;
		String alias;

		public Alias(Class<?> type, String field) {
			this.type = type;
			this.field = field;
		}

		public String getAlias() {
			return alias;
		}

		@Override
		public int compareTo(Alias other) {
			int ret = type.getName().compareTo(other.type.getName());
			if(ret != 0) return ret;
			ret = field.compareTo(other.field);
			return ret;
		}

		@Override
		public boolean equals(Object other) {
			if(other instanceof Alias) {
				return hashCode() == other.hashCode();
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return type.hashCode() * 3 + field.hashCode() * 5;
		}
	}
}
