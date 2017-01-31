package tigase.kernel.core;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.kernel.BeanUtils;
import tigase.kernel.KernelException;
import tigase.kernel.Registrar;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.BeanFactory;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.BeanConfigurator;
import tigase.kernel.core.BeanConfig.State;

public class Kernel {

    static class DelegatedBeanConfig extends BeanConfig {

        private final BeanConfig original;

        DelegatedBeanConfig(String localName, BeanConfig src) {
            super(localName, src.getClazz());
            original = src;
        }

        @Override
        public Class<?> getClazz() {
            return original.getClazz();
        }

        @Override
        public BeanConfig getFactory() {
            return original.getFactory();
        }

        @Override
        public Map<Field, Dependency> getFieldDependencies() {
            return original.getFieldDependencies();
        }

        @Override
        public Kernel getKernel() {
            return original.getKernel();
        }

        public BeanConfig getOriginal() {
            return original;
        }

        @Override
        public State getState() {
            return original.getState();
        }

        @Override
        public boolean isExportable() {
            return original.isExportable();
        }

        @Override
        public String toString() {
            return original.toString();
        }
    }

    private final Map<BeanConfig, Object> beanInstances = new HashMap<BeanConfig, Object>();

    BeanConfigBuilder currentlyUsedConfigBuilder;

    private final DependencyManager dependencyManager = new DependencyManager();

    protected final Logger log = Logger.getLogger(this.getClass().getName());

    private String name;

    private Kernel parent;

    public Kernel() {
        this("<unknown>");
    }

    public Kernel(String name) {
        this.name = name;

        BeanConfig bc = dependencyManager.createBeanConfig(this, "kernel", Kernel.class);
        dependencyManager.register(bc);
        registerBean("kernel").asInstance(this).exec();

        putBeanInstance(bc, this);
    }

    private Object createNewInstance(BeanConfig beanConfig) {
        try {
            if (beanConfig.getFactory() != null) {
                BeanFactory<?> factory = (BeanFactory<?>) beanConfig.getKernel().getInstance(beanConfig.getFactory());
                return factory.createInstance();
            } else {
                if (log.isLoggable(Level.FINER))
                    log.finer("[" + getName() + "] Creating instance of bean " + beanConfig.getBeanName());
                Class<?> clz = beanConfig.getClazz();

                return clz.newInstance();
            }
        } catch (Exception e) {
            throw new KernelException("Can't create instance of bean '" + beanConfig.getBeanName() + "'", e);
        }
    }

    public DependencyManager getDependencyManager() {
        return dependencyManager;
    }

    public <T> T getInstance(BeanConfig beanConfig) {
        if (beanConfig instanceof DelegatedBeanConfig) {
            BeanConfig b = ((DelegatedBeanConfig) beanConfig).original;
            return (T) beanConfig.getKernel().beanInstances.get(b);
        } else {
            return (T) beanConfig.getKernel().beanInstances.get(beanConfig);
        }
    }

    public <T> T getInstance(Class<T> beanClass) {
        return getInstance(beanClass, true);
    }

    @SuppressWarnings("unchecked")
    protected <T> T getInstance(Class<T> beanClass, boolean allowNonExportable) {
        final List<BeanConfig> bcs = dependencyManager.getBeanConfigs(beanClass, allowNonExportable);

        if (bcs.size() > 1)
            throw new KernelException("Too many beans implemented class " + beanClass);
        else if (bcs.isEmpty() && this.parent != null && this.parent != this) {
            return this.parent.getInstance(beanClass, false);
        }

        if (bcs.isEmpty())
            throw new KernelException("Can't find bean implementing " + beanClass);

        BeanConfig bc = bcs.get(0);

        if (bc.getState() != State.initialized) {
            try {
                initBean(bc, new HashSet<BeanConfig>(), 0);
            } catch (Exception e) {
                e.printStackTrace();
                throw new KernelException(e);
            }
        }

        Object result = bc.getKernel().getInstance(bc);

        return (T) result;
    }

