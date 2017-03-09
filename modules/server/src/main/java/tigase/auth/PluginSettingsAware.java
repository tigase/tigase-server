package tigase.auth;

import javax.security.auth.callback.CallbackHandler;
import java.util.Map;

/**
 * Interface should be implemented by {@linkplain CallbackHandler} instance if
 * plugin settings should be injected.
 * @author Daniele Ricci
 */
public interface PluginSettingsAware {

    public void setPluginSettings(Map<String, Object> settings);

}
