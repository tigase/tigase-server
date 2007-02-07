/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
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
package tigase.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import tigase.io.SampleSocketThread.SocketHandler;

/**
 * This is sample class demonstrating how to use <code>tigase.io</code> library
 * for TLS/SSL client connection. This is simple telnet client class which
 * can connect to remote server using plain connection or SSL.
 *
 *
 * Created: Sun Aug  6 15:14:49 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class TelnetClient implements SampleSocketThread.SocketHandler {

  private static final Logger log =	Logger.getLogger("tigase.io.TelnetClient");
  private static final Charset coder = Charset.forName("UTF-8");

	private static int port = 7777;
	private static String hostname = "localhost";
	private static boolean debug = false;
	private static String file = null;
	private static boolean continuous = false;
	private static long delay = 100;
	private static boolean ssl = false;
	private static String sslId = "TelnetClient";

	private SampleSocketThread reader = null;
	private IOInterface iosock = null;

	/**
	 * Creates a new <code>TelnetClient</code> instance.
	 *
	 */
	public TelnetClient(String hostname, int port) throws Exception {
		reader = new SampleSocketThread(this);
		reader.start();
		SocketChannel sc =
			SocketChannel.open(new InetSocketAddress(hostname, port));
		// Basic channel configuration
		iosock = new SocketIO(sc);
		if (ssl) {
			iosock = new TLSIO(iosock,
				new TLSWrapper(TLSUtil.getSSLContext(sslId, "SSL", null), null, true));
		} // end of if (ssl)
		reader.addIOInterface(iosock);
		log.finer("Registered new client socket: " + sc);
	}

	public void writeData(String data) throws IOException {
    ByteBuffer dataBuffer = null;
    if (data != null || data.length() > 0) {
      dataBuffer = coder.encode(CharBuffer.wrap(data));
      iosock.write(dataBuffer);
    } // end of if (data == null || data.equals("")) else
	}

	public void handleSocketAccept(SocketChannel sc) {
		// Empty, not needed any implementation for that
	}

	public void handleIOInterface(IOInterface ioifc) throws IOException {
		ByteBuffer socketInput =
			ByteBuffer.allocate(ioifc.getSocketChannel().socket().getReceiveBufferSize());
		ByteBuffer tmpBuffer = ioifc.read(socketInput);
		if (ioifc.bytesRead() > 0) {
			tmpBuffer.flip();
			CharBuffer cb = coder.decode(tmpBuffer);
			tmpBuffer.clear();
			if (cb != null) {
				System.out.print(new String(cb.array()));
			} // end of if (cb != null)
		} // end of if (socketIO.bytesRead() > 0)
		reader.addIOInterface(ioifc);
	}

	/**
	 * Describe <code>main</code> method here.
	 *
	 * @param args a <code>String[]</code> value
	 */
	public static void main(final String[] args) throws Exception {
		parseParams(args);
		if (debug) {
			turnDebugOn();
		} // end of if (debug)
		if (ssl) {
			TLSUtil.configureSSLContext(sslId, "certs/keystore", "keystore", null);
		} // end of if (ssl)
		TelnetClient client = new TelnetClient(hostname, port);
		InputStreamReader str_reader = new InputStreamReader(System.in);
		if (file != null) {
			FileReader fr = new FileReader(file);
			char[] file_buff = new char[64*1024];
			int res = -1;
			while ((res = fr.read(file_buff)) != -1) {
				client.writeData(new String(file_buff, 0, res));
			} // end of while ((res = fr.read(buff)) != -1)
			fr.close();
		} // end of if (file != null)
		char[] buff = new char[1024];
		for (;;) {
			int res = str_reader.read(buff);
			client.writeData(new String(buff, 0, res));
		} // end of for (;;)
	}

	public static String help() {
    return "\n"
      + "Parameters:\n"
      + " -?                this help message\n"
			+ " -h hostname       host name\n"
      + " -p port           port number\n"
			+ " -ssl              turn SSL on for all connections\n"
			+ " -f file           file with content to send to remote host\n"
			+ " -c                continuous sending file content\n"
			+ " -t millis         delay between sending file content\n"
      + " -v                prints server version info\n"
      + " -d [true|false]   turn on|off debug mode\n"
      ;
  }

  public static String version() {
    return "\n"
      + "-- \n"
      + "Tigase XMPP Telnet, version: "
      + TelnetClient.class.getPackage().getImplementationVersion() + "\n"
      + "Author:	Artur Hefczyc <artur.hefczyc@tigase.org>\n"
      + "-- \n"
      ;
  }

  public static void parseParams(final String[] args) throws Exception {
    if (args != null && args.length > 0) {
      for (int i = 0; i < args.length; i++) {
        if (args[i].equals("-?")) {
          System.out.print(help());
          System.exit(0);
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-v")) {
          System.out.print(version());
          System.exit(0);
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-f")) {
          if (i+1 == args.length) {
            System.out.print(help());
            System.exit(1);
          } // end of if (i+1 == args.length)
          else {
            file = args[++i];
          } // end of else
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-h")) {
          if (i+1 == args.length) {
            System.out.print(help());
            System.exit(1);
          } // end of if (i+1 == args.length)
          else {
            hostname = args[++i];
          } // end of else
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-p")) {
          if (i+1 == args.length) {
            System.out.print(help());
            System.exit(1);
          } // end of if (i+1 == args.length)
          else {
            port = Integer.decode(args[++i]);
          } // end of else
        } // end of if (args[i].equals("-h"))
        if (args[i].equals("-d")) {
          if (i+1 == args.length || args[i+1].startsWith("-")) {
            debug = true;
          } // end of if (i+1 == args.length)
          else {
            ++i;
            debug = args[i].charAt(0) != '-' &&
              (args[i].equals("true") || args[i].equals("yes"));
          } // end of else
        } // end of if (args[i].equals("-d"))
        if (args[i].equals("-c")) {
          if (i+1 == args.length || args[i+1].startsWith("-")) {
            continuous = true;
          } // end of if (i+1 == args.length)
          else {
            ++i;
            continuous = args[i].charAt(0) != '-' &&
              (args[i].equals("true") || args[i].equals("yes"));
          } // end of else
        } // end of if (args[i].equals("-c"))
        if (args[i].equals("-ssl")) {
          if (i+1 == args.length || args[i+1].startsWith("-")) {
            ssl = true;
          } // end of if (i+1 == args.length)
          else {
            ++i;
            ssl = args[i].charAt(0) != '-' &&
              (args[i].equals("true") || args[i].equals("yes"));
          } // end of else
        } // end of if (args[i].equals("-ssl"))
      } // end of for (int i = 0; i < args.length; i++)
    }
  }

	public static void turnDebugOn() {
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(".level", "ALL");
		properties.put("handlers", "java.util.logging.ConsoleHandler");
		properties.put("java.util.logging.ConsoleHandler.formatter",
			"tigase.util.LogFormatter");
		properties.put("java.util.logging.ConsoleHandler.level", "ALL");
		Set<Map.Entry<String, String>> entries = properties.entrySet();
		StringBuilder buff = new StringBuilder();
		for (Map.Entry<String, String> entry : entries) {
			buff.append(entry.getKey() + "=" +	entry.getValue() + "\n");
		}
    try {
      final ByteArrayInputStream bis =
        new ByteArrayInputStream(buff.toString().getBytes());
      LogManager.getLogManager().readConfiguration(bis);
      bis.close();
    } catch (IOException e) {
      log.log(Level.SEVERE, "Can not configure logManager", e);
    } // end of try-catch
	}

} // TelnetClient
