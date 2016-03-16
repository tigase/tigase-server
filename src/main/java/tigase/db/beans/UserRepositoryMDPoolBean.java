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
package tigase.db.beans;

import tigase.db.UserRepository;
import tigase.db.UserRepositoryMDImpl;
import tigase.kernel.beans.Bean;
import tigase.kernel.core.Kernel;

/**
 * Created by andrzej on 07.03.2016.
 */
@Bean(name="userRepository", parent = Kernel.class, exportable = true)
public class UserRepositoryMDPoolBean extends UserRepositoryMDImpl {

	@Override
	public Class<? extends UserRepositoryConfigBean> getConfigClass() {
		return UserRepositoryConfigBean.class;
	}

	public static class UserRepositoryConfigBean extends AuthUserRepositoryConfigBean<UserRepository, UserRepositoryConfigBean> {

		@Override
		protected Class<UserRepository> getRepositoryIfc() {
			return UserRepository.class;
		}

	}
}
