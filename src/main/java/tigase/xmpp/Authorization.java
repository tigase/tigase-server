/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.xmpp;

import tigase.server.Packet;

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

  AUTHORIZED {
    public String getCondition() { return null; }
    public int getErrorCode() { return 0; }
    public String getErrorType() { return null; }
    public Packet getResponseMessage(Packet p, String t, boolean i) {
			return p.okResult(t, 0);
    }
  },
  BAD_REQUEST {
    public String getCondition() { return "bad-request"; }
    public int getErrorCode() { return 400; }
    public String getErrorType() { return ERR_TYPE_MODIFY; }
  },
  CONFLICT {
    public String getCondition() { return "conflict"; }
    public int getErrorCode() { return 409; }
    public String getErrorType() { return ERR_TYPE_CANCEL; }
  },
  FEATURE_NOT_IMPLEMENTED {
    public String getCondition() { return "feature-not-implemented"; }
    public int getErrorCode() { return 501; }
    public String getErrorType() { return ERR_TYPE_CANCEL; }
  },
  FORBIDDEN {
    public String getCondition() { return "forbidden"; }
    public int getErrorCode() { return 403; }
    public String getErrorType() { return ERR_TYPE_AUTH; }
  },
  GONE {
    public String getCondition() { return "gone"; }
    public int getErrorCode() { return 302; }
    public String getErrorType() { return ERR_TYPE_MODIFY; }
  },
  INTERNAL_SERVER_ERROR {
    public String getCondition() { return "internal-server-error"; }
    public int getErrorCode() { return 500; }
    public String getErrorType() { return ERR_TYPE_WAIT; }
  },
  ITEM_NOT_FOUND {
    public String getCondition() { return "item-not-found"; }
    public int getErrorCode() { return 404; }
    public String getErrorType() { return ERR_TYPE_CANCEL; }
  },
  JID_MALFORMED {
    public String getCondition() { return "jid-malformed"; }
    public int getErrorCode() { return 400; }
    public String getErrorType() { return ERR_TYPE_MODIFY; }
  },
  NOT_ACCEPTABLE {
    public String getCondition() { return "not-acceptable"; }
    public int getErrorCode() { return 406; }
    public String getErrorType() { return ERR_TYPE_MODIFY; }
  },
  NOT_ALLOWED {
    public String getCondition() { return "not-allowed"; }
    public int getErrorCode() { return 405; }
    public String getErrorType() { return ERR_TYPE_CANCEL; }
  },
  NOT_AUTHORIZED {
    public String getCondition() { return "not-authorized"; }
    public int getErrorCode() { return 401; }
    public String getErrorType() { return ERR_TYPE_AUTH; }
  },
  PAYMENT_REQUIRED {
    public String getCondition() { return "payment-required"; }
    public int getErrorCode() { return 402; }
    public String getErrorType() { return ERR_TYPE_AUTH; }
  },
  RECIPIENT_UNAVAILABLE {
    public String getCondition() { return "recipient-unavailable"; }
    public int getErrorCode() { return 404; }
    public String getErrorType() { return ERR_TYPE_WAIT; }
  },
  REDIRECT {
    public String getCondition() { return "redirect"; }
    public int getErrorCode() { return 302; }
    public String getErrorType() { return ERR_TYPE_MODIFY; }
  },
  REGISTRATION_REQUIRED {
    public String getCondition() { return "registration-required"; }
    public int getErrorCode() { return 407; }
    public String getErrorType() { return ERR_TYPE_AUTH; }
  },
  REMOTE_SERVER_NOT_FOUND {
    public String getCondition() { return "remote-server-not-found"; }
    public int getErrorCode() { return 404; }
    public String getErrorType() { return ERR_TYPE_CANCEL; }
  },
  REMOTE_SERVER_TIMEOUT {
    public String getCondition() { return "remote-server-timeout"; }
    public int getErrorCode() { return 504; }
    public String getErrorType() { return ERR_TYPE_WAIT; }
  },
  RESOURCE_CONSTRAINT {
    public String getCondition() { return "resource-constraint"; }
    public int getErrorCode() { return 500; }
    public String getErrorType() { return ERR_TYPE_WAIT; }
  },
  SERVICE_UNAVAILABLE {
    public String getCondition() { return "service-unavailable"; }
    public int getErrorCode() { return 503; }
    public String getErrorType() { return ERR_TYPE_CANCEL; }
  },
  SUBSCRIPTION_REQUIRED {
    public String getCondition() { return "subscription-required"; }
    public int getErrorCode() { return 407; }
    public String getErrorType() { return ERR_TYPE_AUTH; }
  },
  UNDEFINED_CONDITION {
    public String getCondition() { return "undefined-condition"; }
    public int getErrorCode() { return 500; }
    public String getErrorType() { return "[undefined]"; }
  },
  UNEXPECTED_REQUEST {
    public String getCondition() { return "unexpected-request"; }
    public int getErrorCode() { return 400; }
    public String getErrorType() { return ERR_TYPE_WAIT; }
  };

  protected static final String ERR_TYPE_MODIFY = "modify";
  protected static final String ERR_TYPE_CANCEL = "cancel";
  protected static final String ERR_TYPE_AUTH   = "auth";
  protected static final String ERR_TYPE_WAIT   = "wait";

  public abstract String getCondition();
  public abstract int getErrorCode();
  public abstract String getErrorType();

  public Packet getResponseMessage(Packet p, String text,
    boolean includeOriginalXML) {
		return p.errorResult(getErrorType(), getCondition(), text,
			includeOriginalXML);
  }

}