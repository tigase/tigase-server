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
package tigase.vhosts;

import tigase.annotations.TigaseDeprecated;
import tigase.db.comp.RepositoryItemAbstract;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.util.StringUtilities;
import tigase.util.repository.DataTypes;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.vhosts.filter.DomainFilterPolicy;
import tigase.xml.Element;
import tigase.xml.XMLUtils;
import tigase.xmpp.jid.JID;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Objects of this class represent virtual host with all hosts configuration settings. In most cases simple domain name
 * string is not enough to deal with the virtual host. VHost can be enabled/disabled. Can be available to selected
 * server components only and so on. Therefore every time there is a detailed information needed for a vhost this
 * classed must be used.
 * <br>
 * This class has it's own XML representation which can be used for creating an instance of the class or can be exported
 * to the XML form for permanent storage:
 * <br>
 * {@code <vhost hostname="vhost.something.com" enabled="true" anon="true" register="true" max-users="99999999999L">
 * <comps/> <other/> }
 * <br>
 * From the init.property file it is also possible to set additional parameters for the vhost. By default everything is
 * enabled and max accounts set to unlimited. In the example below we configure 2 domains:
 * <strong>devel.tigase.org</strong> and <strong>test.tigase.org</strong>. For the first domain there are no additional
 * settings, hence the domain has everything on by default, whereas the second has everything switched off and max user
 * accounts set to 100.
 * <br>
 * <pre>
 * --virt-hosts = devel.tigase.org,test.tigase.org:-anon:-register:max-users=100
 * </pre>
 * <br>
 * It also possible to set forwarding for the domain:
 * <br>
 * <pre>
 * --virt-hosts = test.tigase.org:pres-forw=lpart@domain/res:mess-forw=lpart@domain/res
 * </pre>
 * <br>
 * Please note, forwarding address set this way cannot contain any of characters: [,:=] The order features are set for
 * domain is unimportant.
 * <br>
 * Created: 22 Nov 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
