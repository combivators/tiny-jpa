package net.tiny.el;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.BeanNameELResolver;
import javax.el.BeanNameResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELManager;
import javax.el.ELProcessor;
import javax.el.ELResolver;
import javax.el.EvaluationListener;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.ResourceBundleELResolver;
import javax.el.StaticFieldELResolver;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

public final class ELParser {

	private static Logger LOGGER = Logger.getLogger(ELParser.class.getName());
	private static boolean checked = false;
	private static boolean SUPPORT_EL3 = false;
	public static boolean verbose = false;

	public static boolean isSupportEL3() {
		if(!checked) {
			try {
				Class.forName("javax.el.ELProcessor");
				SUPPORT_EL3 = true;
			} catch (ClassNotFoundException ex) {
				SUPPORT_EL3= false;
			}
			checked = true;
		}
		return SUPPORT_EL3;
	}

	public static ELManager getELManager() {
		ELManager manager = new ELManager();
		manager.setELContext(getELContext());
		return manager;
	}

	public static ExpressionFactory getExpressionFactory() {
		return ELManager.getExpressionFactory();
	}

	public static ELProcessor getELProcessor() {
		ELProcessor processor = new ELProcessor();
		processor.getELManager().setELContext(getELContext());
		return processor;
	}

	public static ELContext getELContext() {
		return new DefaultELContext();
	}

	private static void setVariable(final VariableMapper varMapper, final Map<String, Object> args) {
		for (String key : args.keySet()) {
			Object value = args.get(key);
			if(value != null) {
				ValueExpression ve = getExpressionFactory().createValueExpression(value, value.getClass());
				varMapper.setVariable(key,  ve);
			}
		}
	}

	private static void addFunctions(final FunctionMapper funcMapper, final Class<?>... functions) {
		  for(Class<?> function : functions) {
			  addFunction(funcMapper,  function.getSimpleName(), function);
		  }
	}

	private static void addFunctions(final FunctionMapper funcMapper, final List<Class<?>> staticFunctions, final List<Class<?>> functions) {
		if(null != staticFunctions && !staticFunctions.isEmpty()) {
			  for(Class<?> function : staticFunctions) {
				  addFunction(funcMapper,  "", function);
			  }
		}
		if(null != functions && !functions.isEmpty()) {
		  for(Class<?> function : functions) {
			  addFunction(funcMapper,  function.getSimpleName(), function);
		  }
		}
	}

	private static void addFunction(final FunctionMapper funcMapper, final String prefix,  final Class<?> functionClass) {
		for (Method method : functionClass.getMethods()) {
			if (Modifier.isStatic(method.getModifiers())) {
				funcMapper.mapFunction(prefix, method.getName(), method);
			}
		}
	}

	public static ELContext createELContext(final Map<String, Object> args, final List<Class<?>> staticFunctions, final List<Class<?>> functions) {
		ELContext context = getELContext();
		if (null != args && !args.isEmpty()) {
			setVariable(context.getVariableMapper(), args);
		}
		addFunctions(context.getFunctionMapper(), staticFunctions, functions);
		if(verbose) {
			context.addEvaluationListener(new ELParserListener());
		}
		return context;
	}

	public static ELContext createELContext(final Map<String, Object> args, final List<Class<?>> functions) {
		return createELContext(args, null, functions);
	}

	public static ELContext createELContext(final Map<String, Object> args, Class<?>... functions) {
		ELContext context = getELContext();
		if (null != args && !args.isEmpty()) {
			setVariable(context.getVariableMapper(), args);
		}
		addFunctions(context.getFunctionMapper(), functions);
		if(verbose) {
			context.addEvaluationListener(new ELParserListener());
		}
		return context;
	}

	public static String process(final String expression, final Map<String, Object> args) {
		return eval(expression, String.class, args, null, null);
	}

	public static String process(final String expression, final Map<String, Object> args, Class<?> ... functions) {
		return eval(expression, String.class, args, functions);
	}

	public static String process(final String expression, final Map<String, Object> args,  final List<Class<?>> staticFunctions, final List<Class<?>> functions) {
		return eval(expression, String.class, args, staticFunctions, functions);
	}

	public static <T> T eval(final String expression, final Class<T> expectedType, final Map<String, Object> args) {
		return eval(expression, expectedType, args, null, null);
	}

	public static <T> T eval(final String expression, final Class<T> expectedType, final Map<String, Object> args, Class<?> ... functions) {
        ELContext context = createELContext(args, functions);
		ValueExpression ve = getExpressionFactory().createValueExpression(context, expression, Object.class);
        return expectedType.cast(ve.getValue(context));
	}

	public static <T> T eval(final String expression, final Class<T> expectedType, final Map<String, Object> args,  final List<Class<?>> staticFunctions, final List<Class<?>> functions) {
		ELContext context = createELContext(args, staticFunctions, functions);
		ValueExpression ve = getExpressionFactory().createValueExpression(context, expression, expectedType);
		return expectedType.cast(ve.getValue(context));
	}

