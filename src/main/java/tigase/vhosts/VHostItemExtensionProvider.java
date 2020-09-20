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
 * Interface required to be implemented by factories which are adding extensions to vhost items.
 * @param <T> - class of the extension which will be provided by this factory
 *
 * Class to work should be annotated with <code>@Bean</code> annotation and annotation <code>name</code> parameter
 * should be equal to the extension unique id. Moreover, <code>parent</code> parameter should be set to
 * <code>VHostItemExtensionManager.class</code> and <code>active</code> parameter should be set to <code>true</code>.
 *
 */
public interface VHostItemExtensionProvider<T extends VHostItemExtension> {

	/**
	 * Returns unique id of the extension
	 * @return
	 */
	String getId();

	/**
	 * Returns class of the extension
	 * @return
	 */
	Class<T> getExtensionClazz();

}
