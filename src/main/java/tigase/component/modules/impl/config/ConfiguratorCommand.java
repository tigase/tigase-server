/*
 * ConfiguratorCommand.java
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

package tigase.component.modules.impl.config;

import tigase.component.adhoc.AdHocCommand;
import tigase.component.adhoc.AdHocCommandException;
import tigase.component.adhoc.AdHocResponse;
import tigase.component.adhoc.AdhHocRequest;
import tigase.form.Field;
import tigase.form.Form;
import tigase.kernel.BeanUtils;
import tigase.kernel.TypesConverter;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Converter;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.AbstractBeanConfigurator;
import tigase.kernel.beans.config.BeanConfigurator;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.DependencyManager;
import tigase.kernel.core.Kernel;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.JID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static java.util.Collections.sort;

@Bean(name = "BeanConfiguratorAdHocCommand")
public class ConfiguratorCommand implements AdHocCommand {

	protected final Logger log = Logger.getLogger(this.getClass().getName());
	@Inject(bean = "defaultTypesConverter")
	protected TypesConverter defaultTypesConverter;
	@Inject
	private Kernel kernel;
	@Inject(bean = BeanConfigurator.DEFAULT_CONFIGURATOR_NAME)
	private AbstractBeanConfigurator beanConfigurator;

	@Override
	public void execute(AdhHocRequest request, AdHocResponse response) throws AdHocCommandException {
		final Element data = request.getCommand().getChild("x", "jabber:x:data");
		final Form f = data == null ? null : new Form(data);

		if (request.getAction() != null && "cancel".equals(request.getAction())) {
			response.cancelSession();
		} else if (data == null) {
			Form form = new Form("form", "Select Bean to configure", null);

			ArrayList<String> items = getConfigurableBeansNames();
			ArrayList<String> options = new ArrayList<>();
			ArrayList<String> values = new ArrayList<>();

			sort(items);

			options.add("-- All --");
			values.add("-");
			for (String bn : items) {
				options.add(bn);
				values.add(bn);
			}

			form.addField(Field.fieldListSingle("bean", "-", "Bean to configure", options.toArray(new String[]{}),
					values.toArray(new String[]{})));

			response.getElements().add(form.getElement());
			response.startSession();
		} else if (f != null && f.getAsString("bean") != null) {
			try {
				String n = f.getAsString("bean");
				n = n.equals("-") ? null : n.trim();
				Form form = new Form("form", "Configure Beans", null);

				ArrayList<ConfigFieldItem> citems = getConfigItems(n);
				sort(citems);

				BeanConfig bc = null;

				for (ConfigFieldItem cfi : citems) {
					final String key = cfi.beanConfig.getBeanName() + "/" + cfi.field.getName();
					final Object bean = kernel.getInstance(cfi.beanConfig.getBeanName());
					final Object value = BeanUtils.getValue(bean, cfi.field);

					if (bc == null || !bc.equals(cfi.beanConfig)) {
						form.addField(Field.fieldFixed("Bean: " + cfi.beanConfig.getBeanName()));
						bc = cfi.beanConfig;
					}

					TypesConverter converter = defaultTypesConverter;
					Converter cAnn = cfi.field.getAnnotation(Converter.class);
					if (cAnn != null) {
						converter = kernel.getInstance(cAnn.converter());
					}

					Field field = Field.fieldTextSingle(key, value == null ? "" : converter.toString(value),
							cfi.configField.desc());
					form.addField(field);
				}

				response.getElements().add(form.getElement());
				response.startSession();
			} catch (Exception e) {
				throw new AdHocCommandException(Authorization.INTERNAL_SERVER_ERROR, e.getMessage());
			}
		} else {
			final HashMap<BeanConfig, HashMap<String, Object>> values = new HashMap<>();

			for (final Field field : f.getAllFields()) {
				int i = field.getVar().indexOf('/');
				final String bn = field.getVar().substring(0, i);
				final String fn = field.getVar().substring(i + 1);
				final String value = field.getValue();


				BeanConfig beanConfig = kernel.getDependencyManager().getBeanConfig(bn);
				if (!values.containsKey(beanConfig)) {
					values.put(beanConfig, new HashMap<>());
				}
				HashMap<String, Object> valuesToSet = values.get(beanConfig);
				valuesToSet.put(fn, value);
			}
			for (Map.Entry<BeanConfig, HashMap<String, Object>> entry : values.entrySet()) {
				final BeanConfig beanConfig = entry.getKey();
				final Object bean = kernel.getInstance(beanConfig.getBeanName());

				beanConfigurator.configure(beanConfig, bean, entry.getValue());
			}
		}
	}

	private ArrayList<ConfigFieldItem> getConfigItems(final String beanName) {
		ArrayList<ConfigFieldItem> result = new ArrayList<>();
		for (BeanConfig bc : kernel.getDependencyManager().getBeanConfigs()) {
			if (beanName != null && !beanName.equals(bc.getBeanName()))
				continue;
			final Class<?> cl = bc.getClazz();
			java.lang.reflect.Field[] fields = DependencyManager.getAllFields(cl);
			for (java.lang.reflect.Field field : fields) {
				final ConfigField cf = field.getAnnotation(ConfigField.class);
				if (cf != null) {
					ConfigFieldItem cfi = new ConfigFieldItem();
					cfi.beanConfig = bc;
					cfi.configField = cf;
					cfi.field = field;

					result.add(cfi);
				}
			}

		}
		return result;
	}

	private ArrayList<String> getConfigurableBeansNames() {
		ArrayList<String> result = new ArrayList<>();
		for (BeanConfig bc : kernel.getDependencyManager().getBeanConfigs()) {
			final Class<?> cl = bc.getClazz();
			java.lang.reflect.Field[] fields = DependencyManager.getAllFields(cl);
			for (java.lang.reflect.Field field : fields) {
				final ConfigField cf = field.getAnnotation(ConfigField.class);
				if (cf != null) {
					result.add(bc.getBeanName());
					break;
				}
			}
		}
		return result;
	}

	@Override
	public String getName() {
		return "Configurator";
	}

	@Override
	public String getNode() {
		return "bean-configurator";
	}

	@Override
	public boolean isAllowedFor(JID jid) {
		return true;
	}

	private class ConfigFieldItem implements Comparable<ConfigFieldItem> {
		BeanConfig beanConfig;
		java.lang.reflect.Field field;
		ConfigField configField;

		@Override
		public int compareTo(ConfigFieldItem o) {
			String t0 = beanConfig.getBeanName() + "#" + field.getName();
			String t1 = o.beanConfig.getBeanName() + "#" + o.field.getName();

			return t0.compareTo(t1);
		}
	}
}
