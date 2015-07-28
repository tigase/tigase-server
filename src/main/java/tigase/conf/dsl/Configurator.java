/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.conf.dsl;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import tigase.kernel.KernelException;
import tigase.kernel.Registrar;
import tigase.kernel.beans.config.AbstractBeanConfigurator;
import tigase.kernel.beans.config.BeanConfigurator;
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.Kernel;
import tigase.kernel.core.RegistrarKernel;
import tigase.osgi.ModulesManagerImpl;

/**
 *
 * @author andrzej
 */
public class Configurator {

	private class BeansBinder extends Binding {

		private final Kernel kernel;

		public BeansBinder(Kernel kernel, Binding binding) {
			super(binding.getVariables());
			this.kernel = kernel;
			this.kernel.registerBean(BeanConfigurator.DEFAULT_CONFIGURATOR_NAME).asClass(
					ConfigurationProvider.class).exportable().exec();
		}

		@Override
		public Object invokeMethod(String name, Object args_) {
			try {
				Object[] args = (Object[]) args_;
				System.out.println("executed method " + name + " with args " + Arrays.toString(args));
				String className = null;
				Map props = null;
				Closure closure = null;
				if (args.length > 0) {
					if (args[0] instanceof String) {
						className = (String) args[0];
					} else if (args[0] instanceof Map) {
						props = (Map) args[0];
						if (props.containsKey("class")) {
							className = (String) props.get("class");
						}
					}
					if (className == null) {
						if ("userRepository".equals(name)) {
							className = tigase.db.jdbc.JDBCRepository.class.getCanonicalName();
						} else {
							className = "we need to guess class here!";
						}
					}
					if (args[args.length - 1] instanceof Closure) {
						closure = (Closure) args[args.length - 1];
					}
				}
				Class<?> cls = ModulesManagerImpl.getInstance().forName(className);
				Object configurator = kernel.getInstance(BeanConfigurator.DEFAULT_CONFIGURATOR_NAME);
				if (configurator != null && configurator instanceof ConfigurationProvider) {
					((ConfigurationProvider) configurator).setBeanConfiguration(name, this, props, closure);
				}
				kernel.registerBean(name).asClass(cls).exportable().exec();
			} catch (ClassNotFoundException ex) {
				Logger.getLogger(Configurator.class.getName()).log(Level.SEVERE, null, ex);
			}
			return null;
		}
	}

	public static class ConfigurationProvider extends AbstractBeanConfigurator {

		private final HashMap<String, Binding> configBindings = new HashMap<String, Binding>();
		private final HashMap<String, Closure> configClosures = new HashMap<String, Closure>();
		private final HashMap<String, Map> configProps = new HashMap<String, Map>();

