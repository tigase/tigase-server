/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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
package tigase.db;

import tigase.component.exceptions.RepositoryException;

import java.time.Duration;

/**
 * Interface implemented by every class providing access to data storage, ie. databases, files, key-value stores.
 * 
 * Created by andrzej on 09.03.2016.
 */
public interface DataSource extends Repository {

	/**
	 * Returns a DB connection string or DB connection URI.
	 *
	 * @return a <code>String</code> value representing database connection
	 *         string.
	 */
	String getResourceUri();

	/**
	 * The method is called to initialize the data repository.
	 * @param resource_uri value in most cases representing the database connection string.
	 * @throws RepositoryException if there was an error during initialization of data source.
	 * Some implementations, though, perform so called lazy initialization so even though there
	 * is a problem with the underlying data source it may not be signaled through this method
	 * call.
	 */
	void initialize(String resource_uri) throws RepositoryException;

	/**
	 * This method is called by data source bean watchdog mechanism to ensure that there is proper
	 * connectivity to underlying data storage.
	 * @param watchdogTime time which should pass between checks
	 */
	default void checkConnectivity(Duration watchdogTime) {

	}

}
