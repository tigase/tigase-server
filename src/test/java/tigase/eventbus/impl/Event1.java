/*
 * Event1.java
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
 */

package tigase.eventbus.impl;

import tigase.xml.Element;
import tigase.xmpp.JID;

public class Event1 {

	private String v1;

	private int v2;

	private transient String transientField;

	private JID jid;

	private String emptyField;

	private Element elementField;
	
	private String[] strArrField;

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
	
}
