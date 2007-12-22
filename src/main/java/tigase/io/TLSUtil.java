/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.io;

import java.util.Map;
import java.util.HashMap;
import javax.net.ssl.SSLContext;
import java.util.logging.Logger;
import java.util.logging.Level;

import static tigase.io.SSLContextContainerIfc.*;

/**
 * Describe class TLSUtil here.
 *
 *
 * Created: Mon Jan 23 14:21:31 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class TLSUtil {

  private static final Logger log = Logger.getLogger("tigase.io.TLSUtil");

	private static Map<String, SSLContextContainerIfc> sslContexts =
		new HashMap<String, SSLContextContainerIfc>();

  public static void configureSSLContext(String id, Map<String, String> params) {
		String sslCC_class = params.get(SSL_CONTAINER_CLASS_KEY);
		if (sslCC_class == null) {
			sslCC_class = SSL_CONTAINER_CLASS_VAL;
		}
		try {
			SSLContextContainerIfc sslCC =
				(SSLContextContainerIfc)Class.forName(sslCC_class).newInstance();
			sslCC.init(params);
			sslContexts.put(id, sslCC);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Can not initialize SSL Container: " + sslCC_class, e);
		}
  }

//   public static void configureSSLContext(String id,
// 		String k_store, String k_passwd, String def_cert_alias) {
// 		SSLContextContainer sslCC =
// 			new SSLContextContainer(k_store, k_passwd, def_cert_alias);
// 		sslContexts.put(id, sslCC);
// 	}

//   public static void configureSSLContext(String id) {
// 		SSLContextContainer sslCC =	new SSLContextContainer();
// 		sslContexts.put(id, sslCC);
// 	}

	public static SSLContext getSSLContext(String id, String protocol,
		String hostname) {
		return sslContexts.get(id).getSSLContext(protocol, hostname);
	}

} // TLSUtil
