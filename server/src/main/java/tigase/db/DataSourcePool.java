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

/**
 * Generic interface for all implementations of a pool of data sources.
 *
 * It is not required to implement and provide pool implementation. However, if underlying connection
 * driver does not contain internal connection pool and uses single connection to data storage it is
 * highly recommended to provide pooling implementation for data source to improve performance of
 * access to data source.
 *
 * Created by andrzej on 09.03.2016.
 */
public interface DataSourcePool<T extends DataSource> extends RepositoryPool<T>, DataSource {
}
