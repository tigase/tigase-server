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
package tigase.kernel.beans;

import java.lang.annotation.*;

/**
 * This annotation marks field in class that Kernel should inject dependency here.
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Inject {

	/**
	 * Name of bean to be injected (optional).
	 *
	 * @return name of bean.
	 */
	String bean() default "";

	/**
	 * Specify if injection of dependency is required or not.
	 *
	 * @return <code>true</code> if <code>null</code> value is allowed to inject.
	 */
	boolean nullAllowed() default false;

	/**
	 * Type of bean to be injected (opiotnal).
	 *
	 * @return type of bean.
	 */
	Class<?> type() default EMPTY.class;

	class EMPTY {

		private EMPTY() {
		}
	}
}
