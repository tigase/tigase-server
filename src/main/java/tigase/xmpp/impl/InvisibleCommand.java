/*
 * InvisibleCommand.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
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



package tigase.xmpp.impl;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;

import tigase.server.Iq;
import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.Authorization;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterFactory;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPPreprocessorIfc;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

import static tigase.xmpp.StanzaType.set;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;

/**
 * Class InvisibleCommand implements XEP-0186 Invisible Command support
 *
 * @see <a href="http://xmpp.org/extensions/xep-0186.html">XEP-0186</a>
 *
 * @author andrzej
 */
public class InvisibleCommand
				extends XMPPProcessor
				implements XMPPProcessorIfc, XMPPPreprocessorIfc {
	private static final String ID = "invisible-command";
	private static final Logger log = Logger.getLogger(InvisibleCommand.class
			.getCanonicalName());
	private static final String     XMLNS          = "urn:xmpp:invisible:0";
	private static final String[]   VISIBLE_PATH   = { Iq.ELEM_NAME, "visible" };
	private static final String[]   INVISIBLE_PATH = { Iq.ELEM_NAME, "invisible" };
	private static final String[][] ELEMENT_PATHS  = {
		INVISIBLE_PATH, VISIBLE_PATH
	};
	private static final String[]   XMLNSS         = { XMLNS, XMLNS };
	private static final String     ACTIVE_KEY     = ID + "-active";

	//~--- fields ---------------------------------------------------------------

	/** Field description */
	protected RosterAbstract roster_util = getRosterUtil();

	//~--- methods --------------------------------------------------------------

	@Override
	public String id() {
		return ID;
	}

	@Override
	public boolean preProcess(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) {

		// stop presence broadcast if invisibility is activated - only offline should be allowed to broadcast it to buddies with direct stanza sent
		if ((packet.getElemName() == "presence") && (packet.getStanzaTo() == null) && (packet
				.getType() != StanzaType.unavailable)) {
			Boolean active = (Boolean) session.getSessionData(ACTIVE_KEY);

			active = (active != null) && active;
			if (active) {
				packet.processedBy(ID);
			}

			return active;
		}

		return false;
	}

	@Override
	public void process(final Packet packet, final XMPPResourceConnection session,
			final NonAuthUserRepository repo, final Queue<Packet> results, final Map<String,
			Object> settings) {
		if (session == null) {
			return;
		}
		if (!session.isAuthorized()) {
			try {
				results.offer(session.getAuthState().getResponseMessage(packet,
						"Session is not yet authorized.", false));
			} catch (PacketErrorTypeException ex) {
				log.log(Level.FINEST,
						"ignoring packet from not authorized session which is already of type error");
			}

			return;
		}
		try {
			try {
				StanzaType type = packet.getType();

				switch (type) {
				case set :

					// @todo: need to implement handing
					if (packet.getElement().findChildStaticStr(INVISIBLE_PATH) != null) {

						// invisibility started - set flag
						session.putSessionData(ACTIVE_KEY, Boolean.TRUE);

						// send offline presence
						Element presence = new Element("presence", new String[] { "from", "type" },
								new String[] { session.getJID().toString(),
								"unavailable" });

						session.putSessionData(XMPPResourceConnection.PRESENCE_KEY, presence);
						PresenceState.broadcastOffline(session, results, settings, roster_util);
						session.removeSessionData(PresenceState.OFFLINE_BUD_SENT);

						// session.removeSessionData(XMPPResourceConnection.PRESENCE_KEY);
					} else if (packet.getElement().findChildStaticStr(VISIBLE_PATH) != null) {

						// invisibility left - clear flag
						session.removeSessionData(ACTIVE_KEY);
					}

					// sending result
					results.offer(packet.okResult((Element) null, 0));

					break;

				default :
					results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
							"InvisibleCommand processing type is incorrect", false));
				}
			} catch (NotAuthorizedException ex) {
				results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
						"Not authorized", false));
			} catch (TigaseDBException ex) {
				results.offer(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet,
						"Error accessing database", false));
			}
		} catch (PacketErrorTypeException ex) {
			log.log(Level.SEVERE, "packet error type exception", ex);
		}
	}

	@Override
	public String[][] supElementNamePaths() {
		return ELEMENT_PATHS;
	}

	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>RosterAbstract</code>
	 */
	protected RosterAbstract getRosterUtil() {
		return RosterFactory.getRosterImplementation(true);
	}
}


//~ Formatted in Tigase Code Convention on 13/09/21
