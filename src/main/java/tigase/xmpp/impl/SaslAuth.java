/*
 * SaslAuth.java
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

import tigase.auth.CallbackHandlerFactory;
import tigase.auth.mechanisms.SaslANONYMOUS;
import tigase.auth.MechanismSelector;
import tigase.auth.MechanismSelectorFactory;
import tigase.auth.TigaseSaslProvider;
import tigase.auth.XmppSaslException;
import tigase.auth.XmppSaslException.SaslError;

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;

import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.Priority;

import tigase.util.Base64;

import tigase.xml.Element;

import tigase.xmpp.BareJID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.security.Security;

import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

/**
 * Describe class SaslAuth here.
 *
 *
 * Created: Mon Feb 20 16:28:13 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SaslAuth
				extends AbstractAuthPreprocessor
				implements XMPPProcessorIfc {
	/** Field description */
	public static final String     ID = "urn:ietf:params:xml:ns:xmpp-sasl";
	private static final String    _XMLNS = "urn:ietf:params:xml:ns:xmpp-sasl";
	private final static String    ALLOWED_SASL_MECHANISMS_KEY = "allowed-sasl-mechanisms";
	private static final Element[] DISCO_FEATURES = { new Element("feature", new String[] {
			"var" }, new String[] { _XMLNS }) };
	private static final String[][] ELEMENTS        = {
		{ "auth" }, { "response" }, { "challenge" }, { "failure" }, { "success" }, { "abort" }
	};
	private static final Logger     log = Logger.getLogger(SaslAuth.class.getName());
	private final static String     SASL_SERVER_KEY = "SASL_SERVER_KEY";
	private static final String[]   XMLNSS          = {
		_XMLNS, _XMLNS, _XMLNS, _XMLNS, _XMLNS, _XMLNS
	};

	//~--- fields ---------------------------------------------------------------

	private CallbackHandlerFactory    callbackHandlerFactory = new CallbackHandlerFactory();
	private final Map<String, Object> props                  = new HashMap<String,
			Object>();
	private MechanismSelector         mechanismSelector;

	//~--- constant enums -------------------------------------------------------

	/**
	 * Enum description
	 *
	 */
	public enum ElementType {
		abort, auth, challenge, failure, response, success;
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public int concurrentQueuesNo() {
		return super.concurrentQueuesNo() * 4;
	}

	private Element createReply(final ElementType type, final String cdata) {
		Element reply = new Element(type.toString());

		reply.setXMLNS(_XMLNS);
		if (cdata != null) {
			reply.setCData(cdata);
		}

		return reply;
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void init(Map<String, Object> settings) throws TigaseDBException {
		if (settings != null) {
			props.putAll(settings);
		}
		super.init(settings);

		// we should remove existing tigase.sasl provider if it is not instance of TigaseSaslProvider
		// as it can be loaded from other bundle in OSGi which will cause many issues with instanceof
		// and casting and it is NOT possible to update implementation without removing it first
		if (!(Security.getProvider("tigase.sasl") instanceof TigaseSaslProvider)) {
			Security.removeProvider("tigase.sasl");
		}
		Security.insertProviderAt(new TigaseSaslProvider(settings), 1);

		MechanismSelectorFactory mechanismSelectorFactory = new MechanismSelectorFactory();

		try {
			mechanismSelector = mechanismSelectorFactory.create(settings);
		} catch (Exception e) {
			log.severe("Can't create SASL Mechanism Selector");

			throw new RuntimeException("Can't create SASL Mechanism Selector", e);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 */
	protected void onAuthFail(final XMPPResourceConnection session) {
		session.removeSessionData(SASL_SERVER_KEY);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void process(final Packet packet, final XMPPResourceConnection session,
			final NonAuthUserRepository repo, final Queue<Packet> results, final Map<String,
			Object> settings) {
		if (session == null) {
			return;
		}
		synchronized (session) {

			// If authentication timeout expired, ignore the request....
			if (session.getSessionData(XMPPResourceConnection.AUTHENTICATION_TIMEOUT_KEY) !=
					null) {
				return;
			}
			if (session.isAuthorized()) {

				// Multiple authentication attempts....
				// Another authentication request on already authenticated
				// connection
				// This is not allowed and must be forbidden.
				Packet res = packet.swapFromTo(createReply(ElementType.failure,
						"<not-authorized/>"), null, null);

				// Make sure it gets delivered before stream close
				res.setPriority(Priority.SYSTEM);
				results.offer(res);

				// Optionally close the connection to make sure there is no
				// confusion about the connection state.
				results.offer(Command.CLOSE.getPacket(packet.getTo(), packet.getFrom(), StanzaType
						.set, session.nextStanzaId()));
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST,
							"Discovered second authentication attempt: {0}, packet: {1}",
							new Object[] { session.toString(),
							packet.toString() });
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

			Element request = packet.getElement();

			try {
				SaslServer ss;

				if ("auth" == request.getName()) {
					final String mechanismName = request.getAttributeStaticStr("mechanism");

					if (log.isLoggable(Level.FINEST)) {
						log.finest("Start SASL auth. mechanism=" + mechanismName);
					}

					Collection<String> allowedMechanisms = (Collection<String>) session
							.getSessionData(ALLOWED_SASL_MECHANISMS_KEY);
					session.removeSessionData(ALLOWED_SASL_MECHANISMS_KEY);

					if (allowedMechanisms == null) {
						allowedMechanisms = mechanismSelector.filterMechanisms(Sasl.getSaslServerFactories(), session);
					}
					
					if ((mechanismName == null) || allowedMechanisms == null || !allowedMechanisms.contains(mechanismName)) {
						throw new XmppSaslException(SaslError.invalid_mechanism, "Mechanism '" + mechanismName
								+ "' is not allowed");
					}

					CallbackHandler cbh = callbackHandlerFactory.create(mechanismName, session,
							repo, settings);

					ss = Sasl.createSaslServer(mechanismName, "xmpp", session.getDomain().getVhost()
							.getDomain(), props, cbh);
					if (ss == null) {
						throw new XmppSaslException(SaslError.invalid_mechanism, "Mechanism '" +
								mechanismName + "' is not allowed");
					}
					session.putSessionData(SASL_SERVER_KEY, ss);
				} else if ("response" == request.getName()) {
					ss = (SaslServer) session.getSessionData(SASL_SERVER_KEY);
					if (ss == null) {
						throw new XmppSaslException(SaslError.malformed_request);
					}
				} else {
					throw new XmppSaslException(SaslError.malformed_request,
							"Unrecognized element " + request.getName());
				}

				byte[] data;
				String cdata = request.getCData();

				if ((cdata != null) && (cdata.length() == 1) && cdata.equals("=")) {
					data = new byte[] {};
				} else if ((cdata != null) && (cdata.length() > 0)) {
					data = Base64.decode(cdata);
				} else {
					data = new byte[] {};
				}

				byte[] challenge = ss.evaluateResponse(data);
				String challengeData;

				if (challenge != null) {
					challengeData = Base64.encode(challenge);
				} else {
					challengeData = null;
				}
				if (ss.isComplete() && (ss.getAuthorizationID() != null)) {
					BareJID jid;

					if (ss.getAuthorizationID().contains("@")) {
						jid = BareJID.bareJIDInstance(ss.getAuthorizationID());
					} else {
						jid = BareJID.bareJIDInstance(ss.getAuthorizationID(), session.getDomain()
								.getVhost().getDomain());
					}
					if (log.isLoggable(Level.FINE)) {
						log.finest("Authorized as " + jid);
					}

					boolean anonymous;

					try {
						Boolean x = (Boolean) ss.getNegotiatedProperty(SaslANONYMOUS
								.IS_ANONYMOUS_PROPERTY);

						anonymous = (x == null)
								? false
								: x.booleanValue();
					} catch (Exception e) {
						anonymous = false;
					}
					session.removeSessionData(SASL_SERVER_KEY);
					session.authorizeJID(jid, anonymous);
					results.offer(packet.swapFromTo(createReply(ElementType.success,
							challengeData), null, null));
				} else if (!ss.isComplete()) {
					results.offer(packet.swapFromTo(createReply(ElementType.challenge,
							challengeData), null, null));
				} else {
					throw new XmppSaslException(SaslError.malformed_request);
				}
			} catch (XmppSaslException e) {
				onAuthFail(session);
				if (log.isLoggable(Level.FINER)) {
					log.log(Level.FINER, "SASL unsuccessful", e);
				}

				String el;

				if (e.getSaslErrorElementName() != null) {
					el = "<" + e.getSaslErrorElementName() + "/>";
				} else {
					el = "<not-authorized/>";
				}
				if (e.getMessage() != null) {
					el += "<text xml:lang='en'>" + e.getMessage() + "</text>";
				}

				Packet response = packet.swapFromTo(createReply(ElementType.failure, el), null,
						null);

				response.setPriority(Priority.SYSTEM);
				results.offer(response);
			} catch (SaslException e) {
				onAuthFail(session);
				if (log.isLoggable(Level.FINER)) {
					log.log(Level.FINER, "SASL unsuccessful", e);
				}

				Packet response = packet.swapFromTo(createReply(ElementType.failure,
						"<not-authorized/>"), null, null);

				response.setPriority(Priority.SYSTEM);
				results.offer(response);
			} catch (Exception e) {
				onAuthFail(session);
				if (log.isLoggable(Level.WARNING)) {
					log.log(Level.WARNING, "Problem with SASL", e);
				}

				Packet response = packet.swapFromTo(createReply(ElementType.failure,
						"<temporary-auth-failure/>"), null, null);

				response.setPriority(Priority.SYSTEM);
				results.offer(response);
			}
		}
	}

	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	@Override
	public String[][] supElementNamePaths() {
		return ELEMENTS;
	}

	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	@Override
	public Element[] supStreamFeatures(final XMPPResourceConnection session) {
		if ((session == null) || session.isAuthorized()) {
			return null;
		} else {
			Collection<String> auth_mechs = mechanismSelector.filterMechanisms(Sasl
					.getSaslServerFactories(), session);
			Element[] mechs = new Element[auth_mechs.size()];
			int       idx   = 0;

			session.putSessionData(ALLOWED_SASL_MECHANISMS_KEY, auth_mechs);
			for (String mech : auth_mechs) {
				mechs[idx++] = new Element("mechanism", mech);
			}

			return new Element[] { new Element("mechanisms", mechs, new String[] { "xmlns" },
					new String[] { _XMLNS }) };
		}
	}
	
}


//~ Formatted in Tigase Code Convention on 13/03/12
