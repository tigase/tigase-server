/*
 * HandleEvent.java
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
 *
 */
package tigase.eventbus;

import java.lang.annotation.*;

/**
 * Annotation to mark method as event handler. <br>
 *
 * Example:
 * 
 * <pre>
 * <code>
 	public class Consumer {
		&#64;HandleEvent
		public void onCatchSomeNiceEvent(Event01 event) {
		}
 		&#64;HandleEvent
 		public void onCatchSomeNiceEvent(Event02 event) {
 		}
	}
 * </code>
 * </pre>
 * 
 * Handler method must have only one argument with type equals to expected
 * event.
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface HandleEvent {

	Type filter() default Type.all;

	enum Type {
		remote,
		local,
		all
	}
}
