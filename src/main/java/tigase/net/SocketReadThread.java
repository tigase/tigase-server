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
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
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

	private static SocketReadThread socketReadThread = null;

  private boolean stopping = false;

  private final ConcurrentLinkedQueue<IOService> waiting =
    new ConcurrentLinkedQueue<IOService>();
  private final ConcurrentLinkedQueue<IOService> for_removal =
    new ConcurrentLinkedQueue<IOService>();
  private Selector clientsSel = null;
	private ThreadPoolExecutor executor = null;
  /**
   * Variable <code>completionService</code> keeps reference to server thread pool.
   * There is only one thread pool used by all server modules. Each module requiring
   * separate threads for tasks processing must have access to server thread pool.
   */
  private CompletionService<IOService> completionService = null;

	/**
	 * Creates a new <code>SocketReadThread</code> instance.
	 *
	 */
	private SocketReadThread() {
		try {
			clientsSel = Selector.open();
			int cpus = Runtime.getRuntime().availableProcessors();
			int nThreads = cpus * DEF_MAX_THREADS_PER_CPU;
			executor = new ThreadPoolExecutor(nThreads, nThreads,
				0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
			completionService =
				new ExecutorCompletionService<IOService>(executor);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Server I/O error, can't continue my work.", e);
			stopping = true;
		} // end of try-catch
    new ResultsListener().start();
	}

	public static SocketReadThread getInstance() {
		if (socketReadThread == null) {
			socketReadThread = new SocketReadThread();
			Thread thrd = new Thread(socketReadThread);
			thrd.setName("SocketReadThread");
			thrd.start();
			log.fine("SocketReadThread started.");
		} // end of if (acceptThread == null)
		return socketReadThread;
	}

	public void setMaxThreadPerCPU(int threads) {
		int cpus = Runtime.getRuntime().availableProcessors();
		setMaxThread(threads * cpus);
	}

	public void setMaxThread(int threads) {
		executor.setCorePoolSize(threads);
		executor.setMaximumPoolSize(threads);
	}

	public void addSocketService(IOService s) {
    waiting.offer(s);
    clientsSel.wakeup();
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
    while ((s = waiting.poll()) != null) {
      final SocketChannel sc = s.getSocketChannel();
      try {
				int sel_key = READ_ONLY;
				log.finest("ADDED OP_READ: " + s.getUniqueId());
				if (s.waitingToSend()) {
					sel_key = READ_WRITE;
					log.finest("ADDED OP_WRITE: " + s.getUniqueId());
				}
        sc.register(clientsSel, sel_key, s);
			} catch (Exception e) {
        // Ignore such channel
				log.finest("ERROR adding channel for: " + s.getUniqueId()
					+ ", exception: " + e);
      } // end of try-catch
    } // end of for ()

  }

	// Implementation of java.lang.Runnable

	/**
	 * Describe <code>run</code> method here.
	 *
	 */
	public void run() {
    while (!stopping) {
      try {
				clientsSel.select();
//         Set<SelectionKey> selected_keys = clientsSel.selectedKeys();
//         for (SelectionKey sk : selected_keys) {
        for (Iterator i = clientsSel.selectedKeys().iterator(); i.hasNext();) {
					SelectionKey sk = (SelectionKey)i.next();
					i.remove();
          // According to most guides we should use below code
          // removing SelectionKey from iterator, however a little later
          // we do cancel() on this key so removing is somehow redundant
          // and causes concurrency exception if a few calls are performed
          // at the same time.
          //selected_keys.remove(sk);
          IOService s = (IOService)sk.attachment();
					sk.cancel();
					log.finest("AWAKEN: " + s.getUniqueId());
          completionService.submit(s);
        }
				// Clean-up cancelled keys...
        clientsSel.selectNow();
        addAllWaiting();
			} catch (CancelledKeyException brokene) {
				// According to Java API that should not happen.
				// I think it happens only on the broken Java implementation
				// from Apple.
				log.log(Level.WARNING, "Ups, broken JDK, Apple?", brokene);
      } catch (Exception e) {
        log.log(Level.SEVERE, "Server I/O error.", e);
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

    public void run() {

      for (;;) {
        try {
          final IOService service = completionService.take().get();
          if (service.isConnected()
						//&& !service.getSocketChannel().isRegistered()
							) {
						log.finest("COMPLETED: " + service.getUniqueId());
            addSocketService(service);
          } else {
						log.finest("REMOVED: " + service.getUniqueId());
					} // end of else
        }
        catch (ExecutionException e) {
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

} // SocketReadThread
