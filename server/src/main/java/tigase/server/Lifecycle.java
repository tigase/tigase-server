/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2016 "Tigase, Inc." <office@tigase.org>
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
 */
package tigase.server;

/**
 * Generic inteface which should be implemented by every class which can be
 * started/stopped during runtime it Tigase XMPP Server (especially for 
 * components, processors, etc. which instances can be replaced and they
 * need information that it's lifecycle has ended).
 *
 * @author andrzej
 */
public interface Lifecycle {

	void start();
	
	void stop();
	
}
