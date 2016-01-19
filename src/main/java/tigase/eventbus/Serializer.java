package tigase.eventbus;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import tigase.kernel.BeanUtils;
import tigase.kernel.TypesConverter;
import tigase.xml.Element;

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
						value = TypesConverter.convert(v.getCData(), f.getType());
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
					v.setCData(TypesConverter.toString(value));
				}
				e.addChild(v);
			} catch (IllegalAccessException | InvocationTargetException caught) {
				caught.printStackTrace();
			}
		}

		return e;
	}

}
