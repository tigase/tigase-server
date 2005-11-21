/*
 *  Package Tigase XMPP/Jabber Server
 *  Copyright (C) 2004, 2005, 2006
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
 * Last modified by $Author: .*\$$
 * $Date:  .*\$$
 */

package tigase.server;

/**
 * Interface MessageReceiver
 * Objects of this type can receive messages. They can be in fact routing destination depending on target address. Message are routed to proper destination in MessageRouter class.
 */
public interface MessageReceiver extends ServerComponentIfc {
  // Methods
  // Constructors
  // Accessor Methods
  // Operations
  /**
   * Returns array of Strings. Each String should be a regular expression defining destination addresses for which this receiver can process messages. There can be more than one message receiver for each messages.
   */
  public String routingAddresses ( );
    
  
  /**
   * 
   * @param packet 
   * @return   
   */
  public  addMessage ( tigase.server.Packet packet);
    
  
}

