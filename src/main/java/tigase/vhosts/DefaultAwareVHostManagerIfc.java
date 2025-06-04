package tigase.vhosts;

import tigase.annotations.TigaseDeprecated;
import tigase.xmpp.jid.JID;

import java.util.List;

@TigaseDeprecated(since = "8.5.0", removeIn = "9.0.0", note = "Temporary interface to avoid breaking main API")
@Deprecated
public interface DefaultAwareVHostManagerIfc extends VHostManagerIfc {

    List<JID> getAllVHosts(boolean includeDefaultVhost);

}
