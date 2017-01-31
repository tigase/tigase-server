
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
package tigase.server.xmppserver.proc;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;
import tigase.server.xmppserver.CID;
import tigase.server.xmppserver.S2SConnectionHandlerIfc;
import tigase.server.xmppserver.S2SIOService;

import tigase.util.DNSEntry;
import tigase.util.DNSResolverFactory;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
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

        private static final String ALLOW_PACKETS_FROM_OTHER_DOMAINS_MAP_KEY =
                        "allow-packets-from-other-domains-map";
        private static final String ALLOW_PACKETS_FROM_OTHER_DOMAINS_WITH_SAME_IP_KEY = 
                        "allow-packets-from-other-domains-with-same-ip";
        private static final String ALLOW_PACKETS_FROM_OTHER_DOMAINS_WITH_SAME_IP_WHITELIST_KEY = 
                        "allow-packets-from-other-domains-with-same-ip-whitelist";
        
        private Map<String,String[]> allowedOtherDomainsMap = new ConcurrentHashMap<String,String[]>();
        private boolean allowOtherDomainsWithSameIp = false;
        private String[] allowedOtherDomainsWithSameIpWhitelist = null;
        
        //~--- methods --------------------------------------------------------------

		@Override
		public int order() {
			return 0;
		}
		
        @Override
        public void init(S2SConnectionHandlerIfc<S2SIOService> handler, Map<String, Object> props) {
                super.init(handler, props);
                
                String allowOtherDomainsStr = (String) props.get(ALLOW_PACKETS_FROM_OTHER_DOMAINS_WITH_SAME_IP_KEY);
                if (allowOtherDomainsStr != null) {
                        allowOtherDomainsWithSameIp = Boolean.parseBoolean(allowOtherDomainsStr);
                }
                
                String allowedOtherDomainsWhitelistStr = (String) props.get(ALLOW_PACKETS_FROM_OTHER_DOMAINS_WITH_SAME_IP_WHITELIST_KEY);
                if (allowedOtherDomainsWhitelistStr != null) {
                        allowedOtherDomainsWithSameIpWhitelist = allowedOtherDomainsWhitelistStr.split(",");
                        Arrays.sort(allowedOtherDomainsWithSameIpWhitelist);
                }
                else {
                        allowedOtherDomainsWithSameIpWhitelist = new String[0];
                }

                String allowedOtherDomainsMapStr = (String) props.get(ALLOW_PACKETS_FROM_OTHER_DOMAINS_MAP_KEY);
                allowedOtherDomainsMap.clear();
                if (allowedOtherDomainsMapStr != null) {
                        for (String listOfDomainsStr : allowedOtherDomainsMapStr.split(",")) {
                                String[] listOfDomains = listOfDomainsStr.split(":");
                                Arrays.sort(listOfDomains);
                                for (int i=0; i<listOfDomains.length; i++) {
                                        allowedOtherDomainsMap.put(listOfDomains[i], listOfDomains);
                                }
                        }
                }
        }
        
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
                        if (!isAllowed(p, serv, cid)) {
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
        
        /**
         * Check if incoming packet is allowed on this connection
         * 
         * @param p
         * @param serv
         * @param cid
         * 
         */
        protected boolean isAllowed(Packet p, S2SIOService serv, CID cid) {
                boolean allowed  = serv.isAuthenticated(cid);
                
                // Some servers (e.g. Google) are sending packets from other domains than authenticated domain
                // we should check it if we need to connect to that incompatible domain
                if (!allowed && serv.isAuthenticated()) {
                        String domain = p.getStanzaFrom().getDomain();

                        // here we use mapping as DNS solution will not work in all cases
                        String[] allowedOtherDomainsMapValue = allowedOtherDomainsMap.get(domain);
                        if (allowedOtherDomainsMapValue != null) {
                                ArrayList<CID> authenticatedCids = new ArrayList<CID>(serv.getCIDs());
                                for (CID acid : authenticatedCids) {
                                        if (Arrays.binarySearch(allowedOtherDomainsMapValue, acid.getRemoteHost()) >= 0) {
                                                serv.addCID(cid);
                                                allowed = true;
                                                break;
                                        }
                                }
                        }
                        
                        if (!allowed && allowOtherDomainsWithSameIp) {
                                // we can enable it for all domains or only for whitelisted domains
                                if (allowedOtherDomainsWithSameIpWhitelist.length == 0 
                                        || Arrays.binarySearch(allowedOtherDomainsWithSameIpWhitelist, domain) >= 0) {
                        
                                        try {
                                                DNSEntry[] entries = DNSResolverFactory.getInstance().getHostSRV_Entries(domain);
                                                if (entries != null) {                                                
                                                        String remoteAddress = serv.getRemoteAddress();
                                                        for (DNSEntry entry : entries) {
                                                                if (remoteAddress.equals(entry.getIp())) {
                                                                        serv.addCID(cid);
                                                                        allowed = true;
                                                                        break;
                                                                }
                                                        }
                                                }
                                        } catch (UnknownHostException ex) {
                                                log.log(Level.FINE, "Unknown host for domain: {0}", domain);
                                        }
                                }
                        }
                }
                
                return allowed;
        }
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
