
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
package tigase.xmpp.impl;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;

import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.JID;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPPostprocessorIfc;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorAbstract;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Apr 29, 2010 5:00:25 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MessageAmp extends XMPPProcessor
		implements XMPPPostprocessorIfc, XMPPProcessorIfc {
	private static Logger log = Logger.getLogger(MessageAmp.class.getName());
	private static final String AMP_JID_PROP_KEY = "amp-jid";
	private static final String MSG_OFFLINE_PROP_KEY = "msg-offline";
	private static final String XMLNS = "http://jabber.org/protocol/amp";
	private static final String ID = "amp";
	private static final String[] ELEMENTS = { "message", "presence" };
	private static final String[] XMLNSS = { "jabber:client", "jabber:client" };
	private static Element[] DISCO_FEATURES = {
		new Element("feature", new String[] { "var" }, new String[] { XMLNS }),
		new Element("feature", new String[] { "var" }, new String[] { "msgoffline" }) };

	//~--- fields ---------------------------------------------------------------

	private JID ampJID = null;
	private OfflineMessages offlineProcessor = new OfflineMessages();
	private Message messageProcessor = new Message();

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String id() {
		return ID;
	}

	/**
	 * Method description
	 *
	 *
	 * @param settings
	 *
	 * @throws TigaseDBException
	 */
	@Override
	public void init(Map<String, Object> settings) throws TigaseDBException {
		super.init(settings);
		ampJID = JID.jidInstanceNS((String) settings.get(AMP_JID_PROP_KEY));

		String off_val = (String) settings.get(MSG_OFFLINE_PROP_KEY);

		if ((off_val != null) &&!Boolean.parseBoolean(off_val)) {
			offlineProcessor = null;
			DISCO_FEATURES = new Element[] {
				new Element("feature", new String[] { "var" }, new String[] { XMLNS }) };
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param session
	 * @param repo
	 * @param results
	 * @param settings
	 */
	@Override
	public void postProcess(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) {
		if (offlineProcessor != null) {
			Element amp = packet.getElement().getChild("amp");

			if ((amp == null) || (amp.getXMLNS() != XMLNS)) {
				offlineProcessor.postProcess(packet, session, repo, results, settings);
			}
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param session
	 * @param repo
	 * @param results
	 * @param settings
	 *
	 * @throws XMPPException
	 */
	@Override
	public void process(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings)
			throws XMPPException {
		if ((packet.getElemName() == "presence") && (offlineProcessor != null)) {
			offlineProcessor.process(packet, session, repo, results, settings);
		} else {
			Element amp = packet.getElement().getChild("amp");

			if ((amp == null) || (amp.getXMLNS() != XMLNS)) {
				messageProcessor.process(packet, session, repo, results, settings);
			} else {
				Packet result = packet.copyElementOnly();

				result.setPacketTo(ampJID);
				results.offer(result);

				if (session == null) {
					return;
				}

				JID connectionId = session.getConnectionId();

				if (connectionId.equals(packet.getPacketFrom())) {
					amp.addAttribute("from-conn-id", connectionId.toString());
				}

				if (session.isUserId(packet.getStanzaTo().getBareJID())) {
					amp.addAttribute("to-conn-id", session.getConnectionId().toString());
				}
			}
		}
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
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String[] supElements() {
		return ELEMENTS;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
