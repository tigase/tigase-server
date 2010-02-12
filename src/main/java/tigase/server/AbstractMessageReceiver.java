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

package tigase.server;

//~--- non-JDK imports --------------------------------------------------------

import tigase.annotations.TODO;

import tigase.stats.StatisticType;
import tigase.stats.StatisticsContainer;
import tigase.stats.StatisticsList;

import tigase.util.PatternComparator;
import tigase.util.PriorityQueueAbstract;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

//~--- classes ----------------------------------------------------------------

/**
 * This is an archetype for all classes processing user-level packets. The implementation
 * is designed for a heavy packets processing with internal queues and number of separate
 * threads depending on number of CPUs. Extensions of the class can process normall user
 * packets and administrator packets via ad-hoc commands. Good examples of such
 * components are <code>MUC</code>, <code>PubSub</code>, <code>SessionManager</code>.<p/>
 * The class offers scripting API for administrator ad-hoc commands.<p/>
 * By default it internally uses priority queues which in some rare cases may lead to
 * packets reordering. When this happens and it is unacceptable for the deployment
 * non-priority queues can be used. The queues size is limited and depends on the
 * available memory size.<p/>
 * Packets are processed by <code>processPacket(Packet packet)</code> method which
 * is concurrently called from multiple threads.
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class AbstractMessageReceiver extends BasicComponent
		implements StatisticsContainer, MessageReceiver {

	/**
	 * Configuration property key for setting incoming packets filters on the component level.
	 */
	public static final String INCOMING_FILTERS_PROP_KEY = "incoming-filters";

	/**
	 * Configuration property default vakue with a default incoming packet filter loaded by
	 * Tigase server.<p/>
	 * This is a comma-separated list of classes which should be loaded as packet filters.
	 * The classes must implement <code>PacketFilterIfc</code> interface.
	 */
	public static final String INCOMING_FILTERS_PROP_VAL = "tigase.server.filters.PacketCounter";

	/**
	 * Configuration property key allowing to overwrite a default (memory size dependent)
	 * size for the component internal queues. By default the queue size is adjusted to the
	 * available memory size to avoid out of memory errors.
	 */
	public static final String MAX_QUEUE_SIZE_PROP_KEY = "max-queue-size";

	/** A default value for max queue size property. The value is calculated at the server
	 * startup time using following formula: <br/>
	 * <code>Runtime.getRuntime().maxMemory() / 400000L</code>
	 * You can change the default queue size by setting a different value for the
	 * <code>MAX_QUEUE_SIZE_PROP_KEY</code> property in the server configuration.
	 */
	public static final Integer MAX_QUEUE_SIZE_PROP_VAL =
			new Long(Runtime.getRuntime().maxMemory() / 400000L).intValue();

	/**
	 * Configuration property key for setting outgoing packets filters on the component level.
	 * This is a comma-separated list of classes which should be loaded as packet filters.
	 * The classes must implement <code>PacketFilterIfc</code> interface.
	 */
	public static final String OUTGOING_FILTERS_PROP_KEY = "outgoing-filters";

	/**
	 * Configuration property default vakue with a default outgoing packet filter loaded by
	 * Tigase server.<p/>
	 * This is a comma-separated list of classes which should be loaded as packet filters.
	 * The classes must implement <code>PacketFilterIfc</code> interface.
	 */
	public static final String OUTGOING_FILTERS_PROP_VAL = "tigase.server.filters.PacketCounter";
	/**
	 * Constant used in time calculation procedures. Indicates a second that is 1000 milliseconds.
	 */
	protected static final long SECOND = 1000;
	/**
	 * Constant used in time calculation procedures. Indicates a minute that is 60
	 * <code>SECOND</code>s.
	 */
	protected static final long MINUTE = 60 * SECOND;
	/**
	 * Constant used in time calculation procedures. Indicates a hour that is 60
	 * <code>MINUTE</code>s.
	 */
	protected static final long HOUR = 60 * MINUTE;

	// String added intentionally!!
	// Don't change to AbstractMessageReceiver.class.getName()

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger("tigase.abstract.AbstractMessageReceiver");

	//~--- fields ---------------------------------------------------------------

