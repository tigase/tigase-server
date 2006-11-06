/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <kobit@users.sourceforge.net>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.auth;

import java.security.AuthProvider;
import java.security.AuthProvider;
import java.security.Provider.Service;
import java.security.Provider;
import java.security.Security;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslServer;

/**
 * Describe class TigaseSaslProvider here.
 *
 *
 * Created: Sun Nov  5 22:31:20 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class TigaseSaslProvider extends Provider {

  private static final long serialVersionUID = 1L;
  private static final Logger log =
		Logger.getLogger("tigase.auth.TigaseSaslProvider");
	private static final String MY_NAME = "tigase.sasl";
	private static final double VERSION = 1.0;
	private static final String INFO =
		"This is tigase provider (implements server PLAIN mechanism)";

	/**
	 * Creates a new <code>TigaseSaslProvider</code> instance.
	 *
	 */
	public TigaseSaslProvider() {
		super(MY_NAME, VERSION, INFO);
		putService(new Provider.Service(this, "SaslServerFactory", "PLAIN",
				"tigase.auth.TigaseSaslServerFactory", null, null));
	}

	/**
	 * Describe <code>main</code> method here.
	 *
	 * @param args a <code>String[]</code> value
	 */
	public static void main(final String[] args) throws Exception {

		Security.insertProviderAt(new TigaseSaslProvider(), 6);
// 		Map<String, String> props = new TreeMap<String, String>();
// 		props.put(Sasl.QOP, "auth");
// 		SaslCallbackHandler sch =	new SaslCallbackHandler(props, null);
// 		SaslServer ss =
// 			Sasl.createSaslServer("DIGEST-MD5", "xmpp", "tigase.org", props, sch);
// 		System.out.println("Mechanism: " + ss.getMechanismName());

		Provider[] provs = Security.getProviders();
		for (Provider prov: provs) {
			System.out.println("Provider: " + prov.getName());
			System.out.println(prov.getInfo());
			Set<Provider.Service> servs = prov.getServices();
			if (servs != null) {
				for (Provider.Service serv: servs) {
					System.out.println("   Service: "
						+ "type=" + serv.getType()
						+ ", alg=" + serv.getAlgorithm()
						+ ", prov=" + serv.getProvider().getName()
						+ ", class=" + serv.getClassName()
														 );
				} // end of for (Provider.Service serv: servs)
			} // end of if (servs != null)
		} // end of for (Provider prov: provs)

	}

} // TigaseSaslProvider
