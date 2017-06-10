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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

	public Map<String, Object> read(Reader reader) throws IOException, ConfigException {
		BufferedReader buffReader = new BufferedReader(reader);
		Map<String, Object> props = process(buffReader);
		return props;
	}

	public Map<String, Object> read(File f) throws IOException, ConfigException {
		Map<String, Object> props;

		try (FileReader reader = new FileReader(f)) {
			props = read(reader);
		}

		return props;
	}

//	private void injectBeans(Map<String, Object> props) {
//		List<String> beans
//		props.entrySet().forEach();
//	}

	private Map<String, Object> process(Reader reader) throws IOException, ConfigException {
		holder.map = new HashMap<>();
		int line = 1;
		int pos = 0;
		int read = 0;
		StringBuilder lineContent = new StringBuilder();
		try {
			while ((read = reader.read()) != -1) {
				char c = (char) read;
				if (c != '\n') {
					lineContent.append(c);
				} else  {
					lineContent = new StringBuilder();
				}
				pos++;

				if (holder.state == State.QUOTE) {
					if (holder.quoteChar == c) {
						holder.parent.value = holder.sb.toString();
						holder = holder.parent;
					} else {
						holder.sb.append(c);
					}
					if (c == '\n') {
						line++;
						pos = 0;
					}
					continue;
				}
				if (holder.state == State.COMMENT) {
					if (c != '\n') continue;
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
						if (holder.variable != null && (holder.state != State.PROPERTY && holder.state != State.ENVIRONMENT)) {
							if (holder.variable instanceof CompositeVariable) {
								Object value = holder.value;
								if (value == null) {
									value = decodeValue(holder.sb.toString());
								}

								((CompositeVariable) holder.variable).add(value);
							}
							holder.value = holder.variable;
							holder.variable = null;
						}

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
							holder.key = (holder.value != null && holder.value instanceof String)
										 ? ((String) holder.value).trim()
										 : holder.sb.toString().trim();
						}
						holder.sb = new StringBuilder();
						StateHolder tmp = new StateHolder();
						tmp.state = State.MAP;
						tmp.parent = holder;
						tmp.map = (holder.value instanceof AbstractBeanConfigurator.BeanDefinition)
								  ? ((Map) holder.value)
								  : new HashMap();
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
						String key = (holder.value != null && holder.value instanceof String)
									 ? ((String) holder.value).trim()
									 : holder.sb.toString().trim();
						StateHolder tmp = new StateHolder();
						switch (key) {
							case "env":
								tmp.state = State.ENVIRONMENT;
								tmp.variable = new ConfigReader.EnvironmentVariable();
								break;
							case "prop":
								tmp.state = State.PROPERTY;
								tmp.variable = new ConfigReader.PropertyVariable();
								break;
							default:
								holder.key = key;
								tmp.state = State.BEAN;
								tmp.bean = new AbstractBeanConfigurator.BeanDefinition();
								tmp.bean.setBeanName(holder.key);
								break;
						}
						tmp.parent = holder;
						holder = tmp;
						break;
					}
					case ')': {
						Object val = null;
						switch (holder.state) {
							case ENVIRONMENT:
							case PROPERTY:
								AbstractEnvironmentPropertyVariable prop = (AbstractEnvironmentPropertyVariable) holder.variable;
								String value = holder.value.toString().trim();
								if (prop.getName() == null) {
									prop.setName(value);
								} else {
									prop.setDefValue(value);
								}
								val = holder.variable;
								break;
							case BEAN:
								val = holder.bean;
								Object val1 = holder.value != null ? holder.value : decodeValue(holder.sb.toString().trim());
								setBeanDefinitionValue(val1);
								break;
						}
						holder = holder.parent;
						holder.value = val;
						break;
					}
					case '\n':
						line++;
						pos = 0;
						// there should be no break here!
					case ',':
						if (holder.variable != null && (holder.state != State.PROPERTY && holder.state != State.ENVIRONMENT)) {
							if (holder.variable instanceof CompositeVariable) {
								Object value = holder.value;
								if (value == null) {
									value = decodeValue(holder.sb.toString());
								}

								((CompositeVariable) holder.variable).add(value);
							}
							holder.value = holder.variable;
							holder.variable = null;
						}
						switch (holder.state) {
							case MAP:
								if (holder.key == null || holder.key.isEmpty()) {
									break;
								}
								holder.map.put(holder.key, holder.value != null
														   ? holder.value
														   : decodeValue(holder.sb.toString().trim()));
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
							case ENVIRONMENT:
							case PROPERTY:
								((AbstractEnvironmentPropertyVariable) holder.variable).setName(holder.value.toString().trim());
								holder.value = holder.variable;
								break;
						}
						holder.key = null;
						holder.sb = new StringBuilder();
						holder.value = null;
						break;
					case '+':
					case '-':
					case '/':
					case '*':
						if (holder.state == State.LIST) {
							if (holder.value != null) {
								Object value = holder.value;
								holder.value = null;
								holder.sb = new StringBuilder();
								if (holder.variable == null) {
									CompositeVariable var = new CompositeVariable();
									holder.variable = var;
								}
								((CompositeVariable) holder.variable).add(c, value);
								break;
							}
						} else {
							Object value = holder.value;
							if (value == null) {
								value = decodeValue(holder.sb.toString());
							}
							if (holder.key != null && value != null) {
								holder.value = null;
								holder.sb = new StringBuilder();
								if (holder.variable == null) {
									CompositeVariable var = new CompositeVariable();
									holder.variable = var;
								}
								((CompositeVariable) holder.variable).add(c, value);
								break;
							}
						}
					default:
						holder.sb.append(c);
						break;
				}
			}
		} catch (java.lang.UnsupportedOperationException ex) {
			while ((read = reader.read()) != -1) {
				char c = (char) read;
				if (c != '\n') {
					lineContent.append(c);
				} else {
					break;
				}
			}
			throw new UnsupportedOperationException(ex.getMessage(), line, pos, lineContent.toString(), ex);
		}
		if (holder.state != State.MAP || holder.parent != null) {
			throw new InvalidFormatException("Parsing error - invalid file structure, state = " + holder.state + ", parent = " + holder.parent);
		}

		if (holder.key != null && !holder.key.isEmpty()) {
			holder.map.put(holder.key, holder.value != null ? holder.value : decodeValue(holder.sb.toString().trim()));
		}

		return holder.map;
	}

	public static class ConfigException extends Exception {

		public ConfigException(String msg) {
			super(msg);
		}

		public ConfigException(String msg, Throwable cause) {
			super(msg, cause);
		}
	}

	public static class InvalidFormatException extends ConfigException {

		public InvalidFormatException(String msg) {
			super(msg);
		}

	}

	public static class UnsupportedOperationException extends ConfigException {

		private final int line;
		private final int position;
		private final String lineContent;

		public UnsupportedOperationException(String msg, int line, int position, String lineContent, Throwable cause) {
			super(msg, cause);
			this.line = line;
			this.position = position;
			this.lineContent = lineContent;
		}

		public int getLine() {
			return line;
		}

		public int getPosition() {
			return position;
		}

		public String getLineContent() {
			return lineContent;
		}
	}

	private static Pattern INTEGER_PATTERN = Pattern.compile("([0-9]+)([lL]*)");
	private static Pattern DOUBLE_PATTERN = Pattern.compile("([0-9]+\\.[0-9]+)([dDfF]*)");

	private static double x = 2.1f;

	protected Object decodeValue(String string_in) {
		String string = string_in.trim();
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
			case "null":
				return null;
			default:
				break;
		}

		return string_in;
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
		public Variable variable;
		public StateHolder parent = null;
		public char quoteChar = '\'';
		public Object value;
	}

	public static enum State {
		MAP,
		QUOTE,
		LIST,
		COMMENT,
		BEAN,
		ENVIRONMENT,
		PROPERTY
	}

	public interface Variable {

		Object calculateValue();

	}

	public static abstract class AbstractEnvironmentPropertyVariable implements Variable {

		private String name;
		private String defValue;

		protected AbstractEnvironmentPropertyVariable() {
		}

		protected AbstractEnvironmentPropertyVariable(String name, String defValue) {
			this.setName(name);
			this.setDefValue(defValue);
		}

		protected String getName() {
			return this.name;
		}

		protected void setName(String name) {
			this.name = name;
		}

		protected String getDefValue() {
			return defValue;
		}

		protected void setDefValue(String defValue) {
			this.defValue = defValue;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof AbstractEnvironmentPropertyVariable) {
				AbstractEnvironmentPropertyVariable v = (AbstractEnvironmentPropertyVariable) obj;

				if (v.name == this.name || (v.name != null && v.name.equals(this.name))) {
					if (v.defValue == this.defValue || (v.defValue != null && v.defValue.equals(this.defValue))) {
						return true;
					}
				}
			}
			return false;
		}
	}

	public static class EnvironmentVariable extends AbstractEnvironmentPropertyVariable {

		public EnvironmentVariable() {
		}

		public EnvironmentVariable(String name, String defValue) {
			super(name, defValue);
		}

		@Override
		public Object calculateValue() {
			Object val = System.getenv(getName());
			if (val == null) {
				val = getDefValue();
			}
			return val;
		}

	}

	public static class PropertyVariable extends AbstractEnvironmentPropertyVariable {

		public PropertyVariable() {
		}

		public PropertyVariable(String name, String defValue) {
			super(name, defValue);
		}


		@Override
		public Object calculateValue() {
			return System.getProperty(getName(), getDefValue());
		}
		
	}

	public static class CompositeVariable implements Variable {

		private final List<Object> values = new ArrayList<>();
		private final List<Operation> operations = new ArrayList<>();

		@Override
		public Object calculateValue() {
			if (values.isEmpty())
				return null;

			List<Object> values = this.values.stream()
					.map(val -> (val instanceof Variable) ? ((Variable) val).calculateValue() : val)
					.collect(Collectors.toList());

			Object first = values.get(0);
			if (first instanceof String) {
				if (!operations.stream().allMatch(o -> o == Operation.add)) {
					throw new java.lang.UnsupportedOperationException("Invalid operation for String!");
				}
				return ((List<String>) (List) values).stream().collect(Collectors.joining());
			} else if (first instanceof Number) {
				List<Operation> operations = new ArrayList<>(this.operations);
				for (Operation operation : Operation.values()) {
					Iterator<Operation> it = operations.iterator();
					int pos = 0;
					while (it.hasNext()) {
						Operation o = it.next();
						if (!operation.equals(o)) {
							pos++;
							continue;
						}

						Number arg1 = (Number) values.remove(pos);
						Number arg2 = (Number) values.remove(pos);

						Number result = operation.execute(arg1, arg2);
						values.add(pos, result);
						it.remove();
					}
				}

				if (values.size() > 1) {
					throw new RuntimeException("Variable calculation exception");
				}

				return values.stream().findFirst().get();
			} else {
				throw new java.lang.UnsupportedOperationException("Cannot calculate composite variable!");
			}
		}

		public void add(Object value) {
			values.add(value);
		}

		public void add(char operation, Object value) {
			Operation o = null;
			switch (operation) {
				case '+':
					o = Operation.add;
					break;
				case '-':
					o = Operation.substract;
					break;
				case '*':
					o = Operation.multiply;
					break;
				case '/':
					o = Operation.divide;
					break;
				default:
					throw new java.lang.UnsupportedOperationException();
			}
			if (value instanceof String && o != Operation.add) {
				throw new java.lang.UnsupportedOperationException("Cannot " + o.name() + " a String");
			}
			operations.add(o);
			values.add(value);
		}

		public enum Operation {
			multiply,
			divide,
			add,
			substract;

			public Number execute(Number arg1, Number arg2) {
				if (arg1 instanceof Double || arg2 instanceof Double) {
					double a1 = arg1.doubleValue();
					double a2 = arg2.doubleValue();
					switch (this) {
						case multiply:
							return a1 * a2;
						case divide:
							return a1 / a2;
						case add:
							return a1 + a2;
						case substract:
							return a1 - a2;
					}
				} else if (arg1 instanceof Float || arg2 instanceof Float) {
					float a1 = arg1.floatValue();
					float a2 = arg2.floatValue();
					switch (this) {
						case multiply:
							return a1 * a2;
						case divide:
							return a1 / a2;
						case add:
							return a1 + a2;
						case substract:
							return a1 - a2;
					}
				} else if (arg1 instanceof Long || arg2 instanceof Long) {
					long a1 = arg1.longValue();
					long a2 = arg2.longValue();
					switch (this) {
						case multiply:
							return a1 * a2;
						case divide:
							return a1 / a2;
						case add:
							return a1 + a2;
						case substract:
							return a1 - a2;
					}
				} else if (arg1 instanceof Integer || arg2 instanceof Integer) {
					int a1 = arg1.intValue();
					int a2 = arg2.intValue();
					switch (this) {
						case multiply:
							return a1 * a2;
						case divide:
							return a1 / a2;
						case add:
							return a1 + a2;
						case substract:
							return a1 - a2;
					}
				}
				throw new RuntimeException("Invalid argument exception");
			}
		}

		public List<Object> getArguments() {
			return values;
		}

		public List<Operation> getOperations() {
			return operations;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof CompositeVariable) {
				CompositeVariable v = (CompositeVariable) obj;
				if (v.values.size() != values.size()) {
					return false;
				}
				if (v.operations.size() != operations.size()) {
					return false;
				}

				for (int i=0; i<values.size(); i++) {
					if (!values.get(i).equals(v.values.get(i))) {
						return false;
					}
				}

				for (int i=0; i<operations.size(); i++) {
					if (!operations.get(i).equals(v.operations.get(i))) {
						return false;
					}
				}
				return true;
			}
			return false;
		}
	}
}
