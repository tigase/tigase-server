/*
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
package tigase.io;

import tigase.stats.StatisticsList;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static tigase.util.Algorithms.bytesToHex;

/**
 * Implementation of Proxy (v1 and v2) protocol decoding to obtain remote client IP address
 */
public class ProxyIO implements IOInterface {

	private static final Logger log = Logger.getLogger(ProxyIO.class.getName());

	private static final byte[] SIGNATURE_1 = "PROXY".getBytes(StandardCharsets.UTF_8);
	private static final byte[] SIGNATURE_2 = new byte[] { 0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A };

	private enum State {
		NEW,
		PROXY_1,
		PROXY_2,
		DONE
	}

	enum Family {
		UNSPECIFIED(-1),
		INET(4),
		INET6(16),
		UNIX(108);

		private int addrLen;

		private	Family(int addrLen) {
			this.addrLen = addrLen;
		}

		public int getAddressLength() {
			return addrLen;
		}

		public InetAddress getByAddress(byte[] addr) throws IOException {
			return switch (this) {
				case INET -> InetAddress.getByAddress(addr);
				case INET6 -> Inet6Address.getByAddress(addr);
				default -> throw new IOException("Unsupported socket address");
			};
		}
	}
	enum Transport {
		UNSPECIFIED,
		STREAM,
		DATAGRAM
	}

	private static final int PROXY_1_FIELD_PROTOCOL = 1;
	private static final int PROXY_1_FIELD_SRC_IP = 2;
	private static final int PROXY_1_FIELD_DST_IP = 3;
	private static final int PROXY_1_FIELD_SRC_PORT = 4;
	private static final int PROXY_1_FIELD_DST_PORT = 5;

	private final IOInterface io;
	private State state = State.NEW;
	private byte[] partialData = null;
	private BiConsumer<String,String> addressConsumer = null;

	// Consumer<DestinationAddress,SourceAddress> for incoming connections
	public ProxyIO(IOInterface io, BiConsumer<String,String> addressConsumer) {
		this.io = io;
		this.partialData = null;
		this.addressConsumer = addressConsumer;
	}

	@Override
	public int bytesRead() {
		return io.bytesRead();
	}

	@Override
	public boolean checkCapabilities(String caps) {
		return caps.contains("PROXY") || io.checkCapabilities(caps);
	}

	@Override
	public int getInputPacketSize() throws IOException {
		return io.getInputPacketSize();
	}

	@Override
	public SocketChannel getSocketChannel() {
		return io.getSocketChannel();
	}

	@Override
	public void getStatistics(StatisticsList list, boolean reset) {
		io.getStatistics(list, reset);
	}

	@Override
	public long getBytesSent(boolean reset) {
		return io.getBytesSent(reset);
	}

	@Override
	public long getTotalBytesSent() {
		return io.getTotalBytesSent();
	}

	@Override
	public long getBytesReceived(boolean reset) {
		return io.getBytesReceived(reset);
	}

	@Override
	public long getTotalBytesReceived() {
		return io.getTotalBytesReceived();
	}

	@Override
	public long getBuffOverflow(boolean reset) {
		return io.getBuffOverflow(reset);
	}

	@Override
	public long getTotalBuffOverflow() {
		return io.getTotalBuffOverflow();
	}

	@Override
	public boolean isConnected() {
		return io.isConnected();
	}

	@Override
	public boolean isRemoteAddress(String addr) {
		return io.isRemoteAddress(addr);
	}

