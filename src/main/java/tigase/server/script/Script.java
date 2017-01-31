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

import tigase.server.Command;
import tigase.server.Iq;
import tigase.server.Packet;

//~--- JDK imports ------------------------------------------------------------

import java.io.StringWriter;

import java.util.Arrays;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Jan 2, 2009 1:21:55 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Script extends AbstractScriptCommand {
	private static final Logger log = Logger.getLogger(Script.class.getName());

	//~--- fields ---------------------------------------------------------------

	private CompiledScript compiledScript = null;
	private String ext = null;
	private String language = null;
	private String script = null;
	private ScriptEngine scriptEngine = null;

	//~--- get methods ----------------------------------------------------------

	@Override
	public Bindings getBindings() {
		return scriptEngine.createBindings();
	}

	public String getFileExtension() {
		return ext;
	}

	public String getLanguageName() {
		return language;
	}
	
	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param id
	 * @param description
	 * @param group
	 * @param script
	 * @param lang
	 * @param ext
	 * @param binds
	 *
	 * @throws ScriptException
	 */
	public void init(String id, String description, String group, String script, String lang, String ext,
			Bindings binds)
			throws ScriptException {
		super.init(id, description, group);
		this.script = script;
		this.language = lang;
		this.ext = ext;

		ScriptEngineManager scriptEngineManager = (ScriptEngineManager) binds.get(SCRI_MANA);

		log.log( Level.FINEST,
						 "Trying to load admin command: {0}, description: {1}, language: {2}, ext: {3}",
						 new Object[] { id, description, this.language, this.ext } );

		if (language != null) {
			scriptEngine = scriptEngineManager.getEngineByName(language);
		}

		if (ext != null) {
			scriptEngine = scriptEngineManager.getEngineByExtension(ext);
		}

		if ( !Packet.FULL_DEBUG && (scriptEngine instanceof Compilable)) {
			compiledScript = ((Compilable) scriptEngine).compile(script);
		}

		if (this.language == null && scriptEngine != null) {
			this.language = scriptEngine.getFactory().getLanguageName();
		}

		if (this.ext == null && scriptEngine != null) {
			this.ext = scriptEngine.getFactory().getExtensions().get(0);
		}

		log.log(Level.FINE, "Initialized script command, id: {0}, lang: {1}, ext: {2}",
				new Object[] { id, this.language, this.ext });

	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public void runCommand(Iq packet, Bindings binds, Queue<Packet> results) {
		ScriptContext context = null;
		StringWriter writer = null;

		try {
			long start = System.currentTimeMillis();
			// Bindings localBinds = scriptEngine.createBindings();
			binds.put(PACKET, packet);

			// Workaround for Python which doesn't return values and can overwrite
			// values only if type is correct
			Object res = "";

			binds.put("result", res);
			context = scriptEngine.getContext();
			context.setBindings(binds, ScriptContext.ENGINE_SCOPE);
			writer = new StringWriter();
			context.setErrorWriter(writer);

			if (compiledScript != null) {
				res = compiledScript.eval(context);
			} else {
				res = scriptEngine.eval(script, context);
			}

			if (res == null) {

				// Yes, Python doesn't return results normally
				// (or I don't know how to do it)
				// Python can either return a Packet as 'packet' or string as 'result'
				res = binds.get("result");

				if (res.toString().isEmpty()) {
					res = binds.get(PACKET);

					if (res == packet) {

						// Ups, apparently the script returned no results, to avoid infinite loop
						// we have to handle this somehow...
						res = "Script finished with no errors but returned no results.";
					}
				}
			}

			if (res instanceof Packet) {
				results.offer((Packet) res);
			} else {
				if (res instanceof Queue) {
					results.addAll((Queue<Packet>) res);
				} else {
					Packet result = packet.commandResult(Command.DataType.result);

					// Command.addTextField(result, "Note", "Script execution result");
					String[] text = null;

					if (res != null) {
						text = res.toString().split("\n");
					} else {
						text = new String[] { "Script returned no results." };
					}

					Command.addFieldMultiValue(result, SCRIPT_RESULT, Arrays.asList(text));
					results.offer(result);
				}
			}
			long end = System.currentTimeMillis();
			statisticExecutedIn(end-start);
		} catch (Exception e) {
			Packet result = packet.commandResult(Command.DataType.result);

			Command.addTextField(result, "Note", "Script execution error.");

			StackTraceElement[] ste = e.getStackTrace();
			String[] error =
				new String[ste.length + 2 + ((writer != null) ? writer.toString().split("\n").length : 0)];

			error[0] = e.getMessage();
			error[1] = e.toString();

			for (int i = 0; i < ste.length; i++) {
				error[i + 2] = ste[i].toString();
			}

			if (writer != null) {
				String[] errorMsgs = writer.toString().split("\n");

				for (int i = 0; i < errorMsgs.length; i++) {
					error[i + 2 + ste.length] = errorMsgs[i];
				}
			}

			if (e.getMessage() != null) {
				Command.addTextField(result, "Error message", e.getMessage());
			}

			Command.addFieldMultiValue(result, "Debug info", Arrays.asList(error));
			results.offer(result);
		}
	}	
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
