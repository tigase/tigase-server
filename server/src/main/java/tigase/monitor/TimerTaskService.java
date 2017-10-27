package tigase.monitor;

import tigase.util.common.TimerTask;

public interface TimerTaskService {

	void addTimerTask(TimerTask task, long delay);

	void addTimerTask(TimerTask task, long initialDelay, long period);
}
