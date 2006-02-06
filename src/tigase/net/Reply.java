/*  Package Tigase XMPP/Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <artur.hefczyc@gmail.com>
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
package tigase.net;

import java.util.logging.Logger;

/**
 * <code>Reply</code> is a simple class which instances represents
 * <em>XMPP</em> replyies.
 * There are 3 possible types of replies:
 * </p>
 * <ol>
 *   <li><code>SELF</code> - this reply is sent directly to entity connected to
 *   this session. Usually this is response to request like: authorization or
 *   roster.</li>
 *   <li><code>DESTINATION</code> - this reply is sent to some other entity than
 *   connected to this session. This can be <em>XMPP</em> message or subscription
 *   presence.</li>
 *   <li><code>BROADCAST</code> - this kind of reply is sent to a group of
 *   entities. This can be <em>XMPP</em> availability presence or roster
 *   update.</li>
 * </ol>
 * <p><code>Reply</code> contains additional information about sender address and
 * destination address and of course buffer with data to be send.</p>
 *
 * Created: Tue Oct 19 13:45:57 2004
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Reply {

  private static final Logger log = Logger.getLogger("tigase.xmpp.Reply");

  /**
   * Enumeration used to set reply type.
   * There are 3 possible types of replies:
   * <ol>
   *   <li><code>SELF</code> - this reply is sent directly to entity connected to
   *   this session. Usually this is response to request like: authorization or
   *   roster.</li>
   *   <li><code>DESTINATION</code> - this reply is sent to some other entity than
   *   connected to this session. This can be <em>XMPP</em> message or subscription
   *   presence.</li>
   *   <li><code>BROADCAST</code> - this kind of reply is sent to a group of
   *   entities. This can be <em>XMPP</em> availability presence or roster
   *   update.</li>
   * </ol>
   */
  public enum Type {
    /**
     * <code>SELF</code> - this reply is sent directly to entity connected to
     * this session. Usually this is response to request like: authorization or
     * roster.
     */
    SELF,
    /**
     * <code>DESTINATION</code> - this reply is sent to some other entity than
     * connected to this session. This can be <em>XMPP</em> message or subscription
     * presence.
     */
    DESTINATION,
    /**
     * <code>BROADCAST</code> - this kind of reply is sent to a group of
     * entities. This can be <em>XMPP</em> availability presence or roster
     * update.
     */
    BROADCAST,
    /**
     * <code>STOP</code> - this reply orders to stop connection due to
     * unrecoverable stream errors like TLS or SASL failure.
     */
    STOP,
    /**
     * <code>STARTTLS</code> - this reply orders lower transpart layer
     * to start use TLS for data protection.
     */
    STARTTLS;
  }

  private String to = null;
  private String data = null;
  private Type type = Type.SELF;

  public Reply(String data) {
    this.data = data;
  }

  /**
   * Creates a new <code>Reply</code> instance.
   *
   */
  public Reply(String to, String data, Type type) {
    this.to = to;
    this.data = data;
    this.type = type;
    log.finest("Message to: "+to+", type: "+type+", content: "+data);
  }

  public String getTo() { return to; }

  public String getData() { return data; }

  public Type getType() { return type; }

} // Reply
