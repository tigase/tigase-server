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
 * Last modified by $Author$$
 * $Date$$
 */

package tigase.ui;

/**
 * Interface UIComponent
 * Objects which implements this interface can interract with user through some interface.
 * This can be WWW interface or some standalone application. Everything it needs to do is to return set of possible parameters which can be modified and then passed to UIComponent implementation to process.
 * Some of parameters can be read-only lika work statistics other can be changed like configuration settings or user DB.
 * 
 */
public interface UIComponent {
  // Methods
  // Constructors
  // Accessor Methods
  // Operations
}

