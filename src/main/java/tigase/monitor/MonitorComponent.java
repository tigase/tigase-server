package tigase.monitor;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.script.ScriptEngineManager;

import tigase.component.AbstractKernelBasedComponent;
import tigase.component.modules.impl.JabberVersionModule;
import tigase.component.modules.impl.XmppPingModule;
import tigase.conf.ConfigurationException;
import tigase.db.comp.ComponentRepository;
import tigase.kernel.beans.Bean;
import tigase.kernel.core.Kernel;
import tigase.monitor.modules.*;
import tigase.server.monitor.MonitorRuntime;
import tigase.util.ClassUtil;
import tigase.util.TimerTask;

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
		return true;
	}

	@Override
	protected void registerModules(Kernel kernel) {
		kernel.registerBean(XmppPingModule.class).exec();
		kernel.registerBean(JabberVersionModule.class).exec();
		kernel.registerBean(AdHocCommandMonitorModule.class).exec();
		kernel.registerBean(DiscoveryMonitorModule.class).exec();

		kernel.registerBean(TasksScriptRegistrar.class).exec();

		kernel.registerBean(AddScriptTaskCommand.class).exec();
		kernel.registerBean(AddTimerScriptTaskCommand.class).exec();
		kernel.registerBean(DeleteScriptTaskCommand.class).exec();

		ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		kernel.registerBean("scriptEngineManager").asInstance(scriptEngineManager).exec();
		kernel.registerBean("bindings").asInstance(scriptEngineManager.getBindings()).exec();

		kernel.registerBean("timerTaskService").asInstance(timerTaskService).exec();
		kernel.registerBean("runtime").asInstance(MonitorRuntime.getMonitorRuntime()).exec();
	}

	@Override
	public void setProperties(Map<String, Object> props) throws ConfigurationException {
		if (props.size() <= 1)
			return;

		String repoClass = TaskConfigItemJDBCRepository.class.getName();
		try {
			ComponentRepository<TaskConfigItem> repo_tmp = (ComponentRepository<TaskConfigItem>) Class.forName(
					repoClass).newInstance();

			repo_tmp.setProperties(props);
			log.log(Level.WARNING, "Monitoring Tasks: {0} with items: {1}", new Object[] { repo_tmp, repo_tmp.toString() });
			repo_tmp.reload();

			kernel.registerBean("tasksRepo").asInstance(repo_tmp).exec();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Can not create T repository instance for class: " + repoClass, e);
		}

		super.setProperties(props);

		try {
			Set<Class<MonitorTask>> classes = ClassUtil.getClassesImplementing(MonitorTask.class);
			if (log.isLoggable(Level.FINER))
				log.finer("Found monitor tasks classes: " + classes.toString());

			for (Class<MonitorTask> class1 : classes) {
				if (class1.getAnnotation(Bean.class) != null)
					kernel.registerBean(class1).exec();
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			Set<Class<MonitorExtension>> classes = ClassUtil.getClassesImplementing(MonitorExtension.class);
			if (log.isLoggable(Level.FINER))
				log.finer("Found monitor ext classes: " + classes.toString());

			for (Class<MonitorExtension> class1 : classes) {
				if (class1.getAnnotation(Bean.class) != null)
					kernel.registerBean(class1).exec();
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (kernel.isBeanClassRegistered("monitor-mailer")) {
			Object mailerExt = kernel.getInstance("monitor-mailer");
			if (mailerExt instanceof MonitorExtension) {
				((MonitorExtension) mailerExt).setProperties(props);
			}
		}

		// initialization
		((TasksScriptRegistrar) kernel.getInstance(TasksScriptRegistrar.ID)).load();

		// ((BeanConfigurator)
		// kernel.getInstance(BeanConfigurator.NAME)).configureBeans(props);
	}

}
