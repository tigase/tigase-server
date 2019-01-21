/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
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
 */
package tigase.xmpp.impl;

import tigase.db.NonAuthUserRepository;
import tigase.kernel.beans.Bean;
import tigase.server.Command;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.xmppsession.PacketDefaultHandler;
import tigase.server.xmppsession.SessionManager;
import tigase.xml.Element;
import tigase.xmpp.*;

import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

/**
 * Describe class JabberIqCommand here.
 * <br>
 * Created: Mon Jan 22 22:41:17 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Bean(name = JabberIqCommand.ID, parent = SessionManager.class, active = true)
public class JabberIqCommand
		extends XMPPProcessor
		implements XMPPProcessorIfc {

	private static final String[][] ELEMENTS = {Iq.IQ_COMMAND_PATH};
	private static final Logger log = Logger.getLogger(JabberIqCommand.class.getName());
	private static final String[] XMLNSS = {Command.XMLNS};
	private static final String XMLNS = Command.XMLNS;
	protected static final String ID = XMLNS;
	private static final Element[] DISCO_FEATURES = {new Element("feature", new String[]{"var"}, new String[]{XMLNS})};

	private PacketDefaultHandler defaultHandler = new PacketDefaultHandler();

	@Override
	public Authorization canHandle(Packet packet, XMPPResourceConnection conn) {
		if (conn == null) {
			return null;
		}
		return super.canHandle(packet, conn);
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void process(final Packet packet, final XMPPResourceConnection session, final NonAuthUserRepository repo,
						final Queue<Packet> results, final Map<String, Object> settings) throws XMPPException {
		if (session == null) {
			return;
		}

		defaultHandler.process(packet, session, repo, results);
	}

	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	@Override
	public String[][] supElementNamePaths() {
		return ELEMENTS;
	}

	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}
}

