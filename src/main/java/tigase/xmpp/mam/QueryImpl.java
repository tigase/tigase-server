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
 * Class implements Query interface and is base implementation of query holder used by implementation of XEP-0313:
 * Message Archive Management
 * <br>
 * Created by andrzej on 21.07.2016.
 */
public class QueryImpl
		implements Query {

	private final RSM rsm = new RSM();
	private JID componentJID;
	private Date end;
	private String id;
	private JID questionerJID;
	private Date start;
	private JID with;

	public QueryImpl() {

	}

	public JID getQuestionerJID() {
		return questionerJID;
	}

	public void setQuestionerJID(JID questionerJID) {
		this.questionerJID = questionerJID;
	}

	public JID getComponentJID() {
		return componentJID;
	}

	public void setComponentJID(JID componentJID) {
		this.componentJID = componentJID;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public Date getEnd() {
		return end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	public JID getWith() {
		return with;
	}

	public void setWith(JID with) {
		this.with = with;
	}

	public RSM getRsm() {
		return rsm;
	}

}
