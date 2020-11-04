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

import tigase.xml.Element;
import tigase.xmpp.jid.BareJID;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Classes implementing this interface should be beans registered in `SessionManager`s kernel to receive calls when
 * spammer is being reported by the user.
 */
public interface SpamReportsConsumer {

	static final String XMLNS_PREFIX = "urn:xmpp:reporting:";
	static final String XMLNS = XMLNS_PREFIX + "0";

	static final Element[] FEATURES = Stream.of("0", "abuse:0", "spam:0")
			.map(suffix -> XMLNS_PREFIX + suffix)
			.map(var -> new Element("feature", new String[]{"var"}, new String[]{var}))
			.toArray(Element[]::new);

	/**
	 * Method called when a user reports JID as a spammer
	 * @param jid - jid of the spammer
	 * @param type - type of the abuse
	 * @return
	 */
	boolean spamReportedFrom(BareJID jid, ReportType type);

	enum ReportType {
		abuse, spam;

		private static Map<String, ReportType> VALUES = Arrays.stream(ReportType.values())
				.collect(Collectors.toMap(it -> it.name(), Function.identity()));

		public static ReportType fromReport(Element report) {
			List<Element> children = report.getChildren();
			if (children == null) {
				return null;
			}
			for (Element child : children) {
				ReportType type = ReportType.fromElement(child);
				if (type != null) {
					return type;
				}
			}
			return null;
		}

		public static ReportType fromElement(Element element) {
			return VALUES.get(element.getName());
		}
	}
}
