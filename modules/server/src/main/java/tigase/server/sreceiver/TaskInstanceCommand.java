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

import tigase.db.TigaseDBException;

import tigase.server.Command;
import tigase.server.Packet;

import tigase.stats.StatRecord;

import tigase.util.TigaseStringprepException;

import tigase.xml.XMLUtils;

import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

import static tigase.server.sreceiver.TaskCommons.*;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class TaskInstanceCommand here.
 *
 *
 * Created: Fri May 18 22:45:40 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class TaskInstanceCommand implements TaskCommandIfc {

	/**
	 * Enum description
	 *
	 */
	public enum ACTION {
		TASK_CONFIGURATION, USER_MANAGEMENT,
		REMOVE_TASK;

		/**
		 * Method description
		 *
		 *
		 * 
		 */
		public static String[] strValues() {
			String[] possible_values = new String[values().length];
			int i = 0;

			for (Enum val : values()) {
				possible_values[i++] = val.toString();
			}    // end of for (Enum en_v: en_val.values())

			return possible_values;
		}
	}

	/**
	 * Enum description
	 *
	 */
	public enum ROSTER_ACTION {
		UPDATE_DATA,
		REMOVE_USER;

		/**
		 * Method description
		 *
		 *
		 * 
		 */
		public static String[] strValues() {
			String[] possible_values = new String[values().length];
			int i = 0;

			for (Enum val : values()) {
				possible_values[i++] = val.toString();
			}    // end of for (Enum en_v: en_val.values())

			return possible_values;
		}
	}

	/**
	 * Enum description
	 *
	 */
	public enum USER_ACTION {
		APROVE_PENDING_MODERATIONS, REJECT_PENDING_MODERATIONS,
		SELECT_USER;

		/**
		 * Method description
		 *
		 *
		 * 
		 */
		public static String[] strValues() {
			String[] possible_values = new String[values().length];
			int i = 0;

			for (Enum val : values()) {
				possible_values[i++] = val.toString();
			}    // end of for (Enum en_v: en_val.values())

			return possible_values;
		}
	}

	//~--- static fields --------------------------------------------------------

	private static Logger log = Logger.getLogger("tigase.server.sreceiver.TaskInstanceCommand");
	protected static final String ACTION_FIELD = "action-field";
	protected static final String USER_ACTION_FIELD = "user-action-field";
	protected static final String CONFIRM = "confirm-field";
	protected static final String PENDING_MODERATIONS_FIELD = "Pending moderations";
	protected static final String SUBSCRIBERS_FIELD = "Subscribers";
	protected static final String ROSTER_ACTION_FIELD = "roster-action-field";
	;
	;
	;

	//~--- get methods ----------------------------------------------------------

	@Override
	public String getDescription() {
		return "Manage task exiting instance";
	}

	@Override
	public String getNodeName() {
		return "*";
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public void processCommand(Packet packet, Packet result, StanzaReceiver receiv) {
		String task_name = packet.getStanzaTo().toString();
		ReceiverTaskIfc task = receiv.getTaskInstances().get(task_name);

		if (task == null) {
			task_name = Command.getFieldValue(packet, TASK_NAME_FIELD);

			if (task_name != null) {
				task = receiv.getTask(task_name);
			}    // end of if (task_name != null)
		}      // end of if (task == null)

		if (task != null) {
			task_name = task.getJID().toString();

			if ( !(receiv.isAdmin(packet.getStanzaFrom()) || task.isAdmin(packet.getStanzaFrom()))) {
				Command.addFieldValue(result, "Info",
						"You are not administrator of: " + task_name + " task.", "fixed");
				Command.addFieldValue(result, "Info", "You can not execute task commands.", "fixed");

				return;
			}

			Command.addFieldValue(result, TASK_NAME_FIELD, task_name, "hidden");

			String action_str = Command.getFieldValue(packet, ACTION_FIELD);

			if (action_str == null) {
				taskMainScreen(result, task);

				return;
			}    // end of if (step == null)

			Command.addFieldValue(result, ACTION_FIELD, action_str, "hidden");

			ACTION action = ACTION.valueOf(action_str);

			switch (action) {
				case TASK_CONFIGURATION :
					editConfiguration(packet, result, receiv);

					break;

				case USER_MANAGEMENT :
					try {
						manageUsers(packet, result, receiv);
					} catch (TigaseStringprepException ex) {
						Logger.getLogger(TaskInstanceCommand.class.getName()).log(Level.SEVERE, null, ex);
					}

					break;

				case REMOVE_TASK :
					removeTask(packet, result, receiv);

					break;

				default :
					break;
			}    // end of switch (action)
		} else {
			Command.setStatus(result, Command.Status.executing);
			Command.addAction(result, Command.Action.next);
			Command.addFieldValue(result, TASK_NAME_FIELD, "", "text-single",
					"Enter task JID or Name");
		}    // end of if (task != null) else
	}

	private void editConfiguration(Packet packet, Packet result, StanzaReceiver receiv) {
		String confirm = Command.getFieldValue(packet, CONFIRM);
		String task_name = Command.getFieldValue(packet, TASK_NAME_FIELD);
		ReceiverTaskIfc task = receiv.getTaskInstances().get(task_name);

		if (confirm == null) {
			Command.addFieldValue(result, CONFIRM, "true", "hidden");
			Command.setStatus(result, Command.Status.executing);
			Command.addAction(result, Command.Action.complete);
			propertyItems2Command(task.getParams(), result);
		} else {
			Command.addFieldValue(result, "Info", "Changed parameters for " + task_name + ":",
					"fixed");

			Map<String, PropertyItem> old_params = task.getParams();
			Map<String, Object> new_params = new LinkedHashMap<String, Object>();

			for (Map.Entry<String, PropertyItem> entry : old_params.entrySet()) {
				if (entry.getValue().isMultiValue()) {
					String[] values = Command.getFieldValues(packet, XMLUtils.escape(entry.getKey()));

					new_params.put(entry.getKey(), values);
				} else {
					String value = Command.getFieldValue(packet, XMLUtils.escape(entry.getKey()));

					if (value == null) {
						value = "";
					}    // end of if (value == null)

					value = XMLUtils.unescape(value);

					if ( !value.equals(entry.getValue().toString())) {
						new_params.put(entry.getKey(), value);
						Command.addFieldValue(result, "Info",
								entry.getValue().getDisplay_name() + ": " + value, "fixed");
					}
				}
			}    // end of for (String key: default_props.keySet())

			try {
				task.setParams(new_params);
				receiv.saveTaskToRepository(task);
			} catch (TigaseDBException e) {
				log.log(Level.SEVERE, "Problem with saving task to repository: " + task.getJID(), e);
				Command.addFieldValue(result, "Info",
						"Problem saving task to repository, look in log file for details.", "fixed");
			}
		}
	}

	private void manageUsers(Packet packet, Packet result, StanzaReceiver receiv)
			throws TigaseStringprepException {
		String task_name = Command.getFieldValue(packet, TASK_NAME_FIELD);
		ReceiverTaskIfc task = receiv.getTaskInstances().get(task_name);
		String user_action = Command.getFieldValue(packet, USER_ACTION_FIELD);

		if (user_action == null) {
			Command.setStatus(result, Command.Status.executing);
			Command.addAction(result, Command.Action.complete);
			Command.addAction(result, Command.Action.next);
			Command.addFieldValue(result, "Info", "Select action and user:", "fixed");

			String[] actions = USER_ACTION.strValues();
			List<String> moderated = new LinkedList<String>();

			for (RosterItem ri : task.getRoster().values()) {
				if ( !ri.isModerationAccepted()) {
					moderated.add(ri.getJid().toString());
				}
			}

			if (moderated.size() > 0) {
				Command.addFieldValue(result, USER_ACTION_FIELD, actions[0], "Select action", actions,
						actions);

				String[] moder = moderated.toArray(new String[0]);

				Command.addFieldValue(result, PENDING_MODERATIONS_FIELD, moder[0],
						PENDING_MODERATIONS_FIELD, moder, moder, "list-multi");
			} else {
				Command.addFieldValue(result, USER_ACTION_FIELD, actions[2], "Select action", actions,
						actions);
				Command.addFieldValue(result, "Info", "No pending moderations.", "fixed");
			}

			String[] all_subscr = new String[task.getRoster().values().size()];
			int idx = 0;

			for (RosterItem ri : task.getRoster().values()) {
				all_subscr[idx++] = ri.getJid().toString();
			}

			Command.addFieldValue(result, SUBSCRIBERS_FIELD, all_subscr[0], SUBSCRIBERS_FIELD,
					all_subscr, all_subscr);
		} else {
			Command.addFieldValue(result, USER_ACTION_FIELD, user_action, "hidden");

			String[] jids = Command.getFieldValues(packet, PENDING_MODERATIONS_FIELD);

			switch (USER_ACTION.valueOf(user_action)) {
				case APROVE_PENDING_MODERATIONS :
					if (jids != null) {
						Map<JID, RosterItem> roster = task.getRoster();

						for (String jid : jids) {
							RosterItem ri = roster.get(JID.jidInstanceNS(jid));

							if (ri != null) {
								task.setRosterItemModerationAccepted(ri, true);
								receiv.addOutPacket(getMessage(ri.getJid(), task.getJID(),
										StanzaType.headline, "Your subscription has been approved."));
							} else {
								log.warning("Missing jid: " + jid + " in task: " + task_name + " roster.");
							}
						}

						Command.addFieldValue(result, "Info", "Subscriptions have been approved.",
								"fixed");
					} else {
						Command.addFieldValue(result, "Info", "No subscriptions to approve.", "fixed");
					}

					break;

				case REJECT_PENDING_MODERATIONS :
					if (jids != null) {
						JID[] jjids = new JID[jids.length];
						int idx = 0;

						for (String jid : jids) {
							jjids[idx] = JID.jidInstanceNS(jid);
							receiv.addOutPacket(getMessage(task.getJID(), jjids[idx++], StanzaType.headline,
									"Your subscription has been rejected."));
						}

						receiv.removeTaskSubscribers(task, jjids);
						Command.addFieldValue(result, "Info", "Subscriptions have been rejected.",
								"fixed");
					} else {
						Command.addFieldValue(result, "Info", "No subscriptions to reject.", "fixed");
					}

					break;

				case SELECT_USER :
					JID jid = JID.jidInstance(Command.getFieldValue(packet, SUBSCRIBERS_FIELD));
					RosterItem ri = task.getRoster().get(jid);

					if (ri != null) {
						String roster_action = Command.getFieldValue(packet, ROSTER_ACTION_FIELD);

						if (roster_action == null) {
							Command.setStatus(result, Command.Status.executing);
							Command.addAction(result, Command.Action.complete);
							Command.addFieldValue(result, "Info", "Update subscription data for: " + jid,
									"fixed");
							Command.addFieldValue(result, SUBSCRIBERS_FIELD, jid.toString(), "hidden");

							String[] actions = ROSTER_ACTION.strValues();

							Command.addFieldValue(result, ROSTER_ACTION_FIELD, actions[0], "Select action",
									actions, actions);

							String[] bool_arr = new String[] { "true", "false" };

							Command.addFieldValue(result, "subscribed-ri",
									Boolean.valueOf(ri.isSubscribed()).toString(), "Subscribed", bool_arr,
										bool_arr);
							Command.addFieldValue(result, "moderation-ri",
									Boolean.valueOf(ri.isModerationAccepted()).toString(),
										"Moderation approved", bool_arr, bool_arr);
							Command.addFieldValue(result, "owner-ri",
									Boolean.valueOf(ri.isOwner()).toString(), "Owner", bool_arr, bool_arr);
							Command.addFieldValue(result, "admin-ri",
									Boolean.valueOf(ri.isAdmin()).toString(), "Admin", bool_arr, bool_arr);
						} else {
							Command.addFieldValue(result, ROSTER_ACTION_FIELD, roster_action, "hidden");

							switch (ROSTER_ACTION.valueOf(roster_action)) {
								case UPDATE_DATA :
									ri.setSubscribed(parseBool(Command.getFieldValue(packet, "subscribed-ri")));
									ri.setModerationAccepted(parseBool(Command.getFieldValue(packet,
											"moderation-ri")));
									ri.setOwner(parseBool(Command.getFieldValue(packet, "owner-ri")));
									ri.setAdmin(parseBool(Command.getFieldValue(packet, "admin-ri")));
									task.setRosterItemModerationAccepted(ri, ri.isModerationAccepted());
									Command.addFieldValue(result, "Info",
											"Subscription data for user " + jid + " have been updated.", "fixed");

									break;

								case REMOVE_USER :
									receiv.removeTaskSubscribers(task, jid);
									Command.addFieldValue(result, "Info",
											"Subscription for user " + jid + " has been removed.", "fixed");

									break;

								default :
									break;
							}
						}
					} else {
						Command.addFieldValue(result, "Info",
								"There was a problem accesing user subscription data.", "fixed");
					}

					break;

				default :
					break;
			}
		}
	}

	private void removeTask(Packet packet, Packet result, StanzaReceiver receiv) {
		String confirm = Command.getFieldValue(packet, CONFIRM);
		String task_name = Command.getFieldValue(packet, TASK_NAME_FIELD);

		if (confirm == null) {
			Command.addFieldValue(result, CONFIRM, "true", "hidden");
			Command.setStatus(result, Command.Status.executing);
			Command.addAction(result, Command.Action.complete);
			Command.addFieldValue(result, "Info",
					"Are you sure you want to remove task: " + task_name + " and all it's data?",
						"fixed");
			Command.addFieldValue(result, "Info",
					"Note! There is no undo for task deletion function", "fixed");

			return;
		}    // end of if (confirm == null)

		receiv.removeTaskInstance(receiv.getTask(task_name));
		Command.addFieldValue(result, "Info", "Task " + task_name + " has been removed.", "fixed");
	}

	private void taskMainScreen(Packet result, ReceiverTaskIfc task) {
		Command.setStatus(result, Command.Status.executing);
		Command.addAction(result, Command.Action.next);

		List<StatRecord> stats = task.getStats();

		if (stats != null) {
			for (StatRecord rec : stats) {
				Command.addFieldValue(result, "Info", rec.getDescription() + ": " + rec.getValue(),
						"fixed");
			}    // end of for (StatRecord rec: stats)
		}      // end of if (stats != null)

		String[] actions = ACTION.strValues();

		Command.addFieldValue(result, ACTION_FIELD, actions[0], "Select action", actions, actions);
	}
}    // TaskInstanceCommand


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
