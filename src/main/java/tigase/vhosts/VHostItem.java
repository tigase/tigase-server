/*
 * VHostItem.java
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



package tigase.vhosts;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.comp.RepositoryItemAbstract;

import tigase.server.Command;
import tigase.server.Packet;

import tigase.util.DataTypes;
import tigase.util.TigaseStringprepException;

import tigase.xml.Element;

import tigase.xmpp.JID;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Objects of this class represent virtual host with all hosts configuration
 * settings. In most cases simple domain name string is not enough to deal with
 * the virtual host. VHost can be enabled/disabled. Can be available to selected
 * server components only and so on. Therefore every time there is a detailed
 * information needed for a vhost this classed must be used.
 *
 * This class has it's own XML representation which can be used for creating an
 * instance of the class or can be exported to the XML form for permanent
 * storage:
 *
 * <pre>
 * <vhost hostname="vhost.something.com"
 *        enabled="true"
 *        anon="true"
 *        register="true"
 *        max-users="99999999999L">
 *   <comps/>
 *   <other/>
 * </pre>
 *
 * From the init.property file it is also possible to set additional parameters
 * for the vhost. By default everything is enabled and max accounts set to
 * unlimited. In the example below we configure 2 domains:
 * <strong>devel.tigase.org</strong> and <strong>test.tigase.org</strong>. For
 * the first domain there are no additional settings, hence the domain has
 * everything on by default, whereas the second has everything switched off and
 * max user accounts set to 100.
 *
 * <pre>
 * --virt-hosts = devel.tigase.org,test.tigase.org:-anon:-register:max-users=100
 * </pre>
 *
 * It also possible to set forwarding for the domain:
 *
 * <pre>
 * --virt-hosts = test.tigase.org:pres-forw=lpart@domain/res:mess-forw=lpart@domain/res
 * </pre>
 *
 * Please note, forwarding address set this way cannot contain any of
 * characters: [,:=] The order features are set for domain is unimportant.
 *
 * Created: 22 Nov 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class VHostItem
				extends RepositoryItemAbstract {
	/**
	 * This is an attribute name for storing information whether anonymous user
	 * can login for this domain.
	 */
	public static final String ANONYMOUS_ENABLED_ATT = "anon";

	/** Field description */
	public static final String ANONYMOUS_ENABLED_LABEL = "Anonymous enabled";

	/**
	 * Element name for the VHostItem XML child keeping list of the server
	 * component which can handle packets for this domain. In most cases this
	 * element should be empty.
	 */
	public static final String COMPONENTS_ELEM = "comps";

	/** Field description */
	public static final String DOMAIN_FILTER_POLICY_ATT = "domain-filter";

	/** Field description */
	public static final String DOMAIN_FILTER_POLICY_LABEL = "Domain filter policy";

	/**
	 * This is an attribute name for storing information whether the VHost is
	 * enabled or disabled.
	 */
	public static final String ENABLED_ATT = "enabled";

	/** Field description */
	public static final String ENABLED_LABEL = "Enabled";

	/**
	 * This is an attribute name for storing the VHost name.
	 */
	public static final String HOSTNAME_ATT = "hostname";

	/** Field description */
	public static final String HOSTNAME_LABEL = "Domain name";

	/**
	 * This is an attribute name for storing the maximum number of users for this
	 * virtual domain.
	 */
	public static final String MAX_USERS_NUMBER_ATT = "max-users";

	/** Field description */
	public static final String MAX_USERS_NUMBER_LABEL = "Max users";

	/** Field description */
	public static final String MESSAGE_FORWARD_ADDRESS_ATT = "mess-forw";

	/** Field description */
	public static final String MESSAGE_FORWARD_ADDRESS_LABEL = "Message forward address";

	/**
	 * Element name for the VHostItem XML child keeping all the extra parameters
	 * for the domain. This is a container for future extensions and parameters
	 * which are not defined yet.
	 */
	public static final String OTHER_PARAMS_ELEM = "other";

	/** Field description */
	public static final String OTHER_PARAMS_LABEL = "Other parameters";

	/** Field description */
	public static final String PRESENCE_FORWARD_ADDRESS_ATT = "pres-forw";

	/** Field description */
	public static final String PRESENCE_FORWARD_ADDRESS_LABEL = "Presence forward address";

	/**
	 * This is an attribute name for storing information whether user registration
	 * is allowed for this domain.
	 */
	public static final String REGISTER_ENABLED_ATT = "register";

	/** Field description */
	public static final String REGISTER_ENABLED_LABEL = "In-band registration";

	/** Field description */
	public static final String S2S_SECRET_ATT = "s2s-secret";

	/** Field description */
	public static final String S2S_SECRET_LABEL = "S2S secret";

	/** Field description */
	public static final String TLS_REQUIRED_ATT = "tls-required";

	/** Field description */
	public static final String TLS_REQUIRED_LABEL = "TLS required";

	/**
	 * Element name to for the VHostItem XML storage.
	 */
	public static final String VHOST_ELEM = "vhost";

	/** Field description */
	protected static final String DOMAIN_FILTER_POLICY_PROP_KEY = "domain-filter-policy";

	/** Field description */
	protected static final String S2S_SECRET_PROP_DEF = null;

	/** Field description */
	protected static final String S2S_SECRET_PROP_KEY = "s2s-secret";

	/** Field description */
	protected static final String VHOST_ANONYMOUS_ENABLED_PROP_KEY =
			"vhost-anonymous-enabled";

	/** Field description */
	protected static final Boolean VHOST_ANONYMOUS_ENABLED_PROP_DEF = Boolean.TRUE;

	/** Field description */
	protected static final String VHOST_MAX_USERS_PROP_KEY = "vhost-max-users";

	/** Field description */
	protected static final Long VHOST_MAX_USERS_PROP_DEF = Long.valueOf(0l);

	/** Field description */
	protected static final String VHOST_MESSAGE_FORWARD_PROP_DEF = null;

	/** Field description */
	protected static final String VHOST_MESSAGE_FORWARD_PROP_KEY =
			"vhost-message-forward-jid";

	/** Field description */
	protected static final String VHOST_PRESENCE_FORWARD_PROP_DEF = null;

	/** Field description */
	protected static final String VHOST_PRESENCE_FORWARD_PROP_KEY =
			"vhost-presence-forward-jid";

	/** Field description */
	protected static final String VHOST_REGISTER_ENABLED_PROP_KEY =
			"vhost-register-enabled";

	/** Field description */
	protected static final Boolean VHOST_REGISTER_ENABLED_PROP_DEF = Boolean.TRUE;

	/** Field description */
	protected static final String VHOST_TLS_REQUIRED_PROP_KEY = "vhost-tls-required";

	/** Field description */
	protected static final Boolean VHOST_TLS_REQUIRED_PROP_DEF = Boolean.FALSE;

	/** Field description */
	protected static final DomainFilterPolicy DOMAIN_FILTER_POLICY_PROP_DEF =
			DomainFilterPolicy.ALL;
	private static final Logger log = Logger.getLogger(VHostItem.class.getName());

	/** Field description */
	protected static final String[] VHOST_OTHER_PARAMS_PATH = { VHOST_ELEM,
			OTHER_PARAMS_ELEM };

	/** Field description */
	protected static final String[] VHOST_COMPONENTS_PATH = { VHOST_ELEM, COMPONENTS_ELEM };

	//~--- fields ---------------------------------------------------------------

	private String[] comps = null;
	private long     maxUsersNumber = Long.getLong(VHOST_MAX_USERS_PROP_KEY,
			VHOST_MAX_USERS_PROP_DEF);
	private JID messageForward = JID.jidInstanceNS(System.getProperty(
			VHOST_MESSAGE_FORWARD_PROP_KEY, VHOST_MESSAGE_FORWARD_PROP_DEF));
	private String otherDomainParams = null;
	private JID    presenceForward = JID.jidInstanceNS(System.getProperty(
			VHOST_PRESENCE_FORWARD_PROP_KEY, VHOST_PRESENCE_FORWARD_PROP_DEF));
	private VHostItem unmodifiableItem = null;
	private JID       vhost            = null;
	private boolean   tlsRequired = DataTypes.getProperty(VHOST_TLS_REQUIRED_PROP_KEY,
			VHOST_TLS_REQUIRED_PROP_DEF);
	private String  s2sSecret = System.getProperty(S2S_SECRET_PROP_KEY,
			S2S_SECRET_PROP_DEF);
	private boolean registerEnabled = DataTypes.getProperty(
			VHOST_REGISTER_ENABLED_PROP_KEY, VHOST_REGISTER_ENABLED_PROP_DEF);
	private boolean            enabled = true;
	private DomainFilterPolicy domainFilter = DomainFilterPolicy.valueof(System.getProperty(
			DOMAIN_FILTER_POLICY_PROP_KEY, DOMAIN_FILTER_POLICY_PROP_DEF.toString()));
	private boolean anonymousEnabled = DataTypes.getProperty(
			VHOST_ANONYMOUS_ENABLED_PROP_KEY, VHOST_ANONYMOUS_ENABLED_PROP_DEF);

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 */
	public VHostItem() {}

	/**
	 * The constructor creates the <code>VHostItem</code> instance from a given
	 * XML element. Please refer to the class documentation for more details of
	 * the XML element.
	 *
	 * @param elem
	 *          is an <code>Element</code> object with virtual domain settings.
	 */
	public VHostItem(Element elem) {
		initFromElement(elem);
	}

	/**
	 * The constructor creates the <code>VHostItem</code> instance for a given
	 * domain name with default values for all other parameters. By the default
	 * all domain parameters are set to true.
	 *
	 * @param vhost
	 *          is a <code>String</code> value with a domain name.
	 */
	public VHostItem(JID vhost) {
		setVHost(vhost);
	}

	/**
	 * The constructor creates the <code>VHostItem</code> instance for a given
	 * domain name with default values for all other parameters. By the default
	 * all domain parameters are set to true.
	 *
	 * @param vhost
	 *          is a <code>String</code> value with a domain name.
	 * @throws TigaseStringprepException
	 *           if the provided string causes stringprep processing errors.
	 */
	public VHostItem(String vhost) throws TigaseStringprepException {
		setVHost(vhost);
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 */
	@Override
	public void addCommandFields(Packet packet) {
		Command.addFieldValue(packet, HOSTNAME_LABEL, (vhost != null)
				? vhost.getDomain()
				: "");
		Command.addCheckBoxField(packet, ENABLED_LABEL, enabled);
		Command.addCheckBoxField(packet, ANONYMOUS_ENABLED_LABEL, anonymousEnabled);
		Command.addCheckBoxField(packet, REGISTER_ENABLED_LABEL, registerEnabled);
		Command.addCheckBoxField(packet, TLS_REQUIRED_LABEL, tlsRequired);
		Command.addFieldValue(packet, S2S_SECRET_LABEL, (s2sSecret != null)
				? s2sSecret
				: "");
		Command.addFieldValue(packet, DOMAIN_FILTER_POLICY_LABEL, domainFilter.toString(),
				DOMAIN_FILTER_POLICY_LABEL, DomainFilterPolicy.valuesStr(), DomainFilterPolicy
				.valuesStr());
		Command.addFieldValue(packet, MAX_USERS_NUMBER_LABEL, "" + maxUsersNumber);
		Command.addFieldValue(packet, PRESENCE_FORWARD_ADDRESS_LABEL, ((presenceForward !=
				null)
				? presenceForward.toString()
				: ""));
		Command.addFieldValue(packet, MESSAGE_FORWARD_ADDRESS_LABEL, ((messageForward != null)
				? messageForward.toString()
				: ""));
		Command.addFieldValue(packet, OTHER_PARAMS_LABEL, (otherDomainParams != null)
				? otherDomainParams
				: "");
		super.addCommandFields(packet);
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 */
	@Override
	public void initFromCommand(Packet packet) {
		super.initFromCommand(packet);

		String tmp = Command.getFieldValue(packet, HOSTNAME_LABEL);

		try {
			setVHost(tmp);
		} catch (TigaseStringprepException ex) {
			throw new IllegalArgumentException("Incorrect domain, unable to parse it: " + tmp,
					ex);
		}
		enabled          = Command.getCheckBoxFieldValue(packet, ENABLED_LABEL);
		anonymousEnabled = Command.getCheckBoxFieldValue(packet, ANONYMOUS_ENABLED_LABEL);
		registerEnabled  = Command.getCheckBoxFieldValue(packet, REGISTER_ENABLED_LABEL);
		tlsRequired      = Command.getCheckBoxFieldValue(packet, TLS_REQUIRED_LABEL);
		tmp              = Command.getFieldValue(packet, S2S_SECRET_LABEL);
		if ((tmp != null) &&!tmp.trim().isEmpty()) {
			s2sSecret = tmp;
		} else {
			s2sSecret = null;
		}
		tmp = Command.getFieldValue(packet, DOMAIN_FILTER_POLICY_LABEL);
		try {
			domainFilter = DomainFilterPolicy.valueof(tmp);
			if (domainFilter == null) {
				domainFilter = DomainFilterPolicy.valueof(System.getProperty(
						DOMAIN_FILTER_POLICY_PROP_KEY, DOMAIN_FILTER_POLICY_PROP_DEF.toString()));
			}
		} catch (Exception ex) {
			domainFilter = DOMAIN_FILTER_POLICY_PROP_DEF;
		}
		try {
			maxUsersNumber = Long.parseLong(Command.getFieldValue(packet,
					MAX_USERS_NUMBER_LABEL));
		} catch (Exception e) {
			log.log(Level.WARNING, "Can not parse max users number: {0}", Command.getFieldValue(
					packet, MAX_USERS_NUMBER_LABEL));
		}
		tmp = Command.getFieldValue(packet, PRESENCE_FORWARD_ADDRESS_LABEL);
		if ((tmp != null) &&!tmp.trim().isEmpty()) {
			try {
				presenceForward = JID.jidInstance(tmp);
			} catch (TigaseStringprepException ex) {
				presenceForward = null;

				throw new IllegalArgumentException("Incorrect presence forward address: " + tmp,
						ex);
			}
		}
		tmp = Command.getFieldValue(packet, MESSAGE_FORWARD_ADDRESS_LABEL);
		if ((tmp != null) &&!tmp.trim().isEmpty()) {
			try {
				messageForward = JID.jidInstance(tmp);
			} catch (TigaseStringprepException ex) {
				messageForward = null;

				throw new IllegalArgumentException("Incorrect message forward address: " + tmp,
						ex);
			}
		}
		otherDomainParams = Command.getFieldValue(packet, OTHER_PARAMS_LABEL);
	}

	/**
	 * Method description
	 *
	 *
	 * @param elem
	 */
	@Override
	public void initFromElement(Element elem) {
		if (elem.getName() != VHOST_ELEM) {
			throw new IllegalArgumentException("Incorrect element name, expected: " +
					VHOST_ELEM);
		}
		super.initFromElement(elem);
		setVHost(JID.jidInstanceNS(elem.getAttributeStaticStr(HOSTNAME_ATT)));
		enabled = Boolean.parseBoolean(elem.getAttributeStaticStr(ENABLED_ATT));
		anonymousEnabled = Boolean.parseBoolean(elem.getAttributeStaticStr(
				ANONYMOUS_ENABLED_ATT));
		registerEnabled = Boolean.parseBoolean(elem.getAttributeStaticStr(
				REGISTER_ENABLED_ATT));
		tlsRequired = Boolean.parseBoolean(elem.getAttributeStaticStr(TLS_REQUIRED_ATT));
		s2sSecret   = elem.getAttributeStaticStr(S2S_SECRET_ATT);
		try {
			domainFilter = DomainFilterPolicy.valueof(elem.getAttributeStaticStr(
					DOMAIN_FILTER_POLICY_ATT));
			if (domainFilter == null) {
				domainFilter = DomainFilterPolicy.valueof(System.getProperty(
						DOMAIN_FILTER_POLICY_PROP_KEY, DOMAIN_FILTER_POLICY_PROP_DEF.toString()));
			}
		} catch (Exception e) {
			domainFilter = DOMAIN_FILTER_POLICY_PROP_DEF;
		}
		try {
			maxUsersNumber = Long.parseLong(elem.getAttributeStaticStr(MAX_USERS_NUMBER_ATT));
		} catch (Exception e) {
			log.log(Level.WARNING, "Can not parse max users number: {0}", elem
					.getAttributeStaticStr(MAX_USERS_NUMBER_ATT));
			maxUsersNumber = Long.getLong(VHOST_MAX_USERS_PROP_KEY, VHOST_MAX_USERS_PROP_DEF);
		}

		String tmp = elem.getAttributeStaticStr(PRESENCE_FORWARD_ADDRESS_ATT);

		if ((tmp != null) &&!tmp.trim().isEmpty()) {
			presenceForward = JID.jidInstanceNS(tmp);
		}
		tmp = elem.getAttributeStaticStr(MESSAGE_FORWARD_ADDRESS_ATT);
		if ((tmp != null) &&!tmp.trim().isEmpty()) {
			messageForward = JID.jidInstanceNS(tmp);
		}

		String comps_str = elem.getCDataStaticStr(VHOST_COMPONENTS_PATH);

		if ((comps_str != null) &&!comps_str.isEmpty()) {
			comps = comps_str.split(",");
		}
		otherDomainParams = elem.getCDataStaticStr(VHOST_OTHER_PARAMS_PATH);
	}

	/**
	 * Method description
	 *
	 *
	 * @param propString
	 */
	@Override
	public void initFromPropertyString(String propString) {
		String[] props = propString.split(":");

		try {
			setVHost(props[0]);
		} catch (TigaseStringprepException ex) {
			throw new IllegalArgumentException("Domain misconfiguration, cannot parse it: " +
					props[0], ex);
		}
		for (String tmp : props) {
			boolean val = true;

			if (tmp.startsWith("-")) {
				val = false;
			}
			if (tmp.endsWith(ANONYMOUS_ENABLED_ATT)) {
				anonymousEnabled = val;
			}
			if (tmp.endsWith(REGISTER_ENABLED_ATT)) {
				registerEnabled = val;
			}
			if (tmp.endsWith(TLS_REQUIRED_ATT)) {
				tlsRequired = val;
			}
			if (tmp.startsWith(S2S_SECRET_ATT)) {
				String[] s2 = tmp.split("=");

				s2sSecret = s2[1];
			}
			if (tmp.startsWith(DOMAIN_FILTER_POLICY_ATT)) {
				String[] df = tmp.split("=");

				try {
					domainFilter = DomainFilterPolicy.valueof(df[1]);
					if ( domainFilter == null ){
						domainFilter = DomainFilterPolicy.valueof( System.getProperty(
								DOMAIN_FILTER_POLICY_PROP_KEY, DOMAIN_FILTER_POLICY_PROP_DEF.toString() ) );
					}
				} catch (Exception e) {
					domainFilter = DOMAIN_FILTER_POLICY_PROP_DEF;
				}
			}
			if (tmp.startsWith(MAX_USERS_NUMBER_ATT)) {
				String[] mu = tmp.split("=");

				try {
					maxUsersNumber = Long.parseLong(mu[1]);
				} catch (NumberFormatException ex) {
					maxUsersNumber = 0;
					log.log(Level.WARNING,
							"Incorrect max users number for vhost settings, number parsing error: {0}",
							tmp);
				}
			}
			if (tmp.startsWith(PRESENCE_FORWARD_ADDRESS_ATT)) {
				String[] mu = tmp.split("=");

				try {
					presenceForward = JID.jidInstance(mu[1]);
				} catch (TigaseStringprepException ex) {
					presenceForward = null;
					log.log(Level.WARNING,
							"Incorrect presence forwarding address, address parsing error: {0}", tmp);
				}
			}
			if (tmp.startsWith(MESSAGE_FORWARD_ADDRESS_ATT)) {
				String[] mu = tmp.split("=");

				try {
					messageForward = JID.jidInstance(mu[1]);
				} catch (TigaseStringprepException ex) {
					messageForward = null;
					log.log(Level.WARNING,
							"Incorrect presence forwarding address, address parsing error: {0}", tmp);
				}
			}
		}
	}

	/**
	 * The method exports the <code>VHostItem</code> object to XML representation.
	 *
	 * @return an <code>Element</code> object with vhost information.
	 */
	@Override
	public Element toElement() {
		Element elem      = super.toElement();
		String  comps_str = "";

		if ((comps != null) && (comps.length > 0)) {
			for (String comp : comps) {
				if (!comps_str.isEmpty()) {
					comps_str += ",";
				}
				comps_str += comp;
			}
		}

		String other_params = (otherDomainParams != null)
				? otherDomainParams
				: "";

		elem.addChild(new Element(COMPONENTS_ELEM, comps_str));
		elem.addChild(new Element(OTHER_PARAMS_ELEM, other_params));
		elem.addAttribute(HOSTNAME_ATT, vhost.getDomain());
		elem.addAttribute(ENABLED_ATT, "" + enabled);
		elem.addAttribute(ANONYMOUS_ENABLED_ATT, "" + anonymousEnabled);
		elem.addAttribute(REGISTER_ENABLED_ATT, "" + registerEnabled);
		elem.addAttribute(TLS_REQUIRED_ATT, "" + tlsRequired);
		if (s2sSecret != null) {
			elem.addAttribute(S2S_SECRET_ATT, s2sSecret);
		}
		if (domainFilter != null) {
			elem.addAttribute(DOMAIN_FILTER_POLICY_ATT, domainFilter.toString());
		}
		elem.addAttribute(MAX_USERS_NUMBER_ATT, "" + maxUsersNumber);
		if (presenceForward != null) {
			elem.addAttribute(PRESENCE_FORWARD_ADDRESS_ATT, presenceForward.toString());
		}
		if (messageForward != null) {
			elem.addAttribute(MESSAGE_FORWARD_ADDRESS_ATT, messageForward.toString());
		}

		return elem;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String toPropertyString() {
		StringBuilder sb = new StringBuilder();

		sb.append(vhost.toString());
		if (!anonymousEnabled) {
			sb.append(":-").append(ANONYMOUS_ENABLED_ATT);
		}
		if (!registerEnabled) {
			sb.append(":-").append(REGISTER_ENABLED_ATT);
		}
		if (!tlsRequired) {
			sb.append(":-").append(TLS_REQUIRED_ATT);
		}
		if (s2sSecret != null) {
			sb.append(':').append(S2S_SECRET_ATT).append('=').append(s2sSecret);
		}
		sb.append(':').append(DOMAIN_FILTER_POLICY_ATT).append('=').append(domainFilter
				.toString());
		if (maxUsersNumber > 0) {
			sb.append(':').append(MAX_USERS_NUMBER_ATT).append('=').append(maxUsersNumber);
		}
		if (presenceForward != null) {
			sb.append(':').append(PRESENCE_FORWARD_ADDRESS_ATT).append('=').append(
					presenceForward.toString());
		}
		if (messageForward != null) {
			sb.append(':').append(MESSAGE_FORWARD_ADDRESS_ATT).append('=').append(messageForward
					.toString());
		}

		return sb.toString();
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String toString() {
		return "Domain: " + vhost + ", enabled: " + enabled + ", anonym: " +
				anonymousEnabled + ", register: " + registerEnabled + ", maxusers: " +
				maxUsersNumber + ", tls: " + tlsRequired + ", s2sSecret: " + s2sSecret +
				", domainFilter: " + domainFilter;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Returns an array with the server components names which should process
	 * packets sent to this domain or <code>null</code> (default) if there is no
	 * specific component assigned to this domain.
	 *
	 * @return a <code>String[]</code> object with server component names.
	 */
	public String[] getComps() {
		return comps;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public DomainFilterPolicy getDomainFilter() {
		if ( domainFilter == null ){
			domainFilter = DomainFilterPolicy.valueof( System.getProperty(
					DOMAIN_FILTER_POLICY_PROP_KEY, DOMAIN_FILTER_POLICY_PROP_DEF.toString() ) );
		}
		return domainFilter;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getElemName() {
		return VHOST_ELEM;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getKey() {
		return this.vhost.getDomain();
	}

	/**
	 * This method returns the maximum number of user accounts allowed for this
	 * domain. This parameter is to allow for limiting number of users on per
	 * domain basis.
	 *
	 * @return a <code>long</code> value indicating the maximum number of user
	 *         accounts allowed for this domain.
	 */
	public long getMaxUsersNumber() {
		return maxUsersNumber;
	}

	/**
	 * @return the messageForward
	 */
	public JID getMessageForward() {
		return messageForward;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public JID getMessageForwardAddress() {
		return presenceForward;
	}

	/**
	 * This method allows to access the virtual domain other configuration
	 * parameters. This is future feature API and it is not used right now. It
	 * allows to access configuration parameters which are not specified at the
	 * time of API definition.
	 *
	 * @return a <code>String</code> value with domain extra parameters.
	 */
	public String getOtherDomainParams() {
		return otherDomainParams;
	}

	/**
	 * @return the presenceForward
	 */
	public JID getPresenceForward() {
		return presenceForward;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public JID getPresenceForwardAddress() {
		return presenceForward;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public String getS2sSecret() {
		return s2sSecret;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public VHostItem getUnmodifiableVHostItem() {
		if (unmodifiableItem == null) {
			unmodifiableItem = new UnmodifiableVHostItem();
		}

		return unmodifiableItem;
	}

	/**
	 * This method return a virtual host name as a <code>String</code> value.
	 *
	 * @return a <code>String</code> value with the virtual domain name.
	 */
	public JID getVhost() {
		return this.vhost;
	}

	/**
	 * This method checks whether anonymous login is enabled for this domain. This
	 * is the domain own configuration parameter which allows to disable anonymous
	 * logins on per domain basis.
	 *
	 * @return a <code>boolean</code> value indicating whether anonymous logins
	 *         are allowed for this domain.
	 */
	public boolean isAnonymousEnabled() {
		return anonymousEnabled;
	}

	/**
	 * Checks whether this domain is set as enabled or not. This is domain own
	 * configuration parameter which allows to temporarly disable domain so
	 * packets for this domain are not processed normally. Instead the server
	 * returns an error.
	 *
	 * @return a <code>boolean</code> value <code>true</code> if the domain is
	 *         enabled and <code>false</code> if the domain is disabled.
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * The method checks whether user registration is enabled for this domain or
	 * not. This is the domain own configuration parameter which allows to disable
	 * user accounts registration via XMPP per domain basis.
	 *
	 * @return a <code>boolean</code> value indicating whether user account
	 *         registration is allowed for this domain.
	 */
	public boolean isRegisterEnabled() {
		return registerEnabled;
	}

	/**
	 * The method returns TLS required settings for the vhost.
	 *
	 *
	 * @return a <code>boolean</code> value whether TLS is required for the vhost or not.
	 */
	public boolean isTlsRequired() {
		return tlsRequired;
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * This method allows to enable or disable anonymous logins for this domain.
	 * By default anonymous logins are enabled.
	 *
	 *
	 * @param value
	 */
	public void setAnonymousEnabled(boolean value) {
		this.anonymousEnabled = value;
	}

	/**
	 * Sets an array with the server component names by which packets to this
	 * domain can be processed. Every local domain will be handled by
	 * <code>VHostListener</code> which returns <code>true</code> for
	 * <code>handlesLocalDomains()</code> method call and by all components set
	 * via this method.
	 *
	 * @param comps
	 *          is an <code>String[]</code> array with server component names.
	 */
	public void setComps(String[] comps) {
		this.comps = comps;
	}

	/**
	 * Method description
	 *
	 *
	 * @param domainFilter
	 */
	public void setDomainFilter(DomainFilterPolicy domainFilter) {
		this.domainFilter = domainFilter;
	}

	/**
	 * This method allows to enable or disable local domain. If the domain is
	 * disabled packets sent for this domain are not processed normally, instead
	 * the server returns an error to the sender. Domain is enabled by default.
	 *
	 *
	 * @param value
	 */
	public void setEnabled(boolean value) {
		this.enabled = value;
	}

	/**
	 * This method allows to set the maximum number of user accounts allowed for
	 * this domain. The default value of this parameter is: <code>0L</code>.
	 *
	 * @param maxUsersNumber
	 *          is a <code>long</code> value specifying the maximum number of user
	 *          accounts allowed for this domain.
	 */
	public void setMaxUsersNumber(long maxUsersNumber) {
		this.maxUsersNumber = maxUsersNumber;
	}

	/**
	 * @param messageForward the messageForward to set
	 */
	public void setMessageForward(JID messageForward) {
		this.messageForward = messageForward;
	}

	/**
	 * This method allows to set extra configuration parameters for the virtual
	 * domain. This is future feature API and it is not used right now. It allows
	 * to access configuration parameters which are not specified at the time of
	 * API definition.
	 *
	 * @param otherParams
	 *          is a <code>String</code> value with domain extra parameters.
	 */
	public void setOtherDomainParams(String otherParams) {
		this.otherDomainParams = otherParams;
	}

	/**
	 * @param presenceForward the presenceForward to set
	 */
	public void setPresenceForward(JID presenceForward) {
		this.presenceForward = presenceForward;
	}

	/**
	 * This method allows to enable or disable user account registration for this
	 * domain. By default user account registration is enabled.
	 *
	 *
	 * @param value
	 */
	public void setRegisterEnabled(boolean value) {
		this.registerEnabled = value;
	}

	/**
	 * Method description
	 *
	 *
	 * @param s2sSecret
	 */
	public void setS2sSecret(String s2sSecret) {
		this.s2sSecret = s2sSecret;
	}

	/**
	 * The method sets TLS required property for the vhost. By default TLS is not required.
	 *
	 *
	 * @param value is a <code>boolean</code> parameter specifying whether TLS is required
	 * for the virtual domain.
	 */
	public void setTlsRequired(boolean value) {
		this.tlsRequired = value;
	}

	/**
	 * Method description
	 *
	 *
	 * @param vhost
	 *
	 * @throws TigaseStringprepException
	 */
	public void setVHost(String vhost) throws TigaseStringprepException {
		if (vhost == null) {
			vhost = "";
		}
		this.vhost = JID.jidInstance(vhost);
	}

	/**
	 * Method description
	 *
	 *
	 * @param vhost
	 */
	public void setVHost(JID vhost) {
		this.vhost = vhost;
	}

	//~--- inner classes --------------------------------------------------------

	private class UnmodifiableVHostItem
					extends VHostItem {
		/**
		 * Method description
		 *
		 *
		 * @param elem
		 */
		@Override
		public void initFromElement(Element elem) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		/**
		 * Method description
		 *
		 *
		 * @param propString
		 */
		@Override
		public void initFromPropertyString(String propString) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		/**
		 * The method exports the <code>VHostItem</code> object to XML
		 * representation.
		 *
		 * @return an <code>Element</code> object with vhost information.
		 */
		@Override
		public Element toElement() {
			return VHostItem.this.toElement();
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String toString() {
			return VHostItem.this.toString();
		}

		//~--- get methods --------------------------------------------------------

		/**
		 * Returns an array with the server components names which should process
		 * packets sent to this domain or <code>null</code> (default) if there is no
		 * specific component assigned to this domain.
		 *
		 * @return a <code>String[]</code> object with server component names.
		 */
		@Override
		public String[] getComps() {
			return VHostItem.this.getComps();
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public DomainFilterPolicy getDomainFilter() {
			return VHostItem.this.getDomainFilter();
		}

		/**
		 * This method returns the maximum number of user accounts allowed for this
		 * domain. This parameter is to allow for limiting number of users on per
		 * domain basis.
		 *
		 * @return a <code>long</code> value indicating the maximum number of user
		 *         accounts allowed for this domain.
		 */
		@Override
		public long getMaxUsersNumber() {
			return VHostItem.this.getMaxUsersNumber();
		}

		/**
		 * @return the messageForward
		 */
		@Override
		public JID getMessageForward() {
			return VHostItem.this.messageForward;
		}

		/**
		 * This method allows to access the virtual domain other configuration
		 * parameters. This is future feature API and it is not used right now. It
		 * allows to access configuration parameters which are not specified at the
		 * time of API definition.
		 *
		 * @return a <code>String</code> value with domain extra parameters.
		 */
		@Override
		public String getOtherDomainParams() {
			return VHostItem.this.getOtherDomainParams();
		}

		/**
		 * @return the presenceForward
		 */
		@Override
		public JID getPresenceForward() {
			return VHostItem.this.presenceForward;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String getS2sSecret() {
			return VHostItem.this.getS2sSecret();
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public VHostItem getUnmodifiableVHostItem() {
			return this;
		}

		/**
		 * This method return a virtual host name as a <code>String</code> value.
		 *
		 * @return a <code>String</code> value with the virtual domain name.
		 */
		@Override
		public JID getVhost() {
			return VHostItem.this.getVhost();
		}

		/**
		 * This method checks whether anonymous login is enabled for this domain.
		 * This is the domain own configuration parameter which allows to disable
		 * anonymous logins on per domain basis.
		 *
		 * @return a <code>boolean</code> value indicating whether anonymous logins
		 *         are allowed for this domain.
		 */
		@Override
		public boolean isAnonymousEnabled() {
			return VHostItem.this.isAnonymousEnabled();
		}

		/**
		 * Checks whether this domain is set as enabled or not. This is domain own
		 * configuration parameter which allows to temporarly disable domain so
		 * packets for this domain are not processed normally. Instead the server
		 * returns an error.
		 *
		 * @return a <code>boolean</code> value <code>true</code> if the domain is
		 *         enabled and <code>false</code> if the domain is disabled.
		 */
		@Override
		public boolean isEnabled() {
			return VHostItem.this.isEnabled();
		}

		/**
		 * The method checks whether user registration is enabled for this domain or
		 * not. This is the domain own configuration parameter which allows to
		 * disable user accounts registration via XMPP per domain basis.
		 *
		 * @return a <code>boolean</code> value indicating whether user account
		 *         registration is allowed for this domain.
		 */
		@Override
		public boolean isRegisterEnabled() {
			return VHostItem.this.isRegisterEnabled();
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public boolean isTlsRequired() {
			return VHostItem.this.isTlsRequired();
		}

		//~--- set methods --------------------------------------------------------

		/**
		 * This method allows to enable or disable anonymous logins for this domain.
		 * By default anonymous logins are enabled.
		 *
		 * @param enabled
		 *          is a <code>boolean</code> value indicating whether anonymous
		 *          logins are allowed for this domain.
		 */
		@Override
		public void setAnonymousEnabled(boolean enabled) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		/**
		 * Sets an array with the server component names by which packets to this
		 * domain can be processed. Every local domain will be handled by
		 * <code>VHostListener</code> which returns <code>true</code> for
		 * <code>handlesLocalDomains()</code> method call and by all components set
		 * via this method.
		 *
		 * @param comps
		 *          is an <code>String[]</code> array with server component names.
		 */
		@Override
		public void setComps(String[] comps) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		/**
		 * Method description
		 *
		 *
		 * @param filter
		 */
		@Override
		public void setDomainFilter(DomainFilterPolicy filter) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		/**
		 * This method allows to enable or disable local domain. If the domain is
		 * disabled packets sent for this domain are not processed normally, instead
		 * the server returns an error to the sender. Domain is enabled by default.
		 *
		 * @param enabled
		 *          is a <code>boolean</code> value indicating whether the domain is
		 *          enabled or not.
		 */
		@Override
		public void setEnabled(boolean enabled) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		/**
		 * This method allows to set the maximum number of user accounts allowed for
		 * this domain. The default value of this parameter is: <code>0L</code>.
		 *
		 * @param maxUsersNumber
		 *          is a <code>long</code> value specifying the maximum number of
		 *          user accounts allowed for this domain.
		 */
		@Override
		public void setMaxUsersNumber(long maxUsersNumber) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		/**
		 * @param messageForward the messageForward to set
		 */
		@Override
		public void setMessageForward(JID messageForward) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		/**
		 * This method allows to set extra configuration parameters for the virtual
		 * domain. This is future feature API and it is not used right now. It
		 * allows to access configuration parameters which are not specified at the
		 * time of API definition.
		 *
		 * @param otherParams
		 *          is a <code>String</code> value with domain extra parameters.
		 */
		@Override
		public void setOtherDomainParams(String otherParams) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		/**
		 * @param presenceForward the presenceForward to set
		 */
		@Override
		public void setPresenceForward(JID presenceForward) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		/**
		 * This method allows to enable or disable user account registration for
		 * this domain. By default user account registration is enabled.
		 *
		 * @param enabled
		 *          is a <code>boolean</code> value indicating whether user account
		 *          registration is allowed for this domain or not.
		 */
		@Override
		public void setRegisterEnabled(boolean enabled) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		/**
		 * Method description
		 *
		 *
		 * @param s2sSecret
		 */
		@Override
		public void setS2sSecret(String s2sSecret) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		/**
		 * Method description
		 *
		 *
		 * @param enabled
		 */
		@Override
		public void setTlsRequired(boolean enabled) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}
	}
}


//~ Formatted in Tigase Code Convention on 13/04/05
