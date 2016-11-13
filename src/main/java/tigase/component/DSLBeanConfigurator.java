/*
 * DSLBeanConfigurator.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.component;

import tigase.conf.ConfigWriter;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.*;
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.DependencyManager;
import tigase.kernel.core.Kernel;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by andrzej on 11.08.2016.
 */
@Bean(name = BeanConfigurator.DEFAULT_CONFIGURATOR_NAME)
public class DSLBeanConfigurator extends AbstractBeanConfigurator {

	private static final Logger log = Logger.getLogger(DSLBeanConfigurator.class.getCanonicalName());

	private Map<String, Object> props;

	@Override
	public Map<String, Object> getConfiguration(BeanConfig beanConfig) {
		if (props == null)
			return new HashMap<>();

		Map<String, String> aliassesToFields = getFieldAliasses(beanConfig);

		return getBeanConfigurationProperties(beanConfig, aliassesToFields);
	}

	protected Map<String, Object> getBeanConfigurationProperties(BeanConfig beanConfig, Map<String, String> aliasesToFields) {
		HashMap<String, Object> result = new HashMap<>();
		ArrayDeque<Kernel> kernels = new ArrayDeque<>();
		Kernel kernel = beanConfig.getKernel();
		while (kernel.getParent() != null && kernel != this.kernel) {
			kernels.push(kernel);
			kernel = kernel.getParent();
		}

		Queue<Map<String, Object>> configPath = new ArrayDeque<>();
		Map<String, Object> props = this.props;
		configPath.add(props);

		while((kernel = kernels.poll()) != null) {
			String name = kernel.getName();

			props = (Map<String, Object>) props.get(name);
			if (props == null) {
				configPath.offer(Collections.emptyMap());
				break;
			}

			configPath.offer(props);
		}

		if (!beanConfig.getBeanName().equals(beanConfig.getKernel().getName())) {
			if (props != null) {
				props = (Map<String, Object>) props.get(beanConfig.getBeanName());
				if (props != null) {
					configPath.offer(props);
				} else {
					configPath.offer(Collections.emptyMap());
				}
			}
		}

		while ((props = configPath.poll()) != null) {
			for (Map.Entry<String, Object> e : props.entrySet()) {
				if (configPath.isEmpty()) {
					String fieldName = aliasesToFields.get(e.getKey());
					if (fieldName != null) {
						result.put(fieldName, e.getValue());
					} else {
						result.put(e.getKey(), e.getValue());
					}

				} else {
					String fieldName = aliasesToFields.get(e.getKey());
					if (fieldName != null) {
						result.put(fieldName, e.getValue());
					}
				}
			}
		}

		result.put("name", beanConfig.getBeanName());

		return result;
	}

	protected Map<String, String> getFieldAliasses(BeanConfig beanConfig) {
		Map<String, String> configAliasses = new HashMap<>();
		Class<?> cls = beanConfig.getClazz();

		Field[] fields = DependencyManager.getAllFields(cls);
		for (Field field : fields) {
			ConfigField cf = field.getAnnotation(ConfigField.class);
			if (cf != null) {
				if (!cf.alias().isEmpty()) {
					configAliasses.put(cf.alias(), field.getName());
				}
			}
		}

		do {
			ConfigAliases ca = cls.getAnnotation(ConfigAliases.class);
			if (ca != null) {
				for (ConfigAlias a : ca.value()) {
					configAliasses.put(a.alias(), a.field());
				}
			} else {
				break;
			}
		} while ((cls = cls.getSuperclass()) != null);
		return configAliasses;
	}

	@Override
	protected Map<String, BeanDefinition> getBeanDefinitions(Map<String, Object> values) {
		Map<String, BeanDefinition> beanDefinitions = super.getBeanDefinitions(values);

		for (Map.Entry<String, Object> e : values.entrySet()) {
			if (e.getValue() instanceof BeanDefinition) {
				beanDefinitions.put(e.getKey(), (BeanDefinition) e.getValue());
			}
		}

		return beanDefinitions;
	}

	public Map<String, Object> getProperties() {
		return props;
	}

	public void setProperties(Map<String, Object> props) {
		this.props = props;
	}

	public void dumpConfiguration(File f) throws IOException {
		log.log(Level.WARNING, "Dumping full server configuration to: {0}", f);
		Map<String, Object> dump = new LinkedHashMap<>(props);
		dumpConfiguration(dump, kernel);

		new ConfigWriter().write(f, dump);
	}

	private void dumpConfiguration(Map<String, Object> dump, Kernel kernel) {
		List<BeanConfig> beansToDump = kernel.getDependencyManager().getBeanConfigs().stream()
				.filter(bc -> !Kernel.class.isAssignableFrom(bc.getClazz()) && !(bc instanceof Kernel.DelegatedBeanConfig))
				.collect(Collectors.toList());

		for(BeanConfig bc : beansToDump) {
			BeanDefinition forBean = getBeanDefinitionFromDump(dump, bc.getBeanName());

			if (forBean.getClazzName() == null) {
				forBean.setClazzName(bc.getClazz().getCanonicalName());
			}
			forBean.setActive(bc.getState() != BeanConfig.State.inactive);
			forBean.setExportable(bc.isExportable());

			try {
				Map<Field, Object> defaults = grabCurrentConfig(bc);
				Map<String, Object> cfg = bc.getState() != BeanConfig.State.initialized ? getConfiguration(bc) : null;
				if (defaults != null) {
					defaults.forEach((field,v) -> {
						ConfigField cf = field.getAnnotation(ConfigField.class);
//						if (forBean.containsKey(field.getName()) || (cf != null && !cf.alias().isEmpty() && forBean.containsKey(cf.alias()))) {
//							return;
//						}
						Object v1 = cfg.get(field.getName());
						if (v1 == null && cf != null && !cf.alias().isEmpty()) {
							v1 = cfg.get(cf.alias());
						}
						String prop = (cf == null || cf.alias().isEmpty()) ? field.getName() : cf.alias();
						forBean.put(prop, v1 == null ? v : v1);
					});
				}
			} catch (Exception ex) {
				log.log(Level.FINEST, "failed to retrieve default values for bean " + bc.getBeanName() + ", class = " + bc.getClazz(), ex);
			}
		}

		List<BeanConfig> kernelBeans = kernel.getDependencyManager().getBeanConfigs().stream()
				.filter(bc -> Kernel.class.isAssignableFrom(bc.getClazz()))
				.filter(bc -> bc.getState() == BeanConfig.State.initialized)
				.collect(Collectors.toList());
		for (BeanConfig bc : kernelBeans) {
			Kernel subkernel = kernel.getInstance(bc.getBeanName());
			if (subkernel == kernel)
				continue;
			BeanDefinition forKernel = getBeanDefinitionFromDump(dump, subkernel.getName());
			dumpConfiguration(forKernel, subkernel);
		}
	}

	private BeanDefinition getBeanDefinitionFromDump(Map<String, Object> dump, String name) {
		Map<String, Object> tmp = (Map<String, Object>) dump.get(name);

		if (tmp == null || (!(tmp instanceof BeanDefinition))) {
			BeanDefinition def = new BeanDefinition();
			def.setBeanName(name);
			if (tmp != null)
				def.putAll(tmp);
			dump.put(name, def);
			tmp = def;
		}

		return (BeanDefinition) tmp;
	}
}
