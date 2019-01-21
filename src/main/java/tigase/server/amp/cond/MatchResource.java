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
package tigase.server.amp.cond;

import tigase.server.Packet;
import tigase.server.amp.ConditionIfc;
import tigase.xml.Element;
import tigase.xmpp.jid.JID;

import java.util.logging.Logger;

/**
 * Created: Apr 27, 2010 5:36:54 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MatchResource
		implements ConditionIfc {

	private static final String name = "match-resource";
	private static Logger log = Logger.getLogger(MatchResource.class.getName());

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean match(Packet packet, Element rule) {
		String value = rule.getAttributeStaticStr("value");
		boolean result = false;

		if (value != null) {
			try {
				MatchValue m_val = MatchValue.valueOf(value);
				String jid_resource = (packet.getStanzaTo() != null) ? packet.getStanzaTo().getResource() : null;
				String target_resource = packet.getAttributeStaticStr(TO_RES);
				String from_session_jid_string = packet.getAttributeStaticStr(SESSION_JID);
				JID from_original_jid =
						from_session_jid_string != null ? JID.jidInstance(from_session_jid_string) : null;

				switch (m_val) {
					case any:
						result = true;

						break;

					case other:
						result = ((jid_resource != null) && (target_resource != null) &&
								!jid_resource.equals(target_resource))

								|| (from_original_jid == null && target_resource == null);

						break;

					case exact:
						result = (jid_resource != null) && jid_resource.equals(target_resource);

						break;
				}
			} catch (Exception e) {
				log.info("Incorrect " + name + " condition value for rule: " + rule);
			}
		} else {
			log.info("No value set for rule: " + rule);
		}

		return result;
	}

	private enum MatchValue {
		any,
		exact,
		other;
	}
}
