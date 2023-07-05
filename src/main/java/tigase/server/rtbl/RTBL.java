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
package tigase.server.rtbl;

import tigase.util.Algorithms;
import tigase.xmpp.jid.BareJID;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

public class RTBL {

	private static final Logger logger = Logger.getLogger(RTBL.class.getCanonicalName());
	private final RTBLRepository.Key key;
	private final String hash;

	private final CopyOnWriteArraySet<String> blockedHashes;

	public RTBL(RTBLRepository.Key key, String hash) {
		this(key, hash, new CopyOnWriteArraySet<>());
	}
	
	public RTBL(RTBLRepository.Key key, String hash, Set<String> hashes) {
		this.key = key;
		this.hash = hash;
		this.blockedHashes = new CopyOnWriteArraySet<>(hashes);
	}

	public RTBL(BareJID jid, String node, String hash, Set<String> hashes) {
		this(new RTBLRepository.Key(jid, node), hash, hashes);
	}

	public BareJID getJID() {
		return key.getJid();
	}

	public String getNode() {
		return key.getNode();
	}

	public RTBLRepository.Key getKey() {
		return key;
	}

	public String getHash() {
		return hash;
	}

	public boolean isBlocked(BareJID jid) {
		try {
			MessageDigest md = MessageDigest.getInstance(hash);
			if (isBlocked(jid.getDomain(), md)) {
				return true;
			}
			md.reset();
			if (isBlocked(jid.toString(), md)) {
				return true;
			}
		} catch (NoSuchAlgorithmException e) {
			logger.warning("No hashing mechanism " + hash + " required by RTBL (" + key + ")");
		}
		return false;
	}

	private boolean isBlocked(String jid, MessageDigest md) {
		String hash = Algorithms.bytesToHex(md.digest(jid.getBytes(StandardCharsets.UTF_8)));
		return blockedHashes.contains(hash);
	}

	public Set<String> getBlocked() {
		return blockedHashes;
	}

	@Override
	public String toString() {
		return key.toString() + ", hash: " + hash + ", blocked: " + blockedHashes;
	}
}
