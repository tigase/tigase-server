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

package tigase.server.xmppserver;

import tigase.util.log.LogFormatter;

import java.util.Arrays;
import java.util.Optional;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract class SSLTestAbstract {

	static Logger log;

	static Optional<String> getSslDebugString() {
		// https://docs.oracle.com/en/java/javase/11/security/java-secure-socket-extension-jsse-reference-guide.html#GUID-31B7E142-B874-46E9-8DD0-4E18EC0EB2CF

		StringBuilder sslDebug = new StringBuilder("ssl");
		sslDebug.append(":defaultctx"); //Print default SSL initialization
		sslDebug.append(":handshake"); //Print each handshake message
//		sslDebug.append(":keygen"); //Print key generation data
//		sslDebug.append(":keymanager"); //Print key manager tracing
//		sslDebug.append(":pluggability"); //Print pluggability tracing
//		sslDebug.append(":record"); //Enable per-record tracing
//		sslDebug.append(":respmgr"); //Print status response manager tracing
		sslDebug.append(":session"); //Print session activity
//		sslDebug.append(":sessioncache"); //Print session cache tracing
		sslDebug.append(":sslctx"); //Print SSLContext tracing
//		sslDebug.append(":trustmanager"); //Print trust manager tracing
//		sslDebug.append(":data"); //Messages generated from the handshake: Hex dump of each handshake message
		sslDebug.append(":verbose"); //Messages generated from the handshake: Verbose handshake message printing
//		sslDebug.append(":plaintext"); //Messages generated from the record: Hex dump of record plaintext
//		sslDebug.append(":packet"); //Messages generated from the record: Print raw SSL/TLS packets
		final String sslDebugPropertyString = System.getProperty("test-ssl-debug", "true");
		final boolean sslDebugProperty = Boolean.parseBoolean(sslDebugPropertyString);
		return sslDebugProperty ? Optional.of(sslDebug.toString()) : Optional.empty();
	}
}
