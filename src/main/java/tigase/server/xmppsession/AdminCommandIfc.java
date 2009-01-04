/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

package tigase.server.xmppsession;

import java.util.Queue;
import javax.script.Bindings;
import tigase.server.Packet;

/**
 * Created: Jan 2, 2009 1:20:16 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface AdminCommandIfc {

	public static final String ADMN_CMDS = "adminCommands";
	public static final String USER_REPO = "userRepository";
	public static final String AUTH_REPO = "authRepository";
	public static final String USER_SESS = "userSessions";
	public static final String USER_CONN = "userConnections";
	public static final String ADMN_DISC = "adminDisco";
	public static final String SCRI_MANA = "scriptManager";

	public static final String LANGUAGE = "Language";
	public static final String COMMAND_ID = "Command Id";
	public static final String SCRIPT_TEXT = "Script text";
	public static final String DESCRIPT = "Description";
	public static final String PACKET = "packet";

	public static final String ADD_SCRIPT_CMD = "add-script";
	public static final String DEL_SCRIPT_CMD = "del-script";

	void init(String id, String description);
	
	String getCommandId();

	String getDescription();

	void runCommand(Packet packet, Bindings binds, Queue<Packet> results);

}
