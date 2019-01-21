/**
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
package tigase.auth.callbacks;

import tigase.xmpp.jid.BareJID;

import javax.security.auth.callback.Callback;

public class ValidateCertificateData
		implements Callback {

	private boolean authorized;

	private String authorizedID;

	private BareJID defaultAuthzid;

	public ValidateCertificateData() {
	}

	public ValidateCertificateData(BareJID jid) {
		setDefaultAuthzid(jid);
	}

	public String getAuthorizedID() {
		return authorizedID;
	}

	public void setAuthorizedID(String authorizedID) {
		this.authorizedID = authorizedID;
	}

	public BareJID getDefaultAuthzid() {
		return defaultAuthzid;
	}

	public void setDefaultAuthzid(BareJID defaultAuthzid) {
		this.defaultAuthzid = defaultAuthzid;
	}

	public boolean isAuthorized() {
		return authorized;
	}

	public void setAuthorized(boolean authorized) {
		this.authorized = authorized;
	}

}
