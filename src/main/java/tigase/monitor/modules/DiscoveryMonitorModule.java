package tigase.monitor.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import tigase.component.exceptions.ComponentException;
import tigase.component.exceptions.RepositoryException;
import tigase.component.modules.impl.DiscoveryModule;
import tigase.monitor.AdHocTask;
import tigase.monitor.ConfigurableTask;
import tigase.monitor.InfoTask;
import tigase.monitor.MonitorContext;
import tigase.monitor.MonitorTask;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.JID;

public class DiscoveryMonitorModule extends DiscoveryModule<MonitorContext> {

	private boolean isAdHocCompatible(Object taskInstance) {
		return taskInstance != null
				&& (taskInstance instanceof AdHocTask || taskInstance instanceof InfoTask || taskInstance instanceof ConfigurableTask);
	}

	@Override
	protected void processAdHocCommandItems(Packet packet, JID jid, String node, JID senderJID) throws ComponentException,
			RepositoryException {
		if (jid.getResource() != null && isAdHocCompatible(context.getKernel().getInstance(jid.getResource()))) {
			final Object taskInstance = context.getKernel().getInstance(jid.getResource());

			List<Element> items = new ArrayList<Element>();
			Element resultQuery = new Element("query", new String[] { Packet.XMLNS_ATT }, new String[] { DISCO_ITEMS_XMLNS });
			Packet result = packet.okResult(resultQuery, 0);

			if (taskInstance instanceof InfoTask) {
				items.add(new Element("item", new String[] { "jid", "node", "name" }, new String[] { jid.toString(),
						InfoTaskCommand.NODE, "Task Info" }));
			}
			if (taskInstance instanceof ConfigurableTask) {
				items.add(new Element("item", new String[] { "jid", "node", "name" }, new String[] { jid.toString(),
						ConfigureTaskCommand.NODE, "Task config" }));
			}

			if (taskInstance instanceof AdHocTask) {
				items.addAll(((AdHocTask) taskInstance).getAdHocCommands(jid, senderJID));
			}

			resultQuery.addChildren(items);
			write(result);

		} else if (jid.getResource() != null) {
			throw new ComponentException(Authorization.ITEM_NOT_FOUND);
		} else
			super.processAdHocCommandItems(packet, jid, node, senderJID);
	}

	@Override
	protected void processDiscoInfo(Packet packet, JID jid, String node, JID senderJID) throws ComponentException,
			RepositoryException {
		if (jid.getResource() == null) {
			super.processDiscoInfo(packet, jid, node, senderJID);
		} else if (jid.getResource() != null && context.getKernel().getInstance(jid.getResource()) != null) {
			final Object taskInstance = context.getKernel().getInstance(jid.getResource());

			Element resultQuery = new Element("query", new String[] { "xmlns" }, new String[] { DISCO_INFO_XMLNS });
			Packet resultIq = packet.okResult(resultQuery, 0);

			resultQuery.addChild(new Element("identity", new String[] { "category", "type", "name" }, new String[] {
					"automation", "task", "Task " + jid.getResource() }));

			if (isAdHocCompatible(taskInstance)) {
				resultQuery.addChild(new Element("feature", new String[] { "var" }, new String[] { Command.XMLNS }));
			}

			write(resultIq);
		} else
			throw new ComponentException(Authorization.ITEM_NOT_FOUND);

	}

	@Override
	protected void processDiscoItems(Packet packet, JID jid, String node, JID senderJID) throws ComponentException,
			RepositoryException {
		if (node == null && jid.getResource() == null) {
			Element resultQuery = new Element("query", new String[] { Packet.XMLNS_ATT }, new String[] { DISCO_ITEMS_XMLNS });

			Collection<String> taskNames = context.getKernel().getNamesOf(MonitorTask.class);
			for (String taskName : taskNames) {
				resultQuery.addChild(new Element("item", new String[] { "jid", "name" }, new String[] {
						jid.toString() + "/" + taskName, "Task " + taskName }));
			}

			write(packet.okResult(resultQuery, 0));
		} else {
			Element resultQuery = new Element("query", new String[] { Packet.XMLNS_ATT }, new String[] { DISCO_ITEMS_XMLNS });
			write(packet.okResult(resultQuery, 0));
		}
	}
}
