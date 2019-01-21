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
package tigase.db;

import tigase.component.exceptions.RepositoryException;

/**
 * Describe class TigaseDBException here.
 * <br>
 * Created: Thu Oct 26 12:15:36 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class TigaseDBException
		extends RepositoryException {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new <code>TigaseDBException</code> instance.
	 */
	public TigaseDBException(String message) {
		super(message);
	}

	public TigaseDBException(String message, Throwable cause) {
		super(message, cause);
	}

} // TigaseDBException
