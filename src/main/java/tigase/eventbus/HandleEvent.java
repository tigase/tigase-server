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
package tigase.eventbus;

import java.lang.annotation.*;

/**
 * Annotation to mark method as event handler. <br>
 * <br>
 * Example:
 * <br>
 * <pre>
 * <code>
 * public class Consumer {
 * &#64;HandleEvent
 * public void onCatchSomeNiceEvent(Event01 event) {
 * }
 * &#64;HandleEvent
 * public void onCatchSomeNiceEvent(Event02 event) {
 * }
 * }
 * </code>
 * </pre>
 * <br>
 * Handler method must have only one argument with type equals to expected event.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface HandleEvent {

	enum Type {
		remote,
		local,
		all
	}

	Type filter() default Type.all;
}
