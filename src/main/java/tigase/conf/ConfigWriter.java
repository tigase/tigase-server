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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * Created by andrzej on 05.06.2016.
 */
public class ConfigWriter {

	private int indent = 0;

	public ConfigWriter() {}

	public static Map<String, Object> buildTree(Map<String, Object> props) {
		Map<String, Object> result = new HashMap<>();
		props.forEach((k, v) -> {
			String[] parts = k.split("/");
			Map<String, Object> map = result;
			for (int i=0; i<parts.length-1; i++) {
				map = (Map<String, Object>) map.computeIfAbsent(parts[i], (String key) -> { return new HashMap<String, Object>(); });
			}
			String key = parts[parts.length-1];
			map.put(key, v);
		});

		return result;
	}

	public void write(File f, Map<String, Object> props) throws IOException {
		try (FileWriter writer = new FileWriter(f)) {
			writeMap(writer, props);
		}
	}

	private void writeObject(Writer writer, Object obj) throws IOException {
		if (obj instanceof Map) {
			writer.write("{\n");
			indent++;
			writeMap(writer, (Map<String, Object>) obj);
			indent--;
			writeIndent(writer);
			writer.write("}\n");
		} else if (obj instanceof List) {
			writer.write("[\n");
			indent++;
			writeList(writer, (List) obj);
			indent--;
			writeIndent(writer);
			writer.write("]\n");
		} else if (obj instanceof String) {
			writer.write('\'');
			writer.write((String) obj);
			writer.write("\'\n");
		} else {
			writeString(writer, obj.toString());
			writer.write('\n');
		}
	}

	private void writeMap(Writer writer, Map<String, Object> map) throws IOException {
		for (Map.Entry<String, Object> e : map.entrySet()) {
			writeIndent(writer);
			writeString(writer, e.getKey());
			if (e.getValue() instanceof Map) {
				writer.write(" ");
			} else {
				writer.write(" = ");
			}
			writeObject(writer, e.getValue());
		}
	}

	private void writeIndent(Writer writer) throws IOException {
		for (int i=0; i<indent; i++) {
			writer.write("    ");
		}
	}

	private void writeList(Writer writer, List list) throws IOException {
		for (Object obj : list) {
			writeIndent(writer);
			writeObject(writer, obj);
		}
	}

	private void writeString(Writer writer, String str) throws IOException {
		if (str == null)
			return;

		if (str.contains("=") || str.contains(":") || str.contains(",") || str.contains("[") || str.contains("]")) {
			writer.append('\'');
			writer.write(str);
			writer.append('\'');
		} else {
			writer.write(str);
		}
	}

}