//private static final TigaseTracer tracer = TigaseTracer.getTracer("abstract");
	private int in_queues_size = 1;
	private long last_hour_packets = 0;
	private long last_minute_packets = 0;
	private long last_second_packets = 0;
	protected int maxQueueSize = MAX_QUEUE_SIZE_PROP_VAL;
	private QueueListener out_thread = null;
	private long packetId = 0;
	private long packets_per_hour = 0;
	private long packets_per_minute = 0;
	private long packets_per_second = 0;
	private MessageReceiver parent = null;
	private int pptIdx = 0;

	// Array cache to speed processing up....
	private Priority[] pr_cache = Priority.values();
	private CopyOnWriteArrayList<PacketFilterIfc> outgoing_filters =
		new CopyOnWriteArrayList<PacketFilterIfc>();
	private PriorityQueueAbstract<Packet> out_queue =
		PriorityQueueAbstract.getPriorityQueue(pr_cache.length, maxQueueSize);
	private CopyOnWriteArrayList<PacketFilterIfc> incoming_filters =
		new CopyOnWriteArrayList<PacketFilterIfc>();
	private ArrayList<PriorityQueueAbstract<Packet>> in_queues =
		new ArrayList<PriorityQueueAbstract<Packet>>();
	private long[] processPacketTimings = new long[100];
	private ArrayDeque<QueueListener> processingThreads = null;
	private Timer receiverTasks = null;

	/**
	 * Variable <code>statAddedMessagesEr</code> keeps counter of unsuccessfuly
	 * added messages due to queue overflow.
	 */
	private long statReceivedPacketsEr = 0;

	/**
	 * Variable <code>statAddedMessagesOk</code> keeps counter of successfuly
	 * added messages to queue.
	 */
	private long statReceivedPacketsOk = 0;
	private long statSentPacketsEr = 0;
	private long statSentPacketsOk = 0;
	private ConcurrentHashMap<String, PacketReceiverTask> waitingTasks =
		new ConcurrentHashMap<String, PacketReceiverTask>(16, 0.75f, 4);
	private Set<Pattern> regexRoutings = new ConcurrentSkipListSet<Pattern>(new PatternComparator());

	//~--- methods --------------------------------------------------------------

	/**
	 * This is the main <code>Packet</code> processing method. It is called concurrently
	 * from many threads so implementing it in thread save manner is essential. The method
	 * is called for each packet addressed to the component. <p/>
	 * Please note, the <code>Packet</code> instance may be processed by different parts
	 * of the server, different components or plugins at the same time. Therefore this is
	 * very important to tread the <code>Packet</code> instance as unmodifiable object.<p/>
	 * Processing in this method is asynchronous, therefore there is no result value. If there
	 * are some 'result' packets generated during processing, they should be passed back using
	 * <code>add
	 *
	 *
	 * @param packet
	 */
	public abstract void processPacket(Packet packet);

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 *
	 * @return
	 */
	@Override
	public boolean addPacket(Packet packet) {
		int queueIdx = Math.abs(hashCodeForPacket(packet) % in_queues_size);

		if (log.isLoggable(Level.FINEST)) {
			log.finest("[" + getName() + "] queueIdx=" + queueIdx + ", " + packet.toStringSecure());
		}

		try {
			in_queues.get(queueIdx).put(packet, packet.getPriority().ordinal());
			++statReceivedPacketsOk;
		} catch (InterruptedException e) {
			++statReceivedPacketsEr;

			return false;
		}    // end of try-catch

		return true;
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 *
	 * @return
	 */
	@Override
	public boolean addPacketNB(Packet packet) {
		int queueIdx = Math.abs(hashCodeForPacket(packet) % in_queues_size);

		if (log.isLoggable(Level.FINEST)) {
			log.finest("[" + getName() + "] queueIdx=" + queueIdx + ", " + packet.toStringSecure());
		}

		boolean result = in_queues.get(queueIdx).offer(packet, packet.getPriority().ordinal());

		if (result) {
			++statReceivedPacketsOk;
		} else {

			// Queue overflow!
			++statReceivedPacketsEr;
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * @param packets
	 *
	 * @return
	 */
	@Override
	public boolean addPackets(Queue<Packet> packets) {
		Packet p = null;
		boolean result = true;

		while ((p = packets.peek()) != null) {
			result = addPacket(p);

			if (result) {
				packets.poll();
			} else {
				return false;
			}    // end of if (result) else
		}      // end of while ()

		return true;
	}

	/**
	 * Method description
	 *
	 *
	 * @param address
	 */
	public void addRegexRouting(String address) {
		if (log.isLoggable(Level.FINE)) {
			log.fine(getName() + " - attempt to add regex routing: " + address);
		}

		regexRoutings.add(Pattern.compile(address, Pattern.CASE_INSENSITIVE));

		if (log.isLoggable(Level.FINE)) {
			log.fine(getName() + " - success adding regex routing: " + address);
		}
	}

	/**
	 * Method description
	 *
	 */
	public void clearRegexRoutings() {
		regexRoutings.clear();
	}

	/**
	 * Method description
	 *
	 */
	public synchronized void everyHour() {
		packets_per_hour = statReceivedPacketsOk - last_hour_packets;
		last_hour_packets = statReceivedPacketsOk;
	}

	/**
	 * Method description
	 *
	 */
	public synchronized void everyMinute() {
		packets_per_minute = statReceivedPacketsOk - last_minute_packets;
		last_minute_packets = statReceivedPacketsOk;
		receiverTasks.purge();
	}

	/**
	 * Method description
	 *
	 */
	public synchronized void everySecond() {
		packets_per_second = statReceivedPacketsOk - last_second_packets;
		last_second_packets = statReceivedPacketsOk;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Returns defualt configuration settings for this object.
	 *
	 * @param params
	 *
	 * @return
	 */
	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defs = super.getDefaults(params);
		String queueSize = (String) params.get(GEN_MAX_QUEUE_SIZE);
		int queueSizeInt = MAX_QUEUE_SIZE_PROP_VAL;

		if (queueSize != null) {
			try {
				queueSizeInt = Integer.parseInt(queueSize);
			} catch (NumberFormatException e) {
				queueSizeInt = MAX_QUEUE_SIZE_PROP_VAL;
			}
		}

		defs.put(MAX_QUEUE_SIZE_PROP_KEY, getMaxQueueSize(queueSizeInt));
		defs.put(INCOMING_FILTERS_PROP_KEY, INCOMING_FILTERS_PROP_VAL);
		defs.put(OUTGOING_FILTERS_PROP_KEY, OUTGOING_FILTERS_PROP_VAL);

		return defs;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public Set<Pattern> getRegexRoutings() {
		return regexRoutings;
	}

	/**
	 * Method description
	 *
	 *
	 * @param list
	 */
	@Override
	public void getStatistics(StatisticsList list) {
		list.add(getName(), "Last second packets", packets_per_second, Level.FINE);
		list.add(getName(), "Last minute packets", packets_per_minute, Level.FINE);
		list.add(getName(), "Last hour packets", packets_per_hour, Level.FINE);
		list.add(getName(), StatisticType.MSG_RECEIVED_OK.getDescription(), statReceivedPacketsOk,
				Level.FINE);
		list.add(getName(), StatisticType.MSG_SENT_OK.getDescription(), statSentPacketsOk, Level.FINE);

		if (list.checkLevel(Level.FINEST)) {
			int[] in_priority_sizes = in_queues.get(0).size();

			for (int i = 1; i < in_queues.size(); i++) {
				int[] tmp_pr_sizes = in_queues.get(i).size();

				for (int j = 0; j < tmp_pr_sizes.length; j++) {
					in_priority_sizes[j] += tmp_pr_sizes[j];
				}
			}

			int[] out_priority_sizes = out_queue.size();

			for (int i = 0; i < in_priority_sizes.length; i++) {
				Priority queue = Priority.values()[i];

				list.add(getName(), "In queue: " + queue.name(), in_priority_sizes[queue.ordinal()],
						Level.FINEST);
				list.add(getName(), "Out queue: " + queue.name(), out_priority_sizes[queue.ordinal()],
						Level.FINEST);
			}
		}

		int in_queue_size = 0;

		for (PriorityQueueAbstract<Packet> total_size : in_queues) {
			in_queue_size += total_size.totalSize();
		}

		int out_queue_size = out_queue.totalSize();

		list.add(getName(), "Total In queues wait", in_queue_size, Level.INFO);
		list.add(getName(), "Total Out queues wait", out_queue_size, Level.INFO);
		list.add(getName(), StatisticType.MAX_QUEUE_SIZE.getDescription(), maxQueueSize, Level.FINEST);
		list.add(getName(), StatisticType.IN_QUEUE_OVERFLOW.getDescription(), statReceivedPacketsEr,
				Level.INFO);
		list.add(getName(), StatisticType.OUT_QUEUE_OVERFLOW.getDescription(), statSentPacketsEr,
				Level.INFO);

		long res = 0;

		for (long ppt : processPacketTimings) {
			res += ppt;
		}

		long prcessingTime = res / processPacketTimings.length;

		list.add(getName(),
				"Average processing time on last " + processPacketTimings.length + " runs [ms]",
					prcessingTime, Level.FINE);

		for (PacketFilterIfc packetFilter : incoming_filters) {
			packetFilter.getStatistics(list);
		}

		for (PacketFilterIfc packetFilter : outgoing_filters) {
			packetFilter.getStatistics(list);
		}
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * This method can be overwritten in extending classes to get a different
	 * packets distribution to different threads. For PubSub, probably better
	 * packets distribution to different threads would be based on the
	 * sender address rather then destination address.
	 * @param packet
	 * @return
	 */
	public int hashCodeForPacket(Packet packet) {
		if ((packet.getFrom() != null) && (packet.getFrom() != packet.getStanzaFrom())) {

			// This comes from connection manager so the best way is to get hashcode
			// by the connectionId, which is in the getFrom()
			return packet.getFrom().hashCode();
		}

		// If not, then a better way is to get hashCode from the elemTo address
		// as this would be by the destination address user name:
		if (packet.getStanzaTo() != null) {
			return packet.getStanzaTo().hashCode();
		}

		if (packet.getTo() != null) {
			return packet.getTo().hashCode();
		}

		return 1;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param address
	 *
	 * @return
	 */
	@Override
	public boolean isInRegexRoutings(String address) {

		// log.finest(getName() + " looking for regex routings: " + address);
		for (Pattern pat : regexRoutings) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest(getName() + " matching: " + address + " against " + pat.toString());
			}

			if (pat.matcher(address).matches()) {
				return true;
			}

			// log.finest(getName() + " matching failed against pattern: " + pat.toString());
		}

		return false;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param prefix
	 *
	 * @return
	 */
	public String newPacketId(String prefix) {
		StringBuilder sb = new StringBuilder(32);

		if (prefix != null) {
			sb.append(prefix).append("-");
		}

		sb.append(getName()).append(++packetId);

		return sb.toString();
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param results
	 */
	@Override
	public final void processPacket(final Packet packet, final Queue<Packet> results) {
		addPacketNB(packet);
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public int processingThreads() {
		return 1;
	}

	/**
	 * Method description
	 *
	 */
	@Override
	public void release() {
		stop();
	}

	/**
	 * Method description
	 *
	 *
	 * @param address
	 *
	 * @return
	 */
	public boolean removeRegexRouting(String address) {
		return regexRoutings.remove(Pattern.compile(address, Pattern.CASE_INSENSITIVE));
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param maxQueueSize
	 */
	public void setMaxQueueSize(int maxQueueSize) {
		if ((this.maxQueueSize != maxQueueSize) || (in_queues.size() == 0)) {
			this.maxQueueSize = maxQueueSize / processingThreads();

			if (in_queues.size() == 0) {
				for (int i = 0; i < in_queues_size; i++) {
					PriorityQueueAbstract<Packet> queue =
						PriorityQueueAbstract.getPriorityQueue(pr_cache.length, maxQueueSize);

					in_queues.add(queue);
				}
			} else {
				for (int i = 0; i < in_queues.size(); i++) {
					in_queues.get(i).setMaxSize(maxQueueSize);
				}
			}

			out_queue.setMaxSize(maxQueueSize);
		}    // end of if (this.maxQueueSize != maxQueueSize)
	}

	/**
	 * Method description
	 *
	 *
	 * @param name
	 */
	@Override
	public void setName(String name) {
		super.setName(name);
		in_queues_size = processingThreads();
		setMaxQueueSize(maxQueueSize);
	}

	/**
	 * Method description
	 *
	 *
	 * @param parent
	 */
	@Override
	public void setParent(MessageReceiver parent) {
		this.parent = parent;
	}

	/**
	 * Sets all configuration properties for object.
	 * @param props
	 */
	@Override
	@TODO(note = "Replace fixed filers loading with configurable options for that")
	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);

		int queueSize = (Integer) props.get(MAX_QUEUE_SIZE_PROP_KEY);

		setMaxQueueSize(queueSize);
		incoming_filters.clear();
		outgoing_filters.clear();

		String filters = (String) props.get(INCOMING_FILTERS_PROP_KEY);

		if ((filters != null) &&!filters.trim().isEmpty()) {
			String[] incoming = filters.trim().split(",");

			for (String inc : incoming) {
				try {
					PacketFilterIfc filter = (PacketFilterIfc) Class.forName(inc).newInstance();

					filter.init(getName(), QueueType.IN_QUEUE);
					incoming_filters.add(filter);
					log.config(getName() + " loaded incoming filter: " + inc);
				} catch (Exception e) {
					log.log(Level.WARNING, "Problem loading filter: " + inc + " in component: " + getName(),
							e);
				}
			}
		}

		filters = (String) props.get(OUTGOING_FILTERS_PROP_KEY);

		if ((filters != null) &&!filters.trim().isEmpty()) {
			String[] outgoing = filters.trim().split(",");

			for (String out : outgoing) {
				try {
					PacketFilterIfc filter = (PacketFilterIfc) Class.forName(out).newInstance();

					filter.init(getName(), QueueType.OUT_QUEUE);
					outgoing_filters.add(filter);
					log.config(getName() + " loaded outgoing filter: " + out);
				} catch (Exception e) {
					log.log(Level.WARNING, "Problem loading filter: " + out + " in component: " + getName(),
							e);
				}
			}
		}
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 */
	@Override
	public void start() {
		if (log.isLoggable(Level.FINER)) {
			log.info(getName() + ": starting queue management threads ...");
		}

		startThreads();
	}

	/**
	 * Method description
	 *
	 */
	public void stop() {
		if (log.isLoggable(Level.FINER)) {
			log.info(getName() + ": stopping queue management threads ...");
		}

		stopThreads();
	}

	protected boolean addOutPacket(Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("[" + getName() + "]  " + packet.toStringSecure());
		}

		try {
			out_queue.put(packet, packet.getPriority().ordinal());
			++statSentPacketsOk;
		} catch (InterruptedException e) {
			++statSentPacketsEr;

			return false;
		}    // end of try-catch

		return true;
	}

	/**
	 * Non blocking version of <code>addOutPacket</code>.
	 *
	 * @param packet a <code>Packet</code> value
	 * @return a <code>boolean</code> value
	 */
	protected boolean addOutPacketNB(Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("[" + getName() + "]  " + packet.toStringSecure());
		}

		boolean result = false;

		result = out_queue.offer(packet, packet.getPriority().ordinal());

		if (result) {
			++statSentPacketsOk;
		} else {

			// Queue overflow!
			++statSentPacketsEr;
		}

		return result;
	}

	protected boolean addOutPacketWithTimeout(Packet packet, ReceiverTimeoutHandler handler,
			long delay, TimeUnit unit) {

		// It is automatically added to collections and the Timer
		new PacketReceiverTask(handler, delay, unit, packet);

		return addOutPacket(packet);
	}

	protected boolean addOutPackets(Queue<Packet> packets) {
		Packet p = null;
		boolean result = true;

		while ((p = packets.peek()) != null) {
			result = addOutPacket(p);

			if (result) {
				packets.poll();
			} else {
				return false;
			}    // end of if (result) else
		}      // end of while ()

		return true;
	}

	protected void addTimerTask(TimerTask task, long delay, TimeUnit unit) {
		receiverTasks.schedule(task, unit.toMillis(delay));
	}

	protected void addTimerTask(TimerTask task, long delay) {
		receiverTasks.schedule(task, delay);
	}

	//~--- get methods ----------------------------------------------------------

	protected Integer getMaxQueueSize(int def) {
		return def;
	}

	//~--- methods --------------------------------------------------------------

	private Packet filterPacket(Packet packet, CopyOnWriteArrayList<PacketFilterIfc> filters) {
		Packet result = packet;

		for (PacketFilterIfc packetFilterIfc : filters) {
			result = packetFilterIfc.filter(result);

			if (result == null) {
				break;
			}
		}

		return result;
	}

	private void startThreads() {
		if (processingThreads == null) {
			processingThreads = new ArrayDeque<QueueListener>();

			for (int i = 0; i < in_queues_size; i++) {
				QueueListener in_thread = new QueueListener(in_queues.get(i), QueueType.IN_QUEUE);

				in_thread.setName("in_" + i + "-" + getName());
				in_thread.start();
				processingThreads.add(in_thread);
			}
		}    // end of if (thread == null || ! thread.isAlive())

		if ((out_thread == null) ||!out_thread.isAlive()) {
			out_thread = new QueueListener(out_queue, QueueType.OUT_QUEUE);
			out_thread.setName("out_" + getName());
			out_thread.start();
		}    // end of if (thread == null || ! thread.isAlive())

		receiverTasks = new Timer(getName() + " tasks", true);
		receiverTasks.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				everySecond();
			}
		}, SECOND, SECOND);
		receiverTasks.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				everyMinute();
			}
		}, MINUTE, MINUTE);
		receiverTasks.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				everyHour();
			}
		}, HOUR, HOUR);
	}

	private void stopThreads() {

		// stopped = true;
		try {
			if (processingThreads != null) {
				for (QueueListener in_thread : processingThreads) {
					in_thread.threadStopped = true;
					in_thread.interrupt();

					while (in_thread.isAlive()) {
						Thread.sleep(100);
					}
				}
			}

			if (out_thread != null) {
				out_thread.threadStopped = true;
				out_thread.interrupt();

				while (out_thread.isAlive()) {
					Thread.sleep(100);
				}
			}
		} catch (InterruptedException e) {}

		processingThreads = null;
		out_thread = null;

		if (receiverTasks != null) {
			receiverTasks.cancel();
			receiverTasks = null;
		}
	}

	//~--- inner classes --------------------------------------------------------

	private class PacketReceiverTask extends TimerTask {
		private ReceiverTimeoutHandler handler = null;
		private String id = null;
		private Packet packet = null;

		//~--- constructors -------------------------------------------------------

		private PacketReceiverTask(ReceiverTimeoutHandler handler, long delay, TimeUnit unit,
				Packet packet) {
			super();
			this.handler = handler;
			this.packet = packet;
			id = packet.getFrom().toString() + packet.getStanzaId();
			waitingTasks.put(id, this);
			receiverTasks.schedule(this, unit.toMillis(delay));

//    log.finest("[" + getName() + "]  " + "Added timeout task for: " + id);
		}

		//~--- methods ------------------------------------------------------------

		/**
		 * Method description
		 *
		 *
		 * @param response
		 */
		public void handleResponse(Packet response) {

			// waitingTasks.remove(packet.getFrom() + packet.getId());
			this.cancel();

//    log.finest("[" + getName() + "]  " + "Response received for id: " + id);
			handler.responseReceived(packet, response);
		}

		/**
		 * Method description
		 *
		 */
		public void handleTimeout() {

//    log.finest("[" + getName() + "]  " + "Fired timeout for id: " + id);
			waitingTasks.remove(id);
			handler.timeOutExpired(packet);
		}

		/**
		 * Method description
		 *
		 */
		@Override
		public void run() {
			handleTimeout();
		}
	}


	private class QueueListener extends Thread {
		private QueueType type = null;
		private boolean threadStopped = false;
		private PriorityQueueAbstract<Packet> queue;

		//~--- constructors -------------------------------------------------------

		private QueueListener(PriorityQueueAbstract<Packet> q, QueueType type) {
			this.queue = q;
			this.type = type;
		}

		//~--- methods ------------------------------------------------------------

		/**
		 * Method description
		 *
		 */
		@Override
		public void run() {
			if (log.isLoggable(Level.FINEST)) {
				log.finest(getName() + " starting queue processing.");
			}

			Packet packet = null;
			Queue<Packet> results = new ArrayDeque<Packet>();

			while ( !threadStopped) {
				try {

					// Now process next waiting packet
//        log.finest("[" + getName() + "] before take... " + type);
					// packet = queue.take(getName() + ":" + type);
					packet = queue.take();

//        if (log.isLoggable(Level.FINEST)) {
//          log.finest("[" + getName() + "] packet from " + type +
//                  " queue: " + packet.toString());
//        }
					switch (type) {
						case IN_QUEUE :
							long startPPT = System.currentTimeMillis();

//            tracer.trace(null, packet.getElemTo(), packet.getElemFrom(),
//                    packet.getFrom(), getName(), type.name(), null, packet);
							String id = packet.getTo().toString() + packet.getStanzaId();
							PacketReceiverTask task = waitingTasks.remove(id);

							if (task != null) {
								task.handleResponse(packet);
							} else {

//              log.finest("[" + getName() + "]  " +
//                      "No task found for id: " + id);
								// Maybe this is a command for local processing...
								boolean processed = false;

								if (packet.isCommand() && getComponentId().equals(packet.getStanzaTo())) {
									processed = processScriptCommand(packet, results);

									if (processed) {
										Packet result = null;

										while ((result = results.poll()) != null) {
											addOutPacket(result);
										}
									}
								}

								if ( !processed && ((packet = filterPacket(packet, incoming_filters)) != null)) {
									processPacket(packet);
								}

								processPacketTimings[pptIdx] = System.currentTimeMillis() - startPPT;
								pptIdx = (pptIdx + 1) % processPacketTimings.length;
							}

							break;

						case OUT_QUEUE :

//            tracer.trace(null, packet.getElemTo(), packet.getElemFrom(),
//                    packet.getTo(), getName(), type.name(), null, packet);
							if ((packet = filterPacket(packet, outgoing_filters)) != null) {
								if (parent != null) {
									parent.addPacket(packet);
								} else {

									// It may happen for MessageRouter and this is intentional
									addPacketNB(packet);

									// log.warning("[" + getName() + "]  " + "No parent!");
								}    // end of else
							}

							break;

						default :
							log.severe("Unknown queue element type: " + type);

							break;
					}          // end of switch (qel.type)
				} catch (InterruptedException e) {

					// log.log(Level.SEVERE, "Exception during packet processing: ", e);
					// stopped = true;
				} catch (Exception e) {
					log.log(Level.SEVERE,
							"[" + getName() + "] Exception during packet processing: " + packet.toStringSecure(),
								e);
				}    // end of try-catch
			}      // end of while (! threadStopped)
		}
	}
}    // AbstractMessageReceiver


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
