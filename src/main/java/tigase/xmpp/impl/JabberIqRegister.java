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

import tigase.db.*;
import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.form.*;
import tigase.kernel.beans.*;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.server.Message;
import tigase.server.*;
import tigase.server.xmppsession.SessionManager;
import tigase.stats.StatisticsList;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.vhosts.VHostItem;
import tigase.xml.Element;
import tigase.xml.XMLUtils;
import tigase.xmpp.*;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static tigase.xmpp.Authorization.NOT_ALLOWED;
import static tigase.xmpp.impl.CaptchaProvider.CaptchaItem;

/**
 * XEP-0077: In-Band Registration
 * <br>
 * Created: Thu Feb 16 13:14:06 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
@Bean(name = JabberIqRegister.ID, parent = SessionManager.class, active = true)
public class JabberIqRegister
		extends XMPPProcessor
		implements XMPPProcessorIfc, Initializable, UnregisterAware, RegistrarBean {

	public static final String ID = "jabber:iq:register";
	public static final String REGISTRATION_PER_SECOND_PROP_KEY = "registrations-per-second";
	public static final String REGISTRATION_BLACKLIST_PROP_KEY = "registration-blacklist";
	public static final String REGISTRATION_WHITELIST_PROP_KEY = "registration-whitelist";
	public static final String WHITELIST_REGISTRATION_ONLY_PROP_KEY = "whitelist-registration-only";
	/**
	 * OAuth details for form verifier.
	 */
	public static final String OAUTH_CONSUMERKEY_PROP_KEY = "oauth-consumer-key";
	public static final String OAUTH_CONSUMERSECRET_PROP_KEY = "oauth-consumer-secret";
	public static final String SIGNED_FORM_REQUIRED_PROP_KEY = "signed-form-required";
	private final static String INSTRUCTION_DEF = "Please provide the following information to sign up for an account";
	private final static String INSTRUCTION_EMAIL_REQUIRED_DEF = INSTRUCTION_DEF +
			"\n\nPlease also provide your e-mail address (must be valid!) to which we will send confirmation link.";
	private static final int REMOTE_ADDRESS_IDX = 2;
	private static final String[][] ELEMENTS = {Iq.IQ_QUERY_PATH};
	private static final String[] XMLNSS = {"jabber:iq:register"};
	private static final String[] IQ_QUERY_USERNAME_PATH = {Iq.ELEM_NAME, Iq.QUERY_NAME, "username"};
	private static final String[] IQ_QUERY_REMOVE_PATH = {Iq.ELEM_NAME, Iq.QUERY_NAME, "remove"};
	private static final String[] IQ_QUERY_PASSWORD_PATH = {Iq.ELEM_NAME, Iq.QUERY_NAME, "password"};
	private static final String[] IQ_QUERY_EMAIL_PATH = {Iq.ELEM_NAME, Iq.QUERY_NAME, "email"};
	private static final Element[] FEATURES = {
			new Element("register", new String[]{"xmlns"}, new String[]{"http://jabber.org/features/iq-register"})};
	private static final Element[] DISCO_FEATURES = {
			new Element("feature", new String[]{"var"}, new String[]{"jabber:iq:register"})};
	private static final BareJID smJid = BareJID.bareJIDInstanceNS("sess-man");
	private static Logger log = Logger.getLogger(JabberIqRegister.class.getName());
	@Inject
	private CaptchaProvider captchaProvider;
	@ConfigField(desc = "CAPTCHA Required")
	private boolean captchaRequired = false;
	@ConfigField(desc = "Email Required")
	private boolean emailRequired = true;
	@Inject
	private EventBus eventBus;
	@ConfigField(desc = "Instruction displayed during account registration")
	private String instruction = INSTRUCTION_DEF;
	@ConfigField(desc = "Instruction displayed during account registration when e-mail is required")
	private String instructionEmailRequired = INSTRUCTION_EMAIL_REQUIRED_DEF;
	@ConfigField(desc = "Maximum CAPTCHA repetition in session")
	private int maxCaptchaRepetition = 3;
	@ConfigField(desc = "OAuth consumer key", alias = OAUTH_CONSUMERKEY_PROP_KEY)
	private String oauthConsumerKey;
	@ConfigField(desc = "OAuth consumer secret", alias = OAUTH_CONSUMERSECRET_PROP_KEY)
	private String oauthConsumerSecret;
	@ConfigField(desc = "Registration blacklist", alias = REGISTRATION_BLACKLIST_PROP_KEY)
	private LinkedList<CIDRAddress> registrationBlacklist = new LinkedList<CIDRAddress>();
	@ConfigField(desc = "Registration whitelist", alias = REGISTRATION_WHITELIST_PROP_KEY)
	private LinkedList<CIDRAddress> registrationWhitelist = new LinkedList<CIDRAddress>();
	@ConfigField(desc = "Registrations per second", alias = REGISTRATION_PER_SECOND_PROP_KEY)
	private long registrationsPerSecond = 0;
	@ConfigField(desc = "Require form signed with OAuth", alias = SIGNED_FORM_REQUIRED_PROP_KEY)
	private boolean signedFormRequired = false;
	private long statsInvalidRegistrations;
	private long statsRegisteredUsers;
	private TokenBucketPool tokenBucket = new TokenBucketPool();
	@Inject
	private UserRepository userRepository;
	@Inject(nullAllowed = true)
	private AccountValidator[] validators = null;
	private String welcomeMessage = null;
	@ConfigField(desc = "Allow registration only for whitelisted addresses", alias = WHITELIST_REGISTRATION_ONLY_PROP_KEY)
	private boolean whitelistRegistrationOnly = false;

	private static boolean contains(List<CIDRAddress> addresses, String address) {
		if (address == null) {
			return false;
		}
		for (CIDRAddress cidrAddress : addresses) {
			if (cidrAddress.inRange(address)) {
				return true;
			}
		}
		return false;
	}

	private static LinkedList<CIDRAddress> parseList(String listStr) {
		String[] splitArray = listStr.split(",");
		LinkedList<CIDRAddress> splitList = new LinkedList<CIDRAddress>();
		for (String listEl : splitArray) {
			splitList.add(CIDRAddress.parse(listEl.trim()));
		}
		return splitList;
	}

	private static String parseRemoteAddressFromJid(JID from) {
		try {
			String connectionId = from.getResource();
			return connectionId.split("_")[REMOTE_ADDRESS_IDX];
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}

	public long getRegistrationsPerSecond() {
		return tokenBucket.getDefaultRate();
	}

	public void setRegistrationsPerSecond(long registrationsPerSecond) {
		tokenBucket.setDefaultRate(registrationsPerSecond);
	}

	public LinkedList<String> getRegistrationBlacklist() {
		return registrationBlacklist.stream()
				.map(cidr -> cidr.toString())
				.collect(Collectors.toCollection(LinkedList::new));
	}

	public void setRegistrationBlacklist(LinkedList<String> vals) {
		if (vals == null) {
			registrationBlacklist = new LinkedList<>();
		} else {
			registrationBlacklist = vals.stream()
					.map(val -> CIDRAddress.parse(val))
					.collect(Collectors.toCollection(LinkedList::new));
		}
	}

	public LinkedList<String> getRegistrationWhitelist() {
		return registrationWhitelist.stream()
				.map(cidr -> cidr.toString())
				.collect(Collectors.toCollection(LinkedList::new));
	}

	public void setRegistrationWhitelist(LinkedList<String> vals) {
		if (vals == null) {
			registrationWhitelist = new LinkedList<>();
		} else {
			registrationWhitelist = vals.stream()
					.map(val -> CIDRAddress.parse(val))
					.collect(Collectors.toCollection(LinkedList::new));
		}
	}

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
		tokenBucket.beforeUnregister();
	}

	@Override
	public void register(Kernel kernel) {
		kernel.registerBean("tokenBucketPool").asClass(TokenBucketPool.class).exec();
	}

	@Override
	public void unregister(Kernel kernel) {

	}

	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		list.add(getComponentInfo().getName(), "Registered users", statsRegisteredUsers, Level.INFO);
		list.add(getComponentInfo().getName(), "Invalid registrations", statsInvalidRegistrations, Level.INFO);
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void initialize() {
		tokenBucket.initialize();
		eventBus.registerAll(this);
		try {
			if (userRepository.userExists(smJid)) {
				welcomeMessage = userRepository.getData(smJid, ID, "welcome-message");
			} else {
				userRepository.addUser(smJid);
			}
		} catch (TigaseDBException ex) {
			log.log(Level.WARNING, "failed to read current welcome message from user repository", ex);
		}
	}

	public boolean isSignedFormRequired() {
		return signedFormRequired;
	}

	public void setSignedFormRequired(boolean required) {
		this.signedFormRequired = required;
	}

	@HandleEvent
	public void onWelcomeMessageChange(WelcomeMessageChangedEvent event) {
		this.welcomeMessage = event.getMessage();
	}

	/**
	 * {@inheritDoc}
	 * <br>
	 * TODO: Implement registration form configurable and loading all the fields from the registration form TODO:
	 * rewrite the plugin using the XMPPProcessorAbstract API
	 */
	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
						Queue<Packet> results, Map<String, Object> settings) throws XMPPException {
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

			// The user may send an request from his own account to register with a transport or any other service,
			// then the connection ID matches the session id but this is still not a request to the local server.
			// The TO address must be checked too.....
			if ((packet.getPacketFrom() != null) && packet.getPacketFrom().equals(session.getConnectionId()) &&
					(!session.isAuthorized() ||
							(session.isUserId(id) || session.isLocalDomain(id.toString(), false)))) {

				// We want to allow password change but not user registration if registration is disabled. The only
				// way to tell apart registration from password change is to check whether the user is authenticated.
				// For authenticated user the request means password change or account removal, otherwise registration
				// attempt. Account removal is also called under authenticated session, so it should be blocked if
				// registration for domain is disabled. If user cannot register account he cannot also remove account.
				Element request = packet.getElement();
				boolean isRemoveAccount = request.findChildStaticStr(IQ_QUERY_REMOVE_PATH) != null;

				if (!session.isAuthorized() || isRemoveAccount) {
					if (!isRegistrationAllowedForConnection(packet.getFrom())) {
						results.offer(NOT_ALLOWED.getResponseMessage(packet,
																	 "Registration is not allowed for this connection.",
																	 true));
						++statsInvalidRegistrations;
						return;
					}

					if (!session.getDomain().isRegisterEnabled()) {
						results.offer(NOT_ALLOWED.getResponseMessage(packet,
																	 "Registration is not allowed for this domain.",
																	 true));
						++statsInvalidRegistrations;
						return;
					}

					if (!isTokenInBucket(packet.getFrom())) {
						results.offer(NOT_ALLOWED.getResponseMessage(packet,
																	 "Server is busy. Too many registrations. Try later.",
																	 true));
						++statsInvalidRegistrations;
						return;
					}
				}

				StanzaType type = packet.getType();
				boolean isChangePassword = session.isAuthorized() && !isRemoveAccount;
				switch (type) {
					case set:
						if (isRemoveAccount) {
							doRemoveAccount(packet, request, session, results);
						} else if (isChangePassword) {
							doChangePassword(packet, request, session, results);
						} else {
							doRegisterNewAccount(packet, request, session, results);
						}
						break;
					case get: {
						doGetRegistrationForm(packet, request, session, results);
						break;
					}
					case result:
						// It might be a registration request from transport for example...
						Packet pack_res = packet.copyElementOnly();
						pack_res.setPacketTo(session.getConnectionId());
						results.offer(pack_res);
						break;

					default:
						results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Message type is incorrect",
																				   true));
						break;
				} // end of switch (type)
			} else {
				if (session.isUserId(id)) {
					// It might be a registration request from transport for example...
					Packet pack_res = packet.copyElementOnly();

					pack_res.setPacketTo(session.getConnectionId());
					results.offer(pack_res);
				} else {
					results.offer(packet.copyElementOnly());
				}
			}
		} catch (XMPPProcessorException e) {
			++statsInvalidRegistrations;
			Packet result = e.makeElement(packet, true);
			results.offer(result);
		} catch (TigaseStringprepException ex) {
			++statsInvalidRegistrations;
			results.offer(Authorization.JID_MALFORMED.getResponseMessage(packet,
																		 "Incorrect user name, stringprep processing failed.",
																		 true));
		} catch (NotAuthorizedException e) {
			++statsInvalidRegistrations;
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
																		  "You are not authorized to change registration settings.\n" +
																				  e.getMessage(), true));
		} catch (TigaseDBException e) {
			log.warning("Database problem: " + e);
			results.offer(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet,
																				 "Database access problem, please contact administrator.",
																				 true));
		}
	}

	public void setOAuthCredentials(String oauthConsumerKey, String oauthConsumerSecret) {
		this.oauthConsumerKey = oauthConsumerKey;
		this.oauthConsumerSecret = oauthConsumerSecret;
	}

	public void setWelcomeMessage(String message) throws TigaseDBException {
		if (message != null) {
			userRepository.setData(smJid, ID, "welcome-message", message);
		} else {
			userRepository.removeData(smJid, ID, "welcome-message");
		}
		eventBus.fire(new WelcomeMessageChangedEvent(message));
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

	protected void createAccount(XMPPResourceConnection session, String user_name, VHostItem domain, String password,
								 String email, Map<String, String> reg_params)
			throws XMPPProcessorException, TigaseStringprepException, TigaseDBException {

		final BareJID jid = BareJID.bareJIDInstance(user_name, domain.getVhost().getDomain());

		if (validators != null) {
			for (AccountValidator validator : validators) {
				validator.checkRequiredParameters(jid, reg_params);
			}
		}

		try {
			session.getAuthRepository().addUser(jid, password);

			boolean confirmationRequired = false;
			if (validators != null) {
				for (AccountValidator validator : validators) {
					confirmationRequired |= validator.sendAccountValidation(jid, reg_params);
				}
				if (confirmationRequired) {
					session.getAuthRepository().setAccountStatus(jid, AuthRepository.AccountStatus.pending);
				}
			}

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "User added: {0}, pass: {1}",
						new Object[]{BareJID.toString(user_name, domain.getVhost().getDomain()), password});
			}
			++statsRegisteredUsers;
			session.setRegistration(user_name, password, reg_params);
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Registration data set for: {0}, pass: {1}, reg_params: {2}",
						new Object[]{BareJID.toString(user_name, domain.getVhost().getDomain()), password, reg_params});
			}
			eventBus.fire(new UserRegisteredEvent(jid, email, confirmationRequired, reg_params));
		} catch (UserExistsException e) {
			throw new XMPPProcessorException(Authorization.CONFLICT);
		}
	}

	protected void doGetRegistrationForm(Packet packet, Element request, XMPPResourceConnection session,
										 Queue<Packet> results) throws XMPPProcessorException, NoConnectionIdException {
		if (captchaRequired) {
			results.offer(packet.okResult(prepareCaptchaRegistrationForm(session), 0));
		} else if (signedFormRequired) {
			results.offer(packet.okResult(prepareSignedRegistrationForm(session), 0));
		} else if (emailRequired) {
			results.offer(packet.okResult(prepareEmailRegistrationForm(), 0));
		} else {
			results.offer(packet.okResult(
					"<instructions>" + "Choose a user name and password for use with this service." +
							"</instructions>" + "<username/>" + "<password/>", 1));
		}

	}

	protected void doRemoveAccount(final Packet packet, final Element request, final XMPPResourceConnection session,
								   final Queue<Packet> results)
			throws XMPPProcessorException, NoConnectionIdException, PacketErrorTypeException, NotAuthorizedException,
				   TigaseStringprepException, TigaseDBException {
		// Yes this is registration cancel request. According to XEP-0077 there
		// must not be any more subelements apart from <remove/>
		Element elem = request.findChildStaticStr(Iq.IQ_QUERY_PATH);
		if (elem.getChildren().size() > 1) {
			throw new XMPPProcessorException(Authorization.BAD_REQUEST);
		}

		if (!session.isAuthorized()) {
			throw new XMPPProcessorException(Authorization.FORBIDDEN);
		}

		final String user_name = packet.getStanzaFrom().getLocalpart();

		if (!session.getUserName().equals(user_name)) {
			throw new XMPPProcessorException(Authorization.FORBIDDEN);
		}

		session.getAuthRepository()
				.removeUser(BareJID.bareJIDInstance(user_name, session.getDomain().getVhost().getDomain()));
		try {
			userRepository.removeUser(BareJID.bareJIDInstance(user_name, session.getDomain().getVhost().getDomain()));
		} catch (UserNotFoundException ex) {
			// We ignore this error here. If auth_repo and user_repo are in fact
			// the same database, then user has been already removed with the auth_repo.removeUser(...)
			// then the second call to user_repo may throw the exception which is fine.
		}

		session.logout();

		Packet ok_result = packet.okResult((String) null, 0);

		// We have to set SYSTEM priority for the packet here, otherwise the network connection is closed
		// before the client received a response
		ok_result.setPriority(Priority.SYSTEM);
		results.offer(ok_result);

		Packet close_cmd = Command.CLOSE.getPacket(session.getSMComponentId(), session.getConnectionId(),
												   StanzaType.set, session.nextStanzaId());

		close_cmd.setPacketTo(session.getConnectionId());
		close_cmd.setPriority(Priority.LOWEST);
		results.offer(close_cmd);
	}

	protected boolean isRegistrationAllowedForConnection(JID from) {
		String remoteAdress = parseRemoteAddressFromJid(from);
		if (whitelistRegistrationOnly) {
			return contains(registrationWhitelist, remoteAdress);
		}
		return !contains(registrationBlacklist, remoteAdress);
	}

	protected boolean isTokenInBucket(final JID from) {
		String remoteAdress = parseRemoteAddressFromJid(from);
		if (remoteAdress == null || remoteAdress.isEmpty()) {
			remoteAdress = "<default>";
		}
		return tokenBucket.consume(remoteAdress);
	}

	private tigase.server.Message createWelcomeMessage(String username, XMPPResourceConnection session)
			throws TigaseStringprepException {
		if (welcomeMessage == null) {
			return null;
		}

		JID jid = JID.jidInstance(username, session.getDomainAsJID().getDomain());

		Element messageEl = new Element("message");
		messageEl.setXMLNS(Message.CLIENT_XMLNS);
		messageEl.addChild(new Element("body", welcomeMessage));

		return new Message(messageEl, session.getDomainAsJID(), jid);
	}

	private void doChangePassword(Packet packet, Element request, XMPPResourceConnection session, Queue<Packet> results)
			throws XMPPProcessorException, NoConnectionIdException, TigaseStringprepException, NotAuthorizedException,
				   TigaseDBException {
		String user_name = request.getChildCDataStaticStr(IQ_QUERY_USERNAME_PATH);
		String password = request.getChildCDataStaticStr(IQ_QUERY_PASSWORD_PATH);
		if (null != password) {
			password = XMLUtils.unescape(password);
		}

		if ((user_name == null) || user_name.isBlank() || (password == null) || password.isBlank()) {
			throw new XMPPProcessorException(Authorization.NOT_ACCEPTABLE);
		}

		session.setRegistration(user_name, password, Collections.emptyMap());
		results.offer(packet.okResult((String) null, 0));
	}

	private void doRegisterNewAccount(Packet packet, Element request, XMPPResourceConnection session,
									  Queue<Packet> results)
			throws XMPPProcessorException, NoConnectionIdException, TigaseStringprepException, NotAuthorizedException,
				   TigaseDBException {

		final VHostItem domain = session.getDomain();
		if (!domain.isRegisterEnabled()) {
			throw new NotAuthorizedException("Registration is now allowed for this domain");
		}

		if (domain.getMaxUsersNumber() > 0) {
			long domainUsers = session.getAuthRepository().getUsersCount(domain.getVhost().getDomain());

			if (log.isLoggable(Level.FINEST)) {
				log.finest(
						"Current number of users for domain: " + domain.getVhost().getDomain() + " is: " + domainUsers);
			}
			if (domainUsers >= domain.getMaxUsersNumber()) {
				throw new NotAuthorizedException("Maximum users number for the domain exceeded.");
			}
		}

		Element queryEl = request.getChild("query", "jabber:iq:register");
		Element formEl = queryEl == null ? null : queryEl.getChild("x", "jabber:x:data");
		Form form = formEl == null ? null : new Form(formEl);

		if ((captchaRequired || signedFormRequired) && (form == null)) {
			throw new XMPPProcessorException(Authorization.BAD_REQUEST, "Use proper DataForm registration");
		}

		if (captchaRequired) {
			validatCapchaForm(session, form);
		} else if (signedFormRequired) {
			validateSignedForm(packet, session, form);
		}

		String user_name =
				form != null ? form.getAsString("username") : request.getChildCDataStaticStr(IQ_QUERY_USERNAME_PATH);
		String password =
				form != null ? form.getAsString("password") : request.getChildCDataStaticStr(IQ_QUERY_PASSWORD_PATH);
		String email = form != null ? form.getAsString("email") : request.getChildCDataStaticStr(IQ_QUERY_EMAIL_PATH);

		if (null != password) {
			password = XMLUtils.unescape(password);
		}

		if ((user_name == null) || user_name.equals("") || (password == null) || password.equals("")) {
			throw new XMPPProcessorException(Authorization.NOT_ACCEPTABLE);
		}

		if (emailRequired && (email == null || email.isBlank())) {
			throw new XMPPProcessorException(Authorization.NOT_ACCEPTABLE);
		}

		Map<String, String> reg_params = Collections.emptyMap();
		if ((email != null) && !email.isBlank()) {
			reg_params = new LinkedHashMap<>();
			reg_params.put("email", email.trim());
		}

		createAccount(session, user_name, domain, password, email, reg_params);

		session.removeSessionData("jabber:iq:register:captcha");
		String localPart = BareJID.parseJID(user_name)[0];
		if (localPart == null || localPart.isEmpty()) {
			localPart = user_name;
		}
		tigase.server.Message msg = createWelcomeMessage(localPart, session);
		if (msg != null) {
			results.offer(msg);
		}

//		if (emailRequired) {
//			Element query = new Element("query");
//			query.setXMLNS("jabber:iq:register");
//			query.addChild(new Element("instructions",
//									   "Please click on a link sent co provided e-mail address to activate your account"));
//			query.addChild(new Element("email-confirmation-required"));
//			results.offer(packet.okResult(query, 0));
//		} else {
			results.offer(packet.okResult((String) null, 0));
//		}
	}

	private void validateSignedForm(Packet packet, XMPPResourceConnection session, Form form)
			throws NoConnectionIdException, XMPPProcessorException {
		final String expectedToken = UUID.nameUUIDFromBytes(
				(session.getConnectionId() + "|" + session.getSessionId()).getBytes()).toString();

		FormSignatureVerifier verifier = new FormSignatureVerifier(oauthConsumerKey, oauthConsumerSecret);
		if (!expectedToken.equals(form.getAsString("oauth_token"))) {
			log.finest("Received oauth_token is different that sent one.");
			throw new XMPPProcessorException(Authorization.BAD_REQUEST, "Unknown oauth_token");
		}
		if (!oauthConsumerKey.equals(form.getAsString("oauth_consumer_key"))) {
			log.finest("Unknown oauth_consumer_key");
			throw new XMPPProcessorException(Authorization.BAD_REQUEST, "Unknown oauth_consumer_key");
		}
		try {
			long timestamp = verifier.verify(packet.getStanzaTo(), form);
		} catch (FormSignerException e) {
			log.fine("Form Signature Validation Problem: " + e.getMessage());
			throw new XMPPProcessorException(Authorization.BAD_REQUEST, "Invalid form signature");
		}
	}

	private void validatCapchaForm(XMPPResourceConnection session, Form form) throws XMPPProcessorException {
		CaptchaItem captcha = (CaptchaItem) session.getSessionData("jabber:iq:register:captcha");

		if (captcha == null) {
			log.finest("CAPTCHA is required");
			throw new XMPPProcessorException(Authorization.BAD_REQUEST,
											 "CAPTCHA is required. Please reload your registration form.");
		}

		String capResp = form.getAsString("captcha");

		if (!captcha.isResponseValid(session, capResp)) {
			captcha.incraseErrorCounter();
			log.finest("Invalid captcha");

			if (captcha.getErrorCounter() >= maxCaptchaRepetition) {
				log.finest("Blocking session with not-solved captcha");
				session.removeSessionData("jabber:iq:register:captcha");
			}
			throw new XMPPProcessorException(NOT_ALLOWED, "Invalid captcha");
		}
	}

	private Element prepareCaptchaRegistrationForm(final XMPPResourceConnection session)
			throws NoConnectionIdException {
		Element query = new Element("query", new String[]{"xmlns"}, XMLNSS);
		query.addChild(new Element("instructions", (emailRequired ? INSTRUCTION_EMAIL_REQUIRED_DEF : INSTRUCTION_DEF)));
		Form form = prepareGenericRegistrationForm();

		CaptchaItem captcha = captchaProvider.generateCaptcha();
		session.putSessionData("jabber:iq:register:captcha", captcha);
		Field field = Field.fieldTextSingle("captcha", "", captcha.getCaptchaRequest(session));
		field.setRequired(true);
		form.addField(field);

		query.addChild(form.getElement());
		return query;
	}

	private Element prepareSignedRegistrationForm(final XMPPResourceConnection session) throws NoConnectionIdException {
		Element query = new Element("query", new String[]{"xmlns"}, XMLNSS);
		query.addChild(new Element("instructions", (emailRequired ? INSTRUCTION_EMAIL_REQUIRED_DEF : INSTRUCTION_DEF)));
		Form form = prepareGenericRegistrationForm();

		SignatureCalculator sc = new SignatureCalculator(oauthConsumerKey, oauthConsumerSecret);
		sc.setOauthToken(UUID.nameUUIDFromBytes((session.getConnectionId() + "|" + session.getSessionId()).getBytes())
								 .toString());
		sc.addEmptyFields(form);

		query.addChild(form.getElement());
		return query;
	}

	private Element prepareEmailRegistrationForm() throws NoConnectionIdException {
		Element query = new Element("query", new String[]{"xmlns"}, XMLNSS);
		query.addChild(new Element("instructions", (emailRequired ? INSTRUCTION_EMAIL_REQUIRED_DEF : INSTRUCTION_DEF)));
		Form form = prepareGenericRegistrationForm();
		query.addChild(form.getElement());
		return query;
	}


	private Form prepareGenericRegistrationForm() {
		Form form = new Form("form", "Account Registration", (emailRequired ? INSTRUCTION_EMAIL_REQUIRED_DEF
																			: INSTRUCTION_DEF));

		form.addField(Field.fieldHidden("FORM_TYPE", "jabber:iq:register"));

		Field field = Field.fieldTextSingle("username", "", "Username");
		field.setRequired(true);
		form.addField(field);
		field = Field.fieldTextPrivate("password", "", "Password");
		field.setRequired(true);
		form.addField(field);
		field = Field.fieldTextSingle("email", "", "Email");
		if (emailRequired) {
			field.setRequired(true);
			field.setLabel("Email (MUST BE VALID!)");
		}
		form.addField(field);
		return form;
	}

	public interface AccountValidator {

		void checkRequiredParameters(BareJID jid, Map<String, String> reg_params) throws XMPPProcessorException;

		boolean sendAccountValidation(BareJID jid, Map<String, String> reg_params);

		BareJID validateAccount(String token);

	}

	/**
	 * As in http://commons.apache.org/proper/commons-net/jacoco/org.apache.commons .net.util/SubnetUtils.java.html
	 */
	public static class CIDRAddress {

		static final int NBITS = 32;

		static final String IP_ADDRESS_MASK = "(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})";

		static final String CIDR_ADDRESS_MASK = IP_ADDRESS_MASK + "/(\\d{1,3})";

		static final Pattern IP_PATTERN = Pattern.compile(IP_ADDRESS_MASK);

		static final Pattern CIDR_PATTERN = Pattern.compile(CIDR_ADDRESS_MASK);

		final int high;

		final int low;

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

			/*
			 * Create a binary netmask from the number of bits specification /x
			 */
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

		static int toInteger(String address) {
			Matcher matcher = IP_PATTERN.matcher(address);
			matcher.matches();
			return matchAddress(matcher);
		}

		private CIDRAddress(int high, int low) {
			this.high = high;
			this.low = low;
		}

		@Override
		public String toString() {
			int mask = 0;
			boolean z = false;
			for (int j = 3; j >= 0; j--) {
				int shift = j * 8;
				byte b = (byte) ((~(low >> shift & 0xff) ^ (high >> shift & 0xff)) & 0xff);
				int m = 0x80;
				for (int i = 0; i < 8; i++) {
					if ((b & m) == 0) {
						z = true;
					} else {
						mask++;
					}
					m >>>= 1;
				}
			}
			return String.format("%d.%d.%d.%d/%d", (low >> 24 & 0xff), (low >> 16 & 0xff), (low >> 8 & 0xff),
								 (low & 0xff), mask);
		}

		boolean inRange(String address) {
			int diff = toInteger(address) - low;
			return diff >= 0 && (diff <= (high - low));
		}
	}

	public static class UserRegisteredEvent {

		private boolean confirmationRequired;
		private String email;
		private Map<String, String> params;
		private BareJID user;

		public UserRegisteredEvent(BareJID user, String email, boolean confirmationRequired,
								   Map<String, String> params) {
			this.user = user;
			this.email = email;
			this.params = params;
			this.confirmationRequired = confirmationRequired;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public Map<String, String> getParams() {
			return params;
		}

		public void setParams(Map<String, String> params) {
			this.params = params;
		}

		public BareJID getUser() {
			return user;
		}

		public void setUser(BareJID user) {
			this.user = user;
		}

		public boolean isConfirmationRequired() {
			return confirmationRequired;
		}

		public void setConfirmationRequired(boolean confirmationRequired) {
			this.confirmationRequired = confirmationRequired;
		}
	}

	public static class WelcomeMessageChangedEvent
			implements Serializable {

		private String message;

		public WelcomeMessageChangedEvent() {
		}

		public WelcomeMessageChangedEvent(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}

	}
}