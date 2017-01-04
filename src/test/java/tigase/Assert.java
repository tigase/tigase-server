package tigase;

import tigase.xml.Element;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Class implementing assertions for custom classes.
 * <p>
 * Created by andrzej on 04.01.2017.
 */
public class Assert {

	/**
	 * Method compares if actual element matches expected one.
	 * <p>
	 * Warning: Actual element must have attributes and children which are part of expected element, however may contain
	 * addition elements or attributes and assertion will not fail.
	 *
	 * @param expected
	 * @param actual
	 */
	public static void assertElementEquals(Element expected, Element actual) {
		assertElementEquals("", expected, actual);
	}

	public static void assertElementEquals(String message, Element expected, Element actual) {
		assertTrue(message + ": expected: " + expected + " but was:" + actual, equals(expected, actual));
	}

	public static boolean equals(Element expected, Element actual) {
		if (expected.getName() != actual.getName()) {
			return false;
		}

		Map<String, String> expAttributes = expected.getAttributes();
		if (expAttributes == null) {
			expAttributes = new IdentityHashMap<>();
		}
		Map<String, String> actAttributes = actual.getAttributes();
		if (actAttributes == null) {
			actAttributes = new IdentityHashMap<>();
		} else {
			actAttributes = new IdentityHashMap<>(actAttributes);
			Iterator<String> it = actAttributes.keySet().iterator();
			while (it.hasNext()) {
				String key = it.next();
				if (!expAttributes.containsKey(key)) {
					it.remove();
				}
			}
		}
		if (!expAttributes.equals(actAttributes)) {
			return false;
		}
		List<Element> expChildren = expected.getChildren();
		if (expChildren != null) {
			if (!expChildren.stream()
					.allMatch(expChild -> actual.findChild(actChild -> equals(expChild, actChild)) != null)) {
				return false;
			}
		}
		return true;
	}

}
