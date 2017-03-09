/*
 * MAMRepository.java
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
package tigase.xmpp.mam;

import tigase.component.exceptions.ComponentException;
import tigase.component.exceptions.RepositoryException;
import tigase.xml.Element;

import java.util.Date;

/**
 * Base interface which is required to be implemented by class which should be
 * used as repository implementation for quering using XEP-0313: Message
 * Archive Management
 *
 * Created by andrzej on 19.07.2016.
 */
public interface MAMRepository<Q extends Query, I extends MAMRepository.Item> {

//	int countItems(Q query);

	void queryItems(Q query, ItemHandler<Q, I> itemHandler) throws RepositoryException, ComponentException;

	Q newQuery();

	interface Item {
		String getId();
		Element getMessage();
		Date getTimestamp();
	}

	interface ItemHandler<Q extends Query, I extends Item> {

		void itemFound(Q query, I item);

	}

}
