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
package tigase.component.modules.impl;

import tigase.component.adhoc.AdHocCommand;
import tigase.component.adhoc.AdHocCommandException;
import tigase.component.adhoc.AdHocCommandManager;
import tigase.component.exceptions.ComponentException;
import tigase.component.modules.AbstractModule;
import tigase.criteria.Criteria;
import tigase.criteria.ElementCriteria;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.jid.JID;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

@Bean(name = AdHocCommandModule.ID, active = true)
public class AdHocCommandModule
		extends AbstractModule
		implements Initializable {

	public final static String ID = "commands";
	public static final String XMLNS = Command.XMLNS;
	protected static final String[] COMMAND_PATH = {"iq", "command"};
	protected static final Criteria CRIT = ElementCriteria.nameType("iq", "set")
			.add(ElementCriteria.name("command", Command.XMLNS));
	@Inject(nullAllowed = false)
	protected AdHocCommandManager commandsManager;
	@Inject(nullAllowed = false)
	protected ScriptCommandProcessor scriptProcessor;

	public AdHocCommandModule() {
		this.commandsManager = new AdHocCommandManager();
	}

	public List<Element> getCommandListItems(final JID senderJid, final JID toJid) {
		ArrayList<Element> commandsList = new ArrayList<Element>();
		addCommandListItemsElements(Command.XMLNS, toJid, senderJid, commandsList::add);
		return commandsList;
	}

	public AdHocCommandManager getCommandsManager() {
		return commandsManager;
	}

	public void setCommandsManager(AdHocCommandManager commandsManager) {
		this.commandsManager = commandsManager;
	}

	@Override
	public String[] getFeatures() {
		return new String[]{Command.XMLNS};
	}

	@Override
	public Criteria getModuleCriteria() {
		return CRIT;
	}

	public List<Element> getScriptItems(String node, JID stanzaTo, JID stanzaFrom) {
		ArrayList<Element> result = new ArrayList<Element>();

		addCommandListItemsElements(node, stanzaTo, stanzaFrom, result::add);

		return result;
	}

	public void addCommandListItemsElements(String node, final JID stanzaTo, final JID stanzaFrom, Consumer<Element> collector) {
		for (AdHocCommand c : commandsManager.getAllCommands()) {
			if (c.isAllowedFor(stanzaFrom)) {
				Element i = new Element("item", new String[]{"jid", "node", "name"},
										new String[]{stanzaTo.toString(), c.getNode(), c.getName()});
				c.getGroup().ifPresent(group -> i.setAttribute("group", group));
				collector.accept(i);
			}
		}
		List<Element> scripts = scriptProcessor.getScriptItems(node, stanzaTo, stanzaFrom);
		if (scripts != null) {
			scripts.forEach(collector);
		}
	}

	public ScriptCommandProcessor getScriptProcessor() {
		return scriptProcessor;
	}

	public void setScriptProcessor(ScriptCommandProcessor scriptProcessor) {
		this.scriptProcessor = scriptProcessor;
	}

	@Override
	public void initialize() {
		if (scriptProcessor == null) {
			throw new RuntimeException("scriptProcessor cannot be null!");
		}
	}

	@Override
	public void process(Packet packet) throws ComponentException {
		String node = packet.getAttributeStaticStr(COMMAND_PATH, "node");
		if (commandsManager.hasCommand(node)) {
			try {
				this.commandsManager.process(packet, writer::write);
			} catch (AdHocCommandException e) {
				throw new ComponentException(e.getErrorCondition(), e.getMessage());
			}
		} else {
			processScriptAdHoc(packet);
		}
	}

	public void register(AdHocCommand command) {
		this.commandsManager.registerCommand(command);
	}

	protected void processScriptAdHoc(Packet packet) {
		Queue<Packet> results = new ArrayDeque<Packet>();

		if (scriptProcessor.processScriptCommand(packet, results)) {
			for (Packet p : results) {
				write(p);
			}
		}
	}

	public static interface ScriptCommandProcessor {

		List<Element> getScriptItems(String node, JID jid, JID from);

		boolean processScriptCommand(Packet pc, Queue<Packet> results);

		boolean isAllowed(String node, JID from);

		boolean isAllowed(String node, String domain, JID from);

	}

}
