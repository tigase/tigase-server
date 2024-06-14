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
package tigase.server.xmppsession;

import tigase.eventbus.EventBusAction;
import tigase.xmpp.StreamError;
import tigase.xmpp.jid.BareJID;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DisconnectUserEBAction
		implements Serializable, EventBusAction {

	private StreamError error;
	private String message;
	private List<String> resources;
	private BareJID userJid;

	public DisconnectUserEBAction() {
	}

	public DisconnectUserEBAction(BareJID userJid, List<String> resources, StreamError error, String message) {
		this.userJid = userJid;
		this.resources = Objects.requireNonNullElse(resources, Collections.emptyList());
		this.error = error;
		this.message = message;
	}

	public DisconnectUserEBAction(BareJID userJid, StreamError error, String message) {
		this(userJid, Collections.emptyList(), error, message);
	}

	public StreamError getError() {
		return error;
	}

	public void setError(StreamError error) {
		this.error = error;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public List<String> getResources() {
		return resources;
	}

	public void setResources(List<String> resources) {
		this.resources = resources;
	}

	public BareJID getUserJid() {
		return userJid;
	}

	public void setUserJid(BareJID userJid) {
		this.userJid = userJid;
	}

	@Override
	public String toString() {
		return "DisconnectUserSessions{" + "userJid=" + getUserJid() + ", resources=" + getResources() + ", error=" +
				getError() + ", message='" + getMessage() + '\'' + '}';
	}
}
