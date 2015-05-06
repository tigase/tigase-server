package tigase.kernel;

import tigase.kernel.beans.Inject;

public class Bean7 implements Special {

	@Inject(bean = "beanX")
	private Object obj;

	public Object getObj() {
		return obj;
	}

	public void setObj(Object obj) {
		this.obj = obj;
	}

}
