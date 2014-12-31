/*
 * AmpComponent.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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
 */



package tigase.server.amp;

//~--- non-JDK imports --------------------------------------------------------

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.conf.ConfigurationException;
import tigase.disco.XMPPService;
import tigase.server.AbstractMessageReceiver;
import tigase.server.Packet;
import tigase.server.ServerComponent;
import tigase.server.XMPPServer;
import tigase.server.amp.action.Alert;
import tigase.server.amp.action.Broadcast;
import tigase.server.amp.action.Drop;
import tigase.server.amp.action.Notify;
import tigase.server.amp.action.Store;
import tigase.server.amp.cond.Deliver;
import tigase.server.amp.cond.ExpireAt;
import tigase.server.amp.cond.MatchResource;
import tigase.server.xmppsession.SessionManager;
import tigase.server.xmppsession.SessionManagerHandler;
import tigase.xml.Element;
import tigase.xmpp.JID;

/**
 * Created: Apr 26, 2010 3:22:06 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class AmpComponent
				extends AbstractMessageReceiver
				implements ActionResultsHandlerIfc {
	private static final String AMP_NODE     = "http://jabber.org/protocol/amp";
	private static final Logger log          =
		Logger.getLogger(AmpComponent.class.getName());
	private static final String AMP_XMLNS    = AMP_NODE;
	private static final Element top_feature = new Element("feature",
																							 new String[] { "var" },
																							 new String[] { AMP_NODE });

	//~--- fields ---------------------------------------------------------------

	// ~--- fields ---------------------------------------------------------------
	private Map<String, ActionIfc> actions       = new ConcurrentSkipListMap<String,
																									 ActionIfc>();
	private Map<String, ConditionIfc> conditions = new ConcurrentSkipListMap<String,
																									 ConditionIfc>();

	protected final Broadcast broadcast = new Broadcast();
	//~--- methods --------------------------------------------------------------

	// ~--- methods --------------------------------------------------------------

	@Override
	public boolean addOutPacket(Packet packet) {
		return super.addOutPacket(packet);
	}

	@Override
	public boolean addOutPackets(Queue<Packet> packets) {
		return super.addOutPackets(packets);
	}

	//~--- get methods ----------------------------------------------------------
	
	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defs = super.getDefaults(params);
		ActionIfc action         = new Drop();

		actions.put(action.getName(), action);
		action = new tigase.server.amp.action.Error();
		actions.put(action.getName(), action);
		action = new Notify();
		actions.put(action.getName(), action);
		action = new tigase.server.amp.action.Deliver();
		actions.put(action.getName(), action);
		action = new Store();
		actions.put(action.getName(), action);
		action = new Alert();
		actions.put(action.getName(), action);

		ConditionIfc condition = new Deliver();

		conditions.put(condition.getName(), condition);
		condition = new ExpireAt();
		conditions.put(condition.getName(), condition);
		condition = new MatchResource();
		conditions.put(condition.getName(), condition);
		for (ActionIfc a : actions.values()) {
			Map<String, Object> d = a.getDefaults(params);

			if (d != null) {
				defs.putAll(d);
			}
		}

		Map<String,Object> d = broadcast.getDefaults(params);
		if (d != null) {
			defs.putAll(d);
		}
		
		return defs;
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

		if ((jid != null) &&
				(getName().equals(jid.getLocalpart()) || isLocalDomain(jid.toString())) &&
				(AMP_NODE.equals(node))) {
			if (query == null) {
				query = new Element("query");
				query.setXMLNS(XMPPService.INFO_XMLNS);
			}
			query.addChild(new Element("identity", new String[] { "name", "category", "type" },
																 new String[] { getDiscoDescription(),
							"im", getDiscoCategoryType() }));
			query.addChild(top_feature);
			for (ActionIfc action : actions.values()) {
				query.addChild(new Element("feature", new String[] { "var" },
																	 new String[] {
																		 AMP_NODE + "?action=" + action.getName() }));
			}
			for (ConditionIfc cond : conditions.values()) {
				query.addChild(new Element("feature", new String[] { "var" },
																	 new String[] {
																		 AMP_NODE + "?condition=" + cond.getName() }));
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
			log.finest("Found disco info: " + ((query != null)
																				 ? query.toString()
																				 : null));
		}

		return query;
	}

	//~--- methods --------------------------------------------------------------

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
		Element amp      = packet.getElement().getChild("amp", AMP_XMLNS);

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

	//~--- set methods ----------------------------------------------------------

	@Override
	public void setProperties(Map<String, Object> props) throws ConfigurationException {
		super.setProperties(props);
		if (props.size() == 1) {

			// If props.size() == 1, it means this is a single property update
			// and this component does not support single property change for the rest
			// of it's settings
			return;
		}
		for (ActionIfc a : actions.values()) {
			a.setProperties(props, this);
		}

		broadcast.setProperties(props, this);
		// for (ConditionIfc c : conditions.values()) {
		// c.setProperties(props, this);
		// }
	}

	//~--- methods --------------------------------------------------------------

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
				boolean result = condition.match(packet, rule);;

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



// ~ Formatted in Sun Code Convention

// ~ Formatted by Jindent --- http://www.jindent.com


//~ Formatted in Tigase Code Convention on 13/02/20
