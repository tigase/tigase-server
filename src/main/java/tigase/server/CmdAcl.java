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
package tigase.server;

import tigase.xmpp.jid.BareJID;

import java.util.Objects;

public class CmdAcl {

	public static final CmdAcl ADMIN = new CmdAcl(Type.ADMIN.name());

	public enum Type {
		/**
		 * Everybody can execute the command, even users from a different servers.
		 */
		ALL,
		/**
		 * Only local server administrators can execute command.
		 */
		ADMIN,
		/**
		 * Only users who have accounts on this local server can execute the command.
		 */
		LOCAL,
		/**
		 * Only users who have an account within the given domain can execute the command.
		 */
		DOMAIN,
		/**
		 * Only user who is an admin of the given domain can execute the command.
		 */
		DOMAIN_ADMIN,
		/**
		 * Only user who is an owner of the given domain can execute the command.
		 */
		DOMAIN_OWNER,
		/**
		 * Comma separated list of JIDs of users who can execute the command.
		 */
		JID,
		/**
		 * No one is allowed to execute the command, even server administrators!
		 */
		NONE;
	}
	private final BareJID jid;
	private final Type type;

	public CmdAcl(String value) {
		switch (value) {
			case "ALL":
				type = Type.ALL;
				jid = null;
				break;
			case "ADMIN":
				type = Type.ADMIN;
				jid = null;
				break;
			case "LOCAL":
				type = Type.LOCAL;
				jid = null;
				break;
			case "DOMAIN_OWNER":
				type = Type.DOMAIN_OWNER;
				jid = null;
				break;
			case "DOMAIN_ADMIN":
				type = Type.DOMAIN_ADMIN;
				jid = null;
				break;
			case "NONE":
				type = Type.NONE;
				jid = null;
				break;
			default:
				jid = BareJID.bareJIDInstanceNS(value);
				type = jid == null ? Type.NONE : (jid.getLocalpart() == null ? Type.DOMAIN : Type.JID);
				break;
		}
	}

	public Type getType() {
		return type;
	}

	public boolean isJIDAllowed(BareJID jid) {
		return this.jid.equals(jid);
	}

	public boolean isDomainAllowed(String domain) {
		return domain.equals(domain);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CmdAcl) {
			CmdAcl o = (CmdAcl) obj;
			return type == o.type && (jid == null || jid.equals(jid));
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, jid);
	}

	@Override
	public String toString() {
		switch (type) {
			case ALL:
			case ADMIN:
			case DOMAIN_OWNER:
			case DOMAIN_ADMIN:
			case LOCAL:
				return type.name();
			default:
				return jid.toString();
		}
	}

}

