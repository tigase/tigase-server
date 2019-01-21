/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
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
 */
package tigase.monitor;

import tigase.db.DBInitException;
import tigase.db.comp.UserRepoRepository;
import tigase.kernel.beans.Bean;
import tigase.xmpp.jid.BareJID;

import java.util.Map;

@Bean(name = "configItemRepository", parent = MonitorComponent.class, active = true)
public class TaskConfigItemJDBCRepository
		extends UserRepoRepository<TaskConfigItem> {

	private final static String CONFIG_KEY = "monitor-tasks";

	private final static BareJID REPO_USER_JID = BareJID.bareJIDInstanceNS("tigase-monitor");

	@Override
	public void destroy() {
		// Nothing to do
	}

	@Override
	public String getConfigKey() {
		return CONFIG_KEY;
	}

	@Override
	public String[] getDefaultPropetyItems() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TaskConfigItem getItemInstance() {
		return new TaskConfigItem();
	}

	@Override
	public String getPropertyKey() {
		return "--monitor-tasks";
	}

	@Override
	public BareJID getRepoUser() {
		return REPO_USER_JID;
	}

	@Override
	@Deprecated
	public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {
		// Nothing to do
	}

}
