/*  Tigase Project
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
package tigase.server.bosh;

import java.io.IOException;
import tigase.xmpp.XMPPIOService;

/**
 * Describe class BoshIOService here.
 *
 *
 * Created: Tue Jun  5 22:33:18 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class BoshIOService extends XMPPIOService {

	private static final String EOL = "\r\n";

	private String content_type = "text/xml; charset=utf-8";

	public void writeRawData(String data) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("HTTP/1.1 200 OK" + EOL);
		sb.append("Content-Type: " + content_type + EOL);
		sb.append("Content-Length: " + data.length() + EOL);
		sb.append(EOL);
		sb.append(data);
		super.writeRawData(sb.toString());
	}

}
