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
package tigase.server;

import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.jid.JID;

/**
 * Created: Dec 31, 2009 8:42:05 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Presence
		extends Packet {

	public static final String ELEM_NAME = "presence";

	public static final String[] PRESENCE_ERROR_PATH = {ELEM_NAME, "error"};

	public static final String[] PRESENCE_PRIORITY_PATH = {ELEM_NAME, "priority"};
	public static final String[] PRESENCE_SHOW_PATH = {ELEM_NAME, "show"};

	public Presence(Element elem) throws TigaseStringprepException {
		super(elem);
	}

	public Presence(Element elem, JID stanzaFrom, JID stanzaTo) {
		super(elem, stanzaFrom, stanzaTo);
	}

	@Override
	protected String[] getElNameErrorPath() {
		return PRESENCE_ERROR_PATH;
	}
}

