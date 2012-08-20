/*
 *   Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.server.sreceiver;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Message;
import tigase.server.Packet;

import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

//~--- JDK imports ------------------------------------------------------------

import java.util.Queue;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class TesterTask here.
 *
 *
 * Created: Mon Sep 17 18:07:12 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class TesterTask extends AbstractReceiverTask {
	private static final String TASK_TYPE = "Tester Task";
	private static final String TASK_HELP =
		"This task pretends to be a user."
		+ " Allows you to [un]subscribe to its roster, send a message"
		+ " and perform some other actions. Roster of this task is stored"
		+ " in memory only and is cleared on server restart."
		+ " Full list of supported actions will be sent to you as a response"
		+ " to //help message." + " The purpose of this task is testing of the Tigase server and"
		+ " the task should not be normally loaded on to live system.";

	//~--- constant enums -------------------------------------------------------

	private enum command { help, genn; }

	;

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getHelp() {
		return TASK_HELP;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getType() {
		return TASK_TYPE;
	}

	//~--- methods --------------------------------------------------------------

	protected void processMessage(Packet packet, Queue<Packet> results) {
		if (isPostCommand(packet)) {
			runCommand(packet, results);
		} else {
			String body = packet.getElemCData("/message/body");

			results.offer(Message.getMessage(packet.getStanzaTo(), packet.getStanzaFrom(),
					StanzaType.normal, "This is response to your message: [" + body + "]", "Response",
						null, packet.getStanzaId()));
		}
	}

	private String commandsHelp() {
		return "Available commands are:\n" + "//help - display this help info\n"
				+ "//genn N - generates N messages to random, non-existen users on "
					+ "the server and responds with a simple reply masse: 'Completed N.'\n"
						+ " For now you can only subsribe to and unsubscribe from task roster"
							+ " and send a message to task as it was a user. The task will always"
								+ " respond to your messages with following text:\n"
									+ " This is response to your message: [your message included here]";
	}

	//~--- get methods ----------------------------------------------------------

	private boolean isPostCommand(Packet packet) {
		String body = packet.getElemCData("/message/body");

		if (body != null) {
			for (command comm : command.values()) {
				if (body.startsWith("//" + comm.toString())) {
					return true;
				}
			}
		}

		return false;
	}

	//~--- methods --------------------------------------------------------------

	private void runCommand(Packet packet, Queue<Packet> results) {
		String body = packet.getElemCData("/message/body");
		String[] body_split = body.split(" |\n|\r");
		command comm = command.valueOf(body_split[0].substring(2));

		switch (comm) {
			case help :
				results.offer(Message.getMessage(packet.getStanzaTo(), packet.getStanzaFrom(),
						StanzaType.chat, commandsHelp(), "Commands description", null,
							packet.getStanzaId()));

				break;

			case genn :
				try {
					int number = Integer.parseInt(body_split[1]);
					String domain = packet.getStanzaFrom().getDomain();

					for (int i = 0; i < number; i++) {
						results.offer(Message.getMessage(packet.getStanzaTo(),
								JID.jidInstance("nonename_" + i + "@" + domain), StanzaType.normal,
									"Traffic generattion: " + number, "Internal load test", null,
										packet.getStanzaId()));
					}

					results.offer(Message.getMessage(packet.getStanzaTo(), packet.getStanzaFrom(),
							StanzaType.normal, "Completed " + number, "Response", null,
								packet.getStanzaId()));
				} catch (Exception e) {
					results.offer(Message.getMessage(packet.getStanzaTo(), packet.getStanzaFrom(),
							StanzaType.normal,
							"Incorrect command parameter: "
							+ ((body_split.length > 1) ? body_split[1] : null) + ", expecting Integer.", "Response", null, packet.getStanzaId()));
				}

				break;
		}
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
