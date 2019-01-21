/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
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
 */
package tigase.osgi;

import tigase.annotations.TigaseDeprecated;
import tigase.conf.Configurable;
import tigase.xmpp.XMPPImplIfc;

/**
 * @author andrzej
 */
public interface ModulesManager {

	void registerClass(Class<?> cls);

	void unregisterClass(Class<?> cls);

	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	void registerPluginClass(Class<? extends XMPPImplIfc> pluginCls);

	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	void unregisterPluginClass(Class<? extends XMPPImplIfc> pluginCls);

	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	void registerServerComponentClass(Class<? extends Configurable> compCls);

	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	void unregisterServerComponentClass(Class<? extends Configurable> compCls);

	void update();

	Class<?> forName(String className) throws ClassNotFoundException;

}
