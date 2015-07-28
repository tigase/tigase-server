/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.conf.dsl;

import java.io.File;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import tigase.kernel.core.DependencyGrapher;
import tigase.kernel.core.Kernel;

/**
 *
 * @author andrzej
 */
public class ConfiguratorTest {
	
	private Kernel kernel;
	private Configurator configurator;
	
	public ConfiguratorTest() {
		//Logger logger = Logger.getLogger("tigase.kernel.Kernel");

		// create a ConsoleHandler
		Handler handler = new ConsoleHandler();
		handler.setLevel(Level.ALL);
		//logger.addHandler(handler);
		//logger.setLevel(Level.ALL);
		
		Logger logger = Logger.getLogger("tigase.conf.dsl");
		logger.addHandler(handler);
		logger.setLevel(Level.ALL);
		logger = Logger.getLogger("tigase.conf.dsl.Configurator");
		logger.setLevel(Level.ALL);

		if (logger.isLoggable(Level.CONFIG))
			logger.config("Logger successfully initialized");
	}
	
	@Before
	public void setUp() throws Exception {
		kernel = new Kernel("root");
		configurator = new Configurator(kernel);
	}

	@After
	public void tearDownAfterClass() throws Exception {
		configurator = null;
	}	
	
	@Test
	public void test() {
		configurator.loadConfig(new File("src/test/java/tigase/conf/dsl/ConfigTest.groovy"));
		configurator.configure();
		DependencyGrapher dg = new DependencyGrapher(kernel);
		System.out.println(dg.getDependencyGraph());
		kernel.getInstance("userRepo2");
		kernel.startSubKernels();
		kernel.getInstance("c2s");
	}
}
