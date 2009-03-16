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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Bindings;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import tigase.disco.ServiceEntity;
import tigase.disco.ServiceIdentity;
import tigase.disco.XMPPService;
import tigase.server.Command;
import tigase.server.Packet;

/**
 * Created: Jan 2, 2009 2:29:48 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class AddScriptCommand extends AbstractAdminCommand {

  private static final Logger log =
    Logger.getLogger(AddScriptCommand.class.getName());

	@Override
	@SuppressWarnings({"unchecked"})
	public void runCommand(Packet packet, Bindings binds, Queue<Packet> results) {
		String language = Command.getFieldValue(packet, LANGUAGE);
		String commandId = Command.getFieldValue(packet, COMMAND_ID);
		String description = Command.getFieldValue(packet, DESCRIPT);
		String[] scriptText = Command.getFieldValues(packet, SCRIPT_TEXT);
		if (isEmpty(language) || isEmpty(commandId) || isEmpty(description) ||
						scriptText == null) {
			results.offer(prepareScriptCommand(packet, binds));
		} else {
			StringBuilder sb = new StringBuilder();
			for (String string : scriptText) {
				if (string != null) {
					sb.append(string + "\n");
				}
			}
			try {
				addAdminScript(commandId, description, sb.toString(), language,
								null, binds);
				Packet result = packet.commandResult(Command.DataType.result);
				Command.addTextField(result, "Note", "Script loaded successfuly.");
				results.offer(result);
			} catch (Exception e) {
				log.log(Level.WARNING, "Can't initialize script: ", e);
				Packet result = packet.commandResult(Command.DataType.result);
				Command.addTextField(result, "Note", "Script initialization error.");
				StackTraceElement[] ste = e.getStackTrace();
				String[] error = new String[ste.length + 2];
				error[0] = e.getMessage();
				error[1] = e.toString();
				for (int i = 0; i < ste.length; i++) {
					error[i + 2] = ste[i].toString();
				}
				Command.addTextField(result, "Error message", e.getMessage());
				Command.addFieldMultiValue(result, "Debug info", Arrays.asList(error));
				results.offer(result);
			}
		}
	}

	@SuppressWarnings({"unchecked"})
	public boolean addAdminScript(String cmdId, String cmdDescr, String script,
					String lang, String ext, Bindings binds) throws ScriptException {
		AdminScript as = new AdminScript();
		as.init(cmdId, cmdDescr, script, lang, ext, binds);
		Map<String, AdminCommandIfc> adminCommands =
						(Map<String, AdminCommandIfc>) binds.get(ADMN_CMDS);
		adminCommands.put(as.getCommandId(), as);
		ServiceEntity serviceEntity = (ServiceEntity) binds.get(ADMN_DISC);
		ServiceEntity item = new ServiceEntity(as.getCommandId(),
						"http://jabber.org/protocol/admin#" + as.getCommandId(),
						cmdDescr);
		item.addIdentities(
						new ServiceIdentity("component", "generic", cmdDescr),
						new ServiceIdentity("automation", "command-node", cmdDescr));
		item.addFeatures(XMPPService.CMD_FEATURES);
		serviceEntity.addItems(item);
		return true;
	}

	private Packet prepareScriptCommand(Packet packet, Bindings binds) {
		Packet result = packet.commandResult(Command.DataType.form);
		Command.addFieldValue(result, DESCRIPT, "Short description");
		Command.addFieldValue(result, COMMAND_ID, "new-command");
		ScriptEngineManager scriptEngineManager =
						(ScriptEngineManager)binds.get(SCRI_MANA);
		List<ScriptEngineFactory> scriptFactories =
						scriptEngineManager.getEngineFactories();
		if (scriptFactories != null) {
			String[] langs = new String[scriptFactories.size()];
			int idx = 0;
			String def = null;
			for (ScriptEngineFactory scriptEngineFactory : scriptFactories) {
				langs[idx++] = scriptEngineFactory.getLanguageName();
				if (scriptEngineFactory.getLanguageName().equals("groovy")) {
					def = "groovy";
				}
			}
			if (def == null) {
				def = langs[0];
			}
			Command.addFieldValue(result, LANGUAGE, def, LANGUAGE, langs, langs);
		}
		Command.addFieldMultiValue(result, SCRIPT_TEXT,
						Collections.nCopies(1, ""));
		return result;
	}

}
