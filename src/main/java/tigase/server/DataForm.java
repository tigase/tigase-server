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
package tigase.server;

import tigase.server.Command.DataType;
import tigase.xml.Element;
import tigase.xml.XMLUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * @author Wojciech Kapcia
 */
public class DataForm {

	public static final String FIELD_EL = "field";
	public static final String VALUE_EL = "value";
	protected static final String[] FIELD_VALUE_PATH = {FIELD_EL, VALUE_EL};

	private static final Logger log = Logger.getLogger(DataForm.class.getName());

	/**
	 * Data form-types as defined in the XEP-0050.
	 */

	public static void addCheckBoxField(final Element el, String f_name, boolean f_value) {
		DataForm.addFieldValue(el, f_name, Boolean.toString(f_value), "boolean");
	}

	public static Element createDataForm(DataType data_type) {
		return new Element("x", new String[]{"xmlns", "type"}, new String[]{"jabber:x:data", data_type.name()});
	}

	public static Element addDataForm(Element el, DataType data_type) {
		Element x = createDataForm(data_type);

		el.addChild(x);

		return x;
	}

	public static void addField(final Element el, final String f_name, final String f_label, final String type) {
		Element x = el.getChild("x", "jabber:x:data");

		if (x == null) {
			x = addDataForm(el, DataType.submit);
		}

		Element field = new Element(FIELD_EL, new String[]{"var", "type", "label"},
									new String[]{XMLUtils.escape(f_name), type, f_label});

		x.addChild(field);
	}

	public static void addFieldMultiValue(final Element el, final String f_name, final List<String> f_value) {
		addFieldMultiValue(el, f_name, f_value, null);
	}

	public static void addFieldMultiValue(final Element el, final String f_name, final List<String> f_value,
										  final String label) {
		Element x = el.getChild("x", "jabber:x:data");

		if (x == null) {
			x = addDataForm(el, DataType.result);
		}
		if (f_value != null) {
			Element field = new Element(FIELD_EL, new String[]{"var", "type"},
										new String[]{XMLUtils.escape(f_name), "text-multi"});

			if (label != null) {
				field.addAttribute("label", label);
			}

			for (String val : f_value) {
				if (val != null) {
					Element value = new Element(VALUE_EL, XMLUtils.escape(val));

					field.addChild(value);
				}
			}
			x.addChild(field);
		}
	}

	public static void addFieldMultiValue(final Element el, final String f_name, final Throwable ex) {
		Element x = el.getChild("x", "jabber:x:data");

		if (x == null) {
			x = addDataForm(el, DataType.submit);
		}

		List<String> f_value = null;

		if (ex != null) {
			f_value = new ArrayList<String>(100);
			f_value.add(ex.getLocalizedMessage());
			for (StackTraceElement ste : ex.getStackTrace()) {
				f_value.add("  " + ste.toString());
			}
		}
		if (f_value != null) {
			Element field = new Element(FIELD_EL, new String[]{"var", "type"},
										new String[]{XMLUtils.escape(f_name), "text-multi"});

			for (String val : f_value) {
				if (val != null) {
					Element value = new Element(VALUE_EL, XMLUtils.escape(val));

					field.addChild(value);
				}
			}
			x.addChild(field);
		}
	}

	public static void addFieldValue(final Element el, final String f_name, final String f_value) {
		Element x = el.getChild("x", "jabber:x:data");

		if (x == null) {
			x = addDataForm(el, DataType.submit);
		}

		Element field = new Element(FIELD_EL, new Element[]{new Element(VALUE_EL, XMLUtils.escape(f_value))},
									new String[]{"var"}, new String[]{XMLUtils.escape(f_name)});

		x.addChild(field);
	}

	public static void addFieldValue(final Element el, final String f_name, final String f_value, final String label,
									 final String[] labels, final String[] options) {
		Element x = el.getChild("x", "jabber:x:data");

		if (x == null) {
			x = addDataForm(el, DataType.submit);
		}

		Element field = new Element(FIELD_EL, new Element[]{new Element(VALUE_EL, XMLUtils.escape(f_value))},
									new String[]{"var", "type", "label"},
									new String[]{XMLUtils.escape(f_name), "list-single", XMLUtils.escape(label)});

		for (int i = 0; i < labels.length; i++) {
			field.addChild(new Element("option", new Element[]{new Element(VALUE_EL, XMLUtils.escape(options[i]))},
									   new String[]{"label"}, new String[]{XMLUtils.escape(labels[i])}));
		}
		x.addChild(field);
	}

