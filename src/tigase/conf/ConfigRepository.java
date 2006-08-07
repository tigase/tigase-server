/*  Package Tigase XMPP/Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.conf;

import java.io.IOException;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import tigase.annotations.TODO;
import tigase.xml.db.NodeNotFoundException;
import tigase.xml.db.NodeExistsException;
import tigase.xml.db.XMLDB;

/**
 * Class <code>ConfigRepository</code> provides access to configuration
 * settings.
 *
 * <p>
 * Created: Sat Nov 13 18:53:21 2004
 * </p>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@TODO(note="Implement access methods for other primitive types.")
public class ConfigRepository {

  public static final String XMPP_CONFIG_FILE_PROPERTY_KEY = "xmpp.config.file";
  public static final String XMPP_CONFIG_FILE_PROPERTY_VAL = "xmpp_server.xml";

	public static final String ROOT_NODE = "tigase-config";
  public static final String COMPONENT_NODE = "component";
  private static final Logger log =
    Logger.getLogger("tigase.conf.ConfigRepository");

  private XMLDB xmldb = null;

  private String config_file = null;

  private static Map<String, ConfigRepository> configs =
    new HashMap<String, ConfigRepository>();
  private static ConfigRepository def_config = null;

  public static ConfigRepository getConfigRepository() {
    return getConfigRepository(null);
  }

  public static ConfigRepository getConfigRepository(final String file_name) {
    return getConfigRepository(false, file_name);
  }

  public static ConfigRepository getConfigRepository(final boolean debug,
    final String file_name) {

    ConfigRepository config = null;
    if (file_name == null) {
      config = def_config;
    } // end of if (file_name == null)
    else {
      config = configs.get(file_name);
    } // end of if (file_name == null) else
    if (config == null) {
      if (file_name == null) {
        config = new ConfigRepository(debug);
      } // end of if (file_name == null)
      else {
        config = new ConfigRepository(debug, file_name);
      } // end of if (file_name == null) else
    } // end of if (config == null)
    return config;
  }

  private ConfigRepository(final boolean debug, final String file) {
    config_file = file;
    init();
  }

  private ConfigRepository(final boolean debug) {
    config_file = System.getProperty(XMPP_CONFIG_FILE_PROPERTY_KEY,
      XMPP_CONFIG_FILE_PROPERTY_VAL);
    init();
    def_config = this;
  }

  private void init() {
    try {
      //      xmldb = new XMLDB(config_file, "preferences", "root");
      xmldb = new XMLDB(config_file);
    } catch (IOException e) {
      log.warning("Can not open existing configuration file, creating new one, "
        + e);
      xmldb = XMLDB.createDB(config_file, ROOT_NODE, COMPONENT_NODE);
    } // end of try-catch
    configs.put(config_file, this);
  }

	public Map<String, Object> getProperties(String nodeId) {
		log.config("Reading properties for: " + nodeId);
		String root = nodeId;
		String subnode = "";
		int idx = nodeId.indexOf('/');
		if (idx >= 0) {
			root = nodeId.substring(0, idx);
			subnode = nodeId.substring(idx+1);
		} // end of if (idx >= 0)
		log.config("Looking for properties for " + root + " in " + subnode + " node.");
		List<String> allNodes = new ArrayList<String>();
		getSubnodes(allNodes, root, subnode);
		Map<String, Object> props = new TreeMap<String, Object>();
		String[] keys = getKeys(root, null);
		log.config("Found keys: " + Arrays.toString(keys));
		addVals(props, root, null, keys);
		for (String node : allNodes) {
			keys = getKeys(root, node);
			log.config("In node : '" + node + "' found keys: " + Arrays.toString(keys));
			addVals(props, root, node, keys);
		} // end of for (String node : allNodes)
		return props;
	}

	private void addVals(Map<String, Object> props, String root, String node,
		String[] keys) {
		if (keys != null) {
			for (String key : keys) {
				String node_tmp = (node == null || node.equals("")) ? "" : node + "/";
				props.put(node_tmp + key, get(root, node, key, null));
			} // end of for (String key : keys)
		} // end of if (keys != null)
	}

	public void putProperties(String nodeId, Map<String, ?> props) {
		log.config("Saving properties for: " + nodeId);
		String root = nodeId;
		String subnode = "";
		int idx = nodeId.indexOf('/');
		if (idx >= 0) {
			root = nodeId.substring(0, idx);
			subnode = nodeId.substring(idx+1) + "/";
		} // end of if (idx >= 0)
		try {	xmldb.addNode1(root);
		} catch (NodeExistsException e) {	} // end of try-catch
		for (Map.Entry<String, ?> entry : props.entrySet()) {
			String node = null;
			String key = subnode + entry.getKey();
			idx = key.lastIndexOf('/');
			if (idx >= 0) {
				node = key.substring(0, idx);
				key = key.substring(idx+1);
			} // end of if (idx >= 0)
			Object value = entry.getValue();
			log.config("Setting property: root=" + root + ", node=" + node
				+ ", key=" + key + ", value=" + value);
			set(root, node, key, value);
		} // end of for ()
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

	public void sync() throws IOException {
		xmldb.sync();
	}

	public Object get(final String key) {
    return get(key, null);
  }

  public Object get(final String key, final Object def) {
    return get(null, key, def);
  }

  public Object get(final String node, final String key, final Object def) {
    return get(COMPONENT_NODE, node, key, def);
  }

  public Object get(final String root, final String node, final String key,
    final Object def) {
    Object result;
    try {
      result = xmldb.getData(root, node, key, def);
    } // end of try
    catch (NodeNotFoundException e) {
      result = def;
    } // end of try-catch
    return result;
  }

  public void set(final String key, final Object value) {
    set(null, key, value);
  }

  public void set(final String node, final String key, final Object value) {
    set(COMPONENT_NODE, node, key, value);
  }

  public void set(final String root, final String node, final String key,
    final Object value) {
    try {
      xmldb.setData(root, node, key, value);
    } // end of try
    catch (NodeNotFoundException e1) {
      try {
        xmldb.addNode1(root);
        xmldb.setData(root, node, key, value);
      } catch (Exception e2) { } // end of try-catch
    } // end of try-catch
  }

//   public void setInt(final String key, final int value) {
//     setInt(null, key, value);
//   }

//   public void setInt(final String node, final String key, final int value) {
//     setInt(COMPONENT_NODE, node, key, value);
//   }

//   public void setInt(final String root, final String node, final String key,
//     final int value) {
//     set(root, node, key, Integer.toString(value));
//   }

//   public int getInt(final String key, final int def) {
//     return getInt(null, key, def);
//   }

//   public int getInt(final String node, final String key, final int def) {
//     return getInt(COMPONENT_NODE, node, key, def);
//   }

//   public int getInt(final String root, final String node, final String key,
//     final int def) {
//     try {
//       return get(root, node, key, def);
//     } // end of try
//     catch (NumberFormatException e) {
//       return def;
//     } // end of try-catch
//   }

//   public boolean getBoolean(final String node, final String key,
//     final boolean def) {
//     return getBoolean(COMPONENT_NODE, node, key, def);
//   }

//   public boolean getBoolean(final String root, final String node,
//     final String key, final boolean def) {
//     final String val = get(root, node, key, Boolean.toString(def));
//     return (val != null &&
//       (val.equalsIgnoreCase("yes")
//         || val.equalsIgnoreCase("true")
//         || val.equalsIgnoreCase("on")));
//   }

//   public void setList(final String key, final String[] list) {
//     setList(null, key, list);
//   }

//   public void setList(final String node, final String key,
//     final String[] list) {
//     setList(COMPONENT_NODE, node, key, list);
//   }

//   public void setList(final String root, final String node, final String key,
//     final String[] list) {
//     try {
//       xmldb.setData(root, node, key, list);
//     } // end of try
//     catch (NodeNotFoundException e1) {
//       try {
//         xmldb.addNode1(root);
//         xmldb.setData(root, node, key, list);
//       } catch (Exception e2) { } // end of try-catch
//     } // end of try-catch
//   }

//   public String[] getList(final String key) {
//     return getList(null, key);
//   }

//   public String[] getList(final String node, final String key) {
//     return getList(COMPONENT_NODE, node, key);
//   }

//   public String[] getList(final String root, final String node,
//     final String key) {
//     try {
//       return xmldb.getDataList(root, node, key);
//     } // end of try
//     catch (NodeNotFoundException e) {
//       return null;
//     } // end of try-catch
//   }

  public String[] getSubnodes() {
    return getSubnodes(null);
  }

  public String[] getSubnodes(final String node) {
    return getSubnodes(COMPONENT_NODE, node);
  }

  public String[] getSubnodes(final String root, final String node) {
    try {
      return xmldb.getSubnodes(root, node);
    } // end of try
    catch (NodeNotFoundException e) {
      return null;
    } // end of try-catch
  }

  public String[] getKeys() {
    return getKeys(null);
  }

  public String[] getKeys(final String node) {
    return getKeys(COMPONENT_NODE, node);
  }

  public String[] getKeys(final String root, final String node) {
    try {
      return xmldb.getKeys(root, node);
    } // end of try
    catch (NodeNotFoundException e) {
      return null;
    } // end of try-catch
  }

  public void removeData(final String key) {
    removeData(null, key);
  }

  public void removeData(final String node, final String key) {
    removeData(COMPONENT_NODE, node, key);
  }

  public void removeData(final String root, final String node,
    final String key) {
    try {
      xmldb.removeData(root, node, key);
    } // end of try
    catch (NodeNotFoundException e) {
      log.warning("Attempt to remove data for non-existen node: /"
        +root+"/"+node);
    } // end of try-catch
  }

  public void removeSubnode(final String node) {
    removeSubnode(COMPONENT_NODE, node);
  }

  public void removeSubnode(final String root, final String node) {
    try {
      xmldb.removeSubnode(root, node);
    } // end of try
    catch (NodeNotFoundException e) {
      log.warning("Attempt to remove non-existen node: /"
        +root+"/"+node);
    } // end of try-catch
  }

  public String nodeForPackage(final Class cls) {
    return cls.getPackage().getName().replace('.', '/');
  }

} // ConfigRepository
