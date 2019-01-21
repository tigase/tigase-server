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
package tigase.util.setup;

import tigase.conf.ConfigBuilder;
import tigase.kernel.beans.config.AbstractBeanConfigurator;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.beans.selector.ServerBeanSelector;
import tigase.kernel.core.Kernel;
import tigase.server.ServerComponent;
import tigase.server.xmppsession.SessionManager;
import tigase.xmpp.XMPPImplIfc;
import tigase.xmpp.jid.BareJID;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by andrzej on 30.03.2017.
 */
public class SetupHelper {

	public static enum RestApiSecurity {
		forbidden,
		api_keys,
		open_access
	}

	public static BeanDefinition convertToBeanDefinition(Class<?> cls) {
		return new BeanDefinition(cls);
	}

	public static ConfigBuilder generateConfig(ConfigTypeEnum configType, String dbUri, boolean clusterMode,
											   boolean acs, Optional<Set<String>> optionalComponentsOption,
											   Optional<Set<String>> forceEnabledComponentsOptions,
											   Optional<Set<String>> pluginsOption, String defaultVirtualHost,
											   Optional<BareJID[]> admins, Optional<HttpSecurity> httpSecurity) {
		ConfigBuilder builder = new ConfigBuilder().with("config-type", configType.id().toLowerCase());

		if (clusterMode) {
			builder.with("cluster-mode", "true");
		}
		builder.with("default-virtual-host", defaultVirtualHost)
				.with("admins", admins)
				.with("debug", Arrays.asList("server"));

		builder.withBean(ds -> ds.name("dataSource").withBean(def -> def.name("default").with("uri", dbUri)));

		List<AbstractBeanConfigurator.BeanDefinition> sessManSubBeans = new ArrayList<>();
		if (pluginsOption.isPresent()) {
			Set<String> plugins = pluginsOption.get();
			sessManSubBeans.addAll(SetupHelper.getAvailableProcessors(SessionManager.class, XMPPImplIfc.class)
										   .stream()
										   .filter(def -> (def.isActive() && !plugins.contains(def.getName())) ||
												   ((!def.isActive()) && plugins.contains(def.getName())))
										   .map(def -> new AbstractBeanConfigurator.BeanDefinition.Builder().name(
												   def.getName()).active(plugins.contains(def.getName())).build())
										   .collect(Collectors.toList()));
		}
		if (acs) {
			sessManSubBeans.add(new AbstractBeanConfigurator.BeanDefinition.Builder().name("strategy")
										.clazz("tigase.server.cluster.strategy.OnlineUsersCachingStrategy")
										.build());
		}
		if (!sessManSubBeans.isEmpty()) {
			builder.withBean(sessMan -> sessMan.name("sess-man")
					.with(sessManSubBeans.stream().toArray(x -> new AbstractBeanConfigurator.BeanDefinition[x])));
		}

		Set<String> optionalComponents = optionalComponentsOption.orElse(SetupHelper.getAvailableComponents()
																				 .stream()
																				 .filter(def -> !def.isCoreComponent())
																				 .map(def -> def.getName())
																				 .collect(Collectors.toSet()));
		SetupHelper.getAvailableComponents().stream().filter(def -> !def.isCoreComponent()).filter(def -> {
			ConfigType ct = def.getClazz().getAnnotation(ConfigType.class);

			if (optionalComponents.contains(def.getName())) {
				return def.isActive() == false || (ct != null && !Arrays.asList(ct.value()).contains(configType)) ||
						(forceEnabledComponentsOptions.isPresent() &&
								forceEnabledComponentsOptions.get().contains(def.getName()))
						|| ("http".equals(def.getName()) && httpSecurity.isPresent())
						|| ("pubsub".equals(def.getName()) && optionalComponents.contains("http"));
			} else {
				return def.isActive() == true && (ct != null && Arrays.asList(ct.value()).contains(configType));
			}
		}).forEach(def -> {
			ConfigType ct = def.getClazz().getAnnotation(ConfigType.class);

			builder.withBean(b -> {
				b.name(def.getName())
						.active(optionalComponents.contains(def.getName()))
						.clazz((ct != null && !Arrays.asList(ct.value()).contains(configType)) ? def.getClazz() : null);
				if ("http".equals(def.getName())) {
					httpSecurity.ifPresent(sec -> {
						if (sec.setupUser != null && !sec.setupUser.isEmpty() && sec.setupPassword != null &&
								!sec.setupPassword.isEmpty()) {
							b.withBean(setup -> setup.name("setup")
									.with("admin-user", sec.setupUser)
									.with("admin-password", sec.setupPassword));
						}
					});
				}
				if ("pubsub".equals(def.getName()) && optionalComponents.contains("http")) {
					b.with("trusted", Arrays.asList("http@{clusterNode}"));
				}
			});
		});

		return builder;
	}

	public static List<BeanDefinition> getAvailableBeans(Class processorClazz) {
		return getAvailableBeans(processorClazz, Kernel.class);
	}

	public static List<BeanDefinition> getAvailableBeans(Class processorClazz, Class componentClazz) {
		Kernel kernel = new Kernel();
		kernel.registerBean("beanSelector").asInstance(new ServerBeanSelector()).exportable().exec();
		return AbstractBeanConfigurator.getBeanClassesFromAnnotations(kernel, componentClazz)
				.entrySet()
				.stream()
				.filter(e -> processorClazz.isAssignableFrom(e.getValue()))
				.map(e -> e.getValue())
				.map(SetupHelper::convertToBeanDefinition)
				.collect(Collectors.toList());
	}

	public static List<BeanDefinition> getAvailableComponents() {
		return getAvailableBeans(ServerComponent.class);
	}

	public static List<BeanDefinition> getAvailableProcessors(Class componentClazz, Class processorClazz) {
		return getAvailableBeans(processorClazz, componentClazz);
	}

	public static class HttpSecurity {

		public String setupPassword;
		public String setupUser;

	}

}
