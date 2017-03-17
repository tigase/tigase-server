package tigase.monitor;

import tigase.component.AbstractKernelBasedComponent;
import tigase.component.modules.impl.JabberVersionModule;
import tigase.component.modules.impl.XmppPingModule;
import tigase.kernel.beans.Bean;
import tigase.kernel.core.Kernel;
import tigase.monitor.modules.*;
import tigase.server.monitor.MonitorRuntime;
import tigase.util.TimerTask;

import javax.script.ScriptEngineManager;

@Bean(name = "monitor", parent = Kernel.class, active = true)
public class MonitorComponent extends AbstractKernelBasedComponent {

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
	public String getComponentVersion() {
		String version = this.getClass().getPackage().getImplementationVersion();
		return version == null ? "0.0.0" : version;
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
		return false;
	}

	@Override
	protected void registerModules(Kernel kernel) {
		kernel.registerBean("runtime").asInstance(MonitorRuntime.getMonitorRuntime()).exec();
		kernel.registerBean(TasksScriptRegistrar.class).exec();

		kernel.registerBean(XmppPingModule.class).exec();
		kernel.registerBean(JabberVersionModule.class).exec();
		kernel.registerBean(AdHocCommandMonitorModule.class).exec();
		kernel.registerBean(DiscoveryMonitorModule.class).exec();

		kernel.registerBean(AddScriptTaskCommand.class).exec();
		kernel.registerBean(AddTimerScriptTaskCommand.class).exec();
		kernel.registerBean(DeleteScriptTaskCommand.class).exec();

		ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		kernel.registerBean("scriptEngineManager").asInstance(scriptEngineManager).exec();
		kernel.registerBean("bindings").asInstance(scriptEngineManager.getBindings()).exec();

		kernel.registerBean("timerTaskService").asInstance(timerTaskService).exec();
	}
	
	@Override
	public void start() {
		super.start();
		((TasksScriptRegistrar) kernel.getInstance(TasksScriptRegistrar.ID)).load();
	}
}
