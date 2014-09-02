/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 * 
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.conf;

import java.io.IOException;

/**
 *
 * @author kobit
 */
public class ConfigurationException extends IOException {

  private static final long serialVersionUID = 1L;

	/**
     * Creates a new instance of <code>ConfigurationException</code> without detail message.
	 */
	public ConfigurationException() {
		super();
	}


	/**
     * Constructs an instance of <code>ConfigurationException</code> with the specified detail message.
     * @param msg the detail message.
     */
	public ConfigurationException(String msg) {
		super(msg);
	}

	public ConfigurationException(String string, Exception e) {
		super(string, e);
	}

}
