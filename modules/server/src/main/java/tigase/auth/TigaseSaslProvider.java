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

import java.security.Provider;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.sasl.SaslServerFactory;

/**
 * Describe class TigaseSaslProvider here.
 * 
 * Created: Sun Nov 5 22:31:20 2006
 * 
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public class TigaseSaslProvider extends Provider {

	public static final String FACTORY_KEY = "factory";

	private static final String INFO = "This is tigase provider (provides Tigase server specific mechanisms)";

	private static final Logger log = Logger.getLogger(TigaseSaslProvider.class.getName());

	private static final String MY_NAME = "tigase.sasl";

	private static final long serialVersionUID = 1L;

	private static final double VERSION = 1.0;

	@SuppressWarnings("unchecked")
	public TigaseSaslProvider(Map<String, Object> settings) {
		super(MY_NAME, VERSION, INFO);

		Class<? extends SaslServerFactory> facClass;
		if (settings.containsKey(FACTORY_KEY)) {
			try {
				facClass = (Class<? extends SaslServerFactory>) Class.forName(settings.get(FACTORY_KEY).toString());
			} catch (ClassNotFoundException e) {
				log.log(Level.SEVERE, "Unknown factory class", e);
				throw new RuntimeException(e);
			}
		} else
			facClass = tigase.auth.mechanisms.TigaseSaslServerFactory.class;

		try {
			SaslServerFactory tmp = facClass.newInstance();
			String[] mech = tmp.getMechanismNames(new HashMap<String, Object>());
			for (String name : mech) {
				log.config("Registering SASL mechanism '" + name + "' with factory " + facClass.getName());
				putService(new Provider.Service(this, "SaslServerFactory", name, facClass.getName(), null, null));
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Can't instantiate factory", e);
			throw new RuntimeException(e);
		}
	}
}
