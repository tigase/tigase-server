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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.annotations.TODO;
import tigase.db.TigaseDBException;
import tigase.xml.db.NodeNotFoundException;
import tigase.xml.db.NodeExistsException;
import tigase.xml.db.XMLDB;
import tigase.xml.db.XMLDBException;

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
@TODO(note="Implement access methods for other primitive types.")
public class ConfigXMLRepository extends ConfigurationCache {

  public static final String XMPP_CONFIG_FILE_PROPERTY_KEY = "xmpp.config.file";
  public static final String XMPP_CONFIG_FILE_PROPERTY_VAL = "xmpp_server.xml";

	public static final String ROOT_NODE = "tigase-config";
  public static final String COMPONENT_NODE = "component";
  private static final Logger log =
    Logger.getLogger("tigase.conf.ConfigRepository");

  private XMLDB xmldb = null;

  private String config_file = null;

  private static Map<String, ConfigXMLRepository> configs =
    new LinkedHashMap<String, ConfigXMLRepository>();
  private static ConfigXMLRepository def_config = null;

  public static ConfigXMLRepository getConfigRepository() throws XMLDBException {
    return getConfigRepository(null);
  }

  public static ConfigXMLRepository getConfigRepository(final String file_name)
	  throws XMLDBException {
    return getConfigRepository(false, file_name);
  }

  public static ConfigXMLRepository getConfigRepository(final boolean debug,
    final String file_name) throws XMLDBException {

    ConfigXMLRepository config = null;
    if (file_name == null) {
      config = def_config;
    } // end of if (file_name == null)
    else {
      config = configs.get(file_name);
    } // end of if (file_name == null) else
    if (config == null) {
      if (file_name == null) {
        config = new ConfigXMLRepository(debug);
      } // end of if (file_name == null)
      else {
        config = new ConfigXMLRepository(debug, file_name);
      } // end of if (file_name == null) else
    } // end of if (config == null)
    return config;
  }

  private ConfigXMLRepository(final boolean debug, final String file)
    throws XMLDBException {
    config_file = file;
    init();
  }

  private ConfigXMLRepository(final boolean debug) throws XMLDBException {
    config_file = System.getProperty(XMPP_CONFIG_FILE_PROPERTY_KEY,
      XMPP_CONFIG_FILE_PROPERTY_VAL);
    init();
    def_config = this;
  }

	public ConfigXMLRepository() {	}

  private void init() throws XMLDBException {
    try {
      if (new File(config_file).exists()) {
				xmldb = new XMLDB(config_file);
			} else {
				xmldb = XMLDB.createDB(config_file, ROOT_NODE, COMPONENT_NODE);
			}
    } catch (IOException e) {
      log.warning("Can not open existing configuration file, creating new one, "
        + e);
      xmldb = XMLDB.createDB(config_file, ROOT_NODE, COMPONENT_NODE);
    } // end of try-catch
    configs.put(config_file, this);
  }

	@Override
	public void init(Map<String, Object> params)
			throws ConfigurationException {
    config_file = (String)params.get("-c");
		try {
			init();
		} catch (XMLDBException ex) {
			throw new ConfigurationException("Can not initialize configuration XML repository: ", ex);
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
		} // end of for (String node : allNodes)
		return result;
	}

	@Override
	public ConfigItem getItem(String compName, String node, String key) {
		try {
			Object value = xmldb.getData(compName, node, key, null);
			ConfigItem item = getItemInstance();
			item.set(compName, node, key, value);
			return item;
		} catch (Exception e) {
			log.log(Level.WARNING, "Can't load value for compName=" + compName
					+ ", node=" + node + ", key=" + key, e);
			return null;
		}
	}

	@Override
	public void addItem(String compName, ConfigItem item) {
		try {
			xmldb.setData(item.getCompName(), item.getNodeName(), item.getKeyName(), 
					item.getConfigVal());
    } catch (NodeNotFoundException e1) {
      try {
        xmldb.addNode1(item.getCompName());
				xmldb.setData(item.getCompName(), item.getNodeName(), item.getKeyName(),
						item.getConfigVal());
      } catch (Exception e2) {
				log.log(Level.WARNING, "Can't add item for compName="
						+ item.getCompName()
						+ ", node=" + item.getNodeName() + ", key=" + item.getKeyName()
						+ ", value=" + item.getConfigValToString(), e2);
			} // end of try-catch
		} catch (Exception e) {
			log.log(Level.WARNING, "Can't add item for compName=" + item.getCompName()
					+ ", node=" + item.getNodeName() + ", key=" + item.getKeyName()
					+ ", value=" + item.getConfigValToString(), e);
		}
	}

	@Override
	public void removeItem(String compName, ConfigItem item) {
		try {
			xmldb.removeData(item.getCompName(), item.getNodeName(), item.getKeyName());
		} catch (Exception e) {
			log.log(Level.WARNING, "Can't remove item for compName=" + item.getCompName()
					+ ", node=" + item.getNodeName() + ", key=" + item.getKeyName()
					+ ", value=" + item.getConfigValToString(), e);
		}
	}

	@Override
  public String[] getCompNames() {
		List<String> comps = xmldb.getAllNode1s();
		if (comps != null) {
			return comps.toArray(new String[comps.size()]);
		} // end of if (comps != null)
    return null;
  }

	@Override
  public String[] getKeys(final String root, final String node) {
    try {
      return xmldb.getKeys(root, node);
    } // end of try
    catch (NodeNotFoundException e) {
      return null;
    } // end of try-catch
  }

	@Override
	public int size() {
		return (int)xmldb.getAllNode1sCount();
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

	/** ************ Old code ************ **/

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
					log.log(Level.WARNING, "Can't load value for compName=" + compName
							+ ", node=" + node + ", key=" + key, ex);
				}
			} // end of for (String key : keys)
		} // end of if (keys != null)
	}

	private void getSubnodes(List<String> result, String root, String node) {
		String[] subnodes = getSubnodes(root, node);
		String node_tmp = (node.equals("") ? node : node + "/");
		if (subnodes != null) {
			for (String subnode : subnodes) {
				result.add(node_tmp + subnode);
				log.config("Adding subnode: " + node_tmp + subnode);
				getSubnodes(result, root, node_tmp + subnode);
			} // end of for (String subnode : subnodes)
		} // end of if (subnodes != null)
	}

	@Override
	public void store() throws TigaseDBException {
		try {
			xmldb.sync();
		} catch (IOException ex) {
			throw new TigaseDBException("Problem saving configuration data: ", ex);
		}
	}

  private String[] getSubnodes(final String root, final String node) {
    try {
      return xmldb.getSubnodes(root, node);
    } catch (NodeNotFoundException e) {
      return null;
    } // end of try-catch
  }

  public String nodeForPackage(final Class cls) {
    return cls.getPackage().getName().replace('.', '/');
  }

} // ConfigXMLRepository
