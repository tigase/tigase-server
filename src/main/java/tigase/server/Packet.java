/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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

package tigase.server;

import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

/**
 * Class Packet
 *
 * Represent one XMPP packet.
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Packet {

  private static final String ERROR_NS = "urn:ietf:params:xml:ns:xmpp-stanzas";

	private Set<String> processorsIds = new LinkedHashSet<String>(4, 0.9f);

	/**
	 * Constant <code>OLDTO</code> is kind of hack to store old request address
	 * when the packet is processed by the session manager. The problem is that
	 * SessionManager may work for many virtual domains but has just one real
	 * address. So to forward the request to the SessionManager the 'to' address
	 * is replaced with the real SessionManager address. The response however
	 * needs to be sent with the 'from' address as the original request was 'to'.
	 * Therefore 'oldto' attribute temporarly stores the old 'to' address
	 * and after the packet processing is completed the 'from' attribute
	 * is replaced with original 'to' value.
	 *
	 */
	public static final String OLDTO = "oldto";
	public static final String OLDFROM = "oldfrom";

	protected Element elem;
	private StanzaType type;
	private boolean routed;
	private JID packetTo = null;
	private JID packetFrom = null;
	private JID stanzaTo = null;
	private JID stanzaFrom = null;
	private String stanzaId = null;
	private Permissions permissions = Permissions.NONE;
	private String packetToString = null;
	private String packetToStringSecure = null;
	private Priority priority = Priority.NORMAL;

	public static Packet packetInstance(Element elem) throws TigaseStringprepException {
		if (elem.getName() == Message.ELEM_NAME) {
			return new Message(elem);
		}
		if (elem.getName() == Presence.ELEM_NAME) {
			return new Presence(elem);
		}
		if (elem.getName() == Iq.ELEM_NAME) {
			return new Iq(elem);
		}
		return new Packet(elem);
	}

	public static Packet packetInstance(Element elem, JID stanzaFrom, JID stanzaTo) {
		if (elem.getName() == Message.ELEM_NAME) {
			return new Message(elem, stanzaFrom, stanzaTo);
		}
		if (elem.getName() == Presence.ELEM_NAME) {
			return new Presence(elem, stanzaFrom, stanzaTo);
		}
		if (elem.getName() == Iq.ELEM_NAME) {
			return new Iq(elem, stanzaFrom, stanzaTo);
		}
		return new Packet(elem, stanzaFrom, stanzaTo);
	}

	public static Packet packetInstance(String el_name, String from, String to,
			StanzaType type) throws TigaseStringprepException {
		Element elem = new Element(el_name,
			new String[] {"from", "to", "type"},
			new String[] {from, to, type.toString()});
		return packetInstance(elem);
	}

  protected Packet(final Element elem) throws TigaseStringprepException {
		setElem(elem);
		initVars();
	}

  protected Packet(final Element elem, JID stanzaFrom, JID stanzaTo) {
		setElem(elem);
		initVars(stanzaFrom, stanzaTo);
	}

	public Packet copyElementOnly() {
		Element res_elem = elem.clone();
		Packet result = packetInstance(res_elem, getStanzaFrom(), getStanzaTo());
		return result;
	}

	public void initVars(JID stanzaFrom, JID stanzaTo) {
		this.stanzaTo = stanzaTo;
		this.stanzaFrom = stanzaFrom;
		stanzaId = elem.getAttribute("id");
		packetToString = null;
	}

	public void initVars() throws TigaseStringprepException {
		String tmp = elem.getAttribute("to");
		if (tmp != null) {
			stanzaTo = new JID(tmp);
		}
		tmp = elem.getAttribute("from");
		if (tmp != null) {
			stanzaFrom = new JID(tmp);
		}
		stanzaId = elem.getAttribute("id");
		packetToString = null;
	}

	private void setElem(Element elem) {
		if (elem == null) {
			throw new NullPointerException();
		} // end of if (elem == null)
		this.elem = elem;
		if (elem.getAttribute("type") != null) {
			type = StanzaType.valueof(elem.getAttribute("type"));
		} else {
			type = null;
		} // end of if (elem.getAttribute("type") != null) else
		if (elem.getName() == "cluster") {
			setPriority(Priority.CLUSTER);
		}
		if ((elem.getName() == "presence")
				&& (type == null
				|| type == StanzaType.available
				|| type == StanzaType.unavailable
				|| type == StanzaType.probe)) {
			setPriority(Priority.PRESENCE);
		}
		if (elem.getName().equals("route")) {
			routed = true;
		} else {
			routed = false;
		} // end of if (elem.getName().equals("route")) else
	}

	public void setPriority(Priority priority) {
		this.priority = priority;
	}

	public Priority getPriority() {
		return priority;
	}

	public String getStanzaId() {
		return stanzaId;
	}

	public void setPermissions(Permissions perm) {
		packetToString = null;
		packetToStringSecure = null;
		permissions = perm;
	}

	public Permissions getPermissions() {
		return permissions;
	}

	public void processedBy(String id) {
		processorsIds.add(id);
	}

	public boolean wasProcessed() {
		return processorsIds.size() > 0;
	}

	public boolean wasProcessedBy(String id) {
		return processorsIds.contains(id);
	}

	public Set<String> getProcessorsIds() {
		return processorsIds;
	}

	public StanzaType getType() {
		return type;
	}

	public Element getElement() {
		return elem;
	}

	public String getElemName() {
		return elem.getName();
	}

	public String getXMLNS() {
		return elem.getXMLNS();
	}

	public boolean isXMLNS(String elementPath, String xmlns) {
		String this_xmlns = elem.getXMLNS(elementPath);
		if (this_xmlns == xmlns) {
			return true;
		}
		return false;
	}

	public boolean isElement(String name, String xmlns) {
		return elem.getName() == name && xmlns == elem.getXMLNS();
	}

	public void setPacketTo(JID to) {
		this.packetTo = to;
	}

	public JID getTo() {
		return packetTo != null ? packetTo : stanzaTo;
	}

	public BareJID getToBareJID() {
		return packetTo != null ? packetTo.getBareJID()
				: (stanzaTo != null ? stanzaTo.getBareJID() : null);
	}

	public String getToHost() {
		return packetTo != null ? packetTo.getDomain()
				: (stanzaTo != null ? stanzaTo.getDomain() : null);
	}

	public String getToNick() {
		return packetTo != null ? packetTo.getLocalpart()
				: (stanzaTo != null ? stanzaTo.getLocalpart() : null);
	}

	public void setPacketFrom(JID from) {
		this.packetFrom = from;
	}

	public JID getFrom() {
		return packetFrom != null ? packetFrom : stanzaFrom;
	}

	/**
   * Returns packet destination address.
	 * @return
	 */
  public JID getStanzaTo() {
		return stanzaTo;
  }

	public String getStanzaToHost() {
		return stanzaTo != null ? stanzaTo.getDomain() : null;
	}

	public String getStanzaToNick() {
		return stanzaTo != null ? stanzaTo.getLocalpart() : null;
	}

	public String getAttribute(String key) {
		return elem.getAttribute(key);
	}

	public String getAttribute(String path, String attr_name) {
		return elem.getAttribute(path, attr_name);
	}

  /**
   * Returns packet source address.
	 * @return
	 */
  public JID getStanzaFrom() {
    return stanzaFrom;
  }

	public String getElemCData(final String path) {
		return elem.getCData(path);
	}

	public List<Element> getElemChildren(final String path) {
		return elem.getChildren(path);
	}

	public String getElemCData() {
		return elem.getCData();
	}

