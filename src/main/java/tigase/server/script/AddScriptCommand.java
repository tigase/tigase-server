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

//~--- non-JDK imports --------------------------------------------------------

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Bindings;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import tigase.server.BasicComponent;
import tigase.server.CmdAcl;
import tigase.server.Command;
import tigase.server.Iq;
import tigase.server.Packet;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Jan 2, 2009 2:29:48 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class AddScriptCommand extends AbstractScriptCommand {
	private static final Logger log = Logger.getLogger(AddScriptCommand.class.getName());

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param cmdId
	 * @param cmdDescr
	 * @param cmdGroup
	 * @param script
	 * @param lang
	 * @param ext
	 * @param binds
	 *
	 * 
	 *
	 * @throws ScriptException
	 */
	@SuppressWarnings({ "unchecked" })
	public Script addAdminScript(String cmdId, String cmdDescr, String cmdGroup, String script, String lang,
			String ext, Bindings binds)
			throws ScriptException {
		Script as = new Script();

		as.init(cmdId, cmdDescr, cmdGroup, script, lang, ext, binds);

		Map<String, CommandIfc> adminCommands = (Map<String, CommandIfc>) binds.get(ADMN_CMDS);

		adminCommands.put(as.getCommandId(), as);

		Map<String, EnumSet<CmdAcl>> commandsACL = (Map<String,
			EnumSet<CmdAcl>>) binds.get(COMMANDS_ACL);
		EnumSet<CmdAcl> acl = commandsACL.get(as.getCommandId());

		if (acl != null) {
			for (CmdAcl cmdAcl : acl) {
				if (cmdAcl != CmdAcl.ADMIN) {
					as.setAdminOnly(false);

					break;
				}
			}
		}

		return as;
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public Bindings getBindings() {
		return null;
	}

	//~--- methods --------------------------------------------------------------

	@Override
	@SuppressWarnings({ "unchecked" })
	public void runCommand(Iq packet, Bindings binds, Queue<Packet> results) {
		String language = Command.getFieldValue(packet, LANGUAGE);
		String commandId = Command.getFieldValue(packet, COMMAND_ID);
		String description = Command.getFieldValue(packet, DESCRIPT);
		String group = Command.getFieldValue(packet, GROUP);
		String[] scriptText = Command.getFieldValues(packet, SCRIPT_TEXT);
		boolean saveToDisk = Command.getCheckBoxFieldValue(packet, SAVE_TO_DISK);

		if (isEmpty(language) || isEmpty(commandId) || isEmpty(description) || (scriptText == null)) {
			results.offer(prepareScriptCommand(packet, binds));
		} else {
			StringBuilder sb = new StringBuilder(1024);

			for (String string : scriptText) {
				if (string != null) {
					sb.append(string).append("\n");
				}
			}

			try {
				String originalGroup = group;
				if (group != null && group.contains("${componentName}")) {
					BasicComponent component = (BasicComponent) binds.get("component");
					if (component != null) {
						group = group.replace("${componentName}", component.getDiscoDescription());
					}
				}			
				Script s = addAdminScript(commandId, description, group, sb.toString(), language, null, binds);
				Packet result = packet.commandResult(Command.DataType.result);

				Command.addTextField(result, "Note", "Script loaded successfuly.");
				results.offer(result);

				if (saveToDisk) {
					saveCommandToDisk(commandId, description, originalGroup, sb, s.getFileExtension(), binds);
				}
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

	private Packet prepareScriptCommand(Iq packet, Bindings binds) {
		Packet result = packet.commandResult(Command.DataType.form);

		Command.addFieldValue(result, DESCRIPT, "Short description");
		Command.addFieldValue(result, COMMAND_ID, "new-command");
		Command.addFieldValue(result, GROUP, "group");

		ScriptEngineManager scriptEngineManager = (ScriptEngineManager) binds.get(SCRI_MANA);
		List<ScriptEngineFactory> scriptFactories = scriptEngineManager.getEngineFactories();

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

		Command.addFieldMultiValue(result, SCRIPT_TEXT, Collections.nCopies(1, ""));
		Command.addCheckBoxField(result, SAVE_TO_DISK, true);

		return result;
	}

	private void saveCommandToDisk(String commandId, String description, String group, StringBuilder sb,
			String fileExtension, Bindings binds)
			throws IOException {
		File fileName = new File((String) binds.get(SCRIPT_COMP_DIR), commandId + "." + fileExtension);

		File parentDirectory = fileName.getParentFile();

		if ( ( parentDirectory != null ) && !parentDirectory.exists() ){
			log.log( Level.CONFIG, "Admin scripts directory is missing: {0}, creating...",
							 parentDirectory );
			try {
				parentDirectory.mkdirs();
			} catch ( Exception e ) {
				log.log( Level.WARNING,
								 "Can't create scripts directory , read-only filesystem: " + parentDirectory, e );
			}
		}

		log.log(Level.INFO, "Saving command: {0} to disk file: {1}", new Object[] { commandId,
				fileName.toString() });

		FileWriter fw = new FileWriter(fileName, false);
		String comment = lineCommentStart.get(fileExtension);

		if (comment == null) {
			comment = "//";
		}

		fw.write(comment + " " + SCRIPT_DESCRIPTION + " " + description + '\n');
		fw.write(comment + " " + SCRIPT_ID + " " + commandId + '\n');
		fw.write(comment + " " + SCRIPT_COMPONENT + " " + binds.get(COMPONENT_NAME) + '\n');
		if (group != null) {
			fw.write(comment + " " + SCRIPT_GROUP + " " + group + '\n');
		}
		fw.write(sb.toString());
		fw.close();
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
