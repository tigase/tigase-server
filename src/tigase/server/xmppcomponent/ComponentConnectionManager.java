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

package tigase.server.xmppcomponent;

import tigase.stats.StatisticsContainer;
import tigase.server.AbstractMessageReceiver;
import tigase.conf.Configurable;
import tigase.server.XMPPService;
import java.util.Map;

/**
 * Class ComponentConnectionManager
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ComponentConnectionManager extends AbstractMessageReceiver
	implements StatisticsContainer, Configurable, XMPPService {

  public ComponentConnectionManager() {}

  /**
   * Get object id. This id corresponds to entry in configuration.
   */
	public String getId() { return null; }

  /**
   * Sets configuration property to object.
   */
	public void setProperty(String name, String value) {}

  /**
   * Sets all configuration properties for object.
   */
	public void setProperties() {}

  /**
   * Returns defualt configuration settings for this object.
   */
	public Map<String, String> getStatistics() { return null; }

  /**
   * Returns defualt configuration settings for this object.
   */
	public Map<String, String> getDefaults() { return null; }

}
