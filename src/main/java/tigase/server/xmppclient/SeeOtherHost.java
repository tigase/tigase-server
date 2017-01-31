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
 */
package tigase.server.xmppclient;

import tigase.xmpp.BareJID;

import tigase.util.TigaseStringprepException;
import tigase.vhosts.VHostItem;
import tigase.vhosts.VHostManagerIfc;
import tigase.xml.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.xmpp.JID;

//~--- classes ----------------------------------------------------------------

/**
 * Default and basic implementation of SeeOtherHost returning same host as the
 * initial one
 *
 * @author Wojtek
 */
public class SeeOtherHost implements SeeOtherHostIfc {

	private static final Logger log = Logger.getLogger(SeeOtherHost.class.getName());
	public static final String REDIRECTION_ENABLED = "see-other-host-redirect-enabled";
	
	protected List<BareJID> defaultHost = null;
	private ArrayList<Phase> active = new ArrayList<Phase>();
	protected VHostManagerIfc vHostManager = null;

	@Override
	public BareJID findHostForJID(BareJID jid, BareJID host) {
		if (defaultHost != null && !defaultHost.isEmpty()) {
			return defaultHost.get( 0 );
		} else {
			return host;
		}
	}

	@Override
	public void getDefaults(Map<String, Object> defs, Map<String, Object> params) {
		List<VHostItem.DataType> types = new ArrayList<VHostItem.DataType>();
		types.add(new VHostItem.DataType( REDIRECTION_ENABLED, "see-other-host redirection enabled",
																			 Boolean.class, Boolean.TRUE ) );
		VHostItem.registerData( types );
	}

	@Override
	public void setProperties(final Map<String, Object> props) {
		if (props.containsKey(SeeOtherHostIfc.CM_SEE_OTHER_HOST_ACTIVE)) {
			
			String[] phase =  ((String)props.get(SeeOtherHostIfc.CM_SEE_OTHER_HOST_ACTIVE)).split( ";");
			for ( String ph : phase) {
				try {
					active.add( Phase.valueOf( ph ) );
				} catch ( IllegalArgumentException e ) {
					log.log( Level.FINEST, "unsupported phase configuration item: " + ph + e  );
				}
			}

		} else {
			active.add( Phase.OPEN );
		}
		log.log( Level.CONFIG, props.get( "component-id" ) + " :: see-other-redirect active in: " + Arrays.asList( active ) );

		if ((props.containsKey(SeeOtherHostIfc.CM_SEE_OTHER_HOST_DEFAULT_HOST))
			&& !props.get(SeeOtherHostIfc.CM_SEE_OTHER_HOST_DEFAULT_HOST).toString().trim().isEmpty()) {
			defaultHost = new ArrayList<BareJID>();
			for (String host : ((String) props.get(SeeOtherHostIfc.CM_SEE_OTHER_HOST_DEFAULT_HOST)).split(",")) {
				try {
					defaultHost.add(BareJID.bareJIDInstance(host));
				} catch (TigaseStringprepException ex) {
					log.log(Level.CONFIG, "From JID violates RFC6122 (XMPP:Address Format): ", ex);
				}
			}
			Collections.sort(defaultHost);
		} else {
			defaultHost = null;
		}
	}

	@Override
	public void setNodes(List<JID> nodes) {
		 log.log(Level.CONFIG, "Action invalid for current implementation.");
	}

	@Override
	public boolean isEnabled(VHostItem vHost, Phase ph) {
		return (boolean) vHost.getData( REDIRECTION_ENABLED )
					 && active.contains( ph );
	}

}
