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
package tigase.server.amp;

import tigase.annotations.TigaseDeprecated;
import tigase.db.UserRepository;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigAlias;
import tigase.kernel.beans.config.ConfigAliases;
import tigase.kernel.beans.config.ConfigField;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterElement;
import tigase.xmpp.impl.roster.RosterFlat;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: May 1, 2010 7:44:17 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@ConfigAliases({@ConfigAlias(field = "security", alias = "amp-security-level")})
public abstract class ActionAbstract
		implements ActionIfc {

	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	public static final String AMP_SECURITY_LEVEL = "--amp-security-level";

	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	public static final String AMP_SECURITY_LEVEL_DEFAULT = "STRICT";

	public static final String SECURITY_PROP_KEY = "security-level";
	private static Logger log = Logger.getLogger(ActionAbstract.class.getName());

	protected ActionResultsHandlerIfc resultsHandler = null;
	RosterFlat rosterUtil = new RosterFlat();
	@ConfigField(alias = "security", desc = "Security level")
	private SECURITY security = SECURITY.STRICT;
	@Inject
	private UserRepository user_repository = null;

	@Override
	public void setActionResultsHandler(ActionResultsHandlerIfc resultsHandler) {
		this.resultsHandler = resultsHandler;
	}

	protected Packet prepareAmpPacket(Packet packet, Element rule) throws PacketErrorTypeException {
		boolean error_result = false;

		switch (security) {
			case NONE:
				break;

			case PERFORMANCE:
				error_result = true;

				break;

			case STRICT:
				error_result = !checkUserRoster(packet.getStanzaTo(), packet.getStanzaFrom());

				break;
		}

		Packet result = null;

		if (error_result) {
			result = Authorization.NOT_ACCEPTABLE.getResponseMessage(packet, "Subscription between users not valid",
																	 false);
		} else {
			JID old_from = packet.getStanzaFrom();
			JID old_to = packet.getStanzaTo();
			String from_conn_id = packet.getAttributeStaticStr(FROM_CONN_ID);
			JID new_from = null;

			if (from_conn_id != null) {
				new_from = JID.jidInstanceNS(old_from.getDomain());
			} else {
				new_from = JID.jidInstanceNS(old_to.getDomain());
			}

			// Packet result = Packet.packetInstance(packet.getElement(), new_from, old_from);
			result = packet.copyElementOnly();
			result.initVars(new_from, old_from);

			Element amp = result.getElement().getChild("amp", AMP_XMLNS);

			result.getElement().removeChild(amp);
			amp = new Element("amp", new Element[]{rule}, new String[]{"from", "to", "xmlns", "status"},
							  new String[]{old_from.toString(), old_to.toString(), AMP_XMLNS, getName()});
			result.getElement().addChild(amp);
			removeTigasePayload(result);
			if (from_conn_id != null) {
				result.setPacketTo(JID.jidInstanceNS(from_conn_id));
			}
		}

		return result;
	}

	protected void removeTigasePayload(Packet packet) {
		packet.getElement().removeAttribute(TO_CONN_ID);
		packet.getElement().removeAttribute(TO_RES);
		packet.getElement().removeAttribute(OFFLINE);
		packet.getElement().removeAttribute(FROM_CONN_ID);
		packet.getElement().removeAttribute(SESSION_JID);
		packet.getElement().removeAttribute(EXPIRED);
	}

	private boolean checkUserRoster(JID user, JID contact) {

		if (user.getBareJID().equals(contact.getBareJID())) {
			// this is the same user, no point in checking sub
			return true;
		}

		try {
			String roster_str = user_repository.getData(user.getBareJID(), RosterAbstract.ROSTER);

			if (roster_str != null) {
				Map<BareJID, RosterElement> roster = new LinkedHashMap<BareJID, RosterElement>();

				RosterFlat.parseRosterUtil(roster_str, roster, null);

				RosterElement re = roster.get(contact.getBareJID());

				if (re != null) {
					return rosterUtil.isSubscribedFrom(re.getSubscription());
				}
			}
		} catch (Exception ex) {
			log.log(Level.INFO, "Problem retrieving user roster: " + user, ex);
		}

		return false;
	}

	private enum SECURITY {

		NONE,
		PERFORMANCE,
		STRICT
	}
}

