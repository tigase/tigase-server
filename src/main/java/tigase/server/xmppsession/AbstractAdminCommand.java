/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 * 
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.server.xmppsession;

/**
 * Created: Jan 2, 2009 2:32:17 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class AbstractAdminCommand implements AdminCommandIfc {

	private String commandId = null;
	private String description = null;

	@Override
	public void init(String id, String description) {
		this.commandId = id;
		this.description = description;
	}

	@Override
	public String getCommandId() {
		return this.commandId;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	protected boolean isEmpty(String val) {
		return val == null || val.isEmpty();
	}

}