	static class DefaultELContext extends ELContext {
		final CompositeELResolver resolver = new CompositeELResolver();
		final VariableMapper varMapper =  new DefaultVariableMapper();
		final FunctionMapper funcMapper =  new DefaultFunctionMapper();

		public DefaultELContext() {
            resolver.add(getExpressionFactory().getStreamELResolver());
            resolver.add(new ResourceBundleELResolver());
            resolver.add(new StaticFieldELResolver());
            resolver.add(new MapELResolver());
            resolver.add(new ListELResolver());
            resolver.add(new ArrayELResolver());
            resolver.add(new BeanELResolver());
		    resolver.add(new BeanNameELResolver(new LocalBeanNameResolver()));
		}

		@Override
		public ELResolver getELResolver() {
			return resolver;
		}

		@Override
		public FunctionMapper getFunctionMapper() {
			return funcMapper;
		}

		@Override
		public VariableMapper getVariableMapper() {
			return varMapper;
		}
	}

	static class DefaultFunctionMapper extends FunctionMapper {
		private Map<String, Method> functionMap = new HashMap<String, Method>();

		public DefaultFunctionMapper() {
		}

		public DefaultFunctionMapper(Class<?>... functions) {
			addFunctions(functions);
		}

		public DefaultFunctionMapper(List<Class<?>> functions) {
			addFunctions(functions);
		}

		public DefaultFunctionMapper(String prefix, Class<?> functionClass) {
			addFunction(prefix, functionClass);
		}

		@Override
		public Method resolveFunction(String prefix, String localName) {
			String key = localName;
			if (null != prefix && !prefix.isEmpty()) {
				key = prefix + ":" + localName;
			}
			return functionMap.get(key);
		}

		@Override
		public void mapFunction(String prefix, String localName, Method method) {
			if (prefix == null || localName == null || method == null) {
				throw new NullPointerException();
			}
			int modifiers = method.getModifiers();
			if (!Modifier.isPublic(modifiers)) {
				throw new IllegalArgumentException("method not public");
			}
			if (!Modifier.isStatic(modifiers)) {
				throw new IllegalArgumentException("method not static");
			}
			Class<?> retType = method.getReturnType();
			if (retType == Void.TYPE) {
				throw new IllegalArgumentException("method returns void");
			}
			String key = localName;
			if (null != prefix && !prefix.isEmpty()) {
				key = prefix + ":" + localName;
			}
			functionMap.put(key, method);
		}

		public void addFunctions(List<Class<?>> functions) {
			for (Class<?> function : functions) {
				addFunction(function.getSimpleName(), function);
			}
		}

		public void addFunctions(Class<?>... functions) {
			for (Class<?> function : functions) {
				addFunction(function.getSimpleName(), function);
			}
		}

		public void addFunction(String prefix, Class<?> functionClass) {
			for (Method method : functionClass.getMethods()) {
				if (Modifier.isStatic(method.getModifiers())) {
					String name = method.getName();
					mapFunction(prefix, name, method);
				}
			}
		}
	}

	static class DefaultVariableMapper extends VariableMapper  {

		private Map<String, ValueExpression> variables = null;

		public ValueExpression resolveVariable(String variable) {
            if (variables == null) {
                return null;
            }
            return variables.get(variable);
		}

		public ValueExpression setVariable(String variable, ValueExpression expression) {
            if (variables == null) {
                variables = new HashMap<String, ValueExpression>();
            }
            ValueExpression prev = null;
            if (expression == null) {
                prev = variables.remove(variable);
            } else {
                prev = variables.put(variable, expression);
            }
            return prev;
		}
	}

	static class LocalBeanNameResolver extends BeanNameResolver {
	    /**
	     * A bean repository local to this context
	     */
	    private Map<String, Object> beans = new HashMap<String, Object>();

	    public LocalBeanNameResolver() {
	    	//TODO
	    }

        @Override
        public boolean isNameResolved(String beanName) {
            return beans.containsKey(beanName);
        }

        @Override
        public Object getBean(String beanName) {
            return beans.get(beanName);
        }

        @Override
        public void setBeanValue(String beanName, Object value) {
            beans.put(beanName, value);
        }

        @Override
        public boolean isReadOnly(String beanName) {
            return false;
        }

        @Override
        public boolean canCreateBean(String beanName) {
            return true;
        }
    }

	static class ELParserListener  extends EvaluationListener {
		@Override
		public void beforeEvaluation(ELContext context, String expression) {
			LOGGER.info("beforeEvaluation - context@" + context.hashCode() + "  "  + expression);
		}

	    @Override
	    public void afterEvaluation(ELContext context, String expression) {
	    	LOGGER.info("afterEvaluation - context@" + context.hashCode() + "  "  + expression);
	    }

	    @Override
	    public void propertyResolved(ELContext context, Object base, Object property) {
	    	LOGGER.info("propertyResolved - context@" + context.hashCode() + "  "  + base + "." + property);
	    }
	}
}
