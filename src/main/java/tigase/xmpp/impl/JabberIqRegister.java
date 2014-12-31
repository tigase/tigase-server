/*
 * JabberIqRegister.java
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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.form.Field;
import tigase.form.Form;
import tigase.form.FormSignatureVerifier;
import tigase.form.FormSignerException;
import tigase.form.SignatureCalculator;
import tigase.server.Command;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.Priority;
import tigase.stats.StatisticsList;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xml.XMLUtils;
import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.NoConnectionIdException;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

/**
 * JEP-0077: In-Band Registration
 * 
 * 
 * Created: Thu Feb 16 13:14:06 2006
 * 
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JabberIqRegister extends XMPPProcessor implements XMPPProcessorIfc {
	private static final int REMOTE_ADDRESS_IDX = 2;
	private static final String[][] ELEMENTS = { Iq.IQ_QUERY_PATH };
	public static final String ID = "jabber:iq:register";

	/**
	 * Whitelist properties
	 */

	public static final String REGISTRATION_BLACKLIST_PROP_KEY = "registration-blacklist";

	public static final String REGISTRATION_WHITELIST_PROP_KEY = "registration-whitelist";

	public static final String WHITELIST_REGISTRATION_ONLY_PROP_KEY = "whitelist-registration-only";

	private List<CIDRAddress> registrationWhitelist = new LinkedList<CIDRAddress>();
	private List<CIDRAddress> registrationBlacklist = new LinkedList<CIDRAddress>();
	private boolean whitelistRegistrationOnly = false;

	/**
	 * OAuth details for form verifier.
	 */
	public static final String OAUTH_CONSUMERKEY_PROP_KEY = "oauth-consumer-key";
	public static final String OAUTH_CONSUMERSECRET_PROP_KEY = "oauth-consumer-secret";
	public static final String SIGNED_FORM_REQUIRED_PROP_KEY = "signed-form-required";

	private String oauthConsumerKey;
	private String oauthConsumerSecret;
	private boolean signedFormRequired = false;

	public void setOAuthCredentials(String oauthConsumerKey, String oauthConsumerSecret){
		this.oauthConsumerKey = oauthConsumerKey;
		this.oauthConsumerSecret = oauthConsumerSecret;
	}
	
	public void setSignedFormRequired(boolean required){
		this.signedFormRequired = required;
	}
	
	/**
	 * Private logger for class instances.
	 */
	private static Logger log = Logger.getLogger(JabberIqRegister.class.getName());
	private static final String[] XMLNSS = { "jabber:iq:register" };
	private static final String[] IQ_QUERY_USERNAME_PATH = { Iq.ELEM_NAME, Iq.QUERY_NAME, "username" };
	private static final String[] IQ_QUERY_REMOVE_PATH = { Iq.ELEM_NAME, Iq.QUERY_NAME, "remove" };
	private static final String[] IQ_QUERY_PASSWORD_PATH = { Iq.ELEM_NAME, Iq.QUERY_NAME, "password" };
	private static final String[] IQ_QUERY_EMAIL_PATH = { Iq.ELEM_NAME, Iq.QUERY_NAME, "email" };
	private static final Element[] FEATURES = { new Element("register", new String[] { "xmlns" },
			new String[] { "http://jabber.org/features/iq-register" }) };
	private static final Element[] DISCO_FEATURES = { new Element("feature", new String[] { "var" },
			new String[] { "jabber:iq:register" }) };

	// ~--- methods
	// --------------------------------------------------------------

	@Override
	public String id() {
		return ID;
	}

	/**{@inheritDoc}
	 *
	 * <br><br>
	 *
	 *             TODO: Implement registration form configurable and loading
	 *             all the fields from the registration form TODO: rewrite the
	 *             plugin using the XMPPProcessorAbstract API
	 */
	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results,
			Map<String, Object> settings) throws XMPPException {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Processing packet: " + packet.toString());
		}
		if (session == null) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Session is null, ignoring");
			}

			return;
		} // end of if (session == null)

		BareJID id = session.getDomainAsJID().getBareJID();

		if (packet.getStanzaTo() != null) {
			id = packet.getStanzaTo().getBareJID();
		}
		try {

			// I think it does not make sense to check the 'to', just the
			// connection
			// ID
			// if ((id.equals(session.getDomain()) ||
			// id.equals(session.getUserId().toString()))
			// && packet.getFrom().equals(session.getConnectionId())) {
			// Wrong thinking. The user may send an request from his own account
			// to register with a transport or any other service, then the
			// connection
			// ID matches the session id but this is still not a request to the
			// local
			// server. The TO address must be checked too.....
			// if (packet.getPacketFrom().equals(session.getConnectionId())) {
			if ((packet.getPacketFrom() != null) && packet.getPacketFrom().equals(session.getConnectionId())
					&& (!session.isAuthorized() || (session.isUserId(id) || session.isLocalDomain(id.toString(), false)))) {

				// We want to allow password change but not user registration if
				// registration is disabled. The only way to tell apart
				// registration
				// from password change is to check whether the user is
				// authenticated.
				// For authenticated user the request means password change,
				// otherwise
				// registration attempt.
				// Account deregistration is also called under authenticated
				// session, so
				// it should be blocked here if registration for domain is
				// disabled.
				// Assuming if user cannot register account he cannot also
				// deregister account
				Element request = packet.getElement();
				boolean remove = request.findChildStaticStr(IQ_QUERY_REMOVE_PATH) != null;

				if (!session.isAuthorized() || remove) {
					if (!isRegistrationAllowedForConnection(packet.getFrom())) {
						results.offer(Authorization.NOT_ALLOWED.getResponseMessage(packet,
								"Registration is not allowed for this connection.", true));
						++statsInvalidRegistrations;
						return;
					}

					if (!session.getDomain().isRegisterEnabled()) {
						results.offer(Authorization.NOT_ALLOWED.getResponseMessage(packet,
								"Registration is not allowed for this domain.", true));
						++statsInvalidRegistrations;
						return;
					}
				}

				Authorization result = Authorization.NOT_AUTHORIZED;
				StanzaType type = packet.getType();

				switch (type) {
				case set:

					// Is it registration cancel request?
					Element elem = request.findChildStaticStr(IQ_QUERY_REMOVE_PATH);

					if (elem != null) {

						// Yes this is registration cancel request
						// According to JEP-0077 there must not be any
						// more subelements apart from <remove/>
						elem = request.findChildStaticStr(Iq.IQ_QUERY_PATH);
						if (elem.getChildren().size() > 1) {
							result = Authorization.BAD_REQUEST;
						} else {
							try {
								result = session.unregister(packet.getStanzaFrom().toString());

								Packet ok_result = packet.okResult((String) null, 0);

								// We have to set SYSTEM priority for the packet
								// here,
								// otherwise the network connection is closed
								// before the
								// client received a response
								ok_result.setPriority(Priority.SYSTEM);
								results.offer(ok_result);

								Packet close_cmd = Command.CLOSE.getPacket(session.getSMComponentId(),
										session.getConnectionId(), StanzaType.set, session.nextStanzaId());

								close_cmd.setPacketTo(session.getConnectionId());
								close_cmd.setPriority(Priority.LOWEST);
								results.offer(close_cmd);
							} catch (NotAuthorizedException e) {
								results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
										"You must authorize session first.", true));
							} // end of try-catch
						}
					} else {
						String user_name;
						String password;
						String email;
						if (signedFormRequired) {
							final String expectedToken = UUID.nameUUIDFromBytes(
									(session.getConnectionId() + "|" + session.getSessionId()).getBytes()).toString();

							FormSignatureVerifier verifier = new FormSignatureVerifier(oauthConsumerKey, oauthConsumerSecret);
							Element queryEl = request.getChild("query", "jabber:iq:register");
							Element formEl = queryEl == null ? null : queryEl.getChild("x", "jabber:x:data");
							if (formEl == null) {
								results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
										"Use Signed Registration Form", true));
								++statsInvalidRegistrations;
								return;
							}
							Form form = new Form(formEl);
							if (!expectedToken.equals(form.getAsString("oauth_token"))) {
								log.finest("Received oauth_token is different that sent one.");
								results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Unknown oauth_token", true));
								++statsInvalidRegistrations;
								return;
							}
							if (!oauthConsumerKey.equals(form.getAsString("oauth_consumer_key"))) {
								log.finest("Unknown oauth_consumer_key");
								results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
										"Unknown oauth_consumer_key", true));
								++statsInvalidRegistrations;
								return;
							}
							try {
								long timestamp = verifier.verify(packet.getStanzaTo(), form);
								user_name = form.getAsString("username");
								password = form.getAsString("password");
								email = form.getAsString("email");
							} catch (FormSignerException e) {
								log.fine("Form Signature Validation Problem: " + e.getMessage());
								results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Invalid form signature",
										true));
								++statsInvalidRegistrations;
								return;
							}
						} else {
							// No, so assuming this is registration of a new
							// user or change registration details for existing
							// user
							user_name = request.getChildCDataStaticStr(IQ_QUERY_USERNAME_PATH);
							password = request.getChildCDataStaticStr(IQ_QUERY_PASSWORD_PATH);
							email = request.getChildCDataStaticStr(IQ_QUERY_EMAIL_PATH);
						}
						String pass_enc = null;
						if (null != password) {
							pass_enc = XMLUtils.unescape(password);
						}
						Map<String, String> reg_params = null;

						if ((email != null) && !email.trim().isEmpty()) {
							reg_params = new LinkedHashMap<String, String>();
							reg_params.put("email", email);
						}
						result = session.register(user_name, pass_enc, reg_params);
						if (result == Authorization.AUTHORIZED) {
							results.offer(result.getResponseMessage(packet, null, false));
						} else {
							++statsInvalidRegistrations;
							results.offer(result.getResponseMessage(packet, "Unsuccessful registration attempt", true));
						}
					}

					break;

				case get: {
					if (signedFormRequired) {
						results.offer(packet.okResult(prepareRegistrationForm(session
							), 0));
					} else
						results.offer(packet.okResult("<instructions>"
								+ "Choose a user name and password for use with this service."
								+ "Please provide also your e-mail address." + "</instructions>" + "<username/>"
								+ "<password/>" + "<email/>", 1));

					break;
				}
				case result:

					// It might be a registration request from transport for
					// example...
					Packet pack_res = packet.copyElementOnly();

					pack_res.setPacketTo(session.getConnectionId());
					results.offer(pack_res);

					break;

				default:
					results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Message type is incorrect", true));

					break;
				} // end of switch (type)
			} else {
				if (session.isUserId(id)) {

					// It might be a registration request from transport for
					// example...
					Packet pack_res = packet.copyElementOnly();

					pack_res.setPacketTo(session.getConnectionId());
					results.offer(pack_res);
				} else {
					results.offer(packet.copyElementOnly());
				}
			}
		} catch (TigaseStringprepException ex) {
			results.offer(Authorization.JID_MALFORMED.getResponseMessage(packet,
					"Incorrect user name, stringprep processing failed.", true));
		} catch (NotAuthorizedException e) {
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You are not authorized to change registration settings.\n" + e.getMessage(), true));
		} catch (TigaseDBException e) {
			log.warning("Database problem: " + e);
			results.offer(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet,
					"Database access problem, please contact administrator.", true));
		} // end of try-catch
	}

	private Element prepareRegistrationForm(final XMPPResourceConnection session) throws NoConnectionIdException {
		Element query = new Element("query", new String[] { "xmlns" }, XMLNSS);
		query.addChild(new Element("instructions", "Use the enclosed form to register."));
		Form form = new Form(SignatureCalculator.SUPPORTED_TYPE, "Contest Registration",
				"Please provide the following information to sign up for our special contests!");

		form.addField(Field.fieldTextSingle("username", "", "Username"));
		form.addField(Field.fieldTextPrivate("password", "", "Password"));
		form.addField(Field.fieldTextSingle("email", "", "Email"));

		SignatureCalculator sc = new SignatureCalculator(oauthConsumerKey, oauthConsumerSecret);
		sc.setOauthToken(UUID.nameUUIDFromBytes((session.getConnectionId() + "|" + session.getSessionId()).getBytes()).toString());
		sc.addEmptyFields(form);

		query.addChild(form.getElement());
		return query;
	}

	protected boolean isRegistrationAllowedForConnection(JID from) {
		String remoteAdress = parseRemoteAddressFromJid(from);
		if (whitelistRegistrationOnly) {
			return contains(registrationWhitelist, remoteAdress);
		}
		return !contains(registrationBlacklist, remoteAdress);
	}

	private static String parseRemoteAddressFromJid(JID from) {
		try {
			String connectionId = from.getResource();
			return connectionId.split("_")[REMOTE_ADDRESS_IDX];
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}

	private long statsRegisteredUsers;
	
	private long statsInvalidRegistrations;
	
	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		list.add(getComponentInfo().getName(), "Registered users", statsRegisteredUsers, Level.INFO);
		list.add(getComponentInfo().getName(), "Invalid registrations", statsInvalidRegistrations, Level.INFO);
	}
	
	@Override
	public void init(Map<String, Object> settings) throws TigaseDBException {
		this.oauthConsumerKey = (String) settings.get(OAUTH_CONSUMERKEY_PROP_KEY);
		this.oauthConsumerSecret = (String) settings.get(OAUTH_CONSUMERSECRET_PROP_KEY);
		Object verifyFormSignatureObj = settings.get(SIGNED_FORM_REQUIRED_PROP_KEY);
		if (verifyFormSignatureObj != null && verifyFormSignatureObj instanceof Boolean) {
			signedFormRequired = (Boolean) verifyFormSignatureObj;
		} else if (verifyFormSignatureObj != null) {
			signedFormRequired = Boolean.parseBoolean(verifyFormSignatureObj.toString());
		}

		String whitelistRegistrationOnlyStr = (String) settings.get(WHITELIST_REGISTRATION_ONLY_PROP_KEY);
		if (whitelistRegistrationOnlyStr != null) {
			whitelistRegistrationOnly = Boolean.parseBoolean(whitelistRegistrationOnlyStr);
		}

		String registrationWhitelistStr = (String) settings.get(REGISTRATION_WHITELIST_PROP_KEY);
		if (registrationWhitelistStr != null) {
			registrationWhitelist = parseList(registrationWhitelistStr);
		}

		String registrationBlacklistStr = (String) settings.get(REGISTRATION_BLACKLIST_PROP_KEY);
		if (registrationBlacklistStr != null) {
			registrationBlacklist = parseList(registrationBlacklistStr);
		}
	}

	private static boolean contains(List<CIDRAddress> addresses, String address) {
		if (address == null)
			return false;
		for (CIDRAddress cidrAddress : addresses) {
			if (cidrAddress.inRange(address)) {
				return true;
			}
		}
		return false;
	}

	private static List<CIDRAddress> parseList(String listStr) {
		String[] splitArray = listStr.split(",");
		List<CIDRAddress> splitList = new LinkedList<CIDRAddress>();
		for (String listEl : splitArray) {
			splitList.add(CIDRAddress.parse(listEl.trim()));
		}
		return splitList;
	}

	/**
	 * As in
	 * http://commons.apache.org/proper/commons-net/jacoco/org.apache.commons
	 * .net.util/SubnetUtils.java.html
	 */
	private static class CIDRAddress {

		static final int NBITS = 32;
		static final String IP_ADDRESS_MASK = "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})";
		static final String CIDR_ADDRESS_MASK = IP_ADDRESS_MASK + "/(\\d{1,3})";

		static final Pattern IP_PATTERN = Pattern.compile(IP_ADDRESS_MASK);
		static final Pattern CIDR_PATTERN = Pattern.compile(CIDR_ADDRESS_MASK);

		final int high;
		final int low;

		private CIDRAddress(int high, int low) {
			this.high = high;
			this.low = low;
		}

		boolean inRange(String address) {
			int diff = toInteger(address) - low;
			return diff >= 0 && (diff <= (high - low));
		}

		static int toInteger(String address) {
			Matcher matcher = IP_PATTERN.matcher(address);
			matcher.matches();
			return matchAddress(matcher);
		}

		static int matchAddress(Matcher matcher) {
			int addr = 0;
			for (int i = 1; i <= 4; ++i) {
				int n = Integer.parseInt(matcher.group(i));
				addr |= ((n & 0xff) << 8 * (4 - i));
			}
			return addr;
		}

		static CIDRAddress parse(String mask) {
			if (!mask.contains("/")) {
				mask = mask + "/" + NBITS;
			}
			Matcher matcher = CIDR_PATTERN.matcher(mask);
			matcher.matches();
			int address = matchAddress(matcher);

			/* Create a binary netmask from the number of bits specification /x */
			int cidrPart = Integer.parseInt(matcher.group(5));
			int netmask = 0;
			int broadcast = 0;
			int network = 0;
			for (int j = 0; j < cidrPart; ++j) {
				netmask |= (1 << 31 - j);
			}

			/* Calculate base network address */
			network = (address & netmask);

			/* Calculate broadcast address */
			broadcast = network | ~(netmask);
			return new CIDRAddress(broadcast, network);
		}
	}

	@Override
	public Element[] supDiscoFeatures(XMPPResourceConnection session) {
		if (log.isLoggable(Level.FINEST) && (session != null)) {
			log.finest("VHostItem: " + session.getDomain());
		}
		if ((session != null) && session.getDomain().isRegisterEnabled()) {
			return DISCO_FEATURES;
		} else {
			return null;
		}
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
	public Element[] supStreamFeatures(XMPPResourceConnection session) {
		if (log.isLoggable(Level.FINEST) && (session != null)) {
			log.finest("VHostItem: " + session.getDomain());
		}
		if ((session != null) && session.getDomain().isRegisterEnabled()) {
			return FEATURES;
		} else {
			return null;
		}
	}

	public boolean isSignedFormRequired() {
		return signedFormRequired;
	}
} // JabberIqRegister
