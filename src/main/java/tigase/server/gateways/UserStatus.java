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
package tigase.server.gateways;

import java.util.LinkedHashMap;
import java.util.Map;



/**
 * Describe class UserStatus here.
 *
 *
 * Created: Tue Nov 13 18:47:10 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class UserStatus {

	private String type = null;
	private String show = null;

	private static final Map<String, String> show_vals =
		new LinkedHashMap<String, String>();

	static {
		show_vals.put("away", "away");
		show_vals.put("be_right_back", "away");
		show_vals.put("busy", "dnd");
		show_vals.put("hide", "xa");
		show_vals.put("idle", "xa");
		show_vals.put("on_the_phone", "dnd");
		show_vals.put("out_to_lunch", "xa");
	}

	/**
	 * Creates a new <code>UserStatus</code> instance.
	 *
	 */
	public UserStatus(String type, String show) {
		this.show = show_vals.get(show);
		this.type = type;
	}

	public String getShow() {
		return show;
	}

	public String getType() {
		return type;
	}

}