	public static void addFieldValue(final Element el, final String f_name, final String[] f_values, final String label,
									 final String[] labels, final String[] options) {
		Element x = el.getChild("x", "jabber:x:data");

		if (x == null) {
			x = addDataForm(el, DataType.submit);
		}

		Element field = new Element(FIELD_EL, new String[]{"var", "type", "label"},
									new String[]{XMLUtils.escape(f_name), "list-multi", XMLUtils.escape(label)});

		for (int i = 0; i < labels.length; i++) {
			field.addChild(new Element("option", new Element[]{new Element(VALUE_EL, XMLUtils.escape(options[i]))},
									   new String[]{"label"}, new String[]{XMLUtils.escape(labels[i])}));
		}
		for (int i = 0; i < f_values.length; i++) {
			field.addChild(new Element(VALUE_EL, XMLUtils.escape(f_values[i])));
		}
		x.addChild(field);
	}

	public static void addFieldValue(final Element el, final String f_name, final String f_value, final String label,
									 final String[] labels, final String[] options, final String type) {
		Element x = el.getChild("x", "jabber:x:data");

		if (x == null) {
			x = addDataForm(el, DataType.submit);
		}

		Element field = new Element(FIELD_EL, new Element[]{new Element(VALUE_EL, XMLUtils.escape(f_value))},
									new String[]{"var", "type", "label"},
									new String[]{XMLUtils.escape(f_name), type, XMLUtils.escape(label)});

		for (int i = 0; i < labels.length; i++) {
			field.addChild(new Element("option", new Element[]{new Element(VALUE_EL, XMLUtils.escape(options[i]))},
									   new String[]{"label"}, new String[]{XMLUtils.escape(labels[i])}));
		}
		x.addChild(field);
	}

	public static void addFieldValue(final Element el, final String f_name, final String f_value, final String type) {
		Element x = el.getChild("x", "jabber:x:data");

		if (x == null) {
			x = addDataForm(el, DataType.submit);
		}

		Element field = new Element(FIELD_EL, new Element[]{new Element(VALUE_EL, XMLUtils.escape(f_value))},
									new String[]{"var", "type"}, new String[]{XMLUtils.escape(f_name), type});

		x.addChild(field);
	}

	public static void addFieldValue(final Element el, final String f_name, final String f_value, final String type,
									 final String label) {
		Element x = el.getChild("x", "jabber:x:data");

		if (x == null) {
			x = addDataForm(el, DataType.submit);
		}

		Element field = new Element(FIELD_EL, new Element[]{new Element(VALUE_EL, XMLUtils.escape(f_value))},
									new String[]{"var", "type", "label"},
									new String[]{XMLUtils.escape(f_name), type, XMLUtils.escape(label)});

		x.addChild(field);
	}

	public static void addHiddenField(final Element el, String f_name, String f_value) {
		addFieldValue(el, f_name, f_value, "hidden");
	}

	public static void addInstructions(final Element el, final String instructions) {
		Element x = el.getChild("x", "jabber:x:data");

		if (x == null) {
			x = addDataForm(el, DataType.submit);
		}
		x.addChild(new Element("instructions", instructions));
	}

	public static void addTextField(final Element el, String f_name, String f_value) {
		addFieldValue(el, f_name, f_value, "fixed");
	}

	public static void addTitle(final Element el, final String title) {
		Element x = el.getChild("x", "jabber:x:data");

		if (x == null) {
			x = addDataForm(el, DataType.submit);
		}
		x.addChild(new Element("title", title));
	}

	public static String getFieldKeyStartingWith(final Element el, String f_name) {
		Element x = el.getChild("x", "jabber:x:data");

		if (x != null) {
			List<Element> children = x.getChildren();

			if (children != null) {
				for (Element child : children) {
					if (child.getName().equals(FIELD_EL) && child.getAttributeStaticStr("var").startsWith(f_name)) {
						return child.getAttributeStaticStr("var");
					}
				}
			}
		}

		return null;
	}

