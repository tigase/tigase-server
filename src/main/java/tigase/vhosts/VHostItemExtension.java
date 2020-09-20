/*
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
package tigase.vhosts;

/**
 * Abstract class required to be a superclass for all classes implementing <code>VHostItemExtensionIfc</code>.
 * @param <T>
 */
public abstract class VHostItemExtension<T extends VHostItemExtension<T>> implements VHostItemExtensionIfc<T> {

	/**
	 * Abstract method required to be implemented for merging values stored in this instance with default settings
	 * stored in the default virtual host item (global or default settings of the installation).
	 * 
	 * @param defaults - instance of the extension with default values
	 * @return instance of the extension containing merged values
	 */
	public abstract T mergeWithDefaults(T defaults);

	/**
	 * Generic implementation of a method which combines data returned by <code>toDebugString()</code> with
	 * class name for easier debugging.
	 * 
	 * @return
	 */
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + toDebugString() + ")";
	}
}
