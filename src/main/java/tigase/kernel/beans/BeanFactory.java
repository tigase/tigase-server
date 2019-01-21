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

import tigase.kernel.KernelException;

/**
 * Interface to create factories of beans.
 * <br>
 * Factory is responsible to create instance of bean and inject all dependencies!
 *
 * @param <T> type of created bean.
 */
public interface BeanFactory<T> {

	/**
	 * Create instance of bean. <p> Remember, that dependencies will not be injected to this bean. Factory must do that!
	 * </p>
	 *
	 * @return instancje of bean.
	 *
	 * @throws KernelException when something goes wrong.
	 */
	T createInstance() throws KernelException;

}
