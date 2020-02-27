/*
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
package tigase.xmpp;

import tigase.server.Packet;
import tigase.xml.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * @author andrzej
 */
public class ElementMatcher {

	private final String[] path;
	private final boolean value;
	private final String xmlns;
	private final String[][] attributes;
	private final boolean checkChildren;

	public static ElementMatcher create(String str) {
		List<String> path = new ArrayList<String>();
		String xmlns = null;
		List<String[]> attributes = new ArrayList<>();
		boolean checkChildren = false;
		int offset = 0;
		boolean value = !str.startsWith("-");
		if (str.charAt(0) == '-' || str.charAt(0) == '+') {
			str = str.substring(1);
		}
		while (true) {
			String elemName = null;

			int slashIdx = str.indexOf('/', offset);
			int sIdx = str.indexOf('[', offset);
			if (slashIdx < 0) {
				slashIdx = str.length();
			}

			Element c = null;
			if (slashIdx < sIdx || sIdx < 0) {
				elemName = str.substring(offset, slashIdx);
				xmlns = null;
			} else {
				int eIdx = str.indexOf(']', sIdx);
				elemName = str.substring(offset, sIdx);
				String content = str.substring(sIdx + 1, eIdx);
				String[] parts = content.split(",");
				for (String part : parts) {
					int equalIdx = part.indexOf('=');
					if (equalIdx > 0) {
						attributes.add(new String[]{part.substring(0, equalIdx).intern(), part.substring(equalIdx + 1)});
					} else {
						xmlns = part;
					}
				}
				slashIdx = str.indexOf('/', eIdx);
				if (slashIdx < 0) {
					slashIdx = str.length();
				}
			}

			if (elemName != null && !elemName.isEmpty()) {
				if ("*".equals(elemName)) {
					checkChildren = true;
				} else {
					path.add(elemName.intern());
				}
			}

			if (slashIdx == str.length()) {
				break;
			}
			offset = slashIdx + 1;
		}
		if (xmlns != null) {
			xmlns = xmlns.intern();
		}

		return new ElementMatcher(path.toArray(new String[0]), checkChildren, xmlns, attributes.isEmpty() ? null : attributes.stream().toArray(String[][]::new), value);
	}

	public ElementMatcher(String[] path, String xmlns, boolean value) {
		this(path, false, xmlns, null, value);
	}

	public ElementMatcher(String[] path, boolean checkChildren, String xmlns, String[][] attributes, boolean value) {
		this.path = path;
		this.xmlns = xmlns;
		this.attributes = attributes;
		this.value = value;
		this.checkChildren = checkChildren;
	}

	public boolean matches(Packet packet) {
		Element child = path.length == 0 ? packet.getElement() : packet.getElement().findChildStaticStr(path);
		if (checkChildren) {
			if (child == null) {
				return false;
			}
			List<Element> children = child.getChildren();
			if (children == null) {
				return false;
			}
			for (Element el : children) {
				if (matches(el)) {
					return true;
				}
			}
			return false;
		} else {
			return matches(child);
		}
	}

	protected boolean matches(Element child) {
		if (child == null) {
			return false;
		}
		if (xmlns != null && xmlns != child.getXMLNS()) {
			return false;
		}
		if (attributes != null) {
			for (String[] pair : attributes) {
				if (!pair[1].equals(child.getAttributeStaticStr(pair[0]))) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean getValue() {
		return value;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (!value) {
			sb.append('-');
		}
		for (String p : path) {
			sb.append('/');
			sb.append(p);
		}

		if (checkChildren) {
			sb.append("/*");
		}

		if (xmlns != null || attributes != null) {
			sb.append("[");
			if (xmlns != null) {
				sb.append(xmlns);
			}
			if (attributes != null && attributes.length > 0) {
				boolean first = xmlns == null;
				for (String[] pair : attributes) {
					if (!first) {
						sb.append(",");
					} else {
						first = false;
					}
					sb.append(pair[0]).append("=").append(pair[1]);
				}
			}
			sb.append("]");
		}
		return sb.toString();
	}
}
