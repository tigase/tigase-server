/*  Package Tigase XMPP/Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <kobit@users.sourceforge.net>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.util;

/**
 * <code>JID</code> class contains static methods for <em>JID</em>
 * manipulation.
 *
 * <p>
 * Created: Thu Jan 27 22:53:41 2005
 * </p>
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JID {

  /**
   * Method <code>getNodeID</code> cuts off <em>resource</em> <em>JID</em> part
   * if exists and returns only node ID.
   *
   * @param jid a <code>String</code> value of <em>JID</em> to parse.
   * @return a <code>String</code> value of node <em>ID</em> without resource
   * part.
   */
  public static final String getNodeID(final String jid) {
    int idx = jid.indexOf('/');
    return idx == -1 ? jid : jid.substring(0, idx);
  }

  /**
   * Method <code>getNodeID</code> parses given <em>JID</em> and returns
   * <em>resource</em> part of given <em>JID</em> or empty string if there
   * was no <em>resource</em> part.
   *
   * @param jid a <code>String</code> value of <em>JID</em> to parse.
   * @return a <code>String</code> value of node <em>Resource</em> or empty
   * string.
   */
  public static final String getNodeResource(final String jid) {
    int idx = jid.indexOf('/');
    return idx == -1 ? "" : jid.substring(idx+1);
  }

  /**
   * Method <code>getNodeHost</code> parses given <em>JID</em> and returns node
   * <em>domain</em> part.
   *
   * @param jid a <code>String</code> value of <em>JID</em> to parse.
   * @return a <code>String</code> value of node <em>domain</em> part.
   */
  public static final String getNodeHost(final String jid) {
    String id = getNodeID(jid);
    int idx = id.indexOf('@');
    return idx == -1 ? id : id.substring(idx+1);
  }

  /**
   * Method <code>getNodeHost</code> parses given <em>JID</em> and returns
   * node nick name or empty string if nick name could not be found.
   *
   * @param jid a <code>String</code> value of <em>JID</em> to parse.
   * @return a <code>String</code> value of node nick name or empty string.
   */
  public static final String getNodeNick(final String jid) {
    String id = getNodeID(jid);
    int idx = id.indexOf('@');
    return idx == -1 ? "" : id.substring(0, idx);
  }

  /**
   * This is static method to construct user <em>ID</em> from given
   * <em>JID</em> parts.
   * This is not user session <em>ID</em> (<em>JID</em>), this is just
   * user <em>ID</em> - <em>JID</em> without resource part.
   *
   * @param nick a <code>String</code> value of node part of <em>JID</em>.
   * @param domain a <code>String</code> value of domain part of <em>JID</em>.
   */
  public static final String getNodeID(final String nick, final String domain) {
    return nick + "@" + domain;
  }

	public static final String getJID(final String nick, final String domain,
		final String resource) {
		return nick + "@" + domain + "/" + resource;
	}

} // JID
