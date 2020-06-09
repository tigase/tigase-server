/*
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
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
 */
package tigase;

import tigase.util.log.LogFormatter;

import java.util.Arrays;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestLogger {

	public static void configureLogger(Logger log, Level level) {
		log.setUseParentHandlers(false);
		log.setLevel(level);
		final Handler[] handlers = log.getHandlers();
		if (Arrays.stream(handlers).noneMatch(ConsoleHandler.class::isInstance)) {
			ConsoleHandler ch = new ConsoleHandler();
			ch.setLevel(level);
			ch.setFormatter(new LogFormatter());
			log.addHandler(ch);
		}
		for (Handler logHandler : handlers) {
			logHandler.setLevel(level);
		}
	}

	public static Logger getLogger(Class clazz) {
		return Logger.getLogger("test." + clazz.getName());
	}
}
