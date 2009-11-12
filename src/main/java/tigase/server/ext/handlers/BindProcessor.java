/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

package tigase.server.ext.handlers;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;
import tigase.server.Packet;
import tigase.server.ext.ComponentConnection;
import tigase.server.ext.ComponentProtocolHandler;
import tigase.server.ext.ExtProcessor;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPIOService;

import static tigase.server.ext.ComponentProtocolHandler.*;

/**
 * Created: Nov 2, 2009 2:37:18 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class BindProcessor implements ExtProcessor {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger(BindProcessor.class.getName());

	private static final String EL_NAME = "bind";
	private static final String XMLNS = "urn:xmpp:component:0";
	private static final String ID = EL_NAME;
  private static final Element FEATURES =
			new Element(EL_NAME, new String[]{"xmlns"}, new String[]{XMLNS});

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public boolean process(Packet p, XMPPIOService<List<ComponentConnection>> serv,
			ComponentProtocolHandler handler, Queue<Packet> results) {
		if (p.isXMLNS("/iq/bind", XMLNS)) {
			if (p.getType() == StanzaType.set && serv.isAuthenticated()) {
				String hostname = p.getElemCData("/iq/bind/hostname");
				handler.bindHostname(hostname, serv);
				results.offer(new Packet(okResult(p.getElement())));
			} else {
				log.fine("Ok result received: " + p.toString());
			}
			return true;
		}
		if (p.isXMLNS("/iq/unbind", XMLNS)) {
			if (p.getType() == StanzaType.set && serv.isAuthenticated()) {
				String hostname = p.getElemCData("/iq/unbind/hostname");
				handler.unbindHostname(hostname, serv);
				results.offer(new Packet(okResult(p.getElement())));
			} else {
				log.fine("Ok result received: " + p.toString());
			}
			return true;
		}
		return false;
	}

	private Element okResult(Element elem) {
		Element result = elem.clone();
		result.setAttribute("type", "result");
		return result;
	}

	private Element newBindElement(String host, ComponentProtocolHandler handler) {
		Element result = new Element("iq",
				new String[] {"type", "id"}, new String[] {"set", handler.newPacketId("bind")});
		Element bind = new Element(EL_NAME,
				new Element[] {new Element("hostname", host)},
				new String[] {"xmlns"}, new String[] {XMLNS});
		result.addChild(bind);
		return result;
	}

	@Override
	public void startProcessing(Packet p,
			XMPPIOService<List<ComponentConnection>> serv,
			ComponentProtocolHandler handler, Queue<Packet> results) {
		String[] hostnames =
				(String[])serv.getSessionData().get(EXTCOMP_BIND_HOSTNAMES_PROP_KEY);
		if (hostnames != null) {
			for (String host : hostnames) {
				if (!host.isEmpty()) {
					Packet bind_p = new Packet(newBindElement(host, handler));
					log.info("Generating hostname bind packet: " + bind_p.toString());
					results.offer(bind_p);
				} else {
					log.warning("Empty hostname set for bind...");
				}
			}
		}
	}

	@Override
	public List<Element> getStreamFeatures(XMPPIOService<List<ComponentConnection>> serv,
			ComponentProtocolHandler handler) {
		return Arrays.asList(FEATURES);
	}

}
