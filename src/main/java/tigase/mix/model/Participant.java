/*
 * Tigase MIX - MIX component for Tigase
 * Copyright (C) 2020 Tigase, Inc. (office@tigase.com)
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
package tigase.mix.model;

import tigase.mix.Mix;
import tigase.xml.Element;
import tigase.xmpp.jid.BareJID;

import java.util.List;

public class Participant implements IParticipant {

	private final String participantId;
	private BareJID realJid;
	private String nick;

	public Participant(String participantId, BareJID realJid, String nick) {
		this.participantId = participantId;
		this.realJid = realJid;
		this.nick = nick;
	}

	public Participant(String participantId, Element participantEl) {
		this.participantId = participantId;
		List<Element> children =  participantEl.getChildren();
		if (children != null) {
			for (Element child : children) {
				switch (child.getName()) {
					case "jid":
						realJid = BareJID.bareJIDInstanceNS(child.getCData());
						break;
					case "nick":
						nick = child.getCData();
						break;
					default:
						break;
				}
			}
		}
	}

	@Override
	public String getParticipantId() {
		return participantId;
	}

	@Override
	public BareJID getRealJid() {
		return realJid;
	}

	@Override
	public String getNick() {
		return nick;
	}

	@Override
	public Element toElement() {
		Element participantEl = new Element("participant");
		participantEl.setXMLNS(Mix.CORE1_XMLNS);
		if (nick != null) {
			participantEl.addChild(new Element("nick", nick));
		}
		if (realJid != null) {
			participantEl.addChild(new Element("jid", realJid.toString()));
		}
		return participantEl;
	}
}
