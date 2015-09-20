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

import java.util.Map;
import tigase.db.DBInitException;
import tigase.db.comp.ConfigRepository;

/**
 * Created: Oct 3, 2009 2:00:30 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class CompConfigRepository extends ConfigRepository<CompRepoItem> {

	@Override
	public void destroy() {
		// Nothing to do
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

	@Override
	public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {
		// Nothing to do
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