//  public byte[] getByteData() {
//    return elem.toString().getBytes();
//  }
//
//  public String getStringData() {
//    return elem.toString();
//  }
//
//  public char[] getCharData() {
//    return elem.toString().toCharArray();
//  }

	@Override
	public String toString() {
		if (packetToString == null) {
			packetToString = ", data=" + elem.toString() + 
							", XMLNS="+elem.getXMLNS() +
							", priority="+priority;
		}
		return "from=" + packetFrom + ", to=" + packetTo + packetToString;
	}

	public String toStringSecure() {
		if (packetToStringSecure == null) {
			packetToStringSecure = ", data=" + elem.toStringSecure() +
							", XMLNS="+elem.getXMLNS() +
							", priority="+priority;
		}
		return "from=" + packetFrom + ", to=" + packetTo + packetToStringSecure;
	}

	public String toString(boolean secure) {
		if (secure) {
			return toStringSecure();
		} else {
			return toString();
		}
	}

	public boolean isRouted() {
		return routed;
	}

	public Packet unpackRouted() throws TigaseStringprepException {
		Packet result = packetInstance(elem.getChildren().get(0));
		result.setPacketTo(getTo());
		result.setPacketFrom(getFrom());
		return result;
	}

// 	public Packet packRouted(final String from, final String to) {
// 		Element routed = new Element("route", null, new String[] {"to", "from"},
// 			new String[] {to, from});
// 		routed.addChild(elem);
// 		return new Packet(routed);
// 	}

	public Packet packRouted() {
		Element routedp = new Element("route", new String[] {"to", "from"},
			new String[] {getTo().toString(), getFrom().toString()});
		routedp.addChild(elem);
		return packetInstance(routedp, getFrom(), getTo());
	}

