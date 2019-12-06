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
import tigase.db.comp.RepositoryItem;
import tigase.vhosts.filter.DomainFilterPolicy;
import tigase.xmpp.jid.JID;

import java.util.Set;

public interface VHostItem
		extends RepositoryItem, Comparable<VHostItem> {

	static final String DEF_VHOST_KEY = "default";

	default int compareTo(VHostItem o) {
		return getKey().compareTo(o.getKey());
	}
	
	String[] getComps();

	int[] getC2SPortsAllowed();

	@Deprecated
	@TigaseDeprecated(since = "8.1.0", removeIn = "9.0.0")
	<T> T getData(String key);

	DomainFilterPolicy getDomainFilter();

	String[] getDomainFilterDomains();

	<T extends VHostItemExtension> T getExtension(Class<T> clazz);
	Set<Class<? extends VHostItemExtension>> getExtensionClasses();

	Long getMaxUsersNumber();

	JID getMessageForward();
	
	default JID getMessageForwardAddress() {
		return getMessageForward();
	}

	String getOtherDomainParams();

	JID getPresenceForward();

	default JID getPresenceForwardAddress() {
		return getPresenceForward();
	}

	String getS2sSecret();

	Set<String> getTrustedJIDs();

	JID getVhost();

	void setKey(String domain);

	@Override
	default boolean isAdmin(String id) {
		if (getAdmins() == null) {
			return false;
		}
		for (String admin : getAdmins()) {
			if (admin.equals(id)) {
				return true;
			}
		}

		return false;
	}

	boolean isAnonymousEnabled();

	@Deprecated
	@TigaseDeprecated(since = "8.1.0", removeIn = "9.0.0")
	boolean isData(String key);

	boolean isEnabled();

	boolean isRegisterEnabled();

	boolean isTlsRequired();

	default boolean isTrustedJID(JID jid) {
		Set<String> trustedJids = getTrustedJIDs();
		if (trustedJids == null) {
			return false;
		}

		return trustedJids.contains(jid.toString()) || trustedJids.contains(jid.getBareJID().toString()) ||
				trustedJids.contains(jid.getDomain());
	}

	String[] getSaslAllowedMechanisms();
}
