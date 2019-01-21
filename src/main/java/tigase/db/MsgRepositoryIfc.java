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
package tigase.db;

import tigase.server.amp.db.MsgRepository;
import tigase.xml.Element;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.JID;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by andrzej on 13.03.2016.
 */
public interface MsgRepositoryIfc<T extends DataSource>
		extends OfflineMsgRepositoryIfc, DataSourceAware<T> {

	Map<Enum, Long> getMessagesCount(JID to) throws UserNotFoundException;

	List<Element> getMessagesList(JID to) throws UserNotFoundException;

	void setCondition(ReentrantLock lock, Condition condition);

	Queue<Element> loadMessagesToJID(List<String> db_ids, XMPPResourceConnection session,
													 boolean delete, MsgRepository.OfflineMessagesProcessor proc)
			throws UserNotFoundException;

	int deleteMessagesToJID(List<String> db_ids, XMPPResourceConnection session)
			throws UserNotFoundException;

}
