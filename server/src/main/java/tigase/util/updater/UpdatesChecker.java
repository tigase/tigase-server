/*
 * UpdatesChecker.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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

package tigase.util.updater;

import tigase.component.ScheduledTask;
import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.server.MessageRouter;
import tigase.server.Packet;
import tigase.server.XMPPServer;
import tigase.util.Version;
import tigase.xml.Element;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Describe class UpdatesChecker here.
 * <br>
 * Created: Fri Apr 18 09:35:32 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
@Bean(name = "update-checker", parent = MessageRouter.class, active = true)
public class UpdatesChecker
		extends ScheduledTask {

	private static final Logger log = Logger.getLogger(UpdatesChecker.class.getName());
	private static final String VERSION_URL = "http://update.tigase.net/check/";
	private static Version serverVersion = null;

	static {
		try {
			serverVersion = Version.of(XMPPServer.getImplementationVersion());
		} catch (IllegalArgumentException e) {
			log.log(Level.WARNING, "Problem obtaining current version information");
		}
	}

	@Inject
	private EventBus eventBus;
	private Version latestCheckedVersion = null;
	@ConfigField(desc = "Enables sending XMPP notifications about new version")
	private Boolean notificationsEnabled = true;
	@ConfigField(desc = "List of receivers JIDs", alias = "admins")
	private ConcurrentSkipListSet<BareJID> receivers = new ConcurrentSkipListSet<BareJID>();

	public static void main(String[] args) throws InterruptedException {

		final UpdatesChecker checker = new UpdatesChecker();

		final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
		executorService.schedule(checker, 1, TimeUnit.SECONDS);

		executorService.shutdown();
		executorService.awaitTermination(15, TimeUnit.SECONDS);
		checker.cancel();

		System.exit(0);

	}

	public UpdatesChecker() {
		super(Duration.ofDays(7), Duration.ofDays(7));
	}

	@Override
	public void initialize() {
		super.initialize();
		if (eventBus != null) {
			eventBus.registerAll(this);
		}
	}

	@Override
	public void run() {
		try {
			final URL url = new URL(VERSION_URL);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			connection.setConnectTimeout(1000 * 60);
			connection.setReadTimeout(1000 * 60);
			if (null != serverVersion) {
				connection.setRequestProperty("tigase-server-version", serverVersion.toString());
			}

			BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			final Optional<Version> version = br.lines()
//					.filter(line -> line.startsWith(FILE_START))
					.map(this::getVersion).filter(Objects::nonNull)
//					.filter(e -> !serverVersion.isZero() && e.compareTo(serverVersion) > 0)
					.filter(e -> e.compareTo(serverVersion) > 0).findFirst();

			version.ifPresent(v -> {
				log.log(Level.CONFIG, "Update available: " + v + " (current version: " + serverVersion + ")");

				if (notificationsEnabled && isNewerVersion(v)) {
					receivers.stream()
							.filter(addr -> !addr.toString().contains("{clusternode}"))
							.filter(addr -> !component.getNodesConnectedWithLocal()
									.contains(JID.jidInstanceNS(addr.getDomain())))
							.map(admin -> prepareMessage(v, admin))
							.forEach(p -> component.addPacket(p));
				}

				fire(new UpdatedVersionDiscovered(v));
			});

		} catch (IOException e) {
			log.log(Level.WARNING, "Can not check updates for URL: " + VERSION_URL, e);
		} catch (Exception e) {
			log.log(Level.WARNING, "Unknown exception for: " + VERSION_URL, e);
		}
	}

	@HandleEvent
	protected void onUpdatedVersionDiscovered(UpdatesChecker.UpdatedVersionDiscovered event) {
		if ((event.getVersion() != null) && isNewerVersion(event.getVersion())) {
			log.log(Level.CONFIG, "New version updated from cluster");
		}
	}

	private void fire(Object event) {
		if (eventBus != null) {
			eventBus.fire(event);
		}
	}

	private Version getVersion(String version) {
		try {
			return Version.of(version);
		} catch (IllegalArgumentException e) {
			log.log(Level.FINE, "Error parsing version from server");
			return null;
		}
	}

	private boolean isNewerVersion(Version ver) {
		if (latestCheckedVersion == null || ver.compareTo(latestCheckedVersion) > 0) {
			latestCheckedVersion = ver;
			return true;
		} else {
			return false;
		}
	}

	private Packet prepareMessage(Version v, BareJID jid) {
		String link = "http://tigase.net/downloads";
		final JID sender = JID.jidInstanceNS("updates.checker@" + jid.getDomain());
		Element message = new Element("message");
		Element subject = new Element("subject", "Updates checker - new version of the Tigase server");
		message.addChild(subject);
		message.setXMLNS("jabber:client");
		message.setAttribute("type", "normal");

		Element body = new Element("body", "You are currently using: '" + serverVersion + "' version of Tigase" +
				" server. A new version of the server has been released: '" + v +
				"' and it is available for download at address: " + link + "\n\n" +
				"This is automated message generated by updates checking module." + "\n\n" +
				"You can adjust frequency of the check by setting 'delay' and 'period' " +
				"fields of 'update-checker' (sub-bean of 'message-router' bean) to desired value " +
				"(setting both of them to 0 will disable the checking altogether), in addition you can disable sending XMPP notifications by setting 'notificationsEnabled' to 'false':\n" +
				"'message-router' (class: tigase.server.MessageRouter) {\n" +
				"\t'update-checker' (class: tigase.util.updater.UpdatesChecker) {\n" + "\t\tdelay = PT5M\n" +
				"\t\tperiod = P30D\n" + "\t\tnotificationsEnabled = true\n" +
				"\t\treceivers = ['admin_username@VHost']\n" + "\t}\n" + "}"

		);
		message.addChild(body);
		return Packet.packetInstance(message, sender, JID.jidInstance(jid));
	}

	public static class UpdatedVersionDiscovered {

		private final Version version;

		UpdatedVersionDiscovered(Version version) {
			this.version = version;
		}

		public Version getVersion() {
			return version;
		}
	}
}