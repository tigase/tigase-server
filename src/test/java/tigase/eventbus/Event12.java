package tigase.eventbus;

import java.io.Serializable;

public class Event12 extends Event1 implements Serializable {

	public Runnable r;

	public Runnable getR() {
		return r;
	}

	public void setR(Runnable r) {
		this.r = r;
	}
}
