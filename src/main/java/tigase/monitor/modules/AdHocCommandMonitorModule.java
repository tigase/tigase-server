package tigase.monitor.modules;

import tigase.component.adhoc.AdHocCommand;
import tigase.component.adhoc.AdHocCommandException;
import tigase.component.adhoc.AdHocCommandManager;
import tigase.component.exceptions.ComponentException;
import tigase.component.modules.impl.AdHocCommandModule;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.core.Kernel;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.JID;

@Bean(name = AdHocCommandModule.ID, active = true)
public class AdHocCommandMonitorModule extends AdHocCommandModule implements Initializable {

	private ConfigureTaskCommand configCommand;

	private final AdHocCommandManager customCommandsManager = new AdHocCommandManager();

	private InfoTaskCommand infoCommand;

	@Inject
	private Kernel kernel;

	public AdHocCommandMonitorModule() {
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
	public void initialize() {
		this.infoCommand = new InfoTaskCommand(kernel);
		this.configCommand = new ConfigureTaskCommand(kernel);

		super.initialize();
	}

	@Override
	public void process(Packet packet) throws ComponentException {
		final JID jid = packet.getStanzaTo();

		final Object taskInstance = jid.getResource() != null ? kernel.getInstance(jid.getResource()) : null;

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
