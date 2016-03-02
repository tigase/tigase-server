/*
 * XMPPProcessor.java
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



package tigase.xmpp;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.TigaseDBException;

import tigase.server.Packet;

import tigase.stats.StatisticsList;

import tigase.xml.Element;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Set;
import tigase.server.ComponentInfo;

/**
 * <code>XMPPProcessor</code> abstract class contains basic definition for
 * <em>XMPP</em> processor.
 * To create new processor implementing particular <em>XMPP</em> functionality
 * it is enough to extend this class and implement one abstract method.<br>
 * Additionally to allow system properly recognise this processor you need also
 * to implement own constructor which sets proper values to parent constructor.
 * You must implement exactly one constructor with zero parameters which calls
 * parent constructor with proper values. Refer to constructor documentation
 * for information about required parameters.<br>
 * To fully interact with entity connected to the session or with other entities
 * in <em>XMPP</em> network you should be also familiar with
 * <code>addReply(...)</code>, <code>addMessage(...)</code> and
 * <code>addBroadcast(...)</code> methods.<br>
 * There is also partially implemented functionality to send messages to entities
 * in other networks like <em>SMTP</em> or other implemented by the server.
 * Once this implementation is finished there will be more information available.
 * If you, however, are interested in this particular feature send a question
 * to author.
 *
 * <p>
 * Created: Tue Oct  5 20:31:23 2004
 * </p>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class XMPPProcessor
				implements XMPPImplIfc {
	/** Field description */
	protected static final String ALL_NAMES = "*";

	/** Field description */
	protected static final String[][] ALL_PATHS = {
		{ "*" }
	};
	protected static ComponentInfo cmpInfo = null;

	{
		cmpInfo = new ComponentInfo( id(), this.getClass() );
	}

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(XMPPProcessor.class.getName());

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 */
	protected XMPPProcessor() {}

	//~--- methods --------------------------------------------------------------

	@Override
	public Authorization canHandle(Packet packet, XMPPResourceConnection conn) {
		Authorization result    = null;
		String[][]    elemPaths = supElementNamePaths();

		if (elemPaths != null) {

			// This is the new API style
			String[]        elemXMLNS = supNamespaces();
			Set<StanzaType> types     = supTypes();

			result = checkPacket(packet, elemPaths, elemXMLNS, types);
		} else {

			// And this is the old API left for backward compatibility with plugins
			// from earlier versions
			if (walk(packet.getElement())) {
				result = Authorization.AUTHORIZED;
			}
		}
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "XMPPProcessorIfc: {0} ({1})\n Request: " +
					"{2}, conn: {3}, authorization: {4}", new Object[] { this.getClass()
					.getSimpleName(),
					id(), packet, conn, result });
		}

		return result;
	}

	@Override
	public final int compareTo(XMPPImplIfc proc) {
		return getClass().getName().compareTo(proc.getClass().getName());
	}

	@Override
	@Deprecated
	public int concurrentThreadsPerQueue() {
		return 1;
	}

	@Override
	public void init(Map<String, Object> settings) throws TigaseDBException {}

	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return null;
	}

	@Override
	public String[][] supElementNamePaths() {
		return null;
	}

	@Override
	@Deprecated
	public String[] supElements() {
		return null;
	}

	@Override
	public String[] supNamespaces() {
		return null;
	}

	@Override
	public Element[] supStreamFeatures(final XMPPResourceConnection session) {
		return null;
	}

	@Override
	public Set<StanzaType> supTypes() {
		return null;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public XMPPProcessor getInstance() {
		return this;
	}

	@Override
	public void getStatistics(StatisticsList list) {}

	@Override
	public ComponentInfo getComponentInfo() {
		if ( cmpInfo == null ){
			cmpInfo = new ComponentInfo( id(), this.getClass() );
		}
		return cmpInfo;
	}

	@Override
	public String toString() {
		return id() + getComponentInfo();
	}

	@Override
	@Deprecated
	public boolean isSupporting(final String element, final String ns) {
		String[] impl_elements = supElements();
		String[] impl_xmlns    = supNamespaces();

		if ((impl_elements != null) && (impl_xmlns != null)) {
			for (int i = 0; (i < impl_elements.length) && (i < impl_xmlns.length); i++) {

				// ******   WARNING!!!! WARNING!!!!    *****
				// This is intentional reference comparison!
				// This method is called very, very often and it is also very expensive
				// therefore all XML element names and xmlns are created using
				// String.intern()
				if (((impl_elements[i] == element) || (impl_elements[i] == ALL_NAMES)) &&
						((impl_xmlns[i] == ns) || (impl_xmlns[i] == ALL_NAMES))) {
					return true;
				}    // end of if (ELEMENTS[i].equals(element) && XMLNSS[i].equals(ns))
			}      // end of for (int i = 0; i < ELEMENTS.length; i++)
		}        // end of if (impl_elements != null && impl_xmlns != null)

		return false;
	}

	//~--- methods --------------------------------------------------------------

	private Authorization checkPacket(Packet packet, String[][] elemPaths,
			String[] elemXMLNS, Set<StanzaType> types) {
		Authorization result   = null;
		boolean       names_ok = elemPaths == ALL_PATHS;

		if (!names_ok) {
			for (int i = 0; i < elemPaths.length; i++) {
				if (packet.isXMLNSStaticStr(elemPaths[i], elemXMLNS[i])) {
					names_ok = true;

					break;
				}
			}
		}
		if (names_ok && ((types == null) || types.contains(packet.getType()))) {
			result = Authorization.AUTHORIZED;
		}

		return result;
	}

	private boolean walk(Element elem) {
		boolean result;
		String  xmlns = elem.getXMLNS();

		if (xmlns == null) {
			xmlns = "jabber:client";
		}
		result = isSupporting(elem.getName(), xmlns);
		if (!result) {
			Collection<Element> children = elem.getChildren();

			if (children != null) {
				for (Element child : children) {
					result = walk(child);
				}    // end of for (Element child: children)
			}      // end of if (children != null)
		}

		return result;
	}
}    // XMPPProcessor


//~ Formatted in Tigase Code Convention on 13/04/24
