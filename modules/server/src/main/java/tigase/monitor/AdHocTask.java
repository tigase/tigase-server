package tigase.monitor;

import java.util.Collection;

import tigase.xml.Element;
import tigase.xmpp.JID;

public interface AdHocTask extends MonitorTask {

	Collection<Element> getAdHocCommands(JID toJID, JID senderJID);

}
