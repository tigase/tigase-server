package tigase.monitor.tasks;

import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScriptTask extends AbstractConfigurableTask implements Initializable {

	private static final Logger log = Logger.getLogger(ScriptTask.class.getName());

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
			log.log(Level.WARNING, "Execution failed for the monitoring script: {0}", new Object[]{getScript()});
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Execution failed for the monitoring script: " + getScript(), e);
			}
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
