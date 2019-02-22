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

import org.junit.Ignore;
import org.junit.Test;
import tigase.server.XMPPServer;
import tigase.util.Version;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UpdatesCheckerTest {

	@Test
	@Ignore
	public void retrieveVersion() {

		Logger log = Logger.getLogger(UpdatesChecker.class.getName());
		ConsoleHandler ch = new ConsoleHandler();
		ch.setLevel(Level.ALL);
		log.addHandler(ch);
		log.setLevel(Level.ALL);

		UpdatesChecker.ProductInfoIfc product = new TestProduct("my-awesome-product", "My Awesome Product", "1.2.3");
		UpdatesChecker.ProductInfoIfc product2 = new TestProduct("iot-hub", "IoT hub", "7.7.7");
		final List<UpdatesChecker.ProductInfoIfc> products = Arrays.asList(product, product2);

		String url = "http://atlantiscity.local:8080/rest/update/check/";

		final Version localVersion = XMPPServer.getVersion();
		final Optional<Version> version = UpdatesChecker.retrieveCurrentVersionFromServer(localVersion, products,
																						  UpdatesChecker.VERSION_URL, 10);
		System.out.println(version);

	}

	private class TestProduct
			implements UpdatesChecker.ProductInfoIfc {

		String id;
		String name;
		String version;

		public TestProduct(String id, String name, String version) {
			this.id = id;
			this.name = name;
			this.version = version;
		}

		@Override
		public String getProductId() {
			return id;
		}

		@Override
		public String getProductName() {
			return name;
		}

		@Override
		public Optional<String> getProductVersion() {
			return Optional.ofNullable(version);
		}
	}
}