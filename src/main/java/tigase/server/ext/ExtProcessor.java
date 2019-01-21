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
package tigase.server.ext;

import tigase.server.Packet;
import tigase.xml.Element;

import java.util.List;
import java.util.Queue;

/**
 * Created: Oct 1, 2009 8:40:36 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface ExtProcessor {

	String getId();

	List<Element> getStreamFeatures(ComponentIOService serv, ComponentProtocolHandler handler);

	boolean process(Packet p, ComponentIOService serv, ComponentProtocolHandler handler, Queue<Packet> results);

	void startProcessing(Packet p, ComponentIOService serv, ComponentProtocolHandler handler, Queue<Packet> results);
}

