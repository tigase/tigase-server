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
 * Last modified by $Author:$
 * $Date$
 *
 */

package tigase.server.xmppclient;

import tigase.xmpp.BareJID;

import tigase.vhosts.VHostItem;
import tigase.vhosts.VHostManagerIfc;
import tigase.xml.Element;

import java.util.List;
import java.util.Map;
import tigase.server.Lifecycle;
import tigase.xmpp.JID;

/**
 * @author Wojtek
 */
public interface SeeOtherHostIfc extends Lifecycle {
	public static final String CM_SEE_OTHER_HOST_CLASS_PROPERTY = "--cm-see-other-host";

	public static final String CM_SEE_OTHER_HOST_CLASS_PROP_KEY = "cm-see-other-host";

	public static final String CM_SEE_OTHER_HOST_CLASS_PROP_DEF_VAL =
			"tigase.server.xmppclient.SeeOtherHost";
	public static final String CM_SEE_OTHER_HOST_CLASS_PROP_DEF_VAL_CLUSTER =
			"tigase.server.xmppclient.SeeOtherHostHashed";

	// default properties
	public static final String CM_SEE_OTHER_HOST_DEFAULT_HOST =
			CM_SEE_OTHER_HOST_CLASS_PROP_KEY + "/" + "default-host";
	public static final String CM_SEE_OTHER_HOST_DEFAULT_PORT =
			CM_SEE_OTHER_HOST_CLASS_PROP_KEY + "/" + "default-port";
	public static final String CM_SEE_OTHER_HOST_ACTIVE =
			CM_SEE_OTHER_HOST_CLASS_PROP_KEY + "/" + "active";

	public static enum Phase {
		OPEN, LOGIN, OTHER
	}


	// ~--- methods -------------------------------------------------------------
	/**
	 * Finds an appropriate host for a given JID
	 *
	 * @param jid
	 *          is a user JID extracted from the stream open attributes
	 * @param host
	 *          is "this" host to which the user is now connected and which calls
	 *          the method
	 * @return BareJID of possible host to which the user should connect or NULL
	 */
	BareJID findHostForJID(BareJID jid, BareJID host);

	/**
	 * Sets list of available nodes in cluster environment
	 *
	 * @param nodes current list of nodes
	 */
	void setNodes(List<JID> nodes);

	// ~--- properties ----------------------------------------------------------
	void getDefaults( Map<String, Object> defs, Map<String, Object> params );

	void setProperties( Map<String, Object> props );

	/**
	 * Returns Element object containing stream:error message
	 *
	 * @param xmlns xml namespace of the element
	 * @param destination BareJID address of the redirect destination
	 * @return element containing stream:error message
	 */
	Element getStreamError( String xmlns, BareJID destination );

	/**
	 * Performs check whether redirect is enabled in the given phase
	 * by default see-other-host redirect is only active in stream:open phase
	 *
	 * @param vHost vHost for which redirection should be performed
	 * @param ph phase for which the check should be performed
	 * @return boolean value indicating whether to perform or not redirect for
	 * the phase passed as argument
	 */
	boolean isEnabled(VHostItem vHost, Phase ph);
}
