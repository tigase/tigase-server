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

import java.io.*;
import java.util.*;

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

			switch (c) {
				case ':':
				case '=':
					if (holder.key != null) {
						holder.sb.append(c);
						break;
					}
					holder.key = holder.sb.toString().trim();
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
					holder.key = holder.sb.toString().trim();
					holder.sb = new StringBuilder();
					StateHolder tmp = new StateHolder();
					tmp.state = State.MAP;
					tmp.parent = holder;
					tmp.map = new HashMap();
					holder = tmp;
					break;
				}
				case '}': {
					Map val = holder.map;
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
							holder.map.put(holder.key, holder.value != null ? holder.value : holder.sb.toString().trim());
							break;
						case LIST:
							if (holder.sb.toString().trim().isEmpty()) {
								break;
							}
							holder.list.add(holder.sb.toString().trim());
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
			throw new IOException("Parsing error - invalid file structure");
		}
		return holder.map;
	}

	public class StateHolder {
		public State state = State.MAP;
		public StringBuilder sb = new StringBuilder();
		public String key;
		public List list;
		public Map<String, Object> map;
		public StateHolder parent = null;
		public char quoteChar = '\'';
		public Object value;
	}

	public static enum State {
		MAP,
		QUOTE,
		LIST
	}
}
