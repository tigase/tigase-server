package tigase.component;

import tigase.component.modules.impl.AdHocCommandModule.ScriptCommandProcessor;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.BasicComponent;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.JID;

import java.util.List;
import java.util.Queue;

@Bean(name = "scriptCommandProcessor", active = true)
public class ComponenScriptCommandProcessor implements ScriptCommandProcessor {

	@Inject(bean = "service", nullAllowed = false)
	private BasicComponent component;

	@Override
	public List<Element> getScriptItems(String node, JID jid, JID from) {
		return component.getScriptItems(node, jid, from);
	}

	@Override
	public boolean processScriptCommand(Packet pc, Queue<Packet> results) {
		return component.processScriptCommand(pc, results);
	}

}