    @SuppressWarnings("unchecked")
    public <T> T getInstance(String beanName) {
        BeanConfig bc = dependencyManager.getBeanConfig(beanName);

        if (bc == null && parent != null && parent.getDependencyManager().isBeanClassRegistered(beanName)) {
            return parent.getInstance(beanName);
        }

        if (bc == null)
            throw new KernelException("Unknown bean '" + beanName + "'.");

        if (bc.getState() != State.initialized) {
            try {
                bc.getKernel().initBean(bc, new HashSet<BeanConfig>(), 0);
            } catch (Exception e) {
                e.printStackTrace();
                throw new KernelException(e);
            }
        }

        Object result = bc.getKernel().getInstance(bc);

        return (T) result;
    }

    public String getName() {
        return name;
    }

    public Collection<String> getNamesOf(Class<?> beanType) {
        ArrayList<String> result = new ArrayList<String>();
        List<BeanConfig> bcs = dependencyManager.getBeanConfigs(beanType);
        for (BeanConfig beanConfig : bcs) {
            result.add(beanConfig.getBeanName());
        }
        return Collections.unmodifiableCollection(result);
    }

    public Kernel getParent() {
        return parent;
    }

    public void initAll() {
        try {
            for (BeanConfig bc : dependencyManager.getBeanConfigs()) {
                if (bc.getState() != State.initialized) {
                    initBean(bc, new HashSet<BeanConfig>(), 0);
                }
            }
        } catch (Exception e) {
            throw new KernelException("Can't initialize all beans", e);
        }
    }

