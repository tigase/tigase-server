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
import tigase.disco.ServiceEntity;
import tigase.disco.ServiceIdentity;
import tigase.disco.XMPPService;
import tigase.server.AbstractComponentRegistrator;
import tigase.server.Command;
import tigase.server.MessageReceiver;
import tigase.server.MessageRouter;
import tigase.server.ConnectionManager;
import tigase.server.MessageRouterConfig;
import tigase.server.Packet;
import tigase.server.Permissions;
import tigase.server.ServerComponent;
import tigase.util.ClassUtil;
import tigase.xml.Element;
import tigase.xml.XMLUtils;
import tigase.xml.db.Types.DataType;
import tigase.xmpp.Authorization;

import static tigase.server.MessageRouterConfig.DEF_SM_NAME;

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
	/**
	 * This variable is used only for configuration at run-time to add new
	 * components....
	 */
	private String routerCompName = null;

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
		ServiceEntity item = new ServiceEntity(getName(), "--none--",
			"Add new component...");
		item.addFeatures(CMD_FEATURES);
		item.addIdentities(new ServiceIdentity[] {
				new ServiceIdentity("automation", "command-node",
					"Add new component...")});
		config_set.addItems(new ServiceEntity[] {item});
		serviceEntity.addItems(new ServiceEntity[] {config_list, config_set});
	}

	public void parseArgs(final String[] args) {
		defConfigParams.put(GEN_TEST, new Boolean(false));
		defConfigParams.put("config-type", GEN_CONFIG_DEF);
    if (args != null && args.length > 0) {
      for (int i = 0; i < args.length; i++) {
				String key = null;
				Object val = null;
				if (args[i].startsWith(GEN_CONFIG)) {
					key = "config-type";	val = args[i];
				}
				if (args[i].startsWith(GEN_TEST)) {
					key = args[i];  val = new Boolean(true);
				}
				if (args[i].equals(GEN_USER_DB) || args[i].equals(GEN_USER_DB_URI)
					|| args[i].equals(GEN_AUTH_DB) || args[i].equals(GEN_AUTH_DB_URI)
					|| args[i].startsWith(GEN_EXT_COMP) || args[i].equals(GEN_VIRT_HOSTS)
					|| args[i].equals(GEN_ADMINS) || args[i].equals(GEN_DEBUG)
					|| (args[i].startsWith(GEN_CONF) && !args[i].startsWith(GEN_CONFIG))) {
					key = args[i];  val = args[++i];
				}
				if (key != null) {
					defConfigParams.put(key, val);
					log.config("Setting defaults: " + key + "=" + val.toString());
				} // end of if (key != null)
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
		if (component.getClass().getName().equals(ROUTER_COMP_CLASS_NAME)) {
			routerCompName = component.getName();
		} // end of if (component.getClass().getName().equals())
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

	/**
   * Returns defualt configuration settings in case if there is no
   * config file.
   */
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defaults = new TreeMap<String, Object>();
		defaults.put(LOGGING_KEY + "java.util.logging.ConsoleHandler.level", "WARNING");
		if ((Boolean)params.get(GEN_TEST)) {
			defaults.put(LOGGING_KEY + ".level", "WARNING");
			defaults.put(LOGGING_KEY + "java.util.logging.FileHandler.level", "INFO");
		} else {
			defaults.put(LOGGING_KEY + ".level", "INFO");
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
		defaults.put(LOGGING_KEY + "java.util.logging.FileHandler.limit", "1000000");
		defaults.put(LOGGING_KEY + "java.util.logging.FileHandler.pattern",
			"logs/tigase.log");
		defaults.put(LOGGING_KEY + "tigase.useParentHandlers", "true");
		if (params.get(GEN_DEBUG) != null) {
			defaults.put(LOGGING_KEY + ".level", "INFO");
			defaults.put(LOGGING_KEY + "tigase."+params.get(GEN_DEBUG)+".level", "ALL");
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
		boolean add, boolean feedback, Map<String, Object> orig) throws Exception {
		int root_idx = node_key.indexOf('/');
		String root = root_idx > 0 ? node_key.substring(0, root_idx) : "";
		int key_idx = node_key.lastIndexOf('/');
		String key = key_idx > 0 ? node_key.substring(key_idx+1) : node_key;
		String subnode = null;
		if (root_idx != key_idx) {
			subnode = node_key.substring(root_idx+1, key_idx);
		}
		Object old_val = null;
		if (orig == null) {
			old_val = repository.get(root, subnode, key, null);
		} else {
			old_val = orig.get(node_key);
		} // end of if (orig == null) else
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
			case LONG:
				new_val = Long.decode(value);
				break;
			case LONG_ARR:
				if (add) {
					int old_len = ((long[])old_val).length;
					new_val = Arrays.copyOf((long[])old_val, old_len + 1);
					((long[])new_val)[old_len] = Long.decode(value);
				} else {
					String[] spl = value.split(",");
					new_val = new long[spl.length];
					for (int i = 0; i < spl.length; i++) {
						((long[])new_val)[i] = Long.decode(spl[i].trim());
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
				new_val = value;
				break;
			} // end of switch (type)
			if (orig == null) {
				repository.set(root, subnode, key, new_val);
				repository.sync();
			} else {
				orig.put(node_key, new_val);
			} // end of if (orig == null) else
			return new_val;
		} else {
			if (force) {
				if (orig == null) {
					repository.set(root, subnode, key, value);
					repository.sync();
				} else {
					orig.put(node_key, value);
				} // end of if (orig == null) else
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
			+ " $ ./scripts/config.sh -c tigase-config.xml -print -set -key "
			+ DEF_SM_NAME + "/admins -value admin1@localhost\n"
			+ " Adding next admin account leaving old value(s)\n"
			+ " $ ./scripts/config.sh -c tigase-config.xml -print -add -key "
			+ DEF_SM_NAME + "/admins -value admin2@localhost\n"
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
			case LONG_ARR:
				for (Long s: (long[])value) {
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
			conf.setValue(key, value, add, true, null);
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
					&& packet.getStrCommand().endsWith(DEF_SM_NAME)) {
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
						prepareConfigData(result, spl[2]);
						results.offer(result);
					} else {
						updateConfigChanges(packet, result, spl[2], admin);
						results.offer(result);
					}
				}
			}
			break;
		default:
			break;
		}
	}

	private void newComponentCommand(Packet result) {
		Command.addFieldValue(result, "Info",	"Press:", "fixed");
		Command.addFieldValue(result, "Info",
			"'Next' to set all parameters for the new component.", "fixed");
		Command.addFieldValue(result, "Info",
			"'Finish' to create component with default parameters.", "fixed");
		Command.setStatus(result, "executing");
		Command.addAction(result, "next");
		Command.addFieldValue(result, "Component name",
			"", "text-single", "Component name");
		try {
			Set<Class<MessageReceiver>> receiv_cls =
				ClassUtil.getClassesImplementing(MessageReceiver.class);
			String[] receiv_cls_names = new String[receiv_cls.size()];
			String[] receiv_cls_simple = new String[receiv_cls.size()];
			int idx = 0;
			for (Class<MessageReceiver> reciv: receiv_cls) {
				receiv_cls_names[idx] = reciv.getName();
				receiv_cls_simple[idx++] = reciv.getSimpleName();
			} // end of for (MessageReceiver.class reciv: receiv_cls)
			Command.addFieldValue(result, "Component class", EXT_COMP_CLASS_NAME,
				receiv_cls_simple, receiv_cls_names);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Problem loading MessageReceiver implementations", e);
			Command.addFieldValue(result, "Component class",
				"ERROR!! Problem loading MessageReceiver implementations, "
				+ "look in log file for details...",
				"text-single", "Component class");
		} // end of try-catch
	}

	private boolean isValidCompName(String name) {
		return !(name.contains(" ")
			|| name.contains("\t")
			|| name.contains("@")
			|| name.contains("&"));
	}

	private boolean checkComponentName(Packet result, String name) {
		if (name == null || name.length() == 0) {
			Command.addFieldValue(result, "Info",
				"Note!! Missing component name, please provide valid component name.",
				"fixed");
			newComponentCommand(result);
			return false;
		} // end of if (new_comp_name == null || new_comp_name.length() == 0)
		if (!isValidCompName(name)) {
			Command.addFieldValue(result, "Info",
				"Note!! Incorrect component name, contains invalid characters.",
				"fixed");
			Command.addFieldValue(result, "Info",
				"Please provide correct component name.",	"fixed");
			newComponentCommand(result);
			return false;
		} // end of if (!isValidCompName(new_comp_name))
		String[] comp_names = getComponents();
		for (String comp_name: comp_names) {
			if (comp_name.equals(name)) {
				Command.addFieldValue(result, "Info",
					"Note!! Component with provided name already exist.",
					"fixed");
				Command.addFieldValue(result, "Info",
					"Please provide different component name.",	"fixed");
				newComponentCommand(result);
			return false;
			} // end of if (comp_name.equals(new_comp_name))
		} // end of for (String comp_name: comp_names)
		return true;
	}

	private void createNewComponent(Packet packet, Packet result, boolean admin) {
		String new_comp_name = Command.getFieldValue(packet, "Component name");
		String new_comp_class = Command.getFieldValue(packet, "Component class");
		try {
			MessageReceiver mr =
				(MessageReceiver)Class.forName(new_comp_class).newInstance();
			mr.setName(new_comp_name);
			if (mr instanceof Configurable) {
				Map<String, Object> comp_props =
					((Configurable)mr).getDefaults(defConfigParams);
				Map<String, Object> new_params = new HashMap<String, Object>(comp_props);
				// Convert String values to proper Objecy values
				for (Map.Entry<String, Object> entry: comp_props.entrySet()) {
					String val =
						Command.getFieldValue(packet, XMLUtils.escape(entry.getKey()));
					if (val == null) { val = ""; }
					val = XMLUtils.unescape(val);
					log.info("New component value: " + entry.getKey() + "=" + val);
					setValue(entry.getKey(), val, false, false, new_params);
				} // end of for (Map.Entry entry: prop.entrySet())
				if (admin) {
					// Now we can save all properties to config repository:
					for (Map.Entry<String, Object> entry: new_params.entrySet()) {
						String key = entry.getKey();
						String subnode = null;
						int key_idx = entry.getKey().lastIndexOf('/');
						if (key_idx > 0) {
							key = entry.getKey().substring(key_idx+1);
							subnode = entry.getKey().substring(0, key_idx);
						}
						log.info("Saving property to repository: "
							+ "root=" + new_comp_name
							+ ", subnode=" + subnode
							+ ", key=" + key
							+ ", value=" + entry.getValue());
						repository.set(new_comp_name, subnode, key, entry.getValue());
					} // end of for (Map.Entry entry: prop.entrySet())
					// And load the component itself.....
					// Set class name for the component
					repository.set(routerCompName, "/components/msg-receivers",
						new_comp_name + ".class", new_comp_class);
					// Activate the component
					repository.set(routerCompName, "/components/msg-receivers",
						new_comp_name + ".active", true);
					// Add to the list of automaticaly loaded components
					setValue(routerCompName + "/components/msg-receivers/id-names",
						new_comp_name, true, false, null);
					//repository.sync();
					setup(routerCompName);
				} // end of if (admin)
			}
			Command.addNote(result, "New component vreated: " + new_comp_name);
			Command.addFieldValue(result, "Note",
				"New component created: " + new_comp_name, "fixed");
		} catch (Exception e) {
			log.log(Level.SEVERE, "Problem instantiating component:", e);
			Command.addFieldValue(result, "Component class",
				"ERROR!! Problem instantiating component, "
				+ "look in log file for details...",
				"text-single", "Component class");
		} // end of try-catch
	}

	private void newComponentCommand(Packet packet, Packet result, boolean admin) {
		String params_set = Command.getFieldValue(packet, "Params set");
		if (params_set != null) {
			createNewComponent(packet, result, admin);
			return;
		} // end of if (params_set != null)
		String new_comp_name = Command.getFieldValue(packet, "Component name");
		String new_comp_class = Command.getFieldValue(packet, "Component class");
		if (!checkComponentName(result, new_comp_name)) {
			return;
		} // end of if (!checkComponentName(new_comp_name))
		Command.setStatus(result, "executing");
		Command.addAction(result, "prev");
		Command.addFieldValue(result, "Component name",	new_comp_name,	"hidden");
		Command.addFieldValue(result, "Component class", new_comp_class, "hidden");
		Command.addFieldValue(result, "Info1",	"Press:", "fixed");
		Command.addFieldValue(result, "Info2",
			"1. 'Finish' to create component with this parameters.", "fixed");
		Command.addFieldValue(result, "Info3",
			"2. 'Previous' to go back and select different component.", "fixed");
		try {
			MessageReceiver mr =
				(MessageReceiver)Class.forName(new_comp_class).newInstance();
			Command.addFieldValue(result, "Info4",
				"Component name: " + new_comp_name
				+ ", class: " + mr.getClass().getSimpleName(), "fixed");
			if (mr instanceof ConnectionManager) {
				String ports = Command.getFieldValue(packet, "TCP/IP ports");
				if (ports == null) {
					Command.addAction(result, "next");
					Command.addFieldValue(result, "Info4",
						"This component uses TCP/IP ports, please provide port numbers:",
						"fixed");
					Command.addFieldValue(result, "TCP/IP ports", "1234, 5678");
					return;
				} else {
					String[] ports_arr = ports.split(",");
					int[] ports_i = new int[ports_arr.length];
					try {
						for (int i = 0; i < ports_arr.length; i++) {
							ports_i[i] = Integer.decode(ports_arr[i].trim());
						} // end of for (int i = 0; i < ports_arr.length; i++)
						defConfigParams.put(new_comp_name + "/connections/ports", ports_i);
					} catch (Exception e) {
						Command.addFieldValue(result, "Info4",
							"Incorrect TCP/IP ports provided, please provide port numbers:",
							"fixed");
						Command.addFieldValue(result, "TCP/IP ports", ports);
						return;
					} // end of try-catch
				} // end of else
			}
			Command.addAction(result, "complete");
			mr.setName(new_comp_name);
			if (mr instanceof Configurable) {
				// Load defaults into sorted Map:
				Map<String, Object> comp_props =
					new TreeMap<String, Object>(((Configurable)mr).
						getDefaults(defConfigParams));
				for (Map.Entry<String, Object> entry: comp_props.entrySet()) {
					Command.addFieldValue(result, XMLUtils.escape(entry.getKey()),
						XMLUtils.escape(objectToString(entry.getValue())));
				} // end of for (Map.Entry entry: prop.entrySet())
			} else {
				Command.addFieldValue(result, "Info6",
					"Component is not configurable, do you want to create it?", "fixed");
			} // end of else
			Command.addFieldValue(result, "Params set", "true", "hidden");
		} catch (Exception e) {
			log.log(Level.SEVERE, "Problem instantiating component:", e);
			Command.addFieldValue(result, "Component class",
				"ERROR!! Problem instantiating component, "
				+ "look in log file for details...",
				"text-single", "Component class");
		} // end of try-catch
	}

	private void prepareConfigData(Packet result, String comp_name) {
		if (comp_name.equals("--none--")) {
			newComponentCommand(result);
			return;
		} // end of if (comp_name.equals("--none--"))
		Command.setStatus(result, "executing");
		Command.addAction(result, "complete");
		// Let's try to sort them to make it easier to find options on
		// configuration page.
		Map<String, Object> allprop =
			new TreeMap<String, Object>(getAllProperties(comp_name));
		for (Map.Entry<String, Object> entry: allprop.entrySet()) {
			Command.addFieldValue(result, XMLUtils.escape(entry.getKey()),
				XMLUtils.escape(objectToString(entry.getValue())));
		} // end of for (Map.Entry entry: prop.entrySet())
		Command.addFieldValue(result, XMLUtils.escape("new-prop-name"),
			XMLUtils.escape(comp_name + "/"), "text-single", "New property name");
		Command.addFieldValue(result, XMLUtils.escape("new-prop-value"),
			"", "text-single", "New property value");
	}

	private void updateConfigChanges(Packet packet, Packet result,
		String comp_name, boolean admin) {
		if (comp_name.equals("--none--")) {
			newComponentCommand(packet, result, admin);
			return;
		} // end of if (comp_name.equals("--none--"))
		Command.addNote(result, "You changed following settings:");
		Command.addFieldValue(result, "Note",
			"You changed following settings:", "fixed");
		Map<String, Object> allprop = getAllProperties(comp_name);
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
			setup(comp_name);
		}
	}

	public Object setPropertyValue(String key, String val, Packet result_pack,
		boolean admin) {
		Object result = null;
		try {
			if (admin) {
				result = setValue(key, val, false, false, null);
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

	public 	List<Element> getDiscoFeatures() { return null; }

	public List<Element> getDiscoItems(String node, String jid) {
		if (jid.startsWith(getName()+".")) {
			return serviceEntity.getDiscoItems(node, jid);
		} else {
			return Arrays.asList(serviceEntity.getDiscoItem(null, getName() + "." + jid));
		}
	}

}
