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
import tigase.util.common.TimerTask;
import tigase.xmpp.StanzaType;
import tigase.xmpp.jid.BareJID;

@Bean(name = "rtblSubscribeModule", parent = RTBLComponent.class, active = true)
public class RTBLSubscribeModule
		extends RTBLIqModule<RTBLSubscribeModule.Context> {

	private static final String PUBSUB_XMLNS = "http://jabber.org/protocol/pubsub";

	public RTBLSubscribeModule() {
		super("sub-");
	}

	public void subscribe(BareJID jid, String node) {
		sendIq(jid, StanzaType.set, iqEl -> {
			iqEl.withElement("pubsub", PUBSUB_XMLNS, pubsubEl -> {
				pubsubEl.withElement("subscribe", subscribeEl -> {
					subscribeEl.withAttribute("node", node).withAttribute("jid", getOwnJID().toString());
				});
			});
		}, () -> new Context(Action.subscribe, node));
	}

	public void unsubscribe(BareJID jid, String node) {
		sendIq(jid, StanzaType.set, iqEl -> {
			iqEl.withElement("pubsub", PUBSUB_XMLNS, pubsubEl -> {
				pubsubEl.withElement("unsubscribe", unsubscribeEl -> {
					unsubscribeEl.withAttribute("node", node).withAttribute("jid", getOwnJID().toString());
				});
			});
		}, () -> new Context(Action.unsubscribe, node));
	}
	
	@Override
	protected void handleResult(ResultEvent event, Context ctx) {
		if (event.getResult() == ResultEvent.Result.failureRetry) {
			getComponent().addTimerTask(new TimerTask() {
				@Override
				public void run() {
					switch (ctx.action()) {
						case subscribe -> subscribe(event.getJid(), ctx.node());
						case unsubscribe -> unsubscribe(event.getJid(), ctx.node());
					}
				}
			}, 10000);
		}
	}

	private enum Action {
		subscribe,
		unsubscribe
	}

	public record Context(Action action, String node) {}
}
