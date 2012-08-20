/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.server.sreceiver;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Message;
import tigase.server.Packet;

import tigase.sys.TigaseRuntime;

import tigase.util.TigaseStringprepException;

import tigase.xml.Element;

import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

import static tigase.server.sreceiver.PropertyConstants.*;

//~--- JDK imports ------------------------------------------------------------

import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Jan 22, 2009 11:02:59 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class PubSubTestsTask extends RepoRosterTask {
	private enum command {
		help(" - Displays help info."), newnodes(" N - Create N nodes in the pubsub."),
		newsubscr(" node-name N - create N subscriptions for a given node name."),
		newnodessubscr(" N M -  create N nodes with M subscriptions each."),
		setdelay(" N - set the delay between sending node creation packet."),
		stop(" - Stops the current packets generation.");

		private String helpText = null;

		//~--- constructors -------------------------------------------------------

		private command(String helpText) {
			this.helpText = helpText;
		}

		//~--- get methods --------------------------------------------------------

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getHelp() {
			return helpText;
		}
	}

	//~--- static fields --------------------------------------------------------

	private static final String TASK_TYPE = "PubSub tests";
	private static final String TASK_HELP =
		"This is a PubSub component testing task."
		+ " Only for testing and only to be run by an admnistrator.";

	//~--- fields ---------------------------------------------------------------

	private long delay = 2000;
	private Element conf = new Element("x",
																		 new Element[] { new Element("field",
					new Element[] { new Element("value",
					"http://jabber.org/protocol/pubsub#node_config") },
					new String[] { "var", "type" },
					new String[] { "FORM_TYPE", "hidden" }), new Element("field",
					new Element[] { new Element("value",
					"0") },
					new String[] { "var" },
					new String[] { "pubsub#notify_sub_aff_state" }) },
																		 new String[] { "xmlns", "type" },
																		 new String[] { "jabber:x:data", "submit" });
	private String[] last_nodes = null;
	private long packetsGenerated = 0;
	private boolean stop = false;

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public Map<String, PropertyItem> getDefaultParams() {
		Map<String, PropertyItem> defs = super.getDefaultParams();

		defs.put(DESCRIPTION_PROP_KEY,
						 new PropertyItem(DESCRIPTION_PROP_KEY,
															DESCRIPTION_DISPL_NAME,
															"PubSub Testing Task"));
		defs.put(MESSAGE_TYPE_PROP_KEY,
						 new PropertyItem(MESSAGE_TYPE_PROP_KEY,
															MESSAGE_TYPE_DISPL_NAME,
															MessageType.NORMAL));
		defs.put(ONLINE_ONLY_PROP_KEY,
						 new PropertyItem(ONLINE_ONLY_PROP_KEY, ONLINE_ONLY_DISPL_NAME, false));
		defs.put(REPLACE_SENDER_PROP_KEY,
						 new PropertyItem(REPLACE_SENDER_PROP_KEY,
															REPLACE_SENDER_DISPL_NAME,
															SenderAddress.LEAVE));
		defs.put(SUBSCR_RESTRICTIONS_PROP_KEY,
						 new PropertyItem(SUBSCR_RESTRICTIONS_PROP_KEY,
															SUBSCR_RESTRICTIONS_DISPL_NAME,
															SubscrRestrictions.MODERATED));

		return defs;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getHelp() {
		return TASK_HELP;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getType() {
		return TASK_TYPE;
	}

	//~--- methods --------------------------------------------------------------

	@Override
	protected boolean addOutPacket(Packet packet) {
		++packetsGenerated;

		return super.addOutPacket(packet);
	}

	@Override
	protected void processMessage(Packet packet, Queue<Packet> results) {
		if (isPostCommand(packet)) {
			runCommand(packet, results);
		}
	}

	private void addSubscriptionsForNode(JID from, JID to, String node, int subscr) {
		int j = 0;
		Element el = createPubSubEl(from,
																to,
																"ids-" + (++j),
																node,
																"subscriptions",
																"http://jabber.org/protocol/pubsub#owner");

		for (int i = 0; i < subscr; i++) {
			Element subs = new Element("subscription",
																 new String[] { "jid", "subscription" },
																 new String[] { "frank-" + i + "@" + getJID().getDomain(),
							"subscribed" });

			el.findChild("/iq/pubsub/subscriptions").addChild(subs);

			if (i % 100 == 0) {
				addOutPacket(Packet.packetInstance(el, from, to));
				el = createPubSubEl(from, to, "ids-" + (++j), node, "subscriptions",
														"http://jabber.org/protocol/pubsub#owner");
			}
		}

		addOutPacket(Packet.packetInstance(el, from, to));
	}

	private void addSubscriptionsForNodes(JID from, JID to, String[] nodes, int subscr) {
		for (String node : nodes) {
			addSubscriptionsForNode(from, to, node, subscr);
		}
	}

	private String commandsHelp() {
		StringBuilder sb = new StringBuilder();

		for (command comm : command.values()) {
			sb.append("//" + comm.name() + comm.getHelp() + "\n");
		}

		return "Available commands are:\n" + sb.toString();
	}

	private String[] createNodes(JID from, JID to, int... nums) {
		String[] nodes = new String[nums[0]];

		for (int i = 0; i < nums[0]; i++) {
			if (stop) {
				break;
			}

			String node = "node-" + i;

			nodes[i] = node;

			Element el = createPubSubEl(from,
																	to,
																	"id-" + i,
																	node,
																	"create",
																	"http://jabber.org/protocol/pubsub");

			el.findChild("/iq/pubsub").addChild(new Element("configure"));
			el.findChild("/iq/pubsub/configure").addChild(conf);
			addOutPacket(Packet.packetInstance(el, from, to));

			if ((nums.length > 1) && (nums[1] > 0)) {
				addSubscriptionsForNode(from, to, node, nums[1]);
			}

			while (memoryLow()) {
				try {
					System.gc();
					Thread.sleep(delay);
				} catch (Exception e) {}
			}
		}

		stop = false;

//  if (nums.length > 1 && nums[1] > 0) {
//    addSubscriptionsForNodes(from, nodes, nums[1]);
//  }
		return nodes;
	}

	private Element createPubSubEl(JID from, JID to, String id, String node, String pubsub_call,
																 String xmlns) {
		Element elem = new Element("iq",
															 new Element[] { new Element("pubsub",
						new Element[] { new Element(pubsub_call,
						new String[] { "node" },
						new String[] { node }) },
						new String[] { "xmlns" },
						new String[] { xmlns }) },
															 new String[] { "type", "from", "to", "id" },
															 new String[] { "set", from.toString(), to.toString(), id });

		return elem;
	}

	//~--- get methods ----------------------------------------------------------

	private boolean isPostCommand(Packet packet) {
		String body = packet.getElemCData("/message/body");

		if (body != null) {
			for (command comm : command.values()) {
				if (body.startsWith("//" + comm.toString())) {
					return true;
				}
			}
		}

		return false;
	}

	//~--- methods --------------------------------------------------------------

	private boolean memoryLow() {
		return TigaseRuntime.getTigaseRuntime().getHeapMemUsage() > 80f;
	}

	;

	//~--- methods --------------------------------------------------------------

	private int[] parseNumbers(String[] args, int pos, int num) {

		// The first arg is command name, after that command parameters come
		int[] res = new int[num];

		for (int i = 0; i < res.length; i++) {
			try {
				res[i] = Integer.parseInt(args[i + pos]);
			} catch (Exception e) {
				return null;
			}
		}

		return res;
	}

	private void runCommand(final Packet packet, Queue<Packet> results) {
		String body = packet.getElemCData("/message/body");
		final String[] body_split = body.split("\\s");
		command comm = command.valueOf(body_split[0].substring(2));
		final int[] pars;
		final JID to = JID.jidInstanceNS("pubsub." + getJID().getDomain());

		switch (comm) {
			case help :
				results.offer(Message.getMessage(packet.getStanzaTo(), packet.getStanzaFrom(),
																				 StanzaType.chat, commandsHelp(),
																				 "Commands description", null, packet.getStanzaId()));

				break;

			case setdelay :
				pars = parseNumbers(body_split, 1, 1);

				if (pars != null) {
					delay = pars[0];
				}

				break;

			case newnodes :
				pars = parseNumbers(body_split, 1, 1);

				if (pars != null) {
					addOutPacket(Message.getMessage(packet.getStanzaTo(), packet.getStanzaFrom(),
																					StanzaType.chat, "Task accepted, processing...",
																					"PubSub testing task", null, packet.getStanzaId()));
					runInThread(new Runnable() {
						@Override
						public void run() {
							last_nodes = createNodes(packet.getStanzaFrom(), to, pars);
						}
					}, packet);
				} else {
					results.offer(Message.getMessage(packet.getStanzaTo(), packet.getStanzaFrom(),
																					 StanzaType.chat, "Incorrect command parameters.",
																					 "PubSub testing task", null, packet.getStanzaId()));

					return;
				}

				break;

			case newsubscr :
				if (last_nodes != null) {
					pars = parseNumbers(body_split, 2, 1);

					if (pars != null) {
						addOutPacket(Message.getMessage(packet.getStanzaTo(), packet.getStanzaFrom(),
																						StanzaType.chat, "Task accepted, processing...",
																						"PubSub testing task", null,
																						packet.getStanzaId()));
						runInThread(new Runnable() {
							@Override
							public void run() {
								addSubscriptionsForNode(packet.getStanzaFrom(), to, body_split[1], pars[0]);
							}
						}, packet);
					} else {
						results.offer(Message.getMessage(packet.getStanzaTo(), packet.getStanzaFrom(),
																						 StanzaType.chat, "Incorrect command parameters.",
																						 "PubSub testing task", null,
																						 packet.getStanzaId()));

						return;
					}
				} else {
					results.offer(Message.getMessage(packet.getStanzaTo(), packet.getStanzaFrom(),
																					 StanzaType.chat,
																					 "There are no pubsub nodes created yet.",
																					 "PubSub testing task", null, packet.getStanzaId()));

					return;
				}

				break;

			case newnodessubscr :
				pars = parseNumbers(body_split, 1, 2);

				if (pars != null) {
					addOutPacket(Message.getMessage(packet.getStanzaTo(), packet.getStanzaFrom(),
																					StanzaType.chat,
																					"" + new Date() + " Task accepted, processing...",
																					"PubSub testing task", null, packet.getStanzaId()));
					runInThread(new Runnable() {
						@Override
						public void run() {
							last_nodes = createNodes(packet.getStanzaFrom(), to, pars);
						}
					}, packet);
				} else {
					results.offer(Message.getMessage(packet.getStanzaTo(), packet.getStanzaFrom(),
																					 StanzaType.chat, "Incorrect command parameters.",
																					 "PubSub testing task", null, packet.getStanzaId()));

					return;
				}

				break;

			case stop :
				stop = true;

				break;
		}
	}

	private void runInThread(final Runnable job, final Packet packet) {
		Thread thr = new Thread() {
			@Override
			public void run() {
				long gen_start = System.currentTimeMillis();

				job.run();

				long gen_end = System.currentTimeMillis();
				long gen_time = gen_end - gen_start;
				long gen_hours = gen_time / 3600000;
				long gen_mins = (gen_time - (gen_hours * 3600000)) / 60000;
				long gen_secs = (gen_time - ((gen_hours * 3600000) + (gen_mins * 60000))) / 1000;

				addOutPacket(Message.getMessage(packet.getStanzaTo(), packet.getStanzaFrom(),
																				StanzaType.chat,
																				"" + new Date()
																				+ " Generation of the test data completed.\n"
																				+ "Generated in: " + gen_hours + "h, " + gen_mins
																				+ "m, " + gen_secs + "sec" + ", packets generated: "
																				+ packetsGenerated, "PubSub testing task", null,
																					packet.getStanzaId()));
			}
		};

		thr.setName("pubsub-test-job");
		thr.start();
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
