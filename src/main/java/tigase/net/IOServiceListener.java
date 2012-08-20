/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.net;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

//~--- interfaces -------------------------------------------------------------

/**
 * Describe interface IOServiceListener here.
 *
 *
 * Created: Mon Jan 30 22:37:51 2006
 *
 * @param <IO>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface IOServiceListener<IO extends IOService<?>> {
	void packetsReady(IO service) throws IOException;

	boolean serviceStopped(IO service);

	void tlsHandshakeCompleted(IO service);
}    // IOServiceListener


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
