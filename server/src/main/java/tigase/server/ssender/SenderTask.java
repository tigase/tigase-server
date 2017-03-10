/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.server.ssender;

import java.io.IOException;
import java.util.TimerTask;
import tigase.xmpp.JID;

/**
 * Describe class SenderTask here.
 *
 *
 * Created: Fri Apr 20 12:05:38 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class SenderTask extends TimerTask {

	private JID name = null;

	public void setName(JID name) {
		this.name = name;
	}

	public JID getName() {
		return name;
	}

	/**
	 * <code>init</code> method is a task specific initialization rountine.
	 *
	 * @param handler a <code>StanzaHandler</code> value is a reference to object
	 * which handles all stanza retrieved from data source. The handler is
	 * responsible for delivering stanza to destination address.
	 * @param initString a <code>String</code> value is an initialization string
	 * for this task. For example database tasks would expect database connection
	 * string here, filesystem task would expect directory here.
	 * @exception IOException if an error occurs during task or data storage
	 * initialization.
	 */
	public abstract void init(StanzaHandler handler, String initString)
		throws IOException;

	/**
	 * <code>getInitString</code> method returns initialization string passed
	 * to it in <code>init()</code> method.
	 *
	 * @return a <code>String</code> value of initialization string.
	 */
	public abstract String getInitString();

}
