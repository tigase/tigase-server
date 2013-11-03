/*
 * CommandListenerSMAbstract.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
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



/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package tigase.cluster.api;

//~--- non-JDK imports --------------------------------------------------------

import tigase.cluster.strategy.ClusteringStrategyIfc;
import tigase.cluster.strategy.ConnectionRecordIfc;

import tigase.stats.StatisticsList;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

import static tigase.cluster.strategy.ClusteringStrategyIfc.*;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

/**
 *
 * @author kobit
 *
 * @param <E>
 * @param <R>
 */
public abstract class CommandListenerSMAbstract<R extends ConnectionRecordIfc,
		E extends ClusteringStrategyIfc<R>>
				extends CommandListenerAbstract {
	private final E strat;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param name
	 * @param strat
	 */
	public CommandListenerSMAbstract(String name, E strat) {
		super(name);
		this.strat = strat;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param node is a <code>JID</code>
	 * @param data is a <code>Map<String,String></code>
	 *
	 * @return a value of <code>R</code>
	 */
	public R getConnectionRecord(JID node, Map<String, String> data) {
		BareJID userId       = BareJID.bareJIDInstanceNS(data.get(USER_ID));
		String  resource     = data.get(RESOURCE);
		JID     jid          = JID.jidInstanceNS(userId, resource);
		String  sessionId    = data.get(XMPP_SESSION_ID);
		JID     connectionId = JID.jidInstanceNS(data.get(CONNECTION_ID));
		R       result       = strat.getConnectionRecordInstance();

		result.setRecordFields(node, jid, sessionId, connectionId);

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>E</code>
	 */
	public E getStrategy() {
		return strat;
	}
}


//~ Formatted in Tigase Code Convention on 13/11/02
