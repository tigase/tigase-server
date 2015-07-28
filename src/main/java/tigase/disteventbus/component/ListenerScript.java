package tigase.disteventbus.component;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import tigase.disteventbus.EventBus;
import tigase.disteventbus.EventHandler;
import tigase.kernel.core.Kernel;
import tigase.xml.Element;

public class ListenerScript implements EventHandler {

	private CompiledScript compiledScript;

	private ScriptEngine engine;

	private EventBus eventBus;

	private String eventName;

	private String eventXMLNS;

	private Kernel kernel;

	private String scriptContent;

	@Override
	public void onEvent(String name, String xmlns, Element event) {
		try {
			Bindings bindings = engine.createBindings();
			bindings.put("event", event);
			bindings.put("eventName", name);
			bindings.put("eventXMLNS", xmlns);
			bindings.put("eventXMLNS", xmlns);
			bindings.put("kernel", kernel);

			if (this.compiledScript != null) {
				this.compiledScript.eval(bindings);
			} else {
				this.engine.eval(scriptContent, bindings);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run(Kernel kernel, ScriptEngineManager scriptEngineManager, String scriptName, String scriptExtension,
			String scriptContent, String eventName, String eventXMLNS) throws ScriptException {
		this.kernel = kernel;
		this.eventName = eventName;
		this.eventXMLNS = eventXMLNS;
		this.engine = scriptEngineManager.getEngineByExtension(scriptExtension);
		this.scriptContent = scriptContent;
		this.eventBus = kernel.getInstance("eventBus");
		if (engine instanceof Compilable) {
			this.compiledScript = ((Compilable) engine).compile(scriptContent);
		} else {
			this.compiledScript = null;
		}

		eventBus.addHandler(this.eventName, this.eventXMLNS, this);
	}

	public void unregister() {
		eventBus.removeHandler(this.eventName, this.eventXMLNS, this);
	}
}
