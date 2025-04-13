package tigase.db.services;

import org.jspecify.annotations.NonNull;
import tigase.db.AuthRepository;
import tigase.db.TigaseDBException;
import tigase.db.UserRepository;
import tigase.eventbus.EventBus;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.server.xmppsession.DisconnectUserEBAction;
import tigase.xmpp.StreamError;
import tigase.xmpp.jid.BareJID;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "account-expiration-service", active = false, parent = Kernel.class, exportable = true)
public class AccountExpirationService
	implements Initializable {

	public static final String ACCOUNT_EXPIRATION_DATE = "account-expiration-date";
	private static final Logger log = Logger.getLogger(AccountExpirationService.class.getCanonicalName());
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	@Inject
	private AuthRepository authRepository;
	@Inject
	private EventBus eventBus;
	private RemoveExpiredAccountsTask expiredMessagesRemovalTask = null;
	private LocalTime scheduledRemovalTimeLT = LocalTime.of(0, 5);
	@ConfigField(desc = "Time at which removal of expired accounts should be executed", alias = "scheduled-expired-accounts-removal-time")
	private String scheduledRemovalTime = scheduledRemovalTimeLT.toString();
	@Inject
	private UserRepository userRepository;

	@Override
	public void initialize() {
		final LocalTime now = LocalTime.now();
		Duration initialDelay = now.isAfter(scheduledRemovalTimeLT) ? Duration.ofHours(24)
			.minus(Duration.between(scheduledRemovalTimeLT, now)) : Duration.between(now, scheduledRemovalTimeLT);

		log.log(Level.CONFIG,
		        "Scheduling removing expired users accounts at: " + scheduledRemovalTimeLT + ", next run in: " +
			        initialDelay + ", sec: " + initialDelay.getSeconds());
		scheduler.scheduleAtFixedRate(new RemoveExpiredAccountsTask(), initialDelay.getSeconds(),
		                              TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
	}

	void removeExpiredAccounts() throws TigaseDBException {
		log.log(Level.FINEST, "Running expired accounts removal task");
		final Map<BareJID, String> dataMap = userRepository.getDataMap(ACCOUNT_EXPIRATION_DATE);
		final LocalDate now = LocalDate.now();
		for (Map.Entry<BareJID, String> jidExpirationEntry : dataMap.entrySet()) {
			if (jidExpirationEntry.getValue() != null && LocalDate.parse(jidExpirationEntry.getValue()).isBefore(now)) {
				log.log(Level.FINE, "Removing expired account: " + jidExpirationEntry.getKey() + ", expiration date: " + jidExpirationEntry.getValue());
				authRepository.removeUser(jidExpirationEntry.getKey());
				eventBus.fire(
					new DisconnectUserEBAction(jidExpirationEntry.getKey(), StreamError.Reset, "Account was deleted"));
			}
		}
	}

	public void setScheduledRemovalTime(String scheduledRemovalTime) {
		this.scheduledRemovalTime = scheduledRemovalTime;
		this.scheduledRemovalTimeLT = LocalTime.parse(scheduledRemovalTime);
	}

	public void setUserExpiration(@NonNull BareJID userId, Integer expirationDays) throws TigaseDBException {
		Objects.requireNonNull(userId);
		log.log(Level.FINE, "Setting user expiration for " + userId + " to " + expirationDays);
		if (expirationDays != null) {

			if (expirationDays <= 0) {
				userRepository.removeData(userId, ACCOUNT_EXPIRATION_DATE);
			} else {
				var localDate = LocalDate.now().plusDays(expirationDays);
				userRepository.setData(userId, ACCOUNT_EXPIRATION_DATE, localDate.toString());
			}
		}
	}

	private class RemoveExpiredAccountsTask
		implements Runnable {

		@Override
		public void run() {
			try {
				removeExpiredAccounts();
			} catch (TigaseDBException e) {
				log.log(Level.SEVERE, "Removing expired accounts failed", e);
			}
		}
	}
}
