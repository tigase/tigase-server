
/*
* Tigase Jabber/XMPP Server
* Copyright (C) 2004-2010 "Artur Hefczyc" <artur.hefczyc@tigase.org>
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, version 3 of the License.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. Look for COPYING file in the top folder.
* If not, see http://www.gnu.org/licenses/.
*
* $Rev$
* Last modified by $Author$
* $Date$
 */
package tigase.server.amp;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.JID;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

//~--- classes ----------------------------------------------------------------

/**
 * Created: May 1, 2010 7:44:17 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class ActionAbstract implements ActionIfc {
	protected ActionResultsHandlerIfc resultsHandler = null;

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param params
	 *
	 * @return
	 */
	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		return null;
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param props
	 * @param resultsHandler
	 */
	@Override
	public void setProperties(Map<String, Object> props,
			ActionResultsHandlerIfc resultsHandler) {
		this.resultsHandler = resultsHandler;
	}

	//~--- methods --------------------------------------------------------------

	protected Packet prepareAmpPacket(Packet packet, Element rule) {
		JID old_from = packet.getStanzaFrom();
		JID old_to = packet.getStanzaTo();
		String from_conn_id = packet.getAttribute(FROM_CONN_ID);
		JID new_from = null;

		if (from_conn_id != null) {
			new_from = JID.jidInstanceNS(old_from.getDomain());
		} else {
			new_from = JID.jidInstanceNS(old_to.getDomain());
		}

		// Packet result = Packet.packetInstance(packet.getElement(), new_from, old_from);
		Packet result = packet.copyElementOnly();

		result.initVars(new_from, old_from);

		Element amp = result.getElement().getChild("amp", AMP_XMLNS);

		result.getElement().removeChild(amp);
		amp = new Element("amp", new Element[] { rule }, new String[] { "from", "to", "xmlns",
				"status" }, new String[] { old_from.toString(), old_to.toString(), AMP_XMLNS,
				getName() });
		result.getElement().addChild(amp);
		removeTigasePayload(result);

		if (from_conn_id != null) {
			result.setPacketTo(JID.jidInstanceNS(from_conn_id));
		}

		return result;
	}

	protected void removeTigasePayload(Packet packet) {
		packet.getElement().removeAttribute(TO_CONN_ID);
		packet.getElement().removeAttribute(TO_RES);
		packet.getElement().removeAttribute(OFFLINE);
		packet.getElement().removeAttribute(FROM_CONN_ID);
		packet.getElement().removeAttribute(EXPIRED);
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
