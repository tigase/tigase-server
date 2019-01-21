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
package tigase.kernel.beans;

import tigase.kernel.core.Kernel;

/**
 * Interface used by bean configurators to detect is additional beans should be registered
 * <br>
 * Created by andrzej on 10.03.2016.
 */
public interface BeanSelector {

	/**
	 * Method needs to return true if bean in which annotation class implementing this interface is specified and this
	 * bean should be registered
	 *
	 * @param clazz
	 * @param kernel
	 *
	 * @return
	 */
	boolean shouldRegister(Class clazz, Kernel kernel);

}
