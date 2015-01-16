package tigase.kernel;

public class Bean3 implements Special{

	@Inject(bean = "bean4_1")
	private Bean4 bean41;

	@Inject(bean = "bean4")
	private Bean4 bean4;

	public Bean4 getBean4() {
		return bean4;
	}

	public void setBean4(Bean4 bean4) {
		this.bean4 = bean4;
	}

	public Bean4 getBean41() {
		return bean41;
	}

	public void setBean41(Bean4 bean41) {
		this.bean41 = bean41;
	}

}
