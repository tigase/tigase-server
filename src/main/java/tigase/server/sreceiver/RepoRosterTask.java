/*  Tigase Project
 *  Copyright (C) 2001-2007
 *  "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.server.sreceiver;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.db.TigaseDBException;
import tigase.db.UserRepository;

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

  private static Logger log =
		Logger.getLogger("tigase.server.sreceiver.RepoRosterTask");

	private boolean loaded = false;
	private UserRepository repository = null;

	private static final String roster_node = "/roster";
	private static final String subscribed_key = "subscribed";
	private static final String owner_key = "owner";
	private static final String admin_key = "admin";
	private static final String moderation_accepted_key = "moderation-accepted";

	private void saveToRepository(RosterItem ri) {
		String repo_node = roster_node + "/" + ri.getJid();
		try {
			repository.setData(getJID(), repo_node, subscribed_key,
				new Boolean(ri.isSubscribed()).toString());
			repository.setData(getJID(), repo_node, owner_key,
				new Boolean(ri.isOwner()).toString());
			repository.setData(getJID(), repo_node, admin_key,
				new Boolean(ri.isAdmin()).toString());
			repository.setData(getJID(), repo_node, moderation_accepted_key,
				new Boolean(ri.isModerationAccepted()).toString());
		} catch (TigaseDBException e) {
			log.log(Level.SEVERE, "Problem saving roster data for: " +
				ri.getJid(), e);
		} // end of try-catch
	}

	private RosterItem loadFromRepository(String jid) {
		String repo_node = roster_node + "/" + jid;
		RosterItem ri = new RosterItem(jid);
		try {
			String tmp = repository.getData(getJID(), repo_node, subscribed_key);
			ri.setSubscribed(parseBool(tmp));
			tmp = repository.getData(getJID(), repo_node, owner_key);
			ri.setOwner(parseBool(tmp));
			tmp = repository.getData(getJID(), repo_node, admin_key);
			ri.setAdmin(parseBool(tmp));
			tmp = repository.getData(getJID(), repo_node, moderation_accepted_key);
			ri.setModerationAccepted(parseBool(tmp));
		} catch (TigaseDBException e) {
			log.log(Level.SEVERE, "Problem loading roster data for: " + jid, e);
		} // end of try-catch
		return ri;
	}

	private void removeFromRepository(RosterItem ri) {
		String repo_node = roster_node + "/" + ri.getJid();
		try {
			repository.removeSubnode(getJID(), repo_node);
		} catch (TigaseDBException e) {
			log.log(Level.SEVERE, "Problem removing from roster data for: " +
				ri.getJid(), e);
		} // end of try-catch
	}

	public RosterItem addToRoster(String jid) {
		RosterItem ri = super.addToRoster(jid);
		saveToRepository(ri);
		return ri;
	}

	public RosterItem removeFromRoster(String jid) {
		RosterItem ri = super.removeFromRoster(jid);
		removeFromRepository(ri);
		return ri;
	}

	public void setRosterItemSubscribed(RosterItem ri, boolean subscribed) {
		super.setRosterItemSubscribed(ri, subscribed);
		saveToRepository(ri);
	}

	public void setRosterItemModerationAccepted(RosterItem ri, boolean accepted) {
		super.setRosterItemModerationAccepted(ri, accepted);
		saveToRepository(ri);
	}

	public void loadRoster() {
		try {
			String[] roster = repository.getSubnodes(getJID(), roster_node);
			if (roster != null) {
				for (String jid: roster) {
					addToRoster(loadFromRepository(jid));
				} // end of for (String jid: roster)
			} // end of if (roster != null)
		} catch (TigaseDBException e) {
			log.log(Level.SEVERE, "Problem loading roster from repository", e);
		} // end of try-catch
	}

	public void setParams(final Map<String, Object> map) {
		super.setParams(map);
		repository = (UserRepository)map.get(USER_REPOSITORY_PROP_KEY);
		if (repository != null && !loaded) {
			loaded = true;
			loadRoster();
		} // end of if (repository != null && !loaded)
	}

} // RepoRosterTask
