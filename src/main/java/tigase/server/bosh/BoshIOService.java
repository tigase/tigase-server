/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.server.bosh;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;

import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.XMPPIOService;

//~--- JDK imports ------------------------------------------------------------

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.TimerTask;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class BoshIOService here.
 * 
 * 
 * Created: Tue Jun 5 22:33:18 2007
 * 
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class BoshIOService extends XMPPIOService<Object> {

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(BoshIOService.class.getName());
	public static final String BOSH_CLOSE_CONNECTION_PROP_KEY = "bosh-close-connection";
	public static final String BOSH_EXTRA_HEADERS_FILE_PROP_KEY = "bosh-extra-headers-file";
	public static final String BOSH_EXTRA_HEADERS_FILE_PROP_VAL =
			"etc/bosh-extra-headers.txt";
	private static final String EOL = "\r\n";
	private static final String HTTP_OK_RESPONSE = "HTTP/1.1 200 OK" + EOL;
	private static final String CONTENT_TYPE_HEADER = "Content-Type: ";
	private static final String CONTENT_TYPE_LENGTH = "Content-Length: ";
	private static final String CONNECTION = "Connection: ";
	private static final String SERVER = "Server: Tigase Bosh/"
			+ tigase.server.XMPPServer.getImplementationVersion();

	// ~--- fields ---------------------------------------------------------------

	private String content_type = "text/xml; charset=utf-8";
	private static String extra_headers = null;
	private long rid = -1;
	private UUID sid = null;
	private BoshTask waitTimer = null;

	private static Boolean closeConnections;

	public BoshIOService() {
		super();
		if (closeConnections == null) {
			closeConnections =
					Boolean.parseBoolean(System
							.getProperty(BOSH_CLOSE_CONNECTION_PROP_KEY, "false"));
		}
		if (extra_headers == null) {
			String file_name =
					System.getProperty(BOSH_EXTRA_HEADERS_FILE_PROP_KEY,
							BOSH_EXTRA_HEADERS_FILE_PROP_VAL);
			try {
				BufferedReader br = new BufferedReader(new FileReader(file_name));
				String line = br.readLine();
				StringBuilder sb = new StringBuilder();
				while (line != null) {
					sb.append(line).append(EOL);
					line = br.readLine();
				}
				br.close();
				extra_headers = sb.toString();
			} catch (Exception ex) {
				log.log(Level.WARNING, "Problem reading Bosh extra headers file: " + file_name,
						ex);
			}
		}
	}

	// ~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 * 
	 * 
	 * @return
	 */
	public long getRid() {
		return this.rid;
	}

	/**
	 * Method description
	 * 
	 * 
	 * @return
	 */
	public UUID getSid() {
		return this.sid;
	}

	public void setWaitTimer(BoshTask timer) {
		waitTimer = timer;
	}

	public BoshTask getWaitTimer() {
		return waitTimer;
	}

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 * 
	 * 
	 * @param errorCode
	 * @param packet
	 * @param errorMsg
	 * 
	 * @throws IOException
	 */
	public void sendErrorAndStop(Authorization errorCode, Packet packet, String errorMsg)
			throws IOException {
		String code =
				"<body type='terminate'" + " condition='item-not-found'"
						+ " xmlns='http://jabber.org/protocol/httpbind'/>";

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

	// ~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 * 
	 * 
	 * @param ct
	 */
	public void setContentType(String ct) {
		this.content_type = ct;
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param rid
	 */
	public void setRid(long rid) {
		this.rid = rid;
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param sid
	 */
	public void setSid(UUID sid) {
		this.sid = sid;
	}

	public StringBuilder prepareHeaders(String data) {
		StringBuilder sb = new StringBuilder(200);

		sb.append(HTTP_OK_RESPONSE);
		sb.append(CONTENT_TYPE_HEADER).append(content_type).append(EOL);
		if (data != null) {
			sb.append(CONTENT_TYPE_LENGTH).append(data.getBytes().length).append(EOL);
		}
		if (extra_headers != null) {
			sb.append(extra_headers);
		}
		sb.append(SERVER).append(EOL);
		sb.append(EOL);

		return sb;
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param data
	 * 
	 * @throws IOException
	 */
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
		if (closeConnections)
			stop();
	}

	@Override
	public boolean checkData(char[] data) throws IOException {
		if (data != null && data.length > 7 && data[0] == 'O') {
			if (data[1] == 'P' && data[2] == 'T' && data[3] == 'I' && data[4] == 'O'
					&& data[5] == 'N' && data[6] == 'S') {

				// responding with headers - needed for Chrome browser
				this.writeRawData(prepareHeaders(null).toString());
			}
		}
		// by default do nothing and return false
		return false;
	}

}

// ~ Formatted in Sun Code Convention

// ~ Formatted by Jindent --- http://www.jindent.com
