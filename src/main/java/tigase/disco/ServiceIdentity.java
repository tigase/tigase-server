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
package tigase.disco;

import tigase.xml.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Describe class ServiceIdentity here.
 * <br>
 * Created: Sat Feb 10 13:34:54 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
*/
public class ServiceIdentity {

	private final String category;
	private final String lang;
	private final String name;
	private final String type;

	public static String[] getServiceIdentitiesCapsFromDiscoInfo(Element discoInfo) {
		List<String> list = new ArrayList<>();
		for (ServiceIdentity serviceIdentity : getServiceIdentitiesFromDiscoInfo(discoInfo)) {
			String asCapsString = serviceIdentity.getAsCapsString();
			list.add(asCapsString);
		}
		return list.toArray(new String[0]);
	}

	public static ServiceIdentity[] getServiceIdentitiesFromDiscoInfo(Element discoInfo) {
		List<ServiceIdentity> list = new ArrayList<>();
		final List<Element> identityElements = discoInfo.findChildren(child -> child.getName().equals("identity"));
		if (identityElements != null && !identityElements.isEmpty()) {
			for (Element identityElement : identityElements) {
				ServiceIdentity serviceIdentityFromElement = of(identityElement);
				list.add(serviceIdentityFromElement);
			}
		}
		return list.toArray(new ServiceIdentity[0]);
	}

	private static ServiceIdentity of(Element identity) throws IllegalArgumentException {
		final String lang = identity.getAttributeStaticStr("xml:lang");
		final String category = identity.getAttributeStaticStr("category");
		final String name = identity.getAttributeStaticStr("name");
		final String type = identity.getAttributeStaticStr("type");

		if (category == null || type == null) {
			throw new IllegalArgumentException(
					String.format("Neither category: %s nor type: %s can be null", category, type));
		}

		if (name == null && lang == null) {
			return new ServiceIdentity(category, type);
		} else if (lang == null) {
			return new ServiceIdentity(category, type, name);
		} else {
			return new ServiceIdentity(category, type, name, lang);
		}

	}

	/**
	 * Creates a new <code>ServiceIdentity</code> instance.
	 */
	public ServiceIdentity(String category, String type) {
		this.category = category;
		this.type = type;
		this.name = "";
		this.lang = "";
	}

	public ServiceIdentity(String category, String type, String name) {
		this.category = category;
		this.type = type;
		this.name = name;
		this.lang = "";
	}

	public ServiceIdentity(String category, String type, String name, String lang) {
		this.category = category;
		this.type = type;
		this.name = name;
		this.lang = lang;
	}

	public String getCategory() {
		return category;
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public Element getElement() {
		return new Element("identity", new String[]{"category", "type", "name"}, new String[]{category, type, name});
	}

	public String getAsCapsString() {
		return String.format("%s/%s/%s/%s", category, type, lang, name);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ServiceIdentity that = (ServiceIdentity) o;

		if (!category.equals(that.category)) {
			return false;
		}
		if (!name.equals(that.name)) {
			return false;
		}
		if (!type.equals(that.type)) {
			return false;
		}
		return lang.equals(that.lang);

	}

	public String getLang() {
		return lang;
	}

	@Override
	public int hashCode() {
		int result = category.hashCode();
		result = 31 * result + name.hashCode();
		result = 31 * result + type.hashCode();
		result = 31 * result + lang.hashCode();
		return result;
	}

}
