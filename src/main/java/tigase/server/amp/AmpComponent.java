/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
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
 */
package tigase.server.amp;

import tigase.disco.XMPPService;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.selector.ClusterModeRequired;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.core.Kernel;
import tigase.server.AbstractMessageReceiver;
import tigase.server.Packet;
import tigase.server.amp.action.Broadcast;
import tigase.server.amp.cond.Deliver;
import tigase.server.amp.cond.ExpireAt;
import tigase.server.amp.cond.MatchResource;
import tigase.sys.TigaseRuntime;
import tigase.xml.Element;
import tigase.xmpp.jid.JID;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Apr 26, 2010 3:22:06 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Bean(name = "amp", parent = Kernel.class, active = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SessionManagerMode, ConfigTypeEnum.ConnectionManagersMode,
			 ConfigTypeEnum.ComponentMode})
@ClusterModeRequired(active = false)
public class AmpComponent
		extends AbstractMessageReceiver
		implements ActionResultsHandlerIfc, RegistrarBean {

	private static final String AMP_NODE = "http://jabber.org/protocol/amp";
	private static final Logger log = Logger.getLogger(AmpComponent.class.getName());
	private static final String AMP_XMLNS = AMP_NODE;
	private static final Element top_feature = new Element("feature", new String[]{"var"}, new String[]{AMP_NODE});

	@Inject
	protected Broadcast broadcast = null;
	// ~--- fields ---------------------------------------------------------------
	private ConcurrentSkipListMap<String, ActionIfc> actions = new ConcurrentSkipListMap<String, ActionIfc>();
	@Inject
	private List<ActionIfc> allActions = new ArrayList<>();
	private ConcurrentSkipListMap<String, ConditionIfc> conditions = new ConcurrentSkipListMap<String, ConditionIfc>();

	public AmpComponent() {
		ConditionIfc condition = new Deliver();
		conditions.put(condition.getName(), condition);
		condition = new ExpireAt();
		conditions.put(condition.getName(), condition);
		condition = new MatchResource();
		conditions.put(condition.getName(), condition);

	}

	@Override
	public boolean addOutPacket(Packet packet) {
		return super.addOutPacket(packet);
	}

	@Override
	public boolean addOutPackets(Queue<Packet> packets) {
		return super.addOutPackets(packets);
	}

	@Override
	public String getDiscoCategoryType() {
		return "generic";
	}

	@Override
	public String getDiscoDescription() {
		return "IM AMP Support";
	}

	@Override
	public Element getDiscoInfo(String node, JID jid, JID from) {
		Element query = super.getDiscoInfo(node, jid, from);

		if ((jid != null) && (getName().equals(jid.getLocalpart()) || isLocalDomain(jid.toString())) &&
				(AMP_NODE.equals(node))) {
			if (query == null) {
				query = new Element("query");
				query.setXMLNS(XMPPService.INFO_XMLNS);
			}
			query.addChild(new Element("identity", new String[]{"name", "category", "type"},
									   new String[]{getDiscoDescription(), "im", getDiscoCategoryType()}));
			query.addChild(top_feature);
			for (ActionIfc action : actions.values()) {
				query.addChild(new Element("feature", new String[]{"var"},
										   new String[]{AMP_NODE + "?action=" + action.getName()}));
			}
			for (ConditionIfc cond : conditions.values()) {
				query.addChild(new Element("feature", new String[]{"var"},
										   new String[]{AMP_NODE + "?condition=" + cond.getName()}));
			}

			// for (ProcessingThreads<ProcessorWorkerThread> proc_t :
			// processors.values()) {
			// Element[] discoFeatures =
			// proc_t.getWorkerThread().processor.supDiscoFeatures(null);
			//
			// if (discoFeatures != null) {
			// query.addChildren(Arrays.asList(discoFeatures));
			// } // end of if (discoFeatures != null)
			// }
		}
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Found disco info: " + ((query != null) ? query.toString() : null));
		}

		return query;
	}

	@Override
	public int processingInThreads() {
		return TigaseRuntime.getTigaseRuntime().getCPUsNumber() * 4;
	}

	@Override
	public int processingOutThreads() {
		return TigaseRuntime.getTigaseRuntime().getCPUsNumber() * 4;
	}

	@Override
	public void processPacket(Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("My packet: " + packet);
		}

		if (broadcast.preprocess(packet)) {
			return;
		}

		ActionIfc def = null;

		if (packet.getAttributeStaticStr(AmpFeatureIfc.OFFLINE) == null) {
			def = actions.get("deliver");
		} else {
			def = actions.get("store");
		}

		boolean exec_def = true;
		Element amp = packet.getElement().getChild("amp", AMP_XMLNS);

		if (amp != null) {
			List<Element> rules = amp.getChildren();

			if ((rules != null) && (rules.size() > 0)) {
				for (Element rule : rules) {
					if (matchCondition(packet, rule)) {
						exec_def = executeAction(packet, rule);

						break;
					}
				}
			} else {
				log.warning("AMP packet but empty rule-set! " + packet);

				// In case of such error, let's just drop the packet
				return;
			}
		} else {
			log.warning("Not an AMP packet! " + packet);

			// In case of such error, let's just drop the packet
			return;
		}
		if (exec_def) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Executing default action: " + def.getName());
			}
			def.execute(packet, null);
		}
	}

	public void setAllActions(List<ActionIfc> actions) {
		ConcurrentSkipListMap<String, ActionIfc> map = new ConcurrentSkipListMap<>();
		for (ActionIfc action : actions) {
			action.setActionResultsHandler(this);
			map.put(action.getName(), action);
		}
		this.actions = map;
	}

	@Override
	public void register(Kernel kernel) {

	}

	@Override
	public void unregister(Kernel kernel) {

	}

	// ~--- methods --------------------------------------------------------------
	private boolean executeAction(Packet packet, Element rule) {
		String act = rule.getAttributeStaticStr(AmpFeatureIfc.ACTION_ATT);

		if (act != null) {
			ActionIfc action = actions.get(act);

			if (action != null) {
				boolean result = action.execute(packet, rule);

				if (log.isLoggable(Level.FINEST)) {
					log.finest("Matched action: " + action.getName() + ", result: " + result);
				}

				return result;
			} else {
				log.fine("No action found for act: " + act);
			}
		} else {
			log.fine("No actionset for rule: " + rule);
		}

		return true;
	}

	private boolean matchCondition(Packet packet, Element rule) {
		String cond = rule.getAttributeStaticStr(AmpFeatureIfc.CONDITION_ATT);

		if (cond != null) {
			ConditionIfc condition = conditions.get(cond);

			if (condition != null) {
				boolean result = condition.match(packet, rule);
				;

				if (log.isLoggable(Level.FINEST)) {
					log.finest("Matched condition: " + condition.getName() + ", result: " + result);
				}

				return result;
			} else {
				log.fine("No condition found for cond: " + cond);
			}
		} else {
			log.fine("No condition set for rule: " + rule);
		}

		return false;
	}
}

