package tigase.monitor;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.kernel.BeanUtils;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.core.DependencyManager;
import tigase.kernel.core.Kernel;

@Bean(name = BeanConfigurator.NAME)
public class BeanConfigurator {

	public static final String NAME = "bean-configurator";

	@Inject
	private Kernel kernel;

	protected final Logger log = Logger.getLogger(this.getClass().getName());

	public void configureBeans(final Map<String, Object> props) {
		Iterator<Entry<String, Object>> it = props.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Object> entry = it.next();

			if (!entry.getKey().contains("/")) {
				continue;
			}

			String[] tmp = entry.getKey().split("/");
			if (!kernel.isBeanClassRegistered(tmp[0])) {
				log.config("There is no bean '" + tmp[0] + "'.");
				continue;
			}
			try {
				final Object bean = kernel.getInstance(tmp[0]);
				BeanUtils.setValue(bean, tmp[1], entry.getValue());
				log.config("Property has set: " + tmp[0] + "." + tmp[1] + "=" + entry.getValue());
			} catch (Exception e) {
				log.log(Level.CONFIG, "Cannot set property " + tmp[1] + " of bean " + tmp[0], e);
			}
		}
	}

	private Field getField(String fieldName, Object bean) {
		Field[] fields = DependencyManager.getAllFields(bean.getClass());
		for (Field field : fields) {
			if (field.getName().equals(fieldName)) {
				return field;
			}
		}

		return null;
	}

}
