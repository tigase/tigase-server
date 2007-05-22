/*  Tigase Project
 *  Copyright (C) 2001-2007
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
package tigase.server.sreceiver;

import java.util.Map;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.xml.XMLUtils;

import static tigase.server.sreceiver.PropertyConstants.*;

/**
 * Describe class TaskCommandCommons here.
 *
 *
 * Created: Mon May 21 08:31:25 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class TaskCommandCommons {

	public static void propertyItems2Command(Map<String, PropertyItem> props,
		Packet result) {
		for (Map.Entry<String, PropertyItem> entry: props.entrySet()) {
			if (!entry.getKey().equals(USER_REPOSITORY_PROP_KEY)) {
				PropertyItem item = entry.getValue();
				if (item.getPossible_values() != null) {
					Command.addFieldValue(result,
						XMLUtils.escape(item.getName()),
						XMLUtils.escape(item.getValue().toString()),
						XMLUtils.escape(item.getDisplay_name()),
						item.getPossible_values(), item.getPossible_values());
				} else {
					Command.addFieldValue(result,
						XMLUtils.escape(item.getName()),
						XMLUtils.escape(item.getValue().toString()),
						"text-single", XMLUtils.escape(item.getDisplay_name()));
				} // end of if (item.getPossible_values() != null) else
			} // end of if (!entry.getKey().equals(USER_REPOSITORY_PROP_KEY))
		} // end of for (Map.Entry entry: prop.entrySet())
	}

} // TaskCommandCommons
