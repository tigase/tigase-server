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

//~--- non-JDK imports --------------------------------------------------------

import tigase.annotations.TODO;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class SocketThread here.
 *
 *
 * Created: Mon Jan 30 12:01:17 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SocketThread implements Runnable {
	private static final Logger log = Logger.getLogger(SocketThread.class.getName());

	/** Field description */
	public static final int DEF_MAX_THREADS_PER_CPU = 8;
	private static final int MAX_EMPTY_SELECTIONS = 10;
	private static SocketThread[] socketReadThread = null;
	private static SocketThread[] socketWriteThread = null;
	private static int cpus = Runtime.getRuntime().availableProcessors();
	private static ThreadPoolExecutor executor = null;

	/**
	 * Variable <code>completionService</code> keeps reference to server thread pool.
	 * There is only one thread pool used by all server modules. Each module requiring
	 * separate threads for tasks processing must have access to server thread pool.
	 */
	private static CompletionService<IOService<?>> completionService = null;

	//~--- static initializers --------------------------------------------------

//private static int threadNo = 0;
//private static final int READ_ONLY = SelectionKey.OP_READ;
//private static final int READ_WRITE = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
	static {
		if (socketReadThread == null) {
			int nThreads = (cpus * DEF_MAX_THREADS_PER_CPU) / 2 + 1;

			executor = new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
					new LinkedBlockingQueue<Runnable>());
			completionService = new ExecutorCompletionService<IOService<?>>(executor);
			socketReadThread = new SocketThread[nThreads];
			socketWriteThread = new SocketThread[nThreads];

			for (int i = 0; i < socketReadThread.length; i++) {
				socketReadThread[i] = new SocketThread("socketReadThread-" + i);
				socketReadThread[i].reading = true;

				Thread thrd = new Thread(socketReadThread[i]);

				thrd.setName("socketReadThread-" + i);
				thrd.start();
			}

			log.log(Level.WARNING, "{0} socketReadThreads started.", socketReadThread.length);

			for (int i = 0; i < socketWriteThread.length; i++) {
				socketWriteThread[i] = new SocketThread("socketWriteThread-" + i);
				socketWriteThread[i].writing = true;

				Thread thrd = new Thread(socketWriteThread[i]);

				thrd.setName("socketWriteThread-" + i);
				thrd.start();
			}

			log.log(Level.WARNING, "{0} socketWriteThreads started.", socketWriteThread.length);
		}    // end of if (acceptThread == null)
	}

	//~--- fields ---------------------------------------------------------------

	private Selector clientsSel = null;

	// private boolean selecting = false;
	private int empty_selections = 0;
	private boolean reading = false;
	private boolean writing = false;
	private ConcurrentSkipListSet<IOService<?>> waiting =
		new ConcurrentSkipListSet<IOService<?>>(new IOServiceComparator());
	private boolean stopping = false;

	// IOServices must be added to thread pool after they are removed from
	// the selector and the selector and key is cleared, otherwise we have
	// dead-lock somewhere down in the:
	// java.nio.channels.spi.AbstractSelectableChannel.removeKey(AbstractSelectableChannel.java:111)
	private ConcurrentSkipListSet<IOService<?>> forCompletion =
		new ConcurrentSkipListSet<IOService<?>>(new IOServiceComparator());

	//~--- constructors ---------------------------------------------------------

	/**
	 * Creates a new <code>SocketThread</code> instance.
	 *
	 */
	private SocketThread(String name) {
		try {
			clientsSel = Selector.open();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Server I/O error, can't continue my work.", e);
			stopping = true;
		}    // end of try-catch

		new ResultsListener("ResultsListener-" + name).start();
	}

	//~--- methods --------------------------------------------------------------

