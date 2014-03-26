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

import java.util.Collection;

/**
 * Exception collects all exceptions throwed by handlers or listeners during
 * firing event. This exception is usually throwed after calling all listeners
 * and handlers.
 */
public class EventBusException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	protected static String createMessage(Collection<Throwable> causes) {
		if (causes.isEmpty()) {
			return null;
		}

		StringBuilder b = new StringBuilder();

		int c = causes.size();
		if (c == 1) {
			b.append("Exception caught: ");
		} else {
			b.append(c).append(" exceptions caught: ");
		}

		boolean first = true;
		for (Throwable t : causes) {
			if (first) {
				first = false;
			} else {
				b.append("; ");
			}
			b.append(t.getMessage());
		}

		return b.toString();
	}

	protected static Throwable createThrowable(Collection<Throwable> causes) {
		if (causes.isEmpty()) {
			return null;
		}
		return causes.iterator().next();
	}

	private final Collection<Throwable> causes;

	public EventBusException(Collection<Throwable> causes) {
		super(createMessage(causes), createThrowable(causes));
		this.causes = causes;
	}

	/**
	 * Returns collection of all Exceptions throwed by handlers or listeners.
	 * 
	 * @return collection of Exceptions.
	 */
	public Collection<Throwable> getCauses() {
		return causes;
	}

}
