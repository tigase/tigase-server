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
package tigase.server.xmppsession;

/**
 * Describe class SessionManagerConfig here.
 * <br>
 * Created: Tue Oct 24 23:07:57 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class SessionManagerConfig {

	public static final String defaultHandlerProcId = "default-handler";

	public static final String PLUGINS_CONCURRENCY_PROP_KEY = "plugins-concurrency";

	public static final String PLUGINS_CONF_PROP_KEY = "plugins-conf";

	public static final String PLUGINS_PROP_KEY = "plugins";

	public static final String sessionCloseProcId = "session-close";

	public static final String sessionOpenProcId = "session-open";
	public static final String SM_THREADS_POOL_PROP_KEY = "sm-threads-pool";
	protected static final String ADMIN_SCRIPTS_PROP_KEY = "admin-scripts-dir";
	protected static final String ADMIN_SCRIPTS_PROP_VAL = "scripts/admin/";
	protected static final String AUTO_CREATE_OFFLINE_USER_PROP_KEY = "offline-user-autocreate";
	protected static final String AUTO_CREATE_OFFLINE_USER_PROP_VAL = "false";
	protected static final String FORCE_DETAIL_STALE_CONNECTION_CHECK = "force-detail-stale-connection-check";
	protected static final String SKIP_PRIVACY_PROP_KEY = "skip-privacy";
	protected static final String SM_THREADS_POOL_CUSTOM_PROP_VAL = "custom";
	protected static final String SM_THREADS_POOL_PROP_VAL = "default";

	protected static final String SM_THREADS_FACTOR_PROP_KEY = "sm-threads-factor";
	protected static final int SM_THREADS_FACTOR_PROP_VAL = 1;

	protected static final String ACTIVE_USER_TIMEFRAME_KEY = "active-user-timeframe";
	protected static final long ACTIVE_USER_TIMEFRAME_VAL = 5 * 60 * 1000;

	protected static final String AUTH_TIMEOUT_PROP_KEY = "auth-timeout";
	protected static final long AUTH_TIMEOUT_PROP_VAL = 120;

	protected static final String STALE_CONNECTION_CLOSER_QUEUE_SIZE_KEY = "stale-connection-closer-queue-size";

}    // SessionManagerConfig

