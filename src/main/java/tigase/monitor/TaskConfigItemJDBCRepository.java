package tigase.monitor;

import java.util.Map;

import tigase.db.DBInitException;
import tigase.db.comp.UserRepoRepository;
import tigase.xmpp.BareJID;

public class TaskConfigItemJDBCRepository extends UserRepoRepository<TaskConfigItem> {

	private final static String CONFIG_KEY = "monitor-tasks";

	private final static BareJID REPO_USER_JID = BareJID.bareJIDInstanceNS("tigase-monitor");

	@Override
	public void destroy() {
		// Nothing to do
	}

	@Override
	public String getConfigKey() {
		return CONFIG_KEY;
	}

	@Override
	public String[] getDefaultPropetyItems() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TaskConfigItem getItemInstance() {
		return new TaskConfigItem();
	}

	@Override
	public String getPropertyKey() {
		return "--monitor-tasks";
	}

	@Override
	public BareJID getRepoUser() {
		return REPO_USER_JID;
	}

	@Override
	public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {
		// Nothing to do
	}

}
