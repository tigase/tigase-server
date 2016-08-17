/*
 * ConfigReader.java
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

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by andrzej on 05.06.2016.
 */
public class ConfigReader {

//	private State state = State.NORMAL;
//	private ArrayDeque<State> stack = new ArrayDeque<>();

	private StateHolder holder = new StateHolder();

	public ConfigReader() {

	}

	public static Map<String, Object> flatTree(Map<String, Object> props) {
		Map<String, Object> result = new HashMap<>();
		flatTree(result, null, props);
		return result;
	}

	private static void flatTree(Map<String, Object> result, String prefix, Map<String, Object> props) {
		props.forEach((k,v) -> {
			String key = prefix == null ? k : (prefix + "/" + k);
			if (v instanceof AbstractBeanConfigurator.BeanDefinition) {
				AbstractBeanConfigurator.BeanDefinition beanDefinition = (AbstractBeanConfigurator.BeanDefinition) v;
				if (beanDefinition.getClazzName() != null) {
					result.put(key + "/class", beanDefinition.getClazzName());
				}
				if (beanDefinition.isActive()) {
					result.put(key + "/active", "true");
				} else {
					result.put(key + "/active", "false");
				}
			}
			if (v instanceof Map) {
				flatTree(result, key, (Map<String, Object>) v);
			} else {
				result.put(key, v);
			}
		});
	}

	public Map<String, Object> read(File f) throws IOException {
		Map<String, Object> props = new HashMap<>();

		try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
			props = process(reader);
		}

		return props;
	}

