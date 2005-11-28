/*
 *  Package Tigase XMPP/Jabber Server
 *  Copyright (C) 2004, 2005, 2006
 *  "Artur Hefczyc" <artur.hefczyc@gmail.com>
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

package tigase.conf;

import tigase.server.AbstractComponentRegistrator;
import tigase.server.XMPPService;
import tigase.server.ServerComponent;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class Configurator
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Configurator extends AbstractComponentRegistrator
	implements XMPPService {

  public Configurator(String fileName) {}

  /**
   * Returns defualt configuration settings in case if there is no
   * config file.
   */
	public Map<String, String> getDefaults() {
		Map<String, String> defaults = new TreeMap<String, String>();
		defaults.put("tigase.message-router.id", "router");
		defaults.put("tigase.message-router.class",
			"tigase.server.MessageRouter");
		return defaults;
	}

}
