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
package tigase.server.xmppclient;

import tigase.io.TLSIOIfc;
import tigase.net.IOService;
import tigase.net.SocketThread;
import tigase.server.Packet;
import tigase.xmpp.XMPPIOService;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class C2SIOService<RefObject>
		extends XMPPIOService<RefObject> {

	private static final Logger log = Logger.getLogger(C2SIOService.class.getCanonicalName());

	private AtomicInteger waitForResponse = new AtomicInteger(0);
	private boolean pipelining = true;

	private Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

	private byte[] tlsData = null;

	@Override
	protected void addReceivedPacket(Packet packet) {
		if (pipelining) {
			synchronized (tasks) {
				if (isWaitingForResponse()) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "queuing received packet as a task " + packet);
					}
					boolean result = tasks.offer(() -> {
						waitForResponse.incrementAndGet();
						super.addReceivedPacket(packet);
					});
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "queued (" + result + ") received packet as a task " + packet);
					}
				} else {
					super.addReceivedPacket(packet);
				}
			}
		} else {
			super.addReceivedPacket(packet);
		}
	}

	@Override
	public boolean waitingToRead() {
		return tlsData == null && super.waitingToRead();
	}

	@Override
	public void processWaitingPackets() throws IOException {
		if (pipelining) {
			boolean hadPackets = !this.getWaitingPackets().isEmpty();
			super.processWaitingPackets();
			if (hadPackets && this.getWaitingPackets().isEmpty()) {
				runQueuedTaskIfExists();
			}
		} else {
			super.processWaitingPackets();
		}
	}
	
	public void waitForResponse() {
		waitForResponse.incrementAndGet();
	}

	public boolean isWaitingForResponse() {
		return !tasks.isEmpty() || waitForResponse.get() > 0;
	}

	public void queueTask(Runnable run) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "queuing task " + run);
		}
		moveParsedPacketsToReceived(false);
		tasks.offer(run);
	}

//	@Override
//	public void setUserJid(String jid) {
//		super.setUserJid(jid);
//		if (jid != null) {
//			// in this case user is authenticated and we can finish pipelining
//			this.pipelining = false;
//			//this.waitForResponse = false;
//		}
//	}

	public boolean shouldQueueStreamOpened() {
		boolean value = hasParsedElements();
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "checking if should queue stream opened = " + value);
		}
		return value;
	}

	@Override
	protected boolean handleMalformedInput(ByteBuffer buffer, CharBuffer cb) {
		int i = 0;
		while (cb.position() + i >= 0 && cb.get(cb.position()+i) != '>') {
			i--;
		}
		if (cb.position() + i < 0) {
			cb.position(0);
		} else {
			cb.position(cb.position() + i + 1);
		}

		for (i = 0; i < buffer.limit(); i++) {
			byte b = buffer.get(i);
			switch (b) {
				case 0x16:
					// may be SSL 3.0 or newer header
					if (i + 5 < buffer.limit() && buffer.get(i+5) == 0x01) {
						// found SSL 3.0 header!!
						synchronized (this) {
							extractTlsHandshakeData(buffer, i);
						}
						return true;
					}
				case 0x01:
					if (i >= 2 && (buffer.get(i-2) & 0x80) == 0x80) {
						// found SSL 2.0 header!!
						synchronized (this) {
							extractTlsHandshakeData(buffer, i - 2);
						}
						return true;
					}
				default:
					break;
			}
		}
		log.log(Level.FINER, "Tried Not found SSL/TLS handshake, bb: {0}, contents: {1} , cb: {2}",
				new String[]{String.valueOf(buffer), Arrays.toString(buffer.array()), String.valueOf(cb.array())});
		return false;
	}

	@Override
	public void startTLS(boolean clientMode, boolean wantClientAuth, boolean needClientAuth) throws IOException {
		super.startTLS(clientMode, wantClientAuth, needClientAuth);
		if (tlsData != null && getIO() instanceof TLSIOIfc) {
			synchronized (this) {
				((TLSIOIfc) getIO()).processHandshake(tlsData);
				tlsData = null;
			}
		}
	}

	private void extractTlsHandshakeData(ByteBuffer buffer, int i) {
		buffer.position(i);
		tlsData = new byte[buffer.limit() - (i)];
		buffer.get(tlsData);
		if (getIO() instanceof TLSIOIfc) {
			try {
				((TLSIOIfc) getIO()).processHandshake(tlsData);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			tlsData = null;
		} else {
			SocketThread.removeSocketService((IOService<Object>) this);
		}
	}

	private void runQueuedTaskIfExists() {
		Runnable run;
		synchronized (tasks) {
			waitForResponse.decrementAndGet();
			run = tasks.poll();
		}
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "got task " + run + " to execute");
		}
		if (run != null) {
			run.run();
			run = tasks.peek();
		}
		
		if (run == null) {
			try {
				//waitForResponse = false;
				if (getUserJid() != null) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "finished stream initiation, disabling pipelining...");
					}
					this.pipelining = false;
				}
				if (serviceListener != null) {
					serviceListener.packetsReady(this);
				}
				//SocketThread.addSocketService(this);
			} catch (IOException ex) {
				forceStop();
			}
		}
	}
}
