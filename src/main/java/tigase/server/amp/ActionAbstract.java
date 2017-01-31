/*
 * ActionAbstract.java
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



package tigase.server.amp;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.RepositoryFactory;
import tigase.db.UserRepository;

import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterElement;
import tigase.xmpp.impl.roster.RosterFlat;
import tigase.xmpp.JID;
import tigase.xmpp.PacketErrorTypeException;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

/**
 * Created: May 1, 2010 7:44:17 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class ActionAbstract
				implements ActionIfc {
	/** Field description */
	public static final String AMP_SECURITY_LEVEL = "--amp-security-level";

	/** Field description */
	public static final String AMP_SECURITY_LEVEL_DEFAULT = "STRICT";

	/** Field description */
	public static final String SECURITY_PROP_KEY = "security-level";
	private static Logger log                    =
		Logger.getLogger(ActionAbstract.class.getName());

	//~--- fields ---------------------------------------------------------------

	/** Field description */
	protected ActionResultsHandlerIfc resultsHandler = null;
	private SECURITY security                        = SECURITY.STRICT;
	private UserRepository user_repository           = null;
	RosterFlat rosterUtil                            = new RosterFlat();

	//~--- constant enums -------------------------------------------------------

	private enum SECURITY {

		NONE, PERFORMANCE, STRICT
	};

	//~--- get methods ----------------------------------------------------------

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defs = new LinkedHashMap<String, Object>();
		String sec_str           = (String) params.get(AMP_SECURITY_LEVEL);

		if (null == sec_str) {
			sec_str = AMP_SECURITY_LEVEL_DEFAULT;
		}
		try {
			SECURITY sec = SECURITY.valueOf(sec_str.toUpperCase());

			security = sec;
		} catch (Exception e) {}
		defs.put(SECURITY_PROP_KEY, security.name());

		return defs;
	}

	//~--- set methods ----------------------------------------------------------

	@Override
	public void setProperties(Map<String, Object> props,
														ActionResultsHandlerIfc resultsHandler) {
		this.resultsHandler = resultsHandler;

		String sec_str = (String) props.get(SECURITY_PROP_KEY);

		try {
			SECURITY sec = SECURITY.valueOf(sec_str.toUpperCase());

			security = sec;
		} catch (NullPointerException e) {

			// Ignore, this is expected here
		} catch (Exception e) {
			log.log(Level.WARNING,
							"Incorrect amp security settings, using defaults: " + security, e);
		}

		// Is there shared user repository instance? If so I want to use it:
		user_repository = (UserRepository) props.get(RepositoryFactory.SHARED_USER_REPO_PROP_KEY);
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param rule
	 *
	 * 
	 *
	 * @throws PacketErrorTypeException
	 */
	protected Packet prepareAmpPacket(Packet packet, Element rule)
					throws PacketErrorTypeException {
		boolean error_result = false;

		switch (security) {
		case NONE :
			break;

		case PERFORMANCE :
			error_result = true;

			break;

		case STRICT :
			error_result = !checkUserRoster(packet.getStanzaTo(), packet.getStanzaFrom());

			break;
		}

		Packet result = null;

		if (error_result) {
			result = Authorization.NOT_ACCEPTABLE.getResponseMessage(packet, "Subscription between users not valid", false);
		} else {
			JID old_from        = packet.getStanzaFrom();
			JID old_to          = packet.getStanzaTo();
			String from_conn_id = packet.getAttributeStaticStr(FROM_CONN_ID);
			JID new_from        = null;

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
			amp = new Element("amp", new Element[] { rule }, new String[] { "from", "to",
							"xmlns", "status" }, new String[] { old_from.toString(), old_to.toString(),
							AMP_XMLNS, getName() });
			result.getElement().addChild(amp);
			removeTigasePayload(result);
			if (from_conn_id != null) {
				result.setPacketTo(JID.jidInstanceNS(from_conn_id));
			}
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 */
	protected void removeTigasePayload(Packet packet) {
		packet.getElement().removeAttribute(TO_CONN_ID);
		packet.getElement().removeAttribute(TO_RES);
		packet.getElement().removeAttribute(OFFLINE);
		packet.getElement().removeAttribute(FROM_CONN_ID);
		packet.getElement().removeAttribute(SESSION_JID);
		packet.getElement().removeAttribute(EXPIRED);
	}

	private boolean checkUserRoster(JID user, JID contact) {

		if ( user.getBareJID().equals( contact.getBareJID() ) ){
			// this is the same user, no point in checking sub
			return true;
		}

		try {
			String roster_str = user_repository.getData(user.getBareJID(),
														RosterAbstract.ROSTER);

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
}


//~ Formatted in Tigase Code Convention on 13/02/20
