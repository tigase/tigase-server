package tigase.kernel;

import java.util.Set;

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;

@Bean(name = "bean1")
public class Bean1 {

	@Inject(nullAllowed = true)
	private Bean2 bean2;

	@Inject(nullAllowed = true)
	private Bean3 bean3;

	@Inject(type = Special.class, nullAllowed = true)
	private Set<Special> collectionOfSpecial;

	@Inject(nullAllowed = true)
	private Special[] tableOfSpecial;

	public Bean2 getBean2() {
		return bean2;
	}

	public Bean3 getBean3() {
		return bean3;
	}

	public Set<Special> getCollectionOfSpecial() {
		return collectionOfSpecial;
	}

	public Special[] getTableOfSpecial() {
		return tableOfSpecial;
	}

	public void setBean2(Bean2 bean2) {
		this.bean2 = bean2;
	}

	public void setBean3(Bean3 bean3) {
		this.bean3 = bean3;
	}

	public void setCollectionOfSpecial(Set<Special> xxx) {
		this.collectionOfSpecial = xxx;
	}

	public void setTableOfSpecial(Special[] ss) {
		this.tableOfSpecial = ss;
	}

}
