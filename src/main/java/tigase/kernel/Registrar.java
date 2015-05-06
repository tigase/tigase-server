package tigase.kernel;

import tigase.kernel.core.Kernel;

public interface Registrar {

	void register(Kernel kernel);

	void start(Kernel krnl);

}
