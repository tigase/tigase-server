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
package tigase.xmpp.mam;

import tigase.xmpp.jid.JID;
import tigase.xmpp.rsm.RSM;

import java.util.Date;

/**
 * Interface defining methods required by base version MAMRepository to execute queries. Custom implementations of
 * MAMRepository may use additional methods.
 * <br>
 * Created by andrzej on 19.07.2016.
 */
public interface Query {

	JID getQuestionerJID();

	void setQuestionerJID(JID questionerJID);

	JID getComponentJID();

	void setComponentJID(JID componentJID);

	String getId();

	void setId(String id);

	Date getStart();

	void setStart(Date start);

	Date getEnd();

	void setEnd(Date end);

	JID getWith();

	void setWith(JID with);

	RSM getRsm();

}
