/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.xmpp;

import java.util.Queue;
import tigase.server.Packet;

/**
 *
 * @author andrzej
 */
public interface XMPPPresenceUpdateProcessorIfc {
	
	void presenceUpdate(XMPPResourceConnection session, Packet packet, Queue<Packet> results) throws NotAuthorizedException;
	
}
