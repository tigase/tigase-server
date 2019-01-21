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
package tigase.component.exceptions;

import tigase.xmpp.Authorization;
import tigase.xmpp.XMPPProcessorException;

public class ComponentException
		extends XMPPProcessorException {

	public ComponentException(Authorization errorCondition) {
		super(errorCondition);
	}

	public ComponentException(Authorization errorCondition, String text) {
		super(errorCondition, text);
	}

	public ComponentException(Authorization errorCondition, String text, Throwable cause) {
		super(errorCondition, text, cause);
	}

	public ComponentException(Authorization errorCondition, String text, String message) {
		super(errorCondition, text, message);
	}

	public ComponentException(Authorization errorCondition, String text, String message, Throwable cause) {
		super(errorCondition, text, message, cause);
	}
}
