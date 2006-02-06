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
 * $Author$
 * $Date$
 */

package tigase.server;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Class ThreadExceptionHandler.java is responsible helper class used to catch
 * all unhandled exception from all threads.
 * This is default handler which sends exception stack trace to log system. If
 * necessary other server packages can use own custom handlers and do something
 * else with unhandled exceptions. This handler is only implemented to avoid
 * hidden exception causing bugs.
 *
 * <p>
 * Created: Thu Sep 30 22:24:24 2004
 * </p>
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */

public class ThreadExceptionHandler
  implements Thread.UncaughtExceptionHandler {

  private static final Logger log =
    Logger.getLogger("tigase.server.ThreadExceptionHandler");

  public void uncaughtException(final Thread t, final Throwable e) {
    log.log(Level.SEVERE,
      "Uncaught thread: \"" + t.getName() + "\" exception", e);
  }

}// ThreadExceptionHandler

