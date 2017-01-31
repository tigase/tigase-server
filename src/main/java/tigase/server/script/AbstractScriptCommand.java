/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.server.script;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashMap;
import java.util.Map;
import tigase.stats.StatisticHolderImpl;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Jan 2, 2009 2:32:17 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class AbstractScriptCommand extends StatisticHolderImpl implements CommandIfc {

	/** Field description */
	public static final Map<String, String> lineCommentStart = new LinkedHashMap<String, String>(20);

	//~--- static initializers --------------------------------------------------

	static {
		lineCommentStart.put("groovy", "//");
		lineCommentStart.put("scala", "//");
		lineCommentStart.put("python", "#");
		lineCommentStart.put("py", "#");
		lineCommentStart.put("js", "//");
		lineCommentStart.put("ruby", "#");
		lineCommentStart.put("rb", "#");
		lineCommentStart.put("perl", "#");
		lineCommentStart.put("pl", "#");
		lineCommentStart.put("awk", "//");
		lineCommentStart.put("lisp", ";");
		lineCommentStart.put("el", ";");
		lineCommentStart.put("cl", ";");
		lineCommentStart.put("gc1", ";");
		lineCommentStart.put("gc3", ";");
		lineCommentStart.put("java", "//");
	}

	//~--- fields ---------------------------------------------------------------

	private String commandId = null;
	private String description = null;
	private String group = null;
	private boolean adminOnly = true;

	//~--- get methods ----------------------------------------------------------

	@Override
	public String getCommandId() {
		return this.commandId;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public String getGroup() {
		return this.group;
	}
	
	//~--- methods --------------------------------------------------------------

	@Override
	public void init(String id, String description, String group) {
		this.commandId = id;
		this.description = description;
		this.group = group;

		setStatisticsPrefix("adhoc-command/" + id);
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public boolean isAdminOnly() {
		return adminOnly;
	}

	//~--- set methods ----------------------------------------------------------

	@Override
	public void setAdminOnly(boolean adminOnly) {
		this.adminOnly = adminOnly;
	}

	//~--- get methods ----------------------------------------------------------

	protected boolean isEmpty(String val) {
		return (val == null) || val.isEmpty();
	}	
}
