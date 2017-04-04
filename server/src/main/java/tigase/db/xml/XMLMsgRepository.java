/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License,
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
 */

package tigase.db.xml;

import tigase.db.NonAuthUserRepository;
import tigase.db.Repository;
import tigase.db.UserNotFoundException;
import tigase.server.amp.db.MsgRepository;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.db.NodeNotFoundException;
import tigase.xml.db.XMLDB;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 04.04.2017.
 */
@Repository.Meta( supportedUris = {"memory://.*" } )
public class XMLMsgRepository extends MsgRepository<String,XMLDataSource> {

	private static final Logger log = Logger.getLogger(XMLMsgRepository.class.getCanonicalName());

	private XMLDataSource dataSource;
	private XMLDB xmldb;

	@Override
	public void setDataSource(XMLDataSource dataSource) {
		this.dataSource = dataSource;
		this.xmldb = dataSource.getXMLDB();
	}

	@Override
	public Map<Enum, Long> getMessagesCount(JID to) throws UserNotFoundException {
		throw new UnsupportedOperationException("Feature not implemented!");
	}

	@Override
	public List<Element> getMessagesList(JID to) throws UserNotFoundException {
		throw new UnsupportedOperationException("Feature not implemented!");
	}

	@Override
	public Queue<Element> loadMessagesToJID(XMPPResourceConnection session, boolean delete)
			throws UserNotFoundException {
		return loadMessagesToJID(null, session, delete, null);
	}

	@Override
	public boolean storeMessage(JID from, JID to, Date expired, Element msg, NonAuthUserRepository userRepo)
			throws UserNotFoundException {
		BareJID user = to.getBareJID();
		try {
			String[] old_data = xmldb.getDataList(user.toString(), "offline", "messages");
			String[] all = null;
			String[] list = new String[] { msg.toString() };

			if (old_data != null) {
				all = new String[old_data.length + 1];
				System.arraycopy(old_data, 0, all, 0, old_data.length);
				System.arraycopy(list, 0, all, old_data.length, list.length);
				xmldb.setData(user.toString(), "offline", "messages", all);
			} else {
				xmldb.setData(user.toString(), "offline", "messages", list);
			}    // end of else

		} catch (NodeNotFoundException e) {
			throw new UserNotFoundException("User not found " + user, e);
		}
		return false;
	}

	@Override
	protected void loadExpiredQueue(int max) {
		// nothing to do
	}

	@Override
	protected void loadExpiredQueue(Date expired) {
		// nothing to do
	}

	@Override
	protected void deleteMessage(String db_id) {
		throw new UnsupportedOperationException("Removal of messages using id is not supported!");
	}

	@Override
	public Queue<Element> loadMessagesToJID(List<String> db_ids, XMPPResourceConnection session, boolean delete,
											OfflineMessagesProcessor proc) throws UserNotFoundException {
		BareJID user = null;
		try {
			user = session.getBareJID();
			String[] msgs = xmldb.getDataList(user.toString(), "offline", "messages");
			if (msgs != null) {
				if (delete) {
					xmldb.removeData(user.toString(), "offline", "messages");
				}
				DomBuilderHandler domHandler = new DomBuilderHandler();
				StringBuilder sb = new StringBuilder();

				for ( String msg : msgs ) {
					sb.append( msg );
				}

				char[] data = sb.toString().toCharArray();

				parser.parse( domHandler, data, 0, data.length );

				return domHandler.getParsedElements();
			}
		} catch (NotAuthorizedException e) {
			log.log(Level.WARNING, "Session not authorized yet!", e);
		} catch (NodeNotFoundException e) {
			throw new UserNotFoundException("User not found " + user, e);
		}
		return null;
	}

	@Override
	public int deleteMessagesToJID(List<String> db_ids, XMPPResourceConnection session) throws UserNotFoundException {
		Queue<Element> msgs = loadMessagesToJID(null, session, true, null);
		return msgs == null ? 0 : msgs.size();
	}
}
