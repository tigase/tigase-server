/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
package tigase.server;

/**
 * Implementation of <a
 * href="http://www.xmpp.org/extensions/xep-0086.html">XEP-0086</a>
 * 
 * @author bmalkow
 * 
 */
public class ErrorCondition {

	public static enum XmppErrorType {
		AUTH("auth"), CANCEL("cancel"), MODIFY("modify"), WAIT("wait");

		private String value;

		private XmppErrorType(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}

	public static final ErrorCondition BAD_REQUEST = new ErrorCondition("bad-request", XmppErrorType.MODIFY, 400);
	public static final ErrorCondition CONFLICT = new ErrorCondition("conflict", XmppErrorType.CANCEL, 409);
	public static final ErrorCondition FEATURE_NOT_IMPLEMENTED = new ErrorCondition("feature-not-implemented",
			XmppErrorType.CANCEL, 501);
	public static final ErrorCondition FORBIDDEN = new ErrorCondition("forbidden", XmppErrorType.AUTH, 403);
	public static final ErrorCondition GONE = new ErrorCondition("gone", XmppErrorType.MODIFY, 302);
	public static final ErrorCondition INTERNAL_SERVER_ERROR = new ErrorCondition("internal-server-error",
			XmppErrorType.WAIT, 500);
	public static final ErrorCondition ITEM_NOT_FOUND = new ErrorCondition("item-not-found", XmppErrorType.CANCEL, 404);
	public static final ErrorCondition JID_MALFORMED = new ErrorCondition("recipient-unavailable",
			XmppErrorType.MODIFY, 400);
	public static final ErrorCondition NOT_ACCEPTABLE = new ErrorCondition("not-acceptable", XmppErrorType.MODIFY, 406);
	public static final ErrorCondition NOT_ALLOWED = new ErrorCondition("not-allowed", XmppErrorType.CANCEL, 405);
	public static final ErrorCondition NOT_AUTHORIZED = new ErrorCondition("not-authorized", XmppErrorType.AUTH, 401);
	public static final ErrorCondition PAYMENT_REQUIRED = new ErrorCondition("payment-required", XmppErrorType.AUTH,
			402);
	public static final ErrorCondition RECIPIENT_UNAVAILABLE = new ErrorCondition("recipient-unavailable",
			XmppErrorType.WAIT, 404);
	public static final ErrorCondition REDIRECT = new ErrorCondition("redirect", XmppErrorType.MODIFY, 302);
	public static final ErrorCondition REGISTRATION_REQUIRED = new ErrorCondition("registration-required",
			XmppErrorType.AUTH, 407);
	public static final ErrorCondition REMOTE_SERVER_NOT_FOUND = new ErrorCondition("remote-server-not-found",
			XmppErrorType.CANCEL, 404);
	public static final ErrorCondition REMOTE_SERVER_TIMEOUT = new ErrorCondition("remote-server-timeout",
			XmppErrorType.WAIT, 504);
	public static final ErrorCondition RESOURCE_CONSTRAINT = new ErrorCondition("resource-constraint",
			XmppErrorType.WAIT, 500);
	public static final ErrorCondition SERVICE_UNAVAILABLE = new ErrorCondition("service-unavailable",
			XmppErrorType.CANCEL, 503);
	public static final ErrorCondition SUBSCRIPTION_REQUIRED = new ErrorCondition("subscription-required",
			XmppErrorType.AUTH, 407);
	public static final ErrorCondition UNEXPECTED_REQUEST = new ErrorCondition("unexpected-request",
			XmppErrorType.WAIT, 400);

	private final int legacyErrorCode;

	private final String xmppErrorCondition;

	private final XmppErrorType xmppErrorType;

	protected ErrorCondition(String xmppErrorCondition, XmppErrorType xmppErrorType, int legacyErrorCode) {
		this.xmppErrorCondition = xmppErrorCondition;
		this.xmppErrorType = xmppErrorType;
		this.legacyErrorCode = legacyErrorCode;
	}

	public int getLegacyErrorCode() {
		return legacyErrorCode;
	}

	public String getXmppErrorCondition() {
		return xmppErrorCondition;
	}

	public XmppErrorType getXmppErrorType() {
		return xmppErrorType;
	}

}
