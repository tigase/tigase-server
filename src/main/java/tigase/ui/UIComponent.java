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
