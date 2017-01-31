package tigase.monitor.tasks;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.conf.ConfiguratorAbstract;
import tigase.disteventbus.EventBus;
import tigase.form.Field;
import tigase.form.Form;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.monitor.InfoTask;
import tigase.monitor.MonitorComponent;
import tigase.server.XMPPServer;
import tigase.server.xmppsession.SessionManager;
import tigase.util.DateTimeFormatter;
import tigase.xml.Element;

@Bean(name = "connections-task")
public class ConnectionsTask extends AbstractConfigurableTimerTask implements InfoTask {

	protected final static DateTimeFormatter dtf = new DateTimeFormatter();
	protected static final Logger log = Logger.getLogger(ConnectionsTask.class.getName());
	private static final String USERS_DISCONNECTEED_EVENT_NAME = "UsersDisconnected";
	@Inject
	protected MonitorComponent component;
	@Inject
	protected EventBus eventBus;
	private int lastOnlineUsers;
	@ConfigField(desc = "Minimal amount of disconnected users")
	private int thresholdMinimal = 10;
	@ConfigField(desc = "Percent of disconnected users")
	private int threshold = 80;

	/**
	 * Creates alarm event if required. Event will be created only if both
	 * conditions will met.
	 *
	 * @param currentOnlineUsers
	 *            current amount of online users.
	 * @param lastOnlineUsers
	 *            previous amount of online users.
	 * @param thresholdMinimal
	 *            minimal amount of disconnected users to create alarm event.
	 * @param threshold
	 *            percent of disconnected users to create alarm event.
	 * @return event or <code>null</code>.
	 */
	public static Element createAlarmEvent(int currentOnlineUsers, int lastOnlineUsers, int thresholdMinimal, int threshold) {
		final int delta = currentOnlineUsers - lastOnlineUsers;
		final float percent = (lastOnlineUsers == 0 ? 1 : ((float) delta) / (float) lastOnlineUsers) * 100;

		if (log.isLoggable(Level.FINE))
			log.fine("Data: lastOnlineUsers=" + lastOnlineUsers + "; currentOnlineUsers=" + currentOnlineUsers + "; delta="
					+ delta + "; percent=" + percent + "; thresholdMinimal=" + thresholdMinimal + "; threshold=" + threshold);

		if (-1 * delta >= thresholdMinimal && -1 * percent >= threshold) {
			if (log.isLoggable(Level.FINE))
				log.fine("Creating event!");

			Element event = new Element(USERS_DISCONNECTEED_EVENT_NAME, new String[] { "xmlns" },
					new String[] { MonitorComponent.EVENTS_XMLNS });
			event.addChild(new Element("timestamp", "" + dtf.formatDateTime(new Date())));
			event.addChild(new Element("disconnections", "" + (-1 * delta)));
			event.addChild(new Element("disconnectionsPercent", "" + (-1 * percent)));

			return event;
		} else
			return null;
	}

	@Override
	public Form getCurrentConfiguration() {
		Form x = super.getCurrentConfiguration();
		x.addField(Field.fieldTextSingle("threshold", "" + threshold, "Percent of disconnected users"));
		x.addField(Field.fieldTextSingle("thresholdMinimal", "" + thresholdMinimal, "Minimal amount of disconnected users"));
		return x;
	}

	@Override
	public Form getTaskInfo() {
		Form x = new Form("", "Task Info", "");
		x.addField(Field.fieldTextSingle("lastUsersOnline", "" + lastOnlineUsers, "Last measured online users"));
		return x;
	}

	public int getThreshold() {
		return threshold;
	}

	public void setThreshold(int threshold) {
		this.threshold = threshold;
	}

	public int getThresholdMinimal() {
		return thresholdMinimal;
	}

	public void setThresholdMinimal(int thresholdMinimal) {
		this.thresholdMinimal = thresholdMinimal;
	}

	@Override
	protected void run() {
		if (log.isLoggable(Level.FINEST))
			log.finest("Running task...");

		ConfiguratorAbstract configurator = XMPPServer.getConfigurator();
		SessionManager sess = (SessionManager) configurator.getComponent("sess-man");

		final int currentOnlineUsers = sess.getOpenUsersConnectionsAmount();

		Element event = createAlarmEvent(currentOnlineUsers, lastOnlineUsers, thresholdMinimal, threshold);
		if (event != null) {
			event.addChild(new Element("hostname", component.getDefHostName().toString()));
			eventBus.fire(event);
		}

		this.lastOnlineUsers = currentOnlineUsers;
	}

	@Override
	public void setNewConfiguration(Form form) {
		Field thresholdPercent = form.get("threshold");
		if (thresholdPercent != null) {
			this.threshold = Integer.parseInt(thresholdPercent.getValue());
		}

		Field thresholdNetto = form.get("thresholdMinimal");
		if (thresholdNetto != null) {
			this.thresholdMinimal = Integer.parseInt(thresholdNetto.getValue());
		}

		super.setNewConfiguration(form);
	}
}
