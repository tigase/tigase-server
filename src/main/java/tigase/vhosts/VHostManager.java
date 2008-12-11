/*  Tigase Jabber/XMPP Server
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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.conf.Configurable;
import tigase.disco.ServiceEntity;
import tigase.disco.ServiceIdentity;
import tigase.server.AbstractComponentRegistrator;
import tigase.disco.XMPPService;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.Permissions;
import tigase.server.ServerComponent;
import tigase.stats.StatRecord;
import tigase.stats.StatisticsContainer;
import tigase.util.JIDUtils;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.StanzaType;

/**
 * Describe class VHostManager here.
 *
 *
 * Created: Fri Nov 21 14:28:20 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class VHostManager	extends AbstractComponentRegistrator<VHostListener>
	implements XMPPService, VHostManagerIfc, Configurable,
				StatisticsContainer {

	public static final String VHOSTS_REPO_CLASS_PROPERTY = "--vhost-repo-class";
	public static final String VHOSTS_REPO_CLASS_PROP_KEY = "repository-class";
	public static final String VHOSTS_REPO_CLASS_PROP_VAL =
					"tigase.vhosts.VHostJDBCRepository";

	private static final Logger log =
					Logger.getLogger("tigase.vhosts.VHostManager");

  private LinkedHashSet<VHostListener> localDomainsHandlers =
					new LinkedHashSet<VHostListener>();
	private LinkedHashSet<VHostListener> nonLocalDomainsHandlers =
					new LinkedHashSet<VHostListener>();
	private LinkedHashSet<VHostListener> nameSubdomainsHandlers =
					new LinkedHashSet<VHostListener>();
//	private LinkedHashMap<String, VHostItem> vhosts =
//					new LinkedHashMap<String, VHostItem>();

	private ServiceEntity serviceEntity = null;
	private VHostRepository repo = null;
	private long isLocalDomainCalls = 0;
	private long isAnonymousEnabledCalls = 0;
	private long getComponentsForLocalDomainCalls = 0;
	private long getComponentsForNonLocalDomainCalls = 0;

	/**
	 * Creates a new <code>VHostManager</code> instance.
	 *
	 */
	public VHostManager() {

	}

	@Override
	public void setName(String name) {
		super.setName(name);
		serviceEntity = new ServiceEntity(name, null, "VHosts Manager");
		serviceEntity.addIdentities(
			new ServiceIdentity("component", "generic",	"VHost Manager"),
//			new ServiceIdentity("automation", "command-node",	"All VHosts"),
			new ServiceIdentity("automation", "command-list",
				"VHosts management commands"));
		serviceEntity.addFeatures(DEF_FEATURES);
		serviceEntity.addFeatures(CMD_FEATURES);

		ServiceEntity item = new ServiceEntity(getName(),
						Command.VHOSTS_RELOAD.toString(),
						"Reload VHosts from repository");
		item.addFeatures(CMD_FEATURES);
		item.addIdentities(new ServiceIdentity("automation", "command-node",
						"Reload VHosts from repository"));
		serviceEntity.addItems(item);

		item = new ServiceEntity(getName(),
						Command.VHOSTS_UPDATE.toString(),
						"Add/Update selected VHost information");
		item.addFeatures(CMD_FEATURES);
		item.addIdentities(new ServiceIdentity("automation", "command-node",
						"Add/Update selected VHost information"));
		serviceEntity.addItems(item);

		item = new ServiceEntity(getName(),
						Command.VHOSTS_REMOVE.toString(),
						"Remove selected VHost");
		item.addFeatures(CMD_FEATURES);
		item.addIdentities(new ServiceIdentity("automation", "command-node",
						"Remove selected VHost"));
		serviceEntity.addItems(item);

	}

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

	@Override
	public void componentRemoved(VHostListener component) {
		localDomainsHandlers.remove(component);
		nonLocalDomainsHandlers.remove(component);
		nameSubdomainsHandlers.remove(component);
	}

	@Override
	public boolean isCorrectType(ServerComponent component) {
		return component instanceof VHostListener;
	}

	public void processPacket(Packet packet, Queue<Packet> results) {
		if (!packet.isCommand() ||
						(packet.getType() != null &&
						packet.getType() == StanzaType.result)) {
			return;
		}
		if (packet.getPermissions() != Permissions.ADMIN) {
			try {
				results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
								"You are not authorized for this action.", true));
			} catch (PacketErrorTypeException e) {
				log.warning("Packet processing exception: " + e);
			}
			return;
		}
		Command.Action action = Command.getAction(packet);
		if (action == Command.Action.cancel) {
			Packet result = packet.commandResult(null);
			results.offer(result);
			return;
		}

		Packet result = packet.commandResult("result");
		switch (packet.getCommand()) {
			case VHOSTS_RELOAD:
				repo.reload();
				addCompletedVHostsField(result);
				results.offer(result);
				break;
			case VHOSTS_UPDATE:
					if (Command.getData(packet) == null) {
						prepareVHostData(result);
						results.offer(result);
					} else {
						updateVHostChanges(packet, result);
						results.offer(result);
					}

				break;
			case VHOSTS_REMOVE:
					if (Command.getData(packet) == null) {
						prepareVHostRemove(result);
						results.offer(result);
					} else {
						updateVHostRemove(packet, result);
						results.offer(result);
					}
				break;
			default:
				break;
		}
	}

	public List<Element> getDiscoFeatures() { return null; }

	public Element getDiscoInfo(String node, String jid) {
		if (jid != null && getName().equals(JIDUtils.getNodeNick(jid))) {
			return serviceEntity.getDiscoInfo(node);
		}
		return null;
	}

	public List<Element> getDiscoItems(String node, String jid) {
		if (getName().equals(JIDUtils.getNodeNick(jid)) ||
						getComponentId().equals(jid)) {
			List<Element> items = serviceEntity.getDiscoItems(node, jid);
			log.finest("Processing discoItems for node: " + node + ", result: "
				+ (items == null ? null : items.toString()));
			return items;
		} else {
			Element item = serviceEntity.getDiscoItem(null,
				JIDUtils.getNodeID(getName(),	jid));
			log.finest("Processing discoItems, result: "
				+ (item == null ? null : item.toString()));
			return Arrays.asList(item);
		}
	}

	public boolean isLocalDomain(String domain) {
		++isLocalDomainCalls;
		return repo.contains(domain);
	}

	public boolean isLocalDomainOrComponent(String domain) {
		boolean result = isLocalDomain(domain);
		if (!result) {
			int idx = domain.indexOf('.');
			if (idx > 0) {
				String name = domain.substring(0, idx);
				String basedomain = domain.substring(idx + 1);
				VHostListener listener = components.get(name);
				result = (listener != null && listener.handlesNameSubdomains() &&
								isLocalDomain(basedomain));
			}
		}
		return result;
	}

	public boolean isAnonymousEnabled(String domain) {
		++isAnonymousEnabledCalls;
		VHostItem vhost = repo.getVHost(domain);
		if (vhost == null) {
			return false;
		} else {
			return vhost.isAnonymousEnabled();
		}
	}

	public ServerComponent[] getComponentsForNonLocalDomain(String domain) {
		++getComponentsForNonLocalDomainCalls;
		// Return components for non-local domains
		if (nonLocalDomainsHandlers.size() > 0) {
			return nonLocalDomainsHandlers.toArray(
							new ServerComponent[nonLocalDomainsHandlers.size()]);
		} else {
			return null;
		}
	}
	
	public ServerComponent[] getComponentsForLocalDomain(String domain) {
		++getComponentsForLocalDomainCalls;
		VHostItem vhost = repo.getVHost(domain);
		if (vhost == null) {
			// This is not a local domain.
			// Maybe this is a 'name' subdomain: 'pubsub'.domain.name
			int idx = domain.indexOf('.');
			if (idx > 0) {
				String name = domain.substring(0, idx);
				String basedomain = domain.substring(idx + 1);
				VHostListener listener = components.get(name);
				if (listener != null && listener.handlesNameSubdomains() &&
								isLocalDomain(basedomain)) {
					return new ServerComponent[] {listener};
				}
			}
			return null;
		} else {
			// Return all components for local domains and components selected
			// for this specific domain
			LinkedHashSet<ServerComponent> results =
							new LinkedHashSet<ServerComponent>();
			results.addAll(localDomainsHandlers);
			String[] comps = vhost.getComps();
			if (comps != null) {
				for (String comp_name : comps) {
					VHostListener listener = components.get(comp_name);
					if (listener != null) {
						results.add(listener);
					}
				}
			}
			if (results.size() > 0) {
				return results.toArray(new ServerComponent[results.size()]);
			} else {
				return null;
			}
		}
	}

	public void setProperties(Map<String, Object> properties) {
		String repo_class = (String)properties.get(VHOSTS_REPO_CLASS_PROP_KEY);
		try {
			VHostRepository repo_tmp =
							(VHostRepository) Class.forName(repo_class).newInstance();
			repo_tmp.setProperties(properties);
			repo = repo_tmp;
		} catch (Exception e) {
			log.log(Level.SEVERE, 
							"Can not create VHost repository instance for class: " +
							repo_class, e);
		}
	}

	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defs = new LinkedHashMap<String, Object>();
		String repo_class = (String)params.get(VHOSTS_REPO_CLASS_PROPERTY);
		if (repo_class == null) {
			repo_class = VHOSTS_REPO_CLASS_PROP_VAL;
		}
		defs.put(VHOSTS_REPO_CLASS_PROP_KEY, repo_class);
		try {
			VHostRepository repo_tmp =
							(VHostRepository) Class.forName(repo_class).newInstance();
			repo_tmp.getDefaults(defs, params);
		} catch (Exception e) {
			log.log(Level.SEVERE,
							"Can not instantiate VHosts repository for class: " +
							repo_class, e);
		}
		return defs;
	}

	public List<StatRecord> getStatistics() {
		List<StatRecord> stats = new LinkedList<StatRecord>();
		stats.add(new StatRecord(getName(), "Number of VHosts", "int",
						repo.size(), Level.INFO));
		stats.add(new StatRecord(getName(), "Checks: is local domain", "long",
						isLocalDomainCalls, Level.FINER));
		stats.add(new StatRecord(getName(), "Checks: is anonymous domain", "long",
						isAnonymousEnabledCalls, Level.FINER));
		stats.add(new StatRecord(getName(), "Get components for local domain", "long",
						getComponentsForLocalDomainCalls, Level.FINER));
		stats.add(new StatRecord(getName(), "Get components for non-local domain", "long",
						getComponentsForNonLocalDomainCalls, Level.FINER));
		return stats;
	}

	private void addCompletedVHostsField(Packet result) {
		Command.setStatus(result, Command.Status.completed);
		Command.addFieldValue(result, "Note",
						"Current number of VHosts: " + repo.size(), "fixed");
	}

	private void prepareVHostData(Packet result) {
		Command.setStatus(result, Command.Status.executing);
		Command.addAction(result, Command.Action.complete);
		Command.addFieldValue(result, "VHost", "");
		Command.addFieldValue(result, "Enabled", "true", "Enabled",
						new String[]{"true", "false"},
						new String[]{"true", "false"});
	}

	private void updateVHostChanges(Packet packet, Packet result) {
		String vh = Command.getFieldValue(packet, "VHost");
		if (vh != null && !vh.isEmpty()) {
			VHostItem vhost = new VHostItem(vh);
			String enabled = Command.getFieldValue(packet, "Enabled");
			vhost.setEnabled(enabled == null || enabled.isEmpty() ||
							"true".equals(enabled));
			repo.addVHost(vhost);
			addCompletedVHostsField(result);
		} else {
			Command.setStatus(result, Command.Status.completed);
			Command.addFieldValue(result, "Note",
							"Incorrect VHost name given", "fixed");
		}
	}

	private void updateVHostRemove(Packet packet, Packet result) {
		String vh = Command.getFieldValue(packet, "VHost");
		if (vh != null && !vh.isEmpty()) {
			repo.removeVHost(vh);
			addCompletedVHostsField(result);
		} else {
			Command.setStatus(result, Command.Status.completed);
			Command.addFieldValue(result, "Note",
							"Incorrect VHost name given", "fixed");
		}
	}

	private void prepareVHostRemove(Packet result) {
		Command.setStatus(result, Command.Status.executing);
		Command.addAction(result, Command.Action.complete);
		Command.addFieldValue(result, "VHost", "");
	}

}
