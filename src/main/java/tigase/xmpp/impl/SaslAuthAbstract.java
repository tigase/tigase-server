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

package tigase.xmpp.impl;

import tigase.auth.*;
import tigase.kernel.beans.Inject;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.Priority;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.BareJID;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

abstract public class SaslAuthAbstract
		extends AbstractAuthPreprocessor
		implements XMPPProcessorIfc {

	protected final static String SASL_SERVER_KEY = "SASL_SERVER_KEY";
	private static final String SASL_SUCCESS_ELEMENT_NAME = "success";
	private static final String SASL_FAILURE_ELEMENT_NAME = "failure";
	protected final static String ALLOWED_SASL_MECHANISMS_KEY = "allowed-sasl-mechanisms";

	private static final Logger log = Logger.getLogger(SaslAuthAbstract.class.getName());

	protected final Map<String, Object> props = new HashMap<>();
	@Inject
	protected TigaseSaslProvider saslProvider;

	@Override
	public int concurrentQueuesNo() {
		return super.concurrentQueuesNo() * 4;
	}

	protected Packet createSaslErrorResponse(XmppSaslException.SaslError error, String message, Packet packet) {
		Element failure = new Element(SASL_FAILURE_ELEMENT_NAME);
		failure.setXMLNS(getXmlns());
		failure.addChild((error == null ? XmppSaslException.SaslError.not_authorized : error).getElement());
		if (message != null) {
			failure.addChild(new Element("text", message, new String[]{"xml:lang"}, new String[]{"en"}));
		}

		Packet response = packet.swapFromTo(failure, null, null);
		response.setPriority(Priority.SYSTEM);
		return response;
	}

	/**
	 * Tries to extract BareJID of user who try to log in.
	 */
	@Override
	protected BareJID extractUserJid(final Exception e, XMPPResourceConnection session) {
		BareJID jid = null;

		if (e instanceof SaslInvalidLoginExcepion) {
			String t = ((SaslInvalidLoginExcepion) e).getJid();
			jid = t == null ? null : BareJID.bareJIDInstanceNS(t);
		}

		if (jid != null) {
			jid = super.extractUserJid(e, session);
		}

		return jid;
	}

	abstract protected String getXmlns();

	protected void onAuthFail(final XMPPResourceConnection session) {
		session.removeSessionData(SASL_SERVER_KEY);
	}

	protected void processSessionAlreadyAuthorized(Packet packet, XMPPResourceConnection session,
	                                               Queue<Packet> results) {
		// Multiple authentication attempts
		// Another authentication request on already authenticated connection
		// This is not allowed and must be forbidden
		Packet res = createSaslErrorResponse(null, null, packet);

		// Make sure it gets delivered before stream close
		res.setPriority(Priority.SYSTEM);
		results.offer(res);

		// Optionally close the connection to make sure there is no
		// confusion about the connection state.
		results.offer(
				Command.CLOSE.getPacket(packet.getTo(), packet.getFrom(), StanzaType.set, session.nextStanzaId()));
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Discovered second authentication attempt: {0}, packet: {1}",
			        new Object[]{session.toString(), packet.toString()});
		}
		try {
			session.logout();
		} catch (NotAuthorizedException ex) {
			log.log(Level.FINER, "Unsuccessful session logout: {0}", session.toString());
		}
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Session after logout: {0}", session.toString());
		}
	}

	protected abstract void processSuccess(Packet packet, XMPPResourceConnection session, String challengeData,
	                                       Queue<Packet> results);

}
