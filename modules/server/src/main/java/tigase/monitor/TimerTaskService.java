package tigase.monitor;

public interface TimerTaskService {

	void addTimerTask(tigase.util.TimerTask task, long delay);

	void addTimerTask(tigase.util.TimerTask task, long initialDelay, long period);
}
