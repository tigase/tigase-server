package tigase.monitor;

import tigase.component.Context;
import tigase.kernel.Kernel;

public interface MonitorContext extends Context {

	Kernel getKernel();

}
