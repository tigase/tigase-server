/*
 * CmdAcl.java
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



package tigase.server;

/**
 *
 */
public enum CmdAcl {

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
	 * This is the default. If no access control is provided than by default a list
	 * of JIDs is assumed.
	 */
	OTHER;

	private String aclVal = null;

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param acl
	 *
	 *
	 *
	 * @return a value of <code>CmdAcl</code>
	 */
	public static CmdAcl valueof(String acl) {
		CmdAcl   result = null;
		String[] aclArr = acl.split(":");

		try {
			result = valueOf(aclArr[0]);
			if (aclArr.length > 1) {
				result.setAclVal(aclArr[1]);
			}
		} catch (Exception e) {
			result = OTHER;
			result.setAclVal(acl);
		}

		return result;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	protected String getAclVal() {
		return aclVal;
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param aclVal is a <code>String</code>
	 */
	protected void setAclVal(String aclVal) {
		this.aclVal = aclVal;
	}
}


//~ Formatted in Tigase Code Convention on 13/08/28
