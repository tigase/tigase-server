/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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

package tigase.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Bindings;
import javax.script.ScriptEngineManager;
import tigase.conf.Configurable;
import tigase.disco.ServiceEntity;
import tigase.disco.ServiceIdentity;
import tigase.disco.XMPPService;
import tigase.server.script.AddScriptCommand;
import tigase.server.script.CommandIfc;
import tigase.server.script.RemoveScriptCommand;
import tigase.util.DNSResolver;
import tigase.util.JIDUtils;
import tigase.xml.Element;
import tigase.xmpp.Authorization;

/**
 * Created: Oct 17, 2009 7:49:05 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class BasicComponent implements Configurable, XMPPService {

	/**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger(BasicComponent.class.getName());

	public static final String SCRIPT_DESCRIPTION = "AS:Description:";
	public static final String SCRIPT_ID = "AS:CommandId:";
	public static final String SCRIPT_COMPONENT = "AS:Component:";
	public static final String SCRIPTS_DIR_PROP_KEY = "scripts-dir";
	public static final String SCRIPTS_DIR_PROP_DEF = "scripts/admin";

	private String DEF_HOSTNAME_PROP_VAL = DNSResolver.getDefaultHostname();
	private String name = null;
	private String compId = null;
	private String defHostname = DEF_HOSTNAME_PROP_VAL;
	private Map<String, CommandIfc> scriptCommands =
			new ConcurrentSkipListMap<String, CommandIfc>();
	protected Set<String> admins = new CopyOnWriteArraySet<String>();
	private ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
	private ServiceEntity serviceEntity = null;

	@Override
	public void setName(String name) {
    this.name = name;
		compId = JIDUtils.getNodeID(name, defHostname);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getComponentId() {
		return compId;
	}

	@Override
	public void initializationCompleted() {	}

	@Override
	public void setProperties(Map<String, Object> props) {
		compId = (String)props.get(COMPONENT_ID_PROP_KEY);
		defHostname = (String)props.get(DEF_HOSTNAME_PROP_KEY);
		String[] admins_tmp = (String[])props.get(ADMINS_PROP_KEY);
		if (admins_tmp != null) {
			for (String admin : admins_tmp) {
				admins.add(admin);
			}
		}
		serviceEntity = new ServiceEntity(name, null, getDiscoDescription(), true);
		serviceEntity.addIdentities(
			new ServiceIdentity("component", getDiscoCategory(), getDiscoDescription()));
		serviceEntity.addFeatures("http://jabber.org/protocol/commands");
		CommandIfc command = new AddScriptCommand();
		command.init(CommandIfc.ADD_SCRIPT_CMD, "New command script");
		scriptCommands.put(command.getCommandId(), command);
		command = new RemoveScriptCommand();
		command.init(CommandIfc.DEL_SCRIPT_CMD, "Remove command script");
		scriptCommands.put(command.getCommandId(), command);
		String scripts_dir = (String)props.get(SCRIPTS_DIR_PROP_KEY);
		loadScripts(scripts_dir);
	}

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
    Map<String, Object> defs = new LinkedHashMap<String, Object>();
		defs.put(COMPONENT_ID_PROP_KEY, compId);
		DEF_HOSTNAME_PROP_VAL = DNSResolver.getDefaultHostname();
		defs.put(DEF_HOSTNAME_PROP_KEY, DEF_HOSTNAME_PROP_VAL);
		String[] adm =  null;
		if (params.get(GEN_ADMINS) != null) {
			adm = ((String)params.get(GEN_ADMINS)).split(",");
		} else {
			adm = new String[] {"admin@localhost"};
		}
		defs.put(ADMINS_PROP_KEY, adm);
		String scripts_dir = (String)params.get(GEN_SCRIPT_DIR);
		if (scripts_dir == null) {
			scripts_dir = SCRIPTS_DIR_PROP_DEF;
		}
		defs.put(SCRIPTS_DIR_PROP_KEY, scripts_dir);
		return defs;
	}

	public boolean isAdmin(String jid) {
		return admins.contains(JIDUtils.getNodeID(jid));
	}

	@Override
	public void release() {	}

	@Override
	public void processPacket(Packet packet, Queue<Packet> results) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void initBindings(Bindings binds) {
		binds.put(CommandIfc.SCRI_MANA, scriptEngineManager);
		binds.put(CommandIfc.ADMN_CMDS, scriptCommands);
		binds.put(CommandIfc.ADMN_DISC, serviceEntity);
	}

	protected boolean processScriptCommand(Packet pc, Queue<Packet> results) {
		if (pc.getElemFrom() == null) {
			// The packet has not gone through session manager yet
			return false;
		}
		Command.Action action = Command.getAction(pc);
		if (action == Command.Action.cancel) {
			Packet result = pc.commandResult(Command.DataType.result);
			Command.addTextField(result, "Note", "Command canceled.");
			results.offer(result);
			return true;
		}

		String strCommand = pc.getStrCommand();
		CommandIfc com = scriptCommands.get(strCommand);
		if (strCommand != null && com != null) {
			boolean admin = false;
			try {
				admin = isAdmin(pc.getElemFrom());
				if (admin) {
					if (log.isLoggable(Level.FINER)) {
						log.finer("Processing admin command: " + pc.toString());
					}
//					Bindings binds = com.getBindings();
//					if (binds == null) {
//						binds = scriptEngineManager.getBindings();
//					}
					Bindings binds = scriptEngineManager.getBindings();
					initBindings(binds);
					com.runCommand(pc, binds, results);
				} else {
					if (log.isLoggable(Level.FINER)) {
						log.finer("Command rejected non-admin detected: " + pc.getElemFrom());
					}
					results.offer(Authorization.FORBIDDEN.getResponseMessage(pc,
							"Only Administrator can call the command.", true));
				}
			} catch (Exception e) {
				log.log(Level.WARNING,
						"Unknown admin command processing exception: " +
						pc.toString(), e);
			}
			return true;
		}
		return false;
	}

	private void loadScripts(String scripts_dir) {
		File file = null;
		AddScriptCommand addCommand = new AddScriptCommand();
		Bindings binds = scriptEngineManager.getBindings();
		initBindings(binds);
		String[] dirs = new String[] {scripts_dir, scripts_dir + "/" + getName()};
		for (String scriptsPath : dirs) {
			try {
				File adminDir = new File(scriptsPath);
				if (adminDir != null && adminDir.exists()) {
					for (File f : adminDir.listFiles()) {
						// Just regular files here....
						if (f.isFile()) {
							String cmdId = null;
							String cmdDescr = null;
							String comp = null;
							file = f;
							StringBuilder sb = new StringBuilder();
							BufferedReader buffr = new BufferedReader(new FileReader(file));
							String line = null;
							while ((line = buffr.readLine()) != null) {
								sb.append(line + "\n");
								int idx = line.indexOf(SCRIPT_DESCRIPTION);
								if (idx >= 0) {
									cmdDescr = line.substring(idx + SCRIPT_DESCRIPTION.length()).
											trim();
								}
								idx = line.indexOf(SCRIPT_ID);
								if (idx >= 0) {
									cmdId = line.substring(idx + SCRIPT_ID.length()).trim();
								}
								idx = line.indexOf(SCRIPT_COMPONENT);
								if (idx >= 0) {
									comp = line.substring(idx + SCRIPT_COMPONENT.length()).trim();
								}
							}
							buffr.close();
							if (cmdId == null || cmdDescr == null || comp == null) {
								log.warning("Admin script found but it has no command ID or command description: " +
										file);
								continue;
							}
							// What components should load the script....
							String[] comp_names = comp.split(",");
							boolean found = false;
							for (String cmp : comp_names) {
								found = getName().equals(cmp);
								if (found) {
									break;
								}
							}
							if (!found) {
								log.info("Admin script for a different component: " + comp);
								continue;
							}
							int idx = file.toString().lastIndexOf(".");
							String ext = file.toString().substring(idx + 1);
							addCommand.addAdminScript(cmdId, cmdDescr, sb.toString(), null,
									ext, binds);
							log.config("Loaded admin command from file: " + file +
									", id: " + cmdId + ", ext: " + ext + ", descr: " + cmdDescr);
						}
					}
				} else {
					log.warning("Admin scripts directory is missing: " + adminDir +
							", creating...");
					adminDir.mkdirs();
				}
			} catch (Exception e) {
				log.log(Level.WARNING, "Can't load the admin script file: " + file, e);
			}
		}
	}

	/**
	 * Exists for backward compatibility with the old API.
	 * @return
	 */
	@Deprecated
	public List<Element> getDiscoFeatures() {
		return null;
	}

	@Override
	public List<Element> getDiscoFeatures(String from) {
		return getDiscoFeatures();
	}

	/**
	 * Exists for backward compatibility with the old API.
	 */
	@Deprecated
	public Element getDiscoInfo(String node, String jid) {
		return null;
	}

	@Override
	public Element getDiscoInfo(String node, String jid, String from) {
		// This is only to support the old depreciated API.
		Element result = getDiscoInfo(node, jid);
		if (result != null) {
			return result;
		}
		// OLD API support end
		if (getName().equals(JIDUtils.getNodeNick(jid))) {
			return serviceEntity.getDiscoInfo(node, isAdmin(from));
		}
		return null;
	}

	/**
	 * Exists for backward compatibility with the old API.
	 *
	 * @deprecated
	 */
	@Deprecated
	public List<Element> getDiscoItems(String node, String jid) {
		return null;
	}

	@Override
	public List<Element> getDiscoItems(String node, String jid, String from) {
		// This is only to support the old depreciated API.
		List<Element> result = getDiscoItems(node, jid);
		if (result != null) {
			return result;
		}
		// OLD API support end
		if (getName().equals(JIDUtils.getNodeNick(jid))) {
			if (node != null) {
//				result = serviceEntity.getDiscoItems(null, null, isAdmin(from));
//			} else {
				if (node.equals("http://jabber.org/protocol/commands") && isAdmin(from)) {
					result = new LinkedList<Element>();
					for (CommandIfc comm : scriptCommands.values()) {
						result.add(new Element("item",
								new String[]{"node", "name", "jid"},
								new String[]{comm.getCommandId(), comm.getDescription(),
									getComponentId()}));
					}
				}
			} else {
				result = serviceEntity.getDiscoItems(null, null, isAdmin(from));
				if (result != null) {
					for (Iterator<Element> it = result.iterator(); it.hasNext();) {
						Element element = it.next();
						if (element.getAttribute("node") == null) {
							it.remove();
						}
					}
				}
			}
			//Element result = serviceEntity.getDiscoItem(null, getName() + "." + jid);
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Found disco items: " + (result != null ? result.toString()
						: null));
			}
			return result;
		} else {
			Element res = null;
			if (!serviceEntity.isAdminOnly() || isAdmin(from)) {
				res = serviceEntity.getDiscoItem(null, JIDUtils.getNodeID(getName(), jid));
			}
			result = serviceEntity.getDiscoItems(null, null, isAdmin(from));
			if (res != null) {
				if (result != null) {
					for (Iterator<Element> it = result.iterator(); it.hasNext();) {
						Element element = it.next();
						if (element.getAttribute("node") != null) {
							it.remove();
						}
					}
					result.add(0, res);
				} else {
					result = Arrays.asList(res);
				}
			}
			return result;
		}
	}

	public String getDiscoDescription() {
		return "Undefined description";
	}

	public String getDiscoCategory() {
		return "generic";
	}

	public String getDefHostName() {
		return defHostname;
	}

	public void updateServiceDiscoveryItem(String jid, String node, String name,
			boolean admin) {
		ServiceEntity item = new ServiceEntity(jid, node, name, admin);
		//item.addIdentities(new ServiceIdentity("component", identity_type, name));
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Modifying service-discovery info: " + item.toString());
		}
		serviceEntity.addItems(item);
//		if (!admin) {
//			serviceEntity.setAdminOnly(admin);
//		}
	}

	public void removeServiceDiscoveryItem(String jid, String node, String name) {
		ServiceEntity item = new ServiceEntity(jid, node, name);
		//item.addIdentities(new ServiceIdentity("component", identity_type, name));
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Modifying service-discovery info, removing: " + item.toString());
		}
		serviceEntity.removeItems(item);
	}

}
