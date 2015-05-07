package tigase.monitor.tasks;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;

public class ScriptTask extends AbstractConfigurableTask implements Initializable {

	@Inject
	protected Bindings bindings;

	private ScriptEngine engine;

	private String script;

	@Inject
	protected ScriptEngineManager scriptEngineManager;

	private String scriptExtension;

	@Override
	protected void enable() {
		super.enable();

		try {
			engine.eval(script, bindings);
		} catch (ScriptException e) {
			e.printStackTrace();
		}
	}

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
	public void initialize() {
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
