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
package tigase.eventbus.impl;

import tigase.xml.Element;
import tigase.xmpp.jid.JID;

import java.util.HashSet;

public class Event1 {

	private Element elementField;
	private String emptyField;
	private JID jid;
	private HashSet<String> setField;
	private String[] strArrField;
	private transient String transientField;
	private String v1;
	private int v2;

	public Element getElementField() {
		return elementField;
	}

	public void setElementField(Element elementField) {
		this.elementField = elementField;
	}

	public String getEmptyField() {
		return emptyField;
	}

	public void setEmptyField(String emptyField) {
		this.emptyField = emptyField;
	}

	public JID getJid() {
		return jid;
	}

	public void setJid(JID jid) {
		this.jid = jid;
	}

	public String[] getStrArrField() {
		return this.strArrField;
	}

	public void setStrArrField(String[] v) {
		this.strArrField = v;
	}

	public String getTransientField() {
		return transientField;
	}

	public void setTransientField(String transientField) {
		this.transientField = transientField;
	}

	public String getV1() {
		return v1;
	}

	public void setV1(String v1) {
		this.v1 = v1;
	}

	public int getV2() {
		return v2;
	}

	public void setV2(int v2) {
		this.v2 = v2;
	}

	public HashSet<String> getSetField() {
		return setField;
	}

	public void setSetField(HashSet<String> set) {
		this.setField = set;
	}
}
