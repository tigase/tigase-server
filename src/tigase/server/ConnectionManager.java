/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <kobit@users.sourceforge.net>
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
package tigase.server;

import java.util.Map;

/**
 * Describe class ConnectionManager here.
 *
 *
 * Created: Sun Jan 22 22:52:58 2006
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class ConnectionManager extends AbstractMessageReceiver {

	public static final String PROP_KEY = "connections/";
	public static final String PORTS_PROP_KEY = PROP_KEY + "ports";
	public static final String PORT_TYPE_PROP_KEY = "/type";
	public enum PORT_TYPE {accept, connect};
	public static final String PORT_SOCKET_PROP_KEY = "/socket";
	public enum PORT_SOCKET {plain, ssl};
	public static final String PORT_IFC_PROP_KEY = "/ifc";
	public static final String PORT_IFC_PROP_VAL = "*";
	public static final String PORT_CLASS_PROP_KEY = "/class";

	public Map<String, Object> getDefaults() {
		Map<String, Object> props = super.getDefaults();
		int ports_size = 0;
		int[] ports = null;
		int[] plains = getDefPlainPorts();
		if (plains != null) {
			ports_size += plains.length;
		} // end of if (plains != null)
		int[] ssls = getDefSSLPorts();
		if (ssls != null) {
			ports_size += ssls.length;
		} // end of if (ssls != null)
		if (ports_size > 0) {
			ports = new int[ports_size];
		} // end of if (ports_size > 0)
		if (ports != null) {
			int idx = 0;
			if (plains != null) {
				idx = plains.length;
				for (int i = 0; i < idx; i++) {
					ports[i] = plains[i];
					props.put(PROP_KEY + ports[i] + PORT_TYPE_PROP_KEY,
						PORT_TYPE.accept);
					props.put(PROP_KEY + ports[i] + PORT_SOCKET_PROP_KEY,
						PORT_SOCKET.plain);
					props.put(PROP_KEY + ports[i] + PORT_IFC_PROP_KEY,
						PORT_IFC_PROP_VAL);
					props.put(PROP_KEY + ports[i] + PORT_CLASS_PROP_KEY,
						getDefPortClass());
				} // end of for (int i = 0; i < idx; i++)
			} // end of if (plains != null)
			if (ssls != null) {
				for (int i = idx; i < idx + ssls.length; i++) {
					ports[i] = ssls[i-idx];
					props.put(PROP_KEY + ports[i] + PORT_TYPE_PROP_KEY,
						PORT_TYPE.accept);
					props.put(PROP_KEY + ports[i] + PORT_SOCKET_PROP_KEY,
						PORT_SOCKET.ssl);
					props.put(PROP_KEY + ports[i] + PORT_IFC_PROP_KEY,
						PORT_IFC_PROP_VAL);
					props.put(PROP_KEY + ports[i] + PORT_CLASS_PROP_KEY,
						getDefPortClass());
				} // end of for (int i = 0; i < idx + ssls.length; i++)
			} // end of if (ssls != null)
			props.put(PORTS_PROP_KEY, ports);
		} // end of if (ports != null)
		return props;
	}

	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
	}

	protected int[] getDefPlainPorts() {
		return null;
	}

	protected int[] getDefSSLPorts() {
		return null;
	}

	protected String getDefPortClass() {
		return null;
	}

} // ConnectionManager
