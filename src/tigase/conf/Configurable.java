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

package tigase.conf;

/**
 * Interface Configurable
 * Objects inheriting this interface can be configured. In Tigase system object can't request configuration properties. Configuration of the object is passed to it at some time. Actually it can be passed at any time. This allows dynamic system reconfiguration at runtime.
 * 
 */
public interface Configurable {
  // Methods
  // Constructors
  // Accessor Methods
  // Operations
  /**
   * Get object id. This id corresponds to entry in configuration.
   */
  public String getId ( );
    
  
  /**
   * Sets configuration property to object.
   */
  public void setProperty ( string name, string value);
    
  
  /**
   * Sets all configuration properties for object.
   */
  public void setProperties ( );
    
  
  /**
   * Returns defualt configuration settings for this object.
   */
  public String getDefaults ( );
    
  
}

