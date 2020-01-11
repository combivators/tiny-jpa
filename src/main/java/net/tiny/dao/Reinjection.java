package net.tiny.dao;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.logging.Logger;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

public class Reinjection {

	private static Logger LOGGER = Logger.getLogger(Reinjection.class.getName());

	public <T> void reinject(Class<T> type, T entity) {
		Field[] fields = type.getDeclaredFields(); // Only this class
		for(Field field : fields) {
			if(isRelationshipField(field)) {
				//Is field with @OneToOne @OneToMany @ManyToOne @ManyToMany annotation
				injectInstance(type, entity, field);
			}
		}
	}

	<T> void injectInstance(Class<T> parentType, T parent, Field field)  {
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
		if(null == classType) {
			return;
		}

		Field[] fields = classType.getDeclaredFields(); // Only this class
		for(Field f : fields) {
			if(parentType.equals(f.getType())) { //Has a same type of parent
				//Set relationship field
				injectParent(parentType, parent, field, classType, f, listed);
			}
		}
	}

	<T> void injectParent(Class<T> parentType, T parent, Field field, Class<?> classType, Field targetField, boolean listType) {
		try {
			if(listType) {
				Method getter = findFieldMethod(field, parentType, true);
				Object target = getter.invoke(parent);
				if(null == target) {
					return;
				}
				Collection<?> list = (Collection<?>)target;
				for(Object obj : list) {
					getter = findFieldMethod(targetField, classType, true);
					Object value  = getter.invoke(obj);
					if(!parent.equals(value)) {
						Method setter = findFieldMethod(targetField, classType, false, parentType);
						setter.invoke(obj, parent);
						LOGGER.fine(String.format("'%1$s' was be injected  to '%2$s'.", parent.toString(), obj.toString()));
					}
				}
			} else {
				setFieldValue(parentType, parent, parent, field, classType, targetField);
			}
    	} catch (RuntimeException ex) {
    		throw ex;
    	} catch (Exception ex) {
			throw new IllegalArgumentException("Can not re-inject property class type '"
					+ classType.getName() +"' - " + parent.toString(), ex);
    	}
	}

	<T> void setFieldValue(Class<T> parentType, T parent, Object instance, Field field, Class<?> classType, Field targetField) throws Exception {
		Method getter = findFieldMethod(field, parentType, true);
		Object target = getter.invoke(instance);
		if(null == target) {
			return;
		}
		getter = findFieldMethod(targetField, classType, true);
		Object value  = getter.invoke(target);
		if(!parent.equals(value)) {
			Method setter = findFieldMethod(targetField, classType, false, parentType);
			setter.invoke(target, parent);
			LOGGER.fine(String.format("'%1$s' was be injected  to '%2$s'.", parent.toString(), target.toString()));
		}
	}


	/**
	 * This field with JPA annotation @OneToOne @OneToMany @ManyToOne @ManyToMany
	 * @param field
	 * @return
	 */
	boolean isRelationshipField(Field field) {
		return field.isAnnotationPresent(OneToMany.class) ||
				field.isAnnotationPresent(OneToOne.class)  ||
				field.isAnnotationPresent(ManyToOne.class)  ||
				field.isAnnotationPresent(ManyToMany.class);
	}

	/**
	 * Find a getter or setter method by a field
	 * @param field
	 * @param classType
	 * @param getter
	 * @param parameterTypes
	 * @return method
	 */
	Method findFieldMethod(Field field, Class<?> classType, boolean getter, Class<?>... parameterTypes) throws Exception {
		String fieldName = field.getName();
		String methodName;
		if(getter) {
			methodName = String.format("get%1$s%2$s",fieldName.substring(0, 1).toUpperCase(), fieldName.substring(1));
		} else {
			methodName = String.format("set%1$s%2$s",fieldName.substring(0, 1).toUpperCase(), fieldName.substring(1));
		}
		Method method = classType.getMethod(methodName, parameterTypes);
		if(method != null) {
			return method;
		}
		BeanInfo javaBean = Introspector.getBeanInfo(classType);
		PropertyDescriptor[] descriptors = javaBean.getPropertyDescriptors();
		for(PropertyDescriptor property : descriptors) {
			if(property.getName().equals(field.getName())) {
				if(getter) {
					return property.getReadMethod();
				} else {
					return property.getWriteMethod();
				}
			}
		}
		return null;
	}

}