///**
// * Method description
// *
// *
// * 
// */
//public static SocketThread getInstance() {
//  return socketReadThread[0];
//}
//private static final Object threadNoSync = new Object();
//private int incrementAndGet() {
//int result = 0;
//synchronized (threadNoSync) {
//  threadNo = (threadNo + 1) % socketReadThread.length;
//  result = threadNo;
//}
//return result;
//}

	/**
	 * Method description
	 *
	 *
	 * @param s
	 */
	public static void addSocketService(IOService<?> s) {
		s.setSocketServiceReady(true);
		// Due to a delayed SelectionKey cancelling deregistering
		// nature this distribution doesn't work well, it leads to
		// dead-lock. Let's make sure the service is always processed
		// by the same thread thus the same Selector.
		// socketReadThread[incrementAndGet()].addSocketServicePriv(s);
		if (s.waitingToRead()) {
			socketReadThread[s.hashCode() % socketReadThread.length].addSocketServicePriv(s);
		}

		if (s.waitingToSend()) {
			socketWriteThread[s.hashCode() % socketWriteThread.length].addSocketServicePriv(s);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param s
	 */
	public static void removeSocketService(IOService<Object> s) {
		s.setSocketServiceReady(false);
		socketReadThread[s.hashCode() % socketReadThread.length].removeSocketServicePriv(s);
		socketWriteThread[s.hashCode() % socketWriteThread.length].removeSocketServicePriv(s);
	}

	/**
	 * Method description
	 *
	 *
	 * @param s
	 */
	@SuppressWarnings("unchecked")
	public void addSocketServicePriv(IOService<?> s) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Adding to waiting: {0}", s);
		}

		waiting.add((IOService<Object>) s);

		// Calling lazy wakeup to avoid multiple wakeup calls
		// when lots of new services are added....
		clientsSel.wakeup();

		// wakeupHelper.wakeup();
	}

	public void removeSocketServicePriv(IOService<?> s) {
		waiting.remove(s);
		
		SelectionKey key = s.getSocketChannel().keyFor(clientsSel);
		if ((key != null) && (key.attachment() == s)) {
			key.cancel();
		}
	}
	
	@SuppressWarnings({ "unchecked" })
	@Override
	public void run() {
		while ( !stopping) {
			try {
				clientsSel.select();

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Selector AWAKE: {0}", clientsSel);
				}

				Set<SelectionKey> selected = clientsSel.selectedKeys();
				int selectedKeys = selected.size();

				if ((selectedKeys == 0) && (waiting.size() == 0)) {
					if (log.isLoggable(Level.FINEST)) {
						log.finest("Selected keys = 0!!! a bug again?");
					}

					if ((++empty_selections) > MAX_EMPTY_SELECTIONS) {
						recreateSelector();
					}
				} else {
					empty_selections = 0;

					if (selectedKeys > 0) {

						// This is dirty but selectNow() causes concurrent modification exception
						// and the selectNow() is needed because of a bug in JVM mentioned below
						for (SelectionKey sk : selected) {

							// According to most guides we should use below code
							// removing SelectionKey from iterator, however a little later
							// we do cancel() on this key so removing is somehow redundant
							// and causes concurrency exception if a few calls are performed
							// at the same time.
							// selected_keys.remove(sk);
							IOService s = (IOService) sk.attachment();

							try {
								if (log.isLoggable(Level.FINEST)) {
									StringBuilder sb = new StringBuilder("AWAKEN: " + s.getUniqueId());

									if (sk.isWritable()) {
										sb.append(", ready for WRITING");
									}

									if (sk.isReadable()) {
										sb.append(", ready for READING");
									}

									sb.append(", readyOps() = ").append(sk.readyOps());
									log.finest(sb.toString());
								}

								// Set<SelectionKey> selected_keys = clientsSel.selectedKeys();
								// for (SelectionKey sk : selected_keys) {
								// Handling a bug or not a bug described in the
								// last comment to this issue:
								// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4850373
								// and
								// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6403933
								sk.cancel();
								forCompletion.add(s);

								// IOServices must be added to thread pool after they are removed from
								// the selector and the selector and key is cleared, otherwise we have
								// dead-lock somewhere down in the:
								// java.nio.channels.spi.AbstractSelectableChannel.
								// removeKey(AbstractSelectableChannel.java:111)
								// completionService.submit(s);
							} catch (CancelledKeyException e) {
								if (log.isLoggable(Level.FINEST)) {
									log.log(Level.FINEST, "CancelledKeyException, stopping the connection: {0}",
											s.getUniqueId());
								}

								try {
									s.forceStop();
								} catch (Exception ex2) {
									if (log.isLoggable(Level.WARNING)) {
										log.log(Level.WARNING, "got exception during forceStop: {0}", e);
									}
								}
							}
						}
					}

					// Clean-up cancelled keys...
					clientsSel.selectNow();
				}

				addAllWaiting();

				IOService serv = null;

				while ((serv = forCompletion.pollFirst()) != null) {
					completionService.submit(serv);
				}

				// clientsSel.selectNow();
			} catch (CancelledKeyException brokene) {

				// According to Java API that should not happen.
				// I think it happens only on the broken Java implementation
				// from Apple.
				log.log(Level.WARNING, "Ups, broken JDK, Apple? ", brokene);

				try {
					recreateSelector();
				} catch (Exception e) {
					log.log(Level.SEVERE, "Serious problem, can't recreate selector: ", e);

					// stopping = true;
				}
			} catch (IOException ioe) {

				// According to Java API that should not happen.
				// I think it happens only on the broken Java implementation from Apple
				// and due to a bug: http://bugs.sun.com/view_bug.do?bug_id=6693490
				log.log(Level.WARNING, "Problem with the network connection: ", ioe);

				try {
					recreateSelector();
				} catch (Exception e) {
					log.log(Level.SEVERE, "Serious problem, can't recreate selector: ", e);

					// stopping = true;
				}
			} catch (Exception exe) {
				log.log(Level.SEVERE, "Server I/O error: ", exe);

				try {
					recreateSelector();
				} catch (Exception e) {
					log.log(Level.SEVERE, "Serious problem, can't recreate selector: ", e);

					// stopping = true;
				}

				// stopping = true;
			}
		}
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param threads
	 */
	public void setMaxThread(int threads) {
		executor.setCorePoolSize(threads);
		executor.setMaximumPoolSize(threads);
	}

	/**
	 * Method description
	 *
	 *
	 * @param threads
	 */
	public void setMaxThreadPerCPU(int threads) {
		setMaxThread(threads * cpus);
	}

	//~--- methods --------------------------------------------------------------

	private void addAllWaiting() throws IOException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "waiting.size(): {0}", waiting.size());
		}

		IOService s = null;

		// boolean added = false;
		while ((s = waiting.pollFirst()) != null) {
			SocketChannel sc = s.getSocketChannel();

			try {
				if (sc.isConnected()) {
					if (reading) {
						sc.register(clientsSel, SelectionKey.OP_READ, s);

						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "ADDED OP_READ: {0}", s.getUniqueId());
						}
					}

					if (writing) {
						sc.register(clientsSel, SelectionKey.OP_WRITE, s);

						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "ADDED OP_WRITE: {0}", s.getUniqueId());
						}
					}

					// added = true;
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Socket not connected: {0}", s.getUniqueId());
					}

					try {
						if (log.isLoggable(Level.FINER)) {
							log.log(Level.FINER, "Forcing stopping the service: {0}", s.getUniqueId());
						}

						s.forceStop();
					} catch (Exception e) {
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "Exception while stopping service: " + s.getUniqueId(), e);
						}
					}
				}
			} catch (Exception e) {
				if (log.isLoggable(Level.FINER)) {
					log.log(Level.FINER, "Forcing stopping the service: " + s.getUniqueId(), e);
				}

				try {
					s.forceStop();
				} catch (Exception ez) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Exception while stopping service: " + s.getUniqueId(), ez);
					}
				}
			}    // end of try-catch
		}      // end of for ()

