/*
 * MessageCarbons.java
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


package tigase.xmpp.impl;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import tigase.db.NonAuthUserRepository;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.JID;
import tigase.xmpp.NoConnectionIdException;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPPacketFilterIfc;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPStopListenerIfc;

/**
 * MessageCarbons class implements XEP-0280 Message Carbons protocol extension.
 * 
 * @author andrzej
 */
public class MessageCarbons 
				extends XMPPProcessor
				implements XMPPProcessorIfc, XMPPStopListenerIfc, XMPPPacketFilterIfc
				{    

	private static final String ID = "message-carbons";
	
	private static final String XMLNS = "urn:xmpp:carbons:2";
	
	private static final String[][] ELEMENTS = { { "message" }, { "iq", "enable" }, { "iq", "disable" } };
	private static final String[] XMLNSS = { XMPPProcessor.CLIENT_XMLNS, XMLNS, XMLNS };
 	
	private static final Element[] DISCO_FEATURES = { new Element("feature", new String[] { "var" }, new String[] { XMLNS }) };
	
	private static final String ENABLED_KEY = XMLNS+"-enabled";
	
	/**
	 * Returns plugins unique identifier
	 * 
	 * @return 
	 */
	@Override
	public String id() {
		return ID;
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) throws XMPPException {
		if (packet.getElemName() == "iq") {

			boolean enable = packet.getElement().getChild("enable", XMLNS) != null;
			boolean disable = packet.getElement().getChild("disable", XMLNS) != null;
			
			// we can only enable or disable but we cannot do both
			if ((enable && disable) || (!enable && !disable)) {
				results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, null, false));
			}
			else {
				setEnabled(session, enable);
				
				// send result of operation
				results.offer(packet.okResult((Element) null, 0));
			}
		}
		else if (packet.getElemName() == "message" && packet.getStanzaTo() != null) {
						
			Set<JID> enabledJids = (Set<JID>) session.getCommonSessionData(ENABLED_KEY);
			if (enabledJids == null || enabledJids.isEmpty()) {
				// no session has enabled message carbons
				return;
			}
			
			// if this is error delivering forked message we should not fork it
			if (isErrorDeliveringForkedMessage(packet, session))
				return;
			
			if (session.isUserId(packet.getStanzaTo().getBareJID()) && packet.getStanzaTo().getResource() == null) {
				// message is cloned to all resources by Message.java, it violates RFC6121 
				// while it should be copied only to resources with non negative priority!!
				// until it is not solved there is no need to fork messages
				
/*				// we need to fork this message
				JID sessionJid = session.getJID();	
				
				for (JID jid : enabledJids) {
					// do not fork if message would be sent to this connection by default
					if (sessionJid.equals(jid))
						continue;
					
					
					Packet msgClone = Packet.packetInstance(packet.getElement().clone(), packet.getStanzaFrom(), jid);//packet.copyElementOnly();
					msgClone.setPacketFrom(packet.getPacketTo());
					
					try {						
						msgClone.setPacketTo(session.getConnectionId(jid));
						
						results.offer(msgClone);
					}
					catch (NoConnectionIdException ex) {
						// no connection for this resource, so this jid needs to
						// be removed from list of enabled resources
						enabledJids.remove(jid);
					}
				}*/
			}
			else {
				// if this is error delivering forked message we should not fork it
				if (isErrorDeliveringForkedMessage(packet, session))
					return;
				
				// if this is private message then do not send carbon copy
				Element privateEl = packet.getElement().getChild("private", XMLNS);
				
				if (privateEl != null) {
					// TODO: is it enought to just remove this element?
					packet.getElement().removeChild(privateEl);
					return;
				}
			
				String type = session.isUserId(packet.getStanzaTo().getBareJID()) ? "received" : "sent";
				JID srcJid = JID.jidInstance(session.getBareJID());
				
				for (JID jid : enabledJids) {
					
					// do not send carbon copy to session to which it is addressed
					// or from which it is sent
					if (session.getJID().equals(jid))
						continue;
					
					// prepare carbon copy of message					
					try {						
						Packet msgClone = prepareCarbonCopy(packet, session, srcJid, jid, type);
						results.offer(msgClone);
					}
					catch (NoConnectionIdException ex) {
						// no connection for this resource, so this jid needs to
						// be removed from list of enabled resources
						enabledJids.remove(jid);
					}
				}
			}
		}
	}

	/**
	 * Return array of element containing supported disco features
	 * 
	 * @param session
	 * @return 
	 */
	@Override
	public Element[] supDiscoFeatures(XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}
	
	/**
	 * Returns array of split path of supported elements
	 * 
	 * @return 
	 */
	@Override
	public String[][] supElementNamePaths() {
		return ELEMENTS;
	}

	/**
	 * Returns array of supported namespaces
	 * 
	 * @return 
	 */
	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}
	
	/**
	 * Prepare packet which is carbon copy of message passed as argument
	 * 
	 * @param packet
	 * @param session
	 * @param srcJid
	 * @param jid
	 * @param type
	 * @return
	 * @throws NoConnectionIdException 
	 */
	private Packet prepareCarbonCopy(Packet packet, XMPPResourceConnection session, 
			JID srcJid, JID jid, String type) throws NoConnectionIdException {
		Packet msgClone = Message.getMessage(srcJid, jid, packet.getType(), null, 
				null, null, packet.getStanzaId());
		
		msgClone.setPacketTo(session.getConnectionId(jid));

		Element received = new Element(type);
		received.setXMLNS(XMLNS);
		msgClone.getElement().addChild(received);

		Element forwarded = new Element("forwarded");
		forwarded.setXMLNS("urn:xmpp:forward:0");
		received.addChild(forwarded);

		forwarded.addChild(packet.getElement().clone());
		
		return msgClone;
		
	}
	
	/**
	 * Add/Remove session JID to set of JIDs with enabled carbon copy protocol
	 * 
	 * @param session
	 * @param value
	 * @throws NotAuthorizedException 
	 */
	private void setEnabled(XMPPResourceConnection session, boolean value) throws NotAuthorizedException {
		synchronized(session.getParentSession()) {
			Set<JID> enabledJids = (Set<JID>) session.getCommonSessionData(ENABLED_KEY);
			if (enabledJids == null && value) {
				enabledJids = new CopyOnWriteArraySet<JID>();
				session.putCommonSessionData(ENABLED_KEY, enabledJids);
			}			
			
			if (value) {
				enabledJids.add(session.getJID());
			}
			else if (enabledJids != null) {
				enabledJids.remove(session.getJID());
			}
		}
	}
	
	/**
	 * Returns true if session is enabled for receiving carbon copy messages
	 * 
	 * @param session
	 * @return
	 * @throws NotAuthorizedException 
	 */
	private boolean isEnabled(XMPPResourceConnection session) throws NotAuthorizedException {
		Set<JID> enabledJids = (Set<JID>) session.getCommonSessionData(ENABLED_KEY);

		return enabledJids != null && enabledJids.contains(session.getJID());
	}

	/**
	 * If session is stopped then disable carbon copy for this session
	 * 
	 * @param session
	 * @param results
	 * @param settings 
	 */
	@Override
	public void stopped(XMPPResourceConnection session, Queue<Packet> results, Map<String, Object> settings) {
		if (session.isAuthorized()) {
			try {
				setEnabled(session, false);
			}
			catch (NotAuthorizedException ex) {
				// ingoring exception, should not happen
			}
		}
	}

	/**
	 * Method processes outgoing packets from SessionManager
	 * 
	 * @param packet
	 * @param session
	 * @param repo
	 * @param results 
	 */
	@Override
	public void filter(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results) {
		if ((session == null) ||!session.isAuthorized() || (results == null) || (results
				.size() == 0)) {
			return;
		}
		
		if (packet.getElemName() != Message.ELEM_NAME)
			return;
		
		for (Iterator<Packet> it = results.iterator(); it.hasNext(); ) {
			Packet res = it.next();
			
			if (res.getElemName() != Message.ELEM_NAME)
				continue;
			
			// if it is 
			if (isErrorDeliveringForkedMessage(packet, session)) {
				it.remove();
			}
			
			Element messageEl = res.getElement();			
			
			Element privateEl = messageEl.getChild("private", XMLNS);

			if (privateEl != null) {
				// TODO: is it enought to just remove this element?
				messageEl.removeChild(privateEl);
			}
		}		
	}
	
	/**
	 * Method returns true if 
	 * 
	 * @param packet
	 * @param session
	 * @return 
	 */
	private boolean isErrorDeliveringForkedMessage(Packet packet, XMPPResourceConnection session) {

		if (!session.isAuthorized() || packet.getStanzaTo() == null)
			return false;
			
		
		try {
			// check if it is error from delivering forked message
			if (packet.getType() == StanzaType.error && packet.getStanzaTo().getResource() == null
					&& session.isUserId(packet.getStanzaTo().getBareJID())) {

				Element messageEl = packet.getElement();

				// it will be error if it contains sent element
				Element sentEl = messageEl.getChild("sent", XMLNS);
				if (sentEl != null) {
					return true;
				}

				// it will also be error if it contains received element
				Element receivedEl = messageEl.getChild("received", XMLNS);
				if (receivedEl != null) {
					return true;
				}
			}
		}
		catch (NotAuthorizedException ex) {
			// should not happens
		}
			
		return false;
	}
	
}
