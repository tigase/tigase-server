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
package tigase.io;

import tigase.cert.CertCheckResult;

import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.nio.ByteBuffer;
import java.security.cert.Certificate;

public interface TLSWrapper {

	int bytesConsumed();

	void close() throws SSLException;

	int getAppBuffSize();

	CertCheckResult getCertificateStatus(boolean revocationEnabled, SSLContextContainerIfc sslContextContainer);

	SSLEngineResult.HandshakeStatus getHandshakeStatus();

	Certificate[] getLocalCertificates();

	int getNetBuffSize();

	int getPacketBuffSize();

	Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException;

	TLSStatus getStatus();

	byte[] getTlsUniqueBindingData();

	boolean isClientMode();

	boolean isNeedClientAuth();

	void setDebugId(String id);

	ByteBuffer unwrap(ByteBuffer net, ByteBuffer app) throws SSLException;

	boolean wantClientAuth();

	void wrap(ByteBuffer app, ByteBuffer net) throws SSLException;
}