//	public Packet swapFromTo(Element el) throws TigaseStringprepException {
//		Packet packet = packetInstance(el);
//		packet.setPacketTo(getFrom());
//		packet.setPacketFrom(getTo());
//		return packet;
//	}

	public Packet swapFromTo(Element el, JID stanzaFrom, JID stanzaTo) {
		Packet packet = packetInstance(el, stanzaFrom, stanzaTo);
		packet.setPacketTo(getFrom());
		packet.setPacketFrom(getTo());
		return packet;
	}

	public Packet swapFromTo() {
		Element el = elem.clone();
		Packet packet = packetInstance(el, getStanzaFrom(), getStanzaTo());
		packet.setPacketTo(getFrom());
		packet.setPacketFrom(getTo());
		return packet;
	}

	public String getErrorCondition() {
		List<Element> children = elem.getChildren(elem.getName() + "/error");
		if (children != null) {
			for (Element cond: children) {
				if (!cond.getName().equals("text")) {
					return cond.getName();
				} // end of if (!cond.getName().equals("text"))
			} // end of for (Element cond: children)
		} // end of if (children == null) else
		return null;
	}

// 	public Packet errorResult(final String errorType, final String errorCondition,
// 			final String errorText, final boolean includeOriginalXML) {
// 		return errorResult(errorType, null, errorCondition, errorText, includeOriginalXML);
// 	}

	public Packet errorResult(final String errorType, final Integer errorCode,
		final String errorCondition, final String errorText,
		final boolean includeOriginalXML) {
		Element reply = new Element(elem.getName());
		reply.setAttribute("type", StanzaType.error.toString());
		if (getStanzaFrom() != null) {
			reply.setAttribute("to", getStanzaFrom().toString());
		} // end of if (getElemFrom() != null)
		if (getStanzaTo() != null) {
			reply.setAttribute("from", getStanzaTo().toString());
		} // end of if (getElemTo() != null)
		if (getStanzaId() != null) {
			reply.setAttribute("id", getStanzaId());
		} // end of if (getElemId() != null)
		if (includeOriginalXML) {
			reply.addChildren(elem.getChildren());
		} // end of if (includeOriginalXML)
		if (getAttribute(OLDTO) != null) {
			reply.setAttribute(OLDTO, getAttribute(OLDTO));
		}
		if(getAttribute("xmlns") != null){
			reply.setAttribute("xmlns", getAttribute("xmlns"));
		}
		Element error = new Element("error");
		if(errorCode != null) {
			error.setAttribute("code", errorCode.toString());
		}
		error.setAttribute("type", errorType);
		Element cond = new Element(errorCondition);
		cond.setXMLNS(ERROR_NS);
		error.addChild(cond);
		if (errorText != null) {
			Element t = new Element("text", errorText,
				new String[] {"xml:lang", "xmlns"},
				new String[] {"en", ERROR_NS});
			error.addChild(t);
		} // end of if (text != null && text.length() > 0)
		reply.addChild(error);
		return swapFromTo(reply, getStanzaTo(), getStanzaFrom());
	}

	public Packet okResult(final String includeXML, final int originalXML) {
		Element reply = new Element(elem.getName());
		reply.setAttribute("type", StanzaType.result.toString());
		if (getStanzaFrom() != null) {
			reply.setAttribute("to", getStanzaFrom().toString());
		} // end of if (getElemFrom() != null)
		if (getStanzaTo() != null) {
			reply.setAttribute("from", getStanzaTo().toString());
		} // end of if (getElemFrom() != null)
		if (getStanzaId() != null) {
			reply.setAttribute("id", getStanzaId());
		} // end of if (getElemId() != null)
		if (getAttribute(OLDTO) != null) {
			reply.setAttribute(OLDTO, getAttribute(OLDTO));
		}
		Element old_child = elem;
		Element new_child = reply;
		for (int i = 0; i < originalXML; i++) {
			old_child = old_child.getChildren().get(0);
			Element tmp = new Element(old_child.getName());
			tmp.setXMLNS(old_child.getXMLNS());
			new_child.addChild(tmp);
			new_child = tmp;
		} // end of for (int i = 0; i < originalXML; i++)
		if (includeXML != null) {
			new_child.setCData(includeXML);
		} // end of if (includeOriginalXML)
		Packet result = swapFromTo(reply, getStanzaTo(), getStanzaFrom());
		result.setPriority(priority);
		return result;
	}

	public Packet okResult(final Element includeXML, final int originalXML) {
		Element reply = new Element(elem.getName());
		reply.setAttribute("type", StanzaType.result.toString());
		if (getStanzaFrom() != null) {
			reply.setAttribute("to", getStanzaFrom().toString());
		} // end of if (getElemFrom() != null)
		if (getStanzaTo() != null) {
			reply.setAttribute("from", getStanzaTo().toString());
		} // end of if (getElemFrom() != null)
		if (getStanzaId() != null) {
			reply.setAttribute("id", getStanzaId());
		} // end of if (getElemId() != null)
		if (getAttribute(OLDTO) != null) {
			reply.setAttribute(OLDTO, getAttribute(OLDTO));
		}
		Element old_child = elem;
		Element new_child = reply;
		for (int i = 0; i < originalXML; i++) {
			old_child = old_child.getChildren().get(0);
			Element tmp = new Element(old_child.getName());
			tmp.setXMLNS(old_child.getXMLNS());
			new_child.addChild(tmp);
			new_child = tmp;
		} // end of for (int i = 0; i < originalXML; i++)
		if (includeXML != null) {
			new_child.addChild(includeXML);
		} // end of if (includeOriginalXML)
		Packet result = swapFromTo(reply, getStanzaTo(), getStanzaFrom());
		result.setPriority(priority);
		return result;
	}

	public Packet swapElemFromTo() {
		Element copy = elem.clone();
		copy.setAttribute("to", getStanzaFrom().toString());
		copy.setAttribute("from", getStanzaTo().toString());
		Packet result = packetInstance(copy, getStanzaTo(), getStanzaFrom());
		result.setPriority(priority);
		return result;
	}

	public Packet swapElemFromTo(final StanzaType type) {
		Element copy = elem.clone();
		copy.setAttribute("to", getStanzaFrom().toString());
		copy.setAttribute("from", getStanzaTo().toString());
		copy.setAttribute("type", type.toString());
		Packet result = packetInstance(copy, getStanzaTo(), getStanzaFrom());
		result.setPriority(priority);
		return result;
	}

	public boolean isCommand() {
		return false;
	}

	public boolean isServiceDisco() {
		return false;
	}

	public Command getCommand() {
		return null;
	}

	public String debug() {
		return toString()
				+ ", stanzaFrom=" + stanzaFrom + ", stanzaTo=" + stanzaTo;
	}

}
