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

package tigase.server.script;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import javax.script.Bindings;
import tigase.disco.ServiceEntity;
import tigase.server.Command;
import tigase.server.Iq;
import tigase.server.Packet;

/**
 * Created: Jan 2, 2009 2:30:41 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class RemoveScriptCommand extends AbstractScriptCommand {

	@Override
	@SuppressWarnings({"unchecked"})
	public void runCommand(Iq packet, Bindings binds, Queue<Packet> results) {
		String commandId = Command.getFieldValue(packet, COMMAND_ID);
		if (isEmpty(commandId)) {
			results.offer(prepareScriptCommand(packet, binds));
		} else {
			Map<String, CommandIfc> adminCommands =
							(Map<String, CommandIfc>) binds.get(ADMN_CMDS);
			adminCommands.remove(commandId);
			ServiceEntity serviceEntity = (ServiceEntity) binds.get(ADMN_DISC);
			ServiceEntity item =
							serviceEntity.findNode("http://jabber.org/protocol/admin#" +
							commandId);
			serviceEntity.removeItems(item);
			Packet result = packet.commandResult(Command.DataType.result);
			Command.addTextField(result, "Note",
							"There is no command script to remove");
			results.offer(result);
		}
	}

	@SuppressWarnings({"unchecked"})
	private Packet prepareScriptCommand(Iq packet, Bindings binds) {
		Packet result = null;
		Map<String, CommandIfc> adminCommands =
						(Map<String, CommandIfc>) binds.get(ADMN_CMDS);
		if (adminCommands.size() > 2) {
			result = packet.commandResult(Command.DataType.form);
			Set<String> ids = new LinkedHashSet<String>(adminCommands.keySet());
			ids.remove(ADD_SCRIPT_CMD);
			ids.remove(DEL_SCRIPT_CMD);
			String[] commandIds = ids.toArray(new String[ids.size()]);
			Command.addFieldValue(result, COMMAND_ID, commandIds[0], "Command Id",
							commandIds, commandIds);
		} else {
			result = packet.commandResult(Command.DataType.result);
			Command.addTextField(result, "Note",
							"There is no command script to remove");
		}
		return result;
	}

	@Override
	public Bindings getBindings() {
		return null;
	}

}
