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

package tigase.server.sreceiver;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.TigaseDBException;
import tigase.db.UserExistsException;
import tigase.db.UserRepository;

import tigase.server.Packet;

import tigase.util.TigaseStringprepException;

import tigase.xmpp.JID;

import static tigase.server.sreceiver.PropertyConstants.*;
import static tigase.server.sreceiver.TaskCommons.*;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class RepoRosterTask here.
 *
 *
 * Created: Sat May 12 23:41:52 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class RepoRosterTask extends AbstractReceiverTask {
	private static Logger log = Logger.getLogger("tigase.server.sreceiver.RepoRosterTask");
	private static final String roster_node = "/roster";
	private static final String subscribed_key = "subscribed";
	private static final String owner_key = "owner";
	private static final String admin_key = "admin";
	private static final String moderation_accepted_key = "moderation-accepted";

	//~--- fields ---------------------------------------------------------------

	private boolean loaded = false;
	private UserRepository repository = null;

	//~--- methods --------------------------------------------------------------

	@Override
	public RosterItem addToRoster(JID jid) {
		RosterItem ri = super.addToRoster(jid);

		saveToRepository(ri);

		return ri;
	}

	/**
	 * Method description
	 *
	 *
	 * @param results
	 */
	public void destroy(Queue<Packet> results) {
		super.destroy(results);

		try {
			repository.removeUser(getJID().getBareJID());
		} catch (TigaseDBException e) {
			log.log(Level.SEVERE, "Problem removing task data from repository", e);
		}    // end of try-catch
	}

	/**
	 * Method description
	 *
	 *
	 * @throws TigaseDBException
	 */
	public void loadRoster() throws TigaseDBException {
		String[] roster = repository.getSubnodes(getJID().getBareJID(), roster_node);

		if (roster != null) {
			for (String jid : roster) {
				try {
					JID rjid = JID.jidInstance(jid);

					log.fine(getJID() + ": " + " loadin from repository: " + rjid);
					addToRoster(loadFromRepository(rjid));
				}    // end of for (String jid: roster)
						catch (TigaseStringprepException ex) {
					Logger.getLogger(RepoRosterTask.class.getName()).log(Level.SEVERE, null, ex);
				}
			}      // end of for (String jid: roster)
		}        // end of if (roster != null)
	}

	@Override
	public RosterItem removeFromRoster(JID jid) {
		RosterItem ri = super.removeFromRoster(jid);

		if (ri != null) {
			removeFromRepository(ri);
		}

		return ri;
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param map
	 */
	public void setParams(final Map<String, Object> map) {
		if (map.get(USER_REPOSITORY_PROP_KEY) != null) {
			repository = (UserRepository) map.get(USER_REPOSITORY_PROP_KEY);
		}

		if ((repository != null) &&!loaded) {
			try {
				try {
					repository.addUser(getJID().getBareJID());
				} catch (UserExistsException e) {    /* Ignore, this is correct and expected */
				}

				loaded = true;
				loadRoster();
			} catch (TigaseDBException e) {
				log.log(Level.SEVERE, "Problem loading roster from repository", e);
			}                                      // end of try-catch
		}                                        // end of if (repository != null && !loaded)

		super.setParams(map);
	}

	/**
	 * Method description
	 *
	 *
	 * @param ri
	 * @param admin
	 */
	public void setRosterItemAdmin(RosterItem ri, boolean admin) {
		super.setRosterItemAdmin(ri, admin);
		saveToRepository(ri);
	}

	@Override
	public void setRosterItemModerationAccepted(RosterItem ri, boolean accepted) {
		super.setRosterItemModerationAccepted(ri, accepted);
		saveToRepository(ri);
	}

	/**
	 * Method description
	 *
	 *
	 * @param ri
	 * @param owner
	 */
	public void setRosterItemOwner(RosterItem ri, boolean owner) {
		super.setRosterItemOwner(ri, owner);
		saveToRepository(ri);
	}

	@Override
	public void setRosterItemSubscribed(RosterItem ri, boolean subscribed) {
		super.setRosterItemSubscribed(ri, subscribed);
		log.fine(getJID() + ": " + "Updating subscription for " + ri.getJid() + " to "
				+ subscribed);
		saveToRepository(ri);
	}

	//~--- methods --------------------------------------------------------------

	private RosterItem loadFromRepository(JID jid) {
		log.info(getJID() + ": Loading roster item for: " + jid);

		String repo_node = roster_node + "/" + jid;
		RosterItem ri = new RosterItem(jid);

		try {
			String tmp = repository.getData(getJID().getBareJID(), repo_node, subscribed_key);

			log.info(getJID() + ": " + jid + ": subscribed = " + tmp);
			ri.setSubscribed(parseBool(tmp));
			tmp = repository.getData(getJID().getBareJID(), repo_node, owner_key);
			ri.setOwner(parseBool(tmp));
			tmp = repository.getData(getJID().getBareJID(), repo_node, admin_key);
			ri.setAdmin(parseBool(tmp));
			tmp = repository.getData(getJID().getBareJID(), repo_node, moderation_accepted_key);
			ri.setModerationAccepted(parseBool(tmp));
		} catch (TigaseDBException e) {
			log.log(Level.SEVERE, "Problem loading roster data for: " + jid, e);
		}    // end of try-catch

		return ri;
	}

	private void removeFromRepository(RosterItem ri) {
		String repo_node = roster_node + "/" + ri.getJid();

		try {
			repository.removeSubnode(getJID().getBareJID(), repo_node);
		} catch (TigaseDBException e) {
			log.log(Level.SEVERE, "Problem removing from roster data for: " + ri.getJid(), e);
		}    // end of try-catch
	}

	private void saveToRepository(RosterItem ri) {
		log.info(getJID() + ": Saving roster item for: " + ri.getJid());

//  Thread.dumpStack();
		String repo_node = roster_node + "/" + ri.getJid();

		try {
			String tmp = Boolean.valueOf(ri.isSubscribed()).toString();

			log.info(getJID() + ": " + ri.getJid() + ": subscribed = " + tmp);
			repository.setData(getJID().getBareJID(), repo_node, subscribed_key, tmp);
			repository.setData(getJID().getBareJID(), repo_node, owner_key,
					Boolean.valueOf(ri.isOwner()).toString());
			repository.setData(getJID().getBareJID(), repo_node, admin_key,
					Boolean.valueOf(ri.isAdmin()).toString());
			repository.setData(getJID().getBareJID(), repo_node, moderation_accepted_key,
					Boolean.valueOf(ri.isModerationAccepted()).toString());
		} catch (TigaseDBException e) {
			log.log(Level.SEVERE,
					"Problem saving roster data for: " + "JID = " + getJID() + ", node = " + repo_node
						+ ", RosterItem = " + ri.getJid(), e);
		}    // end of try-catch
	}
}    // RepoRosterTask


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
