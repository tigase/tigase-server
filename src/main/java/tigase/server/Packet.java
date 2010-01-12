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
import tigase.xmpp.JID;

/**
 * Objects of this class carry a single XMPP packet (stanza).
 * The XMPP stanza is carried as an XML element in DOM structure by the
 * Packet object which contains some extra information and convenience methods
 * to quickly access the most important stanza information.<p/>
 * The stanza is accessible directly through the <code>getElement()</code> method
 * and then it can be handles as an XML object. <br/>
 * <strong>Please note! Even though the <code>Packet</code> object and carried the
 * stanza <code>Element</code> is not unmodifiable it should be treated as such. This
 * particular <code>Packet</code> can be processed concurrently at the same time in
 * different components or plugins of the Tigase server. Modifying it may lead to
 * unexpected and hard to diagnoze behaviours. Every time you want to change or
 * update the object you should obtaina a copy of it using one of the utility methods:
 * <code>copyElementOnly()</code>, <code>swapFromTo(...)</code>,
 * <code>errorResult(...)</code>, <code>okResult(...)</code>,
 * <code>swapStanzaFromTo(...)</code></strong><p/>
 * There are no public constructors for the class, instead you have to use factory
 * methods: <code>packetInstance(...)</code> which return instance of one of the
 * classes: <code>Iq</code>, <code>Message</code> or <code>Presence</code>.
 * While creating a new <code>Packet</code> instance JIDs are parsed and processed
 * through the stringprep. Hence some of the factory methods may throw
 * <code>TigaseStringprepException</code> exception. You can avoid this by using
 * the methods which accept preparsed JIDs. Reusing preparsed JIDs is highly
 * recommended.
 * <p/>
 * There are 3 kinds of addresses available from the <code>Packet</code> object:
 * <em>PacketFrom/To</em>, <em>StanzaFrom/To</em> and <em>From/To</em>.<br/>
 * <em>Stanza</em> addresses are the normal XMPP addresses parsed from the XML
 * stanza and as a convenience are available through methods as JID objects. This is
 * not only convenient to the developer but also this is important for performance
 * reasons as parsing JID and processing it through stringprep is quite expensive
 * operation so it is better to do it once and reuse the parsed objects. Please note
 * that any of them can be null. Note also. You should avoid parsing stanza JIDs
 * from the XML element in your code as this may impact the server performance.
 * Reuse the JIDs provided from the <code>Packet</code> methods.<br/>
 * <em>Packet</em> addresses are also JID objects but they may contain a different
 * values from the <em>Stanza</em> addresses. These are the Tigase internal
 * addresses used by the server and they usually contain Tigase component source
 * and destination address. In most cases they are used between connection managers
 * and session managers and can be ignored by other code. One advantage of setting
 * <code>PacketFrom</code> address to address of your component
 * (<code>getComponentId()</code>) address is that if there is a packet delivery problem
 * it will be returned back to the sender with apropriate error message.<br/>
 * <em>Simple From/To</em> addresses contains values following the logic: If
 * PacketFrom/To is not null then it contains PacketFrom/To values otherwise it
 * contains StanzaFrom/To values. This is because the Tigase server tries always
 * to deliver and process the <code>Packet</code> using PacketFrom/To addresses if
 * they are null then Stanza addresses are used instead. So these are just convenience
 * methods which allow avoiding extra <code>IFs</code> in the program code and also
 * save some CPU cycles.
 *
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

	public JID getPacketTo() {
		return this.packetTo;
	}

	public void setPacketFrom(JID from) {
		this.packetFrom = from;
	}

	public JID getPacketFrom() {
		return this.packetFrom;
	}

	public JID getTo() {
		return packetTo != null ? packetTo : stanzaTo;
	}

	public JID getFrom() {
		return packetFrom != null ? packetFrom : stanzaFrom;
	}

	/**
	 *
	 * @return
	 * @deprecated use getStanzaTo() instead
	 */
	@Deprecated
	public String getElemTo() {
		return stanzaTo != null ? stanzaTo.toString() : null;
	}

	/**
	 *
	 * @return
	 * @deprecated use getStanzaFrom() instead.
	 */
	@Deprecated
  public String getElemFrom() {
		return stanzaFrom != null ? stanzaFrom.toString() : null;
   }

	/**
   * Returns packet destination address.
	 * @return
	 */
  public JID getStanzaTo() {
		return stanzaTo;
  }

  /**
   * Returns packet source address.
	 * @return
	 */
  public JID getStanzaFrom() {
    return stanzaFrom;
  }

	public String getAttribute(String key) {
		return elem.getAttribute(key);
	}

	public String getAttribute(String path, String attr_name) {
		return elem.getAttribute(path, attr_name);
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

	public Packet packRouted() {
		Element routedp = new Element("route", new String[] {"to", "from"},
			new String[] {getTo().toString(), getFrom().toString()});
		routedp.addChild(elem);
		return packetInstance(routedp, getFrom(), getTo());
	}

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

	public Packet swapStanzaFromTo() {
		Element copy = elem.clone();
		copy.setAttribute("to", getStanzaFrom().toString());
		copy.setAttribute("from", getStanzaTo().toString());
		Packet result = packetInstance(copy, getStanzaTo(), getStanzaFrom());
		result.setPriority(priority);
		return result;
	}

	@Deprecated
	public Packet swapElemFromTo() {
		return swapStanzaFromTo();
	}

	public Packet swapStanzaFromTo(final StanzaType type) {
		Element copy = elem.clone();
		copy.setAttribute("to", getStanzaFrom().toString());
		copy.setAttribute("from", getStanzaTo().toString());
		copy.setAttribute("type", type.toString());
		Packet result = packetInstance(copy, getStanzaTo(), getStanzaFrom());
		result.setPriority(priority);
		return result;
	}

	@Deprecated
	public Packet swapElemFromTo(final StanzaType type) {
		return swapStanzaFromTo(type);
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
