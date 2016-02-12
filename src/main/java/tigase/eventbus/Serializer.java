/*
 * Serializer.java
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
package tigase.eventbus;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import tigase.kernel.BeanUtils;
import tigase.kernel.TypesConverter;
import tigase.xml.Element;
import tigase.xml.XMLUtils;

public class Serializer {

	public <T> T deserialize(final Element element) {
		try {
			final Class<?> cls = Class.forName(element.getName());
			final Object result = cls.newInstance();

			Field[] fields = BeanUtils.getAllFields(cls);
			for (final Field f : fields) {
				if (Modifier.isTransient(f.getModifiers()))
					continue;
				if (Modifier.isFinal(f.getModifiers()))
					continue;
				if (Modifier.isStatic(f.getModifiers()))
					continue;

				try {
					Object value;
					Element v = element.getChild(f.getName());
					if (v == null)
						continue;

					if (Element.class.isAssignableFrom(f.getType())) {
						if (v.getChildren().size() > 0) {
							value = v.getChildren().get(0);
						} else
							value = null;
					} else {
						value = TypesConverter.convert(XMLUtils.unescape(v.getCData()), f.getType());
					}
					BeanUtils.setValue(result, f, value);
				} catch (IllegalAccessException | InvocationTargetException caught) {
					caught.printStackTrace();
				}
			}
			return (T) result;
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}

	public Element serialize(final Object object) {
		final Class<?> cls = object.getClass();
		Element e = new Element(cls.getName());

		Field[] fields = BeanUtils.getAllFields(cls);
		for (final Field f : fields) {
			if (Modifier.isTransient(f.getModifiers()))
				continue;
			if (Modifier.isFinal(f.getModifiers()))
				continue;
			if (Modifier.isStatic(f.getModifiers()))
				continue;

			try {
				final Object value = BeanUtils.getValue(object, f);

				if (value == null)
					continue;

				Element v = new Element(f.getName());
				if (Element.class.isAssignableFrom(f.getType())) {
					v.addChild((Element) value);
				} else {
					String x = TypesConverter.toString(value);
					v.setCData(XMLUtils.escape(x));
				}
				e.addChild(v);
			} catch (IllegalAccessException | InvocationTargetException caught) {
				caught.printStackTrace();
			}
		}

		return e;
	}

}
