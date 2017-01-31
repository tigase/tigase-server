package tigase.disteventbus.component;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import tigase.util.Algorithms;
import tigase.util.Base64;

public class ListenerScriptRegistrar {

	private EventBusContext context;
	private Map<String, ListenerScript> listenersScripts;
	private ScriptEngineManager scriptEngineManager;
	private String scriptPath = "./listenerScripts";

	public ListenerScriptRegistrar(Map<String, ListenerScript> listenersScripts, EventBusContext context,
			ScriptEngineManager scriptEngineManager) {
		this.context = context;
		this.listenersScripts = listenersScripts;
		this.scriptEngineManager = scriptEngineManager;
	}

	public void delete(String taskName) {
		File par = new File(scriptPath);
		if (!par.exists())
			return;

		File f = new File(par, Algorithms.sha256(taskName) + ".script");
		f.delete();
	}

	public void load() {
		File par = new File(scriptPath);

		if (!par.exists() || !par.isDirectory()) {
			return;
		}

		for (File f : par.listFiles()) {
			if (f.isFile() && f.getName().endsWith(".script")) {
				load(f);
			}
		}

	}

	private void load(final File f) {
		try {
			FileReader reader = new FileReader(f);
			Properties p = new Properties();
			p.load(reader);
			reader.close();

			String scriptName = p.getProperty("scriptName");
			String scriptExtension = p.getProperty("scriptExtension");
			String scriptContent = new String(Base64.decode(p.getProperty("scriptContent")));

			String eventXMLNS = p.getProperty("eventXMLNS");
			String eventName = p.getProperty("eventName");
			runScriptTask(scriptName, scriptExtension, scriptContent, eventName, eventXMLNS);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void registerScript(String scriptName, String scriptExtension, String scriptContent, String eventName,
			String eventXMLNS) throws ScriptException {
		saveScript(scriptName, scriptExtension, scriptContent, eventName, eventXMLNS);
		runScriptTask(scriptName, scriptExtension, scriptContent, eventName, eventXMLNS);
	}

	private void runScriptTask(String scriptName, String scriptExtension, String scriptContent, String eventName,
			String eventXMLNS) throws ScriptException {
		ListenerScript ls = new ListenerScript();
		listenersScripts.put(scriptName, ls);
		ls.run(context, scriptEngineManager, scriptName, scriptExtension, scriptContent,
				eventName == null || eventName.isEmpty() ? null : eventName, eventXMLNS);
	}

	private void saveScript(String scriptName, String scriptExtension, String scriptContent, String eventName, String eventXMLNS) {
		File par = new File(scriptPath);
		if (!par.exists())
			par.mkdirs();

		File f = new File(par, Algorithms.sha256(scriptName) + ".script");

		Properties p = new Properties();
		p.setProperty("scriptName", scriptName);
		p.setProperty("scriptExtension", scriptExtension);
		if (eventName != null)
			p.setProperty("eventName", eventName);
		p.setProperty("eventXMLNS", eventXMLNS);
		p.setProperty("scriptContent", Base64.encode(scriptContent.getBytes()));

		try {
			FileWriter writer = new FileWriter(f);
			p.store(writer, "Script " + scriptName + "." + scriptExtension);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
