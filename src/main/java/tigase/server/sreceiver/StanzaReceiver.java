/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.server.sreceiver;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.RepositoryFactory;
import tigase.db.TigaseDBException;
import tigase.db.UserExistsException;
import tigase.db.UserRepository;

import tigase.server.AbstractMessageReceiver;
import tigase.server.Command;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.sreceiver.PropertyConstants.MessageType;
import tigase.server.sreceiver.PropertyConstants.SenderAddress;
import tigase.server.sreceiver.PropertyConstants.SenderRestrictions;

import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

import tigase.conf.Configurable;
import tigase.conf.ConfigurationException;
import tigase.disco.ServiceEntity;
import tigase.disco.ServiceIdentity;
import tigase.disco.XMPPService;
import tigase.stats.StatisticsList;
import tigase.util.ClassUtil;
import tigase.util.DNSResolverFactory;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.server.sreceiver.PropertyConstants.*;

//~--- classes ----------------------------------------------------------------

/**
 * This is a sibling to <code>StanzaSender</code> class and offers just an
 * opposite functionality. It can receive XMPP packets to do something with
 * them. And what to do it depends on the destination address. Destination
 * address points to certain receiver task and this is up to task to decide what
 * to do with the stanza. Task address is just a usual Jabber ID:
 * <strong>task-short-name@srec.tigase.org</strong>.
 * <p>
 * Like public chat rooms in <strong>MUC</strong> tasks can be preconfigured in
 * the <code>StanzaReceiver</code> configuration or can be created on demand
 * using <code>ad-hoc</code> commands and <code>service-discovery</code>. User
 * can subscribe to some tasks and can add them to the roster just like a normal
 * contacts. This allows to use the functionality from all existing clients
 * without implementing any special protocols or extensions.
 * </p>
 * <p>
 * Possible tasks are:
 * </p>
 * <ul>
 * <li><strong>Interests groups</strong> - simple stanza (message) distribution
 * to a group of interested ppl. To receive this information user has to
 * subscribe to the task first. It is a bit like a <strong>MUC</strong> without
 * a chat room or like a mailing list.</li>
 * <li><strong>Persistent storage</strong> - which is like an archive of some
 * information for group of ppl.</li>
 * <li><strong>Web page publishing</strong> - this might be useful for web sites
 * with kind of <em>Short, instant news board</em> where selected users can send
 * information and they are published instantly. (Through the database for
 * example.)</li>
 * <li><strong>Forums integration</strong> - this might be useful for kind of
 * forums where you can post messages from Web site as well as from your Jabber
 * client.</li>
 * </ul>
 * <br>
 * <strong>Task creation parameters:</strong><br>
 * <ul>
 * <li><strong>Task short name</strong> - the nick name of the task which is
 * used to create Jabber ID for the task.</li>
 * <li><strong>Task description</strong> - description of the task what is the
 * purpose of the task.</li>
 * <li><strong>Task type</strong> - the server, through <code>ad-hoc</code>
 * commands should present available tasks types which can be created. User can
 * select a task to create. There may be some restrictions on tasks creation
 * like certain types can be created only by server administrator or some tasks
 * types can be created only once on single server and so on.</li>
 * <li><strong>Subscription list</strong> - server admin should be able to add
 * list of users who might be interested in subscription to the task. After task
 * is created <code>subscribe</code> presence is sent to these users and they
 * can accept the subscription or not.</li>
 * <li><strong>Subscription restrictions</strong> - who may subscribe to the
 * task like: <em>public task</em> - anybody can subscribe,
 * <em>local users only</em> - users from the local server (domain) only can
 * subscribe, <em>by regex</em> - regular expression matching JIDs,
 * <em>moderated subscription</em> - anybody may request subscription but the
 * creator of the task must approve the subscription.</li>
 * <li><strong>On line only</strong> - the task may distribute packets to online
 * users only.</li>
 * <li><strong>Replace sender address</strong> - whether sender address should
 * be replaced with task address. This might be useful depending where the
 * responses should go. If the list is kind on announces board like new version
 * release then maybe replies should go to the sender. If this is more like
 * topic discussion group then the reply should go to all subscribers.</li>
 * <li><strong>Message type</strong> - whether messages should be distributed as
 * a <code>chat</code>, <code>headline</code> or <code>normal</code>.</li>
 * <li><strong>Who can post</strong> - who can send a message for processing,
 * possible options are: <code>all</code>, <code>subscribed</code>,
 * <code>owner</code>, <code>list</code></li>
 * </ul>
 * There can be also some per task specific settings...
 * <br>
 * Created: Wed May 9 08:27:22 2007
 * 
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class StanzaReceiver extends AbstractMessageReceiver implements Configurable,
		XMPPService, StanzaReceiverIfc {
	private static String[] ADMINS_PROP_VAL = { "admin@localhost", "admin@hostname" };
	private static final int CREATION_MAX_NO_PROP_VAL = 1;

	// private static final String LOCAL_DOMAINS_PROP_KEY = "local-domains";
	// private static String[] LOCAL_DOMAINS_PROP_VAL = {"localhost"};

	/** Field description */
	public static final String MY_DOMAIN_NAME_PROP_KEY = "domain-name";

	/** Field description */
	public static final String MY_DOMAIN_NAME_PROP_VAL = "srecv.localhost";

	// public static final String SIMPLE_JID_PROP_KEY = "simple-jid";
	// public static final String SIMPLE_JID_PROP_VAL = "srecv@localhost";
	private static final String SREC_REPO_CLASS_PROP_KEY = "srec-repo-class";
	private static final String SREC_REPO_URL_PROP_KEY = "srec-repo-url";
	private static final String TASKS_LIST_PROP_KEY = "tasks-list";
	private static final String[] TASKS_LIST_PROP_VAL = {};
	private static final String TASK_ACTIVE_PROP_KEY = "active";
	private static final String TASK_TYPES_PROP_NODE = "task-types/";
	private static final String TASK_TYPE_PROP_KEY = "task-type";
	private static final String TASK_TYPE_PROP_VAL = "News Distribution";
	private static final String TESTER_TASK_NAME = "tester_1";
	private static final String TESTER_TASK_TYPE = "Tester Task";
	private static final Logger log = Logger
			.getLogger("tigase.server.sreceiver.StanzaReceiver");
	private static final String[] TASK_TYPES_PROP_VAL = { TASK_TYPE_PROP_VAL };
	private static final String TASK_TYPES_PROP_KEY = TASK_TYPES_PROP_NODE + "list";
	private static final boolean TASK_ACTIVE_PROP_VAL = true;
	private static final TaskCreationPolicy CREATION_POLICY_PROP_VAL =
			TaskCreationPolicy.ADMIN;
	private static final String CREATION_POLICY_PROP_KEY = TASK_TYPES_PROP_NODE
			+ "default-policy";
	private static final String CREATION_MAX_NO_PROP_KEY = TASK_TYPES_PROP_NODE
			+ "default-max-number";
	private static final String params_node = "/params";
	private static final String task_type_key = "task-type";
	private static final String tasks_node = "/tasks";

	// ~--- fields ---------------------------------------------------------------

	private String[] admins = { "admin@localhost" };

	// private String simpleJid = null;
	private UserRepository repository = null;

	// /**
	// * Variable <code>defaultPolicy</code> specifies default task creation
	// policy.
	// * In other words who can create task. This is default setting for task
	// types
	// * not specified separately in configuration file.
	// */
	// private TaskCreationPolicy defaultPolicy = CREATION_POLICY_PROP_VAL;
	private ServiceEntity serviceEntity = null;

	/**
	 * This maps keeps all available task types which can be instantiated by the
	 * user.
	 */
	private Map<String, TaskType> task_types = new ConcurrentHashMap<String, TaskType>();

	/**
	 * This map keeps all active tasks instances as pairs: (JabberID, task)
	 */
	private Map<String, ReceiverTaskIfc> task_instances =
			new ConcurrentHashMap<String, ReceiverTaskIfc>();

	// private Set<String> local_domains = new HashSet<String>();
	private String my_hostname = MY_DOMAIN_NAME_PROP_VAL;

	/**
	 * Variable <code>defaultMaxTasksNo</code> specifies default max number of
	 * tasks for a task type. This is default setting for task types not specified
	 * separately in configuration file.
	 */
	private int defaultMaxTasksNo = CREATION_MAX_NO_PROP_VAL;
	private Map<String, TaskCommandIfc> commands =
			new ConcurrentHashMap<String, TaskCommandIfc>();

	// ~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 * 
	 */
	public StanzaReceiver() {
		try {
			Set<Class<ReceiverTaskIfc>> ctasks =
					ClassUtil.getClassesImplementing(ReceiverTaskIfc.class);

			for (Class<ReceiverTaskIfc> ctask : ctasks) {
				ReceiverTaskIfc itask = ctask.newInstance();

				task_types.put(itask.getType(), new TaskType(itask));
			} // end of for (Class<ReceiverTaskIfc> ctask: ctasks)
		} catch (Exception e) {
			log.log(Level.SEVERE, "Can not load ReceiverTaskIfc implementations", e);
		} // end of try-catch

		TaskCommandIfc new_task = new NewTaskCommand();

		commands.put(new_task.getNodeName(), new_task);
		new_task = new TaskInstanceCommand();
		commands.put(new_task.getNodeName(), new_task);
	}

	// ~--- methods --------------------------------------------------------------

	@Override
	public boolean addOutPacket(Packet packet) {
		return super.addOutPacket(packet);
	}

	// ~--- get methods ----------------------------------------------------------

	@Override
	public Map<String, Object> getDefaults(final Map<String, Object> params) {
		Map<String, Object> defs = super.getDefaults(params);
		List<String> conf_tasks = new LinkedList<String>();

		conf_tasks.addAll(Arrays.asList(TASKS_LIST_PROP_VAL));

		if (params.get(GEN_CONF + "ssend-forum-task") != null) {
			String[] forum_ids =
					((String) params.get(GEN_CONF + "ssend-forum-task")).split(",");

			for (String id : forum_ids) {
				String task_name = "forum-" + id;

				conf_tasks.add(task_name);
				defs.put(task_name + "/" + TASK_ACTIVE_PROP_KEY, true);
				defs.put(task_name + "/" + TASK_TYPE_PROP_KEY, TASK_TYPE_PROP_VAL);

				Map<String, PropertyItem> default_props =
						task_types.get(TASK_TYPE_PROP_VAL).getTaskType().getDefaultParams();

				for (Map.Entry<String, PropertyItem> entry : default_props.entrySet()) {
					defs.put(task_name + "/props/" + entry.getKey(), entry.getValue().toString());

					if (entry.getKey().equals(ALLOWED_SENDERS_PROP_KEY)) {
						defs.put(task_name + "/props/" + entry.getKey(),
								SenderRestrictions.OWNER.toString());
					}

					if (entry.getKey().equals(MESSAGE_TYPE_PROP_KEY)) {
						defs.put(task_name + "/props/" + entry.getKey(),
								MessageType.NORMAL.toString());
					}

					if (entry.getKey().equals(REPLACE_SENDER_PROP_KEY)) {
						defs.put(task_name + "/props/" + entry.getKey(),
								SenderAddress.REMOVE.toString());
					}

					if (entry.getKey().equals(TASK_OWNER_PROP_KEY)) {
						defs.put(task_name + "/props/" + entry.getKey(), "drupal-forum-" + id + "@"
								+ my_hostname);
					}
				} // end of for ()
			}
		}

		for (String task_name : TASKS_LIST_PROP_VAL) {
			defs.put(task_name + "/" + TASK_ACTIVE_PROP_KEY, TASK_ACTIVE_PROP_VAL);
			defs.put(task_name + "/" + TASK_TYPE_PROP_KEY, TASK_TYPE_PROP_VAL);

			Map<String, PropertyItem> default_props =
					task_types.get(TASK_TYPE_PROP_VAL).getTaskType().getDefaultParams();

			for (Map.Entry<String, PropertyItem> entry : default_props.entrySet()) {
				defs.put(task_name + "/props/" + entry.getKey(), entry.getValue().toString());
			} // end of for ()
		} // end of for (String task_name: TASKS_LIST_PROP_VAL)

		if ((Boolean) params.get(GEN_TEST)) {
			log.config("Activating tester task....");
			conf_tasks.add(TESTER_TASK_NAME);
			defs.put(TESTER_TASK_NAME + "/" + TASK_ACTIVE_PROP_KEY, true);
			defs.put(TESTER_TASK_NAME + "/" + TASK_TYPE_PROP_KEY, TESTER_TASK_TYPE);
			defs.put(TESTER_TASK_NAME + "/props/" + ALLOWED_SENDERS_PROP_KEY,
					SenderRestrictions.ALL.toString());
			defs.put(TESTER_TASK_NAME + "/props/" + MESSAGE_TYPE_PROP_KEY,
					MessageType.NORMAL.toString());
		}

		defs.put(TASKS_LIST_PROP_KEY, conf_tasks.toArray(new String[0]));

		String srec_repo_uri = null;

		if (params.get(GEN_SREC_DB_URI) != null) {
			srec_repo_uri = (String) params.get(GEN_SREC_DB_URI);
		} else {
			if (params.get(RepositoryFactory.GEN_USER_DB_URI) != null) {
				srec_repo_uri = (String) params.get(RepositoryFactory.GEN_USER_DB_URI);
			} // end of if (params.get(GEN_USER_DB_URI) != null)
		} // end of else

		defs.put(SREC_REPO_URL_PROP_KEY, srec_repo_uri);

		if (params.get(GEN_SREC_ADMINS) != null) {
			ADMINS_PROP_VAL = ((String) params.get(GEN_SREC_ADMINS)).split(",");
		} else {
			if (params.get(GEN_ADMINS) != null) {
				ADMINS_PROP_VAL = ((String) params.get(GEN_ADMINS)).split(",");
			} else {
				ADMINS_PROP_VAL = new String[1];
				ADMINS_PROP_VAL[0] = "admin@" + getDefHostName();
			}
		} // end of if (params.get(GEN_SREC_ADMINS) != null) else

		defs.put(ADMINS_PROP_KEY, ADMINS_PROP_VAL);

		String[] local_domains = DNSResolverFactory.getInstance().getDefaultHosts();

		if (params.get(GEN_VIRT_HOSTS) != null) {
			local_domains = ((String) params.get(GEN_VIRT_HOSTS)).split(",");
		}

		// defs.put(LOCAL_DOMAINS_PROP_KEY, LOCAL_DOMAINS_PROP_VAL);
		defs.put(MY_DOMAIN_NAME_PROP_KEY, "srecv." + local_domains[0]);

		// defs.put(SIMPLE_JID_PROP_KEY, "srecv@" + local_domains[0]);
		defs.put(TASK_TYPES_PROP_KEY, TASK_TYPES_PROP_VAL);
		defs.put(CREATION_POLICY_PROP_KEY, CREATION_POLICY_PROP_VAL.toString());
		defs.put(CREATION_MAX_NO_PROP_KEY, CREATION_MAX_NO_PROP_VAL);
		defs.put(TASK_TYPE_PROP_VAL + "/" + CREATION_POLICY_PROP_KEY,
				TaskCreationPolicy.ALL.toString());
		defs.put(TASK_TYPE_PROP_VAL + "/" + CREATION_MAX_NO_PROP_KEY, 100);

		return defs;
	}

	@Override
	public List<Element> getDiscoFeatures(JID from) {
		return null;
	}

	@Override
	public Element getDiscoInfo(String node, JID jid, JID from) {
		if ((jid != null) && jid.toString().startsWith(getName() + ".")) {
			return serviceEntity.getDiscoInfo(node);
		}

		return null;
	}

	@Override
	public List<Element> getDiscoItems(String node, JID jid, JID from) {
		if (jid.toString().startsWith(getName() + ".")) {
			return serviceEntity.getDiscoItems(node, null);
		} else {
			if (node == null) {
				return Arrays.asList(serviceEntity.getDiscoItem(null, getName() + "." + jid));
			} else {
				return null;
			}
		}
	}

	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);

		for (ReceiverTaskIfc task : task_instances.values()) {
			task.getStatistics(list);
		}
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param jidOrName
	 * 
	 * 
	 */
	public ReceiverTaskIfc getTask(String jidOrName) {
		if (jidOrName == null) {
			return null;
		}

		ReceiverTaskIfc task = task_instances.get(jidOrName.toLowerCase());

		if (task == null) {
			task = task_instances.get(jidOrName + "@" + my_hostname);
		}

		return task;
	}

	@Override
	public boolean isAdmin(JID jid) {
		log.log(Level.INFO, "Looking for JID: {0} in admins array: {1}", new Object[] {
				jid.getBareJID().toString(), Arrays.toString(admins) });
		return Arrays.binarySearch(admins, jid.getBareJID().toString()) >= 0;
	}

	// ~--- methods --------------------------------------------------------------

	@Override
	public void processPacket(final Packet packet) {
		if (packet.isCommand()) {
			Iq iqc = (Iq) packet;
			Command.Action action = Command.getAction(iqc);

			if (action == Command.Action.cancel) {
				Packet result = iqc.commandResult(null);

				addOutPacket(result);

				return;
			}

			// Packet result = packet.commandResult("result");
			// TaskCommandIfc command = commands.get("*");
			// if (command != null) {
			// command.processCommand(packet, result, this);
			// } // end of if (command != null)
			// addOutPacket(result);
			// return;
			Packet result = iqc.commandResult(Command.DataType.result);
			String str_command = iqc.getStrCommand();

			if (str_command != null) {
				String[] arr_command = str_command.split("/");

				if (arr_command.length > 1) {
					TaskCommandIfc command = commands.get(arr_command[1]);

					if (command != null) {
						log.fine("Processing task command: " + arr_command[1]);
						command.processCommand(packet, result, this);
					} // end of if (command != null)
				} // end of if (arr_command.length > 1)
			} // end of if (str_command != null)

			addOutPacket(result);

			return;
		}

		if (log.isLoggable(Level.FINEST)) {
			log.finest("Processing packet: " + packet.toString());
		}

		if (packet.getElemName().equals("iq")) {
			processIQPacket(packet);

			return;
		} // end of if (packet.getElemName().equals("iq"))

		ReceiverTaskIfc task = getTask(packet.getStanzaTo().toString());

		if (task == null) {
			task = getTask(packet.getStanzaTo().getLocalpart());
		}

		if (task == null) {
			String resource = packet.getStanzaTo().getResource();

			if (resource != null) {
				task = getTask(resource);
			}
		}

		if (task != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Found a task for packet: " + task.getJID());
			}

			Queue<Packet> results = new ArrayDeque<Packet>();

			task.processPacket(packet, results);
			addOutPackets(results);
		} // end of if (task != null)
	}

	// ~--- set methods ----------------------------------------------------------

	@Override
	public void setProperties(final Map<String, Object> props) throws ConfigurationException {
		super.setProperties(props);
		if (props.size() == 1) {
			// If props.size() == 1, it means this is a single property update 
			// and this component does not support single property change for the rest
			// of it's settings
			return;
		}
		my_hostname = (String) props.get(MY_DOMAIN_NAME_PROP_KEY);

		// simpleJid = (String)props.get(SIMPLE_JID_PROP_KEY);
		serviceEntity = new ServiceEntity(getName(), null, "Stanza Receiver");
		serviceEntity.addIdentities(new ServiceIdentity("component", "generic",
				"Stanza Receiver"));
		serviceEntity.addFeatures(DEF_FEATURES);

		ServiceEntity com =
				new ServiceEntity(my_hostname, "commands", "Tasks management commands");

		com.addFeatures(DEF_FEATURES);
		com.addIdentities(new ServiceIdentity("automation", "command-list",
				"Tasks management commands"));
		serviceEntity.addItems(com);

		for (TaskCommandIfc comm : commands.values()) {
			ServiceEntity item =
					new ServiceEntity(my_hostname, comm.getNodeName(), comm.getDescription());

			item.addFeatures(CMD_FEATURES);
			item.addIdentities(new ServiceIdentity("automation", "command-node", comm
					.getDescription()));
			com.addItems(item);
		} // end of for (TaskCommandIfc comm: commands.values())

		admins = (String[]) props.get(ADMINS_PROP_KEY);
		Arrays.sort(admins);

		// addRouting(my_hostname);
		try {
			String cls_name = (String) props.get(SREC_REPO_CLASS_PROP_KEY);
			String res_uri = (String) props.get(SREC_REPO_URL_PROP_KEY);

			// if (!res_uri.contains("autoCreateUser=true")) {
			// res_uri += "&autoCreateUser=true";
			// } // end of if (!res_uri.contains("autoCreateUser=true"))
			repository = RepositoryFactory.getUserRepository(cls_name, res_uri, null);

			try {
				repository.addUser(getComponentId().getBareJID());
			} catch (UserExistsException e) { /* Ignore, this is correct and expected */
			}

			loadTasksFromRepository();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Can't initialize repository: ", e);
		} // end of try-catch

		String[] tasks_list = (String[]) props.get(TASKS_LIST_PROP_KEY);

		if ((tasks_list != null) && (tasks_list.length > 0)) {
			for (String task_name : tasks_list) {
				String task_type = (String) props.get(task_name + "/" + TASK_TYPE_PROP_KEY);
				Map<String, Object> task_params = new LinkedHashMap<String, Object>();
				String prep = task_name + "/props/";

				for (Map.Entry<String, Object> entry : props.entrySet()) {
					if (entry.getKey().startsWith(prep)) {
						task_params.put(entry.getKey().substring(prep.length()), entry.getValue());
					} // end of if (entry.getKey().startsWith())
				} // end of for (Map.Entry entry: props.entrySet())

				try {
					addTaskInstance(createTask(task_type,
							JID.jidInstance(task_name + "@" + my_hostname), task_params));
				} catch (TigaseStringprepException ex) {
					Logger.getLogger(StanzaReceiver.class.getName()).log(Level.SEVERE, null, ex);
				}
			} // end of for (String task_name: tasks_list)
		}

		// defaultPolicy =
		// TaskCreationPolicy.valueOf((String)props.get(CREATION_POLICY_PROP_KEY));
		defaultMaxTasksNo = (Integer) props.get(CREATION_MAX_NO_PROP_KEY);

		String[] task_types_arr = (String[]) props.get(TASK_TYPES_PROP_KEY);

		for (String task_t : task_types_arr) {
			TaskType tt = task_types.get(task_t);

			if (tt != null) {
				String policy_str = (String) props.get(task_t + "/" + CREATION_POLICY_PROP_KEY);
				TaskCreationPolicy policy = TaskCreationPolicy.valueOf(policy_str);

				tt.setCreationPolicy(policy);

				int max_inst = (Integer) props.get(task_t + "/" + CREATION_MAX_NO_PROP_KEY);

				tt.setMaxInstancesNo(max_inst);
			}
		}

		// local_domains =
		// new
		// HashSet<String>(Arrays.asList((String[])props.get(LOCAL_DOMAINS_PROP_KEY)));
	}

	// ~--- methods --------------------------------------------------------------

	protected void addTaskInstance(String task_type, String task_name,
			Map<String, Object> task_params) throws TigaseStringprepException {
		addTaskInstance(createTask(task_type, JID.jidInstance(task_name + "@" + my_hostname),
				task_params));
	}

	/**
	 * Describe <code>addTaskInstance</code> method here.
	 * 
	 * @param task
	 *          a <code>ReceiverTaskIfc</code> value
	 */
	protected void addTaskInstance(ReceiverTaskIfc task) {
		if (getTask(task.getJID().toString()) == null) {
			addTaskToInstances(task);

			try {
				saveTaskToRepository(task);
			} catch (TigaseDBException e) {
				log.log(Level.SEVERE, "Problem with saving task to repository: " + task.getJID(),
						e);
			} // end of try-catch
		} else {

			// Exception exep = new Exception("Attempt to add another task with jid: "
			// +
			// task.getJID() + ", ignoring.");
			// exep.fillInStackTrace();
			log.log(Level.INFO, "Attempt to add another task with jid: " + task.getJID()
					+ ", ignoring.");
		} // end of else
	}

	// ~--- get methods ----------------------------------------------------------

	protected Map<String, ReceiverTaskIfc> getTaskInstances() {
		return task_instances;
	}

	protected Map<String, TaskType> getTaskTypes() {
		return task_types;
	}

	// public String getSimpleJid() {
	// return simpleJid;
	// }
	protected boolean isAllowedCreate(JID jid, String task_type) {
		TaskType tt = task_types.get(task_type);

		switch (tt.getCreationPolicy()) {
			case ADMIN:
				return isAdmin(jid);

			case LOCAL:
				return isLocalDomain(jid.getDomain());

			default:
				break;
		}

		return true;
	}

	// ~--- methods --------------------------------------------------------------

	protected void removeTaskInstance(ReceiverTaskIfc task) {
		ServiceEntity item =
				new ServiceEntity(task.getJID().toString(), task.getJID().getLocalpart(),
						task.getDescription());

		serviceEntity.removeItems(item);
		task_instances.remove(task.getJID().toString());

		Queue<Packet> results = new ArrayDeque<Packet>();

		task.destroy(results);
		addOutPackets(results);
		task_types.get(task.getType()).instanceRemoved();

		try {
			String repo_node = tasks_node + "/" + task.getJID();

			repository.removeSubnode(getComponentId().getBareJID(), repo_node);
		} catch (TigaseDBException e) {
			log.log(Level.SEVERE, "Problem removing task from repository: " + task.getJID(), e);
		} // end of try-catch
	}

	protected void removeTaskSubscribers(ReceiverTaskIfc task, JID... subscr) {
		Queue<Packet> results = new ArrayDeque<Packet>();

		task.removeSubscribers(results, subscr);
		addOutPackets(results);
	}

	protected void saveTaskToRepository(ReceiverTaskIfc task) throws TigaseDBException {
		String repo_node = tasks_node + "/" + task.getJID();

		repository.setData(getComponentId().getBareJID(), repo_node, task_type_key,
				task.getType());

		Map<String, PropertyItem> task_params = task.getParams();

		repo_node += params_node;

		for (Map.Entry<String, PropertyItem> entry : task_params.entrySet()) {
			if (!entry.getKey().equals(USER_REPOSITORY_PROP_KEY)) {
				if (entry.getValue().getValue().getClass().isArray()) {
					repository.setDataList(getComponentId().getBareJID(), repo_node,
							entry.getKey(), (String[]) entry.getValue().getValue());
					log.info("Saving task data list: " + entry.getKey() + ", value: "
							+ Arrays.toString((String[]) entry.getValue().getValue()));
				} else {
					repository.setData(getComponentId().getBareJID(), repo_node, entry.getKey(),
							entry.getValue().toString());
					log.info("Saving task data: " + entry.getKey() + ", value: "
							+ entry.getValue().toString());
				}
			} // end of if (!entry.getKey().equals(USER_REPOSITORY_PROP_KEY))
		}
	}

	private void addTaskToInstances(ReceiverTaskIfc task) {
		task_instances.put(task.getJID().toString(), task);

		ServiceEntity item =
				new ServiceEntity(task.getJID().toString(), task.getJID().getLocalpart(),
						task.getDescription());

		item.addIdentities(new ServiceIdentity("component", "generic", task.getJID()
				.toString()));
		item.addFeatures(CMD_FEATURES);
		serviceEntity.addItems(item);

		Queue<Packet> results = new ArrayDeque<Packet>();

		task.init(results);
		addOutPackets(results);
		task_types.get(task.getType()).instanceAdded();
	}

	private ReceiverTaskIfc createTask(String task_type, JID task_jid,
			Map<String, Object> task_params) {

		// ReceiverTaskIfc ttask = task_types.get(task_type);
		ReceiverTaskIfc ntask = task_types.get(task_type).getTaskInstance();

		ntask.setStanzaReceiver(this);
		ntask.setJID(task_jid);
		task_params.put(USER_REPOSITORY_PROP_KEY, repository);
		ntask.setParams(task_params);

		return ntask;
	}

	private void loadTasksFromRepository() throws TigaseDBException {
		String[] tasks_jids =
				repository.getSubnodes(getComponentId().getBareJID(), tasks_node);

		if (tasks_jids != null) {
			for (String task_jid : tasks_jids) {
				StringBuilder repo_node = new StringBuilder(tasks_node + "/" + task_jid);
				String task_type =
						repository.getData(getComponentId().getBareJID(), repo_node.toString(),
								task_type_key);

				repo_node.append(params_node);

				String[] keys =
						repository.getKeys(getComponentId().getBareJID(), repo_node.toString());
				Map<String, Object> task_params = new LinkedHashMap<String, Object>();

				if (keys != null) {
					for (String key : keys) {
						String[] vals =
								repository.getDataList(getComponentId().getBareJID(),
										repo_node.toString(), key);

						if (vals.length == 1) {
							task_params.put(key, vals[0]);
							log.info("Loaded data: " + key + ", value: " + vals[0]);
						} else {
							task_params.put(key, vals);
							log.info("Loaded data list: " + key + ", value: " + Arrays.toString(vals));
						}
					} // end of for (String key: keys)
				} // end of if (keys != null)

				try {
					addTaskToInstances(createTask(task_type, JID.jidInstance(task_jid), task_params));
				} catch (Exception e) {
					log.log(Level.WARNING, "Can't create task: " + task_jid, e);
				}
			} // end of for (String task_jid: tasks_jids)
		} // end of if (tasks_jids != null)
	}

	/**
	 * Describe <code>processIQPacket</code> method here.
	 * 
	 * @param packet
	 *          a <code>Packet</code> value
	 * @return a <code>boolean</code> value
	 */
	private boolean processIQPacket(Packet packet) {
		boolean processed = false;
		Element iq = packet.getElement();
		Element query = iq.getChild("query", INFO_XMLNS);
		Element query_rep = null;

		if ((query != null) && (packet.getType() == StanzaType.get)) {
			query_rep = serviceEntity.getDiscoInfo(packet.getStanzaTo().getLocalpart());
			processed = true;
		} // end of if (query != null && packet.getType() == StanzaType.get)

		query = iq.getChild("query", ITEMS_XMLNS);

		if ((query != null) && (packet.getType() == StanzaType.get)) {
			query_rep = query.clone();

			List<Element> items =
					serviceEntity.getDiscoItems(packet.getStanzaTo().getLocalpart(), packet
							.getStanzaTo().toString());

			if ((items != null) && (items.size() > 0)) {
				query_rep.addChildren(items);
			} // end of if (items != null && items.size() > 0)

			processed = true;
		} // end of if (query != null && packet.getType() == StanzaType.get)

		if (query_rep != null) {
			addOutPacket(packet.okResult(query_rep, 0));
		} // end of if (query_rep != null)

		return processed;
	}
} // StanzaReceiver

// ~ Formatted in Sun Code Convention

// ~ Formatted by Jindent --- http://www.jindent.com
