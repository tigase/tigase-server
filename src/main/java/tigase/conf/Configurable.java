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

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.ServerComponent;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

//~--- interfaces -------------------------------------------------------------

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
	 * and XEP-0114 or XEP-0225 component loaded and preconfigured to connect to other
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

	/** Field description */
	public static final String CLUSTER_LISTEN = "cluster-listen";

	/** Field description */
	public static final String CLUSTER_CONECT = "cluster-connect";

	/** Field description */
	public static final String GEN_CONF = "--gen-";

	/** Field description */
	public static final String GEN_TEST = "--test";

	/** Field description */
	public static final String GEN_COMP_NAME = "--comp-name";

	/** Field description */
	public static final String GEN_COMP_CLASS = "--comp-class";

	/** Field description */
	public static final String GEN_EXT_COMP = "--ext-comp";

	/** Field description */
	public static final String GEN_USER_DB = "--user-db";

	/** Field description */
	public static final String USER_REPO_POOL_CLASS = "--user-repo-pool";

	/** Field description */
	public static final String USER_DOMAIN_POOL_CLASS = "--user-domain-repo-pool";

	/** Field description */
	public static final String GEN_AUTH_DB = "--auth-db";

	/** Field description */
	public static final String AUTH_REPO_POOL_CLASS = "--auth-repo-pool";

	/** Field description */
	public static final String AUTH_DOMAIN_POOL_CLASS = "--auth-domain-repo-pool";

	/** Field description */
	public static final String GEN_USER_DB_URI_PROP_KEY = "user-db-uri";

	/** Field description */
	public static final String GEN_USER_DB_URI = "--" + GEN_USER_DB_URI_PROP_KEY;

	/** Field description */
	public static final String GEN_AUTH_DB_URI = "--auth-db-uri";

	/** Field description */
	public static final String GEN_ADMINS = "--admins";

	/** Field description */
	public static final String GEN_TRUSTED = "--trusted";

	/** Field description */
	public static final String GEN_VIRT_HOSTS = "--virt-hosts";

	/** Field description */
	public static final String GEN_SM_PLUGINS = "--sm-plugins";

	/** Field description */
	public static final String GEN_DEBUG = "--debug";

	/** Field description */
	public static final String GEN_DEBUG_PACKAGES = "--debug-packages";

	/** Field description */
	public static final String GEN_MAX_QUEUE_SIZE = "--max-queue-size";

	/** Field description */
	public static final String GEN_SCRIPT_DIR = "--script-dir";

	/** Field description */
	public static final String GEN_SREC_DB = "--gen-srec-db";

	/** Field description */
	public static final String GEN_SREC_DB_URI = "--gen-srec-db-uri";

	/** Field description */
	public static final String GEN_SREC_ADMINS = "--gen-srec-admins";

	/** Field description */
	public static final String MONITORING = "--monitoring";

	/** Field description */
	public static final String USER_REPO_POOL_SIZE = "--user-repo-pool-size";
	
	public static final String XMPP_STANZA_ACK = "--stanza-ack";
	public static final String STANZA_WHITE_CHAR_ACK = "white-char";
	public static final String STANZA_XMPP_ACK = "xmpp";

	/** Field description */
	public static final String STRINGPREP_PROCESSOR = "--stringprep-processor";

	/** Field description */
	public static final String XML_REPO_CLASS_PROP_VAL = "tigase.db.xml.XMLRepository";

	/** Field description */
	public static final String DUMMY_REPO_CLASS_PROP_VAL = "tigase.db.DummyRepository";

	/** Field description */
	public static final String MYSQL_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.JDBCRepository";

	/** Field description */
	public static final String DERBY_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.JDBCRepository";

	/** Field description */
	public static final String PGSQL_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.JDBCRepository";

	/** Field description */
	public static final String TIGASE_AUTH_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.TigaseAuth";

	/** Field description */
	public static final String TIGASE_CUSTOM_AUTH_REPO_CLASS_PROP_VAL =
		"tigase.db.jdbc.TigaseCustomAuth";

	/** Field description */
	public static final String DRUPALWP_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.DrupalWPAuth";

	/** Field description */
	public static final String LIBRESOURCE_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.LibreSourceAuth";

	/** Field description */
	public static final String SHARED_USER_REPO_PROP_KEY = "shared-user-repo";

	/** Field description */
	public static final String SHARED_USER_REPO_PARAMS_PROP_KEY = "shared-user-repo-params";

	/** Field description */
	public static final String SHARED_AUTH_REPO_PROP_KEY = "shared-auth-repo";

	/** Field description */
	public static final String SHARED_AUTH_REPO_PARAMS_PROP_KEY = "shared-auth-repo-params";

