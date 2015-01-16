package tigase.monitor.modules;

import tigase.component.adhoc.AdHocCommand;
import tigase.component.adhoc.AdHocCommandException;
import tigase.component.adhoc.AdHocCommandManager;
import tigase.component.exceptions.ComponentException;
import tigase.component.modules.impl.AdHocCommandModule;
import tigase.monitor.MonitorContext;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.JID;

public class AdHocCommandMonitorModule extends AdHocCommandModule<MonitorContext> {

	private ConfigureTaskCommand configCommand;

	private final AdHocCommandManager customCommandsManager = new AdHocCommandManager();

	private InfoTaskCommand infoCommand;

	public AdHocCommandMonitorModule(ScriptCommandProcessor scriptProcessor) {
		super(scriptProcessor);
	}

	@Override
	public void afterRegistration() {
		super.afterRegistration();
		this.infoCommand = new InfoTaskCommand(context);
		this.configCommand = new ConfigureTaskCommand(context);
	}

	private AdHocCommand getCommand(final Object taskInstance, final String node) {
		if (node.equals(InfoTaskCommand.NODE)) {
			return infoCommand;
		} else if (node.equals(ConfigureTaskCommand.NODE)) {
			return configCommand;
		} else
			return null;
	}

	@Override
	public void process(Packet packet) throws ComponentException {
		final JID jid = packet.getStanzaTo();

		final Object taskInstance = jid.getResource() != null ? context.getKernel().getInstance(jid.getResource()) : null;

		if (jid.getResource() != null && taskInstance != null) {
			processCommand(packet, taskInstance);
		} else if (jid.getResource() != null) {
			throw new ComponentException(Authorization.ITEM_NOT_FOUND);
		} else if (jid.getResource() == null) {
			super.process(packet);
		} else {
			throw new ComponentException(Authorization.NOT_ACCEPTABLE);
		}
	}

	private void processCommand(Packet packet, Object taskInstance) throws ComponentException {
		final Element element = packet.getElement();
		final JID senderJid = packet.getStanzaFrom();
		final Element command = element.getChild(Command.COMMAND_EL, Command.XMLNS);
		final String node = command.getAttributeStaticStr("node");
		final String action = command.getAttributeStaticStr("action");
		final String sessionId = command.getAttributeStaticStr("sessionid");

		AdHocCommand adHocCommand = getCommand(taskInstance, node);

		try {
			write(customCommandsManager.process(packet, command, node, action, sessionId, adHocCommand));
		} catch (AdHocCommandException e) {
			throw new ComponentException(e.getErrorCondition(), e.getMessage());
		}
	}
}
