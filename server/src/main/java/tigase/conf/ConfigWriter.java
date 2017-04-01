/*
 * ConfigWriter.java
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
 *
 */
package tigase.conf;

import tigase.kernel.beans.config.AbstractBeanConfigurator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Created by andrzej on 05.06.2016.
 */
public class ConfigWriter {

	private int indent = 0;

	private boolean resolveVariables = false;

	public ConfigWriter() {}

	public static Map<String, Object> buildTree(Map<String, Object> props) {
		Map<String, Object> result = new LinkedHashMap<>();
		props.forEach((k, v) -> {
			String[] parts = k.split("/");
			Map<String, Object> map = result;
			Map<String, Object> parent = null;
			for (int i=0; i<parts.length-1; i++) {
				parent = map;
				map = (Map<String, Object>) map.computeIfAbsent(parts[i], (String key) -> { return new HashMap<String, Object>(); });
			}
			String key = parts[parts.length-1];
			AbstractBeanConfigurator.BeanDefinition beanDefinition;
			switch (key) {
				case "active":
					if (map instanceof AbstractBeanConfigurator.BeanDefinition) {
						beanDefinition = (AbstractBeanConfigurator.BeanDefinition) map;
					} else {
						beanDefinition = new AbstractBeanConfigurator.BeanDefinition();
						beanDefinition.setBeanName(parts[parts.length-2]);
						beanDefinition.putAll(map);
						parent.put(beanDefinition.getBeanName(), beanDefinition);
					}

					beanDefinition.setActive((v instanceof Boolean) ? ((Boolean) v) : "true".equals(v.toString()));
					break;
				case "class":
					if (map instanceof AbstractBeanConfigurator.BeanDefinition) {
						beanDefinition = (AbstractBeanConfigurator.BeanDefinition) map;
					} else {
						beanDefinition = new AbstractBeanConfigurator.BeanDefinition();
						beanDefinition.setBeanName(parts[parts.length-2]);
						beanDefinition.putAll(map);
						parent.put(beanDefinition.getBeanName(), beanDefinition);
					}

					beanDefinition.setClazzName((String) v);
					break;
				default:
					map.put(key, v);
					break;
			}
		});

		return result;
	}

	public ConfigWriter resolveVariables() {
		this.resolveVariables = true;
		return this;
	}

	public void write(File f, Map<String, Object> props) throws IOException {
		try (FileWriter writer = new FileWriter(f, false)) {
			write(writer, props);
		}
	}

	public void write(Writer writer, Map<String, Object> props) throws IOException {
		writeMap(writer, props);
	}

	private void writeObject(Writer writer, Object obj) throws IOException {
		writeObject(writer, obj, "\n");
	}

	private void writeObject(Writer writer, Object obj, String newLine) throws IOException {
		if (obj == null) {
			writer.write("null");
			if (newLine != null)
				writer.write(newLine);
			return;
		}

		if (obj instanceof AbstractBeanConfigurator.BeanDefinition) {
			AbstractBeanConfigurator.BeanDefinition def = (AbstractBeanConfigurator.BeanDefinition) obj;
			writer.write("(");
			indent++;
			boolean first = true;
			if (def.getClazzName() != null) {
				writer.write("class: ");
				writer.write(def.getClazzName());
				first = false;
			}
			if (!def.isActive()) {
				if (!first) {
					writer.write(",");
					writer.write("\n");
					writeIndent(writer);
				}
				writer.write("active: false");
			}
			if (def.isExportable()) {
				if (!first) {
					writer.write(",");
					writer.write("\n");
					writeIndent(writer);
				}
				writer.write("exportable: true");
			}
			indent--;
			if (def.isEmpty()) {
				writer.write(") {}");
			} else {
				writer.write(") {\n");
				indent++;
				writeMap(writer, (Map<String, Object>) obj);
				indent--;
				writeIndent(writer);
				writer.write("}");
			}
			if (newLine != null) {
				writer.write(newLine);
			}
		} else if (obj instanceof ConfigReader.Variable) {
			if (resolveVariables) {
				writeObject(writer, ((ConfigReader.Variable) obj).calculateValue(), newLine);
			} else if (obj instanceof ConfigReader.EnvironmentVariable) {
				ConfigReader.EnvironmentVariable variable = (ConfigReader.EnvironmentVariable) obj;
				writer.write("env('");
				writer.write(variable.getName());
				writer.write("')");
				if (newLine != null) {
					writer.write(newLine);
				}
			} else if (obj instanceof ConfigReader.PropertyVariable) {
				ConfigReader.PropertyVariable variable = (ConfigReader.PropertyVariable) obj;
				writer.write("prop('");
				writer.write(variable.getName());
				if (variable.getDefValue() != null) {
					writer.write("', '");
					writer.write(variable.getDefValue());
				}
				writer.write("')");
				if (newLine != null) {
					writer.write(newLine);
				}
			} else if (obj instanceof ConfigReader.CompositeVariable) {
				ConfigReader.CompositeVariable variable = (ConfigReader.CompositeVariable) obj;
				List<Object> arguments = variable.getArguments();
				List<ConfigReader.CompositeVariable.Operation> operations = variable.getOperations();
				writeObject(writer,arguments.get(0), null);
				for (int i=0; i<operations.size(); i++) {
					ConfigReader.CompositeVariable.Operation o = operations.get(i);
					switch (o) {
						case multiply:
							writer.write(" * ");
							break;
						case divide:
							writer.write(" / ");
							break;
						case add:
							writer.write(" + ");
							break;
						case substract:
							writer.write(" - ");
							break;
					}
					writeObject(writer,arguments.get(i + 1), null);
				}
				if (newLine != null) {
					writer.write(newLine);
				}
			}
		} else if (obj instanceof Map) {
			Map<String, Object> map = (Map<String, Object>) obj;
			if (map.isEmpty()) {
				writer.write("{}");
			} else {
				writer.write("{\n");
				indent++;
				writeMap(writer, map);
				indent--;
				writeIndent(writer);
				writer.write("}");
			}
			if (newLine != null) {
				writer.write(newLine);
			}
		} else if (obj instanceof Collection) {
			List list = (obj instanceof List) ? (List) obj : new ArrayList((Collection) obj);
			boolean simple = true;
			for (Object o : list) {
				simple &= (o instanceof Number) || (o instanceof String);
			}
			if (simple && list.size() < 6) {
				writer.write("[ ");
				writeListSimple(writer, list);
				writer.write(" ]");
				if (newLine != null) {
					writer.write(newLine);
				}
			} else {
				writer.write("[\n");
				indent++;
				writeList(writer, list);
				indent--;
				writeIndent(writer);
				writer.write("]");
				if (newLine != null) {
					writer.write(newLine);
				}
			}
		} else if ( obj.getClass().isArray() ) {
			List tmp = new ArrayList();
			for (int i = 0; i< Array.getLength(obj); i++) {
				tmp.add(Array.get(obj, i));
			}
			writeObject(writer, tmp);
		} else if (obj instanceof String) {
			writer.write('\'');
			writer.write((String) obj);
			writer.write("\'");
			if (newLine != null)
				writer.write(newLine);
		} else {
			writeString(writer, obj.toString());
			if (obj instanceof Long) {
				writeString(writer, "L");
			}
			if (obj instanceof Float) {
				writeString(writer, "f");
			}
			if (newLine != null)
				writer.write(newLine);
		}
	}

