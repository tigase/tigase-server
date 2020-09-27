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
package tigase.conf;

import tigase.kernel.core.Kernel;
import tigase.server.*;
import tigase.server.script.AbstractScriptCommand;
import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;

import javax.script.Bindings;
import java.util.Arrays;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ad-hoc command used to reconfigure logging of Tigase XMPP Server.
 */
public class SetLoggingCommand
		extends AbstractScriptCommand {

	private static final Logger log = Logger.getLogger(SetLoggingCommand.class.getCanonicalName());

	private static final Level[] LEVELS = {Level.OFF, Level.SEVERE, Level.WARNING, Level.INFO, Level.CONFIG, Level.FINE,
										   Level.FINER, Level.FINEST, Level.ALL};
	private final Kernel kernel;

	public SetLoggingCommand(Kernel kernel) {
		this.kernel = kernel;
		super.init("logging-set", "Set package logging", "Configuration");
	}

	@Override
	public Bindings getBindings() {
		return null;
	}

	@Override
	public void runCommand(Iq packet, Bindings binds, Queue<Packet> results) {
		try {
			try {
				LoggingBean loggingBean = kernel.getParent().getInstance(LoggingBean.class);
				String packageName = Command.getFieldValue(packet, "package-name");
				Level level = null;
				String levelStr = Command.getFieldValue(packet, "level");
				if (levelStr != null) {
					level = Level.parse(levelStr);
				}
				if (packageName == null || packageName.isBlank() || level == null) {
					Iq result = (Iq) packet.commandResult(Command.DataType.form);
					new Command.Builder(result).addAction(Command.Action.execute)
							.addDataForm(Command.DataType.form)
							.addTitle("Set packet logging")
							.withFields(builder -> {
								builder.addField(DataForm.FieldType.TextSingle, "package-name")
										.setLabel("Package name")
										.setRequired(true)
										.setValue(packageName)
										.build();
								builder.addField(DataForm.FieldType.ListSingle, "level")
										.setLabel("Level")
										.setRequired(true)
										.setOptions(Arrays.stream(LEVELS).map(Level::getName).toArray(String[]::new))
										.setValue(levelStr)
										.build();
							});
					results.offer(result);
				} else {
					loggingBean.setPackageLoggingLevel(packageName, level);
					results.offer(packet.commandResult(Command.DataType.result));
				}
			} catch (Throwable ex) {
				if (log.isLoggable(Level.WARNING)) {
					log.log(Level.WARNING,
										  "Execution of command " + getCommandId() + " failed! " + ex.getMessage(), ex);
				}
				results.offer(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet, "Execution of command " +
						getCommandId() + " failed: " + ex.getMessage(), false));
			}
		} catch (PacketErrorTypeException ex) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Packet already of type 'error'");
			}
		}
	}
}
