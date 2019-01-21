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

import tigase.eventbus.EventBus;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;

/**
 * {@link RosterFactory} is an factory that is responsible for creation appropriate instance of {@link RosterAbstract}
 * class
 * <br>
 * Created: Thu Sep 4 18:33:11 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class RosterFactory {

	/** Key used to configure class name holding roster implementation */
	public static final String ROSTER_IMPL_PROP_KEY = "roster-implementation";
	/** Default roster implementation class - {@link RosterFactory} */
	public static final String ROSTER_IMPL_PROP_VAL = RosterFlat.class.getCanonicalName();
	public static String defaultRosterImplementation = ROSTER_IMPL_PROP_VAL;
	/** Holds shared implementation of {@link RosterAbstract} */
	private static RosterAbstract shared = null;
	/**
	 * Creates new instance of class implementing {@link RosterAbstract} - either default one ({@link RosterFlat}) or
	 * the one configured with <em>"roster-implementation"</em> property.
	 *
	 * @param shared_impl determines whether to returns shared or non shared implementation
	 *
	 * @return new instance of class implementing {@link RosterAbstract}
	 */
	public static RosterAbstract getRosterImplementation(boolean shared_impl) {
		try {
			if (shared_impl) {
				return shared;
			}
			return newRosterInstance(defaultRosterImplementation);
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			return null;
		}
	}

	public static RosterAbstract newRosterInstance(String class_name)
			throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		return (RosterAbstract) Class.forName(class_name).newInstance();
	}

	@tigase.kernel.beans.Bean(name = "rosterFactory", exportable = true, active = true)
	public static class Bean
			implements Initializable, UnregisterAware {

		@ConfigField(desc = "Roster implementation class", alias = ROSTER_IMPL_PROP_KEY)
		private String defaultRosterImplementation = ROSTER_IMPL_PROP_VAL;

		@Inject
		private EventBus eventBus;

		public Bean() {

		}

		public void setDefaultRosterImplementation(String defaultRosterImplementation) {
			this.defaultRosterImplementation = defaultRosterImplementation;
			RosterFactory.defaultRosterImplementation = defaultRosterImplementation;
			try {
				RosterFactory.shared = newRosterInstance(defaultRosterImplementation);
			} catch (ClassNotFoundException | IllegalAccessException | InstantiationException ex) {
				throw new RuntimeException(ex);
			}
			if (eventBus != null && shared != null) {
				synchronized (RosterFactory.class) {
					shared.setEventBus(eventBus);
				}
			}
		}

		public void setEventBus(EventBus eventBus) {
			this.eventBus = eventBus;
			synchronized (RosterFactory.class) {
				if (shared != null) {
					shared.setEventBus(eventBus);
				}
			}
		}

		@Override
		public void beforeUnregister() {
			if (shared != null) {
				shared.setEventBus(null);
			}
		}

		public void initialize() {
			if (shared == null) {
				try {
					RosterFactory.shared = newRosterInstance(defaultRosterImplementation);
				} catch (ClassNotFoundException | IllegalAccessException | InstantiationException ex) {
					throw new RuntimeException(ex);
				}
				if (eventBus != null && shared != null) {
					synchronized (RosterFactory.class) {
						shared.setEventBus(eventBus);
					}
				}
			}
		}
	}
}

