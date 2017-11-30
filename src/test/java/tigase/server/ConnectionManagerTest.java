/*
 * ConnectionManagerTest.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 */

package tigase.server;

import org.junit.Test;
import tigase.io.IOInterface;
import tigase.net.IOService;
import tigase.stats.StatisticsList;
import tigase.xmpp.XMPPIOService;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by andrzej on 20.01.2017.
 */
public class ConnectionManagerTest {
	
	@Test
	public void test_watchDog_timeout_whitespace() throws Exception {
		test_watchDogStop(ConnectionManager.WATCHDOG_PING_TYPE.WHITESPACE, Type.timeout, false);
	}

	@Test
	public void test_watchDog_timeout_xmppPing() throws Exception {
		test_watchDogStop(ConnectionManager.WATCHDOG_PING_TYPE.XMPP, Type.timeout, false);
	}

	@Test
	public void test_watchDog_timeout_whitespace_withDataWaiting() throws Exception {
		test_watchDogStop(ConnectionManager.WATCHDOG_PING_TYPE.WHITESPACE, Type.timeout, true);
	}

	@Test
	public void test_watchDog_timeout_xmppPing_withDataWaiting() throws Exception {
		test_watchDogStop(ConnectionManager.WATCHDOG_PING_TYPE.XMPP, Type.timeout, true);
	}

	protected ConnectionManager newConnectionManager(ConnectionManager.WATCHDOG_PING_TYPE pingType) throws Exception {
		ConnectionManager connectionManager = new ConnectionManager() {
			@Override
			public Queue<Packet> processSocketData(XMPPIOService serv) {
				return null;
			}

			@Override
			public boolean processUndeliveredPacket(Packet packet, Long stamp, String errorMessage) {
				return false;
			}

			@Override
			public void reconnectionFailed(Map port_props) {

			}

			@Override
			protected long getMaxInactiveTime() {
				return 3 * MINUTE;
			}

			@Override
			protected XMPPIOService<?> getXMPPIOServiceInstance() {
				return null;
			}

			@Override
			public void xmppStreamClosed(XMPPIOService serv) {

			}

			@Override
			public String[] xmppStreamOpened(XMPPIOService serv, Map attribs) {
				return null;
			}

			@Override
			public void packetsReady(IOService service) throws IOException {

			}

			@Override
			public boolean serviceStopped(IOService service) {
				return super.serviceStopped((XMPPIOService) service);
			}

			@Override
			public void tlsHandshakeCompleted(IOService service) {

			}
		};
		
		Field f = ConnectionManager.class.getDeclaredField("watchdogPingType");
		f.setAccessible(true);
		f.set(connectionManager, pingType);
		f = ConnectionManager.class.getDeclaredField("watchdogDelay");
		f.setAccessible(true);
		f.set(connectionManager, 1);
		f = ConnectionManager.class.getDeclaredField("watchdogTimeout");
		f.setAccessible(true);
		f.set(connectionManager, -1);
		
		return connectionManager;

	}

	protected XMPPIOService getXMPPIOServiceInstance() {
		return new XMPPIOService() {
			@Override
			public boolean waitingToRead() {
				return false;
			}

			@Override
			public boolean waitingToSend() {
				return false;
			}
		};
	}

	protected XMPPIOService registerService(ConnectionManager connectionManager, boolean waitingToSend) throws Exception {

		XMPPIOService service = getXMPPIOServiceInstance();
		assertNotNull(service);

		IOInterface io = new IOInterface() {
			@Override
			public int bytesRead() {
				return 0;
			}

			@Override
			public boolean checkCapabilities(String caps) {
				return false;
			}

			@Override
			public int getInputPacketSize() throws IOException {
				return 0;
			}

			@Override
			public SocketChannel getSocketChannel() {
				return null;
			}

			@Override
			public void getStatistics(StatisticsList list, boolean reset) {

			}

			@Override
			public long getBytesSent(boolean reset) {
				return 0;
			}

			@Override
			public long getTotalBytesSent() {
				return 0;
			}

			@Override
			public long getBytesReceived(boolean reset) {
				return 0;
			}

			@Override
			public long getTotalBytesReceived() {
				return 0;
			}

			@Override
			public long getBuffOverflow(boolean reset) {
				return 0;
			}

			@Override
			public long getTotalBuffOverflow() {
				return 0;
			}

			@Override
			public boolean isConnected() {
				return false;
			}

			@Override
			public boolean isRemoteAddress(String addr) {
				return false;
			}

			@Override
			public ByteBuffer read(ByteBuffer buff) throws IOException {
				throw new IOException("Read failed!");
			}

			@Override
			public void stop() throws IOException {

			}

			@Override
			public boolean waitingToSend() {
				return waitingToSend;
			}

			@Override
			public int waitingToSendSize() {
				return 0;
			}

			@Override
			public int write(ByteBuffer buff) throws IOException {
				throw new IOException("Write failed!");
			}

			@Override
			public void setLogId(String logId) {

			}
		};
		Field f = IOService.class.getDeclaredField("socketIO");
		f.setAccessible(true);
		f.set(service, io);

		String serviceId = UUID.randomUUID().toString();
		f = IOService.class.getDeclaredField("id");
		f.setAccessible(true);
		f.set(service, serviceId);
		getServices(connectionManager).put(serviceId, service);
		service.setIOServiceListener(connectionManager);

		return service;
	}

	protected void test_watchDogStop(ConnectionManager.WATCHDOG_PING_TYPE pingType, Type testType, boolean waitingToSend) throws Exception {
		ConnectionManager connectionManager = newConnectionManager(pingType);

		XMPPIOService service = registerService(connectionManager, waitingToSend);
		Field f = pingType == ConnectionManager.WATCHDOG_PING_TYPE.XMPP ? XMPPIOService.class.getDeclaredField(
				"lastXmppPacketReceivedTime") : IOService.class.getDeclaredField("lastTransferTime");
		f.setAccessible(true);
		switch (testType) {
			case inactivity:
				f.set(service, System.currentTimeMillis() - connectionManager.getMaxInactiveTime());
			case timeout:
				f.set(service, System.currentTimeMillis() - connectionManager.getMaxInactiveTime() / 2);
		}

		execute_watchDogStop(connectionManager);
	}

	protected void execute_watchDogStop(ConnectionManager connectionManager) throws Exception {
		ConnectionManager.Watchdog watchdog = connectionManager.newWatchdog();
		Method m = ConnectionManager.Watchdog.class.getDeclaredMethod("executeWatchdog");
		m.setAccessible(true);
		m.invoke(watchdog);

		assertTrue("All connections should be removed due to IOExceptions", getServices(connectionManager).isEmpty());
	}

	protected Map<String, XMPPIOService> getServices(ConnectionManager connectionManager) throws Exception {
		Field f = ConnectionManager.class.getDeclaredField("services");
		f.setAccessible(true);
		return (Map<String, XMPPIOService>) f.get(connectionManager);
	}

	private enum Type {
		inactivity,
		timeout
	}
}