///** Field description */
//public static final String SHARED_USER_REPO_POOL_PROP_KEY = "shared-user-repo-pool";
///** Field description */
//public static final String SHARED_USER_AUTH_REPO_POOL_PROP_KEY = "shared-user-auth-repo-pool";

	/** Field description */
	public static final String XML_REPO_URL_PROP_VAL = "user-repository.xml";

	/** Field description */
	public static final String MYSQL_REPO_URL_PROP_VAL =
		"jdbc:mysql://localhost/tigase?user=root&password=mypass";

	/** Field description */
	public static final String DERBY_REPO_URL_PROP_VAL = "jdbc:derby:tigase-derbydb;create=true";

	/** Field description */
	public static final String PGSQL_REPO_URL_PROP_VAL =
		"jdbc:postgresql://localhost/tigase?user=tigase";

	/** Field description */
	public static final String TIGASE_AUTH_REPO_URL_PROP_VAL =
		"jdbc:mysql://localhost/tigasedb?user=tigase_user&password=mypass";

	/** Field description */
	public static final String DRUPAL_REPO_URL_PROP_VAL =
		"jdbc:mysql://localhost/drupal?user=root&password=mypass";

	/** Field description */
	public static final String LIBRESOURCE_REPO_URL_PROP_VAL =
		"jdbc:postgresql://localhost/libresource?user=demo";

	/** Field description */
	public static final String DEF_SM_NAME = "sess-man";

	/** Field description */
	public static final String DEF_MONITOR_NAME = "monitor";

	/** Field description */
	public static final String DEF_C2S_NAME = "c2s";

	/** Field description */
	public static final String DEF_S2S_NAME = "s2s";

	/** Field description */
	public static final String DEF_EXT_COMP_NAME = "ext-comp";

	/** Field description */
	public static final String DEF_COMP_PROT_NAME = "ext";

	/** Field description */
	public static final String DEF_CL_COMP_NAME = "cl-comp";

	/** Field description */
	public static final String DEF_SSEND_NAME = "ssend";

	/** Field description */
	public static final String DEF_SRECV_NAME = "srecv";

	/** Field description */
	public static final String DEF_BOSH_NAME = "bosh";

	/** Field description */
	public static final String DEF_STATS_NAME = "stats";

	/** Field description */
	public static final String DEF_CLUST_CONTR_NAME = "cluster-contr";

	/** Field description */
	public static final String DEF_VHOST_MAN_NAME = "vhost-man";

	/** Field description */
	public static final String ROUTER_COMP_CLASS_NAME = "tigase.server.MessageRouter";

	/** Field description */
	public static final String C2S_COMP_CLASS_NAME =
		"tigase.server.xmppclient.ClientConnectionManager";

	/** Field description */
	public static final String C2S_CLUST_COMP_CLASS_NAME = "tigase.cluster.ClientConnectionClustered";

	/** Field description */
	public static final String S2S_COMP_CLASS_NAME = "tigase.server.xmppserver.S2SConnectionManager";

	/** Field description */
	public static final String S2S_CLUST_COMP_CLASS_NAME = "tigase.cluster.S2SConnectionClustered";

	/** Field description */
	public static final String SM_COMP_CLASS_NAME = "tigase.server.xmppsession.SessionManager";

	/** Field description */
	public static final String SM_CLUST_COMP_CLASS_NAME = "tigase.cluster.SessionManagerClustered";

	/** Field description */
	public static final String EXT_COMP_CLASS_NAME =
		"tigase.server.xmppcomponent.ComponentConnectionManager";

	/** Field description */
	public static final String MONITOR_CLASS_NAME = "tigase.server.monitor.MonitorComponent";

	/** Field description */
	public static final String MONITOR_CLUST_CLASS_NAME = "tigase.cluster.MonitorClustered";

	/** Field description */
	public static final String COMP_PROT_CLASS_NAME = "tigase.server.ext.ComponentProtocol";

	/** Field description */
	public static final String CL_COMP_CLASS_NAME = "tigase.cluster.ClusterConnectionManager";

	/** Field description */
	public static final String SSEND_COMP_CLASS_NAME = "tigase.server.ssender.StanzaSender";

	/** Field description */
	public static final String SRECV_COMP_CLASS_NAME = "tigase.server.sreceiver.StanzaReceiver";

	/** Field description */
	public static final String BOSH_COMP_CLASS_NAME = "tigase.server.bosh.BoshConnectionManager";

	/** Field description */
	public static final String STATS_CLASS_NAME = "tigase.stats.StatisticsCollector";

	/** Field description */
	public static final String CLUSTER_CONTR_CLASS_NAME = "tigase.cluster.ClusterController";

	/** Field description */
	public static final String VHOST_MAN_CLASS_NAME = "tigase.vhosts.VHostManager";

	/** Field description */
	public static final String USER_REPO_URL_PROP_KEY = "user-repo-url";

	/** Field description */
	public static final String USER_REPO_PARAMS_NODE = "user-repo-params";

	/** Field description */
	public static final String USER_REPO_POOL_SIZE_PROP_KEY = "user-repo-pool-size";

	/** Field description */
	public static final String USER_REPO_DOMAINS_PROP_KEY = "user-repo-domains";

	/** Field description */
	public static final String AUTH_REPO_DOMAINS_PROP_KEY = "auth-repo-domains";

	/** Field description */
	public static final String AUTH_REPO_URL_PROP_KEY = "auth-repo-url";

	/** Field description */
	public static final String AUTH_REPO_PARAMS_NODE = "auth-repo-params";

	/** Field description */
	public static final String HOSTNAMES_PROP_KEY = "hostnames";

	/** Field description */
	public static final String ADMINS_PROP_KEY = "admins";

	/** Field description */
	public static final String TRUSTED_PROP_KEY = "trusted";

	/** Field description */
	public static final String DEF_HOSTNAME_PROP_KEY = "def-hostname";

	/** Field description */
	public static final String COMPONENT_ID_PROP_KEY = "component-id";

	/** Field description */
	public static final String CLUSTER_NODES_PROP_KEY = "cluster-nodes";

	/** Field description */
	public static final String DEF_AMP_NAME = "amp";

	/** Field description */
	public static final String AMP_CLASS_NAME = "tigase.server.amp.AmpComponent";

	//~--- get methods ----------------------------------------------------------

	/**
	 * Returns default configuration settings for this object.
	 * @param params
	 * @return
	 */
	Map<String, Object> getDefaults(Map<String, Object> params);

	//~--- set methods ----------------------------------------------------------

	/**
	 * Sets all configuration properties for the object.
	 * @param properties
	 */
	void setProperties(Map<String, Object> properties);
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
