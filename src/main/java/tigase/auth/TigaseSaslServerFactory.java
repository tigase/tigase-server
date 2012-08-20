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

package tigase.auth;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import javax.security.sasl.SaslServerFactory;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class TigaseSaslServerFactory here.
 *
 *
 * Created: Mon Nov  6 09:09:53 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class TigaseSaslServerFactory implements SaslServerFactory {

	/**
	 * Creates a new <code>TigaseSaslServerFactory</code> instance.
	 *
	 */
	public TigaseSaslServerFactory() {}

	//~--- methods --------------------------------------------------------------

	// Implementation of javax.security.sasl.SaslServerFactory

	/**
	 * Describe <code>createSaslServer</code> method here.
	 *
	 * @param mechanism a <code>String</code> value
	 * @param protocol a <code>String</code> value
	 * @param serverName a <code>String</code> value
	 * @param props a <code>Map</code> value
	 * @param callbackHandler a <code>CallbackHandler</code> value
	 * @return a <code>SaslServer</code> value
	 * @exception SaslException if an error occurs
	 */
	@Override
	public SaslServer createSaslServer(final String mechanism, final String protocol,
			final String serverName, final Map<String, ?> props,
				final CallbackHandler callbackHandler)
			throws SaslException {
		if (mechanism.equals("PLAIN")) {
			return new SaslPLAIN(props, callbackHandler);
		}    // end of if (mechanism.equals("PLAIN"))

		if (mechanism.equals("ANONYMOUS")) {
			return new SaslPLAIN(props, callbackHandler);
		}    // end of if (mechanism.equals("PLAIN"))

//  if (mechanism.equals("DIGEST-MD5")) {
//    return new SaslDigestMD5(props, callbackHandler);
//  } // end of if (mechanism.equals("PLAIN"))
		throw new SaslException("Mechanism not supported yet.");
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Describe <code>getMechanismNames</code> method here.
	 *
	 * @param map a <code>Map</code> value
	 * @return a <code>String[]</code> value
	 */
	@Override
	public String[] getMechanismNames(final Map map) {
		return null;
	}
}    // TigaseSaslServerFactory


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
