/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.io;

import java.util.Map;
import java.util.HashMap;
import javax.net.ssl.SSLContext;
import java.util.logging.Logger;

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

	private static Map<String, SSLContextContainer> sslContexts =
		new HashMap<String, SSLContextContainer>();

  public static void configureSSLContext(String id,
		String k_store, String k_passwd, String t_store, String t_passwd,
		String def_cert_alias) {
		SSLContextContainer sslCC =
			new SSLContextContainer(k_store, k_passwd, t_store, t_passwd,
				def_cert_alias);
		sslContexts.put(id, sslCC);
  }

  public static void configureSSLContext(String id,
		String k_store, String k_passwd, String def_cert_alias) {
		SSLContextContainer sslCC =
			new SSLContextContainer(k_store, k_passwd, def_cert_alias);
		sslContexts.put(id, sslCC);
	}

  public static void configureSSLContext(String id) {
		SSLContextContainer sslCC =	new SSLContextContainer();
		sslContexts.put(id, sslCC);
	}

	public static SSLContext getSSLContext(String id, String protocol,
		String hostname) {
		return sslContexts.get(id).getSSLContext(protocol, hostname);
	}

} // TLSUtil
