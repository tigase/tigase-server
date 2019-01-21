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
package tigase.xmpp.impl;

import tigase.auth.*;
import tigase.auth.XmppSaslException.SaslError;
import tigase.auth.mechanisms.AbstractSasl;
import tigase.auth.mechanisms.SaslANONYMOUS;
import tigase.db.AuthRepository;
import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.Priority;
import tigase.server.xmppsession.SessionManager;
import tigase.stats.StatisticsList;
import tigase.util.Base64;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.BareJID;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Describe class SaslAuth here.
 * <br>
 * Created: Mon Feb 20 16:28:13 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Bean(name = SaslAuth.ID, parent = SessionManager.class, active = true)
public class SaslAuth
		extends AbstractAuthPreprocessor
		implements XMPPProcessorIfc {

	public static final String ID = "urn:ietf:params:xml:ns:xmpp-sasl";
	private static final String _XMLNS = "urn:ietf:params:xml:ns:xmpp-sasl";
	private final static String ALLOWED_SASL_MECHANISMS_KEY = "allowed-sasl-mechanisms";
	private static final Element[] DISCO_FEATURES = {new Element("feature", new String[]{"var"}, new String[]{_XMLNS})};
	private static final String[][] ELEMENTS = {{"auth"}, {"response"}, {"challenge"}, {"failure"}, {"success"},
												{"abort"}};
	private static final Logger log = Logger.getLogger(SaslAuth.class.getName());
	private final static String SASL_SERVER_KEY = "SASL_SERVER_KEY";
	private static final String[] XMLNSS = {_XMLNS, _XMLNS, _XMLNS, _XMLNS, _XMLNS, _XMLNS};

	public enum ElementType {
		abort,
		auth,
		challenge,
		failure,
		response,
		success
	}

	private final Map<String, Object> props = new HashMap<String, Object>();
	@Inject
	private BruteForceLockerBean bruteForceLocker;
	@Inject
	private TigaseSaslProvider saslProvider;

	@Override
	public int concurrentQueuesNo() {
		return super.concurrentQueuesNo() * 4;
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void process(final Packet packet, final XMPPResourceConnection session, final NonAuthUserRepository repo,
						final Queue<Packet> results, final Map<String, Object> settings) {
		if (session == null) {
			return;
		}
		synchronized (session) {
			// If authentication timeout expired, ignore the request....
			if (session.getSessionData(XMPPResourceConnection.AUTHENTICATION_TIMEOUT_KEY) != null) {
				return;
			}
			final String clientIp = BruteForceLockerBean.getClientIp(session);
			if (session.isAuthorized()) {

				// Multiple authentication attempts....
				// Another authentication request on already authenticated
				// connection
				// This is not allowed and must be forbidden.
				Packet res = packet.swapFromTo(createReply(ElementType.failure, "<not-authorized/>"), null, null);

				// Make sure it gets delivered before stream close
				res.setPriority(Priority.SYSTEM);
				results.offer(res);

				// Optionally close the connection to make sure there is no
				// confusion about the connection state.
				results.offer(Command.CLOSE.getPacket(packet.getTo(), packet.getFrom(), StanzaType.set,
													  session.nextStanzaId()));
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
			} else {
				Element request = packet.getElement();
				try {
					SaslServer ss;

					if ("auth" == request.getName()) {
						final String mechanismName = request.getAttributeStaticStr("mechanism");

						if (log.isLoggable(Level.FINEST)) {
							log.finest("Start SASL auth. mechanism=" + mechanismName);
						}

						Collection<String> allowedMechanisms = (Collection<String>) session.getSessionData(
								ALLOWED_SASL_MECHANISMS_KEY);
						session.removeSessionData(ALLOWED_SASL_MECHANISMS_KEY);

						if (allowedMechanisms == null) {
							allowedMechanisms = saslProvider.filterMechanisms(Sasl.getSaslServerFactories(), session);
						}

						if ((mechanismName == null) || allowedMechanisms == null ||
								!allowedMechanisms.contains(mechanismName)) {
							throw new XmppSaslException(SaslError.invalid_mechanism,
														"Mechanism '" + mechanismName + "' is not allowed");
						}

						CallbackHandler cbh = saslProvider.create(mechanismName, session, repo, settings);

						ss = Sasl.createSaslServer(mechanismName, "xmpp", session.getDomain().getVhost().getDomain(),
												   props, cbh);
						if (ss == null) {
							throw new XmppSaslException(SaslError.invalid_mechanism,
														"Mechanism '" + mechanismName + "' is not allowed");
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
						data = new byte[]{};
					} else if ((cdata != null) && (cdata.length() > 0)) {
						data = Base64.decode(cdata);
					} else {
						data = new byte[]{};
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
							jid = BareJID.bareJIDInstance(ss.getAuthorizationID(),
														  session.getDomain().getVhost().getDomain());
						}

						if (bruteForceLocker.isEnabled(session) &&
								!bruteForceLocker.isLoginAllowed(session, clientIp, jid)) {
							throw new BruteForceLockerBean.LoginLockedException();
						}

						if (log.isLoggable(Level.FINE)) {
							log.finest("Authorized as " + jid);
						}

						boolean anonymous;

						try {
							Boolean x = (Boolean) ss.getNegotiatedProperty(SaslANONYMOUS.IS_ANONYMOUS_PROPERTY);

							anonymous = x != null && x;
						} catch (Exception e) {
							anonymous = false;
						}
						session.removeSessionData(SASL_SERVER_KEY);
						session.authorizeJID(jid, anonymous);
						if (session.getAuthRepository() != null) {
							session.getAuthRepository().loggedIn(jid);
						}
						results.offer(packet.swapFromTo(createReply(ElementType.success, challengeData), null, null));
					} else if (!ss.isComplete()) {
						results.offer(packet.swapFromTo(createReply(ElementType.challenge, challengeData), null, null));
					} else {
						throw new XmppSaslException(SaslError.malformed_request);
					}
				} catch (BruteForceLockerBean.LoginLockedException e) {
					onAuthFail(session);
					if (log.isLoggable(Level.FINER)) {
						log.log(Level.FINER, "Account locked by BruteForceLocker.");
					}
					sendNotAuthorized(SaslError.not_authorized, AbstractSasl.PASSWORD_NOT_VERIFIED_MSG, packet,
									  results);
				} catch (XmppSaslException e) {
					saveIntoBruteForceLocker(session, e);
					onAuthFail(session);
					if (log.isLoggable(Level.FINER)) {
						log.log(Level.FINER, "SASL unsuccessful", e);
					}
					sendNotAuthorized(e.getSaslError(), e.getMessage(), packet, results);
				} catch (SaslException e) {
					saveIntoBruteForceLocker(session, e);
					onAuthFail(session);
					if (log.isLoggable(Level.FINER)) {
						log.log(Level.FINER, "SASL unsuccessful", e);
					}
					sendNotAuthorized(SaslError.not_authorized, null, packet, results);
				} catch (Exception e) {
					onAuthFail(session);
					if (log.isLoggable(Level.WARNING)) {
						log.log(Level.WARNING, "Problem with SASL", e);
					}
					sendNotAuthorized(SaslError.temporary_auth_failure, null, packet, results);
				}
			}
		}
	}

	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		this.bruteForceLocker.getStatistics(getComponentInfo().getName(), list);
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
			Collection<String> auth_mechs = saslProvider.filterMechanisms(Sasl.getSaslServerFactories(), session);
			Element[] mechs = new Element[auth_mechs.size()];
			int idx = 0;

			session.putSessionData(ALLOWED_SASL_MECHANISMS_KEY, auth_mechs);
			for (String mech : auth_mechs) {
				mechs[idx++] = new Element("mechanism", mech);
			}

			return new Element[]{new Element("mechanisms", mechs, new String[]{"xmlns"}, new String[]{_XMLNS})};
		}
	}

	protected void onAuthFail(final XMPPResourceConnection session) {
		session.removeSessionData(SASL_SERVER_KEY);
	}

	private Element createReply(final ElementType type, final String cdata) {
		Element reply = new Element(type.toString());

		reply.setXMLNS(_XMLNS);
		if (cdata != null) {
			reply.setCData(cdata);
		}

		return reply;
	}

	private void disableUser(final XMPPResourceConnection session, final BareJID userJID) {
		try {
			AuthRepository.AccountStatus status = session.getAuthRepository().getAccountStatus(userJID);
			if (status == AuthRepository.AccountStatus.active) {
				log.info("Disabling user " + userJID);
				session.getAuthRepository().setAccountStatus(userJID, AuthRepository.AccountStatus.disabled);
			}
		} catch (TigaseDBException e) {
			log.log(Level.WARNING, "Cannot check status or disable user!", e);
		}
	}

	/**
	 * Tries to extract BareJID of user who try to log in.
	 */
	private BareJID extractUserJid(final Exception e, XMPPResourceConnection session) {
		BareJID jid = null;

		if (e instanceof SaslInvalidLoginExcepion) {
			String t = ((SaslInvalidLoginExcepion) e).getJid();
			jid = t == null ? null : BareJID.bareJIDInstanceNS(t);
		}

		if (jid != null) {
			jid = (BareJID) session.getSessionData(CallbackHandlerFactory.AUTH_JID);
		}

		return jid;
	}

	private void saveIntoBruteForceLocker(final XMPPResourceConnection session, final Exception e) {
		try {
			if (bruteForceLocker.isEnabled(session)) {
				final String clientIp = BruteForceLockerBean.getClientIp(session);
				final BareJID userJid = extractUserJid(e, session);

				if (clientIp == null && log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "There is no client IP. Cannot add entry to BruteForceLocker.");
				}
				if (userJid == null && log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "There is no user JID. Cannot add entry to BruteForceLocker.");
				}

				if (userJid != null && clientIp != null) {
					bruteForceLocker.addInvalidLogin(session, clientIp, userJid);
				}

				if (bruteForceLocker.canUserBeDisabled(session, clientIp, userJid)) {
					disableUser(session, userJid);
				}

			}
		} catch (Throwable caught) {
			log.log(Level.WARNING, "Cannot update BruteForceLocker", caught);
		}
	}

	private void sendNotAuthorized(SaslError error, String message, Packet packet, Queue<Packet> results) {
		String el;
		if (error.getElementName() != null) {
			el = "<" + error.getElementName() + "/>";
		} else {
			el = "<not-authorized/>";
		}
		if (message != null) {
			el += "<text xml:lang='en'>" + message + "</text>";
		}

		Packet response = packet.swapFromTo(createReply(ElementType.failure, el), null, null);

		response.setPriority(Priority.SYSTEM);
		results.offer(response);
	}

}