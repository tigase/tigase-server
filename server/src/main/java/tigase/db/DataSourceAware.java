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

/**
 * Interface providing a generic way to access data sources by classes implementing it.
 *
 * It is required to be implemented by classes requiring access to data sources
 * and should be used with class extending {@link tigase.db.beans.MDRepositoryBean}
 * for ease of use and to provide support for usage of different data source for
 * different domains (vhosts).
 *
 * Created by andrzej on 09.03.2016.
 */
public interface DataSourceAware<T extends DataSource> {

	/**
	 * Method called to provide class with instance of a data source.
	 * @param dataSource
	 * @throws RepositoryException
	 */
	void setDataSource(T dataSource) throws RepositoryException;

}
