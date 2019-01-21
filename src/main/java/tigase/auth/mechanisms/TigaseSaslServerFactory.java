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
package tigase.auth.mechanisms;

import tigase.auth.TigaseSaslProvider;
import tigase.kernel.beans.Bean;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import javax.security.sasl.SaslServerFactory;
import java.util.Map;

@Bean(name = "tigaseSaslServerFactory", parent = TigaseSaslProvider.class, active = true)
public class TigaseSaslServerFactory
		implements SaslServerFactory {

	public static final String ANONYMOUS_MECHANISM_ALLOWED = "anonymous-mechanism-allowed";

	public TigaseSaslServerFactory() {
	}

	@Override
	public SaslServer createSaslServer(final String mechanism, final String protocol, final String serverName,
									   final Map<String, ?> props, final CallbackHandler callbackHandler)
			throws SaslException {
		switch (mechanism) {
			case SaslSCRAM.NAME:
				return new SaslSCRAM(props, callbackHandler);
			case SaslSCRAMPlus.NAME:
				return new SaslSCRAMPlus(props, callbackHandler);
			case SaslSCRAMSha256.NAME:
				return new SaslSCRAMSha256(props, callbackHandler);
			case SaslSCRAMSha256Plus.NAME:
				return new SaslSCRAMSha256Plus(props, callbackHandler);
			case SaslPLAIN.NAME:
				return new SaslPLAIN(props, callbackHandler);
			case SaslANONYMOUS.NAME:
				return new SaslANONYMOUS(props, callbackHandler);
			case SaslEXTERNAL.NAME:
				return new SaslEXTERNAL(props, callbackHandler);
			default:
				throw new SaslException("Mechanism not supported yet.");
		}
	}

	@Override
	public String[] getMechanismNames(Map<String, ?> props) {
		return new String[]{
//				SaslSCRAMSha256Plus.NAME,
//				SaslSCRAMPlus.NAME,
SaslSCRAMSha256.NAME, SaslSCRAM.NAME, "PLAIN", "EXTERNAL", "ANONYMOUS",};
	}

}
