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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.server.ext.handlers;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;
import tigase.server.ext.ComponentConnection;
import tigase.server.ext.ComponentIOService;
import tigase.server.ext.ComponentProtocolHandler;
import tigase.server.ext.ExtProcessor;

import tigase.xml.Element;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Oct 31, 2009 3:51:09 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class StreamFeaturesProcessor implements ExtProcessor {

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(StreamFeaturesProcessor.class.getName());
	private static final String EL_NAME = "stream:features";
	private static final String ID = EL_NAME;
	private static final String STARTTLS = "starttls";
	private static final String SASL = "sasl";

	//~--- get methods ----------------------------------------------------------

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public List<Element> getStreamFeatures(ComponentIOService serv,
			ComponentProtocolHandler handler) {
		return null;
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public boolean process(Packet p, ComponentIOService serv, ComponentProtocolHandler handler,
			Queue<Packet> results) {
		if (p.isElement("features", "http://etherx.jabber.org/streams")) {
			log.fine("Received stream features: " + p.toString());

			Element elem = p.getElement();

			if (elem.getChild(STARTTLS) != null) {
				ExtProcessor proc = handler.getProcessor(STARTTLS);

				proc.startProcessing(null, serv, handler, results);

				return true;
			}

			if (elem.getChild("mechanisms") != null) {
				ExtProcessor proc = handler.getProcessor(SASL);

				proc.startProcessing(null, serv, handler, results);

				return true;
			}

			return true;
		}

		return false;
	}

	@Override
	public void startProcessing(Packet p, ComponentIOService serv,
			ComponentProtocolHandler handler, Queue<Packet> results) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
