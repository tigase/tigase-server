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
package tigase.monitor.tasks;

import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScriptTask
		extends AbstractConfigurableTask
		implements Initializable {

	private static final Logger log = Logger.getLogger(ScriptTask.class.getName());

	@Inject
	protected Bindings bindings;
	@Inject
	protected ScriptEngineManager scriptEngineManager;
	private ScriptEngine engine;
	private String script;
	private String scriptExtension;

	public Bindings getBindings() {
		return bindings;
	}

	public void setBindings(Bindings bindings) {
		this.bindings = bindings;
	}

	public String getScript() {
		return script;
	}

	public ScriptEngineManager getScriptEngineManager() {
		return scriptEngineManager;
	}

	public void setScriptEngineManager(ScriptEngineManager scriptEngineManager) {
		this.scriptEngineManager = scriptEngineManager;
	}

	public String getScriptExtension() {
		return scriptExtension;
	}

	@Override
	public void initialize() {
	}

	public void setScript(String script, String scriptExtension) {
		this.engine = scriptEngineManager.getEngineByExtension(scriptExtension);
		this.script = script;
		this.scriptExtension = scriptExtension;
	}

	@Override
	protected void enable() {
		super.enable();

		try {
			engine.eval(script, bindings);
		} catch (ScriptException e) {
			log.log(Level.WARNING, "Execution failed for the monitoring script: {0}", new Object[]{getScript()});
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Execution failed for the monitoring script: " + getScript(), e);
			}
		}
	}

}
