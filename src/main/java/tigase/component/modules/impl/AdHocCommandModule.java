/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import tigase.component.Context;
import tigase.component.adhoc.AdHocCommand;
import tigase.component.adhoc.AdHocCommandException;
import tigase.component.adhoc.AdHocCommandManager;
import tigase.component.exceptions.ComponentException;
import tigase.component.modules.AbstractModule;
import tigase.criteria.Criteria;
import tigase.criteria.ElementCriteria;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.JID;

public class AdHocCommandModule<CTX extends Context> extends AbstractModule<CTX> {

	public static interface ScriptCommandProcessor {

		List<Element> getScriptItems(String node, JID jid, JID from);

		boolean processScriptCommand(Packet pc, Queue<Packet> results);

	}

	protected static final String[] COMMAND_PATH = { "iq", "command" };

	protected static final Criteria CRIT = ElementCriteria.nameType("iq", "set").add(
			ElementCriteria.name("command", Command.XMLNS));

	public final static String ID = "commands";

	public static final String XMLNS = Command.XMLNS;

	protected AdHocCommandManager commandsManager;

	protected ScriptCommandProcessor scriptProcessor;

	public AdHocCommandModule(ScriptCommandProcessor scriptProcessor) {
		this.commandsManager = new AdHocCommandManager();
		this.scriptProcessor = scriptProcessor;
	}

	public List<Element> getCommandListItems(final JID senderJid, final JID toJid) {
		ArrayList<Element> commandsList = new ArrayList<Element>();
		for (AdHocCommand command : this.commandsManager.getAllCommands()) {
			if (command.isAllowedFor(senderJid))
				commandsList.add(new Element("item", new String[] { "jid", "node", "name" }, new String[] { toJid.toString(),
						command.getNode(), command.getName() }));
		}

		List<Element> scriptCommandsList = scriptProcessor.getScriptItems(Command.XMLNS, toJid, senderJid);
		if (scriptCommandsList != null) {
			commandsList.addAll(scriptCommandsList);
		}
		return commandsList;
	}

	@Override
	public String[] getFeatures() {
		return new String[] { Command.XMLNS };
	}

	@Override
	public Criteria getModuleCriteria() {
		return CRIT;
	}

	public List<Element> getScriptItems(String node, JID stanzaTo, JID stanzaFrom) {
		ArrayList<Element> result = new ArrayList<Element>();

		for (AdHocCommand c : commandsManager.getAllCommands()) {
			if (c.isAllowedFor(stanzaFrom)) {
				Element i = new Element("item", new String[] { "jid", "node", "name" }, new String[] { stanzaTo.toString(),
						c.getNode(), c.getName() });
				result.add(i);
			}
		}

		List<Element> scripts = scriptProcessor.getScriptItems(node, stanzaTo, stanzaFrom);
		if (scripts != null)
			result.addAll(scripts);

		return result;
	}

	@Override
	public void process(Packet packet) throws ComponentException {
		String node = packet.getAttributeStaticStr(COMMAND_PATH, "node");
		if (commandsManager.hasCommand(node)) {
			try {
				write(this.commandsManager.process(packet));
			} catch (AdHocCommandException e) {
				throw new ComponentException(e.getErrorCondition(), e.getMessage());
			}
		} else {
			processScriptAdHoc(packet);
		}
	}

	protected void processScriptAdHoc(Packet packet) {
		Queue<Packet> results = new ArrayDeque<Packet>();

		if (scriptProcessor.processScriptCommand(packet, results)) {
			for (Packet p : results) {
				write(p);
			}
		}
	}

	public void register(AdHocCommand command) {
		this.commandsManager.registerCommand(command);
	}

}
