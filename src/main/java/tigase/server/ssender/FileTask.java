/*  Package Jabber Server
 *  Copyright (C) 2001-2007
 *  "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.server.ssender;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>FileTask</code> implements tasks for cyclic retrieving stanzas from
 * a directory and sending them to the StanzaHandler object.
 * </p>
 * It looks for any new stanza to send. Any single file can contain only single
 * stanza to send and any entry in database table can also contain only single
 * stanza to send. File on hard disk and record in database is deleted after
 * it is read.
 * <p>
 * Any file in given directory is treated the same way - Tigase assumes it
 * contains valid XML data with XMPP stanza to send. You can however set in
 * configuration, using wildchars which files contain stanzas.
 * All stanzas must contain complete data including correct <em>"from"</em>
 * and <em>"to"</em> attributes.
 * </p>
 * <p>
 * By default it looks for <code>*.stanza</code> files in
 * <code>/var/spool/jabber/</code> folder but you can specify different
 * directory name in initialization string. Sample initialization strings:
 * </p>
 * <pre>/var/spool/jabber/*.stanza</pre>
 * <pre>/var/spool/jabber/*</pre>
 * <p>
 * The last is equal to:
 * </p>
 * <pre>/var/spool/jabber/</pre>
 * <p>
 * Note the last forward slash '/' is required in such case if the last element
 * of the path is a directory.
 * </p>
 * <p>
 * <strong>Please note! Tigase must have writing permissions for this directory,
 * otherwise it may not function properly.</strong>
 * </p>
 * <p>
 * Created: Fri Apr 20 12:10:55 2007
 * </p>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class FileTask extends SenderTask {

	/**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.server.ssender.FileTask");

	/**
	 * <code>handler</code> is a reference to object processing stanza
	 * read from database.
	 */
	private StanzaHandler handler = null;
	/**
	 * <code>db_conn</code> field stores database connection string.
	 */
	private String init_str = null;
	/**
	 * Variable <code>file_mask</code> keeps wildchar mask of the files which
	 * are suposed to store XMPP stanzas to send in given directory.
	 */
	private String file_mask = null;
	/**
	 * Variable <code>directory</code> keeps directory path where files
	 * with XMPP stanzas are stored for sending.
	 */
	private String directory = null;

	/**
	 * <code>init</code> method is a task specific initialization rountine.
	 *
	 * @param handler a <code>StanzaHandler</code> value is a reference to object
	 * which handles all stanza retrieved from data source. The handler is
	 * responsible for delivering stanza to destination address.
	 * @param initString a <code>String</code> value is an initialization string
	 * for this task. For example database tasks would expect database connection
	 * string here, filesystem task would expect directory here.
	 * @exception IOException if an error occurs during task or data storage
	 * initialization.
	 */
	public void init(StanzaHandler handler, String initString) throws IOException {
		this.handler = handler;
		init_str = initString;
		if (init_str.endsWith(File.separator)) {
			file_mask = "";
			directory = init_str;
		} else {
			int idx = init_str.lastIndexOf(File.separator);
			directory = init_str.substring(0, idx);
			file_mask = init_str.substring(idx+1, init_str.length());
		}
		log.config("file_mask='" + file_mask + "', directory='" + directory + "'");
	}

	/**
	 * <code>getInitString</code> method returns initialization string passed
	 * to it in <code>init()</code> method.
	 *
	 * @return a <code>String</code> value of initialization string.
	 */
	public String getInitString() {
		return init_str;
	}

	private String readFile(File ffile) throws IOException {
		StringBuilder result = new StringBuilder();
		char[] buff = new char[16*1024];
		FileReader fr = new FileReader(ffile);
		int res = fr.read(buff);
		while (res > 0) {
			result.append(buff, 0, res);
			res = fr.read(buff);
		}
		fr.close();
		return result.toString();
	}

	/**
	 * <code>run</code> method is where all task work is done.
	 */
	public void run() {
		try {
			File fdir = new File(directory);
			String[] files = fdir.list(new MaskFilter(file_mask));
			if (files != null) {
				for (String file: files) {
					log.finest("Processing file: " + file);
					File ffile = new File(fdir, file);
					String stanza = readFile(ffile);
					handler.handleStanza(stanza);
					ffile.delete();
				}
			}
		} catch (IOException e) {
			// Let's ignore it for now.
			log.log(Level.WARNING, "Error retrieving stanzas from database: ", e);
			// It should probably do kind of auto-stop???
			// if so just uncomment below line:
			//this.cancel();
		}
	}

	private static class MaskFilter implements FilenameFilter {

		private String mask = null;

		private MaskFilter(String mask) {
			if (mask.startsWith("*")) {
				this.mask = mask.substring(1);
			} else {
				this.mask = mask;
			}
		}

		public boolean accept(final File file, final String name) {
			return name.endsWith(mask);
		}

	}

}
