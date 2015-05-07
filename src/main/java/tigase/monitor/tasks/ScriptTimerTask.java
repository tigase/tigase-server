package tigase.monitor.tasks;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import tigase.kernel.beans.Inject;
import tigase.monitor.ConfigurableTask;

public class ScriptTimerTask extends AbstractConfigurableTimerTask implements ConfigurableTask {

	@Inject
	protected Bindings bindings;

	private ScriptEngine engine;

	private String script;

	@Inject
	protected ScriptEngineManager scriptEngineManager;

	private String scriptExtension;

	public Bindings getBindings() {
		return bindings;
	}

	public String getScript() {
		return script;
	}

	public ScriptEngineManager getScriptEngineManager() {
		return scriptEngineManager;
	}

	public String getScriptExtension() {
		return scriptExtension;
	}

	@Override
	protected void run() {
		try {
			engine.eval(script, bindings);
		} catch (ScriptException e) {
			e.printStackTrace();
		}
	}

	public void setBindings(Bindings bindings) {
		this.bindings = bindings;
	}

	public void setScript(String script, String scriptExtension) {
		this.engine = scriptEngineManager.getEngineByExtension(scriptExtension);
		this.script = script;
		this.scriptExtension = scriptExtension;
	}

	public void setScriptEngineManager(ScriptEngineManager scriptEngineManager) {
		this.scriptEngineManager = scriptEngineManager;
	}

}
