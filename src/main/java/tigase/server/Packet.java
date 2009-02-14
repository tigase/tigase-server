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
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.disco.XMPPService;

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

	private Set<String> processorsIds = new LinkedHashSet<String>();

	/**
	 * Constant <code>OLDTO</code> is kind of hack to store old request address
	 * when the packet is processed by the session mamaner. The problem is that
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

	private final Element elem;
	private final Command command;
	private final String strCommand;
	private final boolean cmd;
	private final boolean serviceDisco;
	private final StanzaType type;
	private final boolean routed;
	private String to = null;
	private String from = null;
	private Permissions permissions = Permissions.NONE;
	private String packetToString = null;
	private Priority priority = Priority.NORMAL;

  public Packet(final Element elem) {
		if (elem == null) {
			throw new NullPointerException();
		} // end of if (elem == null)
		this.elem = elem;
		if (elem.getName() == "iq") {
			Element child = elem.getChild("command", Command.XMLNS);
			if (child != null) {
				cmd = true;
				strCommand = child.getAttribute("node");
				command = Command.valueof(strCommand);
			} else {
				strCommand = null;
				command = null;
				cmd = false;
			}
			serviceDisco = (isXMLNS("/iq/query", XMPPService.INFO_XMLNS)
				|| isXMLNS("/iq/query", XMPPService.ITEMS_XMLNS));
		} else {
			strCommand = null;
			command = null;
			cmd = false;
			serviceDisco = false;
			if (elem.getName() == "cluster") {
				setPriority(Priority.CLUSTER);
			}
		}
		if (elem.getAttribute("type") != null) {
			type = StanzaType.valueof(elem.getAttribute("type"));
		} else {
			type = null;
		} // end of if (elem.getAttribute("type") != null) else
		if (elem.getName().equals("route")) {
			routed = true;
		} // end of if (elem.getName().equals("route"))
		else {
			routed = false;
		} // end of if (elem.getName().equals("route")) else
	}

	public Packet(String el_name, String from, String to, StanzaType type) {
		this.elem = new Element(el_name,
			new String[] {"from", "to", "type"},
			new String[] {from, to, type.toString()});
		this.type = type;
		this.strCommand = null;
		this.command = null;
		this.cmd = false;
		this.routed = false;
		this.serviceDisco = false;
	}

	public void setPriority(Priority priority) {
		this.priority = priority;
	}

	public Priority getPriority() {
		return priority;
	}

	public void setPermissions(Permissions perm) {
		packetToString = null;
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

	public Command getCommand() {
		return command;
	}

	public String getStrCommand() {
		return strCommand;
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

	public boolean isCommand() {
		return cmd;
	}

	public boolean isServiceDisco() {
		return serviceDisco;
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

	public String getTo() {
		return to != null ? to : getElemTo();
	}

	public void setTo(final String to) {
		packetToString = null;
		this.to = to;
	}

	public String getFrom() {
		return from != null ? from : getElemFrom();
	}

	public void setFrom(final String from) {
		packetToString = null;
		this.from = from;
	}

	public String getAttribute(String key) {
		return elem.getAttribute(key);
	}

	/**
   * Returns packet destination address.
   */
  public String getElemTo() {
    return elem.getAttribute("to");
  }

	public String getAttribute(String path, String attr_name) {
		return elem.getAttribute(path, attr_name);
	}

  /**
   * Returns packet source address.
   */
  public String getElemFrom() {
    return elem.getAttribute("from");
  }

	public String getElemId() {
    return elem.getAttribute("id");
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

  public byte[] getByteData() {
    return elem.toString().getBytes();
  }

  public String getStringData() {
    return elem.toString();
  }

  public char[] getCharData() {
    return elem.toString().toCharArray();
  }

	public String toString() {
		if (packetToString == null) {
			packetToString = ", data=" + elem.toString() + ", XMLNS="+elem.getXMLNS();
		}
		return "to=" + to + ", from=" + from + packetToString;
	}

	public boolean isRouted() {
		return routed;
	}

	public Packet unpackRouted() {
		Packet result = new Packet(elem.getChildren().get(0));
		result.setTo(getTo());
		result.setFrom(getFrom());
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
			new String[] {getTo(), getFrom()});
		routedp.addChild(elem);
		return new Packet(routedp);
	}

	public Packet swapFromTo(final Element el) {
		Packet packet = new Packet(el);
		packet.setTo(getFrom());
		packet.setFrom(getTo());
		return packet;
	}

	public Packet commandResult(Command.DataType cmd_type) {
		Packet result = new Packet(command.createIqCommand(
						getElemTo(),
						getElemFrom(),
						StanzaType.result, elem.getAttribute("id"), strCommand, cmd_type));
		result.setFrom(getTo());
		result.setTo(getFrom());
		return result;
	}

	public static Packet commandResultForm(Packet packet) {
		Packet result = packet.commandResult(Command.DataType.form);
		return result;
	}

	public static Packet commandResultResult(Packet packet) {
		Packet result = packet.commandResult(Command.DataType.result);
		return result;
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
		if (getElemFrom() != null) {
			reply.setAttribute("to", getElemFrom());
		} // end of if (getElemFrom() != null)
		if (getElemTo() != null) {
			reply.setAttribute("from", getElemTo());
		} // end of if (getElemTo() != null)
		if (getElemId() != null) {
			reply.setAttribute("id", getElemId());
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
		return swapFromTo(reply);
	}

	public Packet okResult(final String includeXML, final int originalXML) {
		Element reply = new Element(elem.getName());
		reply.setAttribute("type", StanzaType.result.toString());
		if (getElemFrom() != null) {
			reply.setAttribute("to", getElemFrom());
		} // end of if (getElemFrom() != null)
		if (getElemTo() != null) {
			reply.setAttribute("from", getElemTo());
		} // end of if (getElemFrom() != null)
		if (getElemId() != null) {
			reply.setAttribute("id", getElemId());
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
		return swapFromTo(reply);
	}

	public Packet okResult(final Element includeXML, final int originalXML) {
		Element reply = new Element(elem.getName());
		reply.setAttribute("type", StanzaType.result.toString());
		if (getElemFrom() != null) {
			reply.setAttribute("to", getElemFrom());
		} // end of if (getElemFrom() != null)
		if (getElemTo() != null) {
			reply.setAttribute("from", getElemTo());
		} // end of if (getElemFrom() != null)
		if (getElemId() != null) {
			reply.setAttribute("id", getElemId());
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
		return swapFromTo(reply);
	}

	public Packet swapElemFromTo() {
		Element copy = elem.clone();
		copy.setAttribute("to", getElemFrom());
		copy.setAttribute("from", getElemTo());
		return new Packet(copy);
	}

	public Packet swapElemFromTo(final StanzaType type) {
		Element copy = elem.clone();
		copy.setAttribute("to", getElemFrom());
		copy.setAttribute("from", getElemTo());
		copy.setAttribute("type", type.toString());
		return new Packet(copy);
	}

	public static Packet getMessage(String to, String from, StanzaType type,
		String body, String subject, String thread) {
		Element message = new Element("message",
			new Element[] {new Element("body", body)},
			new String[] {"to", "from", "type"},
			new String[] {to, from, type.toString()});
		if (subject != null) {
			message.addChild(new Element("subject", subject));
		}
		if (thread != null) {
			message.addChild(new Element("thread", thread));
		}
		return new Packet(message);
	}

}
