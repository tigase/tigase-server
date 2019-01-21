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

import tigase.xmpp.jid.BareJID;

/**
 * The <code>UserExistsException</code> is thrown when application tries to add new user with user ID which already
 * exists in repository. According to <code>UserRepository</code> specification there can be the only one registered
 * user with particular ID. <p> Created: Wed Oct 27 14:17:44 2004 </p>
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class UserExistsException
		extends TigaseDBException {

	private static final long serialVersionUID = 1L;
	private BareJID userId = null;

	public UserExistsException(String message) {
		super(message);
	}

	public UserExistsException(BareJID user, String message, Throwable cause) {
		super(message + " (" + user + ")", cause);
		userId = user;
	}

	public UserExistsException(String message, Throwable cause) {
		super(message, cause);
	}

	public BareJID getUserId() {
		return userId;
	}
} 
