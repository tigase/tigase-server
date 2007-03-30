/*
 *  Package Tigase XMPP/Jabber Server
 *  Copyright (C) 2004, 2005, 2006
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import tigase.server.AbstractComponentRegistrator;
import tigase.server.Packet;
import tigase.server.ServerComponent;
import tigase.server.Command;
import tigase.server.Permissions;
import tigase.server.MessageRouter;
import tigase.disco.XMPPService;
import tigase.disco.ServiceEntity;
import tigase.disco.ServiceIdentity;
import tigase.xml.Element;
import tigase.xml.XMLUtils;
import tigase.xml.db.Types.DataType;
import tigase.xmpp.Authorization;

/**
 * Class Configurator
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Configurator extends AbstractComponentRegistrator<Configurable>
	implements Configurable, XMPPService {

	private static final String LOGGING_KEY = "logging/";

  private static final Logger log =
		Logger.getLogger("tigase.conf.Configurator");

	private ConfigRepository repository = null;
	private Timer delayedTask = new Timer("ConfiguratorTask", true);
	private Map<String, Object> defConfigParams = new HashMap<String, Object>();
	private ServiceEntity serviceEntity = null;
	private ServiceEntity config_list = null;
	private ServiceEntity config_set = null;
	private boolean demoMode = false;

	public void setName(String name) {
		super.setName(name);
		serviceEntity = new ServiceEntity(name, "config", "Server configuration");
		serviceEntity.addIdentities(new ServiceIdentity[] {
				new ServiceIdentity("automation", "command-list",
					"Configuration commands")});
		serviceEntity.addFeatures(DEF_FEATURES);
		config_list = new ServiceEntity(name, "list", "List");
		config_list.addIdentities(new ServiceIdentity[] {
				new ServiceIdentity("automation", "command-list",
					"Config listings")});
		config_list.addFeatures(DEF_FEATURES);
		config_set = new ServiceEntity(name, "set", "Set");
		config_set.addIdentities(new ServiceIdentity[] {
				new ServiceIdentity("automation", "command-list",
					"Config settings")});
		config_set.addFeatures(DEF_FEATURES);
		serviceEntity.addItems(new ServiceEntity[] {config_list, config_set});
	}

	public void parseArgs(final String[] args) {
		defConfigParams.put("--test", new Boolean(false));
		defConfigParams.put("config-type", "--gen-config-default");
    if (args != null && args.length > 0) {
      for (int i = 0; i < args.length; i++) {
				if (args[i].startsWith("--gen-config")) {
					defConfigParams.put("config-type", args[i]);
				}
				if (args[i].startsWith("--test")) {
					defConfigParams.put(args[i], new Boolean(true));
				}
				if (args[i].equals("--user-db") || args[i].equals("--user-db-uri")
					|| args[i].equals("--auth-db") || args[i].equals("--auth-db-uri")
					|| args[i].equals("--ext-comp") || args[i].equals("--virt-hosts")
					|| args[i].equals("--admins") || args[i].equals("--debug")) {
					defConfigParams.put(args[i], args[++i]);
				}
      } // end of for (int i = 0; i < args.length; i++)
    }
  }

	public Configurator(String fileName, String[] args) {
		parseArgs(args);
		repository = ConfigRepository.getConfigRepository(fileName);
		defConfigParams.putAll(getAllProperties(null));
	}

	public boolean isCorrectType(ServerComponent component) {
		return component instanceof Configurable;
	}

	public void componentAdded(Configurable component) {
		log.finer(" component: " + component.getName());
		ServiceEntity item = config_list.findNode(component.getName());
		if (item == null) {
			item = new ServiceEntity(getName(), component.getName(),
				"Component: " + component.getName());
			item.addFeatures(CMD_FEATURES);
			item.addIdentities(new ServiceIdentity[] {
					new ServiceIdentity("automation", "command-node",
						"Component: " + component.getName())});
			config_list.addItems(new ServiceEntity[] {item});
		}
		if (config_set.findNode(component.getName()) == null) {
			config_set.addItems(new ServiceEntity[] {item});
		}
		setup(component);
	}

	public void componentRemoved(Configurable component) {}

	public void setup(String name) {
		Configurable component = getComponent(name);
		setup(component);
	}

	public void setup(Configurable component) {
		String compId = component.getName();
		Map<String, Object> prop = repository.getProperties(compId);
		Map<String, Object> defs = component.getDefaults(defConfigParams);
		Set<Map.Entry<String, Object>> defs_entries = defs.entrySet();
		boolean modified = false;
		for (Map.Entry<String, Object> entry : defs_entries) {
			if (!prop.containsKey(entry.getKey())) {
				prop.put(entry.getKey(), entry.getValue());
				modified = true;
			} // end of if ()
		} // end of for ()
		if (modified) {
			repository.putProperties(compId, prop);
			try {
				repository.sync();
			} catch (Exception e) {
				e.printStackTrace();
			} // end of try-catch
		} // end of if (modified)
		component.setProperties(prop);
	}

// 	public String getName() {
// 		return "basic-conf";
// 	}

	/**
   * Returns defualt configuration settings in case if there is no
   * config file.
   */
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defaults = new TreeMap<String, Object>();
		if ((Boolean)params.get("--test")) {
			defaults.put(LOGGING_KEY + ".level", "WARNING");
			defaults.put(LOGGING_KEY + "java.util.logging.FileHandler.level", "WARNING");
		} else {
			defaults.put(LOGGING_KEY + ".level", "FINE");
			defaults.put(LOGGING_KEY + "java.util.logging.FileHandler.level", "ALL");
		}
		defaults.put(LOGGING_KEY + "handlers",
			"java.util.logging.ConsoleHandler java.util.logging.FileHandler");
		defaults.put(LOGGING_KEY + "java.util.logging.ConsoleHandler.formatter",
			"tigase.util.LogFormatter");
		defaults.put(LOGGING_KEY + "java.util.logging.ConsoleHandler.level",
			"WARNING");
		defaults.put(LOGGING_KEY + "java.util.logging.FileHandler.append", "true");
		defaults.put(LOGGING_KEY + "java.util.logging.FileHandler.count", "5");
		defaults.put(LOGGING_KEY + "java.util.logging.FileHandler.formatter",
			"tigase.util.LogFormatter");
		defaults.put(LOGGING_KEY + "java.util.logging.FileHandler.limit", "100000");
		defaults.put(LOGGING_KEY + "java.util.logging.FileHandler.pattern",
			"logs/tigase.log");
		defaults.put(LOGGING_KEY + "tigase.useParentHandlers", "true");
		if (params.get("--debug") != null) {
			defaults.put(LOGGING_KEY + ".level", "FINE");
			defaults.put(LOGGING_KEY + "tigase."+params.get("--debug")+".level", "ALL");
			defaults.put(LOGGING_KEY + "java.util.logging.FileHandler.level", "ALL");
			defaults.put(LOGGING_KEY + "java.util.logging.ConsoleHandler.level", "ALL");
		}
		defaults.put("demo-mode", demoMode);
		return defaults;
	}

  /**
   * Sets all configuration properties for object.
   */
	public void setProperties(final Map<String, Object> properties) {
		setupLogManager(properties);
		demoMode = (Boolean)properties.get("demo-mode");
	}

	private void setupLogManager(Map<String, Object> properties) {
		Set<Map.Entry<String, Object>> entries = properties.entrySet();
		StringBuilder buff = new StringBuilder();
		for (Map.Entry<String, Object> entry : entries) {
			if (entry.getKey().startsWith(LOGGING_KEY)) {
				String key = entry.getKey().substring(LOGGING_KEY.length());
				buff.append(key + "=" +	entry.getValue() + "\n");
				if (key.equals("java.util.logging.FileHandler.pattern")) {
					File log_path = new File(entry.getValue().toString()).getParentFile();
					if(!log_path.exists()) { log_path.mkdirs(); }
				} // end of if (key.equals())
			} // end of if (entry.getKey().startsWith(LOGGING_KEY))
		}
		loadLogManagerConfig(buff.toString());
		log.warning("DONE");
	}

	public static void loadLogManagerConfig(String config) {
    try {
      final ByteArrayInputStream bis =
        new ByteArrayInputStream(config.getBytes());
      LogManager.getLogManager().readConfiguration(bis);
      bis.close();
    } catch (IOException e) {
      log.log(Level.SEVERE, "Can not configure logManager", e);
    } // end of try-catch
	}

	public Map<String, Object> getProperties(String nodeId) {
		return repository.getProperties(nodeId);
	}

	public String[] getComponents() {
		return repository.getSubnodes();
	}

	public Map<String, Object> getAllProperties(String key) {
		Map<String, Object> result = new HashMap<String, Object>();
		String[] comps = getComponents();
		if (comps != null) {
			for (String comp: comps) {
				Map<String, Object> prop = getProperties(comp);
				for (Map.Entry<String, Object> entry: prop.entrySet()) {
					String entry_key = comp + "/" + entry.getKey();
					if (key == null) {
						result.put(entry_key, entry.getValue());
					} else {
						if (entry_key.startsWith(key)) {
							result.put(entry_key, entry.getValue());
						} // end of if (entry_key.startsWith(key))
					} // end of if (key == null) else
				} // end of for (Map.Entry entry: prop.entrySet())
			} // end of for (String comp: comps)
		}
		return result;
	}

	private boolean parseBoolean(String val) {
		return val.equalsIgnoreCase("true")
			|| val.equalsIgnoreCase("yes")
			|| val.equalsIgnoreCase("on")
			;
	}

	public Object setValue(String node_key, String value,
		boolean add, boolean feedback) throws Exception {
		int root_idx = node_key.indexOf('/');
		String root = node_key.substring(0, root_idx);
		int key_idx = node_key.lastIndexOf('/');
		String key = node_key.substring(key_idx+1);
		String subnode = null;
		if (root_idx != key_idx) {
			subnode = node_key.substring(root_idx+1, key_idx);
		}
		Object old_val = repository.get(root, subnode, key, null);
		if (old_val != null) {
			Object new_val = null;
			DataType type = DataType.valueof(old_val.getClass().getSimpleName());
			switch (type) {
			case INTEGER:
				new_val = Integer.decode(value);
				break;
			case INTEGER_ARR:
				if (add) {
					int old_len = ((int[])old_val).length;
					new_val = Arrays.copyOf((int[])old_val, old_len + 1);
					((int[])new_val)[old_len] = Integer.decode(value);
				} else {
					String[] spl = value.split(",");
					new_val = new int[spl.length];
					for (int i = 0; i < spl.length; i++) {
						((int[])new_val)[i] = Integer.decode(spl[i].trim());
					}
				} // end of if (add) else
				break;
			case STRING:
				new_val = value;
				break;
			case STRING_ARR:
				if (add) {
					int old_len = ((String[])old_val).length;
					new_val = Arrays.copyOf((String[])old_val, old_len + 1);
					((String[])new_val)[old_len] = value;
				} else {
					String[] spl = value.split(",");
					new_val = new String[spl.length];
					for (int i = 0; i < spl.length; i++) {
						((String[])new_val)[i] = spl[i].trim();
					}
				} // end of if (add) else
				break;
			case DOUBLE:
				new_val = new Double(Double.parseDouble(value));
				break;
			case DOUBLE_ARR:
				if (add) {
					int old_len = ((double[])old_val).length;
					new_val = Arrays.copyOf((double[])old_val, old_len + 1);
					((double[])new_val)[old_len] = Double.parseDouble(value);
				} else {
					String[] spl = value.split(",");
					new_val = new double[spl.length];
					for (int i = 0; i < spl.length; i++) {
						((double[])new_val)[i] = Double.parseDouble(spl[i].trim());
					}
				}
				break;
			case BOOLEAN:
				new_val = new Boolean(parseBoolean(value));
				break;
			case BOOLEAN_ARR:
				if (add) {
					int old_len = ((boolean[])old_val).length;
					new_val = Arrays.copyOf((boolean[])old_val, old_len + 1);
					((boolean[])new_val)[old_len] = parseBoolean(value);
				} else {
					String[] spl = value.split(",");
					new_val = new boolean[spl.length];
					for (int i = 0; i < spl.length; i++) {
						((boolean[])new_val)[i] = parseBoolean(spl[i].trim());
					}
				}
				break;
			default:
				break;
			} // end of switch (type)
			repository.set(root, subnode, key, new_val);
			repository.sync();
			return new_val;
		} else {
			if (force) {
				repository.set(root, subnode, key, value);
				repository.sync();
				if (feedback) {
					System.out.println("Forced to set new key=value: " + key + "=" + value);
				}
				return value;
			} else {
				if (feedback) {
					System.out.println("Error, given key does not exist in config yet.");
					System.out.println("You can only modify existing values, you can add new.");
					System.out.println("Use '-f' switch to force creation of the new property.");
				}
				return null;
			} // end of if (force) else
		} // end of else
	}

	private static String help() {
		return "\n"
			+ "Parameters:\n"
      + " -h             this help message\n"
			+ " -c file        configuration file\n"
			+ " -key key       node/key for the value to set\n"
			+ " -value value   value to set in configuration file\n"
			+ " -set           set given value for given key\n"
			+ " -add           add given value to the values list for given key\n"
			+ " -print         print content of all configuration settings or of given node/key\n"
			+ " -f             force creation of the new property - dangerous option...\n"
			+ "Samples:\n"
			+ " Setting admin account - overwriting any previous value(s)\n"
			+ " $ ./scripts/config.sh -c tigase-config.xml -print -set -key session_1/admins -value admin1@localhost\n"
			+ " Adding next admin account leaving old value(s)\n"
			+ " $ ./scripts/config.sh -c tigase-config.xml -print -add -key session_1/admins -value admin2@localhost\n"
			+ "\n"
			+ "Note: adding -print option is useful always, even with -set or -add\n"
			+ "      option as it prints set value afterwards.\n"
			;
	}

	private static String objectToString(Object value) {
		String val_str = null;
		DataType type = DataType.valueof(value.getClass().getSimpleName());
		try {
			switch (type) {
			case STRING_ARR:
				for (String s: (String[])value) {
					if (val_str == null) {
						val_str = s;
					} else {
						val_str = val_str + ", " + s;
					} // end of else
				} // end of for (String s: (String[])value)
				break;
			case INTEGER_ARR:
				for (Integer s: (int[])value) {
					if (val_str == null) {
						val_str = s.toString();
					} else {
						val_str = val_str + ", " + s.toString();
					} // end of else
				} // end of for (String s: (String[])value)
				break;
			case DOUBLE_ARR:
				for (Double s: (double[])value) {
					if (val_str == null) {
						val_str = s.toString();
					} else {
						val_str = val_str + ", " + s.toString();
					} // end of else
				} // end of for (String s: (String[])value)
				break;
			case BOOLEAN_ARR:
				for (Boolean s: (boolean[])value) {
					if (val_str == null) {
						val_str = s.toString();
					} else {
						val_str = val_str + ", " + s.toString();
					} // end of else
				} // end of for (String s: (String[])value)
				break;
			default:
				val_str = value.toString();
				break;
			} // end of switch (type)
		} catch (ClassCastException e) {
			log.warning("ERROR! Problem with type casting for property: " + key);
		} // end of try-catch
		return val_str;
	}

	private static void print(String key, Object value) {
		System.out.println(key + " = " + objectToString(value));
	}

	private static String config_file = null;
	private static String key = null;
	private static String value = null;
	private static boolean set = false;
	private static boolean add = false;
	private static boolean print = false;
	private static boolean force = true;

	/**
	 * Describe <code>main</code> method here.
	 *
	 * @param args a <code>String[]</code> value
	 */
	public static void main(final String[] args) throws Exception {

		force = false;

		if (args != null && args.length > 0) {
      for (int i = 0; i < args.length; i++) {
        if (args[i].equals("-h")) {
          System.out.print(help());
          System.exit(0);
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-c")) {
					config_file = args[++i];
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-key")) {
					key = args[++i];
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-value")) {
					value = args[++i];
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-set")) {
					set = true;
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-add")) {
					add = true;
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-print")) {
					print = true;
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-f")) {
					force = true;
        } // end of if (args[i].equals("-h"))
      } // end of for (int i = 0; i < args.length; i++)
		}

		Configurator conf = new Configurator(config_file, args);

		if (set || add) {
			conf.setValue(key, value, add, true);
		} // end of if (set)

		if (print) {
			Map<String, Object> allprop = conf.getAllProperties(key);
			for (Map.Entry<String, Object> entry: allprop.entrySet()) {
				print(entry.getKey(), entry.getValue());
			} // end of for (Map.Entry entry: prop.entrySet())
		} // end of if (print)
	}

	public void processPacket(final Packet packet, final Queue<Packet> results) {

		if (!packet.isCommand()) {
			return;
		}

		if (!packet.getTo().startsWith(getName()+".")) return;

		String msg = "Please be careful, you are service admin and all changes"
			+ " you make are instantly applied to live system!";
		boolean admin = true;
		if (packet.getPermissions() != Permissions.ADMIN) {
			if (demoMode) {
				admin = false;
				msg = "You are not admin. You can safely play with the settings as"
					+ " you can not change anything.";
				if (packet.getStrCommand() != null
					&& packet.getStrCommand().endsWith("session_1")) {
					Packet result = packet.commandResult("result");
					Command.addFieldValue(result, "Note",	msg, "fixed");
					Command.addFieldValue(result, "Note",
						"Restricted area, only admin can see these settings.", "fixed");
					results.offer(result);
					return;
				}
			} else {
				results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
						"You are not authorized for this action.", true));
				return;
			}
		}

		log.finest("Command received: " + packet.getStringData());

		String action = Command.getAction(packet);
		if (action != null && action.equals("cancel")) {
			Packet result = packet.commandResult(null);
			results.offer(result);
			return;
		}

		switch (packet.getCommand()) {
		case OTHER:
			if (packet.getStrCommand() != null) {
				if (packet.getStrCommand().startsWith("config/list/")) {
					String[] spl = packet.getStrCommand().split("/");
					Packet result = packet.commandResult("result");
					Command.addFieldValue(result, "Note",	msg, "fixed");
					Map<String, Object> allprop = getAllProperties(spl[2]);
					for (Map.Entry<String, Object> entry: allprop.entrySet()) {
						Command.addFieldValue(result, XMLUtils.escape(entry.getKey()),
							XMLUtils.escape(objectToString(entry.getValue())));
					} // end of for (Map.Entry entry: prop.entrySet())
					results.offer(result);
				}
				if (packet.getStrCommand().startsWith("config/set/")) {
					String[] spl = packet.getStrCommand().split("/");
					Packet result = packet.commandResult("result");
					Command.addFieldValue(result, "Note",	msg, "fixed");
					if (Command.getData(packet) == null) {
						Command.setStatus(result, "executing");
						Command.addAction(result, "complete");
						Map<String, Object> allprop = getAllProperties(spl[2]);
						for (Map.Entry<String, Object> entry: allprop.entrySet()) {
							Command.addFieldValue(result, XMLUtils.escape(entry.getKey()),
								XMLUtils.escape(objectToString(entry.getValue())));
						} // end of for (Map.Entry entry: prop.entrySet())
						Command.addFieldValue(result, XMLUtils.escape("new-prop-name"),
							XMLUtils.escape(spl[2] + "/"), "text-single", "New property name");
						Command.addFieldValue(result, XMLUtils.escape("new-prop-value"),
							"", "text-single", "New property value");
						results.offer(result);
					} else {
						Command.addNote(result, "You changed following settings:");
						Command.addFieldValue(result, "Note",
								"You changed following settings:", "fixed");
						Map<String, Object> allprop = getAllProperties(spl[2]);
						boolean changed = false;
						for (Map.Entry<String, Object> entry: allprop.entrySet()) {
							String tmp_val = Command.getFieldValue(packet,
								XMLUtils.escape(entry.getKey()));
							String old_val = objectToString(entry.getValue());
							String new_val = old_val;
							if (tmp_val != null) {
								new_val = XMLUtils.unescape(tmp_val);
							}
							if (new_val != null && old_val != null
								&& !new_val.equals(old_val)) {
								defConfigParams.put(entry.getKey(),
									setPropertyValue(entry.getKey(), new_val, result, admin));
								changed = true;
							}
						} // end of for (Map.Entry entry: prop.entrySet())
						String prop_value = Command.getFieldValue(packet, "new-prop-value");
						if (prop_value != null &&	prop_value.trim().length() > 0) {
							setPropertyValue(
								XMLUtils.unescape(Command.getFieldValue(packet, "new-prop-name")),
								XMLUtils.unescape(prop_value), result, admin);
							changed = true;
						}
						if (changed && admin) {
							setup(spl[2]);
						}
						results.offer(result);
					}
				}
			}
			break;
		default:
			break;
		}
	}

	public Object setPropertyValue(String key, String val, Packet result_pack,
		boolean admin) {
		Object result = null;
		try {
			if (admin) {
				result = setValue(key, val, false, false);
			}
			if (result != null) {
				Command.addFieldValue(result_pack, XMLUtils.escape(key),
					XMLUtils.escape(val));
			} else {
				Command.addFieldValue(result_pack, "Note",
					"You can not set new properties yet, you can just modify existing ones.",
					"fixed");
			}
		} catch (Exception e) {
			Command.addFieldValue(result_pack, "Note",
				"Error setting property: " + e, "fixed");
		}
		return result;
	}

	public Element getDiscoInfo(String node, String jid) {
		if (jid != null && jid.startsWith(getName()+".")) {
			return serviceEntity.getDiscoInfo(node);
		}
		return null;
	}

	public List<Element> getDiscoItems(String node, String jid) {
		if (jid.startsWith(getName()+".")) {
			return serviceEntity.getDiscoItems(node, jid);
		} else {
			return Arrays.asList(serviceEntity.getDiscoItem(null, getName() + "." + jid));
		}
	}

}
