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
package tigase.component;

import tigase.component.modules.impl.AdHocCommandModule.ScriptCommandProcessor;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.BasicComponent;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.jid.JID;

import java.util.List;
import java.util.Queue;

@Bean(name = "scriptCommandProcessor", active = true)
public class ComponenScriptCommandProcessor
		implements ScriptCommandProcessor {

	@Inject(bean = "service", nullAllowed = false)
	private BasicComponent component;

	@Override
	public List<Element> getScriptItems(String node, JID jid, JID from) {
		return component.getScriptItems(node, jid, from);
	}

	@Override
	public boolean processScriptCommand(Packet pc, Queue<Packet> results) {
		return component.processScriptCommand(pc, results);
	}

	@Override
	public boolean isAllowed(String node, JID from) {
		return component.canCallCommand(from, node);
	}

	@Override
	public boolean isAllowed(String node, String domain, JID from) {
		return component.canCallCommand(from, domain, node);
	}
}
