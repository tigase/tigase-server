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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

	public static final String VERSION_REQUEST_KEY = "tigase-server-version";
	public static final String PRODUCTS_REQUEST_KEY = "products";

	private static final Logger log = Logger.getLogger(UpdatesChecker.class.getName());
	static final String VERSION_URL = "http://update.tigase.net/check/";

	private final Version serverVersion;

	@Inject
	private EventBus eventBus;
	private Version latestCheckedVersion = null;
	@ConfigField(desc = "Enables sending XMPP notifications about new version")
	private Boolean notificationsEnabled = true;
	@Inject(nullAllowed = true)
	private ArrayList<ProductInfoIfc> productInfos = new ArrayList<>();
	@ConfigField(desc = "List of receivers JIDs", alias = "admins")
	private ConcurrentSkipListSet<BareJID> receivers = new ConcurrentSkipListSet<BareJID>();

	private static Version getVersion(String version) {
		try {
			return Version.of(version);
		} catch (IllegalArgumentException e) {
			log.log(Level.FINE, "Error parsing version from server");
			return null;
		}
	}

	public static Optional<Version> retrieveCurrentVersionFromServer(Version currentVersion,
																	 List<ProductInfoIfc> products, String url,
																	 int timeoutInSeconds) {

		Objects.nonNull(currentVersion);
		Objects.nonNull(url);

		log.log(Level.FINEST, "Retrieving latest version information for localVersion: {0}, products: {1}",
				new String[]{String.valueOf(currentVersion), String.valueOf(products)});
		try {
			final URL u = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) u.openConnection();

			connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(timeoutInSeconds));
			connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(timeoutInSeconds));

			connection.setRequestProperty(VERSION_REQUEST_KEY, currentVersion.toString());

			if (products != null && !products.isEmpty()) {
				String requestProducts = products.stream()
						.filter(productInfoIfc -> productInfoIfc.getProductVersion().isPresent())
						.map(pi -> String.join(":", pi.getProductId(), pi.getProductVersion().get()))
						.collect(Collectors.joining(";"));
				connection.setRequestProperty(PRODUCTS_REQUEST_KEY, requestProducts);
			}

			BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

			return br.lines()
					.map(UpdatesChecker::getVersion)
					.filter(Objects::nonNull)
//					.peek(System.out::println)
					.filter(e -> e.compareTo(currentVersion) > 0)
					.findFirst();

		} catch (IOException e) {
			log.log(Level.WARNING, "Can not check updates for URL: " + url, e);
		}

		return Optional.empty();
	}

	public UpdatesChecker() {
		super(Duration.ofDays(7), Duration.ofDays(7));
		serverVersion = XMPPServer.getVersion();
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
		final Optional<Version> version;
		version = retrieveCurrentVersionFromServer(serverVersion, productInfos, VERSION_URL, 60);
		version.ifPresent(this::sendNewVersionNotification);
	}

	public void setProductInfos(ArrayList<ProductInfoIfc> productInfos) {
		if (productInfos == null) {
			this.productInfos = new ArrayList<>();
		} else {
			this.productInfos = productInfos;
		}
	}

	@HandleEvent
	protected void onUpdatedVersionDiscovered(UpdatesChecker.UpdatedVersionDiscovered event) {
		if ((event.getVersion() != null) && isNewerVersion(event.getVersion())) {
			log.log(Level.CONFIG, "New version updated from cluster");
		}
	}

	private void sendNewVersionNotification(Version v) {
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
	}

	private void fire(Object event) {
		if (eventBus != null) {
			eventBus.fire(event);
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
				"This is automated message generated by updates checking module (you can adjust it's configuration)." +
				"\n\n" + "You can adjust frequency of the check by setting 'delay' and 'period' "

		);
		message.addChild(body);
		return Packet.packetInstance(message, sender, JID.jidInstance(jid));
	}

	public interface ProductInfoIfc {

		/**
		 * Product identifier
		 */
		String getProductId();

		/**
		 * Human readable product name
		 */
		String getProductName();

		/**
		 * Version of the product
		 */
		default Optional<String> getProductVersion() {
			return Optional.ofNullable(this.getClass().getPackage().getImplementationVersion());
		}
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