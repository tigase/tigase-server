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
	
	/**
	 * Method validates whether the connection can at least be established. If yes
	 * then appropriate flag is set.
	 *
	 * @param variables set of {@code Properties} with all configuration options
	 */
	public abstract Result validateDBConnection( Properties variables );

	/**
	 * Method, if the connection is validated by {@code validateDBConnection},
	 * checks whether desired database exists. If not it creates such database
	 * using {@code *-installer-create-db.sql} schema file substituting it's
	 * variables with ones provided.
	 *
	 * @param variables set of {@code Properties} with all configuration options
	 */
	public abstract Result validateDBExists( Properties variables );
	public abstract Result validateDBSchema( Properties variables );
	public abstract Result postInstallation( Properties variables );
	public abstract Result printInfo( Properties variables );

	/**
	 * Method attempts to add XMPP admin user account to the database using
	 * {@code AuthRepository}.
	 *
	 * @param variables set of {@code Properties} with all configuration options
	 */
	public abstract Result addXmppAdminAccount( Properties variables );

	/**
	 * Method checks whether the connection to the database is possible and that
	 * database of specified name exists. If yes then a schema file from
	 * properties is loaded.
	 *
	 * @param variables set of {@code Properties} with all configuration options
	 */
	public abstract Result loadSchemaFile( Properties variables );
	public abstract Result shutdown( Properties variables );
}
