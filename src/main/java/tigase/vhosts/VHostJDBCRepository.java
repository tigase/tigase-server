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

package tigase.vhosts;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.comp.UserRepoRepository;

import tigase.xmpp.BareJID;

//~--- classes ----------------------------------------------------------------

/**
 * This implementation stores virtual domains in the UserRepository database.
 * It loads initial settings and virtual hosts from the configuration file
 * and then loads more vhosts from the database. Virtual domains from the
 * database can overwrite (disable) vhosts loaded from the configuration file.
 *
 * This implementation keeps all virtual hosts and their parameters in a single
 * database field. This might not be very efficient if you want to manager big
 * number of virtual domains. It is sufficient for hundreds of vhosts. If you
 * need thousands of VHosts support I advice to implement this storage in
 * more efficient way using separate database tables instead of UserRepository.
 * Please note there is a limit of about 300 vhosts if you use Derby database.
 *
 *
 * Created: Nov 29, 2008 2:32:48 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class VHostJDBCRepository extends UserRepoRepository<VHostItem> {

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getConfigKey() {
		return VHostRepoDefaults.getConfigKey();
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String[] getDefaultPropetyItems() {
		return VHostRepoDefaults.getDefaultPropetyItems();
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public VHostItem getItemInstance() {
		return VHostRepoDefaults.getItemInstance();
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getItemsListPKey() {
		return VHostRepoDefaults.getItemsListPKey();
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getPropertyKey() {
		return VHostRepoDefaults.getPropertyKey();
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public BareJID getRepoUser() {
		return VHostRepoDefaults.getRepoUser();
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
