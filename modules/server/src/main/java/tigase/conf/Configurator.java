/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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
package tigase.conf;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Dec 7, 2009 4:09:52 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Configurator extends ConfiguratorAbstract {

	private static final Logger log = Logger.getLogger(Configurator.class.getCanonicalName());

	@Override
	public String getDiscoDescription() {
		return "Configuration management";
	}

	@Override
	public String getDiscoCategoryType() {
		return "generic";
	}

	@Override
	public void componentAdded(Configurable component) throws ConfigurationException {
		try {
			super.componentAdded(component);
		} catch (NullPointerException ex) {
			log.log(Level.WARNING, "ignoring NPE", ex);
		}
	}

	public void updateMessageRouter() {
		try {
			setup(getComponent("message-router"));
		} catch (Exception e) {
			log.log(Level.WARNING, "Problem instantiating component:", e);
		}	 // end of try-catch		
	}
}
