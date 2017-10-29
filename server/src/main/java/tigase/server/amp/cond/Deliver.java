/*
 * Deliver.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;
import tigase.server.amp.ConditionIfc;
import tigase.xml.Element;

import java.util.logging.Logger;

//~--- JDK imports ------------------------------------------------------------

/**
 * Created: Apr 27, 2010 5:36:27 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Deliver
		implements ConditionIfc {

	private static final String name = "deliver";
	/**
	 * Private logger for class instances.
	 */
	private static Logger log = Logger.getLogger(Deliver.class.getName());

	//~--- fields ---------------------------------------------------------------
	private boolean offline_storage = true;

	//~--- constant enums -------------------------------------------------------

	/**
	 * Constructs ...
	 */
	public Deliver() {
		String off_val = System.getProperty(MSG_OFFLINE_PROP_KEY);

		offline_storage = (off_val == null) || Boolean.parseBoolean(off_val);
	}

	//~--- constructors ---------------------------------------------------------

	@Override
	public String getName() {
		return name;
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public boolean match(Packet packet, Element rule) {
		String value = rule.getAttributeStaticStr("value");
		boolean result = false;

		if (value != null) {
			try {
				MatchValue m_val = MatchValue.valueOf(value);

				switch (m_val) {
					case direct:
						result = (packet.getAttributeStaticStr(OFFLINE) == null) &&
								(packet.getAttributeStaticStr(FROM_CONN_ID) == null);

						break;

					case forward:

						// Forwarding not supported in Tigase yet
						break;

					case gateway:

						// This can be only determined by the gateway itself
						break;

					case none:
						result = (packet.getAttributeStaticStr(OFFLINE) != null) && !offline_storage;

						break;

					case stored:
						result = (packet.getAttributeStaticStr(OFFLINE) != null) && offline_storage;

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

	//~--- methods --------------------------------------------------------------

	private enum MatchValue {
		direct,
		forward,
		gateway,
		none,
		stored;
	}
}

//~ Formatted in Tigase Code Convention on 13/02/20
