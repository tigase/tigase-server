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

import tigase.auth.BruteForceLockerBean;
import tigase.auth.XmppSaslException;
import tigase.auth.XmppSaslException.SaslError;
import tigase.auth.mechanisms.AbstractSasl;
import tigase.auth.mechanisms.SaslANONYMOUS;
import tigase.db.NonAuthUserRepository;
import tigase.kernel.beans.Bean;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;
import tigase.util.Base64;
import tigase.xml.Element;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.BareJID;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Describe class SaslAuth here.
 * <br>
 * Created: Mon Feb 20 16:28:13 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
*/
@Bean(name = SaslAuth.ID, parent = SessionManager.class, active = true)
public class SaslAuth
		extends SaslAuthAbstract
		implements XMPPProcessorIfc {

	public static final String ID = "urn:ietf:params:xml:ns:xmpp-sasl";
	private static final String _XMLNS = "urn:ietf:params:xml:ns:xmpp-sasl";
	private static final Element[] DISCO_FEATURES = {new Element("feature", new String[]{"var"}, new String[]{_XMLNS})};
	private static final String[][] ELEMENTS = {{"auth"}, {"response"}, {"challenge"}, {"failure"}, {"success"},
												{"abort"}};
	private static final Logger log = Logger.getLogger(SaslAuth.class.getName());
	private static final String[] XMLNSS = {_XMLNS, _XMLNS, _XMLNS, _XMLNS, _XMLNS, _XMLNS};

	@Override
	public String id() {
		return ID;
	}

	public enum ElementType {
		ABORT,
		AUTH,
		CHALLENGE,
		FAILURE,
		RESPONSE,
		SUCCESS;

		private static final Map<String, ElementType> ALL_TYPES = Arrays.stream(
						ElementType.values())
				.collect(Collectors.toMap(ElementType::getElementName, Function.identity()));
		private final String elementName;

		public static ElementType parse(String name) {
			return ALL_TYPES.get(name);
		}

		ElementType() {
			this.elementName = name().toLowerCase();
		}

		public String getElementName() {
			return elementName;
		}
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
				processSessionAlreadyAuthorized(packet, session, results);
				return;
			} else {
				Element request = packet.getElement();
				try {
					SaslServer ss;

					if ("auth" == request.getName()) {
						final String mechanismName = packet.getElement().getAttributeStaticStr("mechanism");

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

						if (isLoginAllowedByBruteForceLocker(session, clientIp, jid)) {
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
						processSuccess(packet, session, challengeData, results);
					} else if (!ss.isComplete()) {
						results.offer(packet.swapFromTo(createReply(ElementType.CHALLENGE, challengeData), null, null));
					} else {
						throw new XmppSaslException(SaslError.malformed_request);
					}
				} catch (BruteForceLockerBean.LoginLockedException e) {
					onAuthFail(session);
					if (log.isLoggable(Level.FINER)) {
						log.log(Level.FINER, "Account locked by BruteForceLocker.");
					}
					results.offer(createSaslErrorResponse(SaslError.not_authorized, AbstractSasl.PASSWORD_NOT_VERIFIED_MSG, packet));
				} catch (XmppSaslException e) {
					saveIntoBruteForceLocker(session, e);
					onAuthFail(session);
					if (log.isLoggable(Level.FINER)) {
						log.log(Level.FINER, "SASL unsuccessful", e);
					}
					results.offer(createSaslErrorResponse(e.getSaslError(), e.getMessage(), packet));
				} catch (SaslException e) {
					saveIntoBruteForceLocker(session, e);
					onAuthFail(session);
					if (log.isLoggable(Level.FINER)) {
						log.log(Level.FINER, "SASL unsuccessful", e);
					}
					results.offer(createSaslErrorResponse(SaslError.not_authorized, null, packet));
				} catch (Exception e) {
					onAuthFail(session);
					if (log.isLoggable(Level.WARNING)) {
						log.log(Level.WARNING, "Problem with SASL", e);
					}
					results.offer(createSaslErrorResponse(SaslError.temporary_auth_failure, null, packet));
				}
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
			Collection<String> auth_mechs = saslProvider.filterMechanisms(Sasl.getSaslServerFactories(), session);
			if (auth_mechs.isEmpty()) {
				return null;
			}

			Element[] mechs = new Element[auth_mechs.size()];
			int idx = 0;

			session.putSessionData(ALLOWED_SASL_MECHANISMS_KEY, auth_mechs);
			for (String mech : auth_mechs) {
				mechs[idx++] = new Element("mechanism", mech);
			}

			return new Element[]{new Element("mechanisms", mechs, new String[]{"xmlns"}, new String[]{_XMLNS})};
		}
	}

	protected String getXmlns() {
		return _XMLNS;
	}

	@Override
	protected void processSuccess(Packet packet, XMPPResourceConnection session, String challengeData,
	                              Queue<Packet> results) {
		results.offer(packet.swapFromTo(createReply(ElementType.SUCCESS, challengeData), null, null));
	}

	protected Element createReply(final ElementType type, final String cdata) {
		Element reply = new Element(type.getElementName());

		reply.setXMLNS(getXmlns());
		if (cdata != null) {
			reply.setCData(cdata);
		}

		return reply;
	}
}