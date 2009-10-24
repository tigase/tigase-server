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

package tigase.server.ext;

import tigase.db.comp.UserRepoRepository;

/**
 * Created: Oct 24, 2009 3:55:41 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class CompDBRepository extends UserRepoRepository<CompRepoItem> {

	@Override
	public String getRepoUser() {
		return CompRepoDefaults.getRepoUser();
	}

	@Override
	public String[] getDefaultPropetyItems() {
		return CompRepoDefaults.getDefaultPropetyItems();
	}

	@Override
	public String getPropertyKey() {
		return CompRepoDefaults.getPropertyKey();
	}

	@Override
	public String getConfigKey() {
		return CompRepoDefaults.getConfigKey();
	}

	@Override
	public CompRepoItem getItemInstance() {
		return CompRepoDefaults.getItemInstance();
	}

}
