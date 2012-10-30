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

/**
 * Describe class PropertyConstants here.
 *
 *
 * Created: Fri May 18 12:52:02 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class PropertyConstants {

	public enum SubscrRestrictions implements DefaultValues {
		PUBLIC,	LOCAL, REGEX, MODERATED;
		public String[] strValues() {
			String[] possible_values = new String[values().length];
			int i = 0;
			for (Enum val: values()) {
				possible_values[i++] = val.toString();
			} // end of for (Enum en_v: en_val.values())
			return possible_values;
		}
	};

	public enum MessageType implements DefaultValues {
		CHAT, HEADLINE, NORMAL;
		public String[] strValues() {
			String[] possible_values = new String[values().length];
			int i = 0;
			for (Enum val: values()) {
				possible_values[i++] = val.toString();
			} // end of for (Enum en_v: en_val.values())
			return possible_values;
		}
	};

	public enum SenderRestrictions implements DefaultValues {
		ALL, SUBSCRIBED, OWNER, LIST;
		public String[] strValues() {
			String[] possible_values = new String[values().length];
			int i = 0;
			for (Enum val: values()) {
				possible_values[i++] = val.toString();
			} // end of for (Enum en_v: en_val.values())
			return possible_values;
		}
	};

	public enum SenderAddress implements DefaultValues {
		REPLACE, LEAVE, REMOVE, REPLACE_SRECV;
		public String[] strValues() {
			String[] possible_values = new String[values().length];
			int i = 0;
			for (Enum val: values()) {
				possible_values[i++] = val.toString();
			} // end of for (Enum en_v: en_val.values())
			return possible_values;
		}
	};

	/**
	 * Constant <code>SUBSCR_LIST_PROP_KEY</code> is a property key for task
	 * instance configuration parameters. With this property you can provide
	 * task with initial list of subscribers. These users however must accept
	 * subscription first before any message is delivered to them. So you
	 * can't force ppl to receive any messages using this setting.
	 */
	public static final String SUBSCR_LIST_PROP_KEY =
		"subscription-list-key";
	/**
	 * Constant <code>SUBSCR_LIST_PROP_KEY</code> is a property value for a key
	 * <strong>SUBSCR_LIST_PROP_KEY</strong>. Please refer to key description
	 * for more details. Default value is an empty String array.
	 */
	public static final String[] SUBSCR_LIST_PROP_VAL = {};
	/**
	 * Constant <code>SUBSCR_RESTRICTIONS_PROP_KEY</code> is a property key for task
	 * instance configuration parameters. With this property you can decide who
	 * can subscribe to the task. Default value is <strong>PUBLIC</strong> so anybody
	 * can subscribe.
	 */
	public static final String SUBSCR_RESTRICTIONS_PROP_KEY =
		"subscription-restr";
	public static final String SUBSCR_RESTRICTIONS_DISPL_NAME =
		"Subscription restrictions";
	public static final SubscrRestrictions SUBSCR_RESTRICTIONS_PROP_VAL =
		SubscrRestrictions.PUBLIC;
	public static final String ALLOWED_SENDERS_PROP_KEY = "allowed-senders";
	public static final String ALLOWED_SENDERS_DISPL_NAME = "Allowed senders";
	public static final SenderRestrictions ALLOWED_SENDERS_PROP_VAL =
		SenderRestrictions.SUBSCRIBED;
	public static final String MESSAGE_TYPE_PROP_KEY = "message-type";
	public static final String MESSAGE_TYPE_DISPL_NAME = "Message type";
	public static final MessageType MESSAGE_TYPE_PROP_VAL = MessageType.CHAT;
	public static final String SUBSCR_RESTR_REGEX_PROP_KEY =
		"subscription-restr-regex";
	public static final String SUBSCR_RESTR_REGEX_DISPL_NAME =
		"Subscription restrictions regex";
	public static final String SUBSCR_RESTR_REGEX_PROP_VAL = ".*";
	public static final String ONLINE_ONLY_PROP_KEY = "online-users-only";
	public static final String ONLINE_ONLY_DISPL_NAME =
		"Send to online users only";
	public static final Boolean  ONLINE_ONLY_PROP_VAL = false;
	public static final String REPLACE_SENDER_PROP_KEY = "replace-sender";
	public static final String REPLACE_SENDER_DISPL_NAME =
		"Replace sender address";
	public static final SenderAddress REPLACE_SENDER_PROP_VAL =
		SenderAddress.REPLACE;
	public static final String ALLOWED_SENDERS_LIST_PROP_KEY =
		"allowed-senders-list";
	public static final String ALLOWED_SENDERS_LIST_DISPL_NAME =
		"List of users allowed to post";
	public static final String ALLOWED_SENDERS_LIST_PROP_VAL = "";
	/**
	 * Constant <code>DESCRIPTION_PROP_KEY</code> is a description for task instance.
	 * Let's say the user want's to create new <em>Interest group</em> for
	 * cyclists. This property allows to set some more detailed information about
	 * the group like: <em>This is group of ppl interested in mountain cycling
	 * near Cambridge.</em>
	 */
	public static final String DESCRIPTION_PROP_KEY = "description";
	public static final String DESCRIPTION_DISPL_NAME =
		"Description";
	public static final String DESCRIPTION_PROP_VAL = "News distribution task";
	public static final String TASK_OWNER_PROP_KEY = "task-owner";
	public static final String TASK_OWNER_DISPL_NAME = "Owner";
	public static final String TASK_OWNER_PROP_VAL = "admin@localhost";

	public static final String TASK_ADMINS_PROP_KEY = "task-admins";
	public static final String TASK_ADMINS_DISPL_NAME = "Administrators";
	public static final String TASK_ADMINS_PROP_VAL = "";

	public static final String USER_REPOSITORY_PROP_KEY = "user-repository";



} // PropertyConstants
