/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2015 "Tigase, Inc." <office@tigase.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License,
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
package tigase.server.xmppclient;

import tigase.db.DataRepository;
import tigase.db.Repository;
import tigase.kernel.beans.config.ConfigField;
import tigase.util.TigaseStringprepException;
import tigase.xmpp.BareJID;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;

/**
 *
 * @author Wojtek
 */
@Repository.Meta( supportedUris = { "jdbc:[^:]+:.*" } )
public class SeeOtherHostDualIPSQLRepository implements SeeOtherHostDualIP.DualIPRepository<DataRepository> {

	public static final String CLUSTER_NODES_TABLE = "cluster_nodes";
	public static final String DB_GET_ALL_DATA_DB_QUERY_KEY = SeeOtherHostIfc.CM_SEE_OTHER_HOST_CLASS_PROP_KEY + "/" + "get-all-data-query";
	public static final String GET_ALL_QUERY_TIMEOUT_QUERY_KEY = SeeOtherHostIfc.CM_SEE_OTHER_HOST_CLASS_PROP_KEY + "/" + "get-all-query-timeout";
	private static final String DEF_DB_GET_ALL_DATA_QUERY = "select * from " + CLUSTER_NODES_TABLE;
	private static final int DEF_QUERY_TIME_OUT = 10;
	@ConfigField(desc = "SQL query to retrieve data")
	private String get_all_data_query = DEF_DB_GET_ALL_DATA_QUERY;
	@ConfigField(desc = "SQL query timeout")
	private int query_timeout = DEF_QUERY_TIME_OUT;
	private DataRepository data_repo = null;

	private static final Logger log = Logger.getLogger(SeeOtherHostDualIPSQLRepository.class.getName() );

	@Override
	public void setDataSource(DataRepository dataSource) {
		data_repo = dataSource;
		try {
			checkDB();
			data_repo.initPreparedStatement(get_all_data_query, get_all_data_query);
		} catch (SQLException ex) {
			throw new RuntimeException( "Repository initialization failed", ex );
		}
	}

	/**
	 * Performs database check
	 *
	 * @throws SQLException
	 */
	private void checkDB() throws SQLException {
		if ( !data_repo.checkTable( CLUSTER_NODES_TABLE ) ){
			throw new SQLException( "Nodes redirection table doesn't exits!" );
		}
	}

	@Override
	public Map<BareJID, BareJID> queryAllDB() throws SQLException {
		Map<BareJID, BareJID> result = new ConcurrentSkipListMap<BareJID, BareJID>();
		if ( data_repo == null ){
			return null;
		}
		PreparedStatement get_all = data_repo.getPreparedStatement( null, get_all_data_query );
		get_all.setQueryTimeout( DEF_QUERY_TIME_OUT );
		ResultSet rs = null;
		synchronized ( get_all ) {
			try {
				rs = get_all.executeQuery();
				while ( rs.next() ) {
					String user_jid = rs.getString( HOSTNAME_ID );
					String node_jid = rs.getString( SECONDARY_HOSTNAME_ID );
					try {
						BareJID hostname_hid = BareJID.bareJIDInstance( user_jid );
						BareJID secondary = BareJID.bareJIDInstance( node_jid );
						result.put( hostname_hid, secondary );
					} catch ( TigaseStringprepException ex ) {
						log.warning( "Invalid host or secondary hostname JID: " + user_jid + ", " + node_jid );
					}
				}
			} finally {
				data_repo.release( null, rs );
			}
		}
		log.info( "Loaded " + result.size() + " redirect definitions from database." );
		return result;
	}

}
