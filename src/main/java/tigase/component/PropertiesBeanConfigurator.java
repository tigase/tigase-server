package tigase.component;

import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.kernel.BeanUtils;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.BeanConfigurator;
import tigase.kernel.core.BeanConfig;

@Bean(name = BeanConfigurator.DEFAULT_CONFIGURATOR_NAME)
public class PropertiesBeanConfigurator implements BeanConfigurator {

	protected final Logger log = Logger.getLogger(this.getClass().getName());

	private Map<String, Object> props;

	@Override
	public void configure(BeanConfig beanConfig, Object bean) {
		if (props == null)
			return;

		HashSet<String> propertiesToSet = getBeanProps(beanConfig.getBeanName());

		for (String key : propertiesToSet) {
			String[] tmp = key.split("/");
			final String property = tmp[1];
			final Object value = props.get(key);

			try {
				BeanUtils.setValue(bean, property, value);
			} catch (Exception e) {
				e.printStackTrace();
				log.log(Level.WARNING, "Can't set property '" + property + "' of bean '" + beanConfig.getBeanName() + "'", e);
			}

		}
	}

	private HashSet<String> getBeanProps(String beanName) {
		HashSet<String> result = new HashSet<String>();

		for (String pn : props.keySet()) {
			if (pn.startsWith(beanName + "/")) {
				result.add(pn);
			}
		}

		return result;
	}

	public Map<String, Object> getProperties() {
		// TODO Auto-generated method stub
		return props;
	}

	public void setProperties(Map<String, Object> props) {
		this.props = props;
	}

}
