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
package tigase.monitor.modules;

import tigase.component.adhoc.AdHocCommand;
import tigase.component.adhoc.AdHocCommandException;
import tigase.component.adhoc.AdHocCommandManager;
import tigase.component.exceptions.ComponentException;
import tigase.component.modules.impl.AdHocCommandModule;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.core.Kernel;
import tigase.monitor.MonitorComponent;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.JID;

@Bean(name = AdHocCommandModule.ID, parent = MonitorComponent.class, active = true)
public class AdHocCommandMonitorModule
		extends AdHocCommandModule
		implements Initializable {

	private final AdHocCommandManager customCommandsManager = new AdHocCommandManager();
	@Inject
	private ConfigureTaskCommand configCommand;
	@Inject
	private InfoTaskCommand infoCommand;

	@Inject
	private Kernel kernel;

	public AdHocCommandMonitorModule() {
	}

	@Override
	public void initialize() {
		super.initialize();
	}

	@Override
	public void process(Packet packet) throws ComponentException {
		final JID jid = packet.getStanzaTo();

		final Object taskInstance = jid.getResource() != null ? kernel.getInstance(jid.getResource()) : null;

		if (jid.getResource() != null && taskInstance != null) {
			processCommand(packet, taskInstance);
		} else if (jid.getResource() != null) {
			throw new ComponentException(Authorization.ITEM_NOT_FOUND);
		} else if (jid.getResource() == null) {
			super.process(packet);
		} else {
			throw new ComponentException(Authorization.NOT_ACCEPTABLE);
		}
	}

	private AdHocCommand getCommand(final Object taskInstance, final String node) {
		if (node.equals(InfoTaskCommand.NODE)) {
			return infoCommand;
		} else if (node.equals(ConfigureTaskCommand.NODE)) {
			return configCommand;
		} else {
			return null;
		}
	}

	private void processCommand(Packet packet, Object taskInstance) throws ComponentException {
		final Element element = packet.getElement();
		final JID senderJid = packet.getStanzaFrom();
		final Element command = element.getChild(Command.COMMAND_EL, Command.XMLNS);
		final String node = command.getAttributeStaticStr("node");
		final String action = command.getAttributeStaticStr("action");
		final String sessionId = command.getAttributeStaticStr("sessionid");

		AdHocCommand adHocCommand = getCommand(taskInstance, node);

		try {
			customCommandsManager.process(packet, command, node, action, sessionId, adHocCommand, writer::write);
		} catch (AdHocCommandException e) {
			throw new ComponentException(e.getErrorCondition(), e.getMessage());
		}
	}
}
