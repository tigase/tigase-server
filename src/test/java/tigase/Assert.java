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
package tigase;

import tigase.xml.Element;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Class implementing assertions for custom classes.
 * <br>
 * Created by andrzej on 04.01.2017.
 */
public class Assert {

	/**
	 * Method compares if actual element matches expected one.
	 * <br>
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