	private void writeMap(Writer writer, Map<String, Object> map) throws IOException {
		List<Map.Entry<String, Object>> items = new ArrayList<>(map.entrySet());

		items.sort((a,b) -> {
			boolean a_ = a.getKey().startsWith("--");
			boolean b_ = b.getKey().startsWith("--");

			if (a_ && !b_) {
				return -1;
			}
			if (!b_ && a_) {
				return 1;
			}

			if ((a.getValue() instanceof Map) && !(b.getValue() instanceof Map)) {
				return 1;
			}
			if (!(a.getValue() instanceof Map) && (b.getValue() instanceof Map)) {
				return -1;
			}

			if ((a.getValue() instanceof Map) && (b.getValue() instanceof Map)) {
				if ("dataSource".equals(a.getKey())) {
					return -1;
				}
				if ("dataSource".equals(b.getKey())) {
					return 1;
				}
				if ("userRepository".equals(a.getKey())) {
					return -1;
				}
				if ("userRepository".equals(b.getKey())) {
					return 1;
				}
				if ("authRepository".equals(a.getKey())) {
					return -1;
				}
				if ("authRepository".equals(b.getKey())) {
					return 1;
				}
			}

			return a.getKey().compareTo(b.getKey());
		});

		for (Map.Entry<String, Object> e : items) {
			writeIndent(writer);
			if (indent == 0 && e.getKey().startsWith("--")) {
				writer.write(e.getKey());
			} else {
				writeString(writer, e.getKey());
			}
			if (e.getValue() instanceof Map) {
				writer.write(" ");
			} else {
				writer.write(" = ");
			}
			if (indent == 0 && e.getKey().startsWith("--") && (e.getValue() instanceof String)) {
				writeString(writer, (String) e.getValue());
				writer.write("\n");
			} else {
				writeObject(writer, e.getValue());
			}
		}
	}

	private void writeIndent(Writer writer) throws IOException {
		for (int i=0; i<indent; i++) {
			writer.write("    ");
		}
	}

	private void writeList(Writer writer, List list) throws IOException {
		boolean first = true;
		for (Object obj : list) {
			if (!first) {
				writer.write(",\n");
			} else {
				first = false;
			}
			writeIndent(writer);
			writeObject(writer, obj, null);
		}
		if (!list.isEmpty()) {
			writer.write("\n");
		}
	}

	private void writeListSimple(Writer writer, List list) throws IOException {
		boolean first = true;
		for (Object obj : list) {
			if (!first) {
				writer.write(", ");
			} else {
				first = false;
			}
			writeObject(writer, obj, null);
		}
	}

	private void writeString(Writer writer, String str) throws IOException {
		if (str == null)
			return;

		if (hasRestrictedChars(str)) {
			writer.append('\'');
			writer.write(str);
			writer.append('\'');
		} else {
			writer.write(str);
		}
	}

	private static final char[] RESTRICTED_CHARS = "=:,[]#+-*/".toCharArray();

	public static boolean hasRestrictedChars(String str) {
		for (char ch : RESTRICTED_CHARS) {
			if (str.indexOf(ch) > -1) {
				return true;
			}
		}
		return false;
	}

}
