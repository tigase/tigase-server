/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014, "Tigase, Inc." <office@tigase.com>
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
 */
package tigase.util;

import java.util.Properties;

/**
 *
 * @author andrzej
 */
public abstract class SchemaLoader {
	
	public static enum Result {
		ok,
		error,
		warning,
		skipped
	}
	
	public static SchemaLoader newInstance(Properties props) {
		return new DBSchemaLoader(props);
	}
	
	public abstract Result validateDBConnection( Properties variables );
	public abstract Result validateDBExists( Properties variables );
	public abstract Result validateDBSchema( Properties variables );
	public abstract Result postInstallation( Properties variables );
	public abstract Result addXmppAdminAccount( Properties variables );
	public abstract Result loadSchemaFile( Properties variables );
	public abstract Result shutdown( Properties variables );
}
