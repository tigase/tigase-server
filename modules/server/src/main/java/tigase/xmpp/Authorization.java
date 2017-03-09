
/*
* @(#)Authorization.java   2010.01.12 at 09:57:32 PST
*
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
package tigase.xmpp;

//~--- non-JDK imports --------------------------------------------------------

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import tigase.server.Packet;

//~--- enums ------------------------------------------------------------------

/**
 * <code>Authorization</code> enumeration type defines authorization error
 * codes.
 * It has also capability to build error response message relevant to
 * specific error code (or success code). It is used not only for authorization
 * process but also by other features implementation accessing session
 * data.<br>
 * All defined errors comes directly from <em>XMPP</em> core RFC. For each error
 * has assigned error code - from old <em>Jabber</em> spec. and error condition -
 * <em>XMPP</em> error spec.
 *
 * <p>
 * Created: Thu Oct 14 22:19:11 2004
 * </p>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public enum Authorization {
	AUTHORIZED {

		@Override
		public String getCondition() {
			return null;
		}

		@Override
		public int getErrorCode() {
			return 0;
		}

		@Override
		public String getErrorType() {
			return null;
		}

		@Override
		public Packet getResponseMessage(Packet p, String t, boolean i) {
			return p.okResult(t, 0);
		}
	},
	BAD_REQUEST {

		@Override
		public String getCondition() {
			return "bad-request";
		}

		@Override
		public int getErrorCode() {
			return 400;
		}

		@Override
		public String getErrorType() {
			return ERR_TYPE_MODIFY;
		}
	},
	CONFLICT {

		@Override
		public String getCondition() {
			return "conflict";
		}

		@Override
		public int getErrorCode() {
			return 409;
		}

		@Override
		public String getErrorType() {
			return ERR_TYPE_CANCEL;
		}
	},
	FEATURE_NOT_IMPLEMENTED {

		@Override
		public String getCondition() {
			return "feature-not-implemented";
		}

		@Override
		public int getErrorCode() {
			return 501;
		}

		@Override
		public String getErrorType() {
			return ERR_TYPE_CANCEL;
		}
	},
	FORBIDDEN {

		@Override
		public String getCondition() {
			return "forbidden";
		}

		@Override
		public int getErrorCode() {
			return 403;
		}

		@Override
		public String getErrorType() {
			return ERR_TYPE_AUTH;
		}
	},
	GONE {

		@Override
		public String getCondition() {
			return "gone";
		}

		@Override
		public int getErrorCode() {
			return 302;
		}

		@Override
		public String getErrorType() {
			return ERR_TYPE_MODIFY;
		}
	},
	INTERNAL_SERVER_ERROR {

		@Override
		public String getCondition() {
			return "internal-server-error";
		}

		@Override
		public int getErrorCode() {
			return 500;
		}

		@Override
		public String getErrorType() {
			return ERR_TYPE_WAIT;
		}
	},
	ITEM_NOT_FOUND {

		@Override
		public String getCondition() {
			return "item-not-found";
		}

		@Override
		public int getErrorCode() {
			return 404;
		}

		@Override
		public String getErrorType() {
			return ERR_TYPE_CANCEL;
		}
	},
	JID_MALFORMED {

		@Override
		public String getCondition() {
			return "jid-malformed";
		}

		@Override
		public int getErrorCode() {
			return 400;
		}

		@Override
		public String getErrorType() {
			return ERR_TYPE_MODIFY;
		}
	},
	NOT_ACCEPTABLE {

		@Override
		public String getCondition() {
			return "not-acceptable";
		}

		@Override
		public int getErrorCode() {
			return 406;
		}

		@Override
		public String getErrorType() {
			return ERR_TYPE_MODIFY;
		}
	},
	NOT_ALLOWED {
		@Override
		public String getCondition() {
			return "not-allowed";
		}

		@Override
		public int getErrorCode() {
			return 405;
		}

		@Override
		public String getErrorType() {
			return ERR_TYPE_CANCEL;
		}
	},
	NOT_AUTHORIZED {

		@Override
		public String getCondition() {
			return "not-authorized";
		}

		@Override
		public int getErrorCode() {
			return 401;
		}

		@Override
		public String getErrorType() {
			return ERR_TYPE_AUTH;
		}
	},
	PAYMENT_REQUIRED {

		@Override
		public String getCondition() {
			return "payment-required";
		}

		@Override
		public int getErrorCode() {
			return 402;
		}

		@Override
		public String getErrorType() {
			return ERR_TYPE_AUTH;
		}
	},
	POLICY_VIOLATION {

		@Override
		public String getCondition() {
			return "policy-violation";
		}

		@Override
		public int getErrorCode() {
			return 0;
		}

		@Override
		public String getErrorType() {
			return ERR_TYPE_CANCEL;
		}
	},
	RECIPIENT_UNAVAILABLE {

		@Override
		public String getCondition() {
			return "recipient-unavailable";
		}

		@Override
		public int getErrorCode() {
			return 404;
		}

		@Override
		public String getErrorType() {
			return ERR_TYPE_WAIT;
		}
	},
	REDIRECT {

		@Override
		public String getCondition() {
			return "redirect";
		}

		@Override
		public int getErrorCode() {
			return 302;
		}

		@Override
		public String getErrorType() {
			return ERR_TYPE_MODIFY;
		}
	},
	REGISTRATION_REQUIRED {

		@Override
		public String getCondition() {
			return "registration-required";
		}

		@Override
		public int getErrorCode() {
			return 407;
		}

		@Override
		public String getErrorType() {
			return ERR_TYPE_AUTH;
		}
	},
	REMOTE_SERVER_NOT_FOUND {

		@Override
		public String getCondition() {
			return "remote-server-not-found";
		}

		@Override
		public int getErrorCode() {
			return 404;
		}

		@Override
		public String getErrorType() {
			return ERR_TYPE_CANCEL;
		}
	},
	REMOTE_SERVER_TIMEOUT {

		@Override
		public String getCondition() {
			return "remote-server-timeout";
		}

		@Override
		public int getErrorCode() {
			return 504;
		}

		@Override
		public String getErrorType() {
			return ERR_TYPE_WAIT;
		}
	},
	RESOURCE_CONSTRAINT {

		@Override
		public String getCondition() {
			return "resource-constraint";
		}

		@Override
		public int getErrorCode() {
			return 500;
		}

		@Override
		public String getErrorType() {
			return ERR_TYPE_WAIT;
		}
	},
	SERVICE_UNAVAILABLE {

		@Override
		public String getCondition() {
			return "service-unavailable";
		}

		@Override
		public int getErrorCode() {
			return 503;
		}

		@Override
		public String getErrorType() {
			return ERR_TYPE_CANCEL;
		}
	},
	SUBSCRIPTION_REQUIRED {

		@Override
		public String getCondition() {
			return "subscription-required";
		}

		@Override
		public int getErrorCode() {
			return 407;
		}

		@Override
		public String getErrorType() {
			return ERR_TYPE_AUTH;
		}
	},
	UNDEFINED_CONDITION {

		@Override
		public String getCondition() {
			return "undefined-condition";
		}

		@Override
		public int getErrorCode() {
			return 500;
		}

		@Override
		public String getErrorType() {
			return "[undefined]";
		}
	},
	UNEXPECTED_REQUEST {

		@Override
		public String getCondition() {
			return "unexpected-request";
		}

		@Override
		public int getErrorCode() {
			return 400;
		}

		@Override
		public String getErrorType() {
			return ERR_TYPE_WAIT;
		}
	};

	protected static final String ERR_TYPE_AUTH = "auth";
	protected static final String ERR_TYPE_CANCEL = "cancel";
	protected static final String ERR_TYPE_MODIFY = "modify";
	protected static final String ERR_TYPE_WAIT = "wait";

	private static final Map<String,Authorization> BY_CONDITION = new ConcurrentHashMap<String,Authorization>();
	
	static {
		for (Authorization v : values()) {
			if (v.getCondition() == null)
				continue;
			BY_CONDITION.put(v.getCondition(), v);
		}
	}
	
	//~--- get methods ----------------------------------------------------------

	public static Authorization getByCondition(String condition) {
		if (condition == null)
			return null;
		return BY_CONDITION.get(condition);
	}
	
	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public abstract String getCondition();

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public abstract int getErrorCode();

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public abstract String getErrorType();

	/**
	 * Utility method for generating an error response for a stanza (enclosed by the given
	 * <code>Packet</code>). In some cases it may happen that the error packet is bounced
	 * back to the sender which triggers another attempt to generate an error response for
	 * the error packet. This may lead to an infinite loop inside the component
	 * (Tigase server) eating up CPU and slowing everything down. To prevent this the
	 * method detects the <em>error</em> stanza typy and generates an exception.
	 * @param packet is the packet for which the error response is generated.
	 * @param text is an error human readable text message.
	 * @param includeOriginalXML is a boolean value indicating whether the original
	 * content of the stanza (children of the top level element) have to be included in the
	 * error stanza.
	 * @return a new <code>Packet</code> instance with an error response for a given
	 * packet.
	 * @throws PacketErrorTypeException if the packet given as a parameter encloses
	 * an error stanza already.
	 */
	public Packet getResponseMessage(Packet packet, String text, boolean includeOriginalXML)
					throws PacketErrorTypeException {
		if ((packet.getType() == null) || (packet.getType() != StanzaType.error)) {
			return packet.errorResult(getErrorType(),
																getErrorCode(),
																getCondition(),
																text,
																includeOriginalXML);
		} else {
			throw new PacketErrorTypeException("The packet has already 'error' type: "
																				 + packet.toString());
		}
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
