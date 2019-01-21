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
package tigase.kernel.beans.config;

import java.lang.annotation.*;

/**
 * Annotation to define configurable field.
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface ConfigField {

	/**
	 * Description of field. May be used in all human readable forms.
	 *
	 * @return description of field.
	 */
	String desc();

	/**
	 * Makes alias of "component root level" property in config file. <p> Not only {@code component/bean/property=value}
	 * will be used but also {@code component/alias=value}. </p>
	 *
	 * @return alias of config field.
	 */
	String alias() default "";

	/**
	 * Allows config to be set on one of parent levels of configuration using alias.
	 * If not set to <code>true</code>, it is possible to use alias only on the config level of the bean.
	 * @return
	 */
	boolean allowAliasFromParent() default true;
}
