package tigase.component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.kernel.BeanUtils;
import tigase.kernel.TypesConverter;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.BeanConfigurator;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.DependencyManager;
import tigase.kernel.core.Kernel;

@Bean(name = BeanConfigurator.DEFAULT_CONFIGURATOR_NAME)
public class PropertiesBeanConfigurator implements BeanConfigurator {

	protected final Logger log = Logger.getLogger(this.getClass().getName());

	private Map<String, Object> props;

	@Inject
	private Kernel kernel;

	private boolean accessToAllFields = false;

	@Override
	public void configure(BeanConfig beanConfig, Object bean) {
		if (props == null)
			return;

		if (log.isLoggable(Level.CONFIG))
			log.config("Configuring bean '" + beanConfig.getBeanName() + "'...");

		final HashMap<Field, Object> valuesToSet = new HashMap<>();

		// Preparing set of properties based on given properties set
		HashSet<String> beanProps = getBeanProps(beanConfig.getBeanName());
		for (String key : beanProps) {
			String[] tmp = key.split("/");
			final String property = tmp[1];
			final Object value = props.get(key);

			try {
				if (log.isLoggable(Level.FINEST))
					log.finest("Preparing property '" + property + "' of bean '" + beanConfig.getBeanName() + "'...");

				final Field field = BeanUtils.getField(beanConfig, property);
				if (field == null) {
					log.warning(
							"Field '" + property + "' does not exists in bean '" + beanConfig.getBeanName() + "'. Ignoring!");
					continue;
				}
				ConfigField cf = field.getAnnotation(ConfigField.class);
				if (!accessToAllFields && cf == null) {
					log.warning("Field '" + property + "' of bean '" + beanConfig.getBeanName()
							+ "' Can't be configured (missing @ConfigField). Ignoring!");
					continue;
				}

				final Object v = TypesConverter.convert(value, field.getType());

				valuesToSet.put(field, v);

			} catch (Exception e) {
				log.log(Level.WARNING, "Can't prepare value of property '" + property + "' of bean '" + beanConfig.getBeanName()
						+ "': '" + value + "'", e);
				throw new RuntimeException("Can't prepare value of property '" + property + "' of bean '"
						+ beanConfig.getBeanName() + "': '" + value + "'");
			}
		}

		// Preparing set of properties based on @ConfigField annotation and
		// aliases
		Field[] fields = DependencyManager.getAllFields(beanConfig.getClazz());
		for (Field field : fields) {
			final ConfigField cf = field.getAnnotation(ConfigField.class);
			if (cf != null && !cf.alias().isEmpty() && props.containsKey(cf.alias())) {
				final Object value = props.get(cf.alias());

				if (valuesToSet.containsKey(field)) {
					if (log.isLoggable(Level.CONFIG))
						log.config("Alias '" + cf.alias() + "' for property " + beanConfig.getBeanName() + "." + field.getName() + " will not be used, because there is configuration for this property already.");
					continue;
				}
				if (log.isLoggable(Level.CONFIG))
					log.config("Using alias '" + cf.alias() + "' for property " + beanConfig.getBeanName() + "." + field.getName());

				try {
					final Object v = TypesConverter.convert(value, field.getType());
					valuesToSet.put(field, v);
				} catch (Exception e) {
					log.log(Level.WARNING, "Can't prepare value of property '" + field.getName() + "' of bean '" + beanConfig.getBeanName()
							+ "': '" + value + "'", e);
					throw new RuntimeException("Can't prepare value of property '" + field.getName() + "' of bean '"
							+ beanConfig.getBeanName() + "': '" + value + "'");
				}
			}
		}

		for (Map.Entry<Field, Object> item : valuesToSet.entrySet()) {
			if (log.isLoggable(Level.FINEST))
				log.finest("Setting property '" + item.getKey().getName() + "' of bean '" + beanConfig.getBeanName() + "'...");
			try {
				BeanUtils.setValue(bean, item.getKey(), item.getValue());
				if (log.isLoggable(Level.FINEST))
					log.finest("Property '" + item.getKey().getName() + "' of bean '" + beanConfig.getBeanName()
							+ "' has been set to " + item.getValue());
			} catch (Exception e) {
				log.log(Level.WARNING, "Can't set property '" + item.getKey().getName() + "' of bean '"
						+ beanConfig.getBeanName() + "' with value '" + item.getValue() + "'", e);
				throw new RuntimeException("Can't set property '" + item.getKey().getName() + "' of bean '"
						+ beanConfig.getBeanName() + "' with value '" + item.getValue() + "'");
			}
		}

	}

	public Map<String, Object> getCurrentConfigurations() {
		HashMap<String, Object> result = new HashMap<>();

		for (BeanConfig bc : kernel.getDependencyManager().getBeanConfigs()) {
			final Object bean = kernel.getInstance(bc.getBeanName());
			final Class<?> cl = bc.getClazz();
			java.lang.reflect.Field[] fields = DependencyManager.getAllFields(cl);
			for (java.lang.reflect.Field field : fields) {
				final ConfigField cf = field.getAnnotation(ConfigField.class);
				if (cf != null) {
					String key = bc.getBeanName() + "/" + field.getName();
					try {
						Object currentValue = BeanUtils.getValue(bean, field);

						result.put(key, currentValue);
					} catch (IllegalAccessException | InvocationTargetException e) {
						e.printStackTrace();
					}
				}
			}
		}

		return result;
	}

	private HashSet<String> getBeanProps(String beanName) {
		HashSet<String> result = new HashSet<>();

		for (String pn : props.keySet()) {
			if (pn.startsWith(beanName + "/")) {
				result.add(pn);
			}
		}

		return result;
	}

	public Map<String, Object> getProperties() {
		return props;
	}

	public void setProperties(Map<String, Object> props) {
		this.props = props;

		String s = System.getProperty("bean-config-access-to-all");
		if (s != null && !s.isEmpty()) {
			this.accessToAllFields = TypesConverter.convert(s, boolean.class);
		}
	}

}
