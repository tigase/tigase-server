/*
 * Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License,
 *  or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program. Look for COPYING file in the top folder.
 *  If not, see http://www.gnu.org/licenses/.
 *
 */

package tigase.server.test;

import tigase.component.exceptions.ComponentException;
import tigase.component.modules.AbstractModule;
import tigase.criteria.Criteria;
import tigase.criteria.ElementCriteria;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.util.TigaseStringprepException;
import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

import java.util.logging.Logger;

@Bean(name = "test-generator-module", parent = TestComponent.class, active = true)
public class TestGeneratorModule
		extends AbstractModule {

	private static final Logger log = Logger.getLogger(TestGeneratorModule.class.getCanonicalName());

	private Criteria CRITERIA = ElementCriteria.name("message");

	@Inject
	private TestComponent component;

	@Override
	public Criteria getModuleCriteria() {
		return CRITERIA;
	}

	private boolean isPostCommand(Packet packet) {
		String body = packet.getElemCDataStaticStr(Message.MESSAGE_BODY_PATH);

		if (body != null) {
			for (command comm : command.values()) {
				if (body.startsWith("//" + comm.toString())) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public void process(Packet packet) throws ComponentException, TigaseStringprepException {
		System.out.println(packet.toString().replace("\n", ""));
		if (isPostCommand(packet)) {
			runCommand(packet);
		} else {
			String body = packet.getElemCDataStaticStr(Message.MESSAGE_BODY_PATH);

			write(Message.getMessage(packet.getStanzaTo(), packet.getStanzaFrom(), StanzaType.normal,
			                         "This is response to your message: [" + body + "]", "Response", null,
			                         packet.getStanzaId()));
		}
	}

	private void runCommand(Packet packet) {
		String body = packet.getElemCDataStaticStr(Message.MESSAGE_BODY_PATH);
		String[] body_split = body.split(" |\n|\r");
		command comm = command.valueOf(body_split[0].substring(2));

		switch (comm) {
			case genn:
				try {
					int number = Integer.parseInt(body_split[1]);
					String domain = packet.getStanzaFrom().getDomain();

					for (int i = 0; i < number; i++) {
						write(Message.getMessage(packet.getStanzaTo(), JID.jidInstance("nonename_" + i + "@" + domain),
						                         StanzaType.normal, "Traffic generattion: " + number,
						                         "Internal load test", null, packet.getStanzaId()));
					}
					write(Message.getMessage(packet.getStanzaTo(), packet.getStanzaFrom(), StanzaType.normal,
					                         "Completed " + number, "Response", null, packet.getStanzaId()));
				} catch (Exception e) {
					write(Message.getMessage(packet.getStanzaTo(), packet.getStanzaFrom(), StanzaType.normal,
					                         "Incorrect command parameter: " +
							                         ((body_split.length > 1) ? body_split[1] : null) +
							                         ", expecting Integer.", "Response", null, packet.getStanzaId()));
				}

				break;
		}
	}

	private enum command {
		genn
	}

}
