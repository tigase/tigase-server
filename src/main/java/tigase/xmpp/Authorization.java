
/*
* @(#)Authorization.java   2010.01.12 at 09:57:32 PST
*
* Tigase Jabber/XMPP Server
* Copyright (C) 2004-2010 "Artur Hefczyc" <artur.hefczyc@tigase.org>
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, version 3 of the License.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. Look for COPYING file in the top folder.
* If not, see http://www.gnu.org/licenses/.
*
* $Rev$
* Last modified by $Author$
* $Date$
 */
package tigase.xmpp;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Packet;

//~--- enums ------------------------------------------------------------------

/**
 * <code>Authorization</code> enumeration type defines authorization error
 * codes.
 * It has also capability to build error response message relevant to
 * specific error code (or success code). It is used not only for authorization
 * process but also by other features implementation accessing session
 * data.<br/>
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
	/**
	 *
	 */
	AUTHORIZED {

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String getCondition() {
			return null;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public int getErrorCode() {
			return 0;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String getErrorType() {
			return null;
		}

		/**
		 * Method description
		 *
		 *
		 * @param p
		 * @param t
		 * @param i
		 *
		 * @return
		 */
		@Override
		public Packet getResponseMessage(Packet p, String t, boolean i) {
			return p.okResult(t, 0);
		}
	},
	/**
	 *
	 */
	BAD_REQUEST {

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String getCondition() {
			return "bad-request";
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public int getErrorCode() {
			return 400;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String getErrorType() {
			return ERR_TYPE_MODIFY;
		}
	},
	/**
	 *
	 */
	CONFLICT {

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String getCondition() {
			return "conflict";
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public int getErrorCode() {
			return 409;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String getErrorType() {
			return ERR_TYPE_CANCEL;
		}
	},
	/**
	 *
	 */
	FEATURE_NOT_IMPLEMENTED {

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String getCondition() {
			return "feature-not-implemented";
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public int getErrorCode() {
			return 501;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String getErrorType() {
			return ERR_TYPE_CANCEL;
		}
	},
	/**
	 *
	 */
	FORBIDDEN {

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String getCondition() {
			return "forbidden";
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public int getErrorCode() {
			return 403;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String getErrorType() {
			return ERR_TYPE_AUTH;
		}
	},
	/**
	 *
	 */
	GONE {

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String getCondition() {
			return "gone";
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public int getErrorCode() {
			return 302;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String getErrorType() {
			return ERR_TYPE_MODIFY;
		}
	},
	/**
	 *
	 */
	INTERNAL_SERVER_ERROR {

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String getCondition() {
			return "internal-server-error";
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public int getErrorCode() {
			return 500;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String getErrorType() {
			return ERR_TYPE_WAIT;
		}
	},
	/**
	 *
	 */
	ITEM_NOT_FOUND {

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String getCondition() {
			return "item-not-found";
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public int getErrorCode() {
			return 404;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String getErrorType() {
			return ERR_TYPE_CANCEL;
		}
	},
	/**
	 *
	 */
	JID_MALFORMED {

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String getCondition() {
			return "jid-malformed";
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public int getErrorCode() {
			return 400;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		@Override
		public String getErrorType() {
			return ERR_TYPE_MODIFY;
		}
	},
	/**
	 * 
	 */
	NOT_ACCEPTABLE {

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getCondition() {
			return "not-acceptable";
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public int getErrorCode() {
			return 406;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getErrorType() {
			return ERR_TYPE_MODIFY;
		}
	},
	NOT_ALLOWED {

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getCondition() {
			return "not-allowed";
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public int getErrorCode() {
			return 405;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getErrorType() {
			return ERR_TYPE_CANCEL;
		}
	},
	NOT_AUTHORIZED {

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getCondition() {
			return "not-authorized";
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public int getErrorCode() {
			return 401;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getErrorType() {
			return ERR_TYPE_AUTH;
		}
	},
	PAYMENT_REQUIRED {

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getCondition() {
			return "payment-required";
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public int getErrorCode() {
			return 402;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getErrorType() {
			return ERR_TYPE_AUTH;
		}
	},
	RECIPIENT_UNAVAILABLE {

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getCondition() {
			return "recipient-unavailable";
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public int getErrorCode() {
			return 404;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getErrorType() {
			return ERR_TYPE_WAIT;
		}
	},
	REDIRECT {

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getCondition() {
			return "redirect";
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public int getErrorCode() {
			return 302;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getErrorType() {
			return ERR_TYPE_MODIFY;
		}
	},
	REGISTRATION_REQUIRED {

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getCondition() {
			return "registration-required";
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public int getErrorCode() {
			return 407;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getErrorType() {
			return ERR_TYPE_AUTH;
		}
	},
	REMOTE_SERVER_NOT_FOUND {

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getCondition() {
			return "remote-server-not-found";
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public int getErrorCode() {
			return 404;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getErrorType() {
			return ERR_TYPE_CANCEL;
		}
	},
	REMOTE_SERVER_TIMEOUT {

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getCondition() {
			return "remote-server-timeout";
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public int getErrorCode() {
			return 504;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getErrorType() {
			return ERR_TYPE_WAIT;
		}
	},
	RESOURCE_CONSTRAINT {

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getCondition() {
			return "resource-constraint";
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public int getErrorCode() {
			return 500;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getErrorType() {
			return ERR_TYPE_WAIT;
		}
	},
	SERVICE_UNAVAILABLE {

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getCondition() {
			return "service-unavailable";
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public int getErrorCode() {
			return 503;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getErrorType() {
			return ERR_TYPE_CANCEL;
		}
	},
	SUBSCRIPTION_REQUIRED {

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getCondition() {
			return "subscription-required";
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public int getErrorCode() {
			return 407;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getErrorType() {
			return ERR_TYPE_AUTH;
		}
	},
	UNDEFINED_CONDITION {

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getCondition() {
			return "undefined-condition";
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public int getErrorCode() {
			return 500;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getErrorType() {
			return "[undefined]";
		}
	},
	UNEXPECTED_REQUEST {

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getCondition() {
			return "unexpected-request";
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public int getErrorCode() {
			return 400;
		}

		/**
		 * Method description
		 *
		 *
		 * @return
		 */
		public String getErrorType() {
			return ERR_TYPE_WAIT;
		}
	};

	protected static final String ERR_TYPE_AUTH = "auth";
	protected static final String ERR_TYPE_CANCEL = "cancel";
	protected static final String ERR_TYPE_MODIFY = "modify";
	protected static final String ERR_TYPE_WAIT = "wait";

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public abstract String getCondition();

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public abstract int getErrorCode();

	/**
	 * Method description
	 *
	 *
	 * @return
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
