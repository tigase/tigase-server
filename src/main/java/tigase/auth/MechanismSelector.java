/*
 * MechanismSelector.java
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



package tigase.auth;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;

import javax.security.sasl.SaslServerFactory;

/**
 * Interface for implementing selectors of SASL mechanisms.
 *
 */
public interface MechanismSelector {
	/**
	 * Method filters all available SASL mechanisms from
	 * {@link SaslServerFactory factories} with current
	 * {@link XMPPResourceConnection session} state.
	 *
	 * @param serverFactories
	 *            {@link SaslServerFactory SaslServerFactory} enumeration.
	 * @param session
	 *            current session
	 * @return collection of all SASL mechanisms available in given session (and
	 *         current XMPP Stream).
	 */
	Collection<String> filterMechanisms(Enumeration<SaslServerFactory> serverFactories,
			XMPPResourceConnection session);

	/**
	 * Initialize selector.
	 *
	 * @param settings
	 *            settings of {@link tigase.xmpp.impl.SaslAuth} plugin.
	 */
	void init(Map<String, Object> settings);
}


//~ Formatted in Tigase Code Convention on 13/09/21
