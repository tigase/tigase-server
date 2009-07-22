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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Pattern;
import tigase.annotations.TODO;
import tigase.conf.Configurable;
import tigase.server.filters.PacketCounter;
import tigase.stats.StatisticType;
import tigase.stats.StatisticsContainer;
import tigase.stats.StatisticsList;
import tigase.util.JIDUtils;
import tigase.util.DNSResolver;
import tigase.util.PriorityQueue;
import tigase.util.tracer.TigaseTracer;
import tigase.vhosts.VHostItem;
import tigase.vhosts.VHostListener;
import tigase.vhosts.VHostManagerIfc;

/**
 * Describe class AbstractMessageReceiver here.
 *
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class AbstractMessageReceiver
  implements StatisticsContainer, MessageReceiver, Configurable, VHostListener {

	protected static final long SECOND = 1000;
	protected static final long MINUTE = 60*SECOND;
	protected static final long HOUR = 60*MINUTE;

	private String DEF_HOSTNAME_PROP_VAL = DNSResolver.getDefaultHostname();
	public static final String MAX_QUEUE_SIZE_PROP_KEY = "max-queue-size";
	//  public static final Integer MAX_QUEUE_SIZE_PROP_VAL = Integer.MAX_VALUE;
  public static final Integer MAX_QUEUE_SIZE_PROP_VAL =
    new Long(Runtime.getRuntime().maxMemory()/400000L).intValue();

  // String added intentionally!! 
	// Don't change to AbstractMessageReceiver.class.getName()
	/**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.abstract.AbstractMessageReceiver");
	private static final TigaseTracer tracer = TigaseTracer.getTracer("abstract");

  protected int maxQueueSize = MAX_QUEUE_SIZE_PROP_VAL;
	private String defHostname = DEF_HOSTNAME_PROP_VAL;

  private MessageReceiver parent = null;

//	private final EnumMap<Priority, LinkedBlockingQueue<Packet>> in_queues =
//					new EnumMap<Priority, LinkedBlockingQueue<Packet>>(Priority.class);
//	private final EnumMap<Priority, LinkedBlockingQueue<Packet>> out_queues =
//					new EnumMap<Priority, LinkedBlockingQueue<Packet>>(Priority.class);

	// Array cache to speed processing up....
	private Priority[] pr_cache = Priority.values();

	private int in_queues_size = 1;
	private ArrayList<PriorityQueue<Packet>> in_queues =
		new ArrayList<PriorityQueue<Packet>>();
  private PriorityQueue<Packet> out_queue =
		new PriorityQueue<Packet>(pr_cache.length, maxQueueSize);

	private CopyOnWriteArrayList<PacketFilterIfc> incoming_filters =
					new CopyOnWriteArrayList<PacketFilterIfc>();
	private CopyOnWriteArrayList<PacketFilterIfc> outgoing_filters =
					new CopyOnWriteArrayList<PacketFilterIfc>();

	// 	private String sync = "SyncObject";
	private Timer receiverTasks = null;
	private ConcurrentHashMap<String, ReceiverTask> waitingTasks =
					new ConcurrentHashMap<String, ReceiverTask>(16, 0.75f, 4);
	private LinkedList<QueueListener> processingThreads = null;
	private QueueListener out_thread = null;
  //private boolean stopped = false;
  private String name = null;
	protected VHostManagerIfc vHostManager = null;
	//private Set<String> routings = new CopyOnWriteArraySet<String>();
	private Set<Pattern> regexRoutings = new CopyOnWriteArraySet<Pattern>();
	private long last_second_packets = 0;
	private long packets_per_second = 0;
	private long last_minute_packets = 0;
	private long packets_per_minute = 0;
	private long last_hour_packets = 0;
	private long packets_per_hour = 0;
//	private long curr_second = 0;
//	private long curr_minute = 0;
//	private long curr_hour = 0;
//	private long[] seconds = new long[60];
//	private int sec_idx = 0;
//	private long[] minutes = new long[60];
//	private int min_idx = 0;
	private String compId = null;
	private long[] processPacketTimings = new long[100];
	private int pptIdx = 0;

  /**
   * Variable <code>statAddedMessagesOk</code> keeps counter of successfuly
   * added messages to queue.
   */
  private long statReceivedPacketsOk = 0;
  private long statSentPacketsOk = 0;
  /**
   * Variable <code>statAddedMessagesEr</code> keeps counter of unsuccessfuly
   * added messages due to queue overflow.
   */
  private long statReceivedPacketsEr = 0;
  private long statSentPacketsEr = 0;

	/**
	 * Describe <code>getComponentId</code> method here.
	 *
	 * @return a <code>String</code> value
	 */
	@Override
	public String getComponentId() {
		return compId;
	}
	
	@Override
	public void initializationCompleted() {}

	private boolean filterPacket(Packet packet,
					CopyOnWriteArrayList<PacketFilterIfc> filters) {
		boolean result = false;
		for (PacketFilterIfc packetFilterIfc : filters) {
			result = packetFilterIfc.filter(packet);
			if (result) {
				break;
			}
		}
		return result;
	}

	/**
	 * This method can be overwritten in extending classes to get a different
	 * packets distribution to different threads. For PubSub, probably better
	 * packets distribution to different threads would be based on the
	 * sender address rather then destination address.
	 * @param packet
	 * @return
	 */
	public int hashCodeForPacket(Packet packet) {
		return packet.getTo().hashCode();
	}

	@Override
  public boolean addPacketNB(Packet packet) {
		int queueIdx = Math.abs(hashCodeForPacket(packet) %	in_queues_size);
		if (log.isLoggable(Level.FINEST)) {
			log.finest("[" + getName() + "] queueIdx=" + queueIdx +
							", " + packet.toString());
		}
//		boolean result = in_queue.offer(packet, packet.getPriority().ordinal(),
//						getName() + ":" + QueueType.IN_QUEUE);
		//boolean result = in_queue.offer(packet, packet.getPriority().ordinal());
		boolean result = in_queues.get(queueIdx).offer(packet,
						packet.getPriority().ordinal());
		if (result) {
			++statReceivedPacketsOk;
//			++curr_second;
		} else {
			// Queue overflow!
			++statReceivedPacketsEr;
		}
		return result;
  }

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
			} // end of if (result) else
		} // end of while ()
    return true;
  }

	@Override
	public boolean addPacket(Packet packet) {
		int queueIdx = Math.abs(hashCodeForPacket(packet) %	in_queues_size);
		if (log.isLoggable(Level.FINEST)) {
			log.finest("[" + getName() + "] queueIdx=" + queueIdx +
							", " + packet.toString());
		}
		try {
//			in_queue.put(packet, packet.getPriority().ordinal(),
//							getName() + ":" + QueueType.IN_QUEUE);
			//in_queue.put(packet, packet.getPriority().ordinal());
			in_queues.get(queueIdx).put(packet, packet.getPriority().ordinal());
			++statReceivedPacketsOk;
//			++curr_second;
		} catch (InterruptedException e) {
			++statReceivedPacketsEr;
			return false;
		} // end of try-catch
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
			log.finest("[" + getName() + "]  " + packet.toString());
		}
		boolean result = false;
