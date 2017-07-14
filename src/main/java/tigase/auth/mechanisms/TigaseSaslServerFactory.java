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

package tigase.auth.mechanisms;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import javax.security.sasl.SaslServerFactory;
import java.util.Map;

public class TigaseSaslServerFactory implements SaslServerFactory {

	public static final String ANONYMOUS_MECHANISM_ALLOWED = "anonymous-mechanism-allowed";

	public TigaseSaslServerFactory() {
	}

	@Override
	public SaslServer createSaslServer(final String mechanism, final String protocol, final String serverName,
									   final Map<String, ?> props, final CallbackHandler callbackHandler) throws SaslException {
		switch (mechanism) {
			case SaslSCRAM.NAME:
				return new SaslSCRAM(props, callbackHandler);
			case SaslSCRAMPlus.NAME:
				return new SaslSCRAMPlus(props, callbackHandler);
			case "PLAIN":
				return new SaslPLAIN(props, callbackHandler);
			case "ANONYMOUS":
				return new SaslANONYMOUS(props, callbackHandler);
			case "EXTERNAL":
				return new SaslEXTERNAL(props, callbackHandler);
			default:
				throw new SaslException("Mechanism not supported yet.");
		}
	}

	@Override
	public String[] getMechanismNames(Map<String, ?> props) {
		return new String[]{
//				SaslSCRAMPlus.NAME,
SaslSCRAM.NAME,
"PLAIN",
"EXTERNAL",
"ANONYMOUS",
};
	}

}
