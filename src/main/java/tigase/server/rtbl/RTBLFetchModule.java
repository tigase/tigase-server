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
package tigase.server.rtbl;

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Iq;
import tigase.util.common.TimerTask;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.jid.BareJID;

import java.util.List;
import java.util.stream.Collectors;

@Bean(name = "rtblFetchModule", parent = RTBLComponent.class, active = true)
public class RTBLFetchModule
		extends RTBLIqModule<String> {

	private static final String PUBSUB_XMLNS = "http://jabber.org/protocol/pubsub";

	@Inject
	private RTBLRepository repository;

	public RTBLFetchModule() {
		super("fetch-");
	}

	public void fetch(BareJID jid, String node) {
		sendIq(jid, StanzaType.get, iqEl -> {
			iqEl.withElement("pubsub", PUBSUB_XMLNS, pubsubEl -> {
				pubsubEl.withElement("items", itemsEl -> {
					itemsEl.withAttribute("node", node);
				});
			});
		}, () -> node);
	}

	@Override
	protected void handleResult(ResultEvent event, String node) {
		switch (event.getResult()) {
			case failureRetry -> getComponent().addTimerTask(new TimerTask() {
				@Override
				public void run() {
					fetch(event.getJid(), node);
				}
			}, 10000);
			case success, failureNoRetry -> {}
		}

	}

	@Override
	protected void handleSuccess(Iq iq) {
		Element pubsubEl = iq.getElemChild("pubsub", PUBSUB_XMLNS);
		if (pubsubEl == null) {
			return;
		}
		Element itemsEl = pubsubEl.getChild("items");
		if (itemsEl == null) {
			return;
		}
		String node = itemsEl.getAttributeStaticStr("node");
		List<Element> items = itemsEl.findChildren(el -> "item".equals(el.getName()));
		if (node == null || items == null) {
			return;
		}

		RTBL existingRTBL = repository.getBlockList(iq.getStanzaFrom().getBareJID(), node);
		if (existingRTBL != null) {
			RTBL rtbl = new RTBL(iq.getStanzaFrom().getBareJID(), node, existingRTBL.getHash(),
								 items.stream().map(it -> it.getAttributeStaticStr("id")).collect(Collectors.toSet()));
			repository.update(rtbl);
		}
	}
}
