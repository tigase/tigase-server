/*
 * ClusterRepoConstants.java
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


package tigase.cluster.repo;

public interface ClusterRepoConstants {
	public static final String CPU_USAGE_COLUMN = "cpu_usage";

	public static final String HOSTNAME_COLUMN = "hostname";

	public static final String SECONDARY_HOSTNAME_COLUMN = "secondary";

	public static final String LASTUPDATE_COLUMN = "last_update";

	public static final String MEM_USAGE_COLUMN = "mem_usage";

	public static final String PASSWORD_COLUMN = "password";

	public static final String PORT_COLUMN = "port";

	public static final String REPO_URI_PROP_KEY = "repo-uri";

	public static final String TABLE_NAME = "cluster_nodes";
}

