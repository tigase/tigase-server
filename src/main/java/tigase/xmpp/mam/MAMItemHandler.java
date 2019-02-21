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
package tigase.xmpp.mam;

import tigase.component.PacketWriter;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Message;
import tigase.server.Priority;
import tigase.util.datetime.TimestampHelper;
import tigase.xml.Element;
import tigase.xmpp.mam.modules.QueryModule;

/**
 * Basic implementation of handler processing items found in repository and converting into forward messages for
 * delivery to client as specified in XEP-0313: Message Archive Management
 * <br>
 * Created by andrzej on 19.07.2016.
 */
@Bean(name = "mamItemHandler", parent = QueryModule.class, active = true)
public class MAMItemHandler
		implements MAMRepository.ItemHandler {

	private static final TimestampHelper TIMESTAMP_FORMATTER = new TimestampHelper();
	
	@Inject
	private PacketWriter packetWriter;

	@Override
	public void itemFound(Query query, MAMRepository.Item item) {
		Element m = new Element("message");
		Element result = new Element("result", new String[]{"xmlns", "id"},
									 new String[]{"urn:xmpp:mam:1", item.getId()});
		if (query.getId() != null) {
			result.setAttribute("queryid", query.getId());
		}
		m.addChild(result);
		Element forwarded = new Element("forwarded", new String[]{"xmlns"}, new String[]{"urn:xmpp:forward:0"});
		result.addChild(forwarded);

		String timestampStr = TIMESTAMP_FORMATTER.formatWithMs(item.getTimestamp());

		Element delay = new Element("delay", new String[]{"xmlns", "stamp"},
									new String[]{"urn:xmpp:delay", timestampStr});
		forwarded.addChild(delay);

		forwarded.addChild(item.getMessage());

		Message packet = new Message(m, query.getComponentJID(), query.getQuestionerJID());
		packet.setPriority(Priority.HIGH);

		if (query.getRsm().getFirst() == null) {
			query.getRsm().setFirst(item.getId());
		}
		query.getRsm().setLast(item.getId());

		packetWriter.write(packet);
	}

}