//	private void injectBeans(Map<String, Object> props) {
//		List<String> beans
//		props.entrySet().forEach();
//	}

	private Map<String, Object> process(Reader reader) throws IOException {
		holder.map = new HashMap<>();
		int read = 0;
		while ((read = reader.read()) != -1) {
			char c = (char) read;

			if (holder.state == State.QUOTE) {
				if (holder.quoteChar == c) {
					holder.parent.value = holder.sb.toString().trim();
					holder = holder.parent;
				} else {
					holder.sb.append(c);
				}
				continue;
			}
			if (holder.state == State.COMMENT) {
				if (c != '\n')
					continue;
				holder = holder.parent;
			}

			switch (c) {
				case '#': {
					StateHolder tmp = new StateHolder();
					tmp.state = State.COMMENT;
					tmp.parent = holder;
					holder = tmp;
					continue;
				}
				case ':':
				case '=':
					if (holder.key != null) {
						holder.sb.append(c);
						break;
					}
					holder.key = holder.value != null ? holder.value.toString() : holder.sb.toString().trim();
					holder.value = null;
					holder.sb = new StringBuilder();
					break;

				case '[': {
					StateHolder tmp = new StateHolder();
					tmp.state = State.LIST;
					tmp.parent = holder;
					tmp.list = new ArrayList();
					holder = tmp;
					break;
				}
				case ']': {
					List val = holder.list;
					if (holder.value == null) {
						String valueStr = holder.sb.toString().trim();
						if (!valueStr.isEmpty()) {
							Object value = decodeValue(valueStr);
							holder.list.add(value);
						}
					} else {
						holder.list.add(holder.value);
					}
//					if (holder.value != null) {
//						val.add(holder.value instanceof String ? decodeValue((String) holder.value) : holder.value);
//					}
					holder = holder.parent;
					holder.value = val;
					break;
				}
				case '\'':
				case '\"': {
					StateHolder tmp = new StateHolder();
					tmp.state = State.QUOTE;
					tmp.parent = holder;
					holder = tmp;
					holder.quoteChar = c;
					break;
				}
				case '{': {
					if (holder.key == null) {
						holder.key = (holder.value != null && holder.value instanceof String) ? ((String) holder.value).trim() : holder.sb.toString().trim();
					}
					holder.sb = new StringBuilder();
					StateHolder tmp = new StateHolder();
					tmp.state = State.MAP;
					tmp.parent = holder;
					tmp.map = (holder.value instanceof AbstractBeanConfigurator.BeanDefinition) ? ((Map) holder.value) : new HashMap();
					holder = tmp;
					break;
				}
				case '}': {
					Map val = holder.map;
					holder = holder.parent;
					// special use case to convert maps with active or class into bean definition
					if (val != null && (val.containsKey("active") || val.containsKey("class"))) {
						AbstractBeanConfigurator.BeanDefinition bean = new AbstractBeanConfigurator.BeanDefinition();
						bean.setBeanName(holder.key);
						Object v = val.remove("class");
						if (v != null) {
							bean.setClazzName((String) v);
						}
						v = val.remove("active");
						if (v != null) {
							bean.setActive((Boolean) v);
						}
						bean.putAll(val);
						holder.value = bean;
					} else {
						holder.value = val;
					}
					break;
				}
				case '(': {
					holder.key = (holder.value != null && holder.value instanceof String) ? ((String) holder.value).trim() : holder.sb.toString().trim();
					StateHolder tmp = new StateHolder();
					tmp.state = State.BEAN;
					tmp.bean = new AbstractBeanConfigurator.BeanDefinition();
					tmp.bean.setBeanName(holder.key);
					tmp.parent = holder;
					holder = tmp;
					break;
				}
				case ')': {
					AbstractBeanConfigurator.BeanDefinition val = holder.bean;
					Object val1 = holder.value != null ? holder.value : decodeValue(holder.sb.toString().trim());
					setBeanDefinitionValue(val1);
					holder = holder.parent;
					holder.value = val;
					break;
				}
				case ',':
				case '\n':
					switch (holder.state) {
						case MAP:
							if (holder.key == null || holder.key.isEmpty()) {
								break;
							}
							holder.map.put(holder.key, holder.value != null ? holder.value : decodeValue(holder.sb.toString().trim()));
							break;
						case LIST:
							if (holder.value == null) {
								String valueStr = holder.sb.toString().trim();
								if (valueStr.isEmpty()) {
									break;
								}
								Object value = decodeValue(valueStr);
								holder.list.add(value);
							} else {
								holder.list.add(holder.value);
							}
							break;
						case BEAN:
							Object val = holder.value != null ? holder.value : decodeValue(holder.sb.toString().trim());
							setBeanDefinitionValue(val);
							break;
					}
					holder.key = null;
					holder.sb = new StringBuilder();
					holder.value = null;
					break;
				default:
					holder.sb.append(c);
					break;
			}
		}
		if (holder.state != State.MAP || holder.parent != null) {
			throw new IOException("Parsing error - invalid file structure, state = " + holder.state + ", parent = " + holder.parent);
		}
		return holder.map;
	}

	private static Pattern INTEGER_PATTERN = Pattern.compile("([0-9]+)([lL]*)");
	private static Pattern DOUBLE_PATTERN = Pattern.compile("([0-9]+\\.[0-9]+)([dDfF]*)");

	private static double x = 2.1f;

	protected Object decodeValue(String string) {
		// Decoding doubles and floats
		Matcher matcher = DOUBLE_PATTERN.matcher(string);
		if (matcher.matches()) {
			String value = matcher.group(1);
			String type = matcher.group(2);
			if (type.isEmpty() || "D".equals(type) || "d".equals(type) ) {
				return Double.parseDouble(value);
			} else {
				return Float.parseFloat(value);
			}
		}

		// Decoding integers and longs
		matcher = INTEGER_PATTERN.matcher(string);
		if (matcher.matches()) {
			String value = matcher.group(1);
			String type = matcher.group(2);
			if ("l".equals(type) || "L".equals(type)) {
				return Long.parseLong(value);
			} else {
				return Integer.parseInt(value);
			}
		}

		// Decoding booleans
		switch (string) {
			case "true":
				return true;
			case "false":
				return false;
			default:
				break;
		}

		return string;
	}

	protected void setBeanDefinitionValue(Object val) {
		if (holder.key == null || holder.key.isEmpty()) {
			return;
		}

		switch (holder.key) {
			case "class":
				holder.bean.setClazzName((String) val);
				break;
			case "active":
				holder.bean.setActive((Boolean) val);
				break;
			case "exportable":
				holder.bean.setExportable((Boolean) val);
				break;
			default:
				throw new RuntimeException("Error in configuration file - unknown bean definition field: " + holder.key);
		}

	}

	public class StateHolder {
		public State state = State.MAP;
		public StringBuilder sb = new StringBuilder();
		public String key;
		public List list;
		public Map<String, Object> map;
		public AbstractBeanConfigurator.BeanDefinition bean;
		public StateHolder parent = null;
		public char quoteChar = '\'';
		public Object value;
	}

	public static enum State {
		MAP,
		QUOTE,
		LIST,
		COMMENT,
		BEAN
	}
}
