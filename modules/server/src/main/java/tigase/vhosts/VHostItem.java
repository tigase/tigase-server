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

import java.lang.reflect.Array;
import tigase.vhosts.filter.DomainFilterPolicy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.db.comp.RepositoryItemAbstract;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.XMPPServer;
import tigase.server.xmppclient.ClientTrustManagerFactory;
import tigase.util.DataTypes;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.JID;
import tigase.util.StringUtilities;
import tigase.xmpp.BareJID;

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
 * {@code
 * <vhost hostname="vhost.something.com"
 *        enabled="true"
 *        anon="true"
 *        register="true"
 *        max-users="99999999999L">
 *   <comps/>
 *   <other/>
 * }
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
				extends RepositoryItemAbstract
				implements Comparable<VHostItem> {
	
	public static class DataType { 
		private final String name;
		private final String key;
		private final Class cls;
		private final Class collectionCls;
		private final Object defValue;
		private final Object[] options;
		private final String[] optionsNames;
		
		public DataType(String key, String name, Class cls, Class<? extends Collection> collectionCls, Object defValue, Object[] options, String[] optionsNames) {
			this.key = key;
			this.name = name;
			this.cls = cls;
			this.collectionCls = collectionCls;
			this.defValue = defValue;
			this.options = options;
			this.optionsNames = optionsNames;
			
			if (defValue != null && !cls.isAssignableFrom(defValue.getClass())) {
				throw new IllegalArgumentException("default value paratemeter must of class " + cls.getCanonicalName());
			}
			
			if (options != null) {
				for (Object option : options) {
					if (option != null && !cls.isAssignableFrom(option.getClass())) {
						throw new IllegalArgumentException("option values must of class " + cls.getCanonicalName());
					}
				}
				if (optionsNames != null && options.length != optionsNames.length) {
					throw new IllegalArgumentException("if passed options name must be specified for each option");
				}
			}
		}
		
		public DataType(String key, String name, Class cls, Class<? extends Collection> collectionCls, Object defValue, Object[] options) {
			this(key, name, cls, collectionCls, defValue, options, null);
		}
		
		public DataType(String key, String name, Class cls, Class<? extends Collection> collectionCls, Object defValue) {
			this(key, name, cls, collectionCls, defValue, null, null);
		}
		
		public DataType(String key, String name, Class cls, Object defValue, Object[] options) {
			this(key, name, cls, null, defValue, options, null);
		}
		
		public DataType(String key, String name, Class cls, Object defValue) {
			this(key, name, cls, null, defValue, null, null);
		}
		
		public String getName() {
			return name;
		}

		public String getKey() {
			return key;
		}

		public Class getCls() {
			return cls;
		}
		
		public Class<? extends Collection> getCollectionCls() {
			return collectionCls;
		}
		
		public <T> T getDefValue() {
			return (T) defValue;
		}
		
		public <T> T[] getOptions() {
			return (T[]) options;
		}
		
		public String[] getOptionsNames() {
			return optionsNames;
		}
	}
	
	/**
	 * This is an attribute name for storing information whether anonymous user
	 * can login for this domain.
	 */
	public static final String ANONYMOUS_ENABLED_ATT = "anon";

	/** Field description */
	public static final String ANONYMOUS_ENABLED_LABEL = "Anonymous enabled";

	/**
	 * List of SASL mechanisms allowed for domain
	 */
	public static final String SASL_MECHANISM_ATT = "sasl-mechanisms";

	public static final String SASL_MECHANISM_LABEL = "Allowed SASL mechanisms";

	/** Field description */
	public static final String COMPONENTS_ATT = "comps";

	/**
	 * Element name for the VHostItem XML child keeping list of the server
	 * component which can handle packets for this domain. In most cases this
	 * element should be empty.
	 */
	public static final String COMPONENTS_ELEM = "comps";

	/**
	 * This is an attribute name for storing information on which ports VHost
	 * should be enabled.
	 */
	public static final String C2S_PORTS_ALLOWED_ATT = "c2s-ports-allowed";

	/** Field description */
	public static final String C2S_PORTS_ALLOWED_LABEL = "Allowed C2S,BOSH,WebSocket ports";

	/** Field description */
	public static final String DOMAIN_FILTER_POLICY_ATT = "domain-filter";

	/** Field description */
	public static final String DOMAIN_FILTER_POLICY_DOMAINS_ATT = "domain-filter-domains";

	/** Field description */
	public static final String DOMAIN_FILTER_POLICY_LABEL = "Domain filter policy";

	/** Field description */
	public static final String DOMAIN_FILTER_POLICY_DOMAINS_LABEL = "Domain filter domains (only LIST and BLACKLIST)";

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

	/** Field description */
	public static final String TRUSTED_JIDS_ATT = "trusted-jids";
	
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

	protected static final Map<String,DataType> dataTypes = new ConcurrentHashMap<String,DataType>();
	
	private static ConcurrentSkipListSet<String> GLOBAL_TRUSTED_JIDS = null;
	
	public static void registerData(List<DataType> types) {
		for (DataType type : types) {
			dataTypes.put(type.getKey(), type);
		}
	}
	
	protected static void initGlobalTrustedJids() {
		String trustedJidsStr = System.getProperty("trusted");
		if (trustedJidsStr == null || trustedJidsStr.isEmpty())
			GLOBAL_TRUSTED_JIDS = null;
		else {
			ConcurrentSkipListSet<String> trusted = new ConcurrentSkipListSet<>();
			for (String trustedStr : trustedJidsStr.split(",")) {
				if (!trustedStr.contains("{"))
					trusted.add(trustedStr);
			}
			if (trusted.isEmpty())
				GLOBAL_TRUSTED_JIDS = null;
			else
				GLOBAL_TRUSTED_JIDS = trusted;
		}
	}
	
	static {
		List<DataType> types = new ArrayList<VHostItem.DataType>();
		types.add(new DataType(ClientTrustManagerFactory.CA_CERT_PATH, "Client Certificate CA", String.class, null));
		types.add(new DataType(ClientTrustManagerFactory.CERT_REQUIRED_KEY, "Client Certificate Required", Boolean.class,
				Boolean.FALSE));
		types.add(new DataType(TRUSTED_JIDS_ATT, "Trusted JIDs", String[].class, 
				ConcurrentSkipListSet.class, null, null));
		VHostItem.registerData(types);
		
		initGlobalTrustedJids();
	}

	
	//~--- fields ---------------------------------------------------------------

	private String[] comps = null;
	private int[] c2sPortsAllowed = null;
	private String[] saslAllowedMechanisms = null;
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
	private String[] domainFilterDomains = null;
	private boolean anonymousEnabled = DataTypes.getProperty(
			VHOST_ANONYMOUS_ENABLED_PROP_KEY, VHOST_ANONYMOUS_ENABLED_PROP_DEF);
	private Map<String,Object> data = new ConcurrentHashMap<String,Object>();
	
	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 */
	public VHostItem() {
		// added to ensure that we have s2sSecret set, as without it S2S connections
		// will always fail (needed mostly for newly added vhosts).
		if (s2sSecret == null) {
			s2sSecret = UUID.randomUUID().toString();
		}
		if (GLOBAL_TRUSTED_JIDS != null) {
			data.put(TRUSTED_JIDS_ATT, GLOBAL_TRUSTED_JIDS);
		}
	}

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
		Command.addFieldValue(packet, DOMAIN_FILTER_POLICY_DOMAINS_LABEL,
							domainFilterDomains != null ? stringArrayToString( domainFilterDomains, ";") : "");
		Command.addFieldValue(packet, MAX_USERS_NUMBER_LABEL, "" + maxUsersNumber);
		String c2sPortsAllowedStr = intArrayToString(c2sPortsAllowed,",");
		Command.addFieldValue(packet, C2S_PORTS_ALLOWED_LABEL,
				c2sPortsAllowedStr != null ? c2sPortsAllowedStr : "");
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
		Command.addFieldValue(packet, SASL_MECHANISM_LABEL,
				saslAllowedMechanisms != null ? stringArrayToString(saslAllowedMechanisms, ",") : "");

		super.addCommandFields(packet);
		
		for (DataType type : dataTypes.values()) {
			if (type.cls != Boolean.class) {
				Object[] options = type.getOptions();
				Object val = getData(type.getKey());
				if (val instanceof Collection) {
					Collection collection = (Collection) val;
					val = Array.newInstance(type.getCls().getComponentType(), collection.size());
					int i=0;
					for (Object v : collection) {
						Array.set(val, i, v);
						i++;
					}
				}
				String valueStr = val != null ? DataTypes.valueToString(val) : "";
				if (options == null || options.length == 0) {
					Command.addFieldValue(packet, type.getName(), valueStr);
				} else {
					String[] optionsStr = new String[options.length];
					for (int i=0; i<options.length; i++) {
						optionsStr[i] = (options[i] != null) ? DataTypes.valueToString(options[i]) : "";
					}
					String[] optionsNames = type.getOptionsNames();
					if (optionsNames == null) 
						optionsNames = optionsStr;
					Command.addFieldValue(packet, type.getName(), valueStr, type.getName(), optionsNames, optionsStr);
				}
			} else {
				boolean val = isData(type.getKey());
				Command.addCheckBoxField(packet, type.getName(), val);
			}
		}
	}

	@Override
	public int compareTo( VHostItem o ) {
		return vhost.compareTo( o.vhost );
	}

	@Override
	public boolean equals( Object v ) {
		boolean result = false;
		if ( v instanceof VHostItem ){

			result = vhost.equals( ( (VHostItem) v ).vhost );
		}
		return result;
	}

	@Override
	public int hashCode() {
		return vhost.hashCode();
	}

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
			} else if (domainFilter == DomainFilterPolicy.LIST || domainFilter == DomainFilterPolicy.BLACKLIST
					|| domainFilter == DomainFilterPolicy.CUSTOM) {
				tmp = Command.getFieldValue(packet, DOMAIN_FILTER_POLICY_DOMAINS_LABEL);
				if ( tmp != null && !tmp.trim().isEmpty() ){
					domainFilterDomains = StringUtilities.stringToArrayOfString( tmp, ";" );
				}
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
		tmp = Command.getFieldValue(packet, C2S_PORTS_ALLOWED_LABEL);
		c2sPortsAllowed = parseIntArray(tmp, ",");

		tmp = Command.getFieldValue(packet, SASL_MECHANISM_LABEL);
		if ((tmp != null) && !tmp.trim().isEmpty()) {
			setSaslAllowedMechanisms(tmp.split(","));
		}

		for (DataType type : dataTypes.values()) {
			String valueStr = Command.getFieldValue(packet, type.getName());
			char typeId = DataTypes.typesMap.get(type.cls.getName());
			Object value = (valueStr == null || valueStr.isEmpty()) ? null :DataTypes.decodeValueType(typeId, valueStr);
			if (value != null && type.getCollectionCls() != null) {
				try {
					Collection collection = type.getCollectionCls().newInstance();
					for (int i = 0; i < Array.getLength(value); i++) {
						collection.add(Array.get(value, i));
					}
					value = collection;
				} catch (InstantiationException | IllegalAccessException ex) {
					throw new IllegalArgumentException("Could not instantiate collection of class: " 
							+ type.getCollectionCls().getCanonicalName(), ex);
				}
			}
			setData(type.getKey(), value);
		}

		log.log( Level.FINE, "Initialized from command: {0}", this);

	}

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
			} else if (domainFilter == DomainFilterPolicy.LIST || domainFilter == DomainFilterPolicy.BLACKLIST
					|| domainFilter == DomainFilterPolicy.CUSTOM) {
				String tmp = elem.getAttributeStaticStr(DOMAIN_FILTER_POLICY_DOMAINS_ATT);
				if ( tmp != null && !tmp.trim().isEmpty() ){
					domainFilterDomains = StringUtilities.stringToArrayOfString( tmp, ";" );
				}
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

		this.c2sPortsAllowed = parseIntArray(elem.getAttributeStaticStr(C2S_PORTS_ALLOWED_ATT), ",");

		tmp = elem.getAttributeStaticStr(SASL_MECHANISM_ATT);
		if (tmp != null) {
			setSaslAllowedMechanisms(tmp.split(";"));
		}
		
		Element data = elem.getChild("data");
		if (data != null) {
			List<Element> items = data.getChildren();
			if (items != null) {
				for (Element item : items) {
					DataType type = dataTypes.get(item.getName());
					char typeChar = type != null ? DataTypes.typesMap.get(type.getCls().getName()) : item.getAttributeStaticStr("type").charAt(0);
					Object value = DataTypes.decodeValueType(typeChar, item.getCData());
					if (type != null && type.getCollectionCls() != null && value != null) {
						try {
							Collection collection = type.getCollectionCls().newInstance();
							for (int i = 0; i < Array.getLength(value); i++) {
								collection.add(Array.get(value, i));
							}
							value = collection;
						} catch (InstantiationException | IllegalAccessException ex) {
							throw new IllegalArgumentException("Could not instantiate collection of class: "
									+ type.getCollectionCls().getCanonicalName(), ex);
						}					
					}
					setData(item.getName(), value);
				}
			}
		}
		log.log( Level.FINE, "Initialized from element: {0}", this);
	}

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
			if (tmp.startsWith(COMPONENTS_ATT)) {
				String[] c         = tmp.split("=");
				String   comps_str = c[1];

				if (!comps_str.isEmpty()) {
					comps = comps_str.split(";");
				}
			}
			if (tmp.startsWith(DOMAIN_FILTER_POLICY_ATT)) {
				String[] df = tmp.split("=");
				String[] domains;

				try {
					if ( df.length == 2 ){
						domainFilter = DomainFilterPolicy.valueof( df[1] );
					} else if ( df.length == 3 ){
						domainFilter = DomainFilterPolicy.valueof( df[1] );
						if ( df[2] != null && !df[2].trim().isEmpty() ){
							domainFilterDomains = StringUtilities.stringToArrayOfString( df[2], ";" );
						}
					}
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
			if (tmp.startsWith(C2S_PORTS_ALLOWED_ATT)) {
				String[] mu = tmp.split("=");

				c2sPortsAllowed = parseIntArray(mu[1], ";");
			}
			if(tmp.startsWith(SASL_MECHANISM_ATT)){
				String[] mu = tmp.split("=");
				setSaslAllowedMechanisms(mu[1].split(";"));
			}

			String[] mu = tmp.split("=");
			if (mu != null && mu.length == 2 && dataTypes.containsKey(mu[0])) {
				parseDataValue(mu[0], mu[1]);
			}

		}
		log.log( Level.FINE, "Initialized from property string: {0}", this);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <br><br>
	 *
	 * The method exports the <code>VHostItem</code> object to XML representation.
	 *
	 * @return an <code>Element</code> object with VHost information.
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
		if (saslAllowedMechanisms != null)
			elem.addAttribute(SASL_MECHANISM_ATT, stringArrayToString(saslAllowedMechanisms, ";"));
		elem.addAttribute(REGISTER_ENABLED_ATT, "" + registerEnabled);
		elem.addAttribute(TLS_REQUIRED_ATT, "" + tlsRequired);
		if (s2sSecret != null) {
			elem.addAttribute(S2S_SECRET_ATT, s2sSecret);
		}
		if (domainFilter != null) {
			elem.addAttribute(DOMAIN_FILTER_POLICY_ATT, domainFilter.toString());
		}
		if (domainFilterDomains != null) {
			elem.addAttribute(DOMAIN_FILTER_POLICY_DOMAINS_ATT, stringArrayToString( domainFilterDomains, ";"));
		}
		elem.addAttribute(MAX_USERS_NUMBER_ATT, "" + maxUsersNumber);
		if (presenceForward != null) {
			elem.addAttribute(PRESENCE_FORWARD_ADDRESS_ATT, presenceForward.toString());
		}
		if (messageForward != null) {
			elem.addAttribute(MESSAGE_FORWARD_ADDRESS_ATT, messageForward.toString());
		}
		if (c2sPortsAllowed != null) {
			String c2sPortsAllowedStr = intArrayToString(c2sPortsAllowed, ",");
			elem.addAttribute(C2S_PORTS_ALLOWED_ATT, c2sPortsAllowedStr);
		}

		if (!data.isEmpty()) {
			Element data = new Element("data");
			for (Map.Entry<String,Object> e : this.data.entrySet()) {
				Element item = new Element(e.getKey());
				item.addAttribute("type", String.valueOf(DataTypes.getTypeId(e.getValue())));
				Object val = e.getValue();
				DataType type = dataTypes.get(e.getKey());
				if (type != null && val instanceof Collection && type.getCollectionCls() != null) {
					Collection collection = (Collection) val;
					val = Array.newInstance(type.getCls().getComponentType(), collection.size());
					int i=0;
					for (Object v : collection) {
						Array.set(val, i, v);
						i++;
					}
				}
				item.setCData(DataTypes.valueToString(val));
				data.addChild(item);
			}
			elem.addChild(data);
		}
		
		return elem;
	}

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
		if (domainFilterDomains!=null) {
			sb.append( "=").append( stringArrayToString( domainFilterDomains, ";"));
		}
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
		if (c2sPortsAllowed != null) {
			sb.append(':').append(C2S_PORTS_ALLOWED_ATT).append('=').append(intArrayToString(c2sPortsAllowed, ";"));
		}

		if (saslAllowedMechanisms != null) {
			sb.append(':').append(SASL_MECHANISM_ATT).append('=').append(stringArrayToString(saslAllowedMechanisms, ";"));
		}

		return sb.toString();
	}

	@Override
	public String toString() {
		String str = "Domain: " + vhost + ", enabled: " + enabled
								 + ", anonym: " + anonymousEnabled + ", register: " + registerEnabled
								 + ", maxusers: " + maxUsersNumber + ", tls: " + tlsRequired
								 + ", s2sSecret: " + s2sSecret + ", domainFilter: " + domainFilter
								 + ", domainFilterDomains: " + stringArrayToString( domainFilterDomains, ";")
								 + ", c2sPortsAllowed: " + intArrayToString( c2sPortsAllowed, "," )
								 + ", saslAllowedMechanisms: " + Arrays.toString( saslAllowedMechanisms );
		
		for (Map.Entry<String,Object> e : data.entrySet()) {
			Object val = e.getValue();
			DataType type = dataTypes.get(e.getKey());
			if (type != null && val instanceof Collection && type.getCollectionCls() != null) {
				Collection collection = (Collection) val;
				val = Array.newInstance(type.getCls().getComponentType(), collection.size());
				int i = 0;
				for (Object v : collection) {
					Array.set(val, i, v);
					i++;
				}
			}			
			str += ", " + e.getKey() + ": " + DataTypes.valueToString(val);
		}
		
		return str;
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
	 * Returns an array with ports on which C2S connections for this VHosts
	 * are allowed.
	 *
	 * @return a <code>int[]</code> object with allowed C2S ports.
	 */
	public int[] getC2SPortsAllowed() {
		return c2sPortsAllowed;
	}

	/**
	 * Return value for key for this VHost
	 * 
	 * @param <T>
	 * @param key
	 * @return 
	 */
	public <T> T getData(String key) {
		T val = (T) data.get(key);
		if (val == null) {
			DataType type = dataTypes.get(key);
			if (type != null) {
				val = type.getDefValue();
			}
		}
		return val;
	}
	
	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>DomainFilterPolicy</code>
	 */
	public DomainFilterPolicy getDomainFilter() {
		if (domainFilter == null) {
			domainFilter = DomainFilterPolicy.valueof(System.getProperty(
					DOMAIN_FILTER_POLICY_PROP_KEY, DOMAIN_FILTER_POLICY_PROP_DEF.toString()));
		}

		return domainFilter;
	}

	public String[] getDomainFilterDomains() {
		return domainFilterDomains;
	}
	
	@Override
	public String getElemName() {
		return VHOST_ELEM;
	}

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
	 *
	 *
	 * @return a value of <code>JID</code>
	 */
	public JID getMessageForwardAddress() {
		return messageForward;
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
	 *
	 *
	 * @return a value of <code>JID</code>
	 */
	public JID getPresenceForwardAddress() {
		return presenceForward;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	public String getS2sSecret() {
		return s2sSecret;
	}

	public Set<String> getTrustedJIDs() {
		return getData(TRUSTED_JIDS_ATT);
	}
	
	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>VHostItem</code>
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
	 * Get boolean value contained by this VHost for key
	 * 
	 * @param key
	 * @return 
	 */
	public boolean isData(String key) {
		if (data.containsKey(key))
			return (Boolean) data.get(key);
		else {
			DataType type = dataTypes.get(key);
			Boolean defValue = (type == null) ? null : (Boolean)type.getDefValue();
			return defValue != null ? defValue : false;
		}
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
		return tlsRequired || XMPPServer.isHardenedModeEnabled();
	}
	
	public boolean isTrustedJID(JID jid) {
		Set<String> trustedJids = VHostItem.this.getTrustedJIDs();
		if (trustedJids == null)
			return false;
		
		return trustedJids.contains(jid.toString()) || trustedJids.contains(jid.getBareJID().toString()) 
				|| trustedJids.contains(jid.getDomain());
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
	 * Sets an array of ports for which C2S connections for this VHost will be
	 * allowed.
	 *
	 * @param ports
	 *			is an <code>int[]</code> array of allowed C2S ports.
	 */
	public void setC2SPortsAllowed(int[] ports) {
		this.c2sPortsAllowed = ports;
	}

	/**
	 * Set value for specified key for this VHost
	 * 
	 * @param key
	 * @param value 
	 */
	public void setData(String key, Object value) {
		if (value == null) {
			this.data.remove(key);
		} else {
			this.data.put(key, value);
		}
	}
	
	public void parseDataValue(String key, String valueStr) {
		DataType type = dataTypes.get(key);
		if (type == null)
			throw new RuntimeException("Key " + key + " is not registered");

		if (valueStr == null) {
			this.data.remove(key);
		} else {
			char typeId = DataTypes.typesMap.get(type.cls.getName());
			if (valueStr.contains(";")) 
				valueStr = valueStr.replace(';', ',');
			Object value = (valueStr == null || valueStr.isEmpty()) ? null : DataTypes.decodeValueType(typeId, valueStr);
			if (type.getCollectionCls() != null && value != null) {
				try {
					Collection collection = type.getCollectionCls().newInstance();
					for (int i = 0; i < Array.getLength(value); i++) {
						collection.add(Array.get(value, i));
					}
					value = collection;
				} catch (InstantiationException | IllegalAccessException ex) {
					throw new IllegalArgumentException("Could not instantiate collection of class: "
							+ type.getCollectionCls().getCanonicalName(), ex);
				}
			}
			
			setData(type.getKey(), value);
		}
	}
	
	/**
	 * This method allow configure DomainFilterPolicy to be applied during packet
	 * filtering.
	 *
	 * @param domainFilter name of the DomainFilterPolicy to be applied
	 */
	public void setDomainFilter(DomainFilterPolicy domainFilter) {
		this.domainFilter = domainFilter;
	}

	/**
	 * This method allow specify list of domains that will be used for packet
	 * filtering when DomainFilteringPolicy is set to either LIST or BLACKLIST.
	 *
	 * @param domainFilterDomains  array of domains to be applied during filtering
	 */
	public void setDomainFilterDomains(String[] domainFilterDomains) {
		this.domainFilterDomains = StringUtilities.internStringArray( domainFilterDomains);

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

	public void setTrustedJIDs(JID[] trustedJids) {
		setData(TRUSTED_JIDS_ATT, trustedJids);
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

	private int[] parseIntArray(String tmp, String separator) {
		int[] c2s_ports_allowed = null;
		if (tmp != null && !tmp.isEmpty()) {
			String[] tmpPorts = tmp.split(separator);
			c2s_ports_allowed = new int[tmpPorts.length];
			int filled = 0;
			for (String portStr : tmpPorts) {
				try {
					c2s_ports_allowed[filled] = Integer.parseInt(portStr);
					filled++;
				} catch (Exception ex) {
					log.log(Level.WARNING, "Can not parse allowed c2s port: {0}", portStr);
				}
			}
			if (filled == 0) {
				c2s_ports_allowed = null;
			}
			else if (filled < c2s_ports_allowed.length) {
				c2s_ports_allowed = Arrays.copyOf(c2s_ports_allowed, filled);
			}
			if (c2s_ports_allowed != null) {
				Arrays.sort(c2s_ports_allowed);
			}
		}
		return c2s_ports_allowed;
	}

	private String intArrayToString(int[] arr, String separator) {
		if (arr == null) {
			return null;
		}
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < arr.length; i++) {
			if (i > 0) {
				buf.append(separator);
			}
			buf.append(arr[i]);
		}
		return buf.toString();
	}

	private String stringArrayToString(String[] arr, String separator) {
		if (arr == null) {
			return null;
		}
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < arr.length; i++) {
			if (i > 0) {
				buf.append(separator);
			}
			buf.append(arr[i]);
		}
		return buf.toString();
	}

	//~--- inner classes --------------------------------------------------------

	private class UnmodifiableVHostItem
					extends VHostItem {
		@Override
		public void initFromElement(Element elem) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		@Override
		public void initFromPropertyString(String propString) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		@Override
		public Element toElement() {
			return VHostItem.this.toElement();
		}

		@Override
		public String toString() {
			return VHostItem.this.toString();
		}

		//~--- get methods --------------------------------------------------------

		@Override
		public String[] getComps() {
			return VHostItem.this.getComps();
		}

		@Override
		public <T> T getData(String key) {
			return VHostItem.this.getData(key);
		}
		
		@Override
		public DomainFilterPolicy getDomainFilter() {
			return VHostItem.this.getDomainFilter();
		}

		@Override
		public String[] getDomainFilterDomains() {
			return VHostItem.this.getDomainFilterDomains();
		}


		@Override
		public long getMaxUsersNumber() {
			return VHostItem.this.getMaxUsersNumber();
		}

		@Override
		public JID getMessageForward() {
			return VHostItem.this.messageForward;
		}

		@Override
		public String getOtherDomainParams() {
			return VHostItem.this.getOtherDomainParams();
		}

		@Override
		public JID getPresenceForward() {
			return VHostItem.this.presenceForward;
		}

		@Override
		public String getS2sSecret() {
			return VHostItem.this.getS2sSecret();
		}

		@Override
		public String[] getSaslAllowedMechanisms() {
			return VHostItem.this.getSaslAllowedMechanisms();
		}
		
		@Override
		public Set<String> getTrustedJIDs() {
			return Collections.unmodifiableSet(VHostItem.this.getTrustedJIDs());
		}

		@Override
		public VHostItem getUnmodifiableVHostItem() {
			return this;
		}

		@Override
		public JID getVhost() {
			return VHostItem.this.getVhost();
		}

		@Override
		public boolean isAnonymousEnabled() {
			return VHostItem.this.isAnonymousEnabled();
		}

		@Override
		public boolean isData(String key) {
			return VHostItem.this.isData(key);
		}
		
		@Override
		public boolean isEnabled() {
			return VHostItem.this.isEnabled();
		}

		@Override
		public boolean isRegisterEnabled() {
			return VHostItem.this.isRegisterEnabled();
		}

		@Override
		public boolean isTlsRequired() {
			return VHostItem.this.isTlsRequired();
		}

		@Override
		public boolean isTrustedJID(JID jid) {
			return VHostItem.this.isTrustedJID(jid);
		}
		
		//~--- set methods --------------------------------------------------------

		@Override
		public void setAnonymousEnabled(boolean enabled) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		@Override
		public void setComps(String[] comps) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		@Override
		public void setC2SPortsAllowed(int[] ports) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		@Override
		public void setData(String key, Object value) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}
		
		@Override
		public void setDomainFilter(DomainFilterPolicy filter) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		@Override
	public void setDomainFilterDomains(String[] domainFilterDomains) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
	}

		@Override
		public void setEnabled(boolean enabled) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		@Override
		public void setMaxUsersNumber(long maxUsersNumber) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		@Override
		public void setMessageForward(JID messageForward) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		@Override
		public void setOtherDomainParams(String otherParams) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		@Override
		public void setPresenceForward(JID presenceForward) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		@Override
		public void setRegisterEnabled(boolean enabled) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		@Override
		public void setS2sSecret(String s2sSecret) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		@Override
		public void setTlsRequired(boolean enabled) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}
	}

	/**
	 * @return the saslAllowedMechanisms
	 */
	public String[] getSaslAllowedMechanisms() {
		return saslAllowedMechanisms;
	}

	/**
	 * @param saslAllowedMechanisms the saslAllowedMechanisms to set
	 */
	public void setSaslAllowedMechanisms(String[] saslAllowedMechanisms) {
		this.saslAllowedMechanisms = saslAllowedMechanisms == null || saslAllowedMechanisms.length == 0 ? null
				: saslAllowedMechanisms;
	}
}

