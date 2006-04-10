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
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
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

  private void addAllWaiting() throws IOException {

    IOService s = null;
    while ((s = waiting.poll()) != null) {
      final SocketChannel sc = s.getSocketChannel();
      try {
        sc.register(clientsSel, SelectionKey.OP_READ, s);
				log.finest("ADDED: " + s.getUniqueId());
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
      } catch (Exception e) {
        log.log(Level.SEVERE, "Server I/O error, can't continue my work.", e);
        stopping = true;
      }
    }

    System.err.println("ClientThread stopped!");
    System.exit(2);
	}

  @TODO(note="ExecutionException is poorly implemented.")
  protected class ResultsListener extends Thread {

    public ResultsListener() { }

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
