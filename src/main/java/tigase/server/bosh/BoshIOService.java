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
package tigase.server.bosh;

import tigase.server.Packet;
import tigase.server.xmppclient.XMPPIOProcessor;
import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.StreamError;
import tigase.xmpp.XMPPIOService;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Describe class BoshIOService here.
 * <br>
 * Created: Tue Jun 5 22:33:18 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class BoshIOService
		extends XMPPIOService<Object> {

	public static final String EOL = "\r\n";
	private static final String CONNECTION = "Connection: ";
	private static final String CONTENT_TYPE_HEADER = "Content-Type: ";
	private static final String CONTENT_TYPE_LENGTH = "Content-Length: ";
	private static final Logger log = Logger.getLogger(BoshIOService.class.getName());
	private static final String HTTP_OK_RESPONSE = "HTTP/1.1 200 OK" + EOL;
	private static final String SERVER = "Server: Tigase Bosh/" + tigase.server.XMPPServer.getImplementationVersion();
	private static final char[] HTTP_CLIENT_ACCESS_POLICY_REQUEST_HEADER = "GET /clientaccesspolicy.xml".toCharArray();

	private final ConfigProvider configProvider;
	private String content_type = "text/xml; charset=utf-8";
	private boolean firstPassCORS = true;
	private boolean firstPassClientAccessPolicy = true;
	private long rid = -1;
	private UUID sid = null;
	private BoshTask waitTimer = null;

	public BoshIOService(ConfigProvider configProvider) {
		super();
		this.configProvider = configProvider;
	}

	public long getRid() {
		return this.rid;
	}

	public void setRid(long rid) {
		this.rid = rid;
	}

	public UUID getSid() {
		return this.sid;
	}

	public void setSid(UUID sid) {
		this.sid = sid;
	}

	public BoshTask getWaitTimer() {
		return waitTimer;
	}

	public void setWaitTimer(BoshTask timer) {
		waitTimer = timer;
	}

	public void sendErrorAndStop(Authorization errorCode, StreamError streamError, Packet packet, String errorMsg)
			throws IOException {
		for (XMPPIOProcessor proc : processors) {
			proc.streamError(this, streamError);
		}
		String code = "<body type='terminate'" + " condition='" +
				(streamError != null ? streamError.getCondition() : errorCode.getCondition()) + "'" +
				" xmlns='http://jabber.org/protocol/httpbind'/>";

		try {
			Packet error = errorCode.getResponseMessage(packet, errorMsg, false);

			code = error.getElement().toString();
		} catch (PacketErrorTypeException e) {

			// ignore
		}

		StringBuilder sb = new StringBuilder(200);

		sb.append("HTTP/1.1 ").append(errorCode.getErrorCode()).append(" ");
		sb.append(errorMsg).append(EOL);
		sb.append(CONTENT_TYPE_HEADER).append(content_type).append(EOL);
		sb.append(CONTENT_TYPE_LENGTH).append(code.getBytes().length).append(EOL);
		String extra_headers = configProvider.getExtraHeaders();
		if (extra_headers != null) {
			sb.append(extra_headers);
		}

		// sb.append("X-error-code").append(code).append(EOL);
		sb.append(CONNECTION + "close" + EOL);
		sb.append(SERVER).append(EOL);
		sb.append(EOL);
		sb.append(code);
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Writing to socket:\n{0}", sb.toString());
		}
		super.writeRawData(sb.toString());
		stop();
	}

	public void setContentType(String ct) {
		this.content_type = ct;
	}

	public StringBuilder prepareHeaders(String data) {
		StringBuilder sb = new StringBuilder(200);

		sb.append(HTTP_OK_RESPONSE);
		sb.append(CONTENT_TYPE_HEADER).append(content_type).append(EOL);
		if (data != null) {
			sb.append(CONTENT_TYPE_LENGTH).append(data.getBytes().length).append(EOL);
		} else {
			sb.append(CONTENT_TYPE_LENGTH).append("0").append(EOL);
		}
		String extra_headers = configProvider.getExtraHeaders();
		if (extra_headers != null) {
			sb.append(extra_headers);
		}
		sb.append(SERVER).append(EOL);
		sb.append(EOL);

		return sb;
	}

	@Override
	public void writeRawData(String data) throws IOException {
		if ((data != null) && data.startsWith("<body")) {
			StringBuilder sb = prepareHeaders(data);

			sb.append(data);
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Writing to socket:\n{0}", sb.toString());
			}
			super.writeRawData(sb.toString());
		} else {
			super.writeRawData(data);
		}
		if (configProvider.isCloseConnections()) {
			stop();
		}
	}

	@Override
	public boolean checkData(char[] data) throws IOException {
		// we need to check this every time as Webkit based browser are reusing
		// existing connections and repeat this CORS request every 10 minutes
		//if (firstPassCORS && 
		if ((data != null) && (data.length > 7)) {
			if ((data[0] == 'O') && (data[1] == 'P') && (data[2] == 'T') && (data[3] == 'I') && (data[4] == 'O') &&
					(data[5] == 'N') && (data[6] == 'S')) {

				// responding with headers - needed for Chrome browser
				this.writeRawData(prepareHeaders(null).toString());

				// connection needs to be closed as in other case data headers are not sent to browser
				// until connection is closed and for OPTIONS request we are not sending any data
				//firstPassCORS = false;

				return false;
			}
			//firstPassCORS = false;
		}
		if (firstPassClientAccessPolicy && (data != null) &&
				(data.length >= HTTP_CLIENT_ACCESS_POLICY_REQUEST_HEADER.length)) {
			if ((data[0] == 'G') && (data[1] == 'E') && (data[2] == 'T')) {
				boolean ok = true;
				int i = 3;

				while (ok && (i < HTTP_CLIENT_ACCESS_POLICY_REQUEST_HEADER.length)) {
					ok &= (data[i] == HTTP_CLIENT_ACCESS_POLICY_REQUEST_HEADER[i]);
					i++;
				}
				if (ok) {
					String client_access_policy = configProvider.getClientAccessPolicy();
					this.writeRawData(prepareHeaders(client_access_policy).toString() + client_access_policy);

					// connection needs to be closed as in other case data headers are not sent to browser
					// until connection is closed
					firstPassClientAccessPolicy = false;

					return true;
				}
			}
			firstPassClientAccessPolicy = false;
		}

		// by default do nothing and return false
		return false;
	}

	public interface ConfigProvider {

		boolean isCloseConnections();

		String getExtraHeaders();

		String getClientAccessPolicy();

	}
}

