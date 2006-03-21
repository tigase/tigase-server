/*  Package Tigase XMPP/Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <artur.hefczyc@gmail.com>
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
 * $Author$
 * $Date$
 */

package tigase.xmpp;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import tigase.server.Packet;

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
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class XMPPProcessor
	implements XMPPProcessorIfc, Comparable<XMPPProcessor> {

	private static XMPPProcessor inst = null;

	protected XMPPProcessor() {	inst = this; }

	public String[] supElements() { return null; }

  public String[] supNamespaces() { return null; }

  public String[] supStreamFeatures(final XMPPResourceConnection session)
	{ return null; }

  public String[] supDiscoFeatures(final XMPPResourceConnection session)
	{ return null; }

  public boolean isSupporting(final String element, final String ns) {
    String[] impl_elements = supElements();
    String[] impl_xmlns = supNamespaces();
    if (impl_elements != null && impl_xmlns != null) {
      for (int i = 0; i < impl_elements.length && i < impl_xmlns.length; i++) {
        if (impl_elements[i].equals(element) && impl_xmlns[i].equals(ns)) {
          return true;
        } // end of if (ELEMENTS[i].equals(element) && XMLNSS[i].equals(ns))
      } // end of for (int i = 0; i < ELEMENTS.length; i++)
    } // end of if (impl_elements != null && impl_xmlns != null)
    return false;
  }

  public static XMPPProcessor getInstance() { return inst; }

	public void stopped(final XMPPResourceConnection session,
		final Queue<Packet> results) {
		// By default do nothing...
	}

	// Implementation of java.lang.Comparable

  /**
   * Method <code>compareTo</code> is used to perform
   *
   * @param proc an <code>XMPPProcessor</code> value
   * @return an <code>int</code> value
   */
  public final int compareTo(final XMPPProcessor proc) {
    return
      getClass().getSimpleName().compareTo(proc.getClass().getSimpleName());
  }

}// XMPPProcessor
