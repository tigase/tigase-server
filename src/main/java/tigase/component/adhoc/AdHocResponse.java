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
package tigase.component.adhoc;

import tigase.server.Command;
import tigase.server.DataForm;
import tigase.xml.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

public class AdHocResponse {

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

	public Collection<Element> getElements() {
		return elements;
	}

	public Element addDataForm(Command.DataType dataType) {
		Element data = DataForm.createDataForm(dataType);
		elements.add(data);
		return data;
	}

	public Element addDataForm(Command.DataType dataType, Consumer<Element> consumer) {
		Element data = addDataForm(dataType);
		consumer.accept(data);
		return data;
	}

	public void startSession() {
		this.newState = State.executing;
		this.sessionid = UUID.randomUUID().toString();
	}

	State getCurrentState() {
		return currentState;
	}

	public State getNewState() {
		return newState;
	}

	public void setNewState(State newState) {
		this.newState = newState;
	}

	String getSessionid() {
		return sessionid;
	}

	void setSessionid(String sessionid) {
		this.sessionid = sessionid;
	}

	public static enum State {
		canceled,
		completed,
		executing
	}

}
