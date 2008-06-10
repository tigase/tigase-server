/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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

package tigase.conf;

import java.util.Map;
import tigase.server.ServerComponent;

/**
 * Interface Configurable
 *
 * Objects inheriting this interface can be configured. In Tigase system object
 * can't request configuration properties. Configuration of the object is passed
 * to it at some time. Actually it can be passed at any time. This allows
 * dynamic system reconfiguration at runtime.
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface Configurable extends ServerComponent {

	/**
	 * Constant <code>GEN_CONFIG</code> keeps the string with which all
	 * configuration types starts.
	 */
	public static final String GEN_CONFIG = "--gen-config";
	/**
	 * Constant <code>GEN_CONFIG_ALL</code> keeps parameter name for configuration
	 * with all available components loaded directly to the server.
	 */
	public static final String GEN_CONFIG_ALL = GEN_CONFIG + "-all";
	/**
	 * Constant <code>GEN_CONFIG_SM</code> keeps parameter name for configuration
	 * with SessionManager loaded and XEP-0114 component preconfigured to connect
	 * to server instance with ClientConnectionManager.
	 */
	public static final String GEN_CONFIG_SM = GEN_CONFIG + "-sm";
	/**
	 * Constant <code>GEN_CONFIG_CS</code> keeps parameter name for configuration
	 * with ClientConnectionManager loaded and XEP-0114 component preconfigured
	 * to connect to server instance with SessionManager loaded.
	 */
	public static final String GEN_CONFIG_CS = GEN_CONFIG + "-cs";
	/**
	 * Constant <code>GEN_CONFIG_DEF</code> keeps parameter name for the most
	 * typical configuration: SessionManager, ClientConnectionManager and
	 * ServerConnectionManager loaded.
	 */
	public static final String GEN_CONFIG_DEF = GEN_CONFIG + "-default";
	/**
	 * Constant <code>GEN_CONFIG_COMP</code> keeps parameter name for
	 * configuration with a single (given as an extra parameter) component
	 * and XEP-0114 component loaded and preconfigured to connect to other
	 * Jabber/XMPP server instance (either Tigase or any different server).
	 */
	public static final String GEN_CONFIG_COMP = GEN_CONFIG + "-comp";

	/**
	 * Constant <code>CLUSTER_MODE</code> sets the cluster mode to either
	 * <code>true</code> or <code>false</code>. By default cluster mode is
	 * set to <code>false</code>.
	 */
	public static final String CLUSTER_MODE = "--cluster-mode";
	/**
	 * Constant <code>CLUSTER_NODES</code> is for setting list of cluster nodes
	 * the instance should try to connect to.
	 */
	public static final String CLUSTER_NODES = "--cluster-nodes";
	public static final String CLUSTER_LISTEN = "cluster-listen";
	public static final String CLUSTER_CONECT = "cluster-connect";

	public static final String GEN_CONF = "--gen-";
	public static final String GEN_TEST = "--test";
	public static final String GEN_COMP_NAME = "--comp-name";
	public static final String GEN_COMP_CLASS = "--comp-class";
	public static final String GEN_EXT_COMP = "--ext-comp";
	public static final String GEN_USER_DB = "--user-db";
	public static final String GEN_AUTH_DB = "--auth-db";
	public static final String GEN_USER_DB_URI = "--user-db-uri";
	public static final String GEN_AUTH_DB_URI = "--auth-db-uri";
	public static final String GEN_ADMINS = "--admins";
	public static final String GEN_TRUSTED = "--trusted";
	public static final String GEN_VIRT_HOSTS = "--virt-hosts";
	public static final String GEN_SM_PLUGINS = "--sm-plugins";
	public static final String GEN_DEBUG = "--debug";

	public static final String GEN_SREC_DB = "--gen-srec-db";
	public static final String GEN_SREC_DB_URI = "--gen-srec-db-uri";
	public static final String GEN_SREC_ADMINS = "--gen-srec-admins";

	public static final String XML_REPO_CLASS_PROP_VAL =
		"tigase.db.xml.XMLRepository";
	public static final String MYSQL_REPO_CLASS_PROP_VAL =
		"tigase.db.jdbc.JDBCRepository";
	public static final String PGSQL_REPO_CLASS_PROP_VAL =
		"tigase.db.jdbc.JDBCRepository";
	public static final String DRUPAL_REPO_CLASS_PROP_VAL =
		"tigase.db.jdbc.DrupalAuth";
	public static final String LIBRESOURCE_REPO_CLASS_PROP_VAL =
		"tigase.db.jdbc.LibreSourceAuth";

	public static final String XML_REPO_URL_PROP_VAL = "user-repository.xml";
	public static final String MYSQL_REPO_URL_PROP_VAL =
		"jdbc:mysql://localhost/tigase?user=root&password=mypass";
	public static final String PGSQL_REPO_URL_PROP_VAL =
		"jdbc:postgresql://localhost/tigase?user=tigase";
	public static final String DRUPAL_REPO_URL_PROP_VAL =
		"jdbc:mysql://localhost/drupal?user=root&password=mypass";
	public static final String LIBRESOURCE_REPO_URL_PROP_VAL =
		"jdbc:postgresql://localhost/libresource?user=demo";

	public static final String DEF_SM_NAME = "sess-man";
	public static final String DEF_C2S_NAME = "c2s";
	public static final String DEF_S2S_NAME = "s2s";
	public static final String DEF_EXT_COMP_NAME = "ext-comp";
	public static final String DEF_CL_COMP_NAME = "cl-comp";
	public static final String DEF_SSEND_NAME = "ssend";
	public static final String DEF_SRECV_NAME = "srecv";
	public static final String DEF_BOSH_NAME = "bosh";

	public static final String DEF_CLUST_CONTR_NAME = "cluster-contr";
	public static final String DEF_STATS_NAME = "stats";

	public static final String ROUTER_COMP_CLASS_NAME =
		"tigase.server.MessageRouter";
	public static final String C2S_COMP_CLASS_NAME =
		"tigase.server.xmppclient.ClientConnectionManager";
	public static final String S2S_COMP_CLASS_NAME =
		"tigase.server.xmppserver.ServerConnectionManager";
	public static final String SM_COMP_CLASS_NAME =
		"tigase.server.xmppsession.SessionManager";
	public static final String SM_CLUS_COMP_CLASS_NAME =
		"tigase.cluster.SessionManagerClustered";
	public static final String EXT_COMP_CLASS_NAME =
		"tigase.server.xmppcomponent.ComponentConnectionManager";
	public static final String CL_COMP_CLASS_NAME =
		"tigase.cluster.ClusterConnectionManager";
	public static final String SSEND_COMP_CLASS_NAME =
		"tigase.server.ssender.StanzaSender";
	public static final String SRECV_COMP_CLASS_NAME =
		"tigase.server.sreceiver.StanzaReceiver";
	public static final String BOSH_COMP_CLASS_NAME =
		"tigase.server.bosh.BoshConnectionManager";

	public static final String STATS_CLASS_NAME =
		"tigase.stats.StatisticsCollector";
	public static final String CLUSTER_CONTR_CLASS_NAME =
    "tigase.cluster.ClusterController";


	public static final String HOSTNAMES_PROP_KEY = "hostnames";
	public static final String ADMINS_PROP_KEY = "admins";
	public static final String TRUSTED_PROP_KEY = "trusted";
	public static final String DEF_HOSTNAME_PROP_KEY = "def-hostname";
	public static final String COMPONENT_ID_PROP_KEY = "component-id";
	public static final String CLUSTER_NODES_PROP_KEY = "cluster-nodes";

	public static final String NULL_ROUTING = "NULL";

	/**
	 * Get object name. This name corresponds to section in configuration.
	 *
	 * @return a <code>String</code> value of object name.
	 */
	String getName();

  /**
   * Sets all configuration properties for the object.
   */
	void setProperties(Map<String, Object> properties);

  /**
   * Returns defualt configuration settings for this object.
   */
	Map<String, Object> getDefaults(Map<String, Object> params);

}
