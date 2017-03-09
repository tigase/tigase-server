/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Tigase, Inc." <office@tigase.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License,
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
 */
package tigase.server.bosh;

import tigase.server.bosh.BoshConnectionManager.BOSH_OPERATION_TYPE;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

/**
 *
 * @author Wojciech Kapcia
 */

public class BoshSidLoggerFilter implements Filter {

	@Override
	public boolean isLoggable( LogRecord record ) {
		boolean matchTracker = false;

		Object[] parameters = record.getParameters();
		if ( parameters != null && parameters.length > 0 && parameters[0] != null
				 && BOSH_OPERATION_TYPE.forName( parameters[0].toString()) != null ){
			matchTracker = true;
		}
		return matchTracker;
	}
}
