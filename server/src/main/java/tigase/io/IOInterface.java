/*
 * IOInterface.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */

package tigase.io;

//~--- non-JDK imports --------------------------------------------------------

import tigase.stats.StatisticsList;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

//~--- JDK imports ------------------------------------------------------------

//~--- interfaces -------------------------------------------------------------

/**
 * Describe interface IOInterface here.
 * <br>
 * Created: Sat May 14 08:07:38 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public interface IOInterface {

	int bytesRead();

	boolean checkCapabilities(String caps);

	//~--- get methods ----------------------------------------------------------

	int getInputPacketSize() throws IOException;

	SocketChannel getSocketChannel();

	void getStatistics(StatisticsList list, boolean reset);

	long getBytesSent(boolean reset);

	long getTotalBytesSent();

	long getBytesReceived(boolean reset);

	long getTotalBytesReceived();

	long getBuffOverflow(boolean reset);

	long getTotalBuffOverflow();

	boolean isConnected();

	boolean isRemoteAddress(String addr);

	//~--- methods --------------------------------------------------------------

	ByteBuffer read(final ByteBuffer buff) throws IOException;

	void stop() throws IOException;

	boolean waitingToSend();

	int waitingToSendSize();

	int write(final ByteBuffer buff) throws IOException;

	void setLogId(String logId);

}    // IOInterface

//~ Formatted in Sun Code Convention

//~ Formatted by Jindent --- http://www.jindent.com
