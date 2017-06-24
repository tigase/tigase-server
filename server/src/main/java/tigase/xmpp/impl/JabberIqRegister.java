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

import tigase.db.*;
import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.form.*;
import tigase.kernel.beans.*;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.server.*;
import tigase.server.Message;
import tigase.server.xmppsession.SessionManager;
import tigase.stats.StatisticsList;
import tigase.util.TigaseStringprepException;
import tigase.vhosts.VHostItem;
import tigase.xml.Element;
import tigase.xml.XMLUtils;
import tigase.xmpp.*;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * JEP-0077: In-Band Registration
 * <p>
 * <p>
 * Created: Thu Feb 16 13:14:06 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Bean(name = JabberIqRegister.ID, parent = SessionManager.class, active = true)
public class JabberIqRegister
		extends XMPPProcessor
		implements XMPPProcessorIfc, Initializable, UnregisterAware, RegistrarBean
{

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
	@Inject
	private EventBus eventBus;
	@Inject(nullAllowed = true)
	private AccountValidator[] validators = null;
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
		return registrationBlacklist.stream().map( cidr -> cidr.toString()).collect(Collectors.toCollection(LinkedList::new));
	}

	public void setRegistrationBlacklist(LinkedList<String> vals) {
		if (vals == null) {
			registrationBlacklist = new LinkedList<>();
		} else {
			registrationBlacklist = vals.stream().map(val -> CIDRAddress.parse(val)).collect(Collectors.toCollection(LinkedList::new));
		}
	}

	public LinkedList<String> getRegistrationWhitelist() {
		return registrationWhitelist.stream().map( cidr -> cidr.toString()).collect(Collectors.toCollection(LinkedList::new));
	}

	public void setRegistrationWhitelist(LinkedList<String> vals) {
		if (vals == null) {
			registrationWhitelist = new LinkedList<>();
		} else {
			registrationWhitelist = vals.stream().map(val -> CIDRAddress.parse(val)).collect(Collectors.toCollection(LinkedList::new));
		}
	}

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
	}

	@Override
	public void register(Kernel kernel) {
		kernel.registerBean("tokenBucketPool").asClass(TokenBucketPool.class).exec();
	}

	@Override
	public void unregister(Kernel kernel) {

	}

	protected void createAccount(XMPPResourceConnection session, String user_name, VHostItem domain, String password,
								 String email, Map<String, String> reg_params)
			throws XMPPProcessorException, TigaseStringprepException, TigaseDBException {

		final BareJID jid = BareJID.bareJIDInstanceNS(user_name, domain.getVhost().getDomain());

		if (validators != null) {
			for (AccountValidator validator : validators) {
				validator.checkRequiredParameters(jid, reg_params);
			}
		}

		try {
			session.getAuthRepository()
					.addUser(BareJID.bareJIDInstance(user_name, domain.getVhost().getDomain()), password);


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

	protected void doGetRegistrationForm(Packet packet, Element request, XMPPResourceConnection session,
										 Queue<Packet> results) throws XMPPProcessorException, NoConnectionIdException {
		if (captchaRequired) {
			// captcha
			results.offer(packet.okResult(prepareCaptchaRegistrationForm(session), 0));
		} else if (signedFormRequired) {
			results.offer(packet.okResult(prepareSignedRegistrationForm(session), 0));
		} else {
			results.offer(packet.okResult(
					"<instructions>" + "Choose a user name and password for use with this service." +
							"Please provide also your e-mail address." + "</instructions>" + "<username/>" +
							"<password/>" + "<email/>", 1));
		}

	}

	private void doRegisterNewAccount(Packet packet, Element request, XMPPResourceConnection session,
									  Queue<Packet> results)
			throws XMPPProcessorException, NoConnectionIdException, TigaseStringprepException, NotAuthorizedException,
				   TigaseDBException {
		// Is it registration cancel request?
		String user_name;
		String password;
		String email;
		if (captchaRequired) {
			CaptchaProvider.CaptchaItem captcha = (CaptchaProvider.CaptchaItem) session.getSessionData(
					"jabber:iq:register:captcha");

			if (captcha == null) {
				log.finest("CAPTCHA is required");
				throw new XMPPProcessorException(Authorization.BAD_REQUEST,
												 "CAPTCHA is required. Please reload your registration form.");
			}

			Element queryEl = request.getChild("query", "jabber:iq:register");
			Element formEl = queryEl == null ? null : queryEl.getChild("x", "jabber:x:data");
			Form form = new Form(formEl);

			String capResp = form.getAsString("captcha");

			if (!captcha.isResponseValid(session, capResp)) {
				captcha.incraseErrorCounter();
				log.finest("Invalid captcha");

				if (captcha.getErrorCounter() >= maxCaptchaRepetition) {
					log.finest("Blocking session with not-solved captcha");
					session.removeSessionData("jabber:iq:register:captcha");
				}
				throw new XMPPProcessorException(Authorization.NOT_ALLOWED, "Invalid captcha");
			}

			user_name = form.getAsString("username");
			password = form.getAsString("password");
			email = form.getAsString("email");
		} else if (signedFormRequired) {
			final String expectedToken = UUID.nameUUIDFromBytes(
					(session.getConnectionId() + "|" + session.getSessionId()).getBytes()).toString();

			FormSignatureVerifier verifier = new FormSignatureVerifier(oauthConsumerKey, oauthConsumerSecret);
			Element queryEl = request.getChild("query", "jabber:iq:register");
			Element formEl = queryEl == null ? null : queryEl.getChild("x", "jabber:x:data");
			if (formEl == null) {
				throw new XMPPProcessorException(Authorization.BAD_REQUEST, "Use Signed Registration Form");
			}
			Form form = new Form(formEl);
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
				user_name = form.getAsString("username");
				password = form.getAsString("password");
				email = form.getAsString("email");
			} catch (FormSignerException e) {
				log.fine("Form Signature Validation Problem: " + e.getMessage());
				throw new XMPPProcessorException(Authorization.BAD_REQUEST, "Invalid form signature");
			}
		} else {
			// No, so assuming this is registration of a new
			// user or change registration details for existing
			// user
			user_name = request.getChildCDataStaticStr(IQ_QUERY_USERNAME_PATH);
			password = request.getChildCDataStaticStr(IQ_QUERY_PASSWORD_PATH);
			email = request.getChildCDataStaticStr(IQ_QUERY_EMAIL_PATH);
		}
		if (null != password) {
			password = XMLUtils.unescape(password);
		}
		Map<String, String> reg_params = null;

		if ((email != null) && !email.trim().isEmpty()) {
			reg_params = new LinkedHashMap<String, String>();
			reg_params.put("email", email);
		}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		if ((user_name == null) || user_name.equals("") || (password == null) || password.equals("")) {
			throw new XMPPProcessorException(Authorization.NOT_ACCEPTABLE);
		}

		if (session.isAuthorized()) {
			session.setRegistration(user_name, password, reg_params);
			results.offer(packet.okResult((String) null, 0));
			return;
		}

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

		createAccount(session, user_name, domain, password, email, reg_params);

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		session.removeSessionData("jabber:iq:register:captcha");
		String localPart = BareJID.parseJID(user_name)[0];
		if (localPart == null || localPart.isEmpty()) {
			localPart = user_name;
		}
		tigase.server.Message msg = createWelcomeMessage(localPart, session);
		if (msg != null) {
			results.offer(msg);
		}

		results.offer(packet.okResult((String) null, 0));
	}

	protected void doRemoveAccount(final Packet packet, final Element request, final XMPPResourceConnection session,
								   final Queue<Packet> results)
			throws XMPPProcessorException, NoConnectionIdException, PacketErrorTypeException, NotAuthorizedException,
				   TigaseStringprepException, TigaseDBException {
		// Yes this is registration cancel request
		// According to JEP-0077 there must not be any
		// more subelements apart from <remove/>
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
			// the same
			// database, then user has been already removed with the
			// auth_repo.removeUser(...)
			// then the second call to user_repo may throw the exception which is
			// fine.
		}

		session.logout();

		Packet ok_result = packet.okResult((String) null, 0);

		// We have to set SYSTEM priority for the packet
		// here,
		// otherwise the network connection is closed
		// before the
		// client received a response
		ok_result.setPriority(Priority.SYSTEM);
		results.offer(ok_result);

		Packet close_cmd = Command.CLOSE.getPacket(session.getSMComponentId(), session.getConnectionId(),
												   StanzaType.set, session.nextStanzaId());

		close_cmd.setPacketTo(session.getConnectionId());
		close_cmd.setPriority(Priority.LOWEST);
		results.offer(close_cmd);
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
		eventBus.registerAll(this);
		try {
			if (userRepository.userExists(smJid)) {
				welcomeMessage = userRepository.getData(smJid, ID, "welcome-message");
			}
		} catch (TigaseDBException ex) {
			log.log(Level.WARNING, "failed to read current welcome message from user repository", ex);
		}
	}

	protected boolean isRegistrationAllowedForConnection(JID from) {
		String remoteAdress = parseRemoteAddressFromJid(from);
		if (whitelistRegistrationOnly) {
			return contains(registrationWhitelist, remoteAdress);
		}
		return !contains(registrationBlacklist, remoteAdress);
	}

	public boolean isSignedFormRequired() {
		return signedFormRequired;
	}

	public void setSignedFormRequired(boolean required) {
		this.signedFormRequired = required;
	}

	protected boolean isTokenInBucket(final JID from) {
		String remoteAdress = parseRemoteAddressFromJid(from);
		if (remoteAdress == null || remoteAdress.isEmpty()) {
			remoteAdress = "<default>";
		}
		return tokenBucket.consume(remoteAdress);
	}

	@HandleEvent
	public void onWelcomeMessageChange(WelcomeMessageChangedEvent event) {
		this.welcomeMessage = event.getMessage();
	}

	private Element prepareCaptchaRegistrationForm(final XMPPResourceConnection session)
			throws NoConnectionIdException {
		Element query = new Element("query", new String[]{"xmlns"}, XMLNSS);
		query.addChild(new Element("instructions", "Use the enclosed form to register."));
		Form form = new Form("form", "Contest Registration",
							 "Please provide the following information to sign up for our special contests!");
		form.addField(Field.fieldHidden("FORM_TYPE", "jabber:iq:register"));

		Field field = Field.fieldTextSingle("username", "", "Username");
		field.setRequired(true);
		form.addField(field);
		field = Field.fieldTextPrivate("password", "", "Password");
		field.setRequired(true);
		form.addField(field);
		field = Field.fieldTextSingle("email", "", "Email");
		field.setRequired(true);
		form.addField(field);

		CaptchaProvider.CaptchaItem captcha = captchaProvider.generateCaptcha();
		session.putSessionData("jabber:iq:register:captcha", captcha);
		field = Field.fieldTextSingle("captcha", "", captcha.getCaptchaRequest(session));
		field.setRequired(true);
		form.addField(field);

		query.addChild(form.getElement());
		return query;
	}

	private Element prepareSignedRegistrationForm(final XMPPResourceConnection session) throws NoConnectionIdException {
		Element query = new Element("query", new String[]{"xmlns"}, XMLNSS);
		query.addChild(new Element("instructions", "Use the enclosed form to register."));
		Form form = new Form(SignatureCalculator.SUPPORTED_TYPE, "Contest Registration",
							 "Please provide the following information to sign up for our special contests!");

		form.addField(Field.fieldTextSingle("username", "", "Username"));
		form.addField(Field.fieldTextPrivate("password", "", "Password"));
		form.addField(Field.fieldTextSingle("email", "", "Email"));

		SignatureCalculator sc = new SignatureCalculator(oauthConsumerKey, oauthConsumerSecret);
		sc.setOauthToken(UUID.nameUUIDFromBytes((session.getConnectionId() + "|" + session.getSessionId()).getBytes())
								 .toString());
		sc.addEmptyFields(form);

		query.addChild(form.getElement());
		return query;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <br><br>
	 * <p>
	 * TODO: Implement registration form configurable and loading
	 * all the fields from the registration form TODO: rewrite the
	 * plugin using the XMPPProcessorAbstract API
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
			if ((packet.getPacketFrom() != null) && packet.getPacketFrom().equals(session.getConnectionId()) &&
					(!session.isAuthorized() ||
							(session.isUserId(id) || session.isLocalDomain(id.toString(), false)))) {

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
																				   "Registration is not allowed for this connection.",
																				   true));
						++statsInvalidRegistrations;
						return;
					}

					if (!session.getDomain().isRegisterEnabled()) {
						results.offer(Authorization.NOT_ALLOWED.getResponseMessage(packet,
																				   "Registration is not allowed for this domain.",
																				   true));
						++statsInvalidRegistrations;
						return;
					}

					if (!isTokenInBucket(packet.getFrom())) {
						results.offer(Authorization.NOT_ALLOWED.getResponseMessage(packet,
																				   "Server is busy. Too many registrations. Try later.",
																				   true));
						++statsInvalidRegistrations;
						return;
					}
				}

				Authorization result = Authorization.NOT_AUTHORIZED;
				StanzaType type = packet.getType();

				switch (type) {
					case set:
						Element removeElem = request.findChildStaticStr(IQ_QUERY_REMOVE_PATH);
						if (removeElem != null) {
							doRemoveAccount(packet, request, session, results);
						} else {
							doRegisterNewAccount(packet, request, session, results);
						}
						break;
					case get: {
						doGetRegistrationForm(packet, request, session, results);
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
						results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Message type is incorrect",
																				   true));

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
		userRepository.setData(smJid, ID, "welcome-message", message);
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

	public interface AccountValidator {

		void checkRequiredParameters(BareJID jid, Map<String,String> reg_params) throws XMPPProcessorException;

		boolean sendAccountValidation(BareJID jid, Map<String,String> reg_params);

		BareJID validateAccount(String token);

	}
	
	/**
	 * As in
	 * http://commons.apache.org/proper/commons-net/jacoco/org.apache.commons
	 * .net.util/SubnetUtils.java.html
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

		boolean inRange(String address) {
			int diff = toInteger(address) - low;
			return diff >= 0 && (diff <= (high - low));
		}

		@Override
		public String toString() {
			int mask = 0;
			boolean z = false;
			for (int j=3; j>=0; j--) {
				int shift = j * 8;
				byte b = (byte) ((~(low >> shift & 0xff) ^ (high >> shift & 0xff)) & 0xff);
				int m = 0x80;
				for (int i=0; i<8; i++) {
					if ((b & m) == 0) {
						z = true;
					} else {
						mask++;
					}
					m >>>= 1;
				}
			}
			return String.format("%d.%d.%d.%d/%d",
						  (low >> 24 & 0xff),
						  (low >> 16 & 0xff),
						  (low >> 8 & 0xff),
						  (low & 0xff), mask);
		}
	}

	public static class UserRegisteredEvent {

		private String email;
		private boolean confirmationRequired;
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