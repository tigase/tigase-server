/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

package tigase.server.ext;

//~--- non-JDK imports --------------------------------------------------------

import java.util.Map;
import tigase.db.DBInitException;
import tigase.db.comp.UserRepoRepository;

import tigase.xmpp.BareJID;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Oct 24, 2009 3:55:41 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public class CompDBRepository extends UserRepoRepository<CompRepoItem> {

	@Override
	public void destroy() {
		// Nothing to destroy here
	}
	
	@Override
	public String getConfigKey() {
		return CompRepoDefaults.getConfigKey();
	}

	@Override
	public String[] getDefaultPropetyItems() {
		return CompRepoDefaults.getDefaultPropetyItems();
	}

	@Override
	public CompRepoItem getItemInstance() {
		return CompRepoDefaults.getItemInstance();
	}

	@Override
	public String getPropertyKey() {
		return CompRepoDefaults.getPropertyKey();
	}

	@Override
	public BareJID getRepoUser() {
		return CompRepoDefaults.getRepoUser();
	}
	
	@Override
	public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {
		// Nothing to do here
	}

	@Override
	public String validateItem(CompRepoItem item) {
		String result = super.validateItem(item);
		if (result == null) {
			result = item.validate();
		}
		return result;
	}
	
}