	@Override
	public ByteBuffer read(ByteBuffer buff) throws IOException {
		if (state != State.DONE) {
			ByteBuffer tmpBuffer = io.read(buff);
			ByteBuffer buf;
			if (partialData == null) {
				buf = tmpBuffer;
			} else {
				buf = ByteBuffer.allocate(partialData.length + tmpBuffer.remaining());
				buf.put(partialData);
				buf.put(tmpBuffer);
				buf.flip();
				tmpBuffer.clear();
				partialData = null;
			}

			buf.mark();
			if (state == State.NEW) {
				boolean proxy1Checked = false;
				boolean proxy2Checked = false;
				if (buf.remaining() >= SIGNATURE_1.length) {
					proxy1Checked = true;
					if (IntStream.range(0, SIGNATURE_1.length).allMatch(i -> buf.get(i) == SIGNATURE_1[i])) {
						state = State.PROXY_1;
					}
				}
				if (buf.remaining() >= SIGNATURE_2.length) {
					proxy2Checked = true;
					if (IntStream.range(0, SIGNATURE_2.length).allMatch(i -> buf.get(i) == SIGNATURE_2[i])) {
						state = State.PROXY_2;
					}
				}

				if (proxy1Checked && proxy2Checked) {
					if (state == State.NEW) {
						state = State.DONE;
						return buf;
					}
				} else {
					partialData = new byte[buf.remaining()];
					buf.get(partialData);
					return tmpBuffer;
				}
			}
			return switch (state) {
				case PROXY_1 ->  {
					String[] fields = new String[6];
					int fieldIndex = 0;
					StringBuilder builder = new StringBuilder();
					boolean awaitLineFeed = false;
					while (buf.hasRemaining()) {
						byte b = buf.get();
						if (!awaitLineFeed) {
							if (b == ' ' || b == '\r') {
								fields[fieldIndex] = builder.toString();
								fieldIndex++;
								builder.setLength(0);
								if (b == '\r') {
									awaitLineFeed = true;
								}
							} else if (b < ' ') {
								state = State.DONE;
								buf.reset();
								yield buf;
							} else {
								builder.append((char) b);
							}
						} else {
							if (b == '\n') {
								// upgrade state!!
								if (!"PROXY".equals(fields[0])) {
									log.log(Level.FINE, () -> "Not a PROXY protocol!");
									state = State.DONE;
									buf.reset();
									yield buf;
								}
								if ("UNKNOWN".equals(fields[PROXY_1_FIELD_PROTOCOL])) {
									state = State.DONE;
									yield buf;
								}

								if (addressConsumer != null) {
									addressConsumer.accept(fields[PROXY_1_FIELD_DST_IP], fields[PROXY_1_FIELD_SRC_IP]);
								}

								yield buf;
							} else {
								buf.reset();
								partialData = new byte[buf.remaining()];
								buf.get(partialData);
								yield tmpBuffer;
							}
						}
					}

					// PROXY1 had incomplete data
					buf.reset();
					partialData = new byte[buf.remaining()];
					buf.get(partialData);
					yield tmpBuffer;
				}

				case PROXY_2 -> {
					if (proxy2Header == null) {
						buf.position(buf.position() + SIGNATURE_2.length);
						if (buf.remaining() < 4) {
							buf.reset();
							partialData = new byte[buf.remaining()];
							buf.get(partialData);
							yield tmpBuffer;
						}

						int verAndCmd = 0xFF & buf.get();
						if ((verAndCmd & 0xF0) != 0x20) {
							log.log(Level.FINE, () -> "Bad Proxy v2 version");
							state = State.DONE;
							buf.reset();
							yield buf;
						}
						boolean isLocal = (verAndCmd & 0xF0) == 0x00;

						int transportAndFamily = 0xFF & buf.get();
						Family family = switch (transportAndFamily >> 4) {
							case 0 -> Family.UNSPECIFIED;
							case 1 -> Family.INET;
							case 2 -> Family.INET6;
							case 3 -> Family.UNIX;
							default -> throw new IOException("Bad Proxy Family value");
						};
						Transport transport = switch (transportAndFamily & 0xF) {
							case 0 -> Transport.UNSPECIFIED;
							case 1 -> Transport.STREAM;
							case 2 -> Transport.DATAGRAM;
							default -> throw new IOException("Bad Proxy Transport value");
						};

						if (!isLocal && (family == Family.UNSPECIFIED || transport != Transport.STREAM)) {
							throw new IOException("Unsupported Proxy mode");
						}

						int length = buf.getChar();
						if (length > 1024) {
							throw new IOException("Unsupported Proxy header length - too long");
						}
						proxy2Header = new Proxy2Header(transport, family, isLocal, length);
						buf.mark();
					}
					
					if (buf.remaining() < proxy2Header.length) {
						buf.reset();
						partialData = new byte[buf.remaining()];
						buf.get(partialData);
						yield tmpBuffer;
					}

					int nonProxyRemaining = buf.remaining() - proxy2Header.length;
					if (proxy2Header.isLocal) {
						buf.position(buf.position() + proxy2Header.length);
						// no content for local and remote address
					} else {
						int dataLength = proxy2Header.family.getAddressLength();
						if (dataLength < 0) {
							throw new IOException("Unsupported socket address");
						}

						byte[] data = new byte[dataLength];
						buf.get(data);
						InetAddress srcAddr = proxy2Header.family.getByAddress(data);
						buf.get(data);
						InetAddress dstAddr = proxy2Header.family.getByAddress(data);
						int srcPort = buf.getChar();
						int dstPort = buf.getChar();

						InetSocketAddress local = new InetSocketAddress(dstAddr, dstPort);
						InetSocketAddress remote = new InetSocketAddress(srcAddr, srcPort);

						if (addressConsumer != null) {
							addressConsumer.accept(local.getHostString(), remote.getHostString());
						}
					}

					while (buf.remaining() > nonProxyRemaining) {
						int type = 0xff & buf.get();
						int length = buf.getChar();
						byte[] value = new byte[length];
						buf.get(value);

						log.log(Level.FINEST, () -> String.format("Proxy v2 T=%x L=%d V=%s for %s", type, length, bytesToHex(value), this));
					}

					state = State.DONE;
					yield buf;
				}

				default -> {
					yield buf;
				}
			};
		} else {
			return io.read(buff);
		}
	}

	private Proxy2Header proxy2Header;

	record Proxy2Header(Transport transport, Family family, boolean isLocal, int length) {
		
	}
	
	@Override
	public void stop() throws IOException {
		io.stop();
	}

	@Override
	public boolean waitingToSend() {
		return io.waitingToSend();
	}

	@Override
	public int waitingToSendSize() {
		return io.waitingToSendSize();
	}

	@Override
	public int write(ByteBuffer buff) throws IOException {
		return io.write(buff);
	}

	@Override
	public void setLogId(String logId) {
		io.setLogId(logId);
	}
}
