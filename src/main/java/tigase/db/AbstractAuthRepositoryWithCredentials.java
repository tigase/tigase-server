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
package tigase.db;

import tigase.auth.CredentialsDecoderBean;
import tigase.auth.CredentialsEncoderBean;
import tigase.auth.credentials.Credentials;
import tigase.auth.credentials.entries.PlainCredentialsEntry;
import tigase.xmpp.jid.BareJID;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractAuthRepositoryWithCredentials
		implements AuthRepository {

	private static final Logger log = Logger.getLogger(AbstractAuthRepositoryWithCredentials.class.getCanonicalName());

	private CredentialsDecoderBean credentialsDecoder;
	private CredentialsEncoderBean credentialsEncoder;

	@Override
	public String getPassword(BareJID user) throws UserNotFoundException, TigaseDBException {
		Credentials credentials = getCredentials(user, Credentials.DEFAULT_USERNAME);
		if (credentials != null) {
			Credentials.Entry entry = credentials.getEntryForMechanism("PLAIN");
			if (entry != null && entry instanceof PlainCredentialsEntry) {
				return ((PlainCredentialsEntry) entry).getPassword();
			} else {
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "No password in plaintext stored for user {0}, returning null...", user);
				}
			}
		}
		return null;
	}

	@Override
	public boolean isMechanismSupported(String domain, String mechanism) {
		if ("PLAIN".equals(mechanism)) {
			return true;
		}
		if (mechanism.endsWith("-PLUS")) {
			mechanism = mechanism.substring(0, mechanism.length() - "-PLUS".length());
		}
		return credentialsDecoder.getSupportedMechanisms().contains(mechanism);
	}

	@Override
	public void setCredentialsCodecs(CredentialsEncoderBean encoder, CredentialsDecoderBean decoder) {
		this.credentialsEncoder = encoder;
		this.credentialsDecoder = decoder;
	}

	protected CredentialsDecoderBean getCredentialsDecoder() {
		return credentialsDecoder;
	}

	protected CredentialsEncoderBean getCredentialsEncoder() {
		return credentialsEncoder;
	}
}
