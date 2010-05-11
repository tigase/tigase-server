
/*
* Tigase Jabber/XMPP Server
* Copyright (C) 2004-2010 "Artur Hefczyc" <artur.hefczyc@tigase.org>
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, version 3 of the License.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. Look for COPYING file in the top folder.
* If not, see http://www.gnu.org/licenses/.
*
* $Rev$
* Last modified by $Author$
* $Date$
 */
package tigase.server.amp.action;

//~--- non-JDK imports --------------------------------------------------------

import tigase.conf.Configurable;

import tigase.db.UserNotFoundException;

import tigase.server.Packet;
import tigase.server.amp.ActionAbstract;
import tigase.server.amp.ActionResultsHandlerIfc;
import tigase.server.amp.MsgRepository;

import tigase.xml.Element;

//~--- JDK imports ------------------------------------------------------------

import java.sql.SQLException;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: May 1, 2010 11:32:59 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Store extends ActionAbstract {
	private static final Logger log = Logger.getLogger(Store.class.getName());
	private static final String name = "store";

	//~--- fields ---------------------------------------------------------------

	private boolean offline_storage = true;
	private MsgRepository repo = null;
	private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 */
	public Store() {
		String off_val = System.getProperty(MSG_OFFLINE_PROP_KEY);

		offline_storage = (off_val == null) || Boolean.parseBoolean(off_val);
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param rule
	 * @param resultsHandler
	 *
	 *
	 * @return
	 */
	@Override
	public boolean execute(Packet packet, Element rule, ActionResultsHandlerIfc resultsHandler) {
		if (repo != null) {
			Date expired = null;
			String stamp = null;

			synchronized (formatter) {
				try {
					expired = formatter.parse(rule.getAttribute("value"));
				} catch (Exception e) {
					expired = null;
				}

				stamp = formatter.format(new Date());
			}

			removeTigasePayload(packet);

			try {
				Element elem = packet.getElement();
				Element x = new Element("delay", "Offline Storage", new String[] { "from", "stamp",
						"xmlns" }, new String[] { packet.getStanzaTo().getDomain(), stamp,
						"urn:xmpp:delay" });

				elem.addChild(x);
				repo.storeMessage(packet.getStanzaFrom(), packet.getStanzaTo(), expired, elem);
			} catch (UserNotFoundException ex) {
				log.info("User not found for offline message: " + packet);
			}
		}

		return false;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param params
	 *
	 * @return
	 */
	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defs = new LinkedHashMap<String, Object>();
		String db_uri = (String) params.get(AMP_MSG_REPO_URI_PARAM);

		if (db_uri == null) {
			db_uri = (String) params.get(Configurable.USER_REPO_URL_PROP_KEY);
		}

		if (db_uri != null) {
			defs.put(AMP_MSG_REPO_URI_PROP_KEY, db_uri);
		}

		return defs;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getName() {
		return name;
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param props
	 */
	@Override
	public void setProperties(Map<String, Object> props) {
		String db_uri = (String) props.get(AMP_MSG_REPO_URI_PROP_KEY);

		if (db_uri != null) {
			repo = MsgRepository.getInstance(db_uri);

			try {
				repo.initRepository(db_uri, null);
			} catch (SQLException ex) {
				repo = null;
				log.log(Level.WARNING, "Problem initializing connection to DB: ", ex);
			}
		}
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
