/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
package tigase.component.adhoc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import tigase.xml.Element;

public class AdHocResponse {

	static enum State {
		canceled,
		completed,
		executing
	}

	private final State currentState;

	private final ArrayList<Element> elements = new ArrayList<Element>();

	private State newState = State.completed;

	private String sessionid;

	AdHocResponse(String sessionid, State currState) {
		this.sessionid = sessionid;
		this.currentState = currState;
	}

	public void cancelSession() {
		this.newState = State.canceled;
	}

	public void completeSession() {
		this.newState = State.completed;
	}

	State getCurrentState() {
		return currentState;
	}

	public Collection<Element> getElements() {
		return elements;
	}

	State getNewState() {
		return newState;
	}

	String getSessionid() {
		return sessionid;
	}

	void setNewState(State newState) {
		this.newState = newState;
	}

	void setSessionid(String sessionid) {
		this.sessionid = sessionid;
	}

	public void startSession() {
		this.newState = State.executing;
		this.sessionid = UUID.randomUUID().toString();
	}

}
