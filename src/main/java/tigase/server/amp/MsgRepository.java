/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.server.amp;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import tigase.db.MsgRepositoryIfc;
import tigase.db.TigaseDBException;
import tigase.osgi.ModulesManagerImpl;

/**
 *
 * @author andrzej
 */
public class MsgRepository {
	
	private static final Map<String, MsgRepositoryIfc> repos =
			new ConcurrentSkipListMap<String, MsgRepositoryIfc>();
	

	public static MsgRepositoryIfc getInstance(String cls, String id_string) throws TigaseDBException {
		try {
			String key = cls + "#" + id_string;
			MsgRepositoryIfc result = repos.get(key);

			if (result == null) {
				result = (MsgRepositoryIfc) ModulesManagerImpl.getInstance().forName(cls).newInstance();
				repos.put(key, result);
			}

			return result;
		} catch (Exception ex) {
			throw new TigaseDBException("Could not create instance of " + cls + " for uri " + id_string, ex);
		}
	}

}
