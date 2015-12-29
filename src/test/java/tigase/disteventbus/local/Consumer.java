package tigase.disteventbus.local;

/**
 * Created by bmalkow on 30.12.2015.
 */
public class Consumer {

	private int counter01 = 0;

	private int counter02 = 0;

	public int getCounter01() {
		return counter01;
	}

	public int getCounter02() {
		return counter02;
	}

	@HandleEvent
	public void onCatchSomeNiceEvent(Event02 event) {
		++counter02;
	}

	@HandleEvent
	public void onEvent01(Event01 event) {
		++counter01;
	}

}
