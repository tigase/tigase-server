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

package tigase.stats;

import java.util.List;
import java.util.Map;

/**
 * Interface StatisticsProviderMBean
 *
 * @author kobit
 */
public interface StatisticsProviderMBean
{

//	/**
//	 * Operation exposed for management
//	 *
//	 * @return java.util.Map<String, String>
//	 */
//	public Map getAllStats();

	/**
	 * Get Attribute exposed for management
	 *
	 * @return
	 */
	public List getComponentsNames();

	/**
	 * Get Attribute exposed for management
	 *
	 * @return
	 */
	public String getName();

	/**
	 * Operation exposed for management
	 * @param param0 Statistics level, 0 - All, 500 - Medium, 800 - Minimal
	 * @return java.util.Map<String, String>
	 */
	public Map getAllStats(int param0);

	/**
	 * Operation exposed for management
	 * @param param0 The component name to provide statistics for
	 * @param param1 Statistics level, 0 - All, 500 - Medium, 800 - Minimal
	 * @return java.util.Map<String, String>
	 */
	public Map getComponentStats(String param0, int param1);
	
}


