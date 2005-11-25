/*  Package Jabber Server
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
package tigase.server;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.Queue;
import java.util.List;
import java.util.ArrayList;

import tigase.annotations.TODO;

import tigase.stats.StatisticsContainer;
import tigase.stats.StatRecord;
import tigase.stats.StatisticType;
/**
 * Describe class AbstractMessageReceiver here.
 *
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class AbstractMessageReceiver extends Thread
	implements StatisticsContainer, MessageReceiver {

	private MessageReceiver parent = null;
	private String[] addresses = null;
	private int maxQueueSize = Integer.MAX_VALUE;

	private LinkedBlockingQueue<Packet> queue = null;
	private boolean stopped = false;

	/**
	 * Variable <code>statAddedMessagesOk</code> keeps counter of successfuly
	 * added messages to queue.
	 */
	private long statAddedMessagesOk = 0;
	/**
	 * Variable <code>statAddedMessagesEr</code> keeps counter of unsuccessfuly
	 * added messages due to queue overflow.
	 */
	private long statAddedMessagesEr = 0;

	public AbstractMessageReceiver(String[] addresses, int maxQueueSize,
		MessageReceiver parent) {

		this.addresses = addresses;
		if (maxQueueSize > 0) {
			this.maxQueueSize = maxQueueSize;
		} // end of if (maxQueueSize > 0)
		this.parent = parent;
	}

  /**
	 * Describe <code>routingAddresses</code> method here.
   * Returns array of Strings. Each String should be a regular expression
   * defining destination addresses for which this receiver can process
   * messages. There can be more than one message receiver for each messages.
	 *
	 * @return a <code>String</code> value
	 */
	public String[] routingAddresses() { return addresses; }

  /**
	 * Describe <code>addMessage</code> method here.
	 *
	 * @param packet a <code>Packet</code> value
	 */
	public boolean addMessage(Packet packet, boolean blocking) {
		boolean result = true;
		if (blocking) {
			try {
				queue.put(packet);
			} // end of try
			catch (InterruptedException e) {
				result = false;
			} // end of try-catch
		} // end of if (blocking)
		else {
			result = queue.offer(packet);
		} // end of if (blocking) else
		if (result) ++statAddedMessagesOk; else ++statAddedMessagesEr;
		return result;
	}

	@TODO(note="Consider better implementation for a case when addMessage fails, maybe packet which couldn't be handled should be put back to input queue.")
	public boolean addMessages(Queue<Packet> packets, boolean blocking) {
		if (packets == null || packets.size() == 0) {
			return false;
		} // end of if (packets != null && packets.size() > 0)
		Packet packet = null;
		boolean result = true;
		while (result && ((packet = packets.poll()) != null)) {
			result = addMessage(packet, blocking);
		} // end of while (result && (packet = packets.poll()) != null)
		return result;
	}

	public void run() {
		queue = new LinkedBlockingQueue<Packet>(maxQueueSize);
		while (! stopped) {
			try {
				Packet packet = queue.take();
				Queue<Packet> result = processPacket(packet);
				parent.addMessages(result, true);
			} // end of try
			catch (InterruptedException e) {
				stopped = true;
			} // end of try-catch
		} // end of while (! stopped)
	}

	public abstract Queue<Packet> processPacket(Packet packet);

	public int queueSize() { return queue.size(); }

  /**
   * Returns defualt configuration settings for this object.
   */
	public List<StatRecord> getStatistics() {
		List<StatRecord> stats = new ArrayList<StatRecord>();
		stats.add(new StatRecord(StatisticType.QUEUE_SIZE, queue.size()));
		stats.add(new StatRecord(StatisticType.MSG_RECEIVED_OK,
				statAddedMessagesOk));
		stats.add(new StatRecord(StatisticType.QUEUE_OVERFLOW,
				statAddedMessagesEr));
		return stats;
	}

} // AbstractMessageReceiver
