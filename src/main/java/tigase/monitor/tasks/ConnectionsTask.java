/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.monitor.tasks;

import tigase.eventbus.EventBus;
import tigase.form.Field;
import tigase.form.Form;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.monitor.InfoTask;
import tigase.monitor.MonitorComponent;
import tigase.server.XMPPServer;
import tigase.server.xmppsession.SessionManager;
import tigase.util.datetime.TimestampHelper;
import tigase.xml.Element;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "connections-task", parent = MonitorComponent.class, active = true)
public class ConnectionsTask
		extends AbstractConfigurableTimerTask
		implements InfoTask, Initializable {

	protected final static TimestampHelper dtf = new TimestampHelper();
	protected static final Logger log = Logger.getLogger(ConnectionsTask.class.getName());
	private static final String USERS_DISCONNECTEED_EVENT_NAME = "tigase.monitor.tasks.UsersDisconnected";
	@Inject
	protected MonitorComponent component;
	@Inject
	protected EventBus eventBus;
	private int lastOnlineUsers;
	@ConfigField(desc = "Percent of disconnected users")
	private int threshold = 80;
	@ConfigField(desc = "Minimal amount of disconnected users")
	private int thresholdMinimal = 10;

	/**
	 * Creates alarm event if required. Event will be created only if both conditions will met.
	 *
	 * @param currentOnlineUsers current amount of online users.
	 * @param lastOnlineUsers previous amount of online users.
	 * @param thresholdMinimal minimal amount of disconnected users to create alarm event.
	 * @param threshold percent of disconnected users to create alarm event.
	 *
	 * @return event or <code>null</code>.
	 */
	public static Element createAlarmEvent(int currentOnlineUsers, int lastOnlineUsers, int thresholdMinimal,
										   int threshold) {
		final int delta = currentOnlineUsers - lastOnlineUsers;
		final float percent = (lastOnlineUsers == 0 ? 1 : ((float) delta) / (float) lastOnlineUsers) * 100;

		if (log.isLoggable(Level.FINE)) {
			log.fine("Data: lastOnlineUsers=" + lastOnlineUsers + "; currentOnlineUsers=" + currentOnlineUsers +
							 "; delta=" + delta + "; percent=" + percent + "; thresholdMinimal=" + thresholdMinimal +
							 "; threshold=" + threshold);
		}

		if (-1 * delta >= thresholdMinimal && -1 * percent >= threshold) {
			if (log.isLoggable(Level.FINE)) {
				log.fine("Creating event!");
			}

			Element event = new Element(USERS_DISCONNECTEED_EVENT_NAME);
			event.addChild(new Element("timestamp", "" + dtf.format(new Date())));
			event.addChild(new Element("disconnections", "" + (-1 * delta)));
			event.addChild(new Element("disconnectionsPercent", "" + (-1 * percent)));

			return event;
		} else {
			return null;
		}
	}

	public ConnectionsTask() {
	}

	@Override
	public Form getCurrentConfiguration() {
		Form x = super.getCurrentConfiguration();
		x.addField(Field.fieldTextSingle("threshold", "" + threshold, "Percent of disconnected users"));
		x.addField(Field.fieldTextSingle("thresholdMinimal", "" + thresholdMinimal,
										 "Minimal amount of disconnected users"));
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
	public void initialize() {
		eventBus.registerEvent(USERS_DISCONNECTEED_EVENT_NAME,
							   "Fired when too many users disconnected in the same time", false);
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

	@Override
	protected void run() {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Running task...");
		}

		SessionManager sess = XMPPServer.getComponent(SessionManager.class);

		final int currentOnlineUsers = sess.getOpenUsersConnectionsAmount();

		Element event = createAlarmEvent(currentOnlineUsers, lastOnlineUsers, thresholdMinimal, threshold);
		if (event != null) {
			event.addChild(new Element("hostname", component.getDefHostName().toString()));
			eventBus.fire(event);
		}

		this.lastOnlineUsers = currentOnlineUsers;
	}
}
