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

//~--- non-JDK imports --------------------------------------------------------

import tigase.conf.Configurable;

import tigase.disco.ServiceEntity;
import tigase.disco.ServiceIdentity;
import tigase.disco.XMPPService;

import tigase.server.script.AddScriptCommand;
import tigase.server.script.CommandIfc;
import tigase.server.script.RemoveScriptCommand;

import tigase.util.DNSResolver;
import tigase.util.TigaseStringprepException;

import tigase.vhosts.VHostItem;
import tigase.vhosts.VHostListener;
import tigase.vhosts.VHostManagerIfc;

import tigase.xml.Element;

import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

//~--- JDK imports ------------------------------------------------------------

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.Bindings;
import javax.script.ScriptEngineManager;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Oct 17, 2009 7:49:05 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class BasicComponent implements Configurable, XMPPService, VHostListener {

	/** Field description */

	// public static JID NULL_ROUTING = null;

	/** Field description */
	public static final String SCRIPTS_DIR_PROP_DEF = "scripts/admin";

	/** Field description */
	public static final String SCRIPTS_DIR_PROP_KEY = "scripts-dir";

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(BasicComponent.class.getName());

	//~--- fields ---------------------------------------------------------------

//static {
//  NULL_ROUTING = JID.jidInstanceNS("NULL");
//}
	private String DEF_HOSTNAME_PROP_VAL = DNSResolver.getDefaultHostname();
	private JID compId = null;
	private String name = null;
	private BareJID defHostname = BareJID.bareJIDInstanceNS(DEF_HOSTNAME_PROP_VAL);
	private Map<String, CommandIfc> scriptCommands = new ConcurrentHashMap<String, CommandIfc>();
	protected Set<BareJID> admins = new ConcurrentSkipListSet<BareJID>();
	private ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
	private String scriptsBaseDir = null;
	private String scriptsCompDir = null;
	private ServiceEntity serviceEntity = null;
	protected VHostManagerIfc vHostManager = null;

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 */
	public void addComponentDomain(String domain) {
		vHostManager.addComponentDomain(domain);
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public JID getComponentId() {
		return compId;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public BareJID getDefHostName() {
		return defHostname;
	}

	/**
	 * Method description
	 *
	 *
	 * @param params
	 *
	 * @return
	 */
	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defs = new LinkedHashMap<String, Object>();

		defs.put(COMPONENT_ID_PROP_KEY, compId.toString());
		DEF_HOSTNAME_PROP_VAL = DNSResolver.getDefaultHostname();
		defs.put(DEF_HOSTNAME_PROP_KEY, DEF_HOSTNAME_PROP_VAL);

		String[] adm = null;

		if (params.get(GEN_ADMINS) != null) {
			adm = ((String) params.get(GEN_ADMINS)).split(",");
		} else {
			adm = new String[] { "admin@localhost" };
		}

		defs.put(ADMINS_PROP_KEY, adm);

		String scripts_dir = (String) params.get(GEN_SCRIPT_DIR);

		if (scripts_dir == null) {
			scripts_dir = SCRIPTS_DIR_PROP_DEF;
		}

		defs.put(SCRIPTS_DIR_PROP_KEY, scripts_dir);

		return defs;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public String getDiscoCategoryType() {
		return "generic";
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public String getDiscoDescription() {
		return "Undefined description";
	}

	/**
	 * Exists for backward compatibility with the old API.
	 * @return
	 */
	@Deprecated
	public List<Element> getDiscoFeatures() {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param from
	 *
	 * @return
	 */
	@Override
	public List<Element> getDiscoFeatures(JID from) {
		return getDiscoFeatures();
	}

	/**
	 * Exists for backward compatibility with the old API.
	 *
	 * @param node
	 * @param jid
	 *
	 * @return
	 */
	@Deprecated
	public Element getDiscoInfo(String node, JID jid) {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param node
	 * @param jid
	 * @param from
	 *
	 * @return
	 */
	@Override
	public Element getDiscoInfo(String node, JID jid, JID from) {

		// This is only to support the old depreciated API.
		Element result = getDiscoInfo(node, jid);

		if (result != null) {
			return result;
		}

		// OLD API support end
		if (getName().equals(jid.getLocalpart())) {
			return serviceEntity.getDiscoInfo(node, isAdmin(from));
		}

		return null;
	}

	/**
	 * Exists for backward compatibility with the old API.
	 *
	 * @deprecated
	 *
	 * @param node
	 * @param jid
	 *
	 * @return
	 */
	@Deprecated
	public List<Element> getDiscoItems(String node, JID jid) {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param node
	 * @param jid
	 * @param from
	 *
	 * @return
	 */
	@Override
	public List<Element> getDiscoItems(String node, JID jid, JID from) {

		// This is only to support the old depreciated API.
		List<Element> result = getDiscoItems(node, jid);

		if (result != null) {
			return result;
		}

		// OLD API support end
		if (getName().equals(jid.getLocalpart())) {
			if (node != null) {

//      result = serviceEntity.getDiscoItems(null, null, isAdmin(from));
//      }else {
				if (node.equals("http://jabber.org/protocol/commands") && isAdmin(from)) {
					result = new LinkedList<Element>();

					for (CommandIfc comm : scriptCommands.values()) {
						result.add(new Element("item", new String[] { "node", "name", "jid" },
								new String[] { comm.getCommandId(),
								comm.getDescription(), jid.toString() }));
					}
				}
			} else {
				result = serviceEntity.getDiscoItems(null, jid.toString(), isAdmin(from));

				if (result != null) {
					for (Iterator<Element> it = result.iterator(); it.hasNext(); ) {
						Element element = it.next();

						if (element.getAttribute("node") == null) {
							it.remove();
						}
					}
				}
			}

			// Element result = serviceEntity.getDiscoItem(null, getName() + "." + jid);
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Found disco items: " + ((result != null) ? result.toString() : null));
			}

			return result;
		} else {
			Element res = null;

			if ( !serviceEntity.isAdminOnly() || isAdmin(from)) {
				res = serviceEntity.getDiscoItem(null, BareJID.toString(getName(), jid.toString()));
			}

			result = serviceEntity.getDiscoItems(null, null, isAdmin(from));

			if (res != null) {
				if (result != null) {
					for (Iterator<Element> it = result.iterator(); it.hasNext(); ) {
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

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 *
	 * @return
	 */
	public VHostItem getVHostItem(String domain) {
		return (vHostManager != null) ? vHostManager.getVHostItem(domain) : null;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public boolean handlesLocalDomains() {
		return false;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public boolean handlesNameSubdomains() {
		return true;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public boolean handlesNonLocalDomains() {
		return false;
	}

	/**
	 * Method description
	 *
	 *
	 * @param binds
	 */
	public void initBindings(Bindings binds) {
		binds.put(CommandIfc.SCRI_MANA, scriptEngineManager);
		binds.put(CommandIfc.ADMN_CMDS, scriptCommands);
		binds.put(CommandIfc.ADMN_DISC, serviceEntity);
		binds.put(CommandIfc.SCRIPT_BASE_DIR, scriptsBaseDir);
		binds.put(CommandIfc.SCRIPT_COMP_DIR, scriptsCompDir);
		binds.put(CommandIfc.COMPONENT_NAME, getName());
	}

	/**
	 * Method description
	 *
	 */
	@Override
	public void initializationCompleted() {}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 *
	 * @return
	 */
	public boolean isAdmin(JID jid) {
		return admins.contains(jid.getBareJID());
	}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 *
	 * @return
	 */
	public boolean isLocalDomain(String domain) {
		return (vHostManager != null) ? vHostManager.isLocalDomain(domain) : false;
	}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 *
	 * @return
	 */
	public boolean isLocalDomainOrComponent(String domain) {
		return (vHostManager != null) ? vHostManager.isLocalDomainOrComponent(domain) : false;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param results
	 */
	@Override
	public void processPacket(Packet packet, Queue<Packet> results) {
		if (packet.isCommand() && getName().equals(packet.getStanzaTo().getLocalpart())
				&& this.isLocalDomain(packet.getStanzaTo().getDomain())) {
			processScriptCommand(packet, results);
		}
	}

	/**
	 * Method description
	 *
	 */
	@Override
	public void release() {}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 */
	public void removeComponentDomain(String domain) {
		vHostManager.removeComponentDomain(domain);
	}

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 * @param node
	 * @param description
	 */
	public void removeServiceDiscoveryItem(String jid, String node, String description) {
		ServiceEntity item = new ServiceEntity(jid, node, description);

		// item.addIdentities(new ServiceIdentity("component", identity_type, name));
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Modifying service-discovery info, removing: " + item.toString());
		}

		serviceEntity.removeItems(item);
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param name
	 */
	@Override
	public void setName(String name) {
		this.name = name;

		try {
			compId = JID.jidInstance(name, defHostname.getDomain(), null);
		} catch (TigaseStringprepException ex) {
			log.log(Level.WARNING, "Problem setting component ID: ", ex);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param props
	 */
	@Override
	public void setProperties(Map<String, Object> props) {
		try {
			compId = JID.jidInstance((String) props.get(COMPONENT_ID_PROP_KEY));
		} catch (TigaseStringprepException ex) {
			log.log(Level.WARNING, "Problem setting component ID: ", ex);
		}

		defHostname = BareJID.bareJIDInstanceNS((String) props.get(DEF_HOSTNAME_PROP_KEY));

		String[] admins_tmp = (String[]) props.get(ADMINS_PROP_KEY);

		if (admins_tmp != null) {
			for (String admin : admins_tmp) {
				try {
					admins.add(BareJID.bareJIDInstance(admin));
				} catch (TigaseStringprepException ex) {
					log.log(Level.CONFIG, "Incorrect admin JID: ", ex);
				}
			}
		}

		serviceEntity = new ServiceEntity(name, null, getDiscoDescription(), true);
		serviceEntity.addIdentities(new ServiceIdentity("component", getDiscoCategoryType(),
				getDiscoDescription()));
		serviceEntity.addFeatures("http://jabber.org/protocol/commands");

		CommandIfc command = new AddScriptCommand();

		command.init(CommandIfc.ADD_SCRIPT_CMD, "New command script");
		scriptCommands.put(command.getCommandId(), command);
		command = new RemoveScriptCommand();
		command.init(CommandIfc.DEL_SCRIPT_CMD, "Remove command script");
		scriptCommands.put(command.getCommandId(), command);
		scriptsBaseDir = (String) props.get(SCRIPTS_DIR_PROP_KEY);
		scriptsCompDir = scriptsBaseDir + "/" + getName();
		loadScripts();
	}

	/**
	 * Method description
	 *
	 *
	 * @param manager
	 */
	@Override
	public void setVHostManager(VHostManagerIfc manager) {
		this.vHostManager = manager;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 * @param node
	 * @param description
	 * @param admin
	 */
	public void updateServiceDiscoveryItem(String jid, String node, String description,
			boolean admin) {
		updateServiceDiscoveryItem(jid, node, description, admin, (String[]) null);
	}

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 * @param node
	 * @param description
	 * @param admin
	 * @param features
	 */
	public void updateServiceDiscoveryItem(String jid, String node, String description,
			boolean admin, String... features) {
		updateServiceDiscoveryItem(jid, node, description, null, null, admin, features);
	}

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 * @param node
	 * @param description
	 * @param category
	 * @param type
	 * @param admin
	 * @param features
	 */
	public void updateServiceDiscoveryItem(String jid, String node, String description,
			String category, String type, boolean admin, String... features) {
		if (serviceEntity.getJID().equals(jid) && (serviceEntity.getNode() == node)) {
			serviceEntity.setDescription(description);

			if ((category != null) || (type != null)) {
				serviceEntity.addIdentities(new ServiceIdentity(category, type, description));
			}

			if (features != null) {
				serviceEntity.setFeatures("http://jabber.org/protocol/commands");
				serviceEntity.addFeatures(features);
			}

			if (log.isLoggable(Level.FINEST)) {
				log.finest("Modifying service-discovery info: " + serviceEntity.toString());
			}
		} else {
			ServiceEntity item = new ServiceEntity(jid, node, description, admin);

			if ((category != null) || (type != null)) {
				item.addIdentities(new ServiceIdentity(category, type, description));
			}

			if (features != null) {
				item.addFeatures(features);
			}

			if (log.isLoggable(Level.FINEST)) {
				log.finest("Adding new item: " + item.toString());
			}

			serviceEntity.addItems(item);
		}
	}

	protected boolean processScriptCommand(Packet pc, Queue<Packet> results) {
		if (pc.getStanzaFrom() == null) {

			// The packet has not gone through session manager yet
			return false;
		}

		Iq iqc = (Iq) pc;
		Command.Action action = Command.getAction(iqc);

		if (action == Command.Action.cancel) {
			Packet result = iqc.commandResult(Command.DataType.result);

			Command.addTextField(result, "Note", "Command canceled.");
			results.offer(result);

			return true;
		}

		String strCommand = iqc.getStrCommand();
		CommandIfc com = scriptCommands.get(strCommand);

		if ((strCommand != null) && (com != null)) {
			boolean admin = false;

			try {
				admin = isAdmin(iqc.getStanzaFrom());

				if (admin) {
					if (log.isLoggable(Level.FINER)) {
						log.finer("Processing admin command: " + pc.toString());
					}

					Bindings binds = com.getBindings();

					if (binds == null) {
						binds = scriptEngineManager.getBindings();
					}

//        Bindings binds = scriptEngineManager.getBindings();
					initBindings(binds);
					com.runCommand(iqc, binds, results);
				} else {
					if (log.isLoggable(Level.FINER)) {
						log.finer("Command rejected non-admin detected: " + pc.getStanzaFrom());
					}

					results.offer(Authorization.FORBIDDEN.getResponseMessage(pc,
							"Only Administrator can call the command.", true));
				}
			} catch (Exception e) {
				log.log(Level.WARNING, "Unknown admin command processing exception: " + pc.toString(),
						e);
			}

			return true;
		}

		return false;
	}

	private void loadScripts() {
		File file = null;
		AddScriptCommand addCommand = new AddScriptCommand();
		Bindings binds = scriptEngineManager.getBindings();

		initBindings(binds);

		String[] dirs = new String[] { scriptsBaseDir, scriptsCompDir };

		for (String scriptsPath : dirs) {
			try {
				File adminDir = new File(scriptsPath);

				if ((adminDir != null) && adminDir.exists()) {
					for (File f : adminDir.listFiles()) {

						// Just regular files here....
						if (f.isFile() &&!f.toString().endsWith("~")) {
							String cmdId = null;
							String cmdDescr = null;
							String comp = null;

							file = f;

							StringBuilder sb = new StringBuilder();
							BufferedReader buffr = new BufferedReader(new FileReader(file));
							String line = null;

							while ((line = buffr.readLine()) != null) {
								sb.append(line + "\n");

								int idx = line.indexOf(CommandIfc.SCRIPT_DESCRIPTION);

								if (idx >= 0) {
									cmdDescr = line.substring(idx
											+ CommandIfc.SCRIPT_DESCRIPTION.length()).trim();
								}

								idx = line.indexOf(CommandIfc.SCRIPT_ID);

								if (idx >= 0) {
									cmdId = line.substring(idx + CommandIfc.SCRIPT_ID.length()).trim();
								}

								idx = line.indexOf(CommandIfc.SCRIPT_COMPONENT);

								if (idx >= 0) {
									comp = line.substring(idx + CommandIfc.SCRIPT_COMPONENT.length()).trim();
								}
							}

							buffr.close();

							if ((cmdId == null) || (cmdDescr == null) || (comp == null)) {
								log.warning("Admin script found but it has no command ID or command"
										+ "description: " + file);

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

							if ( !found) {
								log.info("Admin script for a different component: " + comp);

								continue;
							}

							int idx = file.toString().lastIndexOf(".");
							String ext = file.toString().substring(idx + 1);

							addCommand.addAdminScript(cmdId, cmdDescr, sb.toString(), null, ext, binds);
							log.config(getName() + ": Loaded admin command from file: " + file + ", id: "
									+ cmdId + ", ext: " + ext + ", descr: " + cmdDescr);
						}
					}
				} else {
					log.warning("Admin scripts directory is missing: " + adminDir + ", creating...");
					adminDir.mkdirs();
				}
			} catch (Exception e) {
				log.log(Level.WARNING, "Can't load the admin script file: " + file, e);
			}
		}
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
