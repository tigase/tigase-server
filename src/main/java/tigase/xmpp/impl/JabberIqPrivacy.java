/*
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

import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.NoConnectionIdException;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPPacketFilterIfc;
import tigase.xmpp.XMPPPreprocessorIfc;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterFactory;

import static tigase.xmpp.impl.Privacy.*;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class JabberIqPrivacy here.
 *
 *
 * Created: Mon Oct  9 18:18:11 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JabberIqPrivacy extends XMPPProcessor
		implements XMPPProcessorIfc, XMPPPreprocessorIfc, XMPPPacketFilterIfc {

	/**
	 * Private logger for class instances.
	 */
	private static Logger log = Logger.getLogger(JabberIqPrivacy.class.getName());
	private static final String XMLNS = "jabber:iq:privacy";
	private static final String ID = XMLNS;
	private static final String[] ELEMENTS = { "query" };
	private static final String[] XMLNSS = { XMLNS };
	private static final Element[] DISCO_FEATURES = {
		new Element("feature", new String[] { "var" }, new String[] { XMLNS }) };
	private static final String PRIVACY_INIT_KEY = "privacy-init";
	private static final String LIST_EL_NAME = "list";
	private static final String DEFAULT_EL_NAME = "default";
	private static final String ACTIVE_EL_NAME = "active";
	private static final String PRESENCE_IN_EL_NAME = "presence-in";
	private static final String PRESENCE_OUT_EL_NAME = "presence-out";
	private static final String PRESENCE_EL_NAME = "presence";
	private static RosterAbstract roster_util = RosterFactory.getRosterImplementation(true);
	private static final Comparator<Element> compar = new Comparator<Element>() {
		@Override
		public int compare(Element el1, Element el2) {
			String or1 = el1.getAttribute(ORDER);
			String or2 = el2.getAttribute(ORDER);

			return or1.compareTo(or2);
		}
	};

	//~--- constant enums -------------------------------------------------------

	private enum ITEM_ACTION { allow, deny }

	private enum ITEM_SUBSCRIPTIONS {
		both, to, from, none
	}

	private enum ITEM_TYPE {
		jid, group, subscription, all
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param session
	 * @param repo
	 * @param results
	 */
	@Override
	public void filter(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
			Queue<Packet> results) {
		if ((session == null) ||!session.isAuthorized() || (results == null) || (results.size() == 0)) {
			return;
		}

		for (Iterator<Packet> it = results.iterator(); it.hasNext(); ) {
			Packet res = it.next();

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Checking outbound packet: {0}", res);
			}

			// Always allow presence unavailable to go, privacy lists packets and
			// all other which are allowed by privacy rules
			if ((res.getType() == StanzaType.unavailable) || res.isXMLNS("/iq/query", XMLNS)
					|| allowed(res, session)) {
				continue;
			}

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Packet not allowed to go, removing: {0}", res);
			}

			it.remove();
		}
	}

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
	 * <code>preProcess</code> method checks only incoming stanzas
	 * so it doesn't check for presence-out at all.
	 *
	 * @param packet a <code>Packet</code> value
	 * @param session a <code>XMPPResourceConnection</code> value
	 * @param repo a <code>NonAuthUserRepository</code> value
	 * @param results
	 * @param settings
	 * @return a <code>boolean</code> value
	 */
	@Override
	public boolean preProcess(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) {
		if ((session == null) ||!session.isAuthorized() || packet.isXMLNS("/iq/query", XMLNS)) {
			return false;
		}    // end of if (session == null)

		return !allowed(packet, session);
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
	public void process(final Packet packet, final XMPPResourceConnection session,
			final NonAuthUserRepository repo, final Queue<Packet> results,
				final Map<String, Object> settings)
			throws XMPPException {
		if (session == null) {
			return;
		}    // end of if (session == null)

		try {
			StanzaType type = packet.getType();

			switch (type) {
				case get :
					processGetRequest(packet, session, results);

					break;

				case set :
					processSetRequest(packet, session, results);

					break;

				case result :
				case error :

					// Ignore
					break;

				default :
					results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
							"Request type is incorrect", false));

					break;
			}    // end of switch (type)
		} catch (NotAuthorizedException e) {
			log.log(Level.WARNING,
					"Received privacy request but user session is not authorized yet: {0}", packet);
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		} catch (TigaseDBException e) {
			log.log(Level.WARNING, "Database proble, please contact admin: {0}", e);
			results.offer(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet,
					"Database access problem, please contact administrator.", true));
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

	private boolean allowed(Packet packet, XMPPResourceConnection session) {
		try {

			// If this is a preprocessing phase, always allow all packets to
			// make it possible for the client to communicate with the server.
			if (session.getConnectionId().equals(packet.getPacketFrom())) {
				return true;
			}

                        // allow packets without from attribute and packets with from attribute same as domain name
                        if (packet.getStanzaFrom() == null 
                                || (packet.getStanzaFrom().getLocalpart() == null && session.getBareJID().getDomain().equals(packet.getStanzaFrom().getDomain()))) {
                                return true;
                        }

                        // allow packets without to attribute and packets with to attribute same as domain name
                        if (packet.getStanzaTo() == null 
                                || (packet.getStanzaTo().getLocalpart() == null && session.getBareJID().getDomain().equals(packet.getStanzaTo().getDomain()))) {
                                return true;
                        }
                        
			Element list = Privacy.getActiveList(session);

			if ((list == null) && (session.getSessionData(PRIVACY_INIT_KEY) == null)) {

				// First mark the session as privacy lists loaded for it, this way if there
				// is an exception thrown during database call for this user we won't
				// call it again for the same user.
				session.putSessionData(PRIVACY_INIT_KEY, "");

				String lName = Privacy.getDefaultList(session);

				if (lName != null) {
					Privacy.setActiveList(session, lName);
					list = Privacy.getActiveList(session);
				}                  // end of if (lName != null)
			}                    // end of if (lName == null)

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Active privcy list: {0}", list);
			}

			if (list != null) {
				List<Element> items = list.getChildren();

				if (items != null) {
					Collections.sort(items, compar);

					for (Element item : items) {
						boolean type_matched = false;
						boolean elem_matched = false;
						ITEM_TYPE type = ITEM_TYPE.all;

						if (item.getAttribute(TYPE) != null) {
							type = ITEM_TYPE.valueOf(item.getAttribute(TYPE));
						}              // end of if (item.getAttribute(TYPE) != null)

						String value = item.getAttribute(VALUE);
						BareJID sessionUserId = session.getBareJID();
						JID jid = packet.getStanzaFrom();
						boolean packetIn = true;

						if ((jid == null) || sessionUserId.equals(jid.getBareJID())) {
							jid = packet.getStanzaTo();
							packetIn = false;
						}

						if (jid != null) {
							switch (type) {
								case jid :
									type_matched = jid.toString().contains(value);

									break;

								case group :
									String[] groups = roster_util.getBuddyGroups(session, jid);

									if (groups != null) {
										for (String group : groups) {
											if (type_matched = group.equals(value)) {
												break;
											}    // end of if (group.equals(value))
										}      // end of for (String group: groups)
									}

									break;

								case subscription :
									ITEM_SUBSCRIPTIONS subscr = ITEM_SUBSCRIPTIONS.valueOf(value);

									switch (subscr) {
										case to :
											type_matched = roster_util.isSubscribedTo(session, jid);

											break;

										case from :
											type_matched = roster_util.isSubscribedFrom(session, jid);

											break;

										case none :
											type_matched = ( !roster_util.isSubscribedFrom(session, jid)
													&&!roster_util.isSubscribedTo(session, jid));

											break;

										case both :
											type_matched = (roster_util.isSubscribedFrom(session, jid)
													&& roster_util.isSubscribedTo(session, jid));

											break;

										default :
											break;
									}    // end of switch (subscr)

									break;

								case all :
								default :                                                                        
									type_matched = true;
									break;
							}        // end of switch (type)
						} else {
							if (type == ITEM_TYPE.all) {
								type_matched = true;
							}
						}          // end of if (from != null) else

						if ( !type_matched) {
							continue;
						}          // end of if (!type_matched)

						List<Element> elems = item.getChildren();

						if ((elems == null) || (elems.size() == 0)) {
							elem_matched = true;
						} else {
							for (Element elem : elems) {
								if (
									(packet.getElemName() == PRESENCE_EL_NAME)
									&& ((packetIn && (elem.getName() == PRESENCE_IN_EL_NAME))
									        || ( !packetIn && (elem.getName() == PRESENCE_OUT_EL_NAME)))
									&& ((packet.getType() == null) || (packet.getType() == StanzaType.unavailable))

									)
								
								{
								    elem_matched = true;

									break;
								}

								if (packetIn && (elem.getName() == packet.getElemName())) {
									elem_matched = true;

									break;
								}    // end of if (elem.getName().equals(packet.getElemName()))
							}      // end of for (Element elem: elems)
						}        // end of else

						if ( !elem_matched) {
							break;
						}        // end of if (!elem_matched)

						ITEM_ACTION action = ITEM_ACTION.valueOf(item.getAttribute(ACTION));

						switch (action) {
							case allow :
								return true;

							case deny :
								return false;

							default :
								break;
						}        // end of switch (action)
					}          // end of for (Element item: items)
				}            // end of if (items != null)
			}              // end of if (lName != null)
		} catch (NoConnectionIdException e) {

			// Always allow, this is server dummy session
		} catch (NotAuthorizedException e) {

//    results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
//        "You must authorize session first.", true));
		} catch (TigaseDBException e) {
			log.log(Level.WARNING, "Database problem, please notify the admin. {0}", e);
		}

		return true;
	}

	private void processGetRequest(final Packet packet, final XMPPResourceConnection session,
			final Queue<Packet> results)
			throws NotAuthorizedException, XMPPException, TigaseDBException {
		List<Element> children = packet.getElemChildren("/iq/query");

		if ((children == null) || (children.size() == 0)) {
			String[] lists = Privacy.getLists(session);

			if (lists != null) {
				StringBuilder sblists = new StringBuilder(100);

				for (String list : lists) {
					sblists.append("<list name=\"").append(list).append("\"/>");
				}

				String list = Privacy.getDefaultList(session);

				if (list != null) {
					sblists.append("<default name=\"").append(list).append("\"/>");
				}    // end of if (defList != null)

				list = Privacy.getActiveListName(session);

				if (list != null) {
					sblists.append("<active name=\"").append(list).append("\"/>");
				}    // end of if (defList != null)

				results.offer(packet.okResult(sblists.toString(), 1));
			} else {
				results.offer(packet.okResult((String) null, 1));
			}      // end of if (buddies != null) else
		} else {
			if (children.size() > 1) {
				results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
						"You can retrieve only one list at a time.", true));
			} else {
				Element eList = Privacy.getList(session, children.get(0).getAttribute("name"));

				if (eList != null) {
					results.offer(packet.okResult(eList, 1));
				} else {
					results.offer(Authorization.ITEM_NOT_FOUND.getResponseMessage(packet,
							"Requested list not found.", true));
				}    // end of if (eList != null) else
			}      // end of else
		}        // end of else
	}

	private void processSetRequest(final Packet packet, final XMPPResourceConnection session,
		final Queue<Packet> results)
		throws NotAuthorizedException, XMPPException, TigaseDBException {
		List<Element> children = packet.getElemChildren("/iq/query");

		if ((children != null) && (children.size() == 1)) {
			Element child = children.get(0);

			if (child.getName() == LIST_EL_NAME) {

				// Broken privacy implementation sends list without name set
				// instead of sending BAD_REQUEST error I can just assign
				// 'default' name here.
				String name = child.getAttribute(NAME);

				if ((name == null) || (name.length() == 0)) {
					child.setAttribute(NAME, "default");
				}    // end of if (name == null || name.length() == 0)

				List<Element> items = child.getChildren();
				if (items == null || items.isEmpty()) {
					boolean inUse = name.equals(getDefaultList(session));
					if (!inUse) {
						for (XMPPResourceConnection activeSession : session.getActiveSessions()) {
							inUse |= name.equals(Privacy.getActiveListName(activeSession));
						}
					}
					if (inUse) {
						results.offer(Authorization.CONFLICT.getResponseMessage(packet, null, true));
					}	else {
						Privacy.removeList(session, child);
						results.offer(packet.okResult((String) null, 0));
					}
				}	else {
                                        Authorization error = validateList(session, items);
                                        if (error == null) {
                                                Privacy.addList(session, child);

                                                // updating active list if it's name matches name of updated list
                                                for (XMPPResourceConnection activeSession : session.getActiveSessions()) {
                                                        if (name.equals(Privacy.getActiveListName(activeSession))) {
                                                                Privacy.setActiveList(activeSession, name);
                                                        }
                                                }

                                                results.offer(packet.okResult((String) null, 0));
                                        }
                                        else {
                                                results.offer(error.getResponseMessage(packet, null, true));
                                        }
				}
			}      // end of if (child.getName().equals("list))

			if (child.getName() == DEFAULT_EL_NAME) {
				Privacy.setDefaultList(session, child);
				results.offer(packet.okResult((String) null, 0));
			}      // end of if (child.getName().equals("list))

			if (child.getName() == ACTIVE_EL_NAME) {

				// User selects a different active list
				String listName = child.getAttribute(NAME);
				Element list = Privacy.getList(session, listName);

				if ((listName != null) && (list == null)) {
					results.offer(Authorization.ITEM_NOT_FOUND.getResponseMessage(packet,
							"Selected list not found on the server", true));
				} else {

					// This is either declining of active list use or setting a new active list
					Privacy.setActiveList(session, child.getAttribute(NAME));
					results.offer(packet.okResult((String) null, 0));
				}
			}    // end of if (child.getName().equals("list))
		} else {
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
					"Only 1 element is allowed in privacy set request.", true));
		}    // end of else
	}
        
        public static Authorization validateList(final XMPPResourceConnection session, final List<Element> items) {
                Authorization result = null;
                try {
                        HashSet<Integer> orderSet = new HashSet<Integer>();
                        String[] groups = session != null ? roster_util.getBuddyGroups(session, session.getJID()) : null;
                        
                        for (Element item : items) {
                                ITEM_TYPE type = ITEM_TYPE.all;

                                if (item.getAttribute(TYPE) != null) {
                                        type = ITEM_TYPE.valueOf(item.getAttribute(TYPE));
                                }              // end of if (item.getAttribute(TYPE) != null)
                                
				String value = item.getAttribute(VALUE);

                                switch (type) {
                                        case jid:
                                                // if jid is not valid it will throw exception
                                                JID.jidInstance(value);
                                                break;
                                                       
                                        case group:
                                                boolean matched = false;
                                                
                                                if (groups != null) {
                                                        for (String group : groups) {
                                                                if (matched = group.equals(value)) {
                                                                        break;
                                                                }    // end of if (group.equals(value))
                                                        }      // end of for (String group: groups)
                                                }
                                                
                                                if (!matched) {
                                                        result = Authorization.ITEM_NOT_FOUND;                                                        
                                                }
                                                break;
                                                
                                        case subscription:
                                                // if subscription is not valid it will throw exception
                                                ITEM_SUBSCRIPTIONS.valueOf(value);                                                
                                                break;
                                                
                                        case all:
                                        default:
                                                break;
                                }
                                
                                if (result != null) 
                                        break;
                                
                                // if action is not valid it will throw exception
                                ITEM_ACTION.valueOf(item.getAttribute(ACTION));
                                
                                // checking unique order attribute value
                                Integer order = Integer.parseInt(item.getAttribute(ORDER));
                                if (order == null || order < 0 || !orderSet.add(order)) {
                                        result = Authorization.BAD_REQUEST;
                                }
                                
                                if (result != null) 
                                        break;
                        }
                }
                catch (Exception ex) {
                        // if we get exception list is not valid
                        result = Authorization.BAD_REQUEST;
                }
                
                return result;
        }
        
}    // JabberIqPrivacy


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
