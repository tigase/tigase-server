/*
 * MatchResource.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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



package tigase.server.amp.cond;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.amp.ConditionIfc;
import tigase.server.Packet;

import tigase.xmpp.JID;

import tigase.xml.Element;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;
import java.util.Map;

/**
 * Created: Apr 27, 2010 5:36:54 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MatchResource
				implements ConditionIfc {
	/**
	 * Private logger for class instances.
	 */
	private static Logger log        = Logger.getLogger(MatchResource.class.getName());
	private static final String name = "match-resource";

	//~--- constant enums -------------------------------------------------------

	private enum MatchValue { any, exact, other; }

	//~--- get methods ----------------------------------------------------------

	@Override
	public String getName() {
		return name;
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public boolean match(Packet packet, Element rule) {
		String value   = rule.getAttributeStaticStr("value");
		boolean result = false;

		if (value != null) {
			try {
				MatchValue m_val       = MatchValue.valueOf(value);
				String jid_resource    = (packet.getStanzaTo() != null)
																 ? packet.getStanzaTo().getResource()
																 : null;
				String target_resource = packet.getAttributeStaticStr(TO_RES);
				String from_session_jid_string = packet.getAttributeStaticStr(SESSION_JID);
				JID from_original_jid = from_session_jid_string != null ? 
																JID.jidInstance( from_session_jid_string)
																: null;

				switch (m_val) {
				case any :
					result = true;

					break;

				case other :
					result = ( ( jid_resource != null ) && ( target_resource != null )
										 && !jid_resource.equals( target_resource ) )
									 
									 || ( from_original_jid == null && target_resource == null )
							;

					break;

				case exact :
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
}
