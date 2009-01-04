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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Queue;
import java.util.logging.Logger;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import tigase.server.Command;
import tigase.server.Packet;

/**
 * Created: Jan 2, 2009 1:21:55 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class AdminScript extends AbstractAdminCommand {

  private static final Logger log =
    Logger.getLogger("tigase.server.xmppsession.AdminScript");

	private String script = null;
	private String language = null;

	public void init(String id, String description, String script, String lang) {
		super.init(id, description);
		this.script = script;
		this.language = lang;
		log.info("Initialized script command, lang: " + this.language +
						", script text: \n" + this.script);
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public void runCommand(Packet packet, Bindings binds, Queue<Packet> results) {
		ScriptContext context = null;
		StringWriter writer = null;
		try {
			ScriptEngineManager scriptEngineManager =
							(ScriptEngineManager) binds.get(SCRI_MANA);
			ScriptEngine scriptEngine = scriptEngineManager.getEngineByName(language);
			Bindings localBinds = scriptEngine.createBindings();
			localBinds.put(PACKET, packet);
			// Workaround for Python which doesn't return values and can overwrite
			// values only if type is correct
			Object res = "";
			localBinds.put("result", res);
			context = scriptEngine.getContext();
			context.setBindings(localBinds, ScriptContext.ENGINE_SCOPE);
			writer = new StringWriter();
			context.setErrorWriter(writer);
			StringReader sr = new StringReader(script);
			res = scriptEngine.eval(sr, context);
			if (res == null) {
				// Yes, Python doesn't return results normally
				// (or I don't know how to do it)
				// Python can either return a Packet as 'packet' or string as 'result'
				res = localBinds.get("result");
				if (res.toString().isEmpty()) {
					res = localBinds.get(PACKET);
				}
			}
			if (res instanceof Packet) {
				results.offer((Packet) res);
			} else {
				if (res instanceof Queue) {
					results.addAll((Queue<Packet>) res);
				} else {
					Packet result = packet.commandResult(Command.DataType.result);
					Command.addTextField(result, "Note", "Script execution result.");
					String[] text = null;
					if (res != null) {
						text = res.toString().split("\n");
					} else {
						text = new String[] {"Script returned no results."};
					}
					Command.addFieldMultiValue(result, SCRIPT_TEXT, Arrays.asList(text));
					results.offer(result);
				}
			}
		} catch (Exception e) {
			Packet result = packet.commandResult(Command.DataType.result);
			Command.addTextField(result, "Note", "Script execution error.");
			StackTraceElement[] ste = e.getStackTrace();
			String[] error = new String[ste.length + 2 +
							(writer != null ? writer.toString().split("\n").length : 0)];
			error[0] = e.getMessage();
			error[1] = e.toString();
			for (int i = 0; i < ste.length; i++) {
				error[i+2] = ste[i].toString();
			}
			if (writer != null) {
				String[] errorMsgs = writer.toString().split("\n");
				for (int i = 0; i < errorMsgs.length; i++) {
					error[i + 2 + ste.length] = errorMsgs[i];
				}
			}
			Command.addFieldMultiValue(result, SCRIPT_TEXT, Arrays.asList(error));
			results.offer(result);
		}
	}

}
