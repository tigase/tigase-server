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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.LinkedList;
import java.util.logging.Logger;
import tigase.sys.CPULoadListener;
import tigase.sys.MemoryChangeListener;
import tigase.sys.OnlineJidsReporter;
import tigase.sys.ShutdownHook;
import tigase.sys.TigaseRuntime;
import tigase.xmpp.JID;

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
  private static final Logger log =
					Logger.getLogger(MonitorRuntime.class.getName());

	private static MonitorRuntime runtime = null;
	private LinkedList<ShutdownHook> shutdownHooks =
					new LinkedList<ShutdownHook>();
	private LinkedList<OnlineJidsReporter> onlineJidsReporters =
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

	/**
	 *
	 * @param jid
	 * @return
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
				sb.append("Locked thread " + threadInfo.getThreadName() + " on " +
								threadInfo.getLockInfo().toString()).append('\n');
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

}
