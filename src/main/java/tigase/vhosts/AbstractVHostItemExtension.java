/*
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
package tigase.vhosts;

import tigase.server.DataForm;
import tigase.xml.Element;

/**
 * Abstract class and subclass of <code>VHostItemExtension</code> providing a helper method to add a boolean fields
 * to ad-hoc forms for manipulation of vhost extension.
 * @param <T>
 */
public abstract class AbstractVHostItemExtension<T extends AbstractVHostItemExtension<T>> extends VHostItemExtension<T> {

	/**
	 * Method adds a boolean field to the form
	 * @param commandEl
	 * @param var
	 * @param label
	 * @param value
	 * @param forDefault - if true then we are setting values for default instance and we can choose only YES or NO.
	 * In other case we can also select DEFAULT to enforce usage of the default value for this vhost item extension.s
	 */
	protected void addBooleanFieldWithDefaultToCommand(Element commandEl, String var, String label, Boolean value, boolean forDefault) {
		if (forDefault) {
			DataForm.addFieldValue(commandEl, var, value == null ? "" : value.toString(), label,
								   new String[]{"Yes", "No"}, new String[]{"true", "false"});
		} else {
			DataForm.addFieldValue(commandEl, var, value == null ? "" : value.toString(), label,
								   new String[]{"Default", "Yes", "No"}, new String[]{"", "true", "false"});
		}
	}

}
