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
package tigase.auth;

import tigase.auth.credentials.Credentials;
import tigase.db.beans.AuthRepositoryMDPoolBean;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.core.Kernel;
import tigase.xmpp.jid.BareJID;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Bean(name = "credentialsEncoders", parent = AuthRepositoryMDPoolBean.AuthRepositoryConfigBean.class, active = true)
public class CredentialsEncoderBean
		implements RegistrarBean {

	@Inject
	private List<Credentials.Encoder> encoders;

	public List<String> getSupportedMechanisms() {
		return encoders.stream().map(enc -> enc.getName()).collect(Collectors.toList());
	}

	public String encode(BareJID user, String mechanism, String password) throws NoSuchAlgorithmException {
		for (Credentials.Encoder encoder : encoders) {
			if (mechanism.equals(encoder.getName())) {
				return encoder.encode(user, password);
			}
		}

		throw new NoSuchAlgorithmException("No password encoder for mechanism " + mechanism);
	}

	public List<String[]> encodeForAllMechanisms(BareJID user, String password) {
		List<String[]> entries = new ArrayList<>();
		for (Credentials.Encoder enc : encoders) {
			entries.add(new String[]{enc.getName(), enc.encode(user, password)});
		}
		return entries;
	}

	@Override
	public void register(Kernel kernel) {

	}

	@Override
	public void unregister(Kernel kernel) {

	}
}