//		result = out_queue.offer(packet, packet.getPriority().ordinal(),
//						getName() + ":" + QueueType.OUT_QUEUE);
		result = out_queue.offer(packet, packet.getPriority().ordinal());
		if (result) {
			++statSentPacketsOk;
			//++curr_second;
		} else {
			// Queue overflow!
			++statSentPacketsEr;
		}
		return result;
	}

	protected boolean addOutPacketWithTimeout(Packet packet,
					ReceiverEventHandler handler, long delay,	TimeUnit unit) {
		// It is automatically added to collections and the Timer
		new ReceiverTask(handler, delay, unit, packet);
		return addOutPacket(packet);
	}

	protected boolean addOutPacket(Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("[" + getName() + "]  " + packet.toString());
		}
		try {
//			out_queue.put(packet, packet.getPriority().ordinal(),
//							getName() + ":" + QueueType.OUT_QUEUE);
			out_queue.put(packet, packet.getPriority().ordinal());
			++statSentPacketsOk;
			//++curr_second;
		} catch (InterruptedException e) {
			++statSentPacketsEr;
			return false;
		} // end of try-catch
		return true;
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
			} // end of if (result) else
		} // end of while ()
    return true;
	}

  public abstract void processPacket(Packet packet);

	@Override
  public void getStatistics(StatisticsList list) {
//		long tmp = seconds[(sec_idx == 0 ? 59 : sec_idx - 1)];
//		list.add(getName(), "Last second packets", tmp, Level.FINE);
		list.add(getName(), "Last second packets", packets_per_second, Level.FINE);
//		tmp = minutes[(min_idx == 0 ? 59 : min_idx - 1)];
//		list.add(getName(), "Last minute packets", tmp, Level.FINE);
		list.add(getName(), "Last minute packets", packets_per_minute, Level.FINE);
//		list.add(getName(), "Last hour packets", curr_hour, Level.FINE);
		list.add(getName(), "Last hour packets", packets_per_hour, Level.FINE);
		list.add(getName(), StatisticType.MSG_RECEIVED_OK.getDescription(),
						statReceivedPacketsOk, Level.FINE);
		list.add(getName(), StatisticType.MSG_SENT_OK.getDescription(),
						statSentPacketsOk, Level.FINE);
		int[] in_priority_sizes = in_queues.get(0).size();
		for (int i = 1; i < in_queues.size(); i++) {
			int[] tmp_pr_sizes = in_queues.get(i).size();
			for (int j = 0; j < tmp_pr_sizes.length; j++) {
				in_priority_sizes[j] += tmp_pr_sizes[j];
			}
		}
		int in_queue_size = 0;
		int[] out_priority_sizes = out_queue.size();
		int out_queue_size = 0;
		for (Priority queue : Priority.values()) {
			list.add(getName(), "In queue: " + queue.name(),
							in_priority_sizes[queue.ordinal()], Level.FINEST);
			list.add(getName(), "Out queue: " + queue.name(),
							out_priority_sizes[queue.ordinal()], Level.FINEST);
			in_queue_size += in_priority_sizes[queue.ordinal()];
			out_queue_size += out_priority_sizes[queue.ordinal()];
		}
		list.add(getName(), "Total In queues wait",
						in_queue_size, Level.INFO);
		list.add(getName(), "Total Out queues wait",
						out_queue_size, Level.INFO);
		list.add(getName(), StatisticType.MAX_QUEUE_SIZE.getDescription(),
						maxQueueSize, Level.FINEST);
		list.add(getName(), StatisticType.IN_QUEUE_OVERFLOW.getDescription(),
						statReceivedPacketsEr, Level.INFO);
		list.add(getName(), StatisticType.OUT_QUEUE_OVERFLOW.getDescription(),
						statSentPacketsEr, Level.INFO);
		long res = 0;
		for (long ppt : processPacketTimings) {
			res += ppt;
		}
		long prcessingTime = res / processPacketTimings.length;
		list.add(getName(),
						"Average processing time on last " +
						processPacketTimings.length + " runs [ms]",
						prcessingTime, Level.FINE);
		for (PacketFilterIfc packetFilter : incoming_filters) {
			packetFilter.getStatistics(list);
		}
		for (PacketFilterIfc packetFilter : outgoing_filters) {
			packetFilter.getStatistics(list);
		}
  }

  /**
   * Sets all configuration properties for object.
	 * @param props
	 */
	@Override
	@TODO(note="Replace fixed filers loading with configurable options for that")
  public void setProperties(Map<String, Object> props) {
		int queueSize = (Integer)props.get(MAX_QUEUE_SIZE_PROP_KEY);
		//stopThreads();
		setMaxQueueSize(queueSize);
		//startThreads();
		defHostname = (String)props.get(DEF_HOSTNAME_PROP_KEY);
		compId = (String)props.get(COMPONENT_ID_PROP_KEY);
		incoming_filters.clear();
		outgoing_filters.clear();
		PacketFilterIfc counter = new PacketCounter();
		counter.init(getName(), QueueType.IN_QUEUE);
		incoming_filters.add(counter);
		counter = new PacketCounter();
		counter.init(getName(), QueueType.OUT_QUEUE);
		outgoing_filters.add(counter);
		//addRouting(getComponentId());
  }

  public void setMaxQueueSize(int maxQueueSize) {
		if (this.maxQueueSize != maxQueueSize || in_queues.size() == 0) {
      this.maxQueueSize = maxQueueSize/processingThreads();
			if (in_queues.size() == 0) {
				for (int i = 0; i < in_queues_size; i++) {
					in_queues.add(new PriorityQueue<Packet>(pr_cache.length, maxQueueSize));
				}
			} else {
				for (int i = 0; i < in_queues.size(); i++) {
					in_queues.get(i).setMaxSize(maxQueueSize);
				}
			}
			out_queue.setMaxSize(maxQueueSize);
    } // end of if (this.maxQueueSize != maxQueueSize)
  }

	//   public void setLocalAddresses(String[] addresses) {
	//     localAddresses = addresses;
	//   }

	protected Integer getMaxQueueSize(int def) {
		return def;
	}

  /**
   * Returns defualt configuration settings for this object.
   */
	@Override
  public Map<String, Object> getDefaults(Map<String, Object> params) {
    Map<String, Object> defs = new LinkedHashMap<String, Object>();
		//maxQueueSize = MAX_QUEUE_SIZE_PROP_VAL;
		String queueSize = (String)params.get(GEN_MAX_QUEUE_SIZE);
		int queueSizeInt = MAX_QUEUE_SIZE_PROP_VAL;
		if (queueSize != null) {
			try {
				queueSizeInt = Integer.parseInt(queueSize);
			} catch (NumberFormatException e) {
				queueSizeInt = MAX_QUEUE_SIZE_PROP_VAL;
			}
		}
		defs.put(MAX_QUEUE_SIZE_PROP_KEY, getMaxQueueSize(queueSizeInt));
// 		if (params.get(GEN_VIRT_HOSTS) != null) {
// 			DEF_HOSTNAME_PROP_VAL = ((String)params.get(GEN_VIRT_HOSTS)).split(",")[0];
// 		} else {
		// The default hostname must be a real name of the machine and is a separate
		// thing from virtual hostnames. This is a critical parameter for proper
		// MessageRouter working.
		DEF_HOSTNAME_PROP_VAL = DNSResolver.getDefaultHostname();
// 		}
		defs.put(DEF_HOSTNAME_PROP_KEY, DEF_HOSTNAME_PROP_VAL);
		defs.put(COMPONENT_ID_PROP_KEY, compId);

    return defs;
  }

	@Override
  public void release() {
    stop();
  }

	@Override
  public void setParent(MessageReceiver parent) {
    this.parent = parent;
		//addRouting(getDefHostName());
  }

	@Override
  public void setName(String name) {
    this.name = name;
		compId = JIDUtils.getNodeID(name, defHostname);
		in_queues_size = processingThreads();
		setMaxQueueSize(maxQueueSize);
  }

	@Override
  public String getName() {
    return name;
  }

	private void stopThreads() {
    //stopped = true;
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

	public synchronized void everySecond() {
//		curr_minute -= seconds[sec_idx];
//		seconds[sec_idx] = curr_second;
//		curr_second = 0;
//		curr_minute += seconds[sec_idx];
//		if (sec_idx >= 59) {
//			sec_idx = 0;
//		} else {
//			++sec_idx;
//		}
    packets_per_second = statReceivedPacketsOk - last_second_packets;
		last_second_packets = statReceivedPacketsOk;
	}

	public synchronized void everyMinute() {
//		curr_hour -= minutes[min_idx];
//		minutes[min_idx] = curr_minute;
//		curr_hour += minutes[min_idx];
//		if (min_idx >= 59) {
//			min_idx = 0;
//		} else {
//			++min_idx;
//		}
    packets_per_minute = statReceivedPacketsOk - last_minute_packets;
		last_minute_packets = statReceivedPacketsOk;
	}

	public synchronized void everyHour() {
    packets_per_hour = statReceivedPacketsOk - last_hour_packets;
		last_hour_packets = statReceivedPacketsOk;
	}

	private void startThreads() {
		if (processingThreads == null) {
			//stopped = false;
			processingThreads = new LinkedList<QueueListener>();
			for (int i = 0; i < in_queues_size; i++) {
				QueueListener in_thread =
								new QueueListener(in_queues.get(i), QueueType.IN_QUEUE);
				in_thread.setName("in_" + i + "-" + name);
				in_thread.start();
				processingThreads.add(in_thread);
			}
		} // end of if (thread == null || ! thread.isAlive())
		if (out_thread == null || ! out_thread.isAlive()) {
			//stopped = false;
			out_thread = new QueueListener(out_queue, QueueType.OUT_QUEUE);
			out_thread.setName("out_" + name);
			out_thread.start();
		} // end of if (thread == null || ! thread.isAlive())
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

	@Override
	public void start() {
		if (log.isLoggable(Level.FINER)) {
			log.info(getName() + ": starting queue management threads ...");
		}
		startThreads();
  }

  public void stop() {
		if (log.isLoggable(Level.FINER)) {
			log.info(getName() + ": stopping queue management threads ...");
		}
		stopThreads();
  }

	@Override
	public String getDefHostName() {
		return defHostname;
	}

	@Override
	public boolean handlesLocalDomains() {
		return false;
	}

	@Override
	public boolean handlesNameSubdomains() {
		return true;
	}

	@Override
	public boolean handlesNonLocalDomains() {
		return false;
	}

	@Override
	public void setVHostManager(VHostManagerIfc manager) {
		this.vHostManager = manager;
	}

	public boolean isLocalDomain(String domain) {
		return vHostManager != null ? vHostManager.isLocalDomain(domain) : false;
	}

	public boolean isLocalDomainOrComponent(String domain) {
		return vHostManager != null ? vHostManager.isLocalDomainOrComponent(domain)
						: false;
	}

	public VHostItem getVHostItem(String domain) {
		return vHostManager != null ? vHostManager.getVHostItem(domain) : null;
	}

//	public Set<String> getRoutings() {
//		return routings;
//	}

//	public void addRouting(String address) {
//		routings.add(address);
//		log.fine(getName() + " - added routing: " + address);
//	}

//	public boolean removeRouting(String address) {
//		return routings.remove(address);
//	}

//	public void clearRoutings() {
//		routings.clear();
//	}

//	public boolean isInRoutings(String host) {
//		return routings.contains(host);
//	}

	public Set<Pattern> getRegexRoutings() {
		return regexRoutings;
	}

	public void addRegexRouting(String address) {
		if (log.isLoggable(Level.FINE)) {
			log.fine(getName() + " - attempt to add regex routing: " + address);
		}
		regexRoutings.add(Pattern.compile(address, Pattern.CASE_INSENSITIVE));
		if (log.isLoggable(Level.FINE)) {
			log.fine(getName() + " - success adding regex routing: " + address);
		}
	}

	public boolean removeRegexRouting(String address) {
		return regexRoutings.remove(Pattern.compile(address,
				Pattern.CASE_INSENSITIVE));
	}

	public void clearRegexRoutings() {
		regexRoutings.clear();
	}

	@Override
	public boolean isInRegexRoutings(String address) {
		// 		log.finest(getName() + " looking for regex routings: " + address);
		for (Pattern pat: regexRoutings) {
			if (pat.matcher(address).matches()) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest(getName() + " matched against pattern: " + pat.toString());
				}
				return true;
			}
			// 			log.finest(getName() + " matching failed against pattern: " + pat.toString());
		}
		return false;
	}

	@Override
	public final void processPacket(final Packet packet,
		final Queue<Packet> results)	{
		addPacketNB(packet);
	}

	public int processingThreads() {
		return 1;
	}

	private class QueueListener extends Thread {

		private PriorityQueue<Packet> queue;
		private QueueType type = null;
		private boolean threadStopped = false;

		private QueueListener(PriorityQueue<Packet> q, QueueType type) {
			this.queue = q;
			this.type = type;
		}

		@Override
		public void run() {
			if (log.isLoggable(Level.FINEST)) {
				log.finest(getName() + " starting queue processing.");
			}
			Packet packet = null;
			while (! threadStopped) {
				try {
					// Now process next waiting packet
//					log.finest("[" + getName() + "] before take... " + type);
					//packet = queue.take(getName() + ":" + type);
					packet = queue.take();
//					if (log.isLoggable(Level.FINEST)) {
//						log.finest("[" + getName() + "] packet from " + type +
//										" queue: " + packet.toString());
//					}
					switch (type) {
						case IN_QUEUE:
							tracer.trace(null, packet.getElemTo(), packet.getElemFrom(),
											packet.getFrom(),	getName(), type.name(), null, packet);
							String id = packet.getTo() + packet.getId();
							ReceiverTask task = waitingTasks.remove(id);
							if (task != null) {
								task.handleResponse(packet);
							} else {
//								log.finest("[" + getName() + "]  " +
//												"No task found for id: " + id);
								long startPPT = System.currentTimeMillis();
								if (!filterPacket(packet, incoming_filters)) {
									processPacket(packet);
								}
								processPacketTimings[pptIdx] =
												System.currentTimeMillis() - startPPT;
								pptIdx = (pptIdx + 1) % processPacketTimings.length;
							}
							break;
						case OUT_QUEUE:
							tracer.trace(null, packet.getElemTo(), packet.getElemFrom(),
											packet.getTo(),	getName(), type.name(), null, packet);
							if (!filterPacket(packet, outgoing_filters)) {
								if (parent != null) {
									parent.addPacket(packet);
								} else {
									// It may happen for MessageRouter and this is intentional
									addPacketNB(packet);
								//log.warning("[" + getName() + "]  " + "No parent!");
								} // end of else
							}
							break;
						default:
							log.severe("Unknown queue element type: " + type);
							break;
					} // end of switch (qel.type)
				} catch (InterruptedException e) {
					//log.log(Level.SEVERE, "Exception during packet processing: ", e);
					//				stopped = true;
				} catch (Exception e) {
					log.log(Level.SEVERE, "[" + getName() +
									"] Exception during packet processing: " +
									packet.toString(), e);
				} // end of try-catch
			} // end of while (! threadStopped)
		}

	}

	private class ReceiverTask extends TimerTask {

		private ReceiverEventHandler handler = null;
		private Packet packet = null;
		private String id = null;

		private ReceiverTask(ReceiverEventHandler handler, long delay,
						TimeUnit unit, Packet packet) {
			super();
			this.handler = handler;
			this.packet = packet;
			id = packet.getFrom() + packet.getId();
			waitingTasks.put(id, this);
			receiverTasks.schedule(this, unit.toMillis(delay));
//			log.finest("[" + getName() + "]  " + "Added timeout task for: " + id);
		}

		@Override
		public void run() {
			handleTimeout();
		}
		
		public void handleTimeout() {
//			log.finest("[" + getName() + "]  " + "Fired timeout for id: " + id);
			waitingTasks.remove(packet.getFrom() + packet.getId());
			handler.timeOutExpired(packet);
		}

		public void handleResponse(Packet response) {
			//waitingTasks.remove(packet.getFrom() + packet.getId());
			this.cancel();
//			log.finest("[" + getName() + "]  " + "Response received for id: " + id);
			handler.responseReceived(packet, response);
		}

	}

} // AbstractMessageReceiver
