
/*
* @(#)ConfigXMLRepository.java   2010.01.14 at 04:39:54 PST
*
* Tigase Jabber/XMPP Server
* Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.conf;

//~--- non-JDK imports --------------------------------------------------------

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.db.DBInitException;
import tigase.db.TigaseDBException;
import tigase.xml.db.NodeNotFoundException;
import tigase.xml.db.XMLDB;
import tigase.xml.db.XMLDBException;

//~--- classes ----------------------------------------------------------------

/**
 * Class <code>ConfigXMLRepository</code> provides access to configuration
 * settings.
 *
 * <p>
 * Created: Sat Nov 13 18:53:21 2004
 * </p>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ConfigXMLRepository extends ConfigurationCache {

	/** Field description */
	public static final String COMPONENT_NODE = "component";

	/** Field description */
	public static final String ROOT_NODE = "tigase-config";

	/** Field description */
	public static final String XMPP_CONFIG_FILE_PROPERTY_KEY = "xmpp.config.file";

	/** Field description */
	public static final String XMPP_CONFIG_FILE_PROPERTY_VAL = "xmpp_server.xml";
	private static ConfigXMLRepository def_config = null;
	private static final Logger log = Logger.getLogger("tigase.conf.ConfigRepository");
	private static Map<String, ConfigXMLRepository> configs = new LinkedHashMap<String,
																															ConfigXMLRepository>();

	//~--- fields ---------------------------------------------------------------

	private String config_file = null;
	private XMLDB xmldb = null;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 */
	public ConfigXMLRepository() {}

	private ConfigXMLRepository(final boolean debug) throws XMLDBException {
		config_file = System.getProperty(XMPP_CONFIG_FILE_PROPERTY_KEY,
																		 XMPP_CONFIG_FILE_PROPERTY_VAL);
		init();
		def_config = this;
	}

	private ConfigXMLRepository(final boolean debug, final String file)
					throws XMLDBException {
		config_file = file;
		init();
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * 
	 *
	 * @throws XMLDBException
	 */
	public static ConfigXMLRepository getConfigRepository() throws XMLDBException {
		return getConfigRepository(null);
	}

	/**
	 * Method description
	 *
	 *
	 * @param file_name
	 *
	 * 
	 *
	 * @throws XMLDBException
	 */
	public static ConfigXMLRepository getConfigRepository(final String file_name)
					throws XMLDBException {
		return getConfigRepository(false, file_name);
	}

	/**
	 * Method description
	 *
	 *
	 * @param debug
	 * @param file_name
	 *
	 * 
	 *
	 * @throws XMLDBException
	 */
	public static ConfigXMLRepository getConfigRepository(final boolean debug,
					final String file_name)
					throws XMLDBException {
		ConfigXMLRepository config = null;

		if (file_name == null) {
			config = def_config;
		}    // end of if (file_name == null)
						else {
			config = configs.get(file_name);
		}    // end of if (file_name == null) else

		if (config == null) {
			if (file_name == null) {
				config = new ConfigXMLRepository(debug);
			}    // end of if (file_name == null)
							else {
				config = new ConfigXMLRepository(debug, file_name);
			}    // end of if (file_name == null) else
		}      // end of if (config == null)

		return config;
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public void addItem(String compName, ConfigItem item) {
		try {
			xmldb.setData(item.getCompName(),
										item.getNodeName(),
										item.getKeyName(),
										item.getConfigVal());
		} catch (NodeNotFoundException e1) {
			try {
				xmldb.addNode1(item.getCompName());
				xmldb.setData(item.getCompName(),
											item.getNodeName(),
											item.getKeyName(),
											item.getConfigVal());
			} catch (Exception e2) {
				log.log(Level.WARNING,
								"Can't add item for compName=" + item.getCompName() + ", node="
								+ item.getNodeName() + ", key=" + item.getKeyName() + ", value="
								+ item.getConfigValToString(),
								e2);
			}    // end of try-catch
		} catch (Exception e) {
			log.log(Level.WARNING,
							"Can't add item for compName=" + item.getCompName() + ", node="
							+ item.getNodeName() + ", key=" + item.getKeyName() + ", value="
							+ item.getConfigValToString(),
							e);
		}
	}

	@Override
	public Collection<ConfigItem> allItems() throws TigaseDBException {
		Set<ConfigItem> result = new LinkedHashSet<ConfigItem>();
		String[] compNames = getCompNames();

		if (compNames != null) {
			for (String comp : compNames) {
				result.addAll(getItemsForComponent(comp));
			}
		}

		return result;
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public String[] getCompNames() {
		List<String> comps = xmldb.getAllNode1s();

		if (comps != null) {
			return comps.toArray(new String[comps.size()]);
		}    // end of if (comps != null)

		return null;
	}

	@Override
	public ConfigItem getItem(String compName, String node, String key) {
		try {
			Object value = xmldb.getData(compName, node, key, null);
			ConfigItem item = getItemInstance();

			item.set(compName, node, key, value);

			return item;
		} catch (Exception e) {
			log.log(Level.WARNING,
							"Can't load value for compName=" + compName + ", node=" + node + ", key="
							+ key,
							e);

			return null;
		}
	}

	@Override
	public Set<ConfigItem> getItemsForComponent(String compName) {
		Set<ConfigItem> result = new LinkedHashSet<ConfigItem>();
		List<String> allNodes = new ArrayList<String>();
		String subnode = "";

		getSubnodes(allNodes, compName, subnode);

		String[] keys = getKeys(compName, null);

		log.config("Found keys: " + Arrays.toString(keys));
		addVals(result, compName, null, keys);

		for (String node : allNodes) {
			keys = getKeys(compName, node);
			log.config("In node : '" + node + "' found keys: " + Arrays.toString(keys));
			addVals(result, compName, node, keys);
		}    // end of for (String node : allNodes)

		return result;
	}

	@Override
	public String[] getKeys(final String root, final String node) {
		try {
			return xmldb.getKeys(root, node);
		}    // end of try
						catch (NodeNotFoundException e) {
			return null;
		}    // end of try-catch
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public void initRepository(String repo_uri, Map<String, String> params) throws DBInitException {
		config_file = (String) params.get("-c");

		try {
			init();
		} catch (XMLDBException ex) {
			throw new DBInitException("Can not initialize configuration repository: ",
																			 ex);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param cls
	 *
	 * 
	 */
	public String nodeForPackage(Class cls) {
		return cls.getPackage().getName().replace('.', '/');
	}

	@Override
	public void removeItem(String compName, ConfigItem item) {
		try {
			xmldb.removeData(item.getCompName(), item.getNodeName(), item.getKeyName());
		} catch (Exception e) {
			log.log(Level.WARNING,
							"Can't remove item for compName=" + item.getCompName() + ", node="
							+ item.getNodeName() + ", key=" + item.getKeyName() + ", value="
							+ item.getConfigValToString(),
							e);
		}
	}

	@Override
	public int size() {
		return (int) xmldb.getAllNode1sCount();
	}

	@Override
	public void store() throws TigaseDBException {
		try {
			xmldb.sync();
		} catch (IOException ex) {
			throw new TigaseDBException("Problem saving configuration data: ", ex);
		}
	}

	/** ************ Old code ************ */
	private void addVals(Set<ConfigItem> props, String compName, String node,
											 String[] keys) {
		if (keys != null) {
			for (String key : keys) {
				try {
					Object value = xmldb.getData(compName, node, key, null);
					ConfigItem item = getItemInstance();

					item.set(compName, node, key, value);
					props.add(item);
				} catch (NodeNotFoundException ex) {
					log.log(Level.WARNING,
									"Can't load value for compName=" + compName + ", node=" + node
									+ ", key=" + key,
									ex);
				}
			}    // end of for (String key : keys)
		}      // end of if (keys != null)
	}

	//~--- get methods ----------------------------------------------------------

	private void getSubnodes(List<String> result, String root, String node) {
		String[] subnodes = getSubnodes(root, node);
		String node_tmp = (node.equals("") ? node : node + "/");

		if (subnodes != null) {
			for (String subnode : subnodes) {
				result.add(node_tmp + subnode);
				log.config("Adding subnode: " + node_tmp + subnode);
				getSubnodes(result, root, node_tmp + subnode);
			}    // end of for (String subnode : subnodes)
		}      // end of if (subnodes != null)
	}

	private String[] getSubnodes(final String root, final String node) {
		try {
			return xmldb.getSubnodes(root, node);
		} catch (NodeNotFoundException e) {
			return null;
		}    // end of try-catch
	}

	//~--- methods --------------------------------------------------------------

	private void init() throws XMLDBException {
		try {
			if (new File(config_file).exists()) {
				xmldb = new XMLDB(config_file);
			} else {
				xmldb = XMLDB.createDB(config_file, ROOT_NODE, COMPONENT_NODE);
			}
		} catch (IOException e) {
			log.warning("Can not open existing configuration file, creating new one, " + e);
			xmldb = XMLDB.createDB(config_file, ROOT_NODE, COMPONENT_NODE);
		}    // end of try-catch

		configs.put(config_file, this);
	}
}    // ConfigXMLRepository


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
