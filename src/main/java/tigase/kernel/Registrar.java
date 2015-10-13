package tigase.kernel;

import tigase.kernel.core.Kernel;

// TODO It needs to be designed correctly (stuff to register Kernel based sub-modules in current Kernel).
public interface Registrar {

	void register(Kernel kernel);

	void start(Kernel krnl);

}
