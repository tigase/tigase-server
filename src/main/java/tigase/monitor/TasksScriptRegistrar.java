package tigase.monitor;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import javax.script.ScriptException;

import tigase.kernel.Inject;
import tigase.kernel.Kernel;
import tigase.monitor.tasks.ScriptTask;
import tigase.monitor.tasks.ScriptTimerTask;
import tigase.util.Algorithms;
import tigase.util.Base64;

public class TasksScriptRegistrar {

	public static final String ID = "TasksScriptRegistrar";

	@Inject
	private Kernel kernel;

	private String scriptPath = "./monitorScripts";

	public Kernel getKernel() {
		return kernel;
	}

	public void load() {
		File par = new File(scriptPath);

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
			Long delay;
			if (p.containsKey("delay")) {
				delay = Long.valueOf(p.getProperty("delay"));
			} else {
				delay = null;
			}

			if (delay == null)
				runScriptTask(scriptName, scriptExtension, scriptContent);
			else
				runScriptTask(scriptName, scriptExtension, scriptContent, delay);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void registerScript(String scriptName, String scriptExtension, String scriptContent) throws ScriptException {
		saveScript(scriptName, scriptExtension, scriptContent, null);
		runScriptTask(scriptName, scriptExtension, scriptContent);
	}

	public void registerScript(String scriptName, String scriptExtension, String scriptContent, Long delay)
			throws ScriptException {
		saveScript(scriptName, scriptExtension, scriptContent, delay);
		runScriptTask(scriptName, scriptExtension, scriptContent, delay);
	}

	private void runScriptTask(String scriptName, String scriptExtension, String scriptContent) throws ScriptException {
		kernel.registerBeanClass(scriptName, ScriptTask.class);
		ScriptTask scriptTask = kernel.getInstance(scriptName);
		scriptTask.run(scriptContent, scriptExtension);
	}

	private void runScriptTask(String scriptName, String scriptExtension, String scriptContent, Long delay)
			throws ScriptException {
		kernel.registerBeanClass(scriptName, ScriptTimerTask.class);
		ScriptTimerTask scriptTask = kernel.getInstance(scriptName);
		scriptTask.run(scriptContent, scriptExtension, delay);
	}

	private void saveScript(String scriptName, String scriptExtension, String scriptContent, Long delay) {
		File par = new File(scriptPath);
		if (!par.exists())
			par.mkdirs();

		File f = new File(par, Algorithms.sha256(scriptName) + ".script");

		Properties p = new Properties();
		p.setProperty("scriptName", scriptName);
		p.setProperty("scriptExtension", scriptExtension);
		p.setProperty("scriptContent", Base64.encode(scriptContent.getBytes()));
		if (delay != null) {
			p.setProperty("delay", delay.toString());
		}

		try {
			FileWriter writer = new FileWriter(f);
			p.store(writer, "Script " + scriptName + "." + scriptExtension);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setKernel(Kernel kernel) {
		this.kernel = kernel;
	}

	public void delete(String taskName) {
		File par = new File(scriptPath);
		if (!par.exists())
			return;

		File f = new File(par, Algorithms.sha256(taskName) + ".script");
		f.delete();
	}

}
