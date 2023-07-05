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

import tigase.component.exceptions.ComponentException;
import tigase.component.modules.AbstractModule;
import tigase.criteria.Criteria;
import tigase.criteria.ElementCriteria;
import tigase.criteria.Or;
import tigase.eventbus.EventBusEvent;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.vhosts.VHostManager;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.StanzaType;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class RTBLIqModule<RCTX> extends AbstractModule implements Initializable, UnregisterAware {

	private final Criteria criteria = new Or(ElementCriteria.nameType("iq", "result"),
									  ElementCriteria.nameType("iq", "error"));
	@Inject(bean = "rtbl-component")
	private RTBLComponent component;
	@Inject
	private VHostManager vHostManager;

	private final String requestPrefix;
	private ConcurrentHashMap<RequestKey,RCTX> activeRequests = new ConcurrentHashMap<>();

	public RTBLIqModule(String requestPrefix) {
		this.requestPrefix = requestPrefix;
	}

	public RTBLComponent getComponent() {
		return component;
	}

	public Criteria getModuleCriteria() {
		return criteria;
	}

	@Override
	public void initialize() {
		eventBus.registerAll(this);
	}

	@Override
	public void beforeUnregister() {
		if (eventBus != null) {
			eventBus.unregisterAll(this);
		}
	}

	protected JID getOwnJID() {
		return JID.jidInstanceNS(component.getName(), vHostManager.getDefVHostItem().getDomain());
	}

	protected <T> void sendIq(BareJID to, StanzaType type, Consumer<Element> consumer, Supplier<RCTX> handleContextSupplier) {
		String id = requestPrefix + UUID.randomUUID().toString();
		Element iqEl = new Element("iq").withAttribute("type", type.name()).withAttribute("id", id);
		consumer.accept(iqEl);
		RequestKey requestKey = new RequestKey(to, id);
		activeRequests.put(requestKey, handleContextSupplier.get());
		try {
			write(new Iq(iqEl, getOwnJID(), JID.jidInstance(to)));
		} catch (Throwable ex) {
			activeRequests.remove(requestKey);
		}
	}

	@Override
	public void process(Packet packet) throws ComponentException, TigaseStringprepException {
		JID from = packet.getStanzaFrom();
		if (from == null) {
			return;
		}
		String id = packet.getAttributeStaticStr("id");
		if (id == null) {
			throw new ComponentException(Authorization.BAD_REQUEST);
		}
		if (!id.startsWith(requestPrefix)) {
			return;
		}

		ResultEvent.Result result = ResultEvent.Result.fromPacket(packet);
		if (result == ResultEvent.Result.success) {
			handleSuccess((Iq) packet);
		}
		getEventBus().fire(new ResultEvent(getClass(), from.getBareJID(), id, result));
	}

	protected void handleSuccess(Iq iq) {};

	@HandleEvent
	public void handleResultEvent(ResultEvent event) {
		if (event.isForClass(getClass())) {
			RCTX ctx = activeRequests.remove(new RequestKey(event.getJid(), event.getId()));
			if (ctx != null) {
				handleResult(event, ctx);
			}
		}
	}

	protected abstract void handleResult(ResultEvent event, RCTX ctx);

	public static class RequestKey {
		private final BareJID jid;
		private final String id;

		public RequestKey(BareJID jid, String id) {
			this.jid = jid;
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof RequestKey that)) {
				return false;
			}
			return Objects.equals(jid, that.jid) && Objects.equals(id, that.id);
		}

		@Override
		public int hashCode() {
			return Objects.hash(jid, id);
		}
	}

	public static class ResultEvent
			implements Serializable, EventBusEvent {

		public enum Result {
			success,
			failureRetry,
			failureNoRetry;

			public static ResultEvent.Result fromPacket(Packet packet) throws ComponentException {
				StanzaType type = packet.getType();
				if (type == null) {
					throw new ComponentException(Authorization.BAD_REQUEST);
				}
				return switch (type) {
					case result -> Result.success;
					case error -> {
						Element error = packet.getElemChild("error");
						if (error != null) {
							yield Stream.of(Authorization.INTERNAL_SERVER_ERROR, Authorization.REMOTE_SERVER_NOT_FOUND,
											Authorization.REMOTE_SERVER_TIMEOUT)
										  .map(Authorization::getCondition)
										  .anyMatch(name -> error.getChild(name,
																		   "urn:ietf:params:xml:ns:xmpp-stanzas") !=
												  null) ? Result.failureRetry : Result.failureNoRetry;
						}
						yield Result.failureNoRetry;
					}
					default -> throw new ComponentException(Authorization.BAD_REQUEST);
				};
			}
		}
		private BareJID jid;
		private String id;

		private Result result;
		private String className;


		public ResultEvent() {}
		public ResultEvent(Class<? extends RTBLIqModule> forClazz, BareJID jid, String id, ResultEvent.Result result) {
			this.className = forClazz.getSimpleName();
			this.jid = jid;
			this.id = id;
			this.result = result;
		}

		public BareJID getJid() {
			return jid;
		}

		public void setJid(BareJID jid) {
			this.jid = jid;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public ResultEvent.Result getResult() {
			return result;
		}

		public void setResult(ResultEvent.Result result) {
			this.result = result;
		}

		public boolean isForClass(Class<? extends RTBLIqModule> clazz) {
			return clazz.getSimpleName().equals(className);
		}
	}

}
