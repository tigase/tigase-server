/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.kernel.beans.config;

import java.lang.annotation.*;

/**
 * Annotation to add additional aliases to the fields.
 * <br/>
 * Useful if field annotated with {@link tigase.kernel.beans.config.ConfigField} is inaccessible direcly,
 * ie. is defined in extended class.
 *
 * Created by andrzej on 05.08.2016.
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface ConfigAlias {

	/**
	 * Name of the field
	 * @return
	 */
	String field();

	/**
	 * Alias for the field
	 * @return
	 */
	String alias();

}
