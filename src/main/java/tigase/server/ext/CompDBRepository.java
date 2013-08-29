/*
 * CompDBRepository.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
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



package tigase.server.ext;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.comp.UserRepoRepository;

import tigase.xmpp.BareJID;

/**
 * Created: Oct 24, 2009 3:55:41 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class CompDBRepository
				extends UserRepoRepository<CompRepoItem> {
	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	@Override
	public String getConfigKey() {
		return CompRepoDefaults.getConfigKey();
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String[]</code>
	 */
	@Override
	public String[] getDefaultPropetyItems() {
		return CompRepoDefaults.getDefaultPropetyItems();
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>CompRepoItem</code>
	 */
	@Override
	public CompRepoItem getItemInstance() {
		return CompRepoDefaults.getItemInstance();
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	@Override
	public String getPropertyKey() {
		return CompRepoDefaults.getPropertyKey();
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>BareJID</code>
	 */
	@Override
	public BareJID getRepoUser() {
		return CompRepoDefaults.getRepoUser();
	}
}


//~ Formatted in Tigase Code Convention on 13/08/28
