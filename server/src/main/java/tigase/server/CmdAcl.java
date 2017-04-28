package tigase.server;

//~--- enums ------------------------------------------------------------------

import tigase.xmpp.BareJID;

import java.util.Objects;

/**
 *
 */
public class CmdAcl {

	public static final CmdAcl ADMIN = new CmdAcl(Type.ADMIN.name());

	private final Type type;
	private final BareJID jid;

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
			case LOCAL:
				return type.name();
			default:
				return jid.toString();
		}
	}

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
		 * Comma separated list of JIDs of users who can execute the command.
		 */
		JID,
		/**
		 * No one is allowed to execute the command, even server administrators!
		 */
		NONE;
	}

}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