//  if (added) {
//    clientsSel.wakeup();
//  }
	}

	// Implementation of java.lang.Runnable
	private synchronized void recreateSelector() throws IOException {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Recreating selector, opened channels: {0}", clientsSel.keys().size());
		}

		empty_selections = 0;

		// Handling a bug or not a bug described in the
		// last comment to this issue:
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4850373
		// and
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6403933
		// Recreating the selector and registering all channles with
		// the new selector
		// Selector tempSel = clientsSel;
		// clientsSel = Selector.open();
		boolean cancelled = false;

		// Sometimes this is just a broken connection which causes
		// selector spin... this is the cheaper solution....
		for (SelectionKey sk : clientsSel.keys()) {
			IOService<?> serv = (IOService<?>) sk.attachment();
			SocketChannel sc = serv.getSocketChannel();

			if ((sc == null) ||!sc.isConnected()) {
				cancelled = true;
				sk.cancel();

				try {
					log.log(Level.INFO, "Forcing stopping the service: {0}", serv.getUniqueId());
					serv.forceStop();
				} catch (Exception e) {}
			}

			// waiting.offer(serv);
		}

		if (cancelled) {
			clientsSel.selectNow();
		} else {

			// Unfortunately must be something wrong with the selector
			// itself, now more expensive calculations...
			Selector tempSel = clientsSel;

			clientsSel = Selector.open();

			for (SelectionKey sk : tempSel.keys()) {
				IOService<?> serv = (IOService<?>) sk.attachment();

				sk.cancel();
				waiting.add(serv);
			}

			tempSel.close();
		}
	}

	//~--- inner classes --------------------------------------------------------

	private class IOServiceComparator implements Comparator<IOService<?>> {

		@Override
		public int compare(IOService<?> o1, IOService<?> o2) {
			return o1.getUniqueId().compareTo(o2.getUniqueId());
		}
	}


	@TODO(note = "ExecutionException is poorly implemented.")
	protected class ResultsListener extends Thread {

		/**
		 * Constructs ...
		 *
		 *
		 * @param name
		 */
		public ResultsListener(String name) {
			super();
			setName(name);
		}

		//~--- methods ------------------------------------------------------------

		@Override
		public void run() {
			for (;;) {
				try {
					IOService<?> service = completionService.take().get();

					if (service != null) {
						if (service.isConnected()) {
							if (log.isLoggable(Level.FINEST)) {
								log.log(Level.FINEST, "COMPLETED: {0}", service.getUniqueId());
							}

							addSocketService(service);
						} else {
							if (log.isLoggable(Level.FINEST)) {
								log.log(Level.FINEST, "REMOVED: {0}", service.getUniqueId());
							}
						}    // end of else
					}
				} catch (ExecutionException e) {
					log.log(Level.WARNING, "Protocol execution exception.", e.getCause());

					// TODO: Do something with this
				}        // end of catch
						catch (InterruptedException e) {
					log.log(Level.WARNING, "Protocol execution interrupted.", e);
				}        // end of try-catch
						catch (Exception e) {
					log.log(Level.WARNING, "Protocol execution unknown exception.", e);
				}        // end of catch
			}          // end of for ()
		}
	}
}    // SocketThread


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
