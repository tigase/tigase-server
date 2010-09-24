package tigase.server;

//~--- enums ------------------------------------------------------------------

/**
 *
 */
public enum CmdAcl {
	ALL, ADMIN, LOCAL, DOMAIN, JID, OTHER;

	private String aclVal = null;

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param acl
	 *
	 * @return
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
