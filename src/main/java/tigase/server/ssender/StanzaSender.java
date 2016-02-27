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
package tigase.server.ssender;

import tigase.db.RepositoryFactory;

import tigase.server.AbstractMessageReceiver;
import tigase.server.Packet;

import tigase.xmpp.JID;

import tigase.conf.Configurable;
import tigase.conf.ConfigurationException;
import tigase.util.DNSResolverFactory;
import tigase.util.TigaseStringprepException;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>StanzaSender</code> class implements simple cyclic tasks management
 * mechanism. You can specify as many tasks in configuration as you need.
 * <p>
 * These tasks are designed to pull XMPP stanzas from specific data source like
 * SQL database, directory in the filesystem and so on. Each of these tasks must
 * extend <code>tigase.server.ssende.SenderTask</code> abstract class. Look in
 * specific tasks implementation for more detailed description how to use them.
 * <p>
 * Created: Fri Apr 20 11:11:25 2007
 * </p>
 * 
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class StanzaSender extends AbstractMessageReceiver implements Configurable,
		StanzaHandler {

	private static final String INTERVAL_PROP_KEY = "default-interval";
	private static final long INTERVAL_PROP_VAL = 10;
	private static final String STANZA_LISTENERS_PROP_KEY = "stanza-listeners";
	private static final String TASK_CLASS_PROP_KEY = "class-name";
	private static final String TASK_INIT_PROP_KEY = "init-string";
	private static final String TASK_INTERVAL_PROP_KEY = "interval";
	private static final String JDBC_TASK_NAME = "jdbc";
	private static final String JDBC_TASK_CLASS = "tigase.server.ssender.JDBCTask";
	private static final String JDBC_TASK_INIT =
			"jdbc:mysql://localhost/tigase?user=root&password=mypass&table=xmpp_stanza";
	private static final long JDBC_INTERVAL = 10;

	private static final String DRUPAL_FORUM_TASK_NAME = "drupal-forum";
	private static final String DRUPAL_FORUM_TASK_CLASS =
			"tigase.server.ssender.DrupalForumTask";
	private static final String DRUPAL_FORUM_TASK_INIT =
			"drupal_forum:mysql://localhost/tigase?user=root&password=mypass";
	private static final long DRUPAL_FORUM_INTERVAL = 30;

	private static final String DRUPAL_COMMENTS_TASK_NAME = "drupal-comments";
	private static final String DRUPAL_COMMENTS_TASK_CLASS =
			"tigase.server.ssender.DrupalCommentsTask";
	private static final String DRUPAL_COMMENTS_TASK_INIT =
			"drupal_comments:mysql://localhost/tigase?user=root&password=mypass";
	private static final long DRUPAL_COMMENTS_INTERVAL = 10;

	private static final String FILE_TASK_NAME = "file";
	private static final String FILE_TASK_CLASS = "tigase.server.ssender.FileTask";
	private static final String FILE_TASK_INIT = File.separator + "var" + File.separator
			+ "spool" + File.separator + "jabber" + File.separator + "*.stanza";
	private static final long FILE_INTERVAL = 10;
	private static final String[] STANZA_LISTENERS_PROP_VAL = { JDBC_TASK_NAME,
			FILE_TASK_NAME, DRUPAL_COMMENTS_TASK_NAME };
	private static final String TASK_ACTIVE_PROP_KEY = "active";
	private static final boolean TASK_ACTIVE_PROP_VAL = false;
	public static final String MY_DOMAIN_NAME_PROP_KEY = "domain-name";
	public static String MY_DOMAIN_NAME_PROP_VAL = "srecv.localhost";

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(StanzaSender.class.getName());

	private static final SimpleParser parser = SingletonFactory.getParserInstance();
	// private long interval = INTERVAL_PROP_VAL;
	private Map<String, SenderTask> tasks_list = new LinkedHashMap<String, SenderTask>();
	private Timer tasks = new Timer("StanzaSender", true);

	// Implementation of tigase.server.ServerComponent

	@Override
	public void release() {
		super.release();
	}

	@Override
	public void processPacket(final Packet packet) {
		// do nothing, this component is to send packets not to receive
		// (for now)
	}

	// Implementation of tigase.conf.Configurable

	@Override
	public void setProperties(final Map<String, Object> props) throws ConfigurationException {
		super.setProperties(props);

		if (props.size() == 1) {
			return;
		}

		// interval = (Long)props.get(INTERVAL_PROP_KEY);
		String[] config_tasks = (String[]) props.get(STANZA_LISTENERS_PROP_KEY);
		for (String task_name : config_tasks) {

			// Reconfiguration code. Turn-off old task with that name.
			SenderTask old_task = tasks_list.get(task_name);
			if (old_task != null) {
				old_task.cancel();
			}

			if ((Boolean) props.get(task_name + "/" + TASK_ACTIVE_PROP_KEY)) {
				String task_class = (String) props.get(task_name + "/" + TASK_CLASS_PROP_KEY);
				String task_init = (String) props.get(task_name + "/" + TASK_INIT_PROP_KEY);
				long task_interval = (Long) props.get(task_name + "/" + TASK_INTERVAL_PROP_KEY);
				try {
					SenderTask task = (SenderTask) Class.forName(task_class).newInstance();
					JID taskName = getComponentId().copyWithResource(task_name);
					task.setName(taskName);
					task.init(this, task_init);

					// Install new task
					tasks_list.put(task_name, task);
					tasks.scheduleAtFixedRate(task, task_interval * SECOND, task_interval * SECOND);

					log.config("Initialized task: " + task_name + ", class: " + task_class
							+ ", init: " + task_init + ", interval: " + task_interval);
				} catch (Exception e) {
					log.log(Level.SEVERE, "Can not initialize stanza listener: ", e);
				}
			}
		}
	}

	@Override
	public Map<String, Object> getDefaults(final Map<String, Object> params) {
		Map<String, Object> defs = super.getDefaults(params);
		defs.put(INTERVAL_PROP_KEY, INTERVAL_PROP_VAL);

		if ((Boolean) params.get(GEN_TEST)) {
			defs.put(FILE_TASK_NAME + "/" + TASK_ACTIVE_PROP_KEY, true);
		} else {
			defs.put(FILE_TASK_NAME + "/" + TASK_ACTIVE_PROP_KEY, TASK_ACTIVE_PROP_VAL);
		}
		defs.put(FILE_TASK_NAME + "/" + TASK_CLASS_PROP_KEY, FILE_TASK_CLASS);
		defs.put(FILE_TASK_NAME + "/" + TASK_INIT_PROP_KEY, FILE_TASK_INIT);
		defs.put(FILE_TASK_NAME + "/" + TASK_INTERVAL_PROP_KEY, FILE_INTERVAL);

		if ((Boolean) params.get(GEN_TEST)) {
			defs.put(JDBC_TASK_NAME + "/" + TASK_ACTIVE_PROP_KEY, true);
		} else {
			defs.put(JDBC_TASK_NAME + "/" + TASK_ACTIVE_PROP_KEY, TASK_ACTIVE_PROP_VAL);
		}
		defs.put(JDBC_TASK_NAME + "/" + TASK_CLASS_PROP_KEY, JDBC_TASK_CLASS);
		defs.put(JDBC_TASK_NAME + "/" + TASK_INIT_PROP_KEY, JDBC_TASK_INIT);
		defs.put(JDBC_TASK_NAME + "/" + TASK_INTERVAL_PROP_KEY, JDBC_INTERVAL);

		String repo_uri = DRUPAL_COMMENTS_TASK_INIT;
		if (params.get(GEN_CONF + "drupal-db-uri") != null) {
			repo_uri = (String) params.get(GEN_CONF + "drupal-db-uri");
		} else {
			if (params.get(RepositoryFactory.GEN_USER_DB_URI) != null) {
				repo_uri = (String) params.get(RepositoryFactory.GEN_USER_DB_URI);
			} // end of if (params.get(GEN_USER_DB_URI) != null)
		}

		List<String> listeners = new ArrayList<String>();
		listeners.addAll(Arrays.asList(STANZA_LISTENERS_PROP_VAL));

		String task_name = DRUPAL_COMMENTS_TASK_NAME;
		listeners.add(task_name);
		defs.put(task_name + "/" + TASK_ACTIVE_PROP_KEY, false);
		defs.put(task_name + "/" + TASK_CLASS_PROP_KEY, DRUPAL_COMMENTS_TASK_CLASS);
		defs.put(task_name + "/" + TASK_INTERVAL_PROP_KEY, DRUPAL_COMMENTS_INTERVAL);
		defs.put(task_name + "/" + TASK_INIT_PROP_KEY, repo_uri
				+ "&jid=drupal-comments@srecv." + getDefHostName());

		defs.put(STANZA_LISTENERS_PROP_KEY, listeners.toArray(new String[0]));
		if (params.get(GEN_VIRT_HOSTS) != null) {
			MY_DOMAIN_NAME_PROP_VAL =
					"ssend." + ((String) params.get(GEN_VIRT_HOSTS)).split(",")[0];
		} else {
			MY_DOMAIN_NAME_PROP_VAL = "ssend." + DNSResolverFactory.getInstance().getDefaultHosts()[0];
		}
		return defs;
	}

	@Override
	public void handleStanza(String stanza) {
		parseXMLData(stanza);
	}

	@Override
	public void handleStanza(Packet stanza) {
		addOutPacket(stanza);
	}

	@Override
	public void handleStanzas(Queue<Packet> results) {
		addOutPackets(results);
	}

	private void parseXMLData(String data) {
		DomBuilderHandler domHandler = new DomBuilderHandler();
		parser.parse(domHandler, data.toCharArray(), 0, data.length());
		Queue<Element> elems = domHandler.getParsedElements();
		while (elems != null && elems.size() > 0) {
			Element elem = elems.poll();
			try {
				Packet result = Packet.packetInstance(elem);
				addOutPacket(result);
			} catch (TigaseStringprepException ex) {
				log.info("Packet stringprep addressing problem, dropping packet: " + elem);
			}
		}
	}

}
