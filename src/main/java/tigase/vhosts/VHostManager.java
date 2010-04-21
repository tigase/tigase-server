/*
 *   Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

package tigase.vhosts;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.TigaseDBException;
import tigase.db.comp.ComponentRepository;

import tigase.server.AbstractComponentRegistrator;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.ServerComponent;

import tigase.stats.StatisticsContainer;
import tigase.stats.StatisticsList;

import tigase.xmpp.JID;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.Bindings;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class VHostManager here.
 *
 *
 * Created: Fri Nov 21 14:28:20 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class VHostManager extends AbstractComponentRegistrator<VHostListener>
		implements VHostManagerIfc, StatisticsContainer {

	/** Field description */
	public static final String VHOSTS_REPO_CLASS_PROPERTY = "--vhost-repo-class";

	/** Field description */
	public static final String VHOSTS_REPO_CLASS_PROP_KEY = "repository-class";

	/** Field description */
	public static final String VHOSTS_REPO_CLASS_PROP_VAL = "tigase.vhosts.VHostJDBCRepository";
	private static final Logger log = Logger.getLogger(VHostManager.class.getName());

	//~--- fields ---------------------------------------------------------------

	private long getComponentsForLocalDomainCalls = 0;
	private long getComponentsForNonLocalDomainCalls = 0;

