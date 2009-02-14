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

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Pattern;
import tigase.conf.Configurable;
import tigase.stats.StatRecord;
import tigase.stats.StatisticType;
import tigase.stats.StatisticsContainer;
import tigase.util.JIDUtils;
import tigase.util.DNSResolver;
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
    new Long(Runtime.getRuntime().maxMemory()/200000L).intValue();

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.abstract.AbstractMessageReceiver");

  protected int maxQueueSize = MAX_QUEUE_SIZE_PROP_VAL;
	private String defHostname = DEF_HOSTNAME_PROP_VAL;

  private MessageReceiver parent = null;
	//	private Timer delayedTask = new Timer("MessageReceiverTask", true);

	private final EnumMap<Priority, LinkedBlockingQueue<Packet>> in_queues =
					new EnumMap<Priority, LinkedBlockingQueue<Packet>>(Priority.class);
	private final EnumMap<Priority, LinkedBlockingQueue<Packet>> out_queues =
					new EnumMap<Priority, LinkedBlockingQueue<Packet>>(Priority.class);

//  private LinkedBlockingQueue<Packet> in_queue =
//		new LinkedBlockingQueue<Packet>(maxQueueSize);
//  private LinkedBlockingQueue<Packet> out_queue =
//		new LinkedBlockingQueue<Packet>(maxQueueSize);

	// 	private String sync = "SyncObject";

	// Array cache to speed processing up....
	private Priority[] pr_cache = Priority.values();

	private Timer receiverTasks = null;
	private Thread in_thread = null;
	private Thread out_thread = null;
  private boolean stopped = false;
  private String name = null;
	protected VHostManagerIfc vHostManager = null;
	//private Set<String> routings = new CopyOnWriteArraySet<String>();
	private Set<Pattern> regexRoutings = new CopyOnWriteArraySet<Pattern>();
	private long curr_second = 0;
	private long curr_minute = 0;
	private long curr_hour = 0;
	private long[] seconds = new long[60];
	private int sec_idx = 0;
	private long[] minutes = new long[60];
	private int min_idx = 0;
	private String compId = null;
	private long[] processPacketTimings = new long[100];
	private int pptIdx = 0;

  /**
   * Variable <code>statAddedMessagesOk</code> keeps counter of successfuly
   * added messages to queue.
   */
  private long statReceivedMessagesOk = 0;
  private long statSentMessagesOk = 0;
  /**
   * Variable <code>statAddedMessagesEr</code> keeps counter of unsuccessfuly
   * added messages due to queue overflow.
   */
  private long statReceivedMessagesEr = 0;
  private long statSentMessagesEr = 0;

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

	private boolean tryLowerPriority(Packet packet, EnumMap<Priority,
					LinkedBlockingQueue<Packet>> queues) {
		boolean result = false;
		int q_num = packet.getPriority().ordinal();
		if (q_num < (pr_cache.length - 1)) {
			synchronized (queues) {
				result = queues.get(pr_cache[q_num + 1]).offer(packet);
				queues.notifyAll();
			}
		}
		return result;
	}

	@Override
  public boolean addPacketNB(Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("[" + getName() + "]  " + packet.toString());
		}
		boolean result = false;
		synchronized (in_queues) {
			result = in_queues.get(packet.getPriority()).offer(packet);
			in_queues.notifyAll();
		}
		if (result) {
			++statReceivedMessagesOk;
			++curr_second;
		} else {
			// Queue overflow!
			++statReceivedMessagesEr;
			// System monitor should be notified here now
			// In the meantime let's try to fit the packet in another, lower
			// priority queue
			tryLowerPriority(packet, in_queues);
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
		if (log.isLoggable(Level.FINEST)) {
			log.finest("[" + getName() + "]  " + packet.toString());
		}
		try {
			synchronized (in_queues) {
				in_queues.get(packet.getPriority()).put(packet);
				in_queues.notifyAll();
			}
			++statReceivedMessagesOk;
			++curr_second;
		} catch (InterruptedException e) {
			++statReceivedMessagesEr;
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
		synchronized (out_queues) {
			result = out_queues.get(packet.getPriority()).offer(packet);
			out_queues.notifyAll();
		}
		if (result) {
			++statSentMessagesOk;
			//++curr_second;
		} else {
			// Queue overflow!
			++statSentMessagesEr;
			// System monitor should be notified here now
			// In the meantime let's try to fit the packet in another, lower
			// priority queue
			tryLowerPriority(packet, out_queues);
		}
		return result;
	}

	protected boolean addOutPacket(Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("[" + getName() + "]  " + packet.toString());
		}
		try {
			synchronized (out_queues) {
				out_queues.get(packet.getPriority()).put(packet);
				out_queues.notifyAll();
			}
			++statSentMessagesOk;
			//++curr_second;
		} catch (InterruptedException e) {
			++statSentMessagesEr;
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
  public List<StatRecord> getStatistics() {
    List<StatRecord> stats = new LinkedList<StatRecord>();
		stats.add(new StatRecord(getName(), "Last second packets", "int",
				seconds[(sec_idx == 0 ? 59 : sec_idx - 1)], Level.FINE));
		stats.add(new StatRecord(getName(), "Last minute packets", "int",
				minutes[(min_idx == 0 ? 59 : min_idx - 1)], Level.FINE));
		stats.add(new StatRecord(getName(), "Last hour packets", "int",
				curr_hour, Level.FINE));
    stats.add(new StatRecord(getName(), StatisticType.MSG_RECEIVED_OK,
				statReceivedMessagesOk, Level.FINE));
    stats.add(new StatRecord(getName(), StatisticType.MSG_SENT_OK,
				statSentMessagesOk, Level.FINE));
		int in_queues_size = 0;
		int out_queues_size = 0;
		for (Priority queue : Priority.values()) {
			stats.add(new StatRecord(getName(), "In queue: " + queue.name(), "int",
							in_queues.get(queue).size(), Level.FINEST));
			stats.add(new StatRecord(getName(), "Out queue: " + queue.name(), "int",
							out_queues.get(queue).size(), Level.FINEST));
			in_queues_size += in_queues.get(queue).size();
			out_queues_size += out_queues.get(queue).size();
		}
		stats.add(new StatRecord(getName(), "Total In queues wait", "int",
						in_queues_size, Level.FINE));
		stats.add(new StatRecord(getName(), "Total Out queues wait", "int",
						out_queues_size, Level.FINE));
    stats.add(new StatRecord(getName(), StatisticType.MAX_QUEUE_SIZE,
				maxQueueSize, Level.FINE));
    stats.add(new StatRecord(getName(), StatisticType.IN_QUEUE_OVERFLOW,
				statReceivedMessagesEr, Level.FINE));
    stats.add(new StatRecord(getName(), StatisticType.OUT_QUEUE_OVERFLOW,
				statSentMessagesEr, Level.FINE));
		long res = 0;
		for (long ppt : processPacketTimings) {
			res += ppt;
		}
		stats.add(new StatRecord(getName(),
						"Average processing time on last " +
						processPacketTimings.length + " runs [ms]", "long",
				(res/processPacketTimings.length), Level.FINEST));
    return stats;
  }

  /**
   * Sets all configuration properties for object.
	 * @param props
	 */
	@Override
  public void setProperties(Map<String, Object> props) {
    int queueSize = (Integer)props.get(MAX_QUEUE_SIZE_PROP_KEY);
		stopThreads();
		setMaxQueueSize(queueSize);
		startThreads();
		defHostname = (String)props.get(DEF_HOSTNAME_PROP_KEY);
		compId = (String)props.get(COMPONENT_ID_PROP_KEY);
		//addRouting(getComponentId());
  }

  public void setMaxQueueSize(int maxQueueSize) {
    boolean initialized = in_queues.get(Priority.SYSTEM) != null;
		if (this.maxQueueSize != maxQueueSize || !initialized) {
      this.maxQueueSize = maxQueueSize;
			LinkedBlockingQueue<Packet> queue = null;
			LinkedBlockingQueue<Packet> newQueue = null;
			for (Priority pr : Priority.values()) {
				queue = in_queues.get(pr);
				newQueue = new LinkedBlockingQueue<Packet>(maxQueueSize);
				if (queue != null) {
					newQueue.addAll(queue);
				} // end of if (queue != null)
				in_queues.put(pr, newQueue);
				queue = out_queues.get(pr);
				newQueue = new LinkedBlockingQueue<Packet>(maxQueueSize);
				if (queue != null) {
					newQueue.addAll(queue);
				} // end of if (queue != null)
				out_queues.put(pr, newQueue);
			}
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
		setMaxQueueSize(maxQueueSize);
  }

	@Override
  public String getName() {
    return name;
  }

	private void stopThreads() {
    stopped = true;
		try {
			if (in_thread != null) {
				in_thread.interrupt();
				while (in_thread.isAlive()) {
					Thread.sleep(100);
				}
			}
			if (out_thread != null) {
				out_thread.interrupt();
				while (out_thread.isAlive()) {
					Thread.sleep(100);
				}
			}
		} catch (InterruptedException e) {}
		in_thread = null;
		out_thread = null;
		if (receiverTasks != null) {
			receiverTasks.cancel();
			receiverTasks = null;
		}
	}

	public synchronized void everySecond() {
		curr_minute -= seconds[sec_idx];
		seconds[sec_idx] = curr_second;
		curr_second = 0;
		curr_minute += seconds[sec_idx];
		if (sec_idx >= 59) {
			sec_idx = 0;
		} else {
			++sec_idx;
		}
	}

	public synchronized void everyMinute() {
		curr_hour -= minutes[min_idx];
		minutes[min_idx] = curr_minute;
		curr_hour += minutes[min_idx];
		if (min_idx >= 59) {
			min_idx = 0;
		} else {
			++min_idx;
		}
	}

	private void startThreads() {
		if (in_thread == null || ! in_thread.isAlive()) {
			stopped = false;
			in_thread = new Thread(new QueueListener(in_queues, QueueType.IN_QUEUE));
			in_thread.setName("in_" + name);
			in_thread.start();
		} // end of if (thread == null || ! thread.isAlive())
		if (out_thread == null || ! out_thread.isAlive()) {
			stopped = false;
			out_thread = new Thread(new QueueListener(out_queues, QueueType.OUT_QUEUE));
			out_thread.setName("out_" + name);
			out_thread.start();
		} // end of if (thread == null || ! thread.isAlive())
		receiverTasks = new Timer(getName() + " tasks", true);
		receiverTasks.schedule(new TimerTask() {
				public void run() {
					everySecond();
				}
			}, SECOND, SECOND);
		receiverTasks.schedule(new TimerTask() {
				public void run() {
					everyMinute();
				}
			}, MINUTE, MINUTE);
	}

	@Override
	public void start() {
		log.finer(getName() + ": starting queue management threads ...");
		startThreads();
  }

  public void stop() {
		log.finer(getName() + ": stopping queue management threads ...");
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
		log.fine(getName() + " - attempt to add regex routing: " + address);
		regexRoutings.add(Pattern.compile(address, Pattern.CASE_INSENSITIVE));
		log.fine(getName() + " - success adding regex routing: " + address);
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
				log.finest(getName() + " matched against pattern: " + pat.toString());
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

	private enum QueueType { IN_QUEUE, OUT_QUEUE }

	private class QueueListener implements Runnable {

		private final EnumMap<Priority, LinkedBlockingQueue<Packet>> queues;
		private QueueType type = null;

		private QueueListener(EnumMap<Priority, LinkedBlockingQueue<Packet>> q,
						QueueType type) {
			this.queues = q;
			this.type = type;
		}

		@Override
		public void run() {
			Packet packet = null;
			while (! stopped) {
				try {
					// According to the spec, queues.values() must return values
					// in the same order the enum has been declared, the highest
					// order for the earliest declared enums/queues
					//log.finest("checking queues:");
					for (Map.Entry<Priority, LinkedBlockingQueue<Packet>> queueEntry : queues.entrySet()) {
						// The log call must be commented out after initial tests
						// made to ensure queues are polled in a correct order
						//log.finest("Queue: " + queueEntry.getKey().name());
						while ((packet = queueEntry.getValue().poll()) != null) {
							switch (type) {
								case IN_QUEUE:
									long startPPT = System.currentTimeMillis();
									processPacket(packet);
									processPacketTimings[pptIdx] =
													System.currentTimeMillis() - startPPT;
									if (pptIdx >= (processPacketTimings.length - 1)) {
										pptIdx = 0;
									} else {
										++pptIdx;
									}
									break;
								case OUT_QUEUE:
									if (parent != null) {
										parent.addPacket(packet);
									} else {
										// It may happen for MessageRouter and this is intentional
										addPacketNB(packet);
									//log.warning("[" + getName() + "]  " + "No parent!");
									} // end of else
									break;
								default:
									log.severe("Unknown queue element type: " + type);
									break;
							} // end of switch (qel.type)
						}
					}
				  // Let's make sure there was nothing added to any queue in the meantime
					synchronized (queues) {
						boolean added = false;
						for (LinkedBlockingQueue<Packet> queue : queues.values()) {
							if (added = (queue.size() > 0)) {
								break;
							}
						}
						if (!added) {
							queues.wait();
						}
					}
				} catch (InterruptedException e) {
					//log.log(Level.SEVERE, "Exception during packet processing: ", e);
					//				stopped = true;
				} catch (Exception e) {
					log.log(Level.SEVERE, "[" + getName() +
									"] Exception during packet processing: " +
									packet.toString(), e);
				} // end of try-catch
			} // end of while (! stopped)
		}

	}

} // AbstractMessageReceiver