	public static String getFieldValue(final Element el, String f_name) {
		Element x = el.getChild("x", "jabber:x:data");

		if (x != null) {
			List<Element> children = x.getChildren();

			if (children != null) {
				for (Element child : children) {
					if (child.getName().equals(FIELD_EL) && child.getAttributeStaticStr("var").equals(f_name)) {
						String value = child.getChildCDataStaticStr(FIELD_VALUE_PATH);

						if (value != null) {
							return XMLUtils.unescape(value);
						}
					}
				}
			}
		}

		return null;
	}

	public static String[] getFieldValues(final Element el, final String f_name) {
		Element x = el.getChild("x", "jabber:x:data");

		if (x != null) {
			List<Element> children = x.getChildren();

			if (children != null) {
				for (Element child : children) {
					if (child.getName().equals(FIELD_EL) && child.getAttributeStaticStr("var").equals(f_name)) {
						List<String> values = new LinkedList<String>();
						List<Element> val_children = child.getChildren();

						if (val_children != null) {
							for (Element val_child : val_children) {
								if (val_child.getName().equals(VALUE_EL)) {
									String value = val_child.getCData();

									if (value != null) {
										values.add(XMLUtils.unescape(value));
									}
								}
							}
						}

						return values.toArray(new String[0]);
					}
				}
			}
		}

		return null;
	}

	public static boolean removeFieldValue(final Element el, final String f_name) {
		Element x = el.getChild("x", "jabber:x:data");

		if (x != null) {
			List<Element> children = x.getChildren();

			if (children != null) {
				for (Element child : children) {
					if (child.getName().equals(FIELD_EL) && child.getAttributeStaticStr("var").equals(f_name)) {
						return x.removeChild(child);
					}
				}
			}
		}

		return false;
	}

	public static class Builder {

		private final Element x;

		public Builder(Element parent, DataType type) {
			x = Optional.ofNullable(parent.getChild("x", "jabber:x:data")).orElseGet(() -> createDataEl(parent));
			x.setAttribute("type", type.name());
		}

		public Builder addTitle(String title) {
			Element old;
			while ((old = x.getChild("title")) != null) {
				x.removeChild(old);
			}
			if (title != null) {
				x.addChild(new Element("title", title));
			}
			return this;
		}

		public Builder addInstructions(String[] instructions) {
			List<Element> oldValues = x.mapChildren(el -> el.getName() == "instructions", Function.identity());
			if (oldValues != null) {
				for (Element oldValue : oldValues) {
					x.removeChild(oldValue);
				}
			}
			if (instructions != null) {
				for (String instruction : instructions) {
					x.addChild(new Element("instructions", instruction));
				}
			}
			return this;
		}

		public Field.Builder addField(FieldType type, String var) {
			return new Field.Builder(x, type, var);
		}

		public Builder withFields(Consumer<Builder> consumer) {
			consumer.accept(this);
			return this;
		}

		public Builder withReported(Consumer<Reported.Builder> consumer) {
			Reported.Builder builder = new Reported.Builder(x);
			consumer.accept(builder);
			builder.build();
			return this;
		}

		public Builder withItem(Consumer<Item.Builder> consumer) {
			Item.Builder builder = new Item.Builder(x);
			consumer.accept(builder);
			builder.build();
			return this;
		}

		public Element build() {
			return x;
		}

		private static Element createDataEl(Element parent) {
			Element dataEl = new Element("x", "jabber:x:data");
			parent.addChild(dataEl);
			return dataEl;
		}
	}

	public static class Reported {

		public static class Builder {

			private final Element x;
			private final Element reported;

			public Builder(Element x) {
				this.x = x;
				this.reported = new Element("reported");
			}

			public Field.Builder addField(FieldType type, String var) {
				return new Field.Builder(reported, type, var);
			}

			public Builder withFields(Consumer<Reported.Builder> consumer) {
				consumer.accept(this);
				return this;
			}

			public Element build() {
				if (x != null) {
					x.addChild(reported);
				}
				return reported;
			}
		}
	}

	public static class Item {

		public static class Builder {

