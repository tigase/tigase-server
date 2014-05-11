/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2012 "Andrzej Wójcik" <andrzej.wojcik@tigase.org>
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
 * Last modified by $Author: andrzej $
 */
package tigase.osgi;

import tigase.conf.Configurable;
import tigase.xmpp.XMPPImplIfc;

/**
 *
 * @author andrzej
 */
public interface ModulesManager {

		void registerClass(Class<?> cls);
		
		void unregisterClass(Class<?> cls);
		
        void registerPluginClass(Class<? extends XMPPImplIfc> pluginCls);

        void unregisterPluginClass(Class<? extends XMPPImplIfc> pluginCls);

        void registerServerComponentClass(Class<? extends Configurable> compCls);

        void unregisterServerComponentClass(Class<? extends Configurable> compCls);

        void update();
        
		Class<?> forName(String className) throws ClassNotFoundException;
		
}
