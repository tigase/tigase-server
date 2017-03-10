/*
 * MsgBroadcastRepository.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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
 */
package tigase.server.amp.db;

import tigase.db.DBInitException;
import tigase.db.DataSource;
import tigase.db.DataSourceAware;
import tigase.db.DataSourceHelper;
import tigase.db.beans.MDRepositoryBeanWithStatistics;
import tigase.kernel.beans.Bean;
import tigase.server.amp.AmpComponent;
import tigase.server.amp.JidResourceMap;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by andrzej on 15.03.2016.
 */
public abstract class MsgBroadcastRepository<T,S extends DataSource> implements DataSourceAware<S> {

	protected SimpleParser parser = SingletonFactory.getParserInstance();

	protected long broadcastMessagesLastCleanup = 0;
	protected Map<String,BroadcastMsg> broadcastMessages = new ConcurrentHashMap<String,BroadcastMsg>();

	public abstract void loadMessagesToBroadcast();
	protected abstract void ensureBroadcastMessageRecipient(String id, BareJID recipient);
	protected abstract void insertBroadcastMessage(String id, Element msg, Date expire, BareJID recipient);

	public BroadcastMsg getBroadcastMsg(String id) {
		return broadcastMessages.get(id);
	}

	public String dumpBroadcastMessageKeys() {
		StringBuilder sb = new StringBuilder();
		for (String key : broadcastMessages.keySet()) {
			if (sb.length() == 0)
				sb.append("[");
			else
				sb.append(",");
			sb.append(key);
		}
		return sb.append("]").toString();
	}

	public Collection<BroadcastMsg> getBroadcastMessages() {
		long now = System.currentTimeMillis();
		if (now - broadcastMessagesLastCleanup > 60 * 1000) {
			broadcastMessagesLastCleanup = now;
			List<String> toRemove = new ArrayList<String>();
			for (Map.Entry<String,BroadcastMsg> e : broadcastMessages.entrySet()) {
				if (e.getValue().getDelay(TimeUnit.MILLISECONDS) < 0) {
					toRemove.add(e.getKey());
				}
			}
			for (String key : toRemove)
				broadcastMessages.remove(key);
		}
		return Collections.unmodifiableCollection(broadcastMessages.values());
	}

	public boolean updateBroadcastMessage(String id, Element msg, Date expire, BareJID recipient) {
		boolean isNew = false;
		synchronized (broadcastMessages) {
			BroadcastMsg bmsg = broadcastMessages.get(id);
			if (bmsg == null) {
				bmsg = new BroadcastMsg(null, msg, expire);
				broadcastMessages.put(id, bmsg);
				isNew = true;
				insertBroadcastMessage(id, msg, expire, recipient);
			}
			if (bmsg.addRecipient(recipient)) {
				ensureBroadcastMessageRecipient(id, recipient);
			}
			return isNew;
		}
	}

	public class BroadcastMsg<T> extends MsgRepository.MsgDBItem<T> {

		private JidResourceMap<Boolean> recipients = new JidResourceMap<Boolean>();

		public BroadcastMsg(T db_id, Element msg, Date expired) {
			super(db_id, msg, expired);
		}

		protected boolean addRecipient(BareJID jid) {
			if (recipients.containsKey(jid))
				return false;
			recipients.put(JID.jidInstance(jid), Boolean.TRUE);
			return true;
		}

		public boolean needToSend(JID jid) {
			return recipients.containsKey(jid.getBareJID())
					&& (jid.getResource() == null || !recipients.containsKey(jid));
		}

		public void markAsSent(JID jid) {
			recipients.put(jid, Boolean.TRUE);
		}

	}

	@Bean(name = "msgBroadcastRepository", parent = AmpComponent.class, active = true)
	public static class MsgBroadcastRepositoryBean extends MDRepositoryBeanWithStatistics<MsgBroadcastRepository>
			implements MsgBroadcastRepositoryIfc {

		public MsgBroadcastRepositoryBean() {
			super(MsgBroadcastRepositoryIfc.class);
		}

		@Override
		public void setDataSource(DataSource dataSource) {
			// Nothing to do
		}

		@Override
		protected Class<? extends MsgBroadcastRepository> findClassForDataSource(DataSource dataSource) throws DBInitException {
			return DataSourceHelper.getDefaultClass(MsgBroadcastRepository.class, dataSource.getResourceUri());
		}

		@Override
		public void loadMessagesToBroadcast() {
			getRepository("default").loadMessagesToBroadcast();
		}

		@Override
		public MsgBroadcastRepository.BroadcastMsg getBroadcastMsg(String id) {
			return getRepository("default").getBroadcastMsg(id);
		}

		@Override
		public String dumpBroadcastMessageKeys() {
			return getRepository("default").dumpBroadcastMessageKeys();
		}

		@Override
		public Collection<MsgBroadcastRepository.BroadcastMsg> getBroadcastMessages() {
			return getRepository("default").getBroadcastMessages();
		}

		@Override
		public boolean updateBroadcastMessage(String id, Element msg, Date expire, BareJID recipient) {
			return getRepository("default").updateBroadcastMessage(id, msg, expire, recipient);
		}

		@Override
		public Class<?> getDefaultBeanClass() {
			return MsgBroadcastRepositoryConfigBean.class;
		}

		public static class MsgBroadcastRepositoryConfigBean extends MDRepositoryConfigBean<MsgBroadcastRepository> {

		}
	}

}
