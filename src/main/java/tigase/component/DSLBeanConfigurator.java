/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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

import tigase.conf.ConfigHolder;
import tigase.conf.ConfigWriter;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.config.*;
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.DependencyManager;
import tigase.kernel.core.Kernel;
import tigase.osgi.ModulesManagerImpl;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by andrzej on 11.08.2016.
 */
@Bean(name = BeanConfigurator.DEFAULT_CONFIGURATOR_NAME, active = true)
public class DSLBeanConfigurator
		extends AbstractBeanConfigurator {

	private static final Logger log = Logger.getLogger(DSLBeanConfigurator.class.getCanonicalName());
	private ConfigHolder configHolder;

	private Map<String, Object> props;

	@Override
	public Map<String, Object> getConfiguration(BeanConfig beanConfig) {
		if (props == null) {
			return new HashMap<>();
		}

		Map<String, String> aliassesToFields = getFieldAliasses(beanConfig);

		return getBeanConfigurationProperties(beanConfig, aliassesToFields);
	}

	public Map<String, Object> getProperties() {
		return props;
	}

	public void setProperties(Map<String, Object> props) {
		this.props = props;
	}

	public ConfigHolder getConfigHolder() {
		return configHolder;
	}

	public void setConfigHolder(ConfigHolder config) {
		this.configHolder = config;
		setProperties(config.getProperties());
	}

	public void dumpConfiguration(File f) throws IOException {
		log.log(Level.WARNING, "Dumping full server configuration to: {0}", f);
		Map<String, Object> dump = new LinkedHashMap<>(props);
		dumpConfiguration(dump, kernel);

		new ConfigWriter().resolveVariables().write(f, dump);
	}

	public void dumpConfiguration(Writer writer) throws IOException {
		Map<String, Object> dump = new LinkedHashMap<>(props);
		dumpConfiguration(dump, kernel);

		new ConfigWriter().resolveVariables().write(writer, dump);
	}

	protected boolean hasDirectConfiguration(BeanConfig beanConfig) {
		ArrayDeque<String> kernels = getBeanConfigPath(beanConfig);
		Map<String, Object> result = props;

		String name;
		while (result != null && (name = kernels.poll()) != null) {
			Object r = result.get(name);
			if (r instanceof Map) {
				result = (Map<String, Object>) r;
			} else {
				result = null;
			}
		}

		return result != null;
	}

	protected Map<String, Object> getBeanConfigurationProperties(BeanConfig beanConfig,
																 Map<String, String> aliasesToFields) {
		HashMap<String, Object> result = new HashMap<>();
		ArrayDeque<String> path = getBeanConfigPath(beanConfig);
		Queue<Map<String, Object>> configPath = new ArrayDeque<>();
		Map<String, Object> props = this.props;
		configPath.add(props);

		String name;
		while ((name = path.poll()) != null) {
			props = (Map<String, Object>) props.get(name);
			if (props == null) {
				configPath.offer(Collections.emptyMap());
				break;
			}

			configPath.offer(props);
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

	private void dumpConfiguration(Map<String, Object> dump, Kernel kernel) {
		List<BeanConfig> beansToDump = kernel.getDependencyManager()
				.getBeanConfigs()
				.stream()
				.filter(bc -> !Kernel.class.isAssignableFrom(bc.getClazz()) &&
						!(bc instanceof Kernel.DelegatedBeanConfig))
				.collect(Collectors.toList());

		for (BeanConfig bc : beansToDump) {
			BeanDefinition forBean = getBeanDefinitionFromDump(dump, bc.getBeanName());

			if (forBean.getClazzName() == null) {
				forBean.setClazzName(bc.getClazz().getName());
			}
			forBean.setActive(bc.getState() != BeanConfig.State.inactive);
			forBean.setExportable(bc.isExportable());

			try {
				if (forBean.isActive()) {
					if (RegistrarBean.class.isAssignableFrom(bc.getClazz())) {
						Kernel subkernel = bc.getKernel();
						if (subkernel != kernel) {
							dumpConfiguration(forBean, subkernel);
						}
					}

					Object bean = bc.getKernel().getInstanceIfExistsOr(bc.getBeanName(), bc1 -> {
						try {
							return bc1.getClazz().newInstance();
						} catch (InstantiationException | IllegalAccessException e) {
							log.log(Level.FINEST, "failed to instantiate class for retrieval of default configuration");
						}
						return null;
					});

					Map<Field, Object> defaults = grabCurrentConfig(bean, bc.getBeanName());
					Map<String, Object> cfg =
							bc.getState() != BeanConfig.State.initialized ? getConfiguration(bc) : null;
					Set<String> validProps = new HashSet<>();
					if (defaults != null) {
						defaults.forEach((field, v) -> {
							ConfigField cf = field.getAnnotation(ConfigField.class);
//						if (forBean.containsKey(field.getName()) || (cf != null && !cf.alias().isEmpty() && forBean.containsKey(cf.alias()))) {
//							return;
//						}
							Object v1 = null;
							if (cfg != null) {
								v1 = cfg.get(field.getName());
								if (v1 == null && cf != null && !cf.alias().isEmpty()) {
									v1 = cfg.get(cf.alias());
								}
							}
							String prop = (cf == null || cf.alias().isEmpty()) ? field.getName() : cf.alias();
							forBean.put(prop, v1 == null ? v : v1);
							validProps.add(prop);
						});
					}
					new ArrayList<Map.Entry>(forBean.entrySet()).stream()
							.filter(e -> !validProps.contains(e.getKey()))
							.filter(e -> !(e.getValue() instanceof BeanDefinition))
							.map(e -> e.getKey())
							.forEach(forBean::remove);
					forBean.remove("name");
				} else {
					dumpConfigFromSubBeans(forBean, kernel);
				}
			} catch (Exception ex) {
				log.log(Level.FINEST,
						"failed to retrieve default values for bean " + bc.getBeanName() + ", class = " + bc.getClazz(),
						ex);
			}
		}

//		List<BeanConfig> kernelBeans = kernel.getDependencyManager().getBeanConfigs().stream()
//				.filter(bc -> Kernel.class.isAssignableFrom(bc.getClazz()))
//				.filter(bc -> bc.getState() == BeanConfig.State.initialized)
//				.collect(Collectors.toList());
//		for (BeanConfig bc : kernelBeans) {
//			Kernel subkernel = kernel.getInstance(bc.getBeanName());
//			if (subkernel == kernel)
//				continue;
//			----- here are added new entries!
//			BeanDefinition forKernel = getBeanDefinitionFromDump(dump, subkernel.getName());
//			----- and they are not removed here!
//			dumpConfiguration(forKernel, subkernel);
//		}
	}

	private void dumpConfigFromSubBeans(BeanDefinition beanDef, Kernel kernel) {
		try {
			Object bean = ModulesManagerImpl.getInstance().forName(beanDef.getClazzName()).newInstance();

			Map<String, Object> cfg = new HashMap<>(beanDef);

			Map<Field, Object> defaults = grabCurrentConfig(bean, beanDef.getBeanName());
			if (defaults != null) {
				Set<String> validProps = new HashSet<>();
				defaults.forEach((field, v) -> {
					ConfigField cf = field.getAnnotation(ConfigField.class);
					Object v1 = beanDef.remove(field.getName());
					if (v1 == null) {
						v1 = beanDef.remove(cf.alias());
					}
					String prop = (cf == null || cf.alias().isEmpty()) ? field.getName() : cf.alias();
					beanDef.put(prop, v1 == null ? v : v1);
					validProps.add(prop);
				});
				new ArrayList<Map.Entry>(beanDef.entrySet()).stream()
						.filter(e -> !validProps.contains(e.getKey()))
						.filter(e -> !(e.getValue() instanceof BeanDefinition))
						.map(e -> e.getKey())
						.forEach(beanDef::remove);
			}

			beanDef.remove("name");

			if (RegistrarBean.class.isAssignableFrom(bean.getClass())) {
				Kernel tmpKernel = new Kernel() {
//					@Override
//					protected BeanConfig registerBean(BeanConfig beanConfig, BeanConfig factoryBeanConfig,
//													  Object beanInstance) {
//						return null;
//					}

					@Override
					public void registerLinks(String beanName) {
					}

					@Override
					public <T> T getInstance(String beanName) {
						return null;
					}

					@Override
					protected <T> T getInstance(Class<T> beanClass, boolean allowNonExportable) {
						if (AbstractBeanConfigurator.class.isAssignableFrom(beanClass)) {
							return (T) DSLBeanConfigurator.this;
						}
						return null;
					}

					@Override
					protected void injectIfRequired(BeanConfig beanConfig) {
					}
				};
				tmpKernel.registerBean(AbstractBeanConfigurator.DEFAULT_CONFIGURATOR_NAME)
						.asInstance(this)
						.exportable()
						.exec();
				if (bean instanceof RegistrarBean) {
					((RegistrarBean) bean).register(tmpKernel);
				}

				final Map<String, Class<?>> subbeans = new HashMap<>();
				tmpKernel.getDependencyManager()
						.getBeanConfigs()
						.stream()
						.filter(bc -> !Kernel.class.isAssignableFrom(bc.getClazz()))
						.forEach(bc -> subbeans.put(bc.getBeanName(), bc.getClazz()));
				subbeans.putAll(getBeanClassesFromAnnotations(kernel, bean.getClass()));
				Map<String, BeanDefinition> beansFromConfig = mergeWithBeansPropertyValue(getBeanDefinitions(beanDef),
																						  beanDef);
				subbeans.entrySet().stream().filter(e -> !beansFromConfig.containsKey(e.getKey())).map(e -> {
					BeanDefinition def = new BeanDefinition();
					def.setBeanName(e.getKey());
					def.setClazzName(e.getValue().getName());

					Bean b = e.getValue().getAnnotation(Bean.class);
					if (b != null) {
						def.setActive(b.active());
					}

					Object tmp = beanDef.get(def.getBeanName());
					cfg.entrySet()
							.stream()
							.filter(x -> !(x.getValue() instanceof BeanDefinition))
							.forEach(x -> def.put(x.getKey(), x.getValue()));

					if (tmp != null && tmp instanceof Map) {
						def.putAll((Map<String, Object>) tmp);
					}
					beanDef.put(def.getBeanName(), def);
					return def;
				}).forEach(def -> {
					dumpConfigFromSubBeans(def, kernel);
				});
				beansFromConfig.values().stream().map(def -> {
					if (def.getClazzName() == null) {
						Class x = subbeans.get(def.getBeanName());
						if (x != null) {
							def.setClazzName(x.getName());
						}
					}
					return def;
				}).forEach(def -> dumpConfigFromSubBeans(def, kernel));
			}

		} catch (Exception ex) {
			log.log(Level.FINEST, "exception retrieving configuration of subbeans = " + beanDef.getBeanName(), ex);
		}
	}

	private BeanDefinition getBeanDefinitionFromDump(Map<String, Object> dump, String name) {
		Object tmp = dump.get(name);

		if (tmp == null || (!(tmp instanceof BeanDefinition))) {
			BeanDefinition def = new BeanDefinition();
			def.setBeanName(name);
			dump.entrySet()
					.stream()
					.filter(e -> !(e.getValue() instanceof BeanDefinition))
					.forEach(e -> def.putIfAbsent(e.getKey(), e.getValue()));
			if (tmp != null && tmp instanceof Map) {
				def.putAll((Map<String, Object>) tmp);
			}
			dump.put(name, def);
			tmp = def;
		} else {
			BeanDefinition def = new BeanDefinition((BeanDefinition) tmp);
			dump.entrySet()
					.stream()
					.filter(e -> !(e.getValue() instanceof BeanDefinition))
					.forEach(e -> def.putIfAbsent(e.getKey(), e.getValue()));
			dump.put(name, def);
			tmp = def;
		}

		return (BeanDefinition) tmp;
	}
}
