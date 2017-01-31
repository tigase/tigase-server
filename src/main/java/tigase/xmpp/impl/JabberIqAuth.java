/*
 * JabberIqAuth.java
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
import tigase.auth.callbacks.VerifyPasswordCallback;
import tigase.auth.MechanismSelector;
import tigase.auth.MechanismSelectorFactory;
import tigase.auth.TigaseSaslProvider;

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;

import tigase.server.Command;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.Priority;

import tigase.xml.Element;

import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.security.Security;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.Sasl;

/**
 * JEP-0078: Non-SASL Authentication
 *
 *
 * Created: Thu Feb 16 17:46:16 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JabberIqAuth
				extends AbstractAuthPreprocessor
				implements XMPPProcessorIfc {
	private static final String[][] ELEMENT_PATHS = {
		Iq.IQ_QUERY_PATH
	};

	/**
	 * Private logger for class instances.
	 */
	private static final Logger   log = Logger.getLogger(JabberIqAuth.class.getName());
	private static final String   XMLNS  = "jabber:iq:auth";
	private static final String   ID     = XMLNS;
	private static final String[] XMLNSS = { XMLNS };
	private static final String[] IQ_QUERY_USERNAME_PATH = { Iq.ELEM_NAME, Iq.QUERY_NAME,
			"username" };
	private static final String[] IQ_QUERY_RESOURCE_PATH = { Iq.ELEM_NAME, Iq.QUERY_NAME,
			"resource" };
	private static final String[] IQ_QUERY_PASSWORD_PATH = { Iq.ELEM_NAME, Iq.QUERY_NAME,
			"password" };
	private static final String[] IQ_QUERY_DIGEST_PATH = { Iq.ELEM_NAME, Iq.QUERY_NAME,
			"digest" };
	private static final Element[] FEATURES = { new Element("auth", new String[] {
			"xmlns" }, new String[] { "http://jabber.org/features/iq-auth" }) };
	private static final Element[] DISCO_FEATURES = { new Element("feature", new String[] {
			"var" }, new String[] { XMLNS }) };

	//~--- fields ---------------------------------------------------------------

	private CallbackHandlerFactory callbackHandlerFactory = new CallbackHandlerFactory();
	private MechanismSelector      mechanismSelector;

	//~--- methods --------------------------------------------------------------


	@Override
	public void init(Map<String, Object> settings) throws TigaseDBException {

		// we should remove existing tigase.sasl provider if it is not instance of TigaseSaslProvider
		// as it can be loaded from other bundle in OSGi which will cause many issues with instanceof
		// and casting and it is NOT possible to update implementation without removing it first
		if (!(Security.getProvider("tigase.sasl") instanceof TigaseSaslProvider)) {
			Security.removeProvider("tigase.sasl");
		}
		Security.insertProviderAt(new TigaseSaslProvider(settings), 1);
		super.init(settings);

		MechanismSelectorFactory mechanismSelectorFactory = new MechanismSelectorFactory();

		try {
			mechanismSelector = mechanismSelectorFactory.create(settings);
		} catch (Exception e) {
			log.severe("Can't create SASL Mechanism Selector");

			throw new RuntimeException("Can't create SASL Mechanism Selector", e);
		}
	}

	@Override
	public String id() {
		return ID;
	}


	@Override
	public void process(final Packet packet, final XMPPResourceConnection session,
			final NonAuthUserRepository repo, final Queue<Packet> results, final Map<String,
			Object> settings)
					throws XMPPException {
		if (session == null) {
			return;
		}    // end of if (session == null)
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
				Packet res = Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
						"Cannot authenticate twice on the same stream.", false);

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

			Element    request = packet.getElement();
			StanzaType type    = packet.getType();

			switch (type) {
			case get :
				try {
					StringBuilder            response = new StringBuilder("<username/>");
					final Collection<String> auth_mechs = mechanismSelector.filterMechanisms(Sasl
							.getSaslServerFactories(), session);

					if (auth_mechs.contains("PLAIN") || auth_mechs.contains("DIGEST-MD5")) {
						response.append("<password/>");
					}

					// response.append("<digest/>");
					response.append("<resource/>");
					results.offer(packet.okResult(response.toString(), 1));
				} catch (NullPointerException ex) {
					if (log.isLoggable(Level.FINE))
						log.fine("Database problem, most likely misconfiguration error: " + ex);
					results.offer(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet,
							"Database access problem, please contact administrator.", true));
				}

				break;

			case set :

				// Now we use loginOther() instead to make it easier to
				// customize
				// the authentication protocol without a need to replace the
				// authentication plug-in. The authentication takes place on the
				// AuthRepository
				// level so we do not really care here what the user has sent.
				String user_name = request.getChildCDataStaticStr(IQ_QUERY_USERNAME_PATH);
				String resource  = request.getChildCDataStaticStr(IQ_QUERY_RESOURCE_PATH);
				String password  = request.getChildCDataStaticStr(IQ_QUERY_PASSWORD_PATH);
				String digest    = request.getChildCDataStaticStr(IQ_QUERY_DIGEST_PATH);

				try {
					BareJID user_id = BareJID.bareJIDInstance(user_name, session.getDomain()
							.getVhost().getDomain());
					Authorization result = doAuth(repo, settings, session, user_id, password,
							digest);

					if (result == Authorization.AUTHORIZED) {

						// Some clients don't send resource here, instead they
						// send it later
						// in resource bind packet.
						if ((resource != null) &&!resource.isEmpty()) {
							session.setResource(resource);
						}
						results.offer(session.getAuthState().getResponseMessage(packet,
								"Authentication successful.", false));
					} else {
						results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
								"Authentication failed", false));
						results.offer(Command.CLOSE.getPacket(packet.getTo(), packet.getFrom(),
								StanzaType.set, session.nextStanzaId()));
					}    // end of else
				} catch (Exception e) {
					log.info("Authentication failed: " + user_name);
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Authorization exception: ", e);
					}

					Packet response = Authorization.NOT_AUTHORIZED.getResponseMessage(packet, e
							.getMessage(), false);

					response.setPriority(Priority.SYSTEM);
					results.offer(response);

					Integer retries = (Integer) session.getSessionData("auth-retries");

					if (retries == null) {
						retries = new Integer(0);
					}
					if (retries.intValue() < 3) {
						session.putSessionData("auth-retries", new Integer(retries.intValue() + 1));
					} else {
						results.offer(Command.CLOSE.getPacket(packet.getTo(), packet.getFrom(),
								StanzaType.set, session.nextStanzaId()));
					}
				}

				break;

			default :
				results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
						"Message type is incorrect", false));
				results.offer(Command.CLOSE.getPacket(packet.getTo(), packet.getFrom(), StanzaType
						.set, session.nextStanzaId()));

				break;
			}    // end of switch (type)
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param repo
	 * @param settings
	 * @param session
	 * @param user_id
	 * @param password
	 * @param digest
	 *
	 * 
	 */
	protected Authorization doAuth(NonAuthUserRepository repo, Map<String,
			Object> settings, XMPPResourceConnection session, BareJID user_id, String password,
			String digest) {
		try {
			CallbackHandler cbh = callbackHandlerFactory.create("PLAIN", session, repo,
					settings);
			final NameCallback nc = new NameCallback("Authentication identity", user_id
					.getLocalpart());
			final VerifyPasswordCallback vpc = new VerifyPasswordCallback(password);

			cbh.handle(new Callback[] { nc });
			try {
				cbh.handle(new Callback[] { vpc });
				if (vpc.isVerified()) {
					session.authorizeJID(user_id, false);

					return Authorization.AUTHORIZED;
				}
			} catch (UnsupportedCallbackException e) {
				final PasswordCallback pc = new PasswordCallback("Password", false);

				cbh.handle(new Callback[] { pc });

				char[] p = pc.getPassword();

				if ((p != null) && password.equals(new String(p))) {
					session.authorizeJID(user_id, false);

					return Authorization.AUTHORIZED;
				}
			}

			return Authorization.NOT_AUTHORIZED;
		} catch (Exception e) {
			log.log(Level.WARNING, "Can't authenticate with given CallbackHandler", e);

			return Authorization.INTERNAL_SERVER_ERROR;
		}
	}

	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	@Override
	public String[][] supElementNamePaths() {
		return ELEMENT_PATHS;
	}

	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	@Override
	public Element[] supStreamFeatures(final XMPPResourceConnection session) {
		if ((session == null) || session.isAuthorized()) {
			return null;
		} else if (session.isTlsRequired() && !session.isEncrypted()) {
			return null;
		} else {
			return FEATURES;
		} // end of if (session.isAuthorized()) else
	}
}    // JabberIqAuth
