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
package tigase.server.amp.db;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import tigase.db.AbstractDataSourceAwareTestCase;
import tigase.db.DataSource;
import tigase.db.DataSourceAware;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.jid.BareJID;

import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Created by andrzej on 24.03.2017.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class AbstractMsgBroadcastRepositoryTest<DS extends DataSource> extends AbstractDataSourceAwareTestCase<DS, MsgBroadcastRepository> {

	protected static boolean checkEmoji = true;
	protected static String emoji = "\uD83D\uDE97\uD83D\uDCA9\uD83D\uDE21";
	private static BareJID jid;
	private static Element msg;
	private static String msgId;

	@BeforeClass
	public static void init() throws TigaseStringprepException {
		jid = BareJID.bareJIDInstance("broadcast-" + UUID.randomUUID(), "example.com");
		msgId = "test-" + UUID.randomUUID().toString();
		msg = new Element("message");
		msg.addChild(new Element("body", "Testing broadcast messages" + (checkEmoji ? emoji : "")));
	}
	
	@Test
	public void test1_addingBroadcastMessage() throws InterruptedException {
		Date expire = new Date(System.currentTimeMillis() + (60 * 1000 * 5));
		assertTrue("Not added message to broadcast list!", repo.updateBroadcastMessage(msgId, msg, expire, jid));
		assertNotNull("Not found message with id = " + msgId, repo.getBroadcastMsg(msgId));
		repo.loadMessagesToBroadcast();
		assertNotNull("Not found message with id = " + msgId, repo.getBroadcastMsg(msgId));
	}

	@Test
	public void test2_loadingBroadcastMessagesAndAddingBroadcastMessageRecipient() throws InterruptedException {
		Thread.sleep(1000);
		repo.loadMessagesToBroadcast();
		assertNotNull("Not found message with id = " + msgId, repo.getBroadcastMsg(msgId));

		assertFalse("Added message instead of adding message recipient!",
					repo.updateBroadcastMessage(msgId, null, null, jid));
	}
	
	@Override
	protected Class<? extends DataSourceAware> getDataSourceAwareIfc() {
		return MsgBroadcastRepository.class;
	}
}
