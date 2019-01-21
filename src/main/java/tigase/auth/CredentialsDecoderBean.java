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
import java.util.List;
import java.util.stream.Collectors;

@Bean(name = "credentialDecoders", parent = AuthRepositoryMDPoolBean.AuthRepositoryConfigBean.class, active = true)
public class CredentialsDecoderBean
		implements RegistrarBean {

	@Inject
	private List<Credentials.Decoder> decoders;

	public List<String> getSupportedMechanisms() {
		return decoders.stream().map(dec -> dec.getName()).collect(Collectors.toList());
	}

	public Credentials.Entry decode(BareJID user, String mechanism, String password) throws NoSuchAlgorithmException {
		for (Credentials.Decoder decoder : decoders) {
			if (mechanism.equals(decoder.getName())) {
				return decoder.decode(user, password);
			}
		}

		throw new NoSuchAlgorithmException("No password decoder for mechanism " + mechanism);
	}

	@Override
	public void register(Kernel kernel) {

	}

	@Override
	public void unregister(Kernel kernel) {

	}
}