		@Override
		public void configure(BeanConfig beanConfig, Object bean) throws KernelException {
			final Binding parentBinding = configBindings.get(beanConfig.getBeanName());
			if (parentBinding == null) {
				super.configure(beanConfig, bean);
				return;
			}
			final Binding binding = new Binding(parentBinding.getVariables());
			Closure c = configClosures.get(beanConfig.getBeanName());
			Map props = configProps.get(beanConfig.getBeanName());
			if (c != null) {
				c.setDelegate(new Binding() {

					@Override
					public Object getProperty(String prop) {
						Object val = super.getProperty(prop);
						if (val == null)
							val = binding.getProperty(prop);
						return val;
					}

					@Override
					public Object getVariable(String name) {
						Object val = super.getVariable(name);
						if (val == null)
							val = binding.getVariable(name);
						return val;
					}

					@Override
					public Object invokeMethod(String name, Object args) {
						for (Method m : bean.getClass().getMethods()) {
							if (!m.getName().equals(name))
								continue;
							try {
								return m.invoke(bean, (Object[]) args);
							} catch (IllegalAccessException ex) {
								Logger.getLogger(Configurator.class.getName()).log(Level.SEVERE, null, ex);
							} catch (IllegalArgumentException ex) {
								Logger.getLogger(Configurator.class.getName()).log(Level.SEVERE, null, ex);
							} catch (InvocationTargetException ex) {
								Logger.getLogger(Configurator.class.getName()).log(Level.SEVERE, null, ex);
							}
						}
						return null;
					}

					@Override
					public void setProperty(String k, Object v) {
						super.setProperty(k, v);
						setVariable(k, v);
					}

					@Override
					public void setVariable(String k, Object v) {
						super.setVariable(k, v);
						System.out.println("setting " + k + " with " + v);
					}

				});
				c.setResolveStrategy(Closure.DELEGATE_FIRST);
				System.out.println("execution closure");
				Object x = c.call();
				System.out.println("got result = " + c);
			}
			configProps.putAll(((Binding) c.getDelegate()).getVariables());
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "configuring instance of bean {0} with {1}",
						new Object[] { beanConfig.getBeanName(), configProps });
			}
			super.configure(beanConfig, bean);
		}

		@Override
		protected Map<String, Object> getConfiguration(BeanConfig beanConfig) {
			return configProps.get(beanConfig.getBeanName());
		}

		public void setBeanConfiguration(String name, Binding binding, Map props, Closure c) {
			log.log(Level.FINEST, "setting bean {0} configuration = {1}, {2}, {3}", new Object[] { name, binding, props, c });
			configBindings.put(name, binding);
			if (props == null)
				props = new HashMap();
			configProps.put(name, props);
			configClosures.put(name, c);
		}

	}

	private class KernelBinder extends Binding {

		private final Kernel parentKernel;

		public KernelBinder(Kernel kernel, Binding binding) {
			super(binding.getVariables());
			this.parentKernel = kernel;
		}

		@Override
		public Object invokeMethod(final String name, final Object args_) {
			Registrar reg = new Registrar() {

				@Override
				public void register(Kernel kernel) {
					try {
						Object[] args = (Object[]) args_;
						System.out.println("executed method " + name + " with args " + Arrays.toString(args));
						String className = null;
						Map props = null;
						Closure closure = null;
						if (args.length > 0) {
							if (args[0] instanceof String) {
								className = (String) args[0];
							} else if (args[0] instanceof Map) {
								props = (Map) args[0];
								if (props.containsKey("class")) {
									className = (String) props.get("class");
								}
							}
							if (className == null) {
								if ("userRepository".equals(name)) {
									className = tigase.db.jdbc.JDBCRepository.class.getCanonicalName();
								} else {
									className = tigase.conf.dsl.Dummy.class.getCanonicalName();
								}
							}
							if (args[args.length - 1] instanceof Closure) {
								closure = (Closure) args[args.length - 1];
							}
						}
						Class<?> cls = ModulesManagerImpl.getInstance().forName(className);
						kernel.registerBean(name).asClass(cls).exportable().exec();

						Object configurator = kernel.getInstance(BeanConfigurator.DEFAULT_CONFIGURATOR_NAME);
						if (configurator != null && configurator instanceof ConfigurationProvider) {
							((ConfigurationProvider) configurator).setBeanConfiguration(name, KernelBinder.this, props,
									closure);
						}
					} catch (ClassNotFoundException ex) {
						Logger.getLogger(Configurator.class.getName()).log(Level.SEVERE, null, ex);
					}
				}

				@Override
				public void start(Kernel krnl) {
					krnl.ln(name, krnl.getParent(), name);
				}
			};
			RegistrarKernel k = new RegistrarKernel();
			k.setName(name);
			parentKernel.registerBean(name + "#KERNEL").asInstance(k).exec();
			reg.register(k);
			parentKernel.registerBean(name).asInstance(reg).exec();
			return null;
		}

		public Object invokeMethod2(String name, Object args_) {
			Kernel kernel = new RegistrarKernel();
			// kernel.registerBean(AbstractBeanConfigurator.DEFAULT_CONFIGURATOR_NAME).asClass(ConfigurationProvider.class).exportable().exec();
			try {
				Object[] args = (Object[]) args_;
				System.out.println("executed method " + name + " with args " + Arrays.toString(args));
				String className = null;
				Map props = null;
				Closure closure = null;
				if (args.length > 0) {
					if (args[0] instanceof String) {
						className = (String) args[0];
					} else if (args[0] instanceof Map) {
						props = (Map) args[0];
						if (props.containsKey("class")) {
							className = (String) props.get("class");
						}
					}
					if (className == null) {
						if ("userRepository".equals(name)) {
							className = tigase.db.jdbc.JDBCRepository.class.getCanonicalName();
						} else {
							className = tigase.conf.dsl.Dummy.class.getCanonicalName();
						}
					}
					if (args[args.length - 1] instanceof Closure) {
						closure = (Closure) args[args.length - 1];
					}
				}
				Class<?> cls = ModulesManagerImpl.getInstance().forName(className);
				parentKernel.registerBean(name + "#subkernel").asInstance(kernel).exec();
				kernel.registerBean(name).asClass(cls).exportable().exec();

				Object configurator = kernel.getInstance(BeanConfigurator.DEFAULT_CONFIGURATOR_NAME);
				if (configurator != null && configurator instanceof ConfigurationProvider) {
					((ConfigurationProvider) configurator).setBeanConfiguration(name, this, props, closure);
				}
			} catch (ClassNotFoundException ex) {
				Logger.getLogger(Configurator.class.getName()).log(Level.SEVERE, null, ex);
			}
			return null;
		}
	}

	private class RootKernelBinder extends Binding {

		private final Kernel kernel;

		public RootKernelBinder(Kernel kernel) {
			this.kernel = kernel;
		}

		@Override
		public Object getVariable(String prop) {
			final String key = prop;
			final Binding binding = this;
			if (!hasVariable(prop)) {
				if (BEANS_KEY.equals(prop)) {
					return newBeansClosure(kernel, this);
				} else {
					return newKernelClosure(kernel, prop, this);
				}
			}
			return super.getVariable(prop);
		}

		protected Object newBeansClosure(Kernel kernel, Binding parentBinding) {
			final BeansBinder binding = new BeansBinder(kernel, parentBinding);
			Closure c = new Closure(this) {
				@Override
				public Object call(Object[] args) {
					System.out.println("executed beans with args " + Arrays.toString(args));
					if (args == null || args.length == 0)
						return null;

					Closure c1 = (Closure) args[0];
					c1.setResolveStrategy(Closure.DELEGATE_FIRST);
					c1.setDelegate(binding);
					// parentBinding.setVariable(BEANS_KEY, args[0]);
					return c1.call();
				}
			};
			c.setResolveStrategy(Closure.DELEGATE_ONLY);
			c.setDelegate(binding);
			return c;
		}

		protected Closure newKernelClosure(Kernel kernel, String beanName, Binding parentBinding) {
			final KernelBinder binding = new KernelBinder(kernel, parentBinding);
			Closure c = new Closure(this) {
				@Override
				public Object call(Object[] args) {

					binding.invokeMethod(beanName, args);
					return null;

					// System.out.println("executed " + beanName + " with args "
					// + Arrays.toString(args));
					// if (args == null || args.length == 0)
					// return null;
					//
					// Closure c1 = (Closure) args[0];
					// c1.setResolveStrategy(Closure.DELEGATE_FIRST);
					// c1.setDelegate(binding);
					// //parentBinding.setVariable(BEANS_KEY, args[0]);
					// return c1.call();
				}
			};
			c.setResolveStrategy(Closure.DELEGATE_ONLY);
			c.setDelegate(binding);
			return c;
		}
	}

	private static final String BEANS_KEY = "beans";

	private static final Logger log = Logger.getLogger(Configurator.class.getCanonicalName());

	private Script configScript;

	private final Kernel rootKernel;

	public Configurator(Kernel kernel) {
		rootKernel = kernel;
	}

	public void configure() {
		log.config("starting configuration....");
		// final Closure beans = new Closure(this, this) {
		// public Object call(Object[] args) {
		// return null;
		// }
		// };
		// Binding binding = new Binding() {
		// @Override
		// public Object getVariable(String prop) {
		// final String key = prop;
		// final Binding binding = this;
		// if (!hasVariable(prop)) {
		// return new Closure(this) {
		// public Object call(Object[] args) {
		// System.out.println("executing closure for " + key + " with params = "
		// + Arrays.toString(args));
		// if (args.length == 1 && args[0] instanceof Closure) {
		// Closure c = (Closure) args[0];
		//// c.setDelegate(new KernelWrapper());
		// } else {
		//
		// }
		// binding.setVariable(key, args[0]);
		// return null;
		// }
		// };
		// }
		// return super.getVariable(prop);
		// }
		// };

		Binding binding = new RootKernelBinder(rootKernel);
		// binding.setVariable("beans", beans);

		configScript.setBinding(binding);
		configScript.run();
		log.info("configuration started");
		binding.getVariables().forEach((k, v) -> System.out.println("key = " + k + " value = " + v));
	}

	public Object invokeMethod(String string, Object o) {
		return null;
	}

	public void loadConfig(File file) {
		if (file.getName().endsWith(".groovy")) {
			loadDsl(file);
		}
	}

	private void loadDsl(File file) {
		// final Closure beans = new Closure(this, this) {
		// public Object call(Object[] args) {
		// return null;
		// }
		// };
		Binding binding = new Binding() {
		};
		// @Override
		// public Object getVariable(String prop) {
		// final String key = prop;
		// final Binding binding = this;
		// if (!hasVariable(prop)) {
		// return new Closure(this) {
		// public Object call(Object[] args) {
		// System.out.println("executing closure for " + key + " with params = "
		// + Arrays.toString(args));
		// if (args.length == 1 && args[0] instanceof Closure) {
		// Closure c = (Closure) args[0];
		// c.setDelegate(new KernelWrapper());
		// } else {
		//
		// }
		// binding.setVariable(key, args[0]);
		// return null;
		// }
		// };
		// }
		// return super.getVariable(prop);
		// }
		// };
		//
		// binding.setVariable("beans", beans);

		try {
			log.fine("loading configuration from file " + file.getAbsolutePath());
			GroovyShell shell = new GroovyShell(this.getClass().getClassLoader(), binding);
			configScript = shell.parse(file);
			// shell.evaluate(file);

			binding.getVariables().forEach((k, v) -> System.out.println("key = " + k + " value = " + v));
			log.info("loaded configuration from file " + file.getAbsolutePath());
		} catch (Throwable ex) {
			log.log(Level.SEVERE, "loading configuration from file " + file.getAbsolutePath() + " failed", ex);
		}
	}
}
