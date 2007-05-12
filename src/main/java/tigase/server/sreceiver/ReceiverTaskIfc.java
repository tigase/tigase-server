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

import java.awt.TrayIcon.MessageType;
import java.util.Map;
import java.util.Queue;
import tigase.server.Packet;

/**
 * This is <code>StanzaReceiver</code> task which can receive XMPP packets
 * to do something with them. It may produce new XMPP packets in exchange
 * to send them back or to other users.
 * <p>
 * Created: Wed May  9 13:52:57 2007
 * </p>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface ReceiverTaskIfc {

	public enum SubscrRestrictions {
		PUBLIC,	LOCAL, REGEX, MODERATED;
	};
	public enum MessageType {
		CHAT, HEADLINE, NORMAL;
	};
	public enum SenderRestrictions {
		ALL, SUBSCRIBED, OWNER, LIST;
	};

// 	/**
// 	 * Constant <code>SUBSCR_LIST_PROP_KEY</code> is a property key for task
// 	 * instance configuration parameters. With this property you can provide
// 	 * task with initial list of subscribers. These users however must accept
// 	 * subscription first before any message is delivered to them. So you
// 	 * can't force ppl to receive any messages using this setting.
// 	 */
// 	public static final String SUBSCR_LIST_PROP_KEY =
// 		"subscription-list-key";
// 	/**
// 	 * Constant <code>SUBSCR_LIST_PROP_KEY</code> is a property value for a key
// 	 * <strong>SUBSCR_LIST_PROP_KEY</strong>. Please refer to key description
// 	 * for more details. Default value is an empty String array.
// 	 */
// 	public static final String[] SUBSCR_LIST_PROP_VAL = {};
	/**
	 * Constant <code>SUBSCR_RESTRICTIONS_PROP_KEY</code> is a property key for task
	 * instance configuration parameters. With this property you can decide who
	 * can subscribe to the task. Default value is <strong>PUBLIC</strong> so anybody
	 * can subscribe.
	 */
	public static final String SUBSCR_RESTRICTIONS_PROP_KEY =
		"subscription-restrictions-key";
	public static final SubscrRestrictions SUBSCR_RESTRICTIONS_PROP_VAL =
		SubscrRestrictions.PUBLIC;
	public static final String ALLOWED_SENDERS_PROP_KEY = "allowed-senders";
	public static final SenderRestrictions ALLOWED_SENDERS_PROP_VAL =
		SenderRestrictions.SUBSCRIBED;
	public static final String MESSAGE_TYPE_PROP_KEY = "message-type";
	public static final MessageType MESSAGE_TYPE_PROP_VAL = MessageType.CHAT;
	public static final String SUBSCR_RESTR_REGEX_PROP_KEY = "subscription-regex";
	public static final String SUBSCR_RESTR_REGEX_PROP_VAL = ".*";
	public static final String ONLINE_ONLY_PROP_KEY = "online-users-only";
	public static final boolean ONLINE_ONLY_PROP_VAL = false;
	public static final String REPLACE_SENDER_PROP_KEY = "replace-sender";
	public static final boolean REPLACE_SENDER_PROP_VAL = true;
	public static final String ALLOWED_SENDERS_LIST_PROP_KEY =
		"allowed-senders-list";
	public static final String[] ALLOWED_SENDERS_LIST_PROP_VAL = {};
	/**
	 * Constant <code>DESCRIPTION_PROP_KEY</code> is a description for task instance.
	 * Let's say the user want's to create new <em>Interest group</em> for
	 * cyclists. This property allows to set some more detailed information about
	 * the group like: <em>This is group of ppl interested in mountain cycling
	 * near Cambridge.</em>
	 */
	public static final String DESCRIPTION_PROP_KEY = "description";
	public static final String DESCRIPTION_PROP_VAL = "Abstract receiver task";
	public static final String TASK_OWNER_PROP_KEY = "task-owner";
	public static final String TASK_OWNER_PROP_VAL = "admin@localhost";

	/**
	 * <code>getType</code> method returns the task type name. This
	 * name is displayed on the list of all tasks available for creation.
	 * This is not a nick name of created task this is something more like
	 * "<em>Drupal forums connector</em>" or "<em>Interest group</em>".
	 * Like a Java class name is unique in JVM the task name must be
	 * also inique for <code>StanzaReceiver.</code> Look at
	 * <strong>task nick name</strong> for more details.
	 *
	 * @return a <code>String</code> value of the task name.
	 */
	String getType();

	/**
	 * <code>getHelp</code> method returns task help information. This
	 * general information about tasks abilties so when the user selects
	 * task for creation he might know what the task is about. This is more
	 * like class description rather then instance description.
	 *
	 * @return a <code>String</code> value of task class help information.
	 */
	String getHelp();

	/**
	 * <code>getInstance</code> method returns new task instance of this type.
	 * This is something like <code>Class.newInstance()</code> but as this method
	 * is called on the already created instance there is no danger of the number
	 * of exceptions which could be normally thrown.
	 *
	 * @return a <code>ReceiverTaskIfc</code> new task instance of this type.
	 */
	ReceiverTaskIfc getInstance();

	/**
	 * <code>setJID</code> method sets tasks <strong>Jabber ID</strong>, unique
	 * ID which is used to identify the task. Example of the resulting
	 * Jabber ID for domain <code>tigase.org</code> and
	 * <code>StanzaReceiver</code> component name <code>srec</code> would be:
	 * <code>devs@srec.tigase.org</code>.
	 * There can be many tasks of the same type (having the same
	 * <strong>TaskType</strong>) but they have to have distinct JIDs.
	 * Examples of possible JIDs names are: "<em>admin-forum@srec.tigase.org</em>",
	 * "<em>cycling-interest-group@srec.tigase.org</em>".
	 *
	 * @param jid a <code>String</code> value of the nick name.
	 */
	void setJID(String jid);

	/**
	 * <code>getJID</code> method returns task instance
	 * <strong>Jabber ID</strong>. Refer to corresponding <code>set</code>
	 * method for more details.
	 *
	 * @return a <code>String</code> value of task instance JID,
	 * @see setJID
	 */
	String getJID();

	/**
	 * <code>getDescription</code> method returns a description for task instance.
	 * Let's say the user want's to create new <em>Interest group</em> for
	 * cyclists. This property allows to set some more detailed information about
	 * the group like: <em>This is group of ppl interested in mountain cycling
	 * near Cambridge.</em>
	 *
	 * The description is set via properties using <code>DESCRIPTION_PROP_KEY</code>.
	 *
	 * @return a <code>String</code> value of task instance description;
	 * @see DESCRIPTION_PROP_KEY
	 */
	String getDescription();

	/**
	 * <code>setParams</code> method sets <code>Map</code> with configuration
	 * parameters. Some parameters are predefined and common for most of task
	 * types but some tasks may require/accept extra parameters to work properly.
	 *
	 * @param params a <code>Map</code> value with configuration parameters.
	 */
	void setParams(Map<String, Object> params);

	/**
	 * <code>getParams</code> method return task instance configuration parameters.
	 * If parameters haven't been set before the method should return
	 * <code>Map</code> with a list of possible parameters it may accept with
	 * default values where appropriate.
	 * For more detailed information about configuration parameters please refer
	 * to corresponding <code>set</code> method.
	 *
	 * @return a <code>Map</code> value with task instance configuration parameters.
	 * @see setParams
	 */
	Map<String, Object> getParams();

	/**
	 * <code>processPacket</code> method takes a packet addressed to this task
	 * as a parameter and does something with the packet. If as a result of
	 * input packet processing it generates some other packets to send they
	 * should be included in the <code>results</code> queue.
	 *
	 * @param packet a <code>Packet</code> input packet for processing.
	 * @param results a <code>Queue<Packet></code> is a collection of
	 * result packets to send out.
	 */
	void processPacket(Packet packet, Queue<Packet> results);

} // ReceiverTaskIfc
