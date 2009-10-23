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
package tigase.net;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Comparator;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import tigase.annotations.TODO;

/**
 * Describe class SocketReadThread here.
 *
 *
 * Created: Mon Jan 30 12:01:17 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SocketReadThread implements Runnable {

  private static final Logger log =
		Logger.getLogger("tigase.net.SocketReadThread");

	public static final int DEF_MAX_THREADS_PER_CPU = 5;
	private static final int MAX_EMPTY_SELECTIONS = 10;

	private static SocketReadThread[] socketReadThread = null;
	private static int cpus = Runtime.getRuntime().availableProcessors();
  private boolean stopping = false;
	//private boolean selecting = false;
	private int empty_selections = 0;

  private final ConcurrentSkipListSet<IOService> waiting =
    new ConcurrentSkipListSet<IOService>(new IOServiceComparator());
			// IOServices must be added to thread pool after they are removed from
		// the selector and the selector and key is cleared, otherwise we have
		// dead-lock somewhere down in the:
		// java.nio.channels.spi.AbstractSelectableChannel.removeKey(AbstractSelectableChannel.java:111)
	private ConcurrentSkipListSet<IOService> forCompletion =
					new ConcurrentSkipListSet<IOService>(new IOServiceComparator());

  private Selector clientsSel = null;
	private static ThreadPoolExecutor executor = null;
  /**
   * Variable <code>completionService</code> keeps reference to server thread pool.
   * There is only one thread pool used by all server modules. Each module requiring
   * separate threads for tasks processing must have access to server thread pool.
   */
  private static CompletionService<IOService> completionService = null;
	private static int threadNo = 0;

	/**
	 * Creates a new <code>SocketReadThread</code> instance.
	 *
	 */
	private SocketReadThread() {
		try {
			clientsSel = Selector.open();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Server I/O error, can't continue my work.", e);
			stopping = true;
		} // end of try-catch
    new ResultsListener().start();
	}

	public static SocketReadThread getInstance() {
		if (socketReadThread == null) {
			int nThreads = cpus * DEF_MAX_THREADS_PER_CPU;
			executor = new ThreadPoolExecutor(nThreads, nThreads,
				0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
			completionService =
				new ExecutorCompletionService<IOService>(executor);
			socketReadThread = new SocketReadThread[cpus];
			for (int i = 0; i < socketReadThread.length; i++) {
				socketReadThread[i] = new SocketReadThread();
				Thread thrd = new Thread(socketReadThread[i]);
				thrd.setName("SocketReadThread_CPU-"+i);
				thrd.start();
			}
			log.warning("" + socketReadThread.length + " SocketReadThreads started.");
		} // end of if (acceptThread == null)
		return socketReadThread[0];
	}

	public void setMaxThreadPerCPU(int threads) {
		setMaxThread(threads * cpus);
	}

	public void setMaxThread(int threads) {
		executor.setCorePoolSize(threads);
		executor.setMaximumPoolSize(threads);
	}

//	private static final Object threadNoSync = new Object();
//	private int incrementAndGet() {
//		int result = 0;
//		synchronized (threadNoSync) {
//			threadNo = (threadNo + 1) % socketReadThread.length;
//			result = threadNo;
//		}
//		return result;
//	}

	public void addSocketService(IOService s) {
		// Due to a delayed SelectionKey cancelling deregistering
		// nature this distribution doesn't work well, it leads to
		// dead-lock. Let's make sure the service is always processed
		// by the same thread thus the same Selector.
		//socketReadThread[incrementAndGet()].addSocketServicePriv(s);
		socketReadThread[s.hashCode() % socketReadThread.length].addSocketServicePriv(s);
	}

	public void addSocketServicePriv(IOService s) {
    waiting.add(s);
		// Calling lazy wakeup to avoid multiple wakeup calls
		// when lots of new services are added....
		clientsSel.wakeup();
		//wakeupHelper.wakeup();
	}

	public void removeSocketService(IOService s) {
		SelectionKey key = s.getSocketChannel().keyFor(clientsSel);
		if (key != null && key.attachment() == s) {
			key.cancel();
		} // end of if (key != null)
	}

	private static final int READ_ONLY = SelectionKey.OP_READ;
	private static final int READ_WRITE =
		SelectionKey.OP_READ | SelectionKey.OP_WRITE;

	private void addAllWaiting() throws IOException {

    IOService s = null;
    while ((s = waiting.pollFirst()) != null) {
      SocketChannel sc = s.getSocketChannel();
      try {
				if (sc.isConnected()) {
					int sel_key = READ_ONLY;
					if (log.isLoggable(Level.FINEST)) {
						log.finest("ADDED OP_READ: " + s.getUniqueId());
					}
					if (s.waitingToSend()) {
						sel_key = READ_WRITE;
						if (log.isLoggable(Level.FINEST)) {
							log.finest("ADDED OP_WRITE: " + s.getUniqueId());
						}
					}
					sc.register(clientsSel, sel_key, s);
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.finest("Socket not connected: " + s.getUniqueId());
					}
					try {
						if (log.isLoggable(Level.FINER)) {
							log.finer("Forcing stopping the service: " + s.getUniqueId());
						}
						s.forceStop();
					} catch (Exception e) {
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "Exception while stopping service: " +
											s.getUniqueId(), e);
						}
					}
				}
			} catch (Exception e) {
				if (log.isLoggable(Level.FINER)) {
					log.log(Level.FINER, "Forcing stopping the service: " +
									s.getUniqueId(), e);
				}
				try {
					s.forceStop();
				} catch (Exception ez) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "Exception while stopping service: " +
										s.getUniqueId(), ez);
					}
				}
      } // end of try-catch
    } // end of for ()

  }

	// Implementation of java.lang.Runnable

	private synchronized void recreateSelector() throws IOException {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Recreating selector, opened channels: " + clientsSel.keys().size());
		}
		empty_selections = 0;
		// Handling a bug or not a bug described in the
		// last comment to this issue:
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4850373
		// and
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6403933
		// Recreating the selector and registering all channles with
		// the new selector
			//Selector tempSel = clientsSel;
			//clientsSel = Selector.open();
		boolean cancelled = false;
		// Sometimes this is just a broken connection which causes
		// selector spin... this is the cheaper solution....
		for (SelectionKey sk: clientsSel.keys()) {
			IOService serv = (IOService) sk.attachment();
			SocketChannel sc = serv.getSocketChannel();
			if (sc == null || !sc.isConnected()) {
				cancelled = true;
				sk.cancel();
				try {
					log.info("Forcing stopping the service: " + serv.getUniqueId());
					serv.forceStop();
				} catch (Exception e) {
				}
			}
		  //waiting.offer(serv);
		}
		if (cancelled) {
			clientsSel.selectNow();
		} else {
			// Unfortunately must be something wrong with the selector
			// itself, now more expensive calculations...
			Selector tempSel = clientsSel;
			clientsSel = Selector.open();
			for (SelectionKey sk: tempSel.keys()) {
				IOService serv = (IOService)sk.attachment();
				sk.cancel();
				waiting.add(serv);
			}
			tempSel.close();
		}
	}

	/**
	 * Describe <code>run</code> method here.
	 *
	 */
	@SuppressWarnings({"unchecked"})
	@Override
	public void run() {
    while (!stopping) {
      try {
				clientsSel.select();
				Set<SelectionKey> selected = clientsSel.selectedKeys();
				int selectedKeys = selected.size();
				if(selectedKeys == 0 && waiting.size() == 0) {
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
							//selected_keys.remove(sk);
							IOService s = (IOService)sk.attachment();
							try {
								if (log.isLoggable(Level.FINEST)) {
									StringBuilder sb = new StringBuilder("AWAKEN: " + s.getUniqueId());
									if (sk.isWritable()) {
										sb.append(", ready for WRITING");
									}
									if (sk.isReadable()) {
										sb.append(", ready for READING");
									}
									sb.append(", readyOps() = " + sk.readyOps());
									log.finest(sb.toString());
								}
								//         Set<SelectionKey> selected_keys = clientsSel.selectedKeys();
								//         for (SelectionKey sk : selected_keys) {
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
							  // java.nio.channels.spi.AbstractSelectableChannel.removeKey(AbstractSelectableChannel.java:111)
							  //completionService.submit(s);
							} catch (CancelledKeyException e) {
								if (log.isLoggable(Level.FINEST)) {
									log.finest("CancelledKeyException, stopping the connection: "
									+ s.getUniqueId());
								}
								try {	
									s.forceStop();
								} catch (Exception ex2) {
									if (log.isLoggable(Level.WARNING)) {
										log.warning("got exception during forceStop: " + e);
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
				//clientsSel.selectNow();
			} catch (CancelledKeyException brokene) {
				// According to Java API that should not happen.
				// I think it happens only on the broken Java implementation
				// from Apple.
				log.log(Level.WARNING, "Ups, broken JDK, Apple? ", brokene);
				try {
					recreateSelector();
				} catch (Exception e) {
					log.log(Level.SEVERE, "Serious problem, can't recreate selector: ", e);
					//stopping = true;
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
					//stopping = true;
				}
      } catch (Exception exe) {
        log.log(Level.SEVERE, "Server I/O error: ", exe);
				try {
					recreateSelector();
				} catch (Exception e) {
					log.log(Level.SEVERE, "Serious problem, can't recreate selector: ", e);
					//stopping = true;
				}
        //stopping = true;
      }
    }
	}

  @TODO(note="ExecutionException is poorly implemented.")
  protected class ResultsListener extends Thread {

    public ResultsListener() {
			super();
			setName("SocketReadThread$ResultsListener");
		}

		@Override
		public void run() {

			for (;;) {
				try {
					final IOService service = completionService.take().get();
					if (service.isConnected()) {
						if (log.isLoggable(Level.FINEST)) {
							log.finest("COMPLETED: " + service.getUniqueId());
						}
						addSocketService(service);
					} else {
						if (log.isLoggable(Level.FINEST)) {
							log.finest("REMOVED: " + service.getUniqueId());
						}
					} // end of else
				} catch (ExecutionException e) {
          log.log(Level.WARNING, "Protocol execution exception.", e);
          // TODO: Do something with this
        } // end of catch
        catch (InterruptedException e) {
          log.log(Level.WARNING, "Protocol execution interrupted.", e);
        } // end of try-catch
        catch (Exception e) {
          log.log(Level.WARNING, "Protocol execution unknown exception.", e);
        } // end of catch
      } // end of for ()

    }

  }

	private class IOServiceComparator implements Comparator<IOService> {

		@Override
		public int compare(IOService o1, IOService o2) {
			return o1.getUniqueId().compareTo(o2.getUniqueId());
		}

	}

} // SocketReadThread
