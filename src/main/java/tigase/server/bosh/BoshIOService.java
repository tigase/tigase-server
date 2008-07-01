/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.server.bosh;

import java.util.logging.Logger;
import java.io.IOException;
import java.util.UUID;
import tigase.xmpp.XMPPIOService;
import tigase.server.Packet;
import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;

/**
 * Describe class BoshIOService here.
 *
 *
 * Created: Tue Jun  5 22:33:18 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class BoshIOService extends XMPPIOService {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.server.bosh.BoshIOService");

	private UUID sid = null;
	private long rid = -1;

	private static final String EOL = "\r\n";
	private static final String HTTP_OK_RESPONSE = "HTTP/1.1 200 OK" + EOL;
	private static final String CONTENT_TYPE_HEADER = "Content-Type: ";
	private static final String CONTENT_TYPE_LENGTH = "Content-Length: ";
	private static final String CONNECTION = "Connection: ";
	private static final String SERVER = "Server: Tigase Bosh/"
    + tigase.server.XMPPServer.getImplementationVersion();

	private String content_type = "text/xml; charset=utf-8";

	public void setContentType(String ct) {
		this.content_type = ct;
	}

	public void writeRawData(String data) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append(HTTP_OK_RESPONSE);
		sb.append(CONTENT_TYPE_HEADER + content_type + EOL);
		sb.append(CONTENT_TYPE_LENGTH + data.getBytes().length + EOL);
		sb.append(CONNECTION + "close" + EOL);
		sb.append(SERVER + EOL);
		sb.append(EOL);
		sb.append(data);
		log.finest("Writing to socket:\n" + sb.toString());
		super.writeRawData(sb.toString());
	}

	public void sendErrorAndStop(Authorization errorCode,
		Packet packet, String errorMsg) throws IOException {
		String code = "<body type='terminate'"
      + " condition='item-not-found'"
      + " xmlns='http://jabber.org/protocol/httpbind'/>";
		try {
			Packet error = errorCode.getResponseMessage(packet, errorMsg, false);
			code = error.getElement().toString();
		} catch (PacketErrorTypeException e) {
			// ignore
		}
		StringBuilder sb = new StringBuilder();
		sb.append("HTTP/1.1 " + errorCode.getErrorCode() + " " + errorMsg + EOL);
		sb.append(CONTENT_TYPE_HEADER + content_type + EOL);
		sb.append(CONTENT_TYPE_LENGTH + code.getBytes().length + EOL);
		sb.append(CONNECTION + "close" + EOL);
		sb.append(SERVER + EOL);
		sb.append(EOL);
		sb.append(code);
		log.finest("Writing to socket:\n" + sb.toString());
		super.writeRawData(sb.toString());
		stop();
	}

	public void setSid(UUID sid) {
		this.sid = sid;
	}

	public UUID getSid() {
		return this.sid;
	}

	public void setRid(long rid) {
		this.rid = rid;
	}

	public long getRid() {
		return this.rid;
	}

}
