package tigase.disteventbus.local;

/**
 * Created by bmalkow on 30.12.2015.
 */
public class NewConsumer extends Consumer {

	private int counter01_1 = 0;

	private int counter03 = 0;

	public int getCounter01_1() {
		return counter01_1;
	}

	public int getCounter03() {
		return counter03;
	}

	@HandleEvent
	public void onEvent01(Event01 event) {
		++counter01_1;
	}

	@HandleEvent
	public void onEvent03(Event01 event) {
		++counter03;
	}
}
