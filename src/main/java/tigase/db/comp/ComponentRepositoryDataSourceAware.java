/*
 * ComponentRepositoryDataSourceAware.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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
 *
 */
package tigase.db.comp;

import tigase.db.DataSource;
import tigase.db.DataSourceAware;

/**
 * Interface should be implemented by <code>ComponentRepository</code> which are using
 * <code>DataSource</code> to load/store entries.
 *
 * Interface created mainly for use by <code>AbstractMDComponentRepositoryBean</code>.
 *
 * Created by andrzej on 18.03.2016.
 */
public interface ComponentRepositoryDataSourceAware<Item extends RepositoryItem,DS extends DataSource> extends ComponentRepository<Item>, DataSourceAware<DS> {
}
