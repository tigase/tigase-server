package tigase.monitor;

import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptEngineManager;

import tigase.component.AbstractComponent;
import tigase.component.AbstractContext;
import tigase.component.adhoc.AdHocCommand;
import tigase.component.modules.Module;
import tigase.component.modules.impl.AdHocCommandModule;
import tigase.component.modules.impl.DiscoveryModule;
import tigase.component.modules.impl.JabberVersionModule;
import tigase.component.modules.impl.XmppPingModule;
import tigase.conf.ConfigurationException;
import tigase.kernel.Kernel;
import tigase.monitor.modules.AdHocCommandMonitorModule;
import tigase.monitor.modules.AddScriptTaskCommand;
import tigase.monitor.modules.AddTimerScriptTaskCommand;
import tigase.monitor.modules.DeleteScriptTaskCommand;
import tigase.monitor.modules.DiscoveryMonitorModule;
import tigase.server.monitor.MonitorRuntime;
import tigase.util.TimerTask;

public class MonitorComponent extends AbstractComponent<MonitorContext> {

	private class MonitorContextImpl extends AbstractContext implements MonitorContext {

		public MonitorContextImpl(AbstractComponent<?> component) {
			super(component);
		}

		@Override
		public Kernel getKernel() {
			return kernel;
		}
	}

	private Kernel kernel = new Kernel();

	private final TimerTaskService timerTaskService = new TimerTaskService() {

		@Override
		public void addTimerTask(TimerTask task, long delay) {
			MonitorComponent.this.addTimerTask(task, delay);
		}

		@Override
		public void addTimerTask(TimerTask task, long initialDelay, long period) {
			MonitorComponent.this.addTimerTask(task, initialDelay, period);
		}
	};

	@Override
	protected MonitorContext createContext() {
		return new MonitorContextImpl(this);
	}

	@Override
	public String getComponentVersion() {
		String version = this.getClass().getPackage().getImplementationVersion();
		return version == null ? "0.0.0" : version;
	}

	@Override
	protected Map<String, Class<? extends Module>> getDefaultModulesList() {
		final Map<String, Class<? extends Module>> result = new HashMap<String, Class<? extends Module>>();

		result.put(XmppPingModule.ID, XmppPingModule.class);
		result.put(JabberVersionModule.ID, JabberVersionModule.class);
		result.put(AdHocCommandModule.ID, AdHocCommandMonitorModule.class);
		result.put(DiscoveryModule.ID, DiscoveryMonitorModule.class);

		return result;
	}

	@Override
	public String getDiscoCategory() {
		return "component";
	}

	@Override
	public String getDiscoCategoryType() {
		return "monitor";
	}

	@Override
	public String getDiscoDescription() {
		return "Monitor Component";
	}

	@Override
	public boolean isDiscoNonAdmin() {
		return true;
	}

	@Override
	public void setProperties(Map<String, Object> props) throws ConfigurationException {
		super.setProperties(props);

		// kernel.registerBeanClass("memory-monitor", MemoryMonitorTask.class);

		kernel.registerBeanClass(TasksScriptRegistrar.ID, TasksScriptRegistrar.class);

		ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		kernel.registerBean("scriptEngineManager", scriptEngineManager);
		kernel.registerBean("bindings", scriptEngineManager.getBindings());
		kernel.registerBean("context", getContext());
		kernel.registerBean("monitorComponent", this);
		kernel.registerBean("timerTaskService", timerTaskService);
		kernel.registerBean("runtime", MonitorRuntime.getMonitorRuntime());
		kernel.registerBean("kernel", kernel);

		AdHocCommand ahc;

		ahc = new AddScriptTaskCommand(context);
		((AdHocCommandMonitorModule) getModuleProvider().getModule(AdHocCommandModule.ID)).register(ahc);
		kernel.registerBean(ahc.getName(), ahc);
		ahc = new AddTimerScriptTaskCommand(context);
		((AdHocCommandMonitorModule) getModuleProvider().getModule(AdHocCommandModule.ID)).register(ahc);
		kernel.registerBean(ahc.getName(), ahc);
		ahc = new DeleteScriptTaskCommand(context);
		((AdHocCommandMonitorModule) getModuleProvider().getModule(AdHocCommandModule.ID)).register(ahc);
		kernel.registerBean(ahc.getName(), ahc);

		// initialization
		((TasksScriptRegistrar) kernel.getInstance(TasksScriptRegistrar.ID)).load();
	}

}
