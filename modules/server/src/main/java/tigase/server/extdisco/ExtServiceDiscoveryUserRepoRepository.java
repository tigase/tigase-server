/*
 * ExtServiceDiscoveryUserRepoRepository.java
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
package tigase.server.extdisco;

import tigase.db.DBInitException;
import tigase.db.comp.UserRepoRepository;
import tigase.kernel.beans.Bean;
import tigase.xmpp.BareJID;

import java.util.Map;

/**
 * Created by andrzej on 06.09.2016.
 */
@Bean(name = "externalServiceDiscoveryRepository", parent = ExternalServiceDiscoveryComponent.class, active = true)
public class ExtServiceDiscoveryUserRepoRepository extends UserRepoRepository<ExtServiceDiscoItem> {

	private static final BareJID repoUser = BareJID.bareJIDInstanceNS("external-service-discovery");

	@Override
	public BareJID getRepoUser() {
		return repoUser;
	}

	@Override
	public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {

	}

	@Override
	public String getConfigKey() {
		return repoUser.getDomain();
	}

	@Override
	public String[] getDefaultPropetyItems() {
		return new String[0];
	}

	@Override
	public String getPropertyKey() {
		return null;
	}

	@Override
	public void destroy() {

	}

	@Override
	public ExtServiceDiscoItem getItemInstance() {
		return new ExtServiceDiscoItem();
	}
}