    protected void initBean(BeanConfig tmpBC, Set<BeanConfig> createdBeansConfig, int deep) throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, InstantiationException {
        final BeanConfig beanConfig = tmpBC instanceof DelegatedBeanConfig ? ((DelegatedBeanConfig) tmpBC).original : tmpBC;

        if (beanConfig.getState() == State.initialized)
            return;

        Object bean;
        if (beanConfig.getState() == State.registered) {
            beanConfig.setState(State.instanceCreated);
            if (beanConfig.getFactory() != null && beanConfig.getFactory().getState() != State.initialized) {
                initBean(beanConfig.getFactory(), new HashSet<BeanConfig>(), 0);
            }
            bean = createNewInstance(beanConfig);
            beanConfig.getKernel().putBeanInstance(beanConfig, bean);
            createdBeansConfig.add(beanConfig);
        } else {
            bean = beanConfig.getKernel().getInstance(beanConfig);
        }

        for (final Dependency dep : beanConfig.getFieldDependencies().values()) {
            injectDependencies(bean, dep, createdBeansConfig, deep);
        }

        BeanConfigurator beanConfigurator;
        try {
            if (isBeanClassRegistered(BeanConfigurator.DEFAULT_CONFIGURATOR_NAME) && !beanConfig.getBeanName().equals(BeanConfigurator.DEFAULT_CONFIGURATOR_NAME))
                beanConfigurator = getInstance(BeanConfigurator.DEFAULT_CONFIGURATOR_NAME);
            else
                beanConfigurator = null;
        } catch (KernelException e) {
            beanConfigurator = null;
        }

        if (beanConfigurator != null) {
            beanConfigurator.configure(beanConfig, bean);
        }

        if (deep == 0) {
            for (BeanConfig bc : createdBeansConfig) {
                Object bi = bc.getKernel().getInstance(bc);
                bc.setState(State.initialized);
                if (bi instanceof Initializable) {
                    ((Initializable) bi).initialize();
                }
            }

            if (Registrar.class.isAssignableFrom(beanConfig.getClazz())) {
                RegistrarKernel k = new RegistrarKernel();
                k.setName(beanConfig.getBeanName());
                registerBean(beanConfig.getBeanName() + "#KERNEL").asInstance(k).exec();
                ((Registrar) bean).register(k);
                // ((Registrar) bean).start(k);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void inject(Object[] data, Dependency dependency, Object toBean) throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, InstantiationException {

        if (!dependency.isNullAllowed() && data == null)
            throw new KernelException("Can't inject <null> to field " + dependency.getField());

        Object valueToSet;
        if (data == null) {
            valueToSet = null;
        } else if (Collection.class.isAssignableFrom(dependency.getField().getType())) {
            Collection o;

            if (!dependency.getField().getType().isInterface()) {
                o = (Collection) dependency.getField().getType().newInstance();
            } else if (dependency.getField().getType().isAssignableFrom(Set.class)) {
                o = new HashSet();
            } else {
                o = new ArrayList();
            }

            o.addAll(Arrays.asList(data));

            valueToSet = o;
        } else {
            Object o;
            if (data != null && dependency.getField().getType().equals(data.getClass())) {
                o = data;
            } else {
                int l = Array.getLength(data);
                if (l > 1)
                    throw new KernelException("Can't put many objects to single field " + dependency.getField());
                if (l == 0)
                    o = null;
                else
                    o = Array.get(data, 0);
            }

            valueToSet = o;
        }

        BeanUtils.setValue(toBean, dependency.getField(), valueToSet);
    }

    private void injectDependencies(Object bean, Dependency dep, Set<BeanConfig> createdBeansConfig, int deep)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException {
        BeanConfig[] dependentBeansConfigs = dependencyManager.getBeanConfig(dep);
        ArrayList<Object> dataToInject = new ArrayList<Object>();

        for (BeanConfig b : dependentBeansConfigs) {
            if (b == null) {
                dataToInject.add(null);
            } else {
                if (!b.getKernel().beanInstances.containsKey(b)) {
                    initBean(b, createdBeansConfig, deep + 1);
                }
                Object beanToInject = b.getKernel().getInstance(b);
                // if (beanToInject != null)
                dataToInject.add(beanToInject);
            }
        }
        Object[] d;
        if (dataToInject.isEmpty()) {
            d = new Object[]{};
        } else if (dep.getType() != null) {
            Object[] z = (Object[]) Array.newInstance(dep.getType(), 1);
            d = dataToInject.toArray(z);
        } else {
            d = dataToInject.toArray();
        }
        if (log.isLoggable(Level.FINER))
            log.finer("[" + getName() + "] Injecting " + Arrays.toString(d) + " to " + dep.getBeanConfig() + "#" + dep);

        inject(d, dep, bean);

    }

    void injectIfRequired(final BeanConfig beanConfig) {
        try {
            Collection<Dependency> dps = dependencyManager.getDependenciesTo(beanConfig);
            for (Dependency dep : dps) {
                BeanConfig depbc = dep.getBeanConfig();

                if (depbc.getState() == State.initialized) {
                    if (beanConfig.getState() != State.initialized)
                        initBean(beanConfig, new HashSet<BeanConfig>(), 0);
                    Object bean = depbc.getKernel().getInstance(depbc);

                    injectDependencies(bean, dep, new HashSet<BeanConfig>(), 0);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new KernelException("Can't inject bean " + beanConfig + " to dependend beans.", e);
        }
    }

    public boolean isBeanClassRegistered(final String beanName) {
        boolean x = dependencyManager.isBeanClassRegistered(beanName);
        if (x == false && parent != null) {
            x = parent.isBeanClassRegistered(beanName);
        }
        return x;
    }

    public void ln(String exportingBeanName, Kernel destinationKernel, String destinationName) {
        final BeanConfig sbc = dependencyManager.getBeanConfig(exportingBeanName);
        // Object bean = getInstance(sbc.getBeanName());

        BeanConfig dbc = new DelegatedBeanConfig(destinationName, sbc);

        destinationKernel.dependencyManager.register(dbc);
    }

    void putBeanInstance(BeanConfig beanConfig, Object beanInstance) {
        this.beanInstances.put(beanConfig, beanInstance);
        if (beanInstance instanceof Kernel && beanInstance != this) {
            ((Kernel) beanInstance).setParent(this);
        }
        beanConfig.setState(State.initialized);
    }

    public BeanConfigBuilder registerBean(Class<?> beanClass) {
        if (currentlyUsedConfigBuilder != null)
            throw new KernelException("Registration of bean '" + currentlyUsedConfigBuilder.getBeanName()
                    + "' is not finished yet!");

        Bean annotation = beanClass.getAnnotation(Bean.class);
        if (annotation == null || annotation.name() == null || annotation.name().isEmpty())
            throw new KernelException("Name of bean is not defined.");

        BeanConfigBuilder builder = new BeanConfigBuilder(this, dependencyManager, annotation.name());
        this.currentlyUsedConfigBuilder = builder;
        builder.asClass(beanClass);
        return builder;
    }

    public BeanConfigBuilder registerBean(String beanName) {
        if (currentlyUsedConfigBuilder != null)
            throw new KernelException("Registration of bean '" + currentlyUsedConfigBuilder.getBeanName()
                    + "' is not finished yet!");
        BeanConfigBuilder builder = new BeanConfigBuilder(this, dependencyManager, beanName);
        this.currentlyUsedConfigBuilder = builder;
        return builder;
    }

    public void setName(String name) {
        this.name = name;
    }

    void setParent(Kernel parent) {
        this.dependencyManager.setParent(parent.getDependencyManager());
        this.parent = parent;
    }

    public void startSubKernels() {
        for (BeanConfig bc : dependencyManager.getBeanConfigs(Registrar.class)) {
            Registrar r = getInstance(bc.getBeanName());
            Kernel k = getInstance(bc.getBeanName() + "#KERNEL");
            r.start(k);
            k.startSubKernels();
        }

    }

    public void unregister(final String beanName) {
        if (log.isLoggable(Level.FINER))
            log.finer("[" + getName() + "] Unregistering bean " + beanName);
        BeanConfig unregisteredBeanConfig = dependencyManager.getBeanConfig(beanName);
        if (unregisteredBeanConfig.getKernel() != this) {
            unregisteredBeanConfig.getKernel().unregister(beanName);
            return;
        }

        unregisterInt(beanName);
        try {
            for (BeanConfig bc : dependencyManager.getBeanConfigs()) {
                if (bc.getState() != State.initialized)
                    continue;
                Object ob = bc.getKernel().getInstance(bc);
                for (Dependency d : bc.getFieldDependencies().values()) {
                    if (DependencyManager.match(d, unregisteredBeanConfig)) {
                        BeanConfig[] cbcs = dependencyManager.getBeanConfig(d);
                        if (cbcs.length == 1) {// Clearing single-instance
                            // dependency. Like single field.
                            // BeanConfig cbc = cbcs[0];
                            // if (cbc != null && cbc.equals(removingBC)) {
                            inject(null, d, ob);
                            // }
                        } else if (cbcs.length > 1) { // Clearing multi-instance
                            // dependiency. Like
                            // collections and arrays.

                            injectDependencies(ob, d, new HashSet<BeanConfig>(), 0);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new KernelException("Can't unregister bean", e);
        } finally {
            dependencyManager.unregister(beanName);
        }
    }

    void unregisterInt(String beanName) {
        if (dependencyManager.isBeanClassRegistered(beanName)) {
            // unregistering
            if (log.isLoggable(Level.FINER))
                log.finer("[" + getName() + "] Found registred bean " + beanName + ". Unregistering...");

            BeanConfig oldBeanConfig = dependencyManager.unregister(beanName);
            Object i = oldBeanConfig.getKernel().beanInstances.remove(oldBeanConfig);
            if (i != null && i instanceof UnregisterAware) {
                try {
                    ((UnregisterAware) i).beforeUnregister();
                } catch (Exception e) {
                    e.printStackTrace();
                    log.log(Level.WARNING, "Problem during unregistering bean", e);
                }
            }
        }
    }

}
