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

package tigase.util.tracer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.server.Packet;

/**
 * Created: Jun 30, 2009 5:18:10 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class TigaseTracer {

	private static final Logger log =
					Logger.getLogger(TigaseTracer.class.getName());

	public static final String TRACER_IPS_PROP_KEY = "--tracer-ips";
	public static final String TRACER_JIDS_PROP_KEY = "--tracer-jids";
	public static final String TRACER_CIDS_PROP_KEY = "--tracer-cids";
	
	private static final String DEF_DIR = "logs";
	private static final long DEF_MAX_FILE_SIZE = 10000000;
	private static final int DEF_FILES_COUNT = 5;
	private static final String DEF_FILE_NAME = "packet-tracing.log";

	private static TigaseTracer instance = null;
	private static ConcurrentSkipListSet<String> ips =
					new ConcurrentSkipListSet<String>();
	private static ConcurrentSkipListSet<String> jids =
					new ConcurrentSkipListSet<String>();
	private static ConcurrentSkipListSet<String> cids =
					new ConcurrentSkipListSet<String>();

	private ArrayBlockingQueue<String> waiting =
					new ArrayBlockingQueue<String>(10000, true);
	private Runnable worker = null;
	private File files[] = null;
	private long maxFileSize = DEF_MAX_FILE_SIZE;
	private int filesCount = DEF_FILES_COUNT;
	private String dir = DEF_DIR;
	private String fileName = DEF_FILE_NAME;

	public static TigaseTracer getTracer(String name) {
		if (instance == null) {
			try {
				instance = new TigaseTracer();
			} catch (Exception e) {
				log.log(Level.WARNING, "Error initializing Tigase tracer: ", e);
			}
		}
		return instance;
	}

	public static void addIP(String ip) {
		ips.add(ip);
	}

	public static void removeIP(String ip) {
		ips.remove(ip);
	}

	public static void addJid(String jid) {
		jids.add(jid);
	}

	public static void removeJid(String jid) {
		jids.remove(jid);
	}

	public static void addCid(String cid) {
		cids.add(cid);
	}

	public static void removeCid(String cid) {
		cids.remove(cid);
	}

	private TigaseTracer() throws IOException {
		init();
	}

	private void init() throws IOException {
		files = new File[filesCount];
		for (int i = 0; i < files.length; i++) {
			files[i] = new File(dir, fileName + "." + i);
		}
		worker = new TracingWorker(new FileWriter(files[0], true), files[0].length());
		Thread thr = new Thread(worker);
		thr.setName("tracing-worker");
		thr.setDaemon(true);
		thr.start();
	}

	public boolean trace(String ip, String to_jid, String from_jid, String cid,
					String id, String point, String msg, Packet packet) {
		if (ip != null && ips.contains(ip)) {
			return waiting.offer(format(ip, id, point, msg, packet));
		}
		if (to_jid != null && jids.contains(to_jid)) {
			return waiting.offer(format("TO: " + to_jid, id, point, msg, packet));
		}
		if (from_jid != null && jids.contains(from_jid)) {
			return waiting.offer(format("FROM: " + from_jid, id, point, msg, packet));
		}
		if (cid != null && cids.contains(cid)) {
			return waiting.offer(format(cid, id, point, msg, packet));
		}
		return false;
	}

	private String format(String filter, String id, String point, String msg, Packet packet) {
		StringBuilder sb = new StringBuilder();
		Calendar cal = Calendar.getInstance();
		sb.append(cal.get(Calendar.YEAR));
		sb.append("-");
		sb.append(cal.get(Calendar.MONTH)+1);
		sb.append("-");
		sb.append(cal.get(Calendar.DAY_OF_MONTH));
		sb.append(" ");
		sb.append(cal.get(Calendar.HOUR_OF_DAY));
		sb.append(":");
		sb.append(cal.get(Calendar.MINUTE));
		sb.append(":");
		sb.append(cal.get(Calendar.SECOND));
		sb.append(".");
		sb.append(cal.get(Calendar.MILLISECOND));
		sb.append("[");
		sb.append(filter);
		sb.append("] {");
		sb.append(id);
		sb.append('-');
		sb.append(point);
		sb.append("} ");
		if (msg != null) {
			sb.append(msg);
		}
		if (packet != null) {
			sb.append(packet);
		}
		sb.append('\n');
		return sb.toString();
	}
	
	public boolean tracegByIP(String ip, String point, String msg, Packet packet) {
		return trace(ip, null, null, null, null, point, msg, packet);
	}
	
	public boolean traceByToJid(String jid, String point, String msg, Packet packet) {
		return trace(null, jid, null, null, null, point, msg, packet);
	}
	
	public boolean traceByFromJid(String jid, String point, String msg, Packet packet) {
		return trace(null, null, jid, null, null, point, msg, packet);
	}
	public boolean traceByCid(String cid, String point, String msg, Packet packet) {
		return trace(null, null, null, cid, null, point, msg, packet);
	}

	private Writer rotateFiles(Writer writer) throws IOException {
		writer.close();
		for (int i = files.length-2; i >= 0; --i) {
			File file1 = files[i];
			File file2 = files[i+1];
			if (file1.exists()) {
				if (file2.exists()) {
					file2.delete();
				}
				file1.renameTo(file2);
			}
		}
		return new FileWriter(files[0], false);
	}

	private class TracingWorker implements Runnable {

		private boolean stopped = false;
		private Writer writer = null;
		private long size = 0;

    protected TracingWorker(Writer writer, long initSize) {
			this.writer = writer;
			this.size = initSize;
		}

		@Override
		public void run() {
			while (!stopped) {
				try {
					String entry = waiting.take();
					if (entry != null) {
						try {
							// Save the log entry
							writer.write(entry);
							writer.flush();
							size += entry.length();
							if (size >= maxFileSize) {
								writer = rotateFiles(writer);
								size = 0;
							}
						} catch (IOException ex) {
							log.log(Level.WARNING, "Can not write to trace file: ", ex);
						}
					}
				} catch (InterruptedException ex) {	}
			}
		}

	}

}