//private ServiceEntity serviceEntity = null;
	private String identity_type = "generic";
	private long isAnonymousEnabledCalls = 0;
	private long isLocalDomainCalls = 0;
	private LinkedHashSet<VHostListener> localDomainsHandlers =
		new LinkedHashSet<VHostListener>();
	private LinkedHashSet<VHostListener> nonLocalDomainsHandlers =
		new LinkedHashSet<VHostListener>();
	private LinkedHashSet<VHostListener> nameSubdomainsHandlers =
		new LinkedHashSet<VHostListener>();
	private ConcurrentSkipListSet<String> registeredComponentDomains =
		new ConcurrentSkipListSet<String>();
	private ComponentRepository<VHostItem> repo = null;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Creates a new <code>VHostManager</code> instance.
	 *
	 */
	public VHostManager() {}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 */
	@Override
	public void addComponentDomain(String domain) {
		registeredComponentDomains.add(domain);
	}

	/**
	 * Method description
	 *
	 *
	 * @param component
	 */
	@Override
	public void componentAdded(VHostListener component) {
		component.setVHostManager(this);

		if (component.handlesLocalDomains()) {
			localDomainsHandlers.add(component);
		}

		if (component.handlesNonLocalDomains()) {
			nonLocalDomainsHandlers.add(component);
		}

		if (component.handlesNameSubdomains()) {
			nameSubdomainsHandlers.add(component);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param component
	 */
	@Override
	public void componentRemoved(VHostListener component) {
		localDomainsHandlers.remove(component);
		nonLocalDomainsHandlers.remove(component);
		nameSubdomainsHandlers.remove(component);
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 *
	 * @return
	 */
	@Override
	public ServerComponent[] getComponentsForLocalDomain(String domain) {
		++getComponentsForLocalDomainCalls;

		VHostItem vhost = repo.getItem(domain);

		if (vhost == null) {

			// This is not a local domain.
			// Maybe this is a 'name' subdomain: 'pubsub'.domain.name
			int idx = domain.indexOf('.');

			if (idx > 0) {
				String name = domain.substring(0, idx);
				String basedomain = domain.substring(idx + 1);
				VHostListener listener = components.get(name);

				if ((listener != null) && listener.handlesNameSubdomains()
						&& isLocalDomain(basedomain)) {
					return new ServerComponent[] { listener };
				}
			}

			return null;
		} else {

			// Return all components for local domains and components selected
			// for this specific domain
			LinkedHashSet<ServerComponent> results = new LinkedHashSet<ServerComponent>();

			results.addAll(localDomainsHandlers);

			// The code below seems like a bug to me and redundand
			// localDomainHandlers have been just added above.
//    String[] comps = vhost.getComps();
//
//    if (comps != null) {
//      for (String comp_name : comps) {
//        VHostListener listener = components.get(comp_name);
//
//        if (listener != null) {
//          results.add(listener);
//        }
//      }
//    }
			if (results.size() > 0) {
				return results.toArray(new ServerComponent[results.size()]);
			} else {
				return null;
			}
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 *
	 * @return
	 */
	@Override
	public ServerComponent[] getComponentsForNonLocalDomain(String domain) {
		++getComponentsForNonLocalDomainCalls;

		// Return components for non-local domains
		if (nonLocalDomainsHandlers.size() > 0) {
			return nonLocalDomainsHandlers.toArray(new ServerComponent[nonLocalDomainsHandlers.size()]);
		} else {
			return null;
		}
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
	@SuppressWarnings({ "unchecked" })
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defs = super.getDefaults(params);
		String repo_class = (String) params.get(VHOSTS_REPO_CLASS_PROPERTY);

		if (repo_class == null) {
			repo_class = VHOSTS_REPO_CLASS_PROP_VAL;
		}

		defs.put(VHOSTS_REPO_CLASS_PROP_KEY, repo_class);

		try {
			ComponentRepository<VHostItem> repo_tmp =
				(ComponentRepository<VHostItem>) Class.forName(repo_class).newInstance();

			repo_tmp.getDefaults(defs, params);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Can not instantiate VHosts repository for class: " + repo_class,
					e);
		}

		return defs;
	}

///**
// * Method description
// *
// *
// * @param from
// *
// * @return
// */
//@Override
//public List<Element> getDiscoFeatures(JID from) {
//  return null;
//}
///**
// * Method description
// *
// *
// * @param node
// * @param jid
// * @param from
// *
// * @return
// */
//@Override
//public Element getDiscoInfo(String node, JID jid, JID from) {
//  if ((jid != null) && getName().equals(jid.getLocalpart()) && isAdmin(from)) {
//    return serviceEntity.getDiscoInfo(node);
//  }
//
//  return null;
//}
///**
// * Method description
// *
// *
// * @param node
// * @param jid
// * @param from
// *
// * @return
// */
//@Override
//public List<Element> getDiscoItems(String node, JID jid, JID from) {
//  if (isAdmin(from)) {
//    if (getName().equals(jid.getLocalpart()) || getComponentId().equals(jid)) {
//      List<Element> items = serviceEntity.getDiscoItems(node, jid.toString());
//
//      if (log.isLoggable(Level.FINEST)) {
//        log.finest("Processing discoItems for node: " + node + ", result: "
//            + ((items == null) ? null : items.toString()));
//      }
//
//      return items;
//    } else {
//      if (node == null) {
//        Element item = serviceEntity.getDiscoItem(null,
//          BareJID.toString(getName(), jid.toString()));
//
//        if (log.isLoggable(Level.FINEST)) {
//          log.finest("Processing discoItems, result: "
//              + ((item == null) ? null : item.toString()));
//        }
//
//        return Arrays.asList(item);
//      } else {
//        return null;
//      }
//    }
//  }
//
//  return null;
//}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getDiscoCategoryType() {
		return identity_type;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getDiscoDescription() {
		return "VHost Manager";
	}

	/**
	 * Method description
	 *
	 *
	 * @param list
	 */
	@Override
	public void getStatistics(StatisticsList list) {
		list.add(getName(), "Number of VHosts", repo.size(), Level.FINE);
		list.add(getName(), "Checks: is local domain", isLocalDomainCalls, Level.FINER);
		list.add(getName(), "Checks: is anonymous domain", isAnonymousEnabledCalls, Level.FINER);
		list.add(getName(), "Get components for local domain", getComponentsForLocalDomainCalls,
				Level.FINER);
		list.add(getName(), "Get components for non-local domain",
				getComponentsForNonLocalDomainCalls, Level.FINER);
	}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 *
	 * @return
	 */
	@Override
	public VHostItem getVHostItem(String domain) {
		return repo.getItem(domain);
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param binds
	 */
	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds);
		binds.put(ComponentRepository.COMP_REPO_BIND, repo);
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 *
	 * @return
	 */
	@Override
	public boolean isAnonymousEnabled(String domain) {
		++isAnonymousEnabledCalls;

		VHostItem vhost = repo.getItem(domain);

		if (vhost == null) {
			return false;
		} else {
			return vhost.isAnonymousEnabled();
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param component
	 *
	 * @return
	 */
	@Override
	public boolean isCorrectType(ServerComponent component) {
		return component instanceof VHostListener;
	}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 *
	 * @return
	 */
	@Override
	public boolean isLocalDomain(String domain) {
		++isLocalDomainCalls;

		return repo.contains(domain);
	}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 *
	 * @return
	 */
	@Override
	public boolean isLocalDomainOrComponent(String domain) {
		boolean result = isLocalDomain(domain);

		if ( !result) {
			result = registeredComponentDomains.contains(domain);
		}

		if ( !result) {
			int idx = domain.indexOf('.');

			if (idx > 0) {
				String name = domain.substring(0, idx);
				String basedomain = domain.substring(idx + 1);
				VHostListener listener = components.get(name);

				result = ((listener != null) && listener.handlesNameSubdomains()
						&& isLocalDomain(basedomain));
			}
		}

		return result;
	}

	//~--- methods --------------------------------------------------------------

///**
// * Method description
// *
// *
// * @param packet
// * @param results
// */
//@Override
//public void processPacket(Packet packet, Queue<Packet> results) {
//  if ( !packet.isCommand()
//      || ((packet.getType() != null) && (packet.getType() == StanzaType.result))) {
//    return;
//  }
//
//  if (packet.getPermissions() != Permissions.ADMIN) {
//    try {
//      results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
//          "You are not authorized for this action.", true));
//    } catch (PacketErrorTypeException e) {
//      log.warning("Packet processing exception: " + e);
//    }
//
//    return;
//  }
//
//  Iq iqc = (Iq) packet;
//  Command.Action action = Command.getAction(iqc);
//
//  if (action == Command.Action.cancel) {
//    Packet result = iqc.commandResult(null);
//
//    results.offer(result);
//
//    return;
//  }
//
//  if (log.isLoggable(Level.INFO)) {
//    log.info("Processing command: " + iqc.toString());
//  }
//
//  Packet result = null;
//
//  if ((iqc.getCommand() == Command.VHOSTS_RELOAD) || (Command.getData(iqc) != null)) {
//    result = iqc.commandResult(Command.DataType.result);
//  } else {
//    result = iqc.commandResult(Command.DataType.form);
//  }
//
//  if (log.isLoggable(Level.FINEST)) {
//    log.finest("Preparing result: " + result.toString());
//  }
//
//  switch (iqc.getCommand()) {
//    case VHOSTS_RELOAD :
//      try {
//        repo.reload();
//      } catch (TigaseDBException ex) {
//        log.log(Level.WARNING, "Problem reloading VHost repository: ", ex);
//      }
//
//      addCompletedVHostsField(result);
//      results.offer(result);
//
//      break;
//
//    case VHOSTS_UPDATE :
//      if (Command.getData(packet) == null) {
//        prepareVHostData(result);
//
//        if (log.isLoggable(Level.FINEST)) {
//          log.finest("Sending result back: " + result.toString());
//        }
//
//        results.offer(result);
//      } else {
//        updateVHostChanges(packet, result);
//        results.offer(result);
//      }
//
//      break;
//
//    case VHOSTS_REMOVE :
//      if (Command.getData(packet) == null) {
//        prepareVHostRemove(result);
//        results.offer(result);
//      } else {
//        updateVHostRemove(packet, result);
//        results.offer(result);
//      }
//
//      break;
//
//    default :
//      break;
//  }
//}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 */
	@Override
	public void removeComponentDomain(String domain) {
		registeredComponentDomains.remove(domain);
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
		super.setName(name);

//  serviceEntity = new ServiceEntity(name, null, "VHosts Manager");
//  serviceEntity.addIdentities(new ServiceIdentity("component", "generic", "VHost Manager"),
//
////new ServiceIdentity("automation", "command-node", "All VHosts"),
//  new ServiceIdentity("automation", "command-list", "VHosts management commands"));
//  serviceEntity.addFeatures(DEF_FEATURES);
//  serviceEntity.addFeatures(CMD_FEATURES);
//
//  ServiceEntity item = new ServiceEntity(getName(), Command.VHOSTS_RELOAD.toString(),
//    "Reload VHosts from repository");
//
//  item.addFeatures(CMD_FEATURES);
//  item.addIdentities(new ServiceIdentity("automation", "command-node",
//      "Reload VHosts from repository"));
//  serviceEntity.addItems(item);
//  item = new ServiceEntity(getName(), Command.VHOSTS_UPDATE.toString(),
//      "Add/Update selected VHost information");
//  item.addFeatures(CMD_FEATURES);
//  item.addIdentities(new ServiceIdentity("automation", "command-node",
//      "Add/Update selected VHost information"));
//  serviceEntity.addItems(item);
//  item = new ServiceEntity(getName(), Command.VHOSTS_REMOVE.toString(),
//      "Remove selected VHost");
//  item.addFeatures(CMD_FEATURES);
//  item.addIdentities(new ServiceIdentity("automation", "command-node",
//      "Remove selected VHost"));
//  serviceEntity.addItems(item);
	}

	/**
	 * Method description
	 *
	 *
	 * @param properties
	 */
	@Override
	@SuppressWarnings({ "unchecked" })
	public void setProperties(Map<String, Object> properties) {
		super.setProperties(properties);

		String repo_class = (String) properties.get(VHOSTS_REPO_CLASS_PROP_KEY);

		try {
			ComponentRepository<VHostItem> repo_tmp =
				(ComponentRepository<VHostItem>) Class.forName(repo_class).newInstance();

			repo_tmp.setProperties(properties);
			repo = repo_tmp;
		} catch (Exception e) {
			log.log(Level.SEVERE,
					"Can not create VHost repository instance for class: " + repo_class, e);
		}
	}

	//~--- methods --------------------------------------------------------------

	private void addCompletedVHostsField(Packet result) {
		Command.addFieldValue(result, "Note", "Current number of VHosts: " + repo.size(), "fixed");
	}

	private void prepareVHostData(Packet result) {
		Command.addFieldValue(result, "VHost", "");
		Command.addFieldValue(result, "Enabled", "true", "Enabled", new String[] { "true",
				"false" }, new String[] { "true", "false" });
	}

	private void prepareVHostRemove(Packet result) {
		Command.addFieldValue(result, "VHost", "");
	}

	private void updateVHostChanges(Packet packet, Packet result) {
		String vh = Command.getFieldValue(packet, "VHost");

		if ((vh != null) &&!vh.isEmpty()) {
			VHostItem vhost = new VHostItem(JID.jidInstanceNS(vh));
			String enabled = Command.getFieldValue(packet, "Enabled");

			vhost.setEnabled((enabled == null) || enabled.isEmpty() || "true".equals(enabled));

			try {
				repo.addItem(vhost);
			} catch (TigaseDBException ex) {
				log.log(Level.WARNING, "Problem adding VHost item to repository: ", ex);
			}

			addCompletedVHostsField(result);
		} else {
			Command.addFieldValue(result, "Note", "Incorrect VHost name given", "fixed");
		}
	}

	private void updateVHostRemove(Packet packet, Packet result) {
		String vh = Command.getFieldValue(packet, "VHost");

		if ((vh != null) &&!vh.isEmpty()) {
			try {
				repo.removeItem(vh);
			} catch (TigaseDBException ex) {
				log.log(Level.WARNING, "Problem removing VHost item from repository: ", ex);
			}

			addCompletedVHostsField(result);
		} else {
			Command.addFieldValue(result, "Note", "Incorrect VHost name given", "fixed");
		}
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
