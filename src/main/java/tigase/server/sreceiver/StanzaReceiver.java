/*  Tigase Project
 *  Copyright (C) 2001-2007
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
package tigase.server.sreceiver;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.conf.Configurable;
import tigase.disco.ServiceEntity;
import tigase.disco.ServiceIdentity;
import tigase.disco.XMPPService;
import tigase.server.AbstractMessageReceiver;
import tigase.server.Packet;
import tigase.server.ServerComponent;
import tigase.util.ClassUtil;
import tigase.util.JID;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;

import static tigase.server.sreceiver.ReceiverTaskIfc.*;

/**
 * This is a sibbling of <code>StanzaSender</code> class and offers just
 * an opposite functionaity. It can receive XMPP packets to do something
 * with them. And what to do it depends on the destination address.
 * Destination address points to certain receiver task and this is up
 * to task to decide what to do with the stanza. Task address is just a
 * usuall Jabber ID: <strong>task-short-name@srec.tigase.org</strong>.
 * <p>
 * Like public chat rooms in <strong>MUC</strong> tasks can be preconfigured
 * in the <code>StanzaReceiver</code> configuration or can be created on
 * demand using <code>ad-hoc</code> commands and <code>service-discovery</code>.
 * User can subscribe to some tasks and can add them to the roster just like
 * a normal contacts. This allows to use the functionality from all existing
 * clients without implementing any special protocols or extensions.
 * </p>
 * <p>
 * Possible tasks are:
 * </p>
 * <ul>
 * <li><strong>Interests groups</strong> - simple stanza (message)
 * distribution to a group of interested ppl. To receive this information
 * user has to subscribe to the task first. It is a bit like a
 * <strong>MUC</strong> without a chat room or like a mailing list.</li>
 * <li><strong>Persistent storage</strong> - which is like an archaive of
 * some information for group of ppl.</li>
 * <li><strong>Web page publishing</strong> - this might be useful for
 * web sites with kind of <em>Short, instant news board</em> where selected
 * users can send information and they are published instantly.
 * (Through the database for example.)</li>
 * <li><strong>Forums integration</strong> - this might be useful for kind
 * of forums where you can post messages from Web site as well as from your
 * Jabber client.</li>
 * </ul>
 * <p>
 * <strong>Task creation parameters:</strong><br/>
 * <ul>
 * <li><strong>Task short name</strong> - the nick name of the task which
 * is used to create Jabber ID for the task.</li>
 * <li><strong>Task description</strong> - description of the task what is
 * the purpose of the task.</li>
 * <li><strong>Task type</strong> - the server, through <code>ad-hoc</code>
 * commands should present available tasks types which can be created.
 * User can select a task to create. There may be some restrictions on
 * tasks creation like certain types can be created only by server
 * administrator or some tasks types can be created only once on single
 * server and so on.</li>
 * <li><strong>Subscrption list</strong> - server admin should be able to add
 * list of users who might be interested in subscription to the task. After task
 * is created <code>subscrive</code> presence is sent to these users and they can
 * accept the subscription or not.</li>
 * <li><strong>Subscription restrictions</strong> - who may subscribe to the task
 * like: <em>public task</em> - anybody can subscribe, <em>local users only</em>
 * - users from the local server (domain) only can subscribe, <em>by regex</em>
 * - regular expresion matching JIDs, <em>moderated subscription</em> - anybody
 * may request subscription but the creator of the task must approve the
 * subscription.</li>
 * <li><strong>On line only</strong> - the task may distribute packets to online
 * users only.</li>
 * <li><strong>Replace sender address</strong> - whether sender address should
 * be replaced with task address. This might be useful depending where the
 * responses should go. If the list is kind on announces board like new version
 * release then maybe replies should go to the sender. If this is more like
 * topic discussion group then the reply should go to all subscribers.</li>
 * <li><strong>Message type</strong> - whether messages should be distributed
 * as a <code>chat</code>, <code>headline</code> or <code>normal</code>.</li>
 * <li><strong>Who can post</strong> - who can send a message for processing,
 * possible options are: <code>all</code>, <code>subscribed</code>,
 * <code>owner</code>, <code>list</code></li>
 * </ul>
 * There can be also some per task specific settings...
 * </p>
 * <p>
 * Created: Wed May  9 08:27:22 2007
 * </p>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class StanzaReceiver extends AbstractMessageReceiver
	implements Configurable, XMPPService {

	public static final String TASKS_LIST_PROP_KEY = "tasks-list";
	public static final String[] TASKS_LIST_PROP_VAL =
	{"development-news", "service-news"};
	public static final String TASK_ACTIVE_PROP_KEY = "active";
	public static final boolean TASK_ACTIVE_PROP_VAL = true;
	public static final String TASK_TYPE_PROP_KEY = "task-type";
	public static final String TASK_TYPE_PROP_VAL = "News Distribution";

	public static final Map<String, Object> DEFAULT_PROPS =
		new HashMap<String, Object>();

	static {
		//		DEFAULT_PROPS.put(SUBSCR_LIST_PROP_KEY, SUBSCR_LIST_PROP_VAL);
		DEFAULT_PROPS.put(SUBSCR_RESTRICTIONS_PROP_KEY,
			SUBSCR_RESTRICTIONS_PROP_VAL.toString());
		DEFAULT_PROPS.put(MESSAGE_TYPE_PROP_KEY,
			MESSAGE_TYPE_PROP_VAL.toString());
		DEFAULT_PROPS.put(ALLOWED_SENDERS_PROP_KEY,
			ALLOWED_SENDERS_PROP_VAL.toString());
		DEFAULT_PROPS.put(SUBSCR_RESTR_REGEX_PROP_KEY, SUBSCR_RESTR_REGEX_PROP_VAL);
		DEFAULT_PROPS.put(ONLINE_ONLY_PROP_KEY, ONLINE_ONLY_PROP_VAL);
		DEFAULT_PROPS.put(REPLACE_SENDER_PROP_KEY, REPLACE_SENDER_PROP_VAL);
		DEFAULT_PROPS.put(ALLOWED_SENDERS_LIST_PROP_KEY, ALLOWED_SENDERS_LIST_PROP_VAL);
		DEFAULT_PROPS.put(DESCRIPTION_PROP_KEY, DESCRIPTION_PROP_VAL);
	}

  private static Logger log =
		Logger.getLogger("tigase.server.sreceiver.StanzaReceiver");

	/**
	 * This maps keeps all available task types which can be instantiated
	 * by the user.
	 */
	private Map<String, ReceiverTaskIfc> task_types =
		new ConcurrentSkipListMap<String, ReceiverTaskIfc>();
	/**
	 * This map keeps all active tasks instances as pairs: (JabberID, task)
	 */
	private Map<String, ReceiverTaskIfc> task_instances =
		new ConcurrentSkipListMap<String, ReceiverTaskIfc>();

	private ServiceEntity serviceEntity = null;

	/**
	 * Describe <code>processIQPacket</code> method here.
	 *
	 * @param packet a <code>Packet</code> value
	 * @return a <code>boolean</code> value
	 */
	private boolean processIQPacket(Packet packet) {
		boolean processed = false;
		Element iq = packet.getElement();
		Element query = iq.getChild("query", INFO_XMLNS);
		Element query_rep = null;
		if (query != null && packet.getType() == StanzaType.get) {
			query_rep =
				serviceEntity.getDiscoInfo(JID.getNodeNick(packet.getElemTo()));
			processed = true;
		} // end of if (query != null && packet.getType() == StanzaType.get)
		query = iq.getChild("query", ITEMS_XMLNS);
		if (query != null && packet.getType() == StanzaType.get) {
			query_rep = query.clone();
			List<Element> items =
				serviceEntity.getDiscoItems(JID.getNodeNick(packet.getElemTo()),
					packet.getElemTo());
			if (items != null && items.size() > 0) {
				query_rep.addChildren(items);
			} // end of if (items != null && items.size() > 0)
			processed = true;
		} // end of if (query != null && packet.getType() == StanzaType.get)
		if (query_rep != null) {
			addOutPacket(packet.okResult(query_rep, 0));
		} // end of if (query_rep != null)
		return processed;
	}

	/**
	 * Describe <code>processPacket</code> method here.
	 *
	 * @param packet a <code>Packet</code> value
	 */
	public void processPacket(final Packet packet) {
		log.finest("Processing packet: " + packet.toString());
		if (packet.getElemName().equals("iq")) {
			if (processIQPacket(packet)) {
				return;
			} // end of if (processIQPacket(packet))
		} // end of if (packet.getElemName().equals("iq"))
		ReceiverTaskIfc task = task_instances.get(packet.getElemTo());
		if (task != null) {
			log.finest("Found a task for packet: " + task.getJID());
			Queue<Packet> results = new LinkedList<Packet>();
			task.processPacket(packet, results);
			for (Packet res: results) {
				addOutPacket(res);
			} // end of for (Packet res: results)
		} // end of if (task != null)
	}

	/**
	 * Describe <code>myDomain</code> method here.
	 *
	 * @return a <code>String</code> value
	 */
	private String myDomain() {
		return getName() + "." + getDefHostName();
	}

	/**
	 * Describe <code>addTaskInstance</code> method here.
	 *
	 * @param task a <code>ReceiverTaskIfc</code> value
	 */
	private void addTaskInstance(ReceiverTaskIfc task) {
		task_instances.put(task.getJID(),	task);
		ServiceEntity item = new ServiceEntity(task.getJID(),
			JID.getNodeNick(task.getJID()), task.getDescription());
		serviceEntity.addItems(item);
	}

	/**
	 * Describe <code>setProperties</code> method here.
	 *
	 * @param props a <code>Map</code> value
	 */
	public void setProperties(final Map<String, Object> props) {
		super.setProperties(props);
		addRouting(myDomain());
		serviceEntity = new ServiceEntity(getName(), null, "Stanza Receiver");
		serviceEntity.addIdentities(
			new ServiceIdentity("component", "external", "Stanza Receiver"));
		task_types.clear();
		try {
			Set<Class<ReceiverTaskIfc>> ctasks =
				ClassUtil.getClassesImplementing(ReceiverTaskIfc.class);
			for (Class<ReceiverTaskIfc> ctask: ctasks) {
				ReceiverTaskIfc itask = ctask.newInstance();
				task_types.put(itask.getType(), itask);
			} // end of for (Class<ReceiverTaskIfc> ctask: ctasks)
		} catch (Exception e) {
      log.log(Level.SEVERE, "Can not load ReceiverTaskIfc implementations", e);
		} // end of try-catch
		String[] tasks_list = (String[])props.get(TASKS_LIST_PROP_KEY);
		for (String task_name: tasks_list) {
			String task_type =
				(String)props.get(task_name + "/" + TASK_TYPE_PROP_KEY);
			ReceiverTaskIfc ttask = task_types.get(task_type);
			ReceiverTaskIfc new_task = ttask.getInstance();
			new_task.setJID(task_name + "@" + myDomain());
			Map<String, Object> task_params = new HashMap<String, Object>();
			String prep = task_name + "/props/";
			for (Map.Entry<String, Object> entry: props.entrySet()) {
				if (entry.getKey().startsWith(prep)) {
					task_params.put(entry.getKey().substring(prep.length()),
						entry.getValue());
				} // end of if (entry.getKey().startsWith())
			} // end of for (Map.Entry entry: props.entrySet())
			new_task.setParams(task_params);
			addTaskInstance(new_task);
		} // end of for (String task_name: tasks_list)
	}

	public Map<String, Object> getDefaults(final Map<String, Object> params) {
		Map<String, Object> defs = super.getDefaults(params);
		defs.put(TASKS_LIST_PROP_KEY, TASKS_LIST_PROP_VAL);
		for (String task_name: TASKS_LIST_PROP_VAL) {
			defs.put(task_name + "/" + TASK_ACTIVE_PROP_KEY, TASK_ACTIVE_PROP_VAL);
			defs.put(task_name + "/" + TASK_TYPE_PROP_KEY, TASK_TYPE_PROP_VAL);
			for (Map.Entry entry: DEFAULT_PROPS.entrySet()) {
				defs.put(task_name + "/props/" + entry.getKey(), entry.getValue());
			} // end of for ()
		} // end of for (String task_name: TASKS_LIST_PROP_VAL)
		return defs;
	}

	/**
	 * Describe <code>getDiscoInfo</code> method here.
	 *
	 * @param node a <code>String</code> value
	 * @param jid a <code>String</code> value
	 * @return an <code>Element</code> value
	 */
	public Element getDiscoInfo(String node, String jid) {
		if (jid != null && JID.getNodeHost(jid).startsWith(getName()+".")) {
			return serviceEntity.getDiscoInfo(node);
		}
		return null;
	}

	public 	List<Element> getDiscoFeatures() { return null; }

	public List<Element> getDiscoItems(String node, String jid) {
		if (JID.getNodeHost(jid).startsWith(getName()+".")) {
			return serviceEntity.getDiscoItems(node, null);
		} else {
 			return Arrays.asList(serviceEntity.getDiscoItem(null, getName() + "." + jid));
		}
	}

} // StanzaReceiver
