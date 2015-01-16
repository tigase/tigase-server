package tigase.kernel;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Kernel {

	static String prepareAccessorMainPartName(final String fieldName) {
		if (fieldName.length() == 1) {
			return fieldName.toUpperCase();
		}

		String r;
		if (Character.isUpperCase(fieldName.charAt(1))) {
			r = fieldName.substring(0, 1);
		} else {
			r = fieldName.substring(0, 1).toUpperCase();
		}

		r += fieldName.substring(1);

		return r;
	}

	private final Map<BeanConfig, Object> beanInstances = new HashMap<BeanConfig, Object>();

	private final DependencyManager dependencyManager = new DependencyManager();

	private boolean initialized = false;

	private void configureBean(final BeanConfig beanConfig, final Object bean,
			@SuppressWarnings("unchecked") final Map<BeanConfig, Object>... createdBeans) throws IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		for (Dependency de : beanConfig.getFieldDependencies().values()) {
			if ("ss".equals(de.getField().getName())) {
				System.out.println("! " + de.getField());
			}// BeanConfig depConfig = dependencyManager.getBeanConfig(de);
			Object[] depBeans = getInstance(de, createdBeans);

			inject(depBeans, de, bean);
		}
	}

	private Object createNewInstance(BeanConfig beanConfig) {
		try {
			Class<?> clz = beanConfig.getClazz();

			return clz.newInstance();
		} catch (Exception e) {
			throw new KernelException("Can't create instance of bean '" + beanConfig.getBeanName() + "'", e);
		}
	}

	DependencyManager getDependencyManager() {
		return dependencyManager;
	}

	@SuppressWarnings("unchecked")
	public <T> T getInstance(Class<T> beanClass) {
		if (!initialized)
			init();

		final List<BeanConfig> bcs = dependencyManager.getBeanConfigs(beanClass);

		if (bcs.size() > 1)
			throw new KernelException("Too many beans implemented given class.");
		else if (bcs.isEmpty())
			throw new KernelException("Can't find bean implementing given class.");

		Object result = beanInstances.get(bcs.get(0));

		return (T) result;
	}

	private Object[] getInstance(final Dependency dependency,
			@SuppressWarnings("unchecked") final Map<BeanConfig, Object>... createdBeans) {

		ArrayList<Object> result = new ArrayList<Object>();

		BeanConfig[] bcs = dependencyManager.getBeanConfig(dependency);
		for (BeanConfig bc : bcs) {
			for (Map<BeanConfig, Object> map : createdBeans) {
				if (map.containsKey(bc))
					result.add(map.get(bc));
			}
		}

		if (result.size() > 1 && dependency.getType() != null) {
			Object[] z = (Object[]) Array.newInstance(dependency.getType(), 1);
			return result.toArray(z);
		} else
			return result.toArray();
	}

	@SuppressWarnings("unchecked")
	public <T> T getInstance(String beanName) {
		if (!initialized)
			init();

		final BeanConfig bc = dependencyManager.getBeanConfig(beanName);
		if (bc == null)
			throw new KernelException("Unknown bean '" + beanName + "'.");

		Object result = beanInstances.get(bc);

		return (T) result;
	}

	public Collection<String> getNamesOf(Class<?> beanType) {
		ArrayList<String> result = new ArrayList<String>();
		List<BeanConfig> bcs = dependencyManager.getBeanConfigs(beanType);
		for (BeanConfig beanConfig : bcs) {
			result.add(beanConfig.getBeanName());
		}
		return Collections.unmodifiableCollection(result);
	}

	protected void init() {
		final Collection<BeanConfig> bconfigs = new ArrayList<BeanConfig>(dependencyManager.getBeanConfigs());
		Iterator<BeanConfig> it = bconfigs.iterator();
		while (it.hasNext()) {
			BeanConfig c = it.next();
			if (beanInstances.containsKey(c))
				it.remove();
		}
		init(bconfigs);
		initialized = true;
	}

	@SuppressWarnings("unchecked")
	protected void init(final Collection<BeanConfig> beanConfigs) {
		try {

			final Map<BeanConfig, Object> beans = new HashMap<BeanConfig, Object>();

			for (BeanConfig beanConfig : beanConfigs) {
				beans.put(beanConfig, createNewInstance(beanConfig));
			}

			for (Entry<BeanConfig, Object> be : beans.entrySet()) {
				configureBean(be.getKey(), be.getValue(), beans, beanInstances);
			}

			beanInstances.putAll(beans);

			for (Entry<BeanConfig, Object> be : beans.entrySet()) {
				if (be.getValue() instanceof Initializable) {
					((Initializable) be.getValue()).initialize();
				}
			}
		} catch (Exception e) {
			throw new KernelException(e);
		}
	}

	private void inject(Object data, Dependency dependency, Object toBean) throws IllegalAccessException,
	IllegalArgumentException, InvocationTargetException {

		if (data == null) {
			Method setter = prepareSetterMethod(dependency.getField());
			setter.invoke(toBean, (Object) null);
		} else if (Collection.class.isAssignableFrom(dependency.getField().getType())) {

		} else {
			Object o;
			if (data != null && dependency.getField().getType().equals(data.getClass())) {
				o = data;
			} else {
				int l = Array.getLength(data);
				if (l > 1)
					throw new KernelException("Can't put many objects to single field");
				if (l == 0)
					o = null;
				else
					o = Array.get(data, 0);
			}

			Method setter = prepareSetterMethod(dependency.getField());
			setter.invoke(toBean, o);
		}
	}

	private Method prepareSetterMethod(Field f) {
		String t = prepareAccessorMainPartName(f.getName());
		String sm;
		@SuppressWarnings("unused")
		String gm;
		if (f.getType().isPrimitive() && f.getType().equals(boolean.class)) {
			sm = "set" + t;
			gm = "is" + t;
		} else {
			sm = "set" + t;
			gm = "get" + t;
		}

		try {
			Method m = f.getDeclaringClass().getMethod(sm, f.getType());
			return m;
		} catch (NoSuchMethodException e) {
			throw new KernelException("Class " + f.getDeclaringClass().getName() + " has no setter of field " + f.getName(), e);
		}
	}

	public void registerBean(String beanName, Object bean) {
		BeanConfig bc = dependencyManager.registerBeanClass(beanName, bean.getClass());
		beanInstances.put(bc, bean);
	}

	public void registerBeanClass(String beanName, Class<?> beanClass) {
		BeanConfig bc = dependencyManager.registerBeanClass(beanName, beanClass);
		if (initialized)
			init(Collections.singleton(bc));
	}

	public void unregister(final String beanName) {
		BeanConfig removingBC = dependencyManager.getBeanConfig(beanName);
		Object i = beanInstances.remove(removingBC);

		if (i instanceof UnregisterAware) {
			try {
				((UnregisterAware) i).beforeUnregister();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		try {
			for (BeanConfig bc : dependencyManager.getBeanConfigs()) {
				Object ob = beanInstances.get(bc);
				for (Dependency d : bc.getFieldDependencies().values()) {
					BeanConfig[] cbcs = dependencyManager.getBeanConfig(d);
					if (cbcs.length == 1) {
						BeanConfig cbc = cbcs[0];
						if (cbc != null && cbc.equals(removingBC)) {
							inject(null, d, ob);
						}
					}
				}
			}
		} catch (Exception e) {
			throw new KernelException(e);
		}
	}
}
