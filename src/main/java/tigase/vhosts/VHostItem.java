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

/**
 * Created: 22 Nov 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class VHostItem {

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
