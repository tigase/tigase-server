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
package tigase.server.script;

import tigase.server.Iq;
import tigase.server.Packet;
import tigase.stats.StatisticHolder;

import javax.script.Bindings;
import java.util.Queue;

/**
 * Created: Jan 2, 2009 1:20:16 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface CommandIfc
		extends StatisticHolder {

	public static final String VHOST_MANAGER = "vhostMan";

	public static final String ADMINS_SET = "adminsSet";

	public static final String COMMANDS_ACL = "cmdsAcl";

	public static final String ADMN_CMDS = "adminCommands";

	public static final String USER_REPO = "userRepository";

	public static final String AUTH_REPO = "authRepository";

	public static final String USER_SESS = "userSessions";

	public static final String USER_CONN = "userConnections";

	public static final String ADMN_DISC = "adminDisco";

	public static final String SCRI_MANA = "scriptManager";

	public static final String SCRIPT_BASE_DIR = "scriptBaseDir";

	public static final String SCRIPT_COMP_DIR = "scriptCompDir";

	public static final String COMPONENT_NAME = "componentName";

	public static final String COMPONENT = "component";

	public static final String CONNECTED_NODES = "connectedNodes";

	public static final String CONNECTED_NODES_WITH_LOCAL = "connectedNodesWithLocal";

	public static final String EVENTBUS = "eventBus";

	public static final String SERVICES_MAP = "servicesMap";

	public static final String SCRIPT_DESCRIPTION = "AS:Description:";

	public static final String SCRIPT_ID = "AS:CommandId:";

	public static final String SCRIPT_COMPONENT = "AS:Component:";

	public static final String SCRIPT_CLASS = "AS:ComponentClass:";

	public static final String SCRIPT_GROUP = "AS:Group:";

	public static final String LANGUAGE = "Language";

	public static final String COMMAND_ID = "Command Id";

	public static final String SCRIPT_TEXT = "Script text";

	public static final String SCRIPT_RESULT = "Script result";

	public static final String DESCRIPT = "Description";

	public static final String GROUP = "Group";

	public static final String SAVE_TO_DISK = "Save to disk";

	public static final String REMOVE_FROM_DISK = "Remove from disk";

	public static final String PACKET = "packet";

	public static final String ADD_SCRIPT_CMD = "add-script";

	public static final String DEL_SCRIPT_CMD = "del-script";

	Bindings getBindings();

	String getCommandId();

	String getDescription();

	String getGroup();

	void init(String id, String description, String group);

	boolean isAdminOnly();

	void setAdminOnly(boolean adminOnly);

	void runCommand(Iq packet, Bindings binds, Queue<Packet> results);
}

