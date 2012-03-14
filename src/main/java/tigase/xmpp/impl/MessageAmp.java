
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

import tigase.conf.Configurable;

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.db.UserNotFoundException;

import tigase.server.Packet;
import tigase.server.amp.AmpFeatureIfc;
import tigase.server.amp.MsgRepository;

import tigase.xml.Element;

import tigase.xmpp.JID;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPPostprocessorIfc;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.sql.SQLException;
import java.util.HashMap;

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Apr 29, 2010 5:00:25 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MessageAmp extends XMPPProcessor implements XMPPPostprocessorIfc, XMPPProcessorIfc {
	private static Logger log = Logger.getLogger(MessageAmp.class.getName());
	private static final String FROM_CONN_ID = "from-conn-id";
	private static final String TO_CONN_ID = "to-conn-id";
	private static final String TO_RES = "to-res";
	private static final String OFFLINE = "offline";
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
	private MsgRepository msg_repo = null;
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
		log.log(Level.CONFIG, "Loaded AMP_JID option: {0} = {1}", new Object[] { AMP_JID_PROP_KEY,
				ampJID });

		String off_val = (String) settings.get(MSG_OFFLINE_PROP_KEY);

		if (off_val == null) {
			off_val = System.getProperty(MSG_OFFLINE_PROP_KEY);
		}

		if ((off_val != null) &&!Boolean.parseBoolean(off_val)) {
			offlineProcessor = null;
			DISCO_FEATURES = new Element[] {
				new Element("feature", new String[] { "var" }, new String[] { XMLNS }) };
		}

		String msg_repo_uri = (String) settings.get(AmpFeatureIfc.AMP_MSG_REPO_URI_PROP_KEY);

		if (msg_repo_uri == null) {
			msg_repo_uri = System.getProperty(AmpFeatureIfc.AMP_MSG_REPO_URI_PROP_KEY);

			if (msg_repo_uri == null) {
				msg_repo_uri = System.getProperty(Configurable.GEN_USER_DB_URI_PROP_KEY);
			}
		}

		if (msg_repo_uri != null) {
                        Map<String, String> db_props = new HashMap<String, String>(4);
                        for (Map.Entry<String, Object> entry : settings.entrySet()) {
                                db_props.put(entry.getKey(), entry.getValue().toString());
                        }
                        
                        // Initialization of repository can be done here and in Store
                        // class so repository related parameters for MsgRepository 
                        // should be specified for AMP plugin and AMP component                        
			msg_repo = MsgRepository.getInstance(msg_repo_uri);

			try {
				msg_repo.initRepository(msg_repo_uri, db_props);
			} catch (SQLException ex) {
				msg_repo = null;
				log.log(Level.WARNING, "Problem initializing connection to DB: ", ex);
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
	 */
	@Override
	public void postProcess(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) {
		if ((offlineProcessor != null) && (session == null)) {
			Element amp = packet.getElement().getChild("amp");

			if ((amp == null) || (amp.getXMLNS() != XMLNS) || (amp.getAttribute("status") != null)) {
				try {
					offlineProcessor.savePacketForOffLineUser(packet, msg_repo);
				} catch (UserNotFoundException ex) {
					if (log.isLoggable(Level.FINEST)) {
						log.finest("UserNotFoundException at trying to save packet for off-line user."
								+ packet);
					}
				}
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
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
			Queue<Packet> results, Map<String, Object> settings)
			throws XMPPException {
		if (packet.getElemName() == "presence") {
			if ((offlineProcessor != null) && offlineProcessor.loadOfflineMessages(packet, session)) {
				try {
					Queue<Packet> packets = offlineProcessor.restorePacketForOffLineUser(session, msg_repo);

					if (packets != null) {
						if (log.isLoggable(Level.FINER)) {
							log.finer("Sending off-line messages: " + packets.size());
						}

						results.addAll(packets);
					}    // end of if (packets != null)
				} catch (UserNotFoundException e) {
					log.info("Something wrong, DB problem, cannot load offline messages. " + e);
				}      // end of try-catch
			}
		} else {
			Element amp = packet.getElement().getChild("amp", XMLNS);

			if ((amp == null) || (amp.getAttribute("status") != null)) {
				messageProcessor.process(packet, session, repo, results, settings);
			} else {
				Packet result = packet.copyElementOnly();

				result.setPacketTo(ampJID);
				results.offer(result);

				if (session == null) {
					result.getElement().addAttribute(OFFLINE, "1");

					return;
				}

				if (session.isUserId(packet.getStanzaTo().getBareJID())) {
					result.getElement().addAttribute(TO_CONN_ID, session.getConnectionId().toString());
					result.getElement().addAttribute(TO_RES, session.getResource());
				} else {
					JID connectionId = session.getConnectionId();

					if (connectionId.equals(packet.getPacketFrom())) {
						result.getElement().addAttribute(FROM_CONN_ID, connectionId.toString());
					}
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
