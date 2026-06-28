/*
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
package tigase.db;

import tigase.annotations.TigaseDeprecated;
import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.eventbus.events.StartupFinishedEvent;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.core.Kernel;
import tigase.util.dns.DNSResolverFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

@Deprecated
@TigaseDeprecated(since = "8.5.0", removeIn = "9.0.0")
@Bean(name = "database-deprecated-informer", parent = Kernel.class, active = true, exportable = true)
public class DatabaseDeprecatedInformer implements Initializable {
    @Inject
    private EventBus eventBus;

    public static final Logger log = Logger.getLogger(DatabaseDeprecatedInformer.class.getName());

    public DatabaseDeprecatedInformer() {
    }

    @Override
    public void initialize() {
        eventBus.registerAll(this);
    }

    private static final Set<String> deprecatedDatabases = new ConcurrentSkipListSet<>();

    @Deprecated
    @TigaseDeprecated(since = "8.5.0", removeIn = "9.0.0")
    public static void addDeprecatedDatabase(String db, String uri) {
        deprecatedDatabases.add(db + " :: " + uri);
    }

    @HandleEvent
    @Deprecated
    @TigaseDeprecated(since = "8.5.0", removeIn = "9.0.0")
    public void printDeprecatedDatabaseInformation(StartupFinishedEvent event) {

        // if not this node is being shutdown then do nothing
        if (event.getNode() == null || !DNSResolverFactory.getInstance().getDefaultHost().equals(event.getNode())) {
            return;
        }

        if (!deprecatedDatabases.isEmpty()) {

            StringBuilder sb = new StringBuilder();
            sb.append("\n\n\n\n\tFollowing databases are DEPRECATED and will be removed in future versions:");
            deprecatedDatabases.forEach((item ) -> sb.append("\n\t\t* ").append(item));
            sb.append("\n\n\n\tPlease switch to one of the supported databases (MySQL or PostgreSQL)");
            sb.append("\n\n\n\n\n\n\n");

            log.log(Level.SEVERE, sb.toString());
        }
    }
}
