/*
 * SessionManagerConfig.java
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



package tigase.server.xmppsession;

//~--- non-JDK imports --------------------------------------------------------

/**
 * Describe class SessionManagerConfig here.
 *
 *
 * Created: Tue Oct 24 23:07:57 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class SessionManagerConfig {
	/** Field description */
	public static final String defaultHandlerProcId = "default-handler";

	/** Field description */
	public static final String PLUGINS_CONCURRENCY_PROP_KEY = "plugins-concurrency";

	/** Field description */
	public static final String PLUGINS_CONF_PROP_KEY = "plugins-conf";

	/** Field description */
	public static final String PLUGINS_PROP_KEY = "plugins";

	/** Field description */
	public static final String sessionCloseProcId = "session-close";

	/** Field description */
	public static final String sessionOpenProcId = "session-open";

	/** Field description */
	protected static final String ADMIN_SCRIPTS_PROP_KEY = "admin-scripts-dir";

	/** Field description */
	protected static final String ADMIN_SCRIPTS_PROP_VAL = "scripts/admin/";

	/** Field description */
	protected static final String AUTO_CREATE_OFFLINE_USER_PROP_KEY =
			"offline-user-autocreate";

	/** Field description */
	protected static final String AUTO_CREATE_OFFLINE_USER_PROP_VAL = "false";

	/** Field description */
	protected static final String FORCE_DETAIL_STALE_CONNECTION_CHECK =
			"force-detail-stale-connection-check";

	/** Field description */
	protected static final String SKIP_PRIVACY_PROP_KEY = "skip-privacy";

	/** Field description */
	protected static final String SM_THREADS_POOL_CUSTOM_PROP_VAL = "custom";

	/** Field description */
	public static final String SM_THREADS_POOL_PROP_KEY = "sm-threads-pool";

	/** Field description */
	protected static final String SM_THREADS_POOL_PROP_VAL = "default";
	
	protected static final String SM_THREADS_FACTOR_PROP_KEY = "sm-threads-factor";
	protected static final int SM_THREADS_FACTOR_PROP_VAL = 1;

	protected static final String AUTH_TIMEOUT_PROP_KEY = "auth-timeout";
	protected static final long AUTH_TIMEOUT_PROP_VAL = 120;

	/** Field description */
	protected static final String STALE_CONNECTION_CLOSER_QUEUE_SIZE_KEY =
			"stale-connection-closer-queue-size";

}    // SessionManagerConfig


//~ Formatted in Tigase Code Convention on 13/10/16
