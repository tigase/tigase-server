
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
package tigase.server.xmppserver.proc;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;
import tigase.server.xmppserver.CID;
import tigase.server.xmppserver.S2SIOService;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Dec 10, 2010 5:53:57 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class PacketChecker extends S2SAbstractProcessor {

        private static final Logger log = Logger.getLogger(PacketChecker.class.getName());

        //~--- methods --------------------------------------------------------------

        /**
         * Method description
         *
         *
         * @param p
         * @param serv
         * @param results
         *
         * @return
         */
        @Override
        public boolean process(Packet p, S2SIOService serv, Queue<Packet> results) {
                if ((p.getXMLNS() == XMLNS_SERVER_VAL) || (p.getXMLNS() == XMLNS_CLIENT_VAL)) {
                        if ((p.getStanzaFrom() == null) || (p.getStanzaFrom().getDomain().trim().isEmpty())
                                || (p.getStanzaTo() == null) || p.getStanzaTo().getDomain().trim().isEmpty()) {
                                generateStreamError(false, "improper-addressing", serv);

                                return true;
                        }

                        CID cid = new CID(p.getStanzaTo().getDomain(), p.getStanzaFrom().getDomain());

                        // String remote_hostname = (String) serv.getSessionData().get("remote-hostname");
                        if (!serv.isAuthenticated(cid)) {
                                if (log.isLoggable(Level.FINER)) {
                                        log.log(Level.FINER,
                                                "{0}, Invalid hostname from the remote server for packet: "
                                                + "{1}, authenticated domains for this connection: {2}", new Object[]{serv,
                                                        p, serv.getCIDs()});
                                }

                                generateStreamError(false, "invalid-from", serv);

                                return true;
                        }
                } else {
                        if (log.isLoggable(Level.FINER)) {
                                log.log(Level.FINER, "{0}, Invalid namespace for packet: {1}", new Object[]{serv, p});
                        }

                        generateStreamError(false, "invalid-namespace", serv);

                        return true;
                }

                return false;
        }
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
