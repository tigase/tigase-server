/*
 * XMPPProcessor.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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

/**
 * <code>XMPPProcessor</code> abstract class contains basic definition for
 * <em>XMPP</em> processor.
 * To create new processor implementing particular <em>XMPP</em> functionality
 * it is enough to extend this class and implement one abstract method.<br/>
 * Additionally to allow system properly recognize this processor you need also
 * to implement own constructor which sets proper values to parent constructor.
 * You must implement exactly one constructor with zero parameters which calls
 * parent constructor with proper values. Refer to constructor documentation
 * for information about required parameters.<br/>
 * To fully interact with entity connected to the session or with other entities
 * in <em>XMPP</em> network you should be also familiar with
 * <code>addReply(...)</code>, <code>addMessage(...)</code> and
 * <code>addBroadcast(...)</code> methods.<br/>
 * There is also partialy implemented functionality to send messages to entities
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
	protected static final String ALL = "*";

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

	/**
	 * Method <code>compareTo</code> is used to perform
	 *
	 * @param proc an <code>XMPPProcessor</code> value
	 * @return an <code>int</code> value
	 */
	@Override
	public final int compareTo(XMPPImplIfc proc) {
		return getClass().getName().compareTo(proc.getClass().getName());
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public int concurrentQueuesNo() {
		return 2;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 *
	 * @deprecated
	 */
	@Override
	@Deprecated
	public int concurrentThreadsPerQueue() {
		return 1;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public XMPPProcessor getInstance() {
		return this;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 * @param settings
	 *
	 * @throws TigaseDBException
	 */
	@Override
	public void init(Map<String, Object> settings) throws TigaseDBException {}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param list
	 */
	@Override
	public void getStatistics(StatisticsList list) {}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param conn
	 *
	 * @return
	 */
	@Override
	public Authorization canHandle(Packet packet, XMPPResourceConnection conn) {
		Authorization result = null;
		String[][] elemPaths = supElementNamePaths();

		if (elemPaths != null) {

			// This is the new API style
			String[] elemXMLNS    = supNamespaces();
			Set<StanzaType> types = supTypes();

			result = checkPacket(packet, conn, elemPaths, elemXMLNS, types);
		} else {

			// And this is the old API left for backward compatibility with plugins
			// from earlier versions
			if (walk(packet.getElement())) {
				result = Authorization.AUTHORIZED;
			}
		}
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST,
							"XMPPProcessorIfc: {0} ({1}" + ")" + "\n Request: " +
							"{2}, conn: {3}, authorization: {4}", new Object[] {
								this.getClass().getSimpleName(),
								id(), packet, conn, result });
		}

		return result;
	}

	private Authorization checkPacket(Packet packet, XMPPResourceConnection conn,
																		String[][] elemPaths, String[] elemXMLNS,
																		Set<StanzaType> types) {
		Authorization result = null;

		for (int i = 0; i < elemPaths.length; i++) {
			if (packet.isXMLNSStaticStr(elemPaths[i], elemXMLNS[i])) {
				if ((types == null) || types.contains(packet.getType())) {
					result = Authorization.AUTHORIZED;

					break;
				}
			}
		}

		return result;
	}

	private boolean walk(Element elem) {
		boolean result;
		String xmlns = elem.getXMLNS();

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

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public Set<StanzaType> supTypes() {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String[][] supElementNamePaths() {
		return null;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param element
	 * @param ns
	 *
	 * @return
	 */
	@Override
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
				if (((impl_elements[i] == element) || (impl_elements[i] == ALL)) &&
						((impl_xmlns[i] == ns) || (impl_xmlns[i] == ALL))) {
					return true;
				}    // end of if (ELEMENTS[i].equals(element) && XMLNSS[i].equals(ns))
			}      // end of for (int i = 0; i < ELEMENTS.length; i++)
		}        // end of if (impl_elements != null && impl_xmlns != null)

		return false;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param session
	 *
	 * @return
	 */
	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String[] supElements() {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String[] supNamespaces() {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 *
	 * @return
	 */
	@Override
	public Element[] supStreamFeatures(final XMPPResourceConnection session) {
		return null;
	}
}    // XMPPProcessor


//~ Formatted in Tigase Code Convention on 13/02/16
