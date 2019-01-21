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
package tigase.component.responses;

import tigase.server.Packet;
import tigase.xmpp.StanzaType;

/**
 * Main interface for callback of all <a href='http://xmpp.org/rfcs/rfc6120.html#stanzas-semantics-iq'>IQ</a>
 * asynchronous request-response mechanism.
 *
 * @author bmalkow
 */
public interface AsyncCallback {

	/**
	 * Called when received response has type {@linkplain StanzaType#error error}.
	 *
	 * @param responseStanza received IQ stanza
	 * @param errorCondition error condition
	 */
	void onError(Packet responseStanza, String errorCondition);

	/**
	 * Called when received response has type {@linkplain StanzaType#result result}.
	 *
	 * @param responseStanza received stanza
	 */
	void onSuccess(Packet responseStanza);

	/**
	 * Called when response wasn't received in given time.
	 */
	void onTimeout();

}