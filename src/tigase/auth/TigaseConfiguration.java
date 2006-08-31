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

import java.util.HashMap;
import java.util.Map;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

/**
 * Describe class TigaseConfiguration here.
 *
 *
 * Created: Fri Feb 17 16:47:02 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class TigaseConfiguration extends Configuration {

	private Map<String, AppConfigurationEntry[]> configuration = null;
	private static TigaseConfiguration tigaseConfig = null;

	/**
	 * Creates a new <code>TigaseConfiguration</code> instance.
	 *
	 */
	private TigaseConfiguration() {
		this.configuration = new HashMap<String, AppConfigurationEntry[]>();
	}

	public AppConfigurationEntry[] getAppConfigurationEntry(final String name) {
		return configuration.get(name);
	}

	public void putAppConfigurationEntry(String name,
		AppConfigurationEntry[] entry) {
		configuration.put(name, entry);
	}

	public void refresh() {}

	public static TigaseConfiguration	getConfiguration() {
		if (tigaseConfig == null) {
			tigaseConfig = new TigaseConfiguration();
			Configuration.setConfiguration(tigaseConfig);
		} // end of if (tigaseConfig == null)
		return tigaseConfig;
	}

	} // TigaseConfiguration
