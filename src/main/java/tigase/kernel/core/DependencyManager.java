package tigase.kernel.core;

import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.kernel.beans.Inject;
import tigase.kernel.core.BeanConfig.State;

public class DependencyManager {

	protected final Logger log = Logger.getLogger(this.getClass().getName());
	private final Map<String, BeanConfig> beanConfigs = new HashMap<String, BeanConfig>();
	private DependencyManager parent;
	/**
	 * if <code>true</code> then DependencyManager will throw exception if it
	 * can't create beanConfig. If <code>false</code> then
	 * {@link DependencyManager#createBeanConfig(Kernel, String, Class)} will
	 * return null instead of BeanConfig.
	 */
	private boolean throwExceptionIfCannotCreate = false;

	public static Field[] getAllFields(Class<?> klass) {
		List<Field> fields = new ArrayList<Field>();
		fields.addAll(Arrays.asList(klass.getDeclaredFields()));
		if (klass.getSuperclass() != null) {
			fields.addAll(Arrays.asList(getAllFields(klass.getSuperclass())));
		}
		return fields.toArray(new Field[] {});
	}

	public static boolean match(Dependency dependency, BeanConfig beanConfig) {
		if (dependency.getBeanName() != null) {
			return beanConfig.getBeanName().equals(dependency.getBeanName());
		} else if (dependency.getType() != null) {
			return dependency.getType().isAssignableFrom(beanConfig.getClazz());
		} else
			throw new RuntimeException("Unsupported dependecy type.");
	}

	protected BeanConfig createBeanConfig(final Kernel kernel, final String beanName, final Class<?> beanClass) {
		try {
			BeanConfig result = new BeanConfig(beanName, beanClass);
			result.setKernel(kernel);
			prepareDependencies(result);
			return result;
		} catch (java.lang.NoClassDefFoundError e) {
			log.log(Level.WARNING, "Cannot create bean config '" + beanName + "', type=" + beanClass.getName()
					+ ". Bean requires unknown class " + e.getMessage());

			if (throwExceptionIfCannotCreate) {
				throw e;
			} else {
				return null;
			}
		}
	}

	private Map<Field, Inject> createFieldsDependencyList(final Class<?> cls) {
		Map<Field, Inject> deps = new HashMap<Field, Inject>();
		for (Field field : getAllFields(cls)) {
			Inject injectAnnotation = field.getAnnotation(Inject.class);
			if (injectAnnotation != null) {
				deps.put(field, injectAnnotation);
			}
		}
		return deps;
	}

	public BeanConfig[] getBeanConfig(Dependency dependency) {
		ArrayList<BeanConfig> bcs = new ArrayList<BeanConfig>();
		if (this.parent != null && this.parent != this) {
			BeanConfig[] pds = this.parent.getBeanConfig(dependency);
			for (BeanConfig beanConfig : pds) {
				if (beanConfig != null && beanConfig.isExportable())
					bcs.add(beanConfig);
			}
		}
		if (dependency.getBeanName() != null) {
			BeanConfig b = beanConfigs.get(dependency.getBeanName());
			if (b != null)
				bcs.add(b);
			if (bcs.isEmpty())
				bcs.add(null);
		} else if (dependency.getType() != null) {
			bcs.addAll(getBeanConfigs(dependency.getType()));
		} else
			throw new RuntimeException("Unsupported dependecy type.");
		return bcs.toArray(new BeanConfig[] {});
	}

	public BeanConfig getBeanConfig(String beanName) {
		return this.beanConfigs.get(beanName);
	}

	public Collection<BeanConfig> getBeanConfigs() {
		return Collections.unmodifiableCollection(beanConfigs.values());
	}

	public List<BeanConfig> getBeanConfigs(Class<?> type) {
		return getBeanConfigs(type, true);
	}

	public List<BeanConfig> getBeanConfigs(final Class<?> type, final boolean allowNonExportable) {
		ArrayList<BeanConfig> result = new ArrayList<BeanConfig>();
		for (BeanConfig bc : beanConfigs.values()) {
			if (type.isAssignableFrom(bc.getClazz()) && (allowNonExportable || bc.isExportable())) {
				result.add(bc);
			}
		}
		return result;
	}

	public Collection<Dependency> getDependenciesTo(BeanConfig destination) {
		HashSet<Dependency> result = new HashSet<Dependency>();
		for (BeanConfig candidate : beanConfigs.values()) {
			for (Dependency dp : candidate.getFieldDependencies().values()) {
				List<BeanConfig> bcs = Arrays.asList(getBeanConfig(dp));
				if (bcs.contains(destination)) {
					result.add(dp);
				}
			}
		}
		return result;
	}

	public HashSet<BeanConfig> getDependentBeans(final BeanConfig beanConfig) {
		HashSet<BeanConfig> result = new HashSet<BeanConfig>();
		for (BeanConfig candidate : beanConfigs.values()) {
			for (Dependency dp : candidate.getFieldDependencies().values()) {
				List<BeanConfig> bcs = Arrays.asList(getBeanConfig(dp));
				if (bcs.contains(beanConfig)) {
					result.add(candidate);
				}
			}
		}
		return result;
	}

	DependencyManager getParent() {
		return parent;
	}

	void setParent(DependencyManager parent) {
		this.parent = parent;
	}

	public boolean isBeanClassRegistered(String beanName) {
		return beanConfigs.containsKey(beanName);
	}

	public boolean isThrowExceptionIfCannotCreate() {
		return throwExceptionIfCannotCreate;
	}

	public void setThrowExceptionIfCannotCreate(boolean throwExceptionIfCannotCreate) {
		this.throwExceptionIfCannotCreate = throwExceptionIfCannotCreate;
	}

	protected void prepareDependencies(BeanConfig beanConfig) {
		final String id = beanConfig.getBeanName();
		final Class<?> cls = beanConfig.getClazz();

		Map<Field, Inject> deps = createFieldsDependencyList(cls);
		for (Entry<Field, Inject> e : deps.entrySet()) {
			Dependency d = new Dependency(beanConfig);
			d.setField(e.getKey());
			d.setNullAllowed(e.getValue().nullAllowed());
			if (!e.getValue().bean().isEmpty()) {
				d.setBeanName(e.getValue().bean());
			} else if (e.getValue().type() != Inject.EMPTY.class) {
				d.setType(e.getValue().type());
			} else if (e.getKey().getType().isArray()) {
				d.setType(e.getKey().getType().getComponentType());
			} else {
				d.setType(e.getKey().getType());
			}

			beanConfig.getFieldDependencies().put(e.getKey(), d);
		}
	}

	void register(BeanConfig factoryBeanConfig) {
		beanConfigs.put(factoryBeanConfig.getBeanName(), factoryBeanConfig);
		factoryBeanConfig.setState(State.registered);

	}

	public BeanConfig unregister(String beanName) {
		return beanConfigs.remove(beanName);
	}

}