*/
public class VHostItemImpl
		extends RepositoryItemAbstract
		implements VHostItem {

	public static final String ANONYMOUS_ENABLED_ATT = "anon";
	public static final String ANONYMOUS_ENABLED_LABEL = "Anonymous enabled";
	public static final String SASL_MECHANISM_ATT = "sasl-mechanisms";
	public static final String SASL_MECHANISM_LABEL = "Allowed SASL mechanisms";
	public static final String COMPONENTS_ATT = "comps";
	/**
	 * Element name for the VHostItem XML child keeping list of the server component which can handle packets for this
	 * domain. In most cases this element should be empty.
	 */
	public static final String COMPONENTS_ELEM = "comps";
	public static final String C2S_PORTS_ALLOWED_ATT = "c2s-ports-allowed";
	public static final String C2S_PORTS_ALLOWED_LABEL = "Allowed C2S,BOSH,WebSocket ports";
	public static final String DOMAIN_FILTER_POLICY_ATT = "domain-filter";
	public static final String DOMAIN_FILTER_POLICY_DOMAINS_ATT = "domain-filter-domains";
	public static final String DOMAIN_FILTER_POLICY_LABEL = "Domain filter policy";
	public static final String DOMAIN_FILTER_POLICY_DOMAINS_LABEL = "Domain filter domains (only LIST and BLACKLIST)";
	public static final String ENABLED_ATT = "enabled";
	public static final String ENABLED_LABEL = "Enabled";
	public static final String HOSTNAME_ATT = "hostname";
	public static final String HOSTNAME_LABEL = "Domain name";
	public static final String MAX_USERS_NUMBER_ATT = "max-users";
	public static final String MAX_USERS_NUMBER_LABEL = "Max users";
	public static final String MESSAGE_FORWARD_ADDRESS_ATT = "mess-forw";
	public static final String MESSAGE_FORWARD_ADDRESS_LABEL = "Message forward address";
	public static final String OTHER_PARAMS_ELEM = "other";
	public static final String OTHER_PARAMS_LABEL = "Other parameters";
	public static final String PRESENCE_FORWARD_ADDRESS_ATT = "pres-forw";
	public static final String PRESENCE_FORWARD_ADDRESS_LABEL = "Presence forward address";
	public static final String REGISTER_ENABLED_ATT = "register";
	public static final String REGISTER_ENABLED_LABEL = "In-band registration";
	public static final String S2S_SECRET_ATT = "s2s-secret";
	public static final String S2S_SECRET_LABEL = "S2S secret";
	public static final String TLS_REQUIRED_ATT = "tls-required";
	public static final String TLS_REQUIRED_LABEL = "TLS required";
	private static final String TRUSTED_JIDS_ATT = "trusted-jids";
	public static final String TRUSTED_JIDS_LABEL = "Trusted JIDs";
	public static final String VHOST_ELEM = "vhost";
	protected static final String DOMAIN_FILTER_POLICY_PROP_KEY = "domain-filter-policy";
	protected static final String S2S_SECRET_PROP_DEF = null;
	protected static final String S2S_SECRET_PROP_KEY = "s2s-secret";
	protected static final String VHOST_ANONYMOUS_ENABLED_PROP_KEY = "vhost-anonymous-enabled";
	protected static final Boolean VHOST_ANONYMOUS_ENABLED_PROP_DEF = Boolean.FALSE;
	protected static final String VHOST_MAX_USERS_PROP_KEY = "vhost-max-users";
	protected static final Long VHOST_MAX_USERS_PROP_DEF = Long.valueOf(0l);
	protected static final String VHOST_MESSAGE_FORWARD_PROP_DEF = null;
	protected static final String VHOST_MESSAGE_FORWARD_PROP_KEY = "vhost-message-forward-jid";
	protected static final String VHOST_PRESENCE_FORWARD_PROP_DEF = null;
	protected static final String VHOST_PRESENCE_FORWARD_PROP_KEY = "vhost-presence-forward-jid";
	protected static final String VHOST_REGISTER_ENABLED_PROP_KEY = "vhost-register-enabled";
	protected static final Boolean VHOST_REGISTER_ENABLED_PROP_DEF = Boolean.TRUE;
	protected static final String VHOST_TLS_REQUIRED_PROP_KEY = "vhost-tls-required";
	protected static final Boolean VHOST_TLS_REQUIRED_PROP_DEF = Boolean.TRUE;
	protected static final DomainFilterPolicy DOMAIN_FILTER_POLICY_PROP_DEF = DomainFilterPolicy.ALL;
	protected static final String[] VHOST_OTHER_PARAMS_PATH = {VHOST_ELEM, OTHER_PARAMS_ELEM};
	protected static final String[] VHOST_COMPONENTS_PATH = {VHOST_ELEM, COMPONENTS_ELEM};
	protected static final Map<String, DataType> dataTypes = Collections.synchronizedMap(new LinkedHashMap<>());
	private static final Logger log = Logger.getLogger(VHostItemImpl.class.getName());
	
	private boolean anonymousEnabled = VHOST_ANONYMOUS_ENABLED_PROP_DEF;
	private int[] c2sPortsAllowed = null;

	private String[] comps = new String[0];
	private Map<String, Element> unknownExtensions = new ConcurrentHashMap<>();
	private Map<Class<? extends VHostItemExtension>, VHostItemExtension> extensions = new ConcurrentHashMap<>();
	@Deprecated
	@TigaseDeprecated(since = "8.1.0", removeIn = "9.0.0")
	private Map<String, Object> oldData = new ConcurrentHashMap<String, Object>();
	private DomainFilterPolicy domainFilter = DOMAIN_FILTER_POLICY_PROP_DEF;
	private String[] domainFilterDomains = null;
	private boolean enabled = true;
	private Long maxUsersNumber = VHOST_MAX_USERS_PROP_DEF;
	private JID messageForward = JID.jidInstanceNS(VHOST_MESSAGE_FORWARD_PROP_DEF);
	private String otherDomainParams = null;
	private JID presenceForward = JID.jidInstanceNS(VHOST_PRESENCE_FORWARD_PROP_DEF);
	private boolean registerEnabled = VHOST_REGISTER_ENABLED_PROP_DEF;
	private String s2sSecret = S2S_SECRET_PROP_DEF;
	private String[] saslAllowedMechanisms = null;
	private boolean tlsRequired = VHOST_TLS_REQUIRED_PROP_DEF;
	private Set<String> trustedJids = Collections.emptySet();
	private JID vhost = null;

	private VHostItemExtensionManager extensionManager;
	
	static DomainFilterPolicy getPolicyFromConfString(String configuration) {
		String[] df = configuration.split("=");

		try {
			if (df.length >= 2) {
				return DomainFilterPolicy.valueof(df[1]);
			} else {
				return DomainFilterPolicy.ALL;
			}
		} catch (Exception e) {
			return DomainFilterPolicy.ALL;
		}
	}

	static String[] getDomainsFromConfString(String configuration) {
		String[] df = configuration.split("=");

		try {
			if (df.length >= 3 && df[2] != null && !df[2].trim().isEmpty()) {
				return StringUtilities.stringToArrayOfString(df[2], ";");
			}
		} catch (Exception e) {
			// just return default for non-existing configuration
		}
		return new String[0];
	}

	@Deprecated
	@TigaseDeprecated(since = "8.1.0", removeIn = "9.0.0")
	public static void registerData(List<DataType> types) {
		for (DataType type : types) {
			dataTypes.put(type.getKey(), type);
		}
	}

	public VHostItemImpl() {
		// added to ensure that we have s2sSecret set, as without it S2S connections
		// will always fail (needed mostly for newly added vhosts).
		if (s2sSecret == null) {
			s2sSecret = UUID.randomUUID().toString();
		}
	}

	/**
	 * The constructor creates the <code>VHostItem</code> instance from a given XML element. Please refer to the class
	 * documentation for more details of the XML element.
	 *
	 * @param elem is an <code>Element</code> object with virtual domain settings.
	 */
	public VHostItemImpl(Element elem) {
		this();
		initFromElement(elem);
	}

	/**
	 * The constructor creates the <code>VHostItem</code> instance for a given domain name with default values for all
	 * other parameters. By the default all domain parameters are set to true.
	 *
	 * @param vhost is a <code>String</code> value with a domain name.
	 */
	public VHostItemImpl(JID vhost) {
		this();
		setVHost(vhost);
	}

	/**
	 * The constructor creates the <code>VHostItem</code> instance for a given domain name with default values for all
	 * other parameters. By the default all domain parameters are set to true.
	 *
	 * @param vhost is a <code>String</code> value with a domain name.
	 *
	 * @throws TigaseStringprepException if the provided string causes stringprep processing errors.
	 */
	public VHostItemImpl(String vhost) throws TigaseStringprepException {
		this();
		setVHost(vhost);
	}

	protected void setExtensionManager(VHostItemExtensionManager extensionManager) {
		this.extensionManager = extensionManager;
	}

	@Override
	public void addCommandFields(Packet packet) {
		if (!isDefault()) {
			Command.addInstructions(packet, "❗NOTE: Options without value set will use configuration defined in 'DEFAULT' VHost❗");
		} else {
			Command.addInstructions(packet, "❗This VHost is intended to configure certain default values for all VHosts in the system and it's not actual, available domain to which you can connect❗");
		}
		Command.addFieldValue(packet, HOSTNAME_LABEL, (vhost != null) ? vhost.getDomain() : "");
		if (!isDefault()) {
			Command.addCheckBoxField(packet, ENABLED_LABEL, enabled);
		}
		Command.addCheckBoxField(packet, ANONYMOUS_ENABLED_LABEL, anonymousEnabled);
		Command.addCheckBoxField(packet, REGISTER_ENABLED_LABEL, registerEnabled);
		Command.addCheckBoxField(packet, TLS_REQUIRED_LABEL, tlsRequired);
		if (!isDefault()) {
			Command.addFieldValue(packet, S2S_SECRET_LABEL, (s2sSecret != null) ? s2sSecret : "");
		}
		if (isDefault()) {
			Command.addFieldValue(packet, DOMAIN_FILTER_POLICY_LABEL, domainFilter.toString(),
								  DOMAIN_FILTER_POLICY_LABEL, DomainFilterPolicy.valuesStr(), DomainFilterPolicy.valuesStr());
		} else {
			String[] values = Stream.concat(Stream.of("DEFAULT"), Arrays.stream(DomainFilterPolicy.valuesStr()))
					.toArray(String[]::new);
			Command.addFieldValue(packet, DOMAIN_FILTER_POLICY_LABEL, domainFilter != null ? domainFilter.toString() : "",
								  DOMAIN_FILTER_POLICY_LABEL, values, values);
		}
		Command.addFieldValue(packet, DOMAIN_FILTER_POLICY_DOMAINS_LABEL,
							  domainFilterDomains != null ? StringUtilities.stringArrayToString(domainFilterDomains, ";") : "");
		Command.addFieldValue(packet, MAX_USERS_NUMBER_LABEL,
							  Optional.ofNullable(maxUsersNumber).map(String::valueOf).orElse(""));
		String c2sPortsAllowedStr = intArrayToString(c2sPortsAllowed, ",");
		Command.addFieldValue(packet, C2S_PORTS_ALLOWED_LABEL, c2sPortsAllowedStr != null ? c2sPortsAllowedStr : "");
		Command.addFieldValue(packet, PRESENCE_FORWARD_ADDRESS_LABEL,
							  ((presenceForward != null) ? presenceForward.toString() : ""));
		Command.addFieldValue(packet, MESSAGE_FORWARD_ADDRESS_LABEL,
							  ((messageForward != null) ? messageForward.toString() : ""));
		Command.addFieldValue(packet, OTHER_PARAMS_LABEL, (otherDomainParams != null) ? otherDomainParams : "");
		Command.addFieldValue(packet, SASL_MECHANISM_LABEL,
							  saslAllowedMechanisms != null ? StringUtilities.stringArrayToString(saslAllowedMechanisms, ",") : "");

		Command.addFieldMultiValue(packet, TRUSTED_JIDS_LABEL,
								   trustedJids != null ? new ArrayList<>(trustedJids) : Collections.EMPTY_LIST);

		super.addCommandFields(packet);

		extensionManager.addMissingExtensions(extensions.values()
													  .stream()
													  .map(v -> (VHostItemExtension) v)
													  .collect(Collectors.toSet()))
				.sorted(Comparator.comparing(VHostItemExtension::getId))
				.forEach(ext -> ext.addCommandFields(ext.getId(), packet, isDefault()));

		for (DataType type : dataTypes.values()) {
			if (type.cls != Boolean.class) {
				Object[] options = type.getOptions();
				Object val = getData(type.getKey());
				if (val instanceof Collection) {
					Collection collection = (Collection) val;
					val = Array.newInstance(type.getCls().getComponentType(), collection.size());
					int i = 0;
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
					for (int i = 0; i < options.length; i++) {
						optionsStr[i] = (options[i] != null) ? DataTypes.valueToString(options[i]) : "";
					}
					String[] optionsNames = type.getOptionsNames();
					if (optionsNames == null) {
						optionsNames = optionsStr;
					}
					Command.addFieldValue(packet, type.getName(), valueStr, type.getName(), optionsNames, optionsStr);
				}
			} else {
				boolean val = isData(type.getKey());
				Command.addCheckBoxField(packet, type.getName(), val);
			}
		}
	}
	
	@Override
	public boolean equals(Object v) {
		return (v instanceof VHostItem) ? getKey().equals(((VHostItem) v).getKey()) : false;
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
			throw new IllegalArgumentException("Incorrect domain, unable to parse it: " + tmp, ex);
		}
		enabled = Command.getCheckBoxFieldValue(packet, ENABLED_LABEL);
		anonymousEnabled = Command.getCheckBoxFieldValue(packet, ANONYMOUS_ENABLED_LABEL);
		registerEnabled = Command.getCheckBoxFieldValue(packet, REGISTER_ENABLED_LABEL);
		Optional.ofNullable(Command.getFieldValue(packet, TLS_REQUIRED_LABEL))
				.ifPresent(s -> tlsRequired = Boolean.parseBoolean(s));
		tmp = Command.getFieldValue(packet, S2S_SECRET_LABEL);
		if ((tmp != null) && !tmp.trim().isEmpty()) {
			s2sSecret = tmp;
		}
		tmp = Command.getFieldValue(packet, DOMAIN_FILTER_POLICY_LABEL);
		try {
			domainFilter = DomainFilterPolicy.valueof(tmp);
			if (domainFilter == null && isDefault()) {
				domainFilter = DOMAIN_FILTER_POLICY_PROP_DEF;
			}
			if (domainFilter != null && domainFilter.isDomainListRequired()) {
				domainFilterDomains = Optional.ofNullable(
						Command.getFieldValue(packet, DOMAIN_FILTER_POLICY_DOMAINS_LABEL))
						.map(String::trim)
						.filter(s -> !s.isEmpty())
						.map(s -> StringUtilities.stringToArrayOfString(s, ";"))
						.orElse(null);
			} else {
				domainFilterDomains = null;
			}
		} catch (Exception ex) {
			domainFilter = isDefault() ? DOMAIN_FILTER_POLICY_PROP_DEF : null;
			domainFilterDomains = null;
		}
		try {
			maxUsersNumber = Optional.ofNullable(Command.getFieldValue(packet, MAX_USERS_NUMBER_LABEL))
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.map(Long::parseLong)
					.orElse(isDefault() ? VHOST_MAX_USERS_PROP_DEF : null);
		} catch (Exception e) {
			log.log(Level.WARNING, "Can not parse max users number: {0}",
					Command.getFieldValue(packet, MAX_USERS_NUMBER_LABEL));
		}
		tmp = Command.getFieldValue(packet, PRESENCE_FORWARD_ADDRESS_LABEL);
		if ((tmp != null) && !tmp.trim().isEmpty()) {
			try {
				presenceForward = JID.jidInstance(tmp);
			} catch (TigaseStringprepException ex) {
				presenceForward = null;

				throw new IllegalArgumentException("Incorrect presence forward address: " + tmp, ex);
			}
		}
		tmp = Command.getFieldValue(packet, MESSAGE_FORWARD_ADDRESS_LABEL);
		if ((tmp != null) && !tmp.trim().isEmpty()) {
			try {
				messageForward = JID.jidInstance(tmp);
			} catch (TigaseStringprepException ex) {
				messageForward = null;

				throw new IllegalArgumentException("Incorrect message forward address: " + tmp, ex);
			}
		}
		otherDomainParams = Optional.ofNullable(Command.getFieldValue(packet, OTHER_PARAMS_LABEL)).filter(s -> !s.isEmpty()).orElse(null);
		tmp = Command.getFieldValue(packet, C2S_PORTS_ALLOWED_LABEL);
		c2sPortsAllowed = parseIntArray(tmp, ",");

		tmp = Command.getFieldValue(packet, SASL_MECHANISM_LABEL);
		if ((tmp != null) && !tmp.trim().isEmpty()) {
			final String[] split = tmp.split(",");
			for (String mechanism : split) {
				mechanism.trim();
			}
			setSaslAllowedMechanisms(split);
		}

		String[] tmps = Command.getFieldValues(packet, TRUSTED_JIDS_LABEL);
		if (tmps != null) {
			trustedJids = Arrays.stream(tmps).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
		} else {
			trustedJids = Collections.emptySet();
		}

		extensionManager.newExtensionInstances().map(extension -> {
			extension.initFromCommand(extension.getId(), packet);
			return extension;
		}).forEach(extension -> extensions.put((Class<VHostItemExtension>) extension.getClass(), extension));

		for (DataType type : dataTypes.values()) {
			String valueStr = Command.getFieldValue(packet, type.getName());
			Character typeId = DataTypes.typesMap.get(type.cls.getName());
			Object value = (valueStr == null || valueStr.isEmpty())
						   ? null
						   : DataTypes.decodeValueType(typeId, valueStr);
			if (value != null && type.getCollectionCls() != null) {
				try {
					Collection collection = type.getCollectionCls().newInstance();
					for (int i = 0; i < Array.getLength(value); i++) {
						collection.add(Array.get(value, i));
					}
					value = collection;
				} catch (InstantiationException | IllegalAccessException ex) {
					throw new IllegalArgumentException(
							"Could not instantiate collection of class: " + type.getCollectionCls().getCanonicalName(),
							ex);
				}
			}
			setData(type.getKey(), value);
		}

		log.log(Level.FINE, "Initialized from command: {0}", this);

	}

	private static final Set<String> bannedExtensionIds = new HashSet<>(Arrays.asList(COMPONENTS_ELEM, OTHER_PARAMS_ELEM, TRUSTED_JIDS_ATT, "data"));

	@Override
	public void initFromElement(Element elem) {
		if (elem.getName() != VHOST_ELEM) {
			throw new IllegalArgumentException("Incorrect element name, expected: " + VHOST_ELEM);
		}
		super.initFromElement(elem);
		setVHost(JID.jidInstanceNS(elem.getAttributeStaticStr(HOSTNAME_ATT)));
		enabled = Boolean.parseBoolean(elem.getAttributeStaticStr(ENABLED_ATT));
		anonymousEnabled = Boolean.parseBoolean(elem.getAttributeStaticStr(ANONYMOUS_ENABLED_ATT));
		registerEnabled = Boolean.parseBoolean(elem.getAttributeStaticStr(REGISTER_ENABLED_ATT));
		tlsRequired = Boolean.parseBoolean(elem.getAttributeStaticStr(TLS_REQUIRED_ATT));
		s2sSecret = elem.getAttributeStaticStr(S2S_SECRET_ATT);
		try {
			domainFilter = DomainFilterPolicy.valueof(elem.getAttributeStaticStr(DOMAIN_FILTER_POLICY_ATT));
			if (domainFilter == null && isDefault()) {
				domainFilter = DOMAIN_FILTER_POLICY_PROP_DEF;
			}
			if (domainFilter != null && domainFilter.isDomainListRequired()) {
				String tmp = elem.getAttributeStaticStr(DOMAIN_FILTER_POLICY_DOMAINS_ATT);
				if (tmp != null && !tmp.trim().isEmpty()) {
					domainFilterDomains = StringUtilities.stringToArrayOfString(tmp, ";");
				}
			}
		} catch (Exception e) {
			domainFilter = isDefault() ? DOMAIN_FILTER_POLICY_PROP_DEF : null;
		}
		try {
			maxUsersNumber = Optional.ofNullable(elem.getAttributeStaticStr(MAX_USERS_NUMBER_ATT))
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.map(Long::parseLong)
					.orElse(isDefault() ? Long.getLong(VHOST_MAX_USERS_PROP_KEY, VHOST_MAX_USERS_PROP_DEF) : null);
		} catch (Exception e) {
			log.log(Level.WARNING, "Can not parse max users number: {0}",
					elem.getAttributeStaticStr(MAX_USERS_NUMBER_ATT));
			maxUsersNumber = isDefault() ? Long.getLong(VHOST_MAX_USERS_PROP_KEY, VHOST_MAX_USERS_PROP_DEF) : null;
		}

		String tmp = elem.getAttributeStaticStr(PRESENCE_FORWARD_ADDRESS_ATT);

		if ((tmp != null) && !tmp.trim().isEmpty()) {
			presenceForward = JID.jidInstanceNS(tmp);
		}
		tmp = elem.getAttributeStaticStr(MESSAGE_FORWARD_ADDRESS_ATT);
		if ((tmp != null) && !tmp.trim().isEmpty()) {
			messageForward = JID.jidInstanceNS(tmp);
		}

		String comps_str = elem.getCDataStaticStr(VHOST_COMPONENTS_PATH);

		if ((comps_str != null) && !comps_str.isEmpty()) {
			comps = comps_str.split(",");
		}
		otherDomainParams = Optional.ofNullable(elem.getCDataStaticStr(VHOST_OTHER_PARAMS_PATH))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.orElse(isDefault() ? "" : null);

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
					Character typeChar = type != null
										 ? DataTypes.typesMap.get(type.getCls().getName())
										 : item.getAttributeStaticStr("type").charAt(0);
					Object value = DataTypes.decodeValueType(typeChar, item.getCData());
					if (type != null && type.getCollectionCls() != null && value != null) {
						try {
							Collection collection = type.getCollectionCls().newInstance();
							for (int i = 0; i < Array.getLength(value); i++) {
								collection.add(Array.get(value, i));
							}
							value = collection;
						} catch (InstantiationException | IllegalAccessException ex) {
							throw new IllegalArgumentException("Could not instantiate collection of class: " +
																	   type.getCollectionCls().getCanonicalName(), ex);
						}
					}
					setData(item.getName(), value);
				}
			}
		}

		tmp = elem.getCDataStaticStr(new String[]{VHOST_ELEM, TRUSTED_JIDS_ATT});
		if (tmp == null) {
			tmp = (String) oldData.remove(TRUSTED_JIDS_ATT);
		}
		if (tmp != null && !tmp.isEmpty()) {
			this.trustedJids = Collections.unmodifiableSet(
					Arrays.stream(tmp.split(",")).map(String::trim).collect(Collectors.toSet()));
		} else  {
			this.trustedJids = Collections.emptySet();
		}

		List<Element> children = elem.getChildren();
		if (children != null) {
			unknownExtensions.putAll(children
					.stream()
					.filter(child -> !bannedExtensionIds.contains(child.getName()))
					.collect(Collectors.toConcurrentMap(Element::getName, Function.identity())));
		}
		extensionManager.newExtensionInstances()
				.map(this::initExtension)
				.forEach(extension -> extensions.put((Class<VHostItemExtension>) extension.getClass(), extension));

		log.log(Level.FINE, "Initialized from element: {0}", this);
	}

	protected VHostItemExtension initExtension(VHostItemExtension extension) {
		final String id = extension.getId();
		Element extElem = unknownExtensions.remove(id);
		if (extElem != null) {
			try {
				extension.initFromElement(extElem);
			} catch (Throwable ex) {
				throw new IllegalArgumentException(
						"Could not initialize " + extension.getClass().getCanonicalName() + " with data " +
								extElem);
			}
		} else if (extension instanceof VHostItemExtensionBackwardCompatible) {
			try {
				((VHostItemExtensionBackwardCompatible) extension).initFromData(oldData);
			} catch (Throwable ex) {
				throw new IllegalArgumentException(
						"Could not initialize " + extension.getClass().getCanonicalName() + " with oldData " +
								oldData, ex);
			}
		}
		return extension;
	}

	@Override
	public void setKey(String key) {
		setVHost(JID.jidInstanceNS(key));
	}
	
	@Override
	public void initFromPropertyString(String propString) {
		String[] props = propString.split(":");

		try {
			setVHost(props[0]);
		} catch (TigaseStringprepException ex) {
			throw new IllegalArgumentException("Domain misconfiguration, cannot parse it: " + props[0], ex);
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
				String[] c = tmp.split("=");
				String comps_str = c[1];

				if (!comps_str.isEmpty()) {
					comps = comps_str.split(";");
				}
			}
			if (tmp.startsWith(DOMAIN_FILTER_POLICY_ATT)) {
				domainFilter = getPolicyFromConfString(tmp);
				if (domainFilter == null && isDefault()) {
					domainFilter = DOMAIN_FILTER_POLICY_PROP_DEF;
				}
				if (domainFilter.isDomainListRequired()) {
					domainFilterDomains = getDomainsFromConfString(tmp);
				}
			}
			if (tmp.startsWith(MAX_USERS_NUMBER_ATT)) {
				String[] mu = tmp.split("=");

				try {
					maxUsersNumber = Long.parseLong(mu[1]);
				} catch (NumberFormatException ex) {
					maxUsersNumber = 0L;
					log.log(Level.WARNING, "Incorrect max users number for vhost settings, number parsing error: {0}",
							tmp);
				}
			}
			if (tmp.startsWith(PRESENCE_FORWARD_ADDRESS_ATT)) {
				String[] mu = tmp.split("=");

				try {
					presenceForward = JID.jidInstance(mu[1]);
				} catch (TigaseStringprepException ex) {
					presenceForward = null;
					log.log(Level.WARNING, "Incorrect presence forwarding address, address parsing error: {0}", tmp);
				}
			}
			if (tmp.startsWith(MESSAGE_FORWARD_ADDRESS_ATT)) {
				String[] mu = tmp.split("=");

				try {
					messageForward = JID.jidInstance(mu[1]);
				} catch (TigaseStringprepException ex) {
					messageForward = null;
					log.log(Level.WARNING, "Incorrect presence forwarding address, address parsing error: {0}", tmp);
				}
			}
			if (tmp.startsWith(C2S_PORTS_ALLOWED_ATT)) {
				String[] mu = tmp.split("=");

				c2sPortsAllowed = parseIntArray(mu[1], ";");
			}
			if (tmp.startsWith(SASL_MECHANISM_ATT)) {
				String[] mu = tmp.split("=");
				setSaslAllowedMechanisms(mu[1].split(";"));
			}

			String[] mu = tmp.split("=");
			if (mu != null && mu.length == 2) {
				if (dataTypes.containsKey(mu[0])) {
					parseDataValue(mu[0], mu[1]);

				} else if (mu[0].equals(TRUSTED_JIDS_ATT)) {
					if (mu[1].contains(",")) {
						trustedJids = Collections.unmodifiableSet(Arrays.stream(mu[1].split(",")).map(String::trim).collect(Collectors.toSet()));
					} else {
						trustedJids = Collections.unmodifiableSet(Arrays.stream(mu[1].split(";")).map(String::trim).collect(Collectors.toSet()));
					}
				}
			}

		}
		if (trustedJids == null) {
			trustedJids = Collections.EMPTY_SET;
		}
		if (extensionManager != null) {
			extensionManager.newExtensionInstances()
					.map(this::initExtension)
					.forEach(extension -> extensions.put((Class<VHostItemExtension>) extension.getClass(), extension));
		}
		log.log(Level.FINE, "Initialized from property string: {0}", this);
	}

	@Override
	public Element toElement() {
		Element elem = super.toElement();
		String comps_str = "";

		if ((comps != null) && (comps.length > 0)) {
			for (String comp : comps) {
				if (!comps_str.isEmpty()) {
					comps_str += ",";
				}
				comps_str += comp;
			}
		}

		String other_params = (otherDomainParams != null) ? otherDomainParams : "";

		if (comps_str != null && !comps_str.isEmpty()) {
			elem.addChild(new Element(COMPONENTS_ELEM, comps_str));
		}
		if (other_params != null && !other_params.isEmpty()) {
			elem.addChild(new Element(OTHER_PARAMS_ELEM, other_params));
		}
		elem.addAttribute(HOSTNAME_ATT, vhost.getDomain());
		elem.addAttribute(ENABLED_ATT, "" + enabled);
		elem.addAttribute(ANONYMOUS_ENABLED_ATT, "" + anonymousEnabled);
		if (saslAllowedMechanisms != null) {
			elem.addAttribute(SASL_MECHANISM_ATT, StringUtilities.stringArrayToString(saslAllowedMechanisms, ";"));
		}
		elem.addAttribute(REGISTER_ENABLED_ATT, "" + registerEnabled);
		elem.addAttribute(TLS_REQUIRED_ATT, "" + tlsRequired);
		if (s2sSecret != null) {
			elem.addAttribute(S2S_SECRET_ATT, s2sSecret);
		}
		if (domainFilter != null) {
			elem.addAttribute(DOMAIN_FILTER_POLICY_ATT, domainFilter.toString());
		}
		if (domainFilterDomains != null) {
			elem.addAttribute(DOMAIN_FILTER_POLICY_DOMAINS_ATT, StringUtilities.stringArrayToString(domainFilterDomains, ";"));
		}
		if (maxUsersNumber != null) {
			elem.addAttribute(MAX_USERS_NUMBER_ATT, "" + maxUsersNumber);
		}
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

		if (trustedJids != null && !trustedJids.isEmpty()) {
			elem.addChild(new Element(TRUSTED_JIDS_ATT,
									  StringUtilities.stringArrayToString(trustedJids.toArray(new String[0]), ",")));
		}

		extensions.entrySet()
				.stream()
				.sorted(Comparator.comparing(e -> e.getKey().getSimpleName()))
				.map(Map.Entry::getValue)
				.map(VHostItemExtension::toElement)
				.filter(Objects::nonNull)
				.forEach(elem::addChild);

		unknownExtensions.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).forEach((e) -> {
			elem.addChild((Element) e.getValue());
		});

		if (!oldData.isEmpty()) {
			Element data = new Element("data");
			for (Map.Entry<String, Object> e : this.oldData.entrySet()) {
				Element item = new Element(e.getKey());
				item.addAttribute("type", String.valueOf(DataTypes.getTypeId(e.getValue())));
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
		sb.append(':').append(DOMAIN_FILTER_POLICY_ATT).append('=').append(domainFilter.toString());
		if (domainFilterDomains != null) {
			sb.append("=").append(StringUtilities.stringArrayToString(domainFilterDomains, ";"));
		}
		if (maxUsersNumber != null && maxUsersNumber > 0) {
			sb.append(':').append(MAX_USERS_NUMBER_ATT).append('=').append(maxUsersNumber);
		}
		if (presenceForward != null) {
			sb.append(':').append(PRESENCE_FORWARD_ADDRESS_ATT).append('=').append(presenceForward.toString());
		}
		if (messageForward != null) {
			sb.append(':').append(MESSAGE_FORWARD_ADDRESS_ATT).append('=').append(messageForward.toString());
		}
		if (c2sPortsAllowed != null) {
			sb.append(':').append(C2S_PORTS_ALLOWED_ATT).append('=').append(intArrayToString(c2sPortsAllowed, ";"));
		}

		if (saslAllowedMechanisms != null) {
			sb.append(':')
					.append(SASL_MECHANISM_ATT)
					.append('=')
					.append(StringUtilities.stringArrayToString(saslAllowedMechanisms, ";"));
		}

		return sb.toString();
	}

	@Override
	public String toString() {
		final StringBuilder sb = toString(this);
		for (Map.Entry<String, Object> e : oldData.entrySet()) {
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
			sb.append(", ").append(e.getKey()).append(": ").append(DataTypes.valueToString(val));
		}
		return sb.toString();
	}

	protected static StringBuilder toString(VHostItem item) {
		StringBuilder sb = new StringBuilder();
		sb.append("Domain: ").append(item.getVhost());
		sb.append(", enabled: ").append(item.isEnabled());
		sb.append(", anonym: ").append(item.isAnonymousEnabled());
		sb.append(", register: ").append(item.isRegisterEnabled());
		sb.append(", tls: ").append(item.isTlsRequired());
		if (item.getMaxUsersNumber() != null) {
			sb.append(", maxusers: ").append(item.getMaxUsersNumber());
		}
		if (item.getS2sSecret() != null) {
			sb.append(", s2sSecret: ").append(item.getS2sSecret());
		}
		if (item.getDomainFilter() != null) {
			sb.append(", domainFilter: ").append(item.getDomainFilter());
		}
		if (item.getDomainFilterDomains() != null && item.getDomainFilterDomains().length > 0) {
			sb.append(", domainFilterDomains: ").append(StringUtilities.stringArrayToString(item.getDomainFilterDomains(), ";"));
		}
		if (item.getC2SPortsAllowed() != null && item.getC2SPortsAllowed().length > 0) {
			sb.append(", c2sPortsAllowed: ").append(Arrays.toString(item.getC2SPortsAllowed()));
		}
		if (item.getSaslAllowedMechanisms() != null && item.getSaslAllowedMechanisms().length > 0) {
			sb.append(", saslAllowedMechanisms: ").append(Arrays.toString(item.getSaslAllowedMechanisms()));
		}
		if (item.getTrustedJIDs() != null && !item.getTrustedJIDs().isEmpty()) {
			sb.append(", trustedJids: ").append(item.getTrustedJIDs());
		}
		if (item.getMessageForward() != null) {
			sb.append(", messageForward: ").append(item.getMessageForward());
		}
		if (item.getPresenceForward() != null) {
			sb.append(", presenceForward: ").append(item.getPresenceForward());
		}
		if (!item.getExtensions().isEmpty()) {
			sb.append(item.getExtensions()
							  .stream()
							  .map(VHostItemExtension::toString)
							  .collect(Collectors.joining(", ", "; Extensions: [", "]")));
		}
		return sb;
	}

	/**
	 * Returns an array with the server components names which should process packets sent to this domain or
	 * <code>null</code> (default) if there is no specific component assigned to this domain.
	 *
	 * @return a <code>String[]</code> object with server component names.
	 */
	@Override
	public String[] getComps() {
		return comps;
	}

	/**
	 * Sets an array with the server component names by which packets to this domain can be processed. Every local
	 * domain will be handled by <code>VHostListener</code> which returns <code>true</code> for
	 * <code>handlesLocalDomains()</code> method call and by all components set via this method.
	 *
	 * @param comps is an <code>String[]</code> array with server component names.
	 */
	public void setComps(String[] comps) {
		this.comps = comps;
	}

	/**
	 * Returns an array with ports on which C2S connections for this VHosts are allowed.
	 *
	 * @return a <code>int[]</code> object with allowed C2S ports.
	 */
	@Override
	public int[] getC2SPortsAllowed() {
		return c2sPortsAllowed;
	}

	public void setC2SPortsAllowed(int[] ports) {
		this.c2sPortsAllowed = ports;
	}

	@Deprecated
	@TigaseDeprecated(since = "8.1.0", removeIn = "9.0.0")
	public <T> T getData(String key) {
		T val = (T) oldData.get(key);
		if (val == null) {
			DataType type = dataTypes.get(key);
			if (type != null) {
				val = type.getDefValue();
			}
		}
		return val;
	}

	@Override
	public DomainFilterPolicy getDomainFilter() {
		return domainFilter;
	}

	/**
	 * This method allow configure DomainFilterPolicy to be applied during packet filtering.
	 *
	 * @param domainFilter name of the DomainFilterPolicy to be applied
	 */
	public void setDomainFilter(DomainFilterPolicy domainFilter) {
		this.domainFilter = domainFilter;
	}

	@Override
	public String[] getDomainFilterDomains() {
		return domainFilterDomains;
	}

	/**
	 * This method allow specify list of domains that will be used for packet filtering when DomainFilteringPolicy is
	 * set to either LIST or BLACKLIST.
	 *
	 * @param domainFilterDomains array of domains to be applied during filtering
	 */
	public void setDomainFilterDomains(String[] domainFilterDomains) {
		this.domainFilterDomains = StringUtilities.internStringArray(domainFilterDomains);

	}

	@Override
	public String getElemName() {
		return VHOST_ELEM;
	}
	public <T extends VHostItemExtension> T getExtension(Class<T> clazz) {
		return (T) extensions.computeIfAbsent(clazz, key -> {
			if (extensionManager == null) {
				return null;
			}
			VHostItemExtension ext = extensionManager.newExtensionInstanceForClass(key);
			if (ext != null) {
				this.initExtension(ext);
			}
			return ext;
		});
	}

	@Override
	public <T extends VHostItemExtension> Set<T> getExtensions() {
		final Collection<VHostItemExtension> values = extensions.values();
		final Set<VHostItemExtension> vHostItemExtensions = new HashSet<>(values);
		return (Set<T>) Collections.unmodifiableSet(vHostItemExtensions);
	}

	public Set<Class<? extends VHostItemExtension>> getExtensionClasses() {
		return new HashSet(extensions.keySet());
	}

	@Override
	public String getKey() {
		return this.vhost == null ? null : this.vhost.getDomain();
	}

	@Override
	public Long getMaxUsersNumber() {
		return maxUsersNumber;
	}

	public void setMaxUsersNumber(long maxUsersNumber) {
		this.maxUsersNumber = maxUsersNumber;
	}

	@Override
	public JID getMessageForward() {
		return messageForward;
	}

	public void setMessageForward(JID messageForward) {
		this.messageForward = messageForward;
	}

	@Override
	public JID getMessageForwardAddress() {
		return messageForward;
	}

	/**
	 * This method allows to access the virtual domain other configuration parameters. This is future feature API and it
	 * is not used right now. It allows to access configuration parameters which are not specified at the time of API
	 * definition.
	 *
	 * @return a <code>String</code> value with domain extra parameters.
	 */
	@Override
	public String getOtherDomainParams() {
		return otherDomainParams;
	}

	/**
	 * This method allows to set extra configuration parameters for the virtual domain. This is future feature API and
	 * it is not used right now. It allows to access configuration parameters which are not specified at the time of API
	 * definition.
	 *
	 * @param otherParams is a <code>String</code> value with domain extra parameters.
	 */
	public void setOtherDomainParams(String otherParams) {
		this.otherDomainParams = otherParams;
	}

	@Override
	public JID getPresenceForward() {
		return presenceForward;
	}

	public void setPresenceForward(JID presenceForward) {
		this.presenceForward = presenceForward;
	}

	@Override
	public JID getPresenceForwardAddress() {
		return presenceForward;
	}

	@Override
	public String getS2sSecret() {
		return s2sSecret;
	}

	public void setS2sSecret(String s2sSecret) {
		this.s2sSecret = s2sSecret;
	}

	@Override
	public Set<String> getTrustedJIDs() {
		return trustedJids;
	}
	
	public JID getVhost() {
		return this.vhost;
	}

	public boolean isAnonymousEnabled() {
		return anonymousEnabled;
	}

	public void setAnonymousEnabled(boolean value) {
		this.anonymousEnabled = value;
	}

	@Deprecated
	@TigaseDeprecated(since = "8.1.0", removeIn = "9.0.0")
	public boolean isData(String key) {
		if (oldData.containsKey(key)) {
			return (Boolean) oldData.get(key);
		} else {
			DataType type = dataTypes.get(key);
			Boolean defValue = (type == null) ? null : (Boolean) type.getDefValue();
			return defValue != null ? defValue : false;
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean value) {
		this.enabled = value;
	}

	public boolean isRegisterEnabled() {
		return registerEnabled;
	}

	public void setRegisterEnabled(boolean value) {
		this.registerEnabled = value;
	}

	public boolean isTlsRequired() {
		return tlsRequired;
	}

	public void setTlsRequired(boolean value) {
		this.tlsRequired = value;
	}

	public void setData(String key, Object value) {
		if (value == null) {
			this.oldData.remove(key);
		} else {
			this.oldData.put(key, value);
		}
	}

	public void parseDataValue(String key, String valueStr) {
		DataType type = dataTypes.get(key);
		if (type == null) {
			throw new RuntimeException("Key " + key + " is not registered");
		}

		if (valueStr == null) {
			this.oldData.remove(key);
		} else {
			char typeId = DataTypes.typesMap.get(type.cls.getName());
			if (valueStr.contains(";")) {
				valueStr = valueStr.replace(';', ',');
			}
			Object value = (valueStr == null || valueStr.isEmpty())
						   ? null
						   : DataTypes.decodeValueType(typeId, valueStr);
			if (type.getCollectionCls() != null && value != null) {
				try {
					Collection collection = type.getCollectionCls().newInstance();
					for (int i = 0; i < Array.getLength(value); i++) {
						collection.add(Array.get(value, i));
					}
					value = collection;
				} catch (InstantiationException | IllegalAccessException ex) {
					throw new IllegalArgumentException(
							"Could not instantiate collection of class: " + type.getCollectionCls().getCanonicalName(),
							ex);
				}
			}

			setData(type.getKey(), value);
		}
	}

	public void setVHost(String vhost) throws TigaseStringprepException {
		if (vhost == null) {
			vhost = "";
		}
		this.vhost = JID.jidInstance(vhost);
	}

	public void setVHost(JID vhost) {
		this.vhost = vhost;
	}

	public String[] getSaslAllowedMechanisms() {
		return saslAllowedMechanisms;
	}

	public void setSaslAllowedMechanisms(String[] saslAllowedMechanisms) {
		this.saslAllowedMechanisms =
				saslAllowedMechanisms == null || saslAllowedMechanisms.length == 0 ? null : saslAllowedMechanisms;
	}
	
	protected void initializeFromDefaults(VHostItemDefaults vhostDefaults) {
		if (comps == null) {
			comps = new String[0];
		}
		if (vhostDefaults.getTrusted() != null) {
			trustedJids = vhostDefaults.getTrusted();
		} else if (trustedJids == null) {
			trustedJids = Collections.EMPTY_SET;
		}
		maxUsersNumber = vhostDefaults.getMaxUsersNumber();
		messageForward = vhostDefaults.getMessageForward();
		presenceForward = vhostDefaults.getPresenceForward();
		tlsRequired = vhostDefaults.isTlsRequired();
		s2sSecret = vhostDefaults.getS2sSecret();
		registerEnabled = vhostDefaults.isRegisterEnabled();
		domainFilter = vhostDefaults.getDomainFilter();
		domainFilterDomains = vhostDefaults.getDomainFilterDomains();
		anonymousEnabled = vhostDefaults.isAnonymousEnabled();

		if (s2sSecret == null) {
			s2sSecret = UUID.randomUUID().toString();
		}
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
			} else if (filled < c2s_ports_allowed.length) {
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


	public static class DataType {

		private final Class cls;
		private final Class collectionCls;
		private final Object defValue;
		private final String key;
		private final String name;
		private final Object[] options;
		private final String[] optionsNames;

		public DataType(String key, String name, Class cls, Class<? extends Collection> collectionCls, Object defValue,
						Object[] options, String[] optionsNames) {
			this.key = key;
			this.name = name;
			this.cls = cls;
			this.collectionCls = collectionCls;
			this.defValue = defValue;
			this.options = options;
			this.optionsNames = optionsNames;

			if (defValue != null && !cls.isAssignableFrom(defValue.getClass())) {
				throw new IllegalArgumentException(
						"default value parameter must be of class " + cls.getCanonicalName());
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

		public DataType(String key, String name, Class cls, Class<? extends Collection> collectionCls, Object defValue,
						Object[] options) {
			this(key, name, cls, collectionCls, defValue, options, null);
		}

		public DataType(String key, String name, Class cls, Class<? extends Collection> collectionCls,
						Object defValue) {
			this(key, name, cls, collectionCls, defValue, null, null);
		}

		public DataType(String key, String name, Class cls, Object defValue, Object[] options) {
			this(key, name, cls, null, defValue, options, null);
		}

		public DataType(String key, String name, Class cls, Object defValue) {
			this(key, name, cls, null, defValue, null, null);
		}

		public <E extends Enum<E>> DataType(String key, String name, Class<? extends Enum<E>> e, E defValue) {

			String[] options = new String[defValue != null
										  ? e.getEnumConstants().length
										  : e.getEnumConstants().length + 1];
			int idx = 0;

			if (defValue == null) {
				options[idx++] = null;
			}
			for (Enum<E> en : e.getEnumConstants()) {
				options[idx++] = en.name();
			}

			this.key = key;
			this.name = name;
			this.cls = String.class;
			this.collectionCls = null;
			this.defValue = (defValue != null ? defValue.name() : null);
			this.options = options;
			this.optionsNames = null;
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

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append(key).append(':');
			sb.append(name).append(" @ ");
			sb.append(cls);
			return sb.toString();
		}
	}

	protected static class VHostItemWrapper implements VHostItem {

		private VHostItem defaultVHost;
		private VHostItem item;
		private VHostItemDefaults vhostDefaults;

		private boolean anonymousEnabled = false;
		private int[] c2sPortsAllowed = null;
		private String[] comps;
		private DomainFilterPolicy domainFilter = DOMAIN_FILTER_POLICY_PROP_DEF;
		private String[] domainFilterDomains = null;
		private Long maxUsersNumber = VHOST_MAX_USERS_PROP_DEF;
		private JID messageForward = JID.jidInstanceNS(VHOST_MESSAGE_FORWARD_PROP_DEF);
		private String otherDomainParams = null;
		private JID presenceForward = JID.jidInstanceNS(VHOST_PRESENCE_FORWARD_PROP_DEF);
		private boolean registerEnabled = VHOST_REGISTER_ENABLED_PROP_DEF;
		private String s2sSecret = S2S_SECRET_PROP_DEF;
		private String[] saslAllowedMechanisms = null;
		private boolean tlsRequired = VHOST_TLS_REQUIRED_PROP_DEF;
		private Set<String> trustedJids = null;

		private Map<Class<? extends VHostItemExtension>, VHostItemExtension> extensions = new ConcurrentHashMap<>();

		private boolean editable = true;

		public VHostItemWrapper() {
		}

		public void setItem(VHostItem item) {
			this.item = item;
		}

		public void setDefaultVHost(VHostItem item) {
			this.defaultVHost = unwrap(item);
			refresh();
		}

		public void setVHostDefaults(VHostItemDefaults vHostDefaults) {
			this.vhostDefaults = vHostDefaults;
		}

		private static VHostItem unwrap(VHostItem item) {
			return (item instanceof VHostItemWrapper) ? ((VHostItemWrapper)item).item : item;
		}

		public void refresh() {
			if (defaultVHost == null || item == null) {
				return;
			}

			if (item.getKey() != null && item.getKey().equals(VHostItem.DEF_VHOST_KEY)) {
				overrideSettingsWithNewDefaults(item);
			} else {
				mergeSettingsWithUpdatedDefaults(item, defaultVHost);
			}
		}

		private void overrideSettingsWithNewDefaults(VHostItem item) {
			defaultVHost = item;
			comps = defaultVHost.getComps();
			c2sPortsAllowed = defaultVHost.getC2SPortsAllowed();
			domainFilter = Optional.ofNullable(defaultVHost.getDomainFilter()).orElse(DOMAIN_FILTER_POLICY_PROP_DEF);
			domainFilterDomains = defaultVHost.getDomainFilterDomains();
			maxUsersNumber = defaultVHost.getMaxUsersNumber();
			messageForward = defaultVHost.getMessageForward();
			otherDomainParams = defaultVHost.getOtherDomainParams();
			presenceForward = defaultVHost.getPresenceForward();
			registerEnabled = defaultVHost.isRegisterEnabled();
			s2sSecret = defaultVHost.getS2sSecret();
			trustedJids = defaultVHost.getTrustedJIDs();
			saslAllowedMechanisms = defaultVHost.getSaslAllowedMechanisms();
			tlsRequired = defaultVHost.isTlsRequired();
			anonymousEnabled = defaultVHost.isAnonymousEnabled();

			extensions.clear();
			extensions = defaultVHost.getExtensionClasses()
					.stream()
					.map(defaultVHost::getExtension)
					.collect(Collectors.toMap(VHostItemExtension::getClass, Function.identity()));
		}

		private void mergeSettingsWithUpdatedDefaults(VHostItem item, VHostItem defaultVHost) {
			comps = Stream.concat(Arrays.stream(item.getComps()), Arrays.stream(defaultVHost.getComps())).toArray(String[]::new);
			c2sPortsAllowed = Optional.ofNullable(item.getC2SPortsAllowed()).orElseGet(defaultVHost::getC2SPortsAllowed);
			domainFilter = Optional.ofNullable(item.getDomainFilter()).orElseGet(defaultVHost::getDomainFilter);
			if (domainFilter.isDomainListRequired()) {
				domainFilterDomains = Optional.ofNullable(item.getDomainFilterDomains()).orElseGet(defaultVHost::getDomainFilterDomains);
			} else {
				domainFilterDomains = null;
			}
			maxUsersNumber = Optional.ofNullable(Optional.ofNullable(item.getMaxUsersNumber()).orElseGet(defaultVHost::getMaxUsersNumber)).orElse(VHOST_MAX_USERS_PROP_DEF);
			messageForward = Optional.ofNullable(item.getMessageForward()).orElseGet(defaultVHost::getMessageForward);
			otherDomainParams = Optional.ofNullable(item.getOtherDomainParams()).orElseGet(defaultVHost::getOtherDomainParams);
			presenceForward = Optional.ofNullable(item.getPresenceForward()).orElseGet(defaultVHost::getPresenceForward);
			registerEnabled = defaultVHost.isRegisterEnabled() && item.isRegisterEnabled();
			s2sSecret = item.getS2sSecret();
			trustedJids = Collections.unmodifiableSet(Stream.concat(item.getTrustedJIDs().stream(), defaultVHost.getTrustedJIDs().stream()).collect(
					Collectors.toSet()));
			saslAllowedMechanisms = Optional.ofNullable(item.getSaslAllowedMechanisms()).orElseGet(defaultVHost::getSaslAllowedMechanisms);
			tlsRequired = defaultVHost.isTlsRequired() || item.isTlsRequired();
			anonymousEnabled = defaultVHost.isAnonymousEnabled() && item.isAnonymousEnabled();

			extensions.clear();
			for (Class<? extends VHostItemExtension> extClass : item.getExtensionClasses()) {
				final VHostItemExtension defaultsExtension = defaultVHost.getExtension(extClass);
				final VHostItemExtension itemExtension = item.getExtension(extClass);
				extensions.put(extClass, itemExtension.mergeWithDefaults(defaultsExtension));
			}
		}

		@Override
		public String[] getComps() {
			return comps;
		}

		@Override
		public int[] getC2SPortsAllowed() {
			return c2sPortsAllowed;
		}

		@Override
		public <T> T getData(String key) {
			return this.item.getData(key);
		}

		@Override
		public DomainFilterPolicy getDomainFilter() {
			return domainFilter;
		}

		@Override
		public String[] getDomainFilterDomains() {
			return domainFilterDomains;
		}

		@Override
		public <T extends VHostItemExtension> T getExtension(Class<T> clazz) {
			return (T) extensions.computeIfAbsent(clazz, cls -> {
				T ext = (T) item.getExtension(cls);
				if (ext != null) {
					ext.mergeWithDefaults(defaultVHost.getExtension(cls));
				}
				return ext;
			});
		}

		@Override
		public <T extends VHostItemExtension> Set<T> getExtensions() {
			final Collection<VHostItemExtension> values = extensions.values();
			final Set<VHostItemExtension> vHostItemExtensions = new HashSet<>(values);
			return (Set<T>)Collections.unmodifiableSet(vHostItemExtensions);
		}

		@Override
		public Set<Class<? extends VHostItemExtension>> getExtensionClasses() {
			return new HashSet<>(extensions.keySet());
		}

		@Override
		public Long getMaxUsersNumber() {
			return maxUsersNumber;
		}

		@Override
		public JID getMessageForward() {
			return messageForward;
		}
		
		@Override
		public String getOtherDomainParams() {
			return otherDomainParams;
		}

		@Override
		public JID getPresenceForward() {
			return presenceForward;
		}
		
		@Override
		public String getS2sSecret() {
			return s2sSecret;
		}

		@Override
		public Set<String> getTrustedJIDs() {
			return trustedJids;
		}

		@Override
		public JID getVhost() {
			return item.getVhost();
		}

		@Override
		public boolean isAnonymousEnabled() {
			return anonymousEnabled;
		}

		@Override
		public boolean isData(String key) {
			return item.isData(key);
		}

		@Override
		public boolean isEnabled() {
			return item.isEnabled();
		}

		@Override
		public boolean isRegisterEnabled() {
			return registerEnabled;
		}

		@Override
		public boolean isTlsRequired() {
			return tlsRequired;
		}
		
		@Override
		public String[] getSaslAllowedMechanisms() {
			return saslAllowedMechanisms;
		}

		@Override
		public void addCommandFields(Packet packet) {
			item.addCommandFields(packet);
			if (vhostDefaults.isTlsRequired()) {
				Element commandEl = packet.getElement().getChild(Command.COMMAND_EL, Command.XMLNS);
				if (commandEl != null) {
					Element x = commandEl.getChild("x", "jabber:x:data");
					if (x != null) {
						List<Element> fields = x.getChildren();
						if (fields != null) {
							for (int i = 0; i < fields.size(); i++) {
								Element field = fields.get(i);
								x.removeChild(field);
								if (TLS_REQUIRED_LABEL.equals(field.getAttributeStaticStr("var"))) {
									field = new Element("field", new String[]{"var", "type"},
														new String[]{"TLS", "fixed"});
									field.addChild(new Element("value", XMLUtils.escape(
											"This installation forces VHost to require TLS. If you need to use unencrypted connections set 'vhost-tls-required' property to 'false' in the installation configuration file")));
									x.addChild(field);
								} else {
									x.addChild(field);
								}
							}
						}
					}
				}
			}
		}

		@Override
		public String[] getAdmins() {
			return item.getAdmins();
		}

		@Override
		public void setAdmins(String[] admins) {
			throw new UnsupportedOperationException("This is unmodifiable instance of VHostItem");
		}

		@Override
		public String getKey() {
			return item.getKey();
		}

		@Override
		public void setKey(String domain) {
			if (!editable) {
				throw new UnsupportedOperationException("This is unmodifiable instance of VHostItem");
			}
			item.setKey(domain);
		}

		@Override
		public String getOwner() {
			return item.getOwner();
		}

		@Override
		public void setOwner(String owner) {
			throw new UnsupportedOperationException("This is unmodifiable instance of VHostItem");
		}

		@Override
		public void initFromCommand(Packet packet) {
			if (!editable) {
				throw new UnsupportedOperationException("This is unmodifiable instance of VHostItem");
			}
			item.initFromCommand(packet);
			refresh();
		}

		@Override
		public void initFromElement(Element elem) {
			if (!editable) {
				throw new UnsupportedOperationException("This is unmodifiable instance of VHostItem");
			}
			item.initFromElement(elem);
			refresh();
		}

		@Override
		public void initFromPropertyString(String propString) {
			if (!editable) {
				throw new UnsupportedOperationException("This is unmodifiable instance of VHostItem");
			}
			item.initFromPropertyString(propString);
			refresh();
		}
		
		@Override
		public boolean isOwner(String id) {
			return item.isOwner(id);
		}

		@Override
		public Element toElement() {
			return item.toElement();
		}

		@Override
		public String toPropertyString() {
			return item.toPropertyString();
		}

		protected void readOnly() {
			editable = false;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("(Wrapped)");
			sb.append(VHostItemImpl.toString(this));
			return sb.toString();
		}
	}
}

