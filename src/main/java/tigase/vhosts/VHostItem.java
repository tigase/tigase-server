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
import tigase.xml.Element;

/**
 * Created: 22 Nov 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class VHostItem {

	private static final Logger log =
					Logger.getLogger("tigase.vhosts.VHostItem");

	public static final String VHOST_ELEM = "vhost";
	public static final String COMPONENTS_ELEM = "comps";
	public static final String OTHER_PARAMS_ELEM = "other";
	public static final String HOSTNAME_ATT = "hostname";
	public static final String ENABLED_ATT = "enabled";
	public static final String ANONYMOUS_ENABLED_ATT = "anon";
	public static final String REGISTER_ENABLED_ATT = "register";
	public static final String MAX_USERS_NUMBER_ATT = "max-users";

	private String vhost = null;
	private String[] comps = null;
	private boolean enabled = true;
	private boolean anonymousEnabled = true;
	private boolean registerEnabled = true;
	private long maxUsersNumber = 99999999999L;
	private String otherDomainParams = null;

	public VHostItem(String vhost) {
		this.vhost = vhost;
	}

	public VHostItem(Element elem) {
		vhost = elem.getAttribute(HOSTNAME_ATT);
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

	public Element toXML() {
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

	public String[] getComps() {
		return comps;
	}

	public void setComps(String[] comps) {
		this.comps = comps;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isRegisterEnabled() {
		return registerEnabled;
	}

	public void setRegisterEnabled(boolean enabled) {
		this.registerEnabled = enabled;
	}

	public boolean isAnonymousEnabled() {
		return anonymousEnabled;
	}

	public void setAnonymousEnabled(boolean enabled) {
		this.anonymousEnabled = enabled;
	}

	public long getMaxUsersNumber() {
		return maxUsersNumber;
	}

	public void setMaxUsersNumber(long maxUsersNumber) {
		this.maxUsersNumber = maxUsersNumber;
	}

	public String  getOtherDomainParams() {
		return otherDomainParams;
	}

	public void setOtherDomainParams(String otherParams) {
		this.otherDomainParams = otherParams;
	}

	public String getVhost() {
		return vhost;
	}

}
