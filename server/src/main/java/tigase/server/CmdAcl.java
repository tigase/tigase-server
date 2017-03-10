package tigase.server;

//~--- enums ------------------------------------------------------------------

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
	 */
	public static CmdAcl valueof(String acl) {
		CmdAcl result = null;
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

	protected String getAclVal() {
		return aclVal;
	}

	//~--- set methods ----------------------------------------------------------

	protected void setAclVal(String aclVal) {
		this.aclVal = aclVal;
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
