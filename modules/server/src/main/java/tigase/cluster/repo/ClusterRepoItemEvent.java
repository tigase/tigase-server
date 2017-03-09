/*
 * ShutdownEvent.java
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
 */

package tigase.cluster.repo;

import tigase.cluster.ClusterConnectionManager.REPO_ITEM_UPDATE_TYPE;

import java.io.Serializable;

/**
 *
 * @author andrzej
 */
public class ClusterRepoItemEvent implements Serializable {

	private static final long serialVersionUID = 1L;

	private final ClusterRepoItem item;
	private final REPO_ITEM_UPDATE_TYPE action;
	
//	public ClusterRepoItemEvent() {}
	
	public ClusterRepoItemEvent(ClusterRepoItem item, REPO_ITEM_UPDATE_TYPE action) {
		this.item = item;
		this.action = action;
	}

	public REPO_ITEM_UPDATE_TYPE getAction() {
		return action;
	}

	public ClusterRepoItem getItem() {
		return item;
	}

	@Override
	public String toString() {
		return "item=" + item + ", action=" + action;
	}
}
