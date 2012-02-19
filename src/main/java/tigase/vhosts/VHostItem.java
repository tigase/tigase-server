/*
 *   Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.vhosts;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.comp.RepositoryItemAbstract;

import tigase.server.Command;
import tigase.server.Packet;

import tigase.util.TigaseStringprepException;

import tigase.xml.Element;

import tigase.xmpp.JID;

//~--- JDK imports ------------------------------------------------------------

import java.util.IllegalFormatException;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

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
public class VHostItem extends RepositoryItemAbstract {
	private static final Logger log = Logger.getLogger(VHostItem.class.getName());

	/**
	 * Element name to for the VHostItem XML storage.
	 */
	public static final String VHOST_ELEM = "vhost";

	/**
	 * Element name for the VHostItem XML child keeping list of the server
	 * component which can handle packets for this domain. In most cases this
	 * element should be empty.
	 */
	public static final String COMPONENTS_ELEM = "comps";

	/**
	 * Element name for the VHostItem XML child keeping all the extra parameters
	 * for the domain. This is a container for future extensions and parameters
	 * which are not defined yet.
	 */
	public static final String OTHER_PARAMS_ELEM = "other";

	/** Field description */
	public static final String OTHER_PARAMS_LABEL = "Other parameters";

	/**
	 * This is an attribute name for storing the VHost name.
	 */
	public static final String HOSTNAME_ATT = "hostname";

	/** Field description */
	public static final String HOSTNAME_LABEL = "Domain name";

	/**
	 * This is an attribute name for storing information whether the VHost is
	 * enabled or disabled.
	 */
	public static final String ENABLED_ATT = "enabled";

	/** Field description */
	public static final String ENABLED_LABEL = "Enabled";

	/**
	 * This is an attribute name for storing information whether anonymous user
	 * can login for this domain.
	 */
	public static final String ANONYMOUS_ENABLED_ATT = "anon";

	/** Field description */
	public static final String ANONYMOUS_ENABLED_LABEL = "Anonymous enabled";

	/**
	 * This is an attribute name for storing information whether user registration
	 * is allowed for this domain.
	 */
	public static final String REGISTER_ENABLED_ATT = "register";

	/** Field description */
	public static final String REGISTER_ENABLED_LABEL = "In-band registration";

	/**
	 * This is an attribute name for storing the maximum number of users for this
	 * virtual domain.
	 */
	public static final String MAX_USERS_NUMBER_ATT = "max-users";

	/** Field description */
	public static final String MAX_USERS_NUMBER_LABEL = "Max users";

	public static final String PRESENCE_FORWARD_ADDRESS_ATT = "pres-forw";
	public static final String PRESENCE_FORWARD_ADDRESS_LABEL = "Presence forward address";
	public static final String MESSAGE_FORWARD_ADDRESS_ATT = "mess-forw";
	public static final String MESSAGE_FORWARD_ADDRESS_LABEL = "Message forward address";

	// ~--- fields ---------------------------------------------------------------

	private String[] comps = null;
	private long maxUsersNumber = 0L;
	private String otherDomainParams = null;
	private VHostItem unmodifiableItem = null;
	private JID vhost = null;
	private boolean registerEnabled = true;
	private boolean enabled = true;
	private boolean anonymousEnabled = true;
	private JID presenceForward = null;
	private JID messageForward = null;

	// ~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 * 
	 */
	public VHostItem() {
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

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 * 
	 * 
	 * @param packet
	 */
	@Override
	public void addCommandFields(Packet packet) {
		Command.addFieldValue(packet, HOSTNAME_LABEL, (vhost != null) ? vhost.getDomain()
				: "");
		Command.addCheckBoxField(packet, ENABLED_LABEL, enabled);
		Command.addCheckBoxField(packet, ANONYMOUS_ENABLED_LABEL, anonymousEnabled);
		Command.addCheckBoxField(packet, REGISTER_ENABLED_LABEL, registerEnabled);
		Command.addFieldValue(packet, MAX_USERS_NUMBER_LABEL, "" + maxUsersNumber);

		Command.addFieldValue(packet, PRESENCE_FORWARD_ADDRESS_LABEL,
				(presenceForward != null ? presenceForward.toString() : ""));
		Command.addFieldValue(packet, MESSAGE_FORWARD_ADDRESS_LABEL,
				(messageForward != null ? messageForward.toString() : ""));

		Command.addFieldValue(packet, OTHER_PARAMS_LABEL,
				(otherDomainParams != null) ? otherDomainParams : "");
		super.addCommandFields(packet);
	}

	// ~--- get methods ----------------------------------------------------------

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

	public JID getPresenceForwardAddress() {
		return presenceForward;
	}

	public JID getMessageForwardAddress() {
		return presenceForward;
	}

	// ~--- methods --------------------------------------------------------------

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
		enabled = Command.getCheckBoxFieldValue(packet, ENABLED_LABEL);
		anonymousEnabled = Command.getCheckBoxFieldValue(packet, ANONYMOUS_ENABLED_LABEL);
		registerEnabled = Command.getCheckBoxFieldValue(packet, REGISTER_ENABLED_LABEL);

		try {
			maxUsersNumber =
					Long.parseLong(Command.getFieldValue(packet, MAX_USERS_NUMBER_LABEL));
		} catch (Exception e) {
			log.warning("Can not parse max users number: "
					+ Command.getFieldValue(packet, MAX_USERS_NUMBER_LABEL));
		}

		tmp = Command.getFieldValue(packet, PRESENCE_FORWARD_ADDRESS_LABEL);
		if (tmp != null && !tmp.trim().isEmpty()) {
			try {
				presenceForward = JID.jidInstance(tmp);
			} catch (TigaseStringprepException ex) {
				presenceForward = null;
				throw new IllegalArgumentException("Incorrect presence forward address: " + tmp,
						ex);
			}
		}

		tmp = Command.getFieldValue(packet, MESSAGE_FORWARD_ADDRESS_LABEL);
		if (tmp != null && !tmp.trim().isEmpty()) {
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
			throw new IllegalArgumentException("Incorrect element name, expected: "
					+ VHOST_ELEM);
		}

		super.initFromElement(elem);
		setVHost(JID.jidInstanceNS(elem.getAttribute(HOSTNAME_ATT)));
		enabled = Boolean.parseBoolean(elem.getAttribute(ENABLED_ATT));
		anonymousEnabled = Boolean.parseBoolean(elem.getAttribute(ANONYMOUS_ENABLED_ATT));
		registerEnabled = Boolean.parseBoolean(elem.getAttribute(REGISTER_ENABLED_ATT));

		try {
			maxUsersNumber = Long.parseLong(elem.getAttribute(MAX_USERS_NUMBER_ATT));
		} catch (Exception e) {
			log.log(Level.WARNING, "Can not parse max users number: {0}",
					elem.getAttribute(MAX_USERS_NUMBER_ATT));
		}

		String tmp = elem.getAttribute(PRESENCE_FORWARD_ADDRESS_ATT);
		if (tmp != null && !tmp.trim().isEmpty()) {
			presenceForward = JID.jidInstanceNS(tmp);
		}

		tmp = elem.getAttribute(MESSAGE_FORWARD_ADDRESS_ATT);
		if (tmp != null && !tmp.trim().isEmpty()) {
			messageForward = JID.jidInstanceNS(tmp);
		}

		String comps_str = elem.getCData("/" + VHOST_ELEM + "/" + COMPONENTS_ELEM);

		if ((comps_str != null) && !comps_str.isEmpty()) {
			comps = comps_str.split(",");
		}

		otherDomainParams = elem.getCData("/" + VHOST_ELEM + "/" + OTHER_PARAMS_ELEM);
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
			throw new IllegalArgumentException("Domain misconfiguration, cannot parse it: "
					+ props[0], ex);
		}
		for (String tmp : props) {
			if (tmp.startsWith("-")) {
				if (tmp.endsWith(ANONYMOUS_ENABLED_ATT)) {
					anonymousEnabled = false;
				}
				if (tmp.endsWith(REGISTER_ENABLED_ATT)) {
					registerEnabled = false;
				}
			}
			if (tmp.startsWith(MAX_USERS_NUMBER_ATT)) {
				String[] mu = tmp.split("=");
				try {
					maxUsersNumber = Long.parseLong(mu[1]);
				} catch (NumberFormatException ex) {
					maxUsersNumber = 0;
					log.warning("Incorrect max users numner for vhost settings, number parsing error: "
							+ tmp);
				}
			}
			if (tmp.startsWith(PRESENCE_FORWARD_ADDRESS_ATT)) {
				String[] mu = tmp.split("=");
				try {
					presenceForward = JID.jidInstance(mu[1]);
				} catch (TigaseStringprepException ex) {
					presenceForward = null;
					log.warning("Incorrect presence forwarding address, address parsing error: "
							+ tmp);
				}
			}
			if (tmp.startsWith(MESSAGE_FORWARD_ADDRESS_ATT)) {
				String[] mu = tmp.split("=");
				try {
					messageForward = JID.jidInstance(mu[1]);
				} catch (TigaseStringprepException ex) {
					messageForward = null;
					log.warning("Incorrect presence forwarding address, address parsing error: "
							+ tmp);
				}
			}
		}
	}

	// ~--- get methods ----------------------------------------------------------

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

	// ~--- set methods ----------------------------------------------------------

	/**
	 * This method allows to enable or disable anonymous logins for this domain.
	 * By default anonymous logins are enabled.
	 * 
	 * @param enabled
	 *          is a <code>boolean</code> value indicating whether anonymous
	 *          logins are allowed for this domain.
	 */
	public void setAnonymousEnabled(boolean enabled) {
		this.anonymousEnabled = enabled;
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
	 * This method allows to enable or disable local domain. If the domain is
	 * disabled packets sent for this domain are not processed normally, instead
	 * the server returns an error to the sender. Domain is enabled by default.
	 * 
	 * @param enabled
	 *          is a <code>boolean</code> value indicating whether the domain is
	 *          enabled or not.
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
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
	 * @return the presenceForward
	 */
	public JID getPresenceForward() {
		return presenceForward;
	}

	/**
	 * @param presenceForward the presenceForward to set
	 */
	public void setPresenceForward(JID presenceForward) {
		this.presenceForward = presenceForward;
	}

	/**
	 * @return the messageForward
	 */
	public JID getMessageForward() {
		return messageForward;
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
	 * This method allows to enable or disable user account registration for this
	 * domain. By default user account registration is enabled.
	 * 
	 * @param enabled
	 *          is a <code>boolean</code> value indicating whether user account
	 *          registration is allowed for this domain or not.
	 */
	public void setRegisterEnabled(boolean enabled) {
		this.registerEnabled = enabled;
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

	// ~--- methods --------------------------------------------------------------

	/**
	 * The method exports the <code>VHostItem</code> object to XML representation.
	 * 
	 * @return an <code>Element</code> object with vhost information.
	 */
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

		elem.addChild(new Element(COMPONENTS_ELEM, comps_str));
		elem.addChild(new Element(OTHER_PARAMS_ELEM, other_params));
		elem.addAttribute(HOSTNAME_ATT, vhost.getDomain());
		elem.addAttribute(ENABLED_ATT, "" + enabled);
		elem.addAttribute(ANONYMOUS_ENABLED_ATT, "" + anonymousEnabled);
		elem.addAttribute(REGISTER_ENABLED_ATT, "" + registerEnabled);
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
		if (maxUsersNumber > 0) {
			sb.append(':').append(MAX_USERS_NUMBER_ATT).append('=').append(maxUsersNumber);
		}
		if (presenceForward != null) {
			sb.append(':').append(PRESENCE_FORWARD_ADDRESS_ATT).append('=')
					.append(presenceForward.toString());
		}
		if (messageForward!= null) {
			sb.append(':').append(MESSAGE_FORWARD_ADDRESS_ATT).append('=')
					.append(messageForward.toString());
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
		return "Domain: " + vhost + ", enabled: " + enabled + ", anonym: " + anonymousEnabled
				+ ", register: " + registerEnabled + ", maxusers: " + maxUsersNumber;
	}

	// ~--- inner classes --------------------------------------------------------

	private class UnmodifiableVHostItem extends VHostItem {

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

		// ~--- methods ------------------------------------------------------------

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

		// ~--- get methods --------------------------------------------------------

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
		 * @return the presenceForward
		 */
		public JID getPresenceForward() {
			return VHostItem.this.presenceForward;
		}

		/**
		 * @param presenceForward the presenceForward to set
		 */
		public void setPresenceForward(JID presenceForward) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		/**
		 * @return the messageForward
		 */
		public JID getMessageForward() {
			return VHostItem.this.messageForward;
		}

		/**
		 * @param messageForward the messageForward to set
		 */
		public void setMessageForward(JID messageForward) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
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

		// ~--- set methods --------------------------------------------------------

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

		// ~--- methods ------------------------------------------------------------

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
	}
}

// ~ Formatted in Sun Code Convention

// ~ Formatted by Jindent --- http://www.jindent.com
