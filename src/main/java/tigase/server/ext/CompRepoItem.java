/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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

package tigase.server.ext;

import java.util.logging.Logger;
import tigase.db.RepositoryItem;
import tigase.net.ConnectionType;
import tigase.xml.Element;

/**
 * Created: Oct 3, 2009 4:39:51 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class CompRepoItem implements RepositoryItem {

	/**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log = Logger.getLogger(CompRepoItem.class.getName());

	//"accept:muc.domain.tld:5277:user:passwd"
	private String domain = "muc.domain.tld";
	private String remoteHost = null;
	private ConnectionType type = ConnectionType.accept;
	private int port = -1;
	private String auth_pass = "pass";
	private String xmlns = "jabber:component:accept";
	private String prop_xmlns = "accept";
	private String[] routings = { domain, ".*@" + domain, ".*\\." + domain };

	@Override
	public void initFromPropertyString(String propString) {
		String[] props = propString.split(":");
		if (props.length > 0) {
			domain = props[0];
			routings = new String[] { domain, ".*@" + domain, ".*\\." + domain };
		}
		if (props.length > 1) {
			auth_pass = props[1];
		}
		if (props.length > 2) {
			if (props[2].equals("connect")) {
				type = ConnectionType.connect;
			}
		}
		if (props.length > 3) {
			try {
				port = Integer.parseInt(props[3]);
			} catch (Exception e) {
				port = 5277;
				log.warning("Incorrect port number, can't parse: " + props[3]);
			}
		}
		if (props.length > 4) {
			remoteHost = props[4];
		}
		if (props.length > 5) {
			prop_xmlns = props[5];
			if (props[4].equals("accept")) {
				xmlns = "jabber:component:accept";
			}
			if (props[4].equals("client")) {
				xmlns = "jabber:client";
			}
			if (props[4].equals("connect")) {
				xmlns = "jabber:component:connect";
			}
		}
	}

	@Override
	public String toPropertyString() {
		return domain + ":" + auth_pass + ":" + type.name() + ":" + port + ":" +
				remoteHost + ":" + prop_xmlns;
	}

	@Override
	public void initFromElement(Element elem) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Element toElement() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getKey() {
		return domain;
	}

	public ConnectionType getConnectionType() {
		return type;
	}

	public String getDomain() {
		return domain;
	}

	public String getRemoteHost() {
		return remoteHost;
	}

	public int getPort() {
		return port;
	}

	public String getAuthPasswd() {
		return auth_pass;
	}

	public String[] getRoutings() {
		return routings;
	}

	public String getXMLNS() {
		return xmlns;
	}

}
