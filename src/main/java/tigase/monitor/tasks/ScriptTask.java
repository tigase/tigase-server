package tigase.monitor.tasks;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import tigase.kernel.Initializable;
import tigase.kernel.Inject;
import tigase.monitor.MonitorTask;

public class ScriptTask implements MonitorTask, Initializable {

	@Inject
	protected Bindings bindings;

	@Inject
	protected ScriptEngineManager scriptEngineManager;

	public Bindings getBindings() {
		return bindings;
	}

	public ScriptEngineManager getScriptEngineManager() {
		return scriptEngineManager;
	}

	@Override
	public void initialize() {
	}

	public void run(String script, String scriptExtension) throws ScriptException {

		ScriptEngine engine = scriptEngineManager.getEngineByExtension(scriptExtension);
		engine.eval(script, bindings);
	}

	public void setBindings(Bindings bindings) {
		this.bindings = bindings;
	}

	public void setScriptEngineManager(ScriptEngineManager scriptEngineManager) {
		this.scriptEngineManager = scriptEngineManager;
	}

}