			private final Element x;
			private final Element item;

			public Builder(Element x) {
				this.x = x;
				this.item = new Element("item");
			}

			public Field.Builder addField(String var) {
				return new Field.Builder(item, null, var);
			}

			public Builder withFields(Consumer<Item.Builder> consumer) {
				consumer.accept(this);
				return this;
			}

			public Element build() {
				if (x != null) {
					x.addChild(item);
				}
				return item;
			}
		}
	}

	public static class Field {

		public static class Builder {

			private final Element parent;
			private final Element el;
			private final FieldType type;

			public Builder(Element form, FieldType type, String var) {
				this.parent = form;
				this.type = Optional.ofNullable(type).orElse(FieldType.TextSingle);
				this.el = new Element("field");
				el.setAttribute("var", var);
				if (type != null) {
					this.el.setAttribute("type", type.value());
				}
			}
			
			public Builder setLabel(String label) {
				if (label == null) {
					el.removeAttribute("label");
				} else {
					el.setAttribute("label", label);
				}
				return this;
			}

			public Builder setDesc(String desc) {
				removeChildren("desc");
				if (desc != null) {
					el.addChild(new Element("desc", desc));
				}
				return this;
			}

			public Builder setRequired(boolean required) {
				removeChildren("required");
				if (required) {
					el.addChild(new Element("required"));
				}
				return this;
			}

			public Builder addOption(String value) {
				this.addOption(value, null);
				return this;
			}

			public Builder addOption(String value, String label) {
				switch (type) {
					case ListMulti:
					case ListSingle:
						break;
					default:
						throw new UnsupportedOperationException("Invalid field type!");
				}
				Element option = new Element("option");
				if (label != null) {
					option.setAttribute("label", label);
				}
				option.addChild(new Element("value", value));
				el.addChild(option);
				return this;
			}
			
			public Builder setOptions(String[] values) {
				return setOptions(values, null);
			}

			public Builder setOptions(String[] values, String[] labels) {
				for (int i=0; i<values.length; i++) {
					addOption(values[i], labels == null ? null : labels[i]);
				}
				return this;
			}

			public Builder setValue(Boolean value) {
				return setValue(value == null ? null : (value ? "true" : "false"));
			}

			public Builder setValue(String value) {
				switch (type) {
					case JidMulti:
					case ListMulti:
					case TextMulti:
						throw new UnsupportedOperationException("Invalid field type!");
					default:
						break;
				}
				removeOldValues();
				if (value != null) {
					el.addChild(new Element("value", value));
				}
				return this;
			}

			public Builder setValues(String[] values) {
				switch (type) {
					case Boolean:
					case Fixed:
					case Hidden:
					case JidSingle:
					case ListSingle:
					case TextSingle:
					case TextPrivate:
						throw new UnsupportedOperationException("Invalid field type!");
					default:
						break;
				}
				removeOldValues();
				if (values != null) {
					for (String value : values) {
						el.addChild(new Element("value", value));
					}
				}
				return this;
			}

			public Element build() {
				if (parent != null) {
					parent.addChild(el);
				}
				return el;
			}

			private void removeOldValues() {
				removeChildren(FIELD_EL);
			}

			private void removeChildren(String name) {
				List<Element> oldValues = el.mapChildren(el -> el.getName() == name, Function.identity());
				if (oldValues != null) {
					for (Element oldValue : oldValues) {
						el.removeChild(oldValue);
					}
				}
			}

		}

	}
	
	public enum FieldType  {
		Boolean,
		Fixed,
		Hidden,
		JidMulti,
		JidSingle,
		ListMulti,
		ListSingle,
		TextMulti,
		TextPrivate,
		TextSingle;

		public String value() {
			switch (this) {
				case Boolean:
					return "boolean";
				case Fixed:
					return "fixed";
				case Hidden:
					return "hidden";
				case JidMulti:
					return "jid-multi";
				case JidSingle:
					return "jid-single";
				case ListMulti:
					return "list-multi";
				case ListSingle:
					return "list-single";
				case TextMulti:
					return "text-multi";
				case TextPrivate:
					return "text-private";
				case TextSingle:
					return "text-single";
			}
			return null;
		}
	}

}
