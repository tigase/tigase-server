package tigase.monitor;

import tigase.kernel.BeanUtils;
import tigase.kernel.TypesConverter;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.Kernel;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = BeanConfigurator.NAME)
public class BeanConfigurator {

	public static final String NAME = "bean-configurator";

	protected final Logger log = Logger.getLogger(this.getClass().getName());

	@Inject
	private Kernel kernel;

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
				final BeanConfig bc = kernel.getDependencyManager().getBeanConfig(tmp[0]);
				final Object bean = kernel.getInstance(bc);
				String fieldName = tmp[1];
				Object value = entry.getValue();

				Field field = BeanUtils.getField(bc, fieldName);

				BeanUtils.setValue(bean, field, TypesConverter.convert(value, field.getType()));
				log.config("Property has set: " + tmp[0] + "." + tmp[1] + "=" + entry.getValue());
			} catch (Exception e) {
				log.log(Level.CONFIG, "Cannot set property " + tmp[1] + " of bean " + tmp[0], e);
			}
		}
	}

}
