/*  Tigase Jabber/XMPP Server
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

import java.util.logging.Logger;
import tigase.db.RepositoryItem;
import tigase.server.Packet;
import tigase.xml.Element;

/**
 * Objects of this class represent virtual host with all hosts configuration
 * settings. In most cases simple domain name string is not enough to deal with
 * the virtual host. VHost can be enabled/disabled. Can be available to selected
 * server components only and so on. Therefore every time there is a detailed
 * information needed for a vhost this classed must be used.
 *
 * This class has it's own XML representation which can be used for creating
 * an instance of the class or can be exported to the XML form for permanent
 * storage:
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
 * Created: 22 Nov 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class VHostItem implements RepositoryItem {

	private static final Logger log =
					Logger.getLogger("tigase.vhosts.VHostItem");

	/**
	 * Element name to for the VHostItem XML storage.
	 */
	public static final String VHOST_ELEM = "vhost";
	/**
	 * Element name for the VHostItem XML child keeping list of the server component
	 * which can handle packets for this domain. In most cases this element should
	 * be empty.
	 */
	public static final String COMPONENTS_ELEM = "comps";
	/**
	 * Element name for the VHostItem XML child keeping all the extra parameters
	 * for the domain. This is a container for future extensions and parameters
	 * which are not defined yet.
	 */
	public static final String OTHER_PARAMS_ELEM = "other";
	/**
	 * This is an attribute name for storing the VHost name.
	 */
	public static final String HOSTNAME_ATT = "hostname";
	/**
	 * This is an attribute name for storing information whether the VHost is
	 * enabled or disabled.
	 */
	public static final String ENABLED_ATT = "enabled";
	/**
	 * This is an attribute name for storing information whether anonymous
	 * user can login for this domain.
	 */
	public static final String ANONYMOUS_ENABLED_ATT = "anon";
	/**
	 * This is an attribute name for storing information whether user registration
	 * is allowed for this domain.
	 */
	public static final String REGISTER_ENABLED_ATT = "register";
	/**
	 * This is an attribute name for storing the maximum number of users for
	 * this virtual domain.
	 */
	public static final String MAX_USERS_NUMBER_ATT = "max-users";

	private String vhost = null;
	private String[] comps = null;
	private boolean enabled = true;
	private boolean anonymousEnabled = true;
	private boolean registerEnabled = true;
	private long maxUsersNumber = 0L;
	private String otherDomainParams = null;
	private VHostItem unmodifiableItem = null;

	public VHostItem() {	}

	/**
	 * The constructor creates the <code>VHostItem</code> instance for a given
	 * domain name with default values for all other parameters. By the default
	 * all domain parameters are set to true.
	 * @param vhost is a <code>String</code> value with a domain name.
	 */
	public VHostItem(String vhost) {
		setVHost(vhost);
	}

	/**
	 * The constructor creates the <code>VHostItem</code> instance from a given
	 * XML element. Please refer to the class documentation for more details of
	 * the XML element.
	 * @param elem is an <code>Element</code> object with virtual domain settings.
	 */
	public VHostItem(Element elem) {
		setVHost(elem.getAttribute(HOSTNAME_ATT));
		enabled = Boolean.parseBoolean(elem.getAttribute(ENABLED_ATT));
		anonymousEnabled =
						Boolean.parseBoolean(elem.getAttribute(ANONYMOUS_ENABLED_ATT));
		registerEnabled =
						Boolean.parseBoolean(elem.getAttribute(REGISTER_ENABLED_ATT));
		try {
			maxUsersNumber = Long.parseLong(elem.getAttribute(MAX_USERS_NUMBER_ATT));
		} catch (Exception e) {
			log.warning("Can not parse max users number: " +
							elem.getAttribute(MAX_USERS_NUMBER_ATT));
		}
		String comps_str = elem.getCData("/" + VHOST_ELEM + "/" + COMPONENTS_ELEM);
		if (comps_str != null && !comps_str.isEmpty()) {
			comps = comps_str.split(",");
		}
		otherDomainParams =
						elem.getCData("/" + VHOST_ELEM + "/" + OTHER_PARAMS_ELEM);
	}

	public VHostItem getUnmodifiableVHostItem() {
		if (unmodifiableItem == null) {
			unmodifiableItem = new UnmodifiableVHostItem();
		}
		return unmodifiableItem;
	}

	/**
	 * The method exports the <code>VHostItem</code> object to XML representation.
	 * @return an <code>Element</code> object with vhost information.
	 */
	@Override
	public Element toElement() {
		String comps_str = "";
		if (comps != null && comps.length > 0) {
			for (String comp : comps) {
				if (!comps_str.isEmpty()) {
					comps_str += ",";
				}
				comps_str += comp;
			}
		}
		String other_params = otherDomainParams != null ? otherDomainParams : "";
		Element elem = new Element(VHOST_ELEM, new Element[] {
							new Element(COMPONENTS_ELEM, comps_str),
							new Element(OTHER_PARAMS_ELEM, other_params)
						},
						new String[]{HOSTNAME_ATT, ENABLED_ATT, ANONYMOUS_ENABLED_ATT,
							REGISTER_ENABLED_ATT, MAX_USERS_NUMBER_ATT},
						new String[]{vhost, "" + enabled, "" + anonymousEnabled,
							"" + registerEnabled, "" + maxUsersNumber});
		return elem;
	}

	/**
	 * Returns an array with the server components names which should process
	 * packets sent to this domain or <code>null</code> (default) if there is
	 * no specific component assigned to this domain.
	 * @return a <code>String[]</code> object with server component names.
	 */
	public String[] getComps() {
		return comps;
	}

	/**
	 * Sets an array with the server component names by which packets to this
	 * domain can be processed. Every local domain will be handled by
	 * <code>VHostListener</code> which returns <code>true</code> for
	 * <code>handlesLocalDomains()</code> method call and by all components
	 * set via this method.
	 * @param comps is an <code>String[]</code> array with server component names.
	 */
	public void setComps(String[] comps) {
		this.comps = comps;
	}

	/**
	 * Checks whether this domain is set as enabled or not. This is domain own
	 * configuration parameter which allows to temporarly disable domain so packets
	 * for this domain are not processed normally. Instead the server returns
	 * an error.
	 * @return a <code>boolean</code> value <code>true</code> if the domain is
	 * enabled and <code>false</code> if the domain is disabled.
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * This method allows to enable or disable local domain. If the domain is
	 * disabled packets sent for this domain are not processed normally, instead
	 * the server returns an error to the sender.
	 * Domain is enabled by default.
	 * @param enabled is a <code>boolean</code> value indicating whether the
	 * domain is enabled or not.
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * The method checks whether user registration is enabled for this domain
	 * or not. This is the domain own configuration parameter which allows to
	 * disable user accounts registration via XMPP per domain basis.
	 * @return a <code>boolean</code> value indicating whether user account
	 * registration is allowed for this domain.
	 */
	public boolean isRegisterEnabled() {
		return registerEnabled;
	}

	/**
	 * This method allows to enable or disable user account registration for this
	 * domain. By default user account registration is enabled.
	 * @param enabled is a <code>boolean</code> value indicating whether user
	 * account registration is allowed for this domain or not.
	 */
	public void setRegisterEnabled(boolean enabled) {
		this.registerEnabled = enabled;
	}

	/**
	 * This method checks whether anonymous login is enabled for this domain.
	 * This is the domain own configuration parameter which allows to disable
	 * anonymous logins on per domain basis.
	 * @return a <code>boolean</code> value indicating whether anonymous logins
	 * are allowed for this domain.
	 */
	public boolean isAnonymousEnabled() {
		return anonymousEnabled;
	}

	/**
	 * This method allows to enable or disable anonymous logins for this domain.
	 * By default anonymous logins are enabled.
	 * @param enabled is a <code>boolean</code> value indicating whether anonymous
	 * logins are allowed for this domain.
	 */
	public void setAnonymousEnabled(boolean enabled) {
		this.anonymousEnabled = enabled;
	}

	/**
	 * This method returns the maximum number of user accounts allowed for
	 * this domain. This parameter is to allow for limiting number of users
	 * on per domain basis.
	 * @return a <code>long</code> value indicating the maximum number of
	 * user accounts allowed for this domain.
	 */
	public long getMaxUsersNumber() {
		return maxUsersNumber;
	}

	/**
	 * This method allows to set the maximum number of user accounts allowed for
	 * this domain. The default value of this parameter is: <code>0L</code>.
	 * @param maxUsersNumber is a <code>long</code> value specifying the maximum
	 * number of user accounts allowed for this domain.
	 */
	public void setMaxUsersNumber(long maxUsersNumber) {
		this.maxUsersNumber = maxUsersNumber;
	}

	/**
	 * This method allows to access the virtual domain other configuration
	 * parameters. This is future feature API and it is not used right now.
	 * It allows to access configuration parameters which are not specified
	 * at the time of API definition.
	 * @return a <code>String</code> value with domain extra parameters.
	 */
	public String  getOtherDomainParams() {
		return otherDomainParams;
	}

	/**
	 * This method allows to set extra configuration parameters for the virtual
	 * domain.  This is future feature API and it is not used right now.
	 * It allows to access configuration parameters which are not specified
	 * at the time of API definition.
	 * @param otherParams is a <code>String</code> value with domain extra
	 * parameters.
	 */
	public void setOtherDomainParams(String otherParams) {
		this.otherDomainParams = otherParams;
	}

	/**
	 * This method return a virtual host name as a <code>String</code> value.
	 * @return a <code>String</code> value with the virtual domain name.
	 */
	public String getVhost() {
		return this.vhost;
	}

	public void setVHost(String vhost) {
		this.vhost = vhost.toLowerCase();
	}

	@Override
	public void initFromPropertyString(String propString) {
		setVHost(propString);
	}

	@Override
	public String toPropertyString() {
		return this.vhost;
	}

	@Override
	public void initFromElement(Element elem) {
		if (elem.getName() != VHOST_ELEM) {
			throw new IllegalArgumentException("Incorrect element name, expected: " +
					VHOST_ELEM);
		}
		setVHost(elem.getAttribute(HOSTNAME_ATT));
		enabled = Boolean.parseBoolean(elem.getAttribute(ENABLED_ATT));
		anonymousEnabled =
						Boolean.parseBoolean(elem.getAttribute(ANONYMOUS_ENABLED_ATT));
		registerEnabled =
						Boolean.parseBoolean(elem.getAttribute(REGISTER_ENABLED_ATT));
		try {
			maxUsersNumber = Long.parseLong(elem.getAttribute(MAX_USERS_NUMBER_ATT));
		} catch (Exception e) {
			log.warning("Can not parse max users number: " +
							elem.getAttribute(MAX_USERS_NUMBER_ATT));
		}
		String comps_str = elem.getCData("/" + VHOST_ELEM + "/" + COMPONENTS_ELEM);
		if (comps_str != null && !comps_str.isEmpty()) {
			comps = comps_str.split(",");
		}
		otherDomainParams =
						elem.getCData("/" + VHOST_ELEM + "/" + OTHER_PARAMS_ELEM);
	}

	@Override
	public String getKey() {
		return this.vhost;
	}

	@Override
	public void addCommandFields(Packet packet) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void initFromCommand(Packet packet) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	private class UnmodifiableVHostItem extends VHostItem {

		@Override
		public VHostItem getUnmodifiableVHostItem() {
			return this;
		}

		/**
	 * The method exports the <code>VHostItem</code> object to XML representation.
	 * @return an <code>Element</code> object with vhost information.
	 */
		@Override
		public Element toElement() {
			return VHostItem.this.toElement();
		}

		/**
	 * Returns an array with the server components names which should process
	 * packets sent to this domain or <code>null</code> (default) if there is
	 * no specific component assigned to this domain.
	 * @return a <code>String[]</code> object with server component names.
	 */
		@Override
		public String[] getComps() {
			return VHostItem.this.getComps();
		}

		/**
	 * Sets an array with the server component names by which packets to this
	 * domain can be processed. Every local domain will be handled by
	 * <code>VHostListener</code> which returns <code>true</code> for
	 * <code>handlesLocalDomains()</code> method call and by all components
	 * set via this method.
	 * @param comps is an <code>String[]</code> array with server component names.
	 */
		@Override
		public void setComps(String[] comps) {
			throw new UnsupportedOperationException("This is unmodifiable instance of VHostItem");
		}

		/**
	 * Checks whether this domain is set as enabled or not. This is domain own
	 * configuration parameter which allows to temporarly disable domain so packets
	 * for this domain are not processed normally. Instead the server returns
	 * an error.
	 * @return a <code>boolean</code> value <code>true</code> if the domain is
	 * enabled and <code>false</code> if the domain is disabled.
	 */
		@Override
		public boolean isEnabled() {
			return VHostItem.this.isEnabled();
		}

		/**
	 * This method allows to enable or disable local domain. If the domain is
	 * disabled packets sent for this domain are not processed normally, instead
	 * the server returns an error to the sender.
	 * Domain is enabled by default.
	 * @param enabled is a <code>boolean</code> value indicating whether the
	 * domain is enabled or not.
	 */
		@Override
		public void setEnabled(boolean enabled) {
			throw new UnsupportedOperationException("This is unmodifiable instance of VHostItem");
		}

		/**
	 * The method checks whether user registration is enabled for this domain
	 * or not. This is the domain own configuration parameter which allows to
	 * disable user accounts registration via XMPP per domain basis.
	 * @return a <code>boolean</code> value indicating whether user account
	 * registration is allowed for this domain.
	 */
		@Override
		public boolean isRegisterEnabled() {
			return VHostItem.this.isRegisterEnabled();
		}

		/**
	 * This method allows to enable or disable user account registration for this
	 * domain. By default user account registration is enabled.
	 * @param enabled is a <code>boolean</code> value indicating whether user
	 * account registration is allowed for this domain or not.
	 */
		@Override
		public void setRegisterEnabled(boolean enabled) {
			throw new UnsupportedOperationException("This is unmodifiable instance of VHostItem");
		}

		/**
	 * This method checks whether anonymous login is enabled for this domain.
	 * This is the domain own configuration parameter which allows to disable
	 * anonymous logins on per domain basis.
	 * @return a <code>boolean</code> value indicating whether anonymous logins
	 * are allowed for this domain.
	 */
		@Override
		public boolean isAnonymousEnabled() {
			return VHostItem.this.isAnonymousEnabled();
		}

		/**
	 * This method allows to enable or disable anonymous logins for this domain.
	 * By default anonymous logins are enabled.
	 * @param enabled is a <code>boolean</code> value indicating whether anonymous
	 * logins are allowed for this domain.
	 */
		@Override
		public void setAnonymousEnabled(boolean enabled) {
			throw new UnsupportedOperationException("This is unmodifiable instance of VHostItem");
		}

		/**
	 * This method returns the maximum number of user accounts allowed for
	 * this domain. This parameter is to allow for limiting number of users
	 * on per domain basis.
	 * @return a <code>long</code> value indicating the maximum number of
	 * user accounts allowed for this domain.
	 */
		@Override
		public long getMaxUsersNumber() {
			return VHostItem.this.getMaxUsersNumber();
		}

		/**
	 * This method allows to set the maximum number of user accounts allowed for
	 * this domain. The default value of this parameter is: <code>0L</code>.
	 * @param maxUsersNumber is a <code>long</code> value specifying the maximum
	 * number of user accounts allowed for this domain.
	 */
		@Override
		public void setMaxUsersNumber(long maxUsersNumber) {
			throw new UnsupportedOperationException("This is unmodifiable instance of VHostItem");
		}

		/**
	 * This method allows to access the virtual domain other configuration
	 * parameters. This is future feature API and it is not used right now.
	 * It allows to access configuration parameters which are not specified
	 * at the time of API definition.
	 * @return a <code>String</code> value with domain extra parameters.
	 */
		@Override
		public String getOtherDomainParams() {
			return VHostItem.this.getOtherDomainParams();
		}

		/**
	 * This method allows to set extra configuration parameters for the virtual
	 * domain.  This is future feature API and it is not used right now.
	 * It allows to access configuration parameters which are not specified
	 * at the time of API definition.
	 * @param otherParams is a <code>String</code> value with domain extra
	 * parameters.
	 */
		@Override
		public void setOtherDomainParams(String otherParams) {
			throw new UnsupportedOperationException("This is unmodifiable instance of VHostItem");
		}

		/**
	 * This method return a virtual host name as a <code>String</code> value.
	 * @return a <code>String</code> value with the virtual domain name.
	 */
		@Override
		public String getVhost() {
			return VHostItem.this.getVhost();
		}

		@Override
		public void initFromPropertyString(String propString) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

		@Override
		public void initFromElement(Element elem) {
			throw new UnsupportedOperationException(
					"This is unmodifiable instance of VHostItem");
		}

	}

}
