package tigase.kernel.beans.config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import tigase.kernel.BeanUtils;
import tigase.kernel.KernelException;
import tigase.kernel.beans.Inject;
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.DependencyManager;
import tigase.kernel.core.Kernel;

public abstract class AbstractBeanConfigurator implements BeanConfigurator {

	private final ConcurrentHashMap<BeanConfig, HashMap<Field, Object>> defaultFieldValues = new ConcurrentHashMap<BeanConfig, HashMap<Field, Object>>();

	@Inject(nullAllowed = false)
	private Kernel kernel;

	@Override
	public void configure(BeanConfig beanConfig, Object bean) throws KernelException {
		try {
			Map<String, Object> ccc = getConfiguration(beanConfig);

			HashMap<Field, Object> defaultConfig = defaultFieldValues.get(beanConfig);
			if (defaultConfig == null) {
				defaultConfig = new HashMap<Field, Object>();
				defaultFieldValues.put(beanConfig, defaultConfig);
			}

			final Field[] fields = DependencyManager.getAllFields(beanConfig.getClazz());
			for (Field field : fields) {

				ConfigField configField = field.getAnnotation(ConfigField.class);

				if (configField == null) {
					continue;
				} else {
					Object currentValue = BeanUtils.getValue(bean, field);
					if (!defaultFieldValues.contains(field)) {
						defaultConfig.put(field, currentValue);
					}
				}

				if (!ccc.containsKey(field.getName()))
					continue;

				Object valueToSet = ccc.get(field.getName());

				Method setter = BeanUtils.prepareSetterMethod(field);
				if (setter != null) {
					setter.invoke(bean, valueToSet);
				} else {
					field.setAccessible(true);
					field.set(bean, valueToSet);
				}
			}
		} catch (Exception e) {
			throw new KernelException("Cannot inject configuration to bean " + beanConfig.getBeanName(), e);
		}
	}

	protected abstract Map<String, Object> getConfiguration(BeanConfig beanConfig);

	public Kernel getKernel() {
		return kernel;
	}

	public void restoreDefaults(String beanName) {
		BeanConfig beanConfig = kernel.getDependencyManager().getBeanConfig(beanName);
		Object bean = kernel.getInstance(beanName);

		try {
			HashMap<Field, Object> defaultConfig = defaultFieldValues.get(beanConfig);
			if (defaultConfig == null) {
				return;
			}

			final Field[] fields = DependencyManager.getAllFields(beanConfig.getClazz());
			for (Field field : fields) {

				ConfigField configField = field.getAnnotation(ConfigField.class);

				if (configField == null) {
					continue;
				}

				if (!defaultConfig.containsKey(field))
					continue;

				Object valueToSet = defaultConfig.get(field);

				Method setter = BeanUtils.prepareSetterMethod(field);
				if (setter != null) {
					setter.invoke(bean, valueToSet);
				} else {
					field.setAccessible(true);
					field.set(bean, valueToSet);
				}
			}
		} catch (Exception e) {
			throw new KernelException("Cannot inject configuration to bean " + beanConfig.getBeanName(), e);
		}

	}

	public void setKernel(Kernel kernel) {
		this.kernel = kernel;
	}

}
