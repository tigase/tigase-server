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
import tigase.auth.mechanisms.AbstractSasl;
import tigase.auth.mechanisms.AbstractSaslSCRAM;
import tigase.auth.mechanisms.SaslANONYMOUS;
import tigase.auth.mechanisms.SaslSCRAMPlus;
import tigase.db.AuthRepository;
import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.Priority;
import tigase.server.xmppsession.SessionManager;
import tigase.util.Base64;
import tigase.xml.Element;
import tigase.xmpp.*;
import tigase.xmpp.jid.BareJID;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Bean(name = SaslAuth2.ID, parent = SessionManager.class, active = false)
public class SaslAuth2 extends AbstractAuthPreprocessor
		implements XMPPProcessorIfc {

	public static final String ID = "urn:xmpp:sasl:2";
	private static final Logger log = Logger.getLogger(SaslAuth2.class.getName());
	private static final String XMLNS = "urn:xmpp:sasl:2";

	protected final static String ALLOWED_SASL_MECHANISMS_KEY = "allowed-sasl-mechanisms";
	protected final static String USER_AGENT_KEY = "user-agent-key";
	protected final static String SASL_FEATURES_KEY = "sasl-features-key";

	private static final Element[] DISCO_FEATURES = {new Element("feature", new String[]{"var"}, new String[]{XMLNS})};
	private static final String[][] ELEMENTS = {{"authenticate"}, {"response"}, {"challenge"}, {"failure"}, {"success"},
												{"continue"}, {"next"}, {"data"}, {"upgrade"}, {"parameters"}, {"hash"},
												{"abort"}};
	private final static String SASL_SERVER_KEY = "SASL_SERVER_KEY";
	private static final String[] XMLNSS = {XMLNS, XMLNS, XMLNS, XMLNS, XMLNS, XMLNS, XMLNS, XMLNS, XMLNS, XMLNS, XMLNS,
											XMLNS};

	public enum ElementType {
		ABORT,
		AUTHENTICATE,
		CHALLENGE,
		FAILURE,
		RESPONSE,
		SUCCESS,
		CONTINUE,
		NEXT,
		DATA,
		UPGRADE,
		PARAMETERS,
		HASH;

		private static Map<String, ElementType> ALL_TYPES = Arrays.stream(ElementType.values())
				.collect(Collectors.toMap(ElementType::getElementName, Function.identity()));

		public static ElementType parse(String name) {
			return ALL_TYPES.get(name);
		}
		private final String elementName;

		ElementType() {
			this.elementName = name().toLowerCase();
		}

		public String getElementName() {
			return elementName;
		}

	}

	@Inject(nullAllowed = true)
	private BruteForceLockerBean bruteForceLocker;

	@Inject
	private TigaseSaslProvider saslProvider;

	@Inject(nullAllowed = true)
	private SessionManager sessionManager;

	@Inject
	private List<Inline> inlines;
	
	private final Map<String, Object> props = new HashMap<String, Object>();
	
	@Override
	public int concurrentQueuesNo() {
		return super.concurrentQueuesNo() * 4;
	}
	
	@Override
	public String id() {
		return ID;
	}

	public void setBruteForceLocker(BruteForceLockerBean bruteForceLocker) {
		log.log(Level.CONFIG, bruteForceLocker != null ? "BruteForceLocker enabled" : "BruteForceLocker disabled" );
		this.bruteForceLocker = bruteForceLocker;
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

	public List<Inline> getInlines() {
		return inlines;
	}

	public void setInlines(List<Inline> inlines) {
		if (inlines == null) {
			inlines = Collections.emptyList();
		}
		List<Inline> smInlines = inlines.stream().filter(it -> it instanceof StreamManagementInline).collect(Collectors.toList());
		if (!smInlines.isEmpty()) {
			inlines.removeAll(smInlines);
			inlines.addAll(0, smInlines);
		}
		this.inlines = inlines;
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

			session.putSessionData(ALLOWED_SASL_MECHANISMS_KEY, auth_mechs);

			List<Element> children = new ArrayList<>();
			for (String mech : auth_mechs) {
				children.add(new Element("mechanism", mech));
			}

			Element inlineEl = new Element("inline");
			children.add(inlineEl);
			for (Inline inline : inlines) {
				Element[] features = inline.supStreamFeatures(Inline.Action.sasl2);
				if (features != null) {
					for (Element feature : features) {
						inlineEl.addChild(feature);
					}
				}
			}

			if (session.isEncrypted() && session.getSessionData(AbstractSaslSCRAM.LOCAL_CERTIFICATE_KEY) != null &&
					SaslSCRAMPlus.containsScramPlus(auth_mechs)) {
				Element bindings = AbstractSaslSCRAM.getSupportedChannelBindings(session);
				return new Element[]{new Element("authentication", children.toArray(Element[]::new), new String[]{"xmlns"},
												 new String[]{XMLNS}), bindings};
			}
			else {
				return new Element[]{new Element("authentication", children.toArray(Element[]::new), new String[]{"xmlns"},
												 new String[]{XMLNS})};
			}
		}
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
						Queue<Packet> results, Map<String, Object> settings) throws XMPPException {
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
				Packet res = createSaslErrorResponse(null, null, packet);

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
				return;
			} else {
				try {
					ElementType action = ElementType.parse(packet.getElemName());
					if (action == null) {
						throw new XmppSaslException(XmppSaslException.SaslError.malformed_request,
													"Unrecognized element " + action.getElementName());
					}

					SaslServer ss;
					byte[] data = new byte[0];

					switch (action) {
						case AUTHENTICATE:
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
								throw new XmppSaslException(XmppSaslException.SaslError.invalid_mechanism,
															"Mechanism '" + mechanismName + "' is not allowed");
							}

							CallbackHandler cbh = saslProvider.create(mechanismName, session, repo, settings);

							ss = Sasl.createSaslServer(mechanismName, "xmpp", session.getDomain().getVhost().getDomain(),
													   props, cbh);
							if (ss == null) {
								throw new XmppSaslException(XmppSaslException.SaslError.invalid_mechanism,
															"Mechanism '" + mechanismName + "' is not allowed");
							}
							session.putSessionData(SASL_SERVER_KEY, ss);

							UserAgent userAgent = parseUserAgent(packet);
							session.putSessionData(USER_AGENT_KEY, userAgent);
							List<Element> features = packet.getElement().findChildren(el -> el.getName() != "initial-response" && el.getName() != "user-agent");
							if (features != null && !features.isEmpty()) {
								session.putSessionData(SASL_FEATURES_KEY, features);
							}

							Element initialResponse = packet.getElement().getChild("initial-response");
							if (initialResponse != null) {
								String cdata = initialResponse.getCData();
								if (cdata != null && !cdata.equals("=")) {
									data = Base64.decode(cdata);
								}
							}
							break;
						case RESPONSE:
							ss = (SaslServer) session.getSessionData(SASL_SERVER_KEY);
							if (ss == null) {
								throw new XmppSaslException(XmppSaslException.SaslError.malformed_request);
							}
							String cdata = packet.getElement().getCData();
							if (cdata != null && !cdata.equals("=")) {
								data = Base64.decode(cdata);
							}
							break;
						default:
							throw new XmppSaslException(XmppSaslException.SaslError.malformed_request,
														"Unrecognized element " + action.getElementName());
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

						if (bruteForceLocker != null && bruteForceLocker.isEnabled(session) &&
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

						Element success = new Element("success");
						success.setXMLNS(XMLNS);
						if (challengeData != null) {
							success.addChild(new Element("additional-data", challengeData));
						}

						CompletableFuture<Inline.Result> result = CompletableFuture.completedFuture(new Inline.Result(null, true));

						List<Element> children = (List<Element>) session.getSessionData(SASL_FEATURES_KEY);
						if (children != null && !children.isEmpty()) {
							for (Element child : children) {
								for (Inline inline : inlines) {
									if (inline.canHandle(session, child)) {
										result = result.thenCompose(r -> {
											if (r.element != null) {
												success.addChild(r.element);
											}
											if (r.shouldContinue) {
												return inline.process(session, child);
											} else {
												return CompletableFuture.completedFuture(new Inline.Result(null, false));
											}
										});
									}
								}
							}
						}
						result.thenCompose(x -> {
							if (x.element != null) {
								success.addChild(x.element);
							}
							try {
								success.addChild(new Element("authorization-identifier", session.getJID().toString()));
								return CompletableFuture.completedFuture(packet.swapFromTo(success, null, null));
							} catch (NotAuthorizedException ex) {
								return CompletableFuture.failedFuture(ex);
							}
						}).thenAccept(resultPacket -> {
							sessionManager.addOutPacket(resultPacket);
							// we need to send new stream features, so lets request them from SM and send result directly to C2S
							Packet getFeaturesCmd = Command.GETFEATURES.getPacket(packet.getFrom(), packet.getTo(), StanzaType.get,
																				  UUID.randomUUID().toString(), null);
							getFeaturesCmd.setPacketFrom(packet.getPacketFrom());
							getFeaturesCmd.setPacketTo(packet.getPacketTo());
							sessionManager.addOutPacket(getFeaturesCmd);
						}).exceptionally(ex -> {
							sessionManager.addOutPacket(createSaslErrorResponse(
									XmppSaslException.SaslError.not_authorized, ex.getMessage(), packet));
							return (Void) null;
						});
					} else if (!ss.isComplete()) {
						results.offer(packet.swapFromTo(createReply(ElementType.CHALLENGE, challengeData), null, null));
					} else {
						throw new XmppSaslException(XmppSaslException.SaslError.malformed_request);
					}
				} catch (BruteForceLockerBean.LoginLockedException e) {
					onAuthFail(session);
					if (log.isLoggable(Level.FINER)) {
						log.log(Level.FINER, "Account locked by BruteForceLocker.");
					}
					results.offer(createSaslErrorResponse(XmppSaslException.SaslError.not_authorized, AbstractSasl.PASSWORD_NOT_VERIFIED_MSG, packet));
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
					results.offer(createSaslErrorResponse(XmppSaslException.SaslError.not_authorized, null, packet));
				} catch (Exception e) {
					onAuthFail(session);
					if (log.isLoggable(Level.WARNING)) {
						log.log(Level.WARNING, "Problem with SASL", e);
					}
					results.offer(createSaslErrorResponse(XmppSaslException.SaslError.temporary_auth_failure, null, packet));
				}
			}
		}
	}

	protected void onAuthFail(final XMPPResourceConnection session) {
		session.removeSessionData(SASL_SERVER_KEY);
	}

	private Element createReply(final SaslAuth2.ElementType type, final String cdata) {
		Element reply = new Element(type.getElementName());

		reply.setXMLNS(XMLNS);
		if (cdata != null) {
			reply.setCData(cdata);
		}

		return reply;
	}

	private void disableUser(final XMPPResourceConnection session, final BareJID userJID) {
		try {
			AuthRepository.AccountStatus status = session.getAuthRepository().getAccountStatus(userJID);
			if (status == AuthRepository.AccountStatus.active) {
				log.log(Level.CONFIG, "Disabling user " + userJID);
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
			if (bruteForceLocker != null && bruteForceLocker.isEnabled(session)) {
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

	private Packet createSaslErrorResponse(XmppSaslException.SaslError error, String message, Packet packet) {
		Element failure = new Element(ElementType.FAILURE.getElementName());
		failure.setXMLNS(XMLNS);
		failure.addChild((error == null ? XmppSaslException.SaslError.not_authorized : error).getElement());
		if (message != null) {
			failure.addChild(new Element("text", message, new String[]{"xml:lang"}, new String[]{"en"}));
		}

		Packet response = packet.swapFromTo(failure, null, null);
		response.setPriority(Priority.SYSTEM);
		return response;
	}

	private UserAgent parseUserAgent(Packet packet) {
		Element el = packet.getElemChild("user-agent");
		if (el == null) {
			return null;
		}

		Element software = el.getChild("software");
		Element device = el.getChild("device");

		return new UserAgent(el.getAttributeStaticStr("id"), software != null ? software.getCData() : null,
							 device != null ? device.getCData() : null);
	}

	public interface Inline {

		enum Action {
			sasl2,
			bind2
		}

		boolean canHandle(XMPPResourceConnection connection, Element el);

		Element[] supStreamFeatures(Inline.Action action);

		CompletableFuture<Result> process(XMPPResourceConnection session, Element action);

		public static class Result {
			public final Element element;
			public final boolean shouldContinue;

			public Result(Element element, boolean shouldContinue) {
				this.element = element;
				this.shouldContinue = shouldContinue;
			}
		}

	}

	public static class UserAgent {
		private final String id;
		private final String software;
		private final String device;

		public UserAgent(String id, String software, String device) {
			this.id = id;
			this.software = software;
			this.device = device;
		}

		public String getId() {
			return id;
		}

		public String getSoftware() {
			return software;
		}

		public String getDevice() {
			return device;
		}
	}

}
