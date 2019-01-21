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

/**
 * Created by andrzej on 14.08.2016.
 */
public interface RegistrarBeanWithDefaultBeanClass
		extends RegistrarBean {

	/**
	 * Returns default class for all bean which are defined using configuration as subbeans of bean implementing this
	 * interface.
	 *
	 * This allows users to have more convenient configuration file without the need to specify class for each of
	 * subbbeans if most of them will have the same class.
	 *
	 * @return
	 */
	Class<?> getDefaultBeanClass();

}
