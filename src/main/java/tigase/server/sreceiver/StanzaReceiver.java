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

import java.util.Map;
import java.util.Queue;
import tigase.conf.Configurable;
import tigase.server.AbstractMessageReceiver;
import tigase.server.Packet;
import tigase.server.ServerComponent;

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
	implements Configurable {

	/**
	 * Describe <code>processPacket</code> method here.
	 *
	 * @param packet a <code>Packet</code> value
	 */
	public void processPacket(final Packet packet) {
		// do nothing, this component is to send packets not to receive
		// (for now)
	}

	/**
	 * Describe <code>setProperties</code> method here.
	 *
	 * @param props a <code>Map</code> value
	 */
	public void setProperties(final Map<String, Object> props) {
		super.setProperties(props);
	}

	public Map<String, Object> getDefaults(final Map<String, Object> params) {
		Map<String, Object> defs = super.getDefaults(params);
		return defs;
	}


} // StanzaReceiver
