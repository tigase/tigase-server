/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 * 
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.server.monitor;

import tigase.sys.*;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.logging.*;

/**
 * Created: Feb 19, 2009 12:31:14 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MonitorRuntime extends TigaseRuntime {

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(MonitorRuntime.class.getName());

	private static MonitorRuntime runtime = null;
	private final LinkedHashSet<ShutdownHook> shutdownHooks =
					new LinkedHashSet<ShutdownHook>();
	private final LinkedList<OnlineJidsReporter> onlineJidsReporters =
					new LinkedList<OnlineJidsReporter>();

	private MonitorRuntime() {
		super();
		Runtime.getRuntime().addShutdownHook(new MainShutdownThread());
	}
	
	public static MonitorRuntime getMonitorRuntime() {
		if (runtime == null) {
			runtime = new MonitorRuntime();
		}
		return runtime;
	}

	@Override
	public synchronized void addShutdownHook(ShutdownHook hook) {
		shutdownHooks.add(hook);
	}

	@Override
	public synchronized void addMemoryChangeListener(MemoryChangeListener memListener) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public synchronized void addCPULoadListener(CPULoadListener cpuListener) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public synchronized void addOnlineJidsReporter(OnlineJidsReporter onlineReporter) {
		onlineJidsReporters.add(onlineReporter);
	}

	@Override
	public boolean hasCompleteJidsInfo() {
		if (onlineJidsReporters.size() == 1) {
			return onlineJidsReporters.getFirst().hasCompleteJidsInfo();
		} else {
			for (OnlineJidsReporter onlineJidsReporter : onlineJidsReporters) {
				if (!onlineJidsReporter.hasCompleteJidsInfo()) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public boolean isJidOnline(JID jid) {
		if (onlineJidsReporters.size() == 1) {
			return onlineJidsReporters.getFirst().containsJid(jid.getBareJID());
		} else {
			for (OnlineJidsReporter onlineJidsReporter : onlineJidsReporters) {
				if (onlineJidsReporter.containsJid(jid.getBareJID())) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean isJidOnlineLocally(BareJID jid) {
		if (onlineJidsReporters.size() == 1) {
			return onlineJidsReporters.getFirst().containsJidLocally(jid);
		} else {
			for (OnlineJidsReporter onlineJidsReporter : onlineJidsReporters) {
				if (onlineJidsReporter.containsJidLocally(jid)) {
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean isJidOnlineLocally(JID jid) {
		if (onlineJidsReporters.size() == 1) {
			return onlineJidsReporters.getFirst().containsJidLocally(jid);
		} else {
			for (OnlineJidsReporter onlineJidsReporter : onlineJidsReporters) {
				if (onlineJidsReporter.containsJidLocally(jid)) {
					return true;
				}
			}
		}
		return false;
	}	
	
	@Override
	public JID[] getConnectionIdsForJid(JID jid) {
		if (onlineJidsReporters.size() == 1) {
			return onlineJidsReporters.getFirst().getConnectionIdsForJid(jid.getBareJID());
		} else {
			for (OnlineJidsReporter onlineJidsReporter : onlineJidsReporters) {
				JID[] connIds = onlineJidsReporter.getConnectionIdsForJid(jid.getBareJID());
				if (connIds != null) {
					return connIds;
				}
			}
		}
		return null;
	}

	private class ShutdownHandlerThread extends Thread {

		private ShutdownHook hook = null;
		private String result = null;

		public ShutdownHandlerThread(ThreadGroup group, ShutdownHook hook) {
			super(group, hook.getName());
			this.hook = hook;
			setDaemon(true);
		}

		@Override
		public void run () {
			result = hook.shutdown();
		}

		public String getResultMessage() {
			return result;
		}

	}
	
	private class MainShutdownThread extends Thread {

		public MainShutdownThread() {
			super();
			setName("MainShutdownThread");
		}

		@Override
		public void run() {
			System.out.println("ShutdownThread started...");
			log.warning("ShutdownThread started...");
			LinkedList<ShutdownHandlerThread> thlist = 
							new LinkedList<ShutdownHandlerThread>();
			ThreadGroup threads =
							new ThreadGroup(Thread.currentThread().getThreadGroup(),
							"Tigase Shutdown");
			for (ShutdownHook shutdownHook : shutdownHooks) {
				ShutdownHandlerThread thr =
								new ShutdownHandlerThread(threads, shutdownHook);
				thr.start();
				thlist.add(thr);
			}
			// We allow for max 10 secs for the shutdown code to run...
			long shutdownStart = System.currentTimeMillis();
			while (threads.activeCount() > 0 &&
							(System.currentTimeMillis() - shutdownStart) < 10000) {
				try {
					sleep(100);
				} catch (Exception e) {	}
			}
			StringBuilder sb = new StringBuilder();
			for (ShutdownHandlerThread shutdownHandlerThread : thlist) {
				if (shutdownHandlerThread.getResultMessage() != null) {
					sb.append(shutdownHandlerThread.getResultMessage());
				}
			}
			ThreadMXBean thBean = ManagementFactory.getThreadMXBean();
			sb.append("\nTotal number of threads: " + thBean.getThreadCount()).append('\n');
			long[] tids = thBean.findDeadlockedThreads();
			if (tids != null && tids.length > 0) {
				sb.append("Locked threads:\n");
				ThreadInfo[] lockedThreads = thBean.getThreadInfo(tids);
				for (ThreadInfo threadInfo : lockedThreads) {
					sb.append("Locked thread ")
							.append(threadInfo.getThreadName())
							.append(" on ")
							.append(threadInfo.getLockInfo().toString())
							.append(", locked synchronizes: ")
							.append(Arrays.toString(threadInfo.getLockedSynchronizers()))
							.append(", locked monitors: ")
							.append(Arrays.toString(threadInfo.getLockedMonitors()))
							.append(" by [")
							.append(threadInfo.getLockOwnerId())
							.append("] ")
							.append(threadInfo.getLockOwnerName())
							.append('\n');
					StackTraceElement[] ste = threadInfo.getStackTrace();
					for (StackTraceElement stackTraceElement : ste) {
						sb.append(stackTraceElement.toString()).append('\n');
					}
				}
			} else {
				sb.append("No locked threads.\n");
			}
			if (sb.length() > 0) {
				System.out.println(sb.toString());
				log.warning(sb.toString());
			}


			String SHUTDOWN_THREAD_DUMP = "shutdown-thread-dump";
			if (System.getProperty(SHUTDOWN_THREAD_DUMP) == null ||
					Boolean.TRUE.equals(Boolean.valueOf(System.getProperty(SHUTDOWN_THREAD_DUMP)))) {

				try {
					// we have to configure logger here
					Logger THREAD_DUMP_LOGGER = Logger.getLogger("ThreadDumpLogger");
					String threadDumpPath = "logs/thread-dump.log";
					FileHandler fileHandler = new FileHandler(threadDumpPath, 10000000, 5, true);
					fileHandler.setLevel(Level.ALL);
					fileHandler.setFormatter(new Formatter() {
						@Override
						public String format(LogRecord record) {
							return (new Date(record.getMillis()) + ": " + record.getMessage());
						}
					});

					THREAD_DUMP_LOGGER.addHandler(fileHandler);
					THREAD_DUMP_LOGGER.setLevel(Level.ALL);
					THREAD_DUMP_LOGGER.setUseParentHandlers(false);

					StringBuilder threadDumpBuilder = new StringBuilder("All threads information:\n");
					for (ThreadInfo threadInfo : ManagementFactory.getThreadMXBean().dumpAllThreads(true, true)) {
						threadDumpBuilder.append(threadInfo);
					}
					threadDumpBuilder.append("\n===========\n\n");
					THREAD_DUMP_LOGGER.log(Level.INFO, threadDumpBuilder.toString());

					String msg =
							"Save thread-dump to file: " + threadDumpPath + ", size: " + threadDumpBuilder.length();
					System.out.println(msg);
					log.warning(msg);
				} catch (IOException e) {
					System.out.println("exception while initialization");
					e.printStackTrace();
					log.log(Level.WARNING, "Failed creating thread dumper logger");
				}
			}


			System.out.println("ShutdownThread finished...");
			log.warning("ShutdownThread finished...");
		}
	}

}
