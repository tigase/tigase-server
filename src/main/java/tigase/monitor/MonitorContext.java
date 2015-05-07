package tigase.monitor;

import tigase.component.Context;
import tigase.kernel.core.Kernel;

public interface MonitorContext extends Context {

	Kernel getKernel();

}
