/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.server.ext.handlers;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;
import tigase.server.ext.CompRepoItem;
import tigase.server.ext.ComponentConnection;
import tigase.server.ext.ComponentIOService;
import tigase.server.ext.ComponentProtocolHandler;
import tigase.server.ext.ExtProcessor;

import tigase.util.Base64;

import tigase.xml.Element;

import static tigase.server.ext.ComponentProtocolHandler.*;

//~--- JDK imports ------------------------------------------------------------

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Oct 31, 2009 11:06:57 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SASLProcessor implements ExtProcessor {

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(SASLProcessor.class.getName());
	private static final String ID = "sasl";
	private static final String XMLNS = "urn:ietf:params:xml:ns:xmpp-sasl";
	private static final Element FEATURES = new Element("mechanisms",
		new Element[] { new Element("mechanism", "PLAIN") }, new String[] { "xmlns" },
		new String[] { "urn:ietf:params:xml:ns:xmpp-sasl" });

	//~--- get methods ----------------------------------------------------------

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public List<Element> getStreamFeatures(ComponentIOService serv,
			ComponentProtocolHandler handler) {
		if (serv.getSessionData().get(ID) != null) {
			return null;
		} else {
			return Arrays.asList(FEATURES);
		}
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public boolean process(Packet p, ComponentIOService serv, ComponentProtocolHandler handler,
			Queue<Packet> results) {
		if (p.isElement("auth", XMLNS)) {
			String cdata = p.getElemCData();
			String[] credentials = decodeMessage(cdata);

			log.fine("External credentials: " + Arrays.toString(credentials));

			CompRepoItem repo_item = handler.getCompRepoItem(credentials[1]);
			boolean auth_ok = false;

			if (repo_item != null) {
				String local_password = repo_item.getAuthPasswd();

				if (local_password.equals(credentials[2])) {
					auth_ok = true;
				}
			}

			if (auth_ok) {
				Element success = new Element("success", new String[] { "xmlns" },
					new String[] { XMLNS });

				results.offer(Packet.packetInstance(success, null, null));
				handler.authenticated(serv);
			} else {
				Element failure = new Element("failure",
					new Element[] { new Element("not-authorized") }, new String[] { "xmlns" },
					new String[] { XMLNS });

				handler.authenticationFailed(serv, Packet.packetInstance(failure, null, null));
			}

			serv.getSessionData().put(ID, ID);

			return true;
		}

		if (p.isElement("success", XMLNS)) {
			handler.authenticated(serv);
			serv.getSessionData().put(ID, ID);

			return true;
		}

		if (p.isElement("abort", XMLNS)) {
			serv.stop();

			return true;
		}

		if (p.isElement("failure", XMLNS)) {
			serv.stop();

			return true;
		}

		return false;
	}

	@Override
	public void startProcessing(Packet p, ComponentIOService serv,
			ComponentProtocolHandler handler, Queue<Packet> results) {
		CompRepoItem comp_item = (CompRepoItem) serv.getSessionData().get(REPO_ITEM_KEY);
		String domain = comp_item.getDomain();
		String secret = comp_item.getAuthPasswd();
		String challenge = encodeMessage(null, domain, secret);

//  <auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl'
//       mechanism='PLAIN'>AGp1bGlldAByMG0zMG15cjBtMzA=</auth>
		Element auth = new Element("auth", challenge, new String[] { "xmlns", "mechanism" },
			new String[] { XMLNS,
				"PLAIN" });

		results.offer(Packet.packetInstance(auth, null, null));
	}

	private String[] decodeMessage(String input) {

		// Container for authoriz, user_id and password
		String[] result = new String[3];
		byte[] challenge = Base64.decode(input);
		int auth_idx = 0;

		while ((challenge[auth_idx] != 0) && (auth_idx < challenge.length)) {
			++auth_idx;
		}

		String authoriz = new String(challenge, 0, auth_idx);
		int user_idx = ++auth_idx;

		while ((challenge[user_idx] != 0) && (user_idx < challenge.length)) {
			++user_idx;
		}

		String user_id = new String(challenge, auth_idx, user_idx - auth_idx);

		++user_idx;

		String passwd = new String(challenge, user_idx, challenge.length - user_idx);

		result[0] = ((authoriz.length() > 0) ? authoriz : null);
		result[1] = ((user_id.length() > 0) ? user_id : null);
		result[2] = ((passwd.length() > 0) ? passwd : null);

		return result;
	}

	private String encodeMessage(String authoriz, String user_id, String password) {
		int authoriz_size = ((authoriz != null) ? authoriz.getBytes().length : 0);
		int user_id_size = ((user_id != null) ? user_id.getBytes().length : 0);
		int password_size = ((password != null) ? password.getBytes().length : 0);

		// 2 NUL U+0000 characters
		int size = 2;

		size += authoriz_size + user_id_size + password_size;

		byte[] result = new byte[size];

		if ((authoriz != null) && (authoriz_size > 0)) {
			System.arraycopy(authoriz.getBytes(), 0, result, 0, authoriz_size);
		}

		result[authoriz_size] = 0;

		if ((user_id != null) && (user_id_size > 0)) {
			System.arraycopy(user_id.getBytes(), 0, result, authoriz_size + 1, user_id_size);
		}

		result[authoriz_size + 1 + user_id_size] = 0;

		if ((password != null) && (password_size > 0)) {
			System.arraycopy(password.getBytes(), 0, result, authoriz_size + 1 + user_id_size + 1,
					password_size);
		}

		return Base64.encode(result);
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
