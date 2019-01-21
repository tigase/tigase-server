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

import tigase.kernel.beans.Inject;
import tigase.monitor.ConfigurableTask;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScriptTimerTask
		extends AbstractConfigurableTimerTask
		implements ConfigurableTask {

	private static final Logger log = Logger.getLogger(ScriptTimerTask.class.getName());

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

	public void setScript(String script, String scriptExtension) {
		this.engine = scriptEngineManager.getEngineByExtension(scriptExtension);
		this.script = script;
		this.scriptExtension = scriptExtension;
	}

	@Override
	protected void run() {
		try {
			engine.eval(script, bindings);
		} catch (Throwable e) {
			log.log(Level.WARNING, "Error while executing SimpleTimerTask", e);
		}
	}

}
