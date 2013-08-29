/*
 * MonitorRuntime.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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
 */



package tigase.server.monitor;

//~--- non-JDK imports --------------------------------------------------------

import tigase.sys.CPULoadListener;
import tigase.sys.MemoryChangeListener;
import tigase.sys.OnlineJidsReporter;
import tigase.sys.ShutdownHook;
import tigase.sys.TigaseRuntime;

import tigase.xmpp.JID;

//~--- JDK imports ------------------------------------------------------------

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import java.util.LinkedList;
import java.util.logging.Logger;

/**
 * Created: Feb 19, 2009 12:31:14 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MonitorRuntime
				extends TigaseRuntime {
	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger   log     = Logger.getLogger(MonitorRuntime.class
			.getName());
	private static MonitorRuntime runtime = null;

	//~--- fields ---------------------------------------------------------------

	private LinkedList<ShutdownHook>       shutdownHooks = new LinkedList<ShutdownHook>();
	private LinkedList<OnlineJidsReporter> onlineJidsReporters =
			new LinkedList<OnlineJidsReporter>();

	//~--- constructors ---------------------------------------------------------

	private MonitorRuntime() {
		super();
		Runtime.getRuntime().addShutdownHook(new MainShutdownThread());
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param cpuListener is a <code>CPULoadListener</code>
	 */
	@Override
	public synchronized void addCPULoadListener(CPULoadListener cpuListener) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Method description
	 *
	 *
	 * @param memListener is a <code>MemoryChangeListener</code>
	 */
	@Override
	public synchronized void addMemoryChangeListener(MemoryChangeListener memListener) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Method description
	 *
	 *
	 * @param onlineReporter is a <code>OnlineJidsReporter</code>
	 */
	@Override
	public synchronized void addOnlineJidsReporter(OnlineJidsReporter onlineReporter) {
		onlineJidsReporters.add(onlineReporter);
	}

	/**
	 * Method description
	 *
	 *
	 * @param hook is a <code>ShutdownHook</code>
	 */
	@Override
	public synchronized void addShutdownHook(ShutdownHook hook) {
		shutdownHooks.add(hook);
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 *
	 * @param jid
	 *
	 *
	 * @return a value of <code>JID[]</code>
	 */
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

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>MonitorRuntime</code>
	 */
	public static MonitorRuntime getMonitorRuntime() {
		if (runtime == null) {
			runtime = new MonitorRuntime();
		}

		return runtime;
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
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

	/**
	 * Method description
	 *
	 *
	 * @param jid is a <code>JID</code>
	 *
	 * @return a value of <code>boolean</code>
	 */
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

	//~--- inner classes --------------------------------------------------------

	private class MainShutdownThread
					extends Thread {
		/**
		 * Constructs ...
		 *
		 */
		public MainShutdownThread() {
			super();
			setName("MainShutdownThread");
		}

		//~--- methods ------------------------------------------------------------

		/**
		 * Method description
		 *
		 */
		@Override
		public void run() {
			System.out.println("ShutdownThread started...");
			log.warning("ShutdownThread started...");

			LinkedList<ShutdownHandlerThread> thlist = new LinkedList<ShutdownHandlerThread>();
			ThreadGroup                       threads = new ThreadGroup(Thread.currentThread()
					.getThreadGroup(), "Tigase Shutdown");

			for (ShutdownHook shutdownHook : shutdownHooks) {
				ShutdownHandlerThread thr = new ShutdownHandlerThread(threads, shutdownHook);

				thr.start();
				thlist.add(thr);
			}

			// We allow for max 10 secs for the shutdown code to run...
			long shutdownStart = System.currentTimeMillis();

			while ((threads.activeCount() > 0) && (System.currentTimeMillis() -
					shutdownStart) < 10000) {
				try {
					sleep(100);
				} catch (Exception e) {}
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

			if ((tids != null) && (tids.length > 0)) {
				sb.append("Locked threads:\n");

				ThreadInfo[] lockedThreads = thBean.getThreadInfo(tids);

				for (ThreadInfo threadInfo : lockedThreads) {
					sb.append("Locked thread " + threadInfo.getThreadName() + " on " + threadInfo
							.getLockInfo().toString()).append('\n');

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
			System.out.println("ShutdownThread finished...");
			log.warning("ShutdownThread finished...");
		}
	}


	private class ShutdownHandlerThread
					extends Thread {
		private ShutdownHook hook   = null;
		private String       result = null;

		//~--- constructors -------------------------------------------------------

		/**
		 * Constructs ...
		 *
		 *
		 * @param group
		 * @param hook
		 */
		public ShutdownHandlerThread(ThreadGroup group, ShutdownHook hook) {
			super(group, hook.getName());
			this.hook = hook;
			setDaemon(true);
		}

		//~--- methods ------------------------------------------------------------

		/**
		 * Method description
		 *
		 */
		@Override
		public void run() {
			result = hook.shutdown();
		}

		//~--- get methods --------------------------------------------------------

		/**
		 * Method description
		 *
		 *
		 * @return a value of <code>String</code>
		 */
		public String getResultMessage() {
			return result;
		}
	}
}


//~ Formatted in Tigase Code Convention on 13/08/28
