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
package tigase.xmpp.impl.roster;

import tigase.kernel.beans.*;
import tigase.kernel.core.Kernel;
import tigase.server.xmppsession.SessionManager;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.roster.RosterAbstract.SubscriptionType;
import tigase.xmpp.jid.JID;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Describe class DynamicRoster here.
 * <br>
 * Created: Tue Nov 6 11:28:10 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Bean(name = "dynamic-rosters", parent = SessionManager.class, active = false)
public class DynamicRoster
		implements RegistrarBean, Initializable, UnregisterAware {

	private static final EnumSet<SubscriptionType> subs = EnumSet.noneOf(SubscriptionType.class);
	private static DynamicRoster instance;
	private static Logger log = Logger.getLogger("tigase.xmpp.impl.DynamicRoster");
	@Inject(nullAllowed = true)
	private DynamicRosterIfc[] dynamicRosters;
	public static JID[] addBuddies(final XMPPResourceConnection session, final Map<String, Object> settings,
								   JID[] buddies)
			throws NotAuthorizedException, RosterRetrievingException, RepositoryAccessException {
		return addBuddies(session, settings, buddies, subs);
	}

	public static JID[] addBuddies(final XMPPResourceConnection session, final Map<String, Object> settings,
								   JID[] buddies, final EnumSet<SubscriptionType> subscrs)
			throws NotAuthorizedException, RosterRetrievingException, RepositoryAccessException {
		List<JID> result = getBuddiesList(session, settings);

		if (result != null && subscrs != null && !subscrs.isEmpty()) {
			final Iterator<JID> iterator = result.iterator();
			while (iterator.hasNext()) {
				final JID jid = iterator.next();
				Element buddy = getBuddyItem(session, settings, jid);
				String sub = null;
				if (buddy != null && ((sub = buddy.getAttributeStaticStr("subscription")) != null)) {
					try {
						final SubscriptionType subType = SubscriptionType.valueOf(sub.toLowerCase());
						if (!subscrs.contains(subType)) {
							iterator.remove();
						}
					} catch (IllegalArgumentException e) {
						log.log(Level.FINEST, "Illegal dynamic roster element, skipping");
					}
				}
			}
		}

		if (buddies != null) {
			if (result == null) {
				result = new ArrayList<JID>();
			}
			addBuddiesToList(result, buddies);
		}
		if ((result != null) && (result.size() > 0)) {
			return result.toArray(new JID[result.size()]);
		}

		return null;
	}

	public static void addBuddiesToList(List<JID> list, JID[] buddies) {
		for (JID buddy : buddies) {
			if (!list.contains(buddy)) {
				list.add(buddy);
			}
		}
	}

	public static JID[] getBuddies(final XMPPResourceConnection session, final Map<String, Object> settings)
			throws NotAuthorizedException, RosterRetrievingException, RepositoryAccessException {
		List<JID> result = getBuddiesList(session, settings);

		if ((result != null) && (result.size() > 0)) {
			return result.toArray(new JID[result.size()]);
		}

		return null;
	}

	public static List<JID> getBuddiesList(final XMPPResourceConnection session, final Map<String, Object> settings)
			throws NotAuthorizedException, RosterRetrievingException, RepositoryAccessException {
		DynamicRosterIfc[] dynr = getDynamicRosters(settings);

		if (dynr != null) {
			ArrayList<JID> result = new ArrayList<JID>();

			for (DynamicRosterIfc dri : dynr) {
				JID[] buddies = dri.getBuddies(session);

				if (buddies != null) {
					addBuddiesToList(result, buddies);

					// result.addAll(Arrays.asList(buddies));
				}
			}
			if (result.size() > 0) {
				return result;
			}
		}

		return null;
	}

	public static Element getBuddyItem(final XMPPResourceConnection session, final Map<String, Object> settings,
									   JID buddy)
			throws NotAuthorizedException, RosterRetrievingException, RepositoryAccessException {
		DynamicRosterIfc[] dynr = getDynamicRosters(settings);

		if (dynr != null) {
			for (DynamicRosterIfc dri : dynr) {
				Element item = dri.getBuddyItem(session, buddy);

				if (item != null) {
					return item;
				}
			}
		}

		return null;
	}

	public static DynamicRosterIfc[] getDynamicRosters(Map<String, Object> settings) {
		return instance == null ? null : instance.dynamicRosters;
	}

	public static Element getItemExtraData(XMPPResourceConnection session, Map<String, Object> settings, Element item) {
		DynamicRosterIfc[] dynr = getDynamicRosters(settings);

		if (dynr != null) {
			Element result = null;

			for (DynamicRosterIfc dri : dynr) {
				if ((result = dri.getItemExtraData(item)) != null) {
					break;
				}
			}

			return result;
		} else {
			return null;
		}
	}

	public static List<Element> getRosterItems(final XMPPResourceConnection session, final Map<String, Object> settings)
			throws NotAuthorizedException, RosterRetrievingException, RepositoryAccessException {
		DynamicRosterIfc[] dynr = getDynamicRosters(settings);

		if (dynr != null) {
			ArrayList<Element> result = new ArrayList<Element>();

			for (DynamicRosterIfc dri : dynr) {
				List<Element> items = dri.getRosterItems(session);

				if (items != null) {
					result.addAll(items);
				}
			}
			if (result.size() > 0) {
				return result;
			}
		}

		return null;
	}

	public static void setItemExtraData(XMPPResourceConnection session, Map<String, Object> settings, Element item) {
		DynamicRosterIfc[] dynr = getDynamicRosters(settings);

		if (dynr != null) {
			for (DynamicRosterIfc dri : dynr) {
				dri.setItemExtraData(item);
			}
		}
	}

	@Override
	public void register(Kernel kernel) {

	}

	@Override
	public void unregister(Kernel kernel) {

	}

	@Override
	public void beforeUnregister() {
		if (instance == this) {
			instance = null;
		}
	}

	@Override
	public void initialize() {
		instance = this;
	}
}

