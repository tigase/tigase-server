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
package tigase.component.eventbus;

/**
 * Base event object.
 * 
 * @param <H>
 *            handler type.
 */
public abstract class Event<H extends EventHandler> {

	private Object source;

	protected Event() {
		super();
	}

	/**
	 * Invokes handlers method.
	 * 
	 * @param handler
	 *            handler
	 */
	protected abstract void dispatch(H handler) throws Exception;

	/**
	 * Returns events source.
	 * 
	 * @return events source. May be <code>null</code>.
	 */
	public Object getSource() {
		return source;
	}

	/**
	 * Sets source.
	 * 
	 * @param source
	 *            event source.
	 */
	void setSource(Object source) {
		this.source = source;
	};

}
