Tigase Kernel
=================

Tigase Kernel is an implementation of `IoC <https://en.wikipedia.org/wiki/Inversion_of_control>`__ created for Tigase XMPP Server. It is responsible for maintaining object lifecycle and provides mechanisms for dependency resolutions between beans.

Additionally, as and optional feature, Tigase Kernel is capable of configuring beans using a provided bean configurator.

Basics
------------

What is kernel?
^^^^^^^^^^^^^^^^^^^

Kernel is an instance of the ``Kernel`` class which is responsible for managing scope and visibility of beans. Kernel handles bean:

-  registration of a bean

-  unregistration of a bean

-  initialization of a bean

-  deinitialization of a bean

-  dependency injection to the bean

-  handling of bean lifecycle

-  registration of additional beans based on annotations *(optionally using registered class implementing ``BeanConfigurator`` as ``defaultBeanConfigurator``)*

-  configuration of a bean *(optionally thru registered class implementing ``BeanConfigurator`` as ``defaultBeanConfigurator``)*

Kernel core is responsible for dependency resolution and maintaining lifecycle of beans. Other features, like proper configuration of beans are done by additional beans working inside the Kernel.

Kernel identifies beans by their name, so each kernel may have only one bean named ``abc``. If more than one bean has the same name, then the last one registered will be used as its registration will override previously registered beans. You may use whatever name you want to name a bean inside kernel but it cannot:

-  be ``service`` as this name is used by Tigase Kernel internally when \`RegistrarBean`s are in use (see `RegistrarBean <#registrarBean>`__

-  end with ``#KERNEL`` as this names are also used by Tigase Kernel internally

.. Tip::

   Kernel initializes beans using lazy initialization. This means that if a bean is not required by any other beans, or not retrieved from the kernel manually, an instance will not be created.

During registration of a bean, the kernel checks if there is any beans which requires this newly registered bean and if so, then instance of a newly registered bean will be created and injected to fields which require it.

What is a kernel scope?
^^^^^^^^^^^^^^^^^^^^^^^^^^^

Each kernel has its own scope in which it can look for beans. By default kernel while injecting dependencies may look for them only in the same kernel instance in which new instance of a bean is created or in the direct parent kernel. This way it is possible to have separate beans named the same in the different kernel scopes.

.. Note::

   If bean is marked as ``exportable``, it is also visible in all descendants kernel scopes.

What is a bean?
^^^^^^^^^^^^^^^^^^^^^^^^^^^
A bean is a named instance of the class which has parameterless constructor and which is registered in the kernel.

.. Warning::

    Parameterless constructor is a required as it will be used by kernel to create an instance of the bean, see `bean lifecycle <#beanLifecycle>`__.

Lifecycle of a bean
--------------------------

Creating instance of a bean
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Instantiation of a bean
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

During this step, kernel creates instance of the class which was registered for this bean (for more details see **Registration of a bean**). Instance of a bean is created using paremeterless constructor of a class.

.. Note::

   Bean instance is only created for required beans (i.e. beans that were injected somewhere).

.. Note::

   It’s possible to create bean instance without the need to inject it anywhere - such bean should be annoted with ``@Autostart`` annotation.

Configuring a bean *(optional)*
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In this step kernel passes class instance of a bean to the configurator bean (an instance of ``BeanConfigurator`` if available), for configuring it. During this step, ``BeanConfigurator`` instance, which is aware of the configuration loaded from the file, injects this configuration to the bean fields annotated with ``@ConfigField`` annotation. By default configurator uses reflections to access those fields. However, if a bean has a corresponding public ``setter``/``getter`` methods for a field annotated with ``@ConfigField`` (method parameter/return type matches field type), then configurator will use them instead of accessing a field via reflection.

.. Note::

   If there is no value for a field specified in the configuration or value is equal to the current value of the field, then configurator will skip setting value for this field (It will also not call ``setter`` method even if it exists).

At the end of the configuration step, if bean implements ``ConfigurationChangedAware`` interface, then method ``beanConfigurationChanged(Collection<String> changedFields)`` is being called, to notify bean about field names which values has changed. This is useful, if you need to update bean configuration, when you have all configuration available inside bean.

.. Note::

   Configuration of the bean may be changed at runtime and it will be applied in the same way as initial configuration is passed to the bean. So please keep in mind that ``getter``/``setter`` may be called multiple times - even for already configured and initialized bean.

Injecting dependencies
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

At this point kernel looks for the bean class fields annotated with ``@Inject`` and looks for a value for each of this fields. During this step, kernel checks list of available beans in this kernel, which matches field type and additional constraints specified in the annotation.

When a required value (instance of a bean) is found, then kernel tries to inject it using reflection. However, if there is a matching ``getter``/``setter`` defined for that field it will be called instead of reflection.

.. Note::

   If dependency changes, ie. due to reconfiguration, then value of the dependent field will change and ``setter`` will be called if it exists. So please keep in mind that ``getter``/``setter`` may be called multiple times - even for already configured and initialized bean.

Initialization of a bean
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

When bean is configured and dependencies are set, then initialization of a bean is almost finished. At this point, if bean implements ``Initializable`` interface, kernel calls ``initialize()`` method to allow bean initialize properly if needed.

Destroying instance of a bean
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

When bean is being unloaded, then reference to its instance is just dropped. However, if bean class implements ``UnregisterAware`` interface, then kernel calls ``beforeUnregister()`` method. This is very useful in case which bean acquires some resources during initialization and should release them now.

.. Note::

   This method will not be called if bean was not initialized fully (bean initialization step was note passed)!

Reconfiguration of a bean *(optional)*
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

At any point in time bean may be reconfigured by default bean configurator (instance of ``BeanConfigurator``) registered in the kernel. This will happen in the same way as it described in `Configuring a bean <#beanConfiguration>`__ in **Creating instace of a bean** section.

Updating dependencies
^^^^^^^^^^^^^^^^^^^^^^^^

It may happen, that due to reconfiguration or registration/unregistration or activation/deactivation of some other beans dependencies of a bean will change. As a result, Tigase Kernel will inject new dependencies as described in `Injecting dependencies <#beanInjectingDependencies>`__

Registration of a bean
---------------------------

There are few ways to register a bean.

Using annotation *(recommended but optional)*
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To register a bean using annotation you need to annotate it with ``@Bean`` annotation and pass values for following properties:

-  ``name`` - name under which item should be registered

-  ``active`` - ``true`` if bean should be enabled without enabling it in the configuration *(however it is still possible to disable it using configuration)*

-  ``parent`` - class of the parent bean which activation should trigger registration of your bean. **In most cases parent class should be implementing ``RegistrarBean``**

-  ``parents`` - array of classes which should be threaten as ``parent`` classes if more than one parent class is required *(optional)*

-  ``exportable`` - ``true`` if bean should be visible in all descendant kernels (in other case default visibility rules will be applied) *(optional)*

-  ``selectors`` - array of selector classes which will decide whether class should be registered or not *(optional)*

.. Tip::

   If ``parent`` is set to ``Kernel.class`` it tells kernel to register this bean in the root/main kernel (top-level kernel).

If you want your bean ``SomeDependencyBean`` to be registered when another bean ``ParentBean`` is being registered (like a required dependency), you may annotate your bean ``SomeDependencyBean`` with ``@Bean`` annotation like this example:

.. code:: java

   @Bean(name = "nameOfSomeDependencyBean", parent = ParentBean.class, active = true)
   public class SomeDependencyBean {
       ...
   }

.. Warning::

    Works only if bean registered as ``defaultBeanConfigurator`` supports this feature. By default Tigase XMPP Server uses ``DSLBeanConfigurator`` which is subclass of ``AbstractBeanConfigurator`` which provides support for this feature.

Setting ``parent`` to class not implementing ``RegistrarBean`` interface
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If ``parent`` is set to the class which is not implementing ``RegistrarBean`` interface, then your bean will be registered in the same kernel scope in which parent bean is registered. If you do so, ie. by setting parent to the class of the bean which is registered in the ``kernel1`` and your bean will be also registered in ``kernel1``. As the result it will be exposed to other beans in the same kernel scope. This also means that if you will configure it in the same way as you would set ``parent`` to the ``parent`` of annotation of the class to which your ``parent`` point to.

**Example.**

.. code:: java

   @Bean(name="bean1", parent=Kernel.class)
   public class Bean1 {
       @ConfigField(desc="Description")
       private int field1 = 0;
       ....
   }

   @Bean(name="bean2", parent=Bean1.class)
   public class Bean2 {
       @ConfigField(desc="Description")
       private int field2 = 0;
       ....
   }

In this case it means that ``bean1`` is registered in the root/main kernel instance. At the same time, ``bean2`` is also registered to the root/main kernel as its value of ``parent`` property of annotation points to class not implementing ``RegistrarBean``.

To configure value of ``field1`` in instance of ``bean1`` and ``field2`` in instance of ``bean2`` in DSL (for more information about DSL format please check section ``DSL file format`` of the ``Admin Guide``) you would need to use following entry in the config file:

.. code::

   bean1 {
       field1 = 1
   }
   bean2 {
       field2 = 2
   }

As you can see, this resulted in the ``bean2`` configuration being on the same level as ``bean1`` configuration.

Calling kernel methods
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

As a class
~~~~~~~~~~~~~~

To register a bean as a class, you need to have an instance of a Tigase Kernel execute it’s ``registerBean()`` method passing your ``Bean1`` class.

.. code:: java

   kernel.registerBean(Bean1.class).exec();

.. Note::

   To be able to use this method you will need to annotate ``Bean1`` class with ``@Bean`` annotation and provide a bean name which will be used for registration of the bean.

As a factory
~~~~~~~~~~~~~~

To do this you need to have an instance of a Tigase Kernel execute it’s ``registerBean()`` method passing your bean ``Bean5`` class.

.. code:: java

   kernel.registerBean("bean5").asClass(Bean5.class).withFactory(Bean5Factory.class).exec();

As an instance
~~~~~~~~~~~~~~

For this you need to have an instance of a Tigase Kernel execute it’s ``registerBean()`` method passing your bean ``Bean41`` class instance.

.. code:: java

   Bean41 bean41 = new Bean41();
   kernel.registerBean("bean4_1").asInstance(bean41).exec();

.. Warning::

    Beans registered as an instance will not inject dependencies. As well this bean instances will not be configured by provided bean configurators.

Using config file *(optional)*
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If there is registered a bean ``defaultBeanConfigurator`` which supports registration in the config file, it is possible to do so. By default Tigase XMPP Server uses ``DSLBeanConfigurator`` which provides support for that and registration is possible in the config file in DSL. As registration of beans using a config file is part of the admin of the Tigase XMPP Server tasks, it is described in explained in the Admin Guide in subsection ``Defining bean`` of ``DSL file format`` section.

.. Tip::

   This way allows admin to select different class for a bean. This option should be used to provide alternative implementations to the default beans which should be registered using annotations.

.. Warning::

    Works only if bean registered as ``defaultBeanConfigurator`` supports this feature. By default Tigase XMPP Server uses ``DSLBeanConfigurator`` which provides support for that.

Defining dependencies
-------------------------

All dependencies are defined with annotations:

.. code:: java

   public class Bean1 {
     @Inject
     private Bean2 bean2;

     @Inject(bean = "bean3")
     private Bean3 bean3;

     @Inject(type = Bean4.class)
     private Bean4 bean4;

     @Inject
     private Special[] tableOfSpecial;

     @Inject(type = Special.class)
     private Set<Special> collectionOfSpecial;

     @Inject(nullAllowed = true)
     private Bean5 bean5;
   }

Kernel automatically determines type of a required beans based on field type. As a result, there is no need to specify the type of a bean in case of ``bean4`` field.

When there are more than one bean instances matching required dependency fields, the type needs to be an array or collection. If kernel is unable to resolve dependencies, it will throw an exception unless ``@Inject`` annotation has ``nullAllowed`` set to ``true``. This is useful to make some dependencies optional. To help kernel select a single bean instance when more that one bean will match field dependency, you may set name of a required bean as shown in annotation to field ``bean3``.

Dependencies are inserted using getters/setters if those methods exist, otherwise they are inserted directly to the fields. Thanks to usage of setters, it is possible to detect a change of dependency instance and react as required, i.e. clear internal cache.

.. Warning::

    Kernel is resolving dependencies during injection only using beans visible in its scope. This makes it unable to inject an instance of a class which is not registered in the same kernel as a bean or not visible in this kernel scope (see `Scope and visibility <#kernelScope>`__).

.. Warning::

    If two beans have bidirectional dependencies, then it is required to allow at least one of them be ``null`` (make it an optional dependency). In other case it will create circular dependency which cannot be satisfied and kernel will throw exceptions at runtime.

Nested kernels and exported beans
--------------------------------------

Tigase Kernel allows the usage of nested kernels. This allows you to create complex applications and maintain proper separation and visibility of beans in scopes as each module (subkernel) may work within its own scope.

Subkernels may be created using one of two ways:

Manual registration of new a new kernel
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

You can create an instance of a new kernel and register it as a bean within the parent kernel.

.. code:: java

   Kernel parent = new Kernel("parent");
   Kernel child = new Kernel("child");
   parent.registerBean(child.getName()).asInstance(child).exec();

Usage of RegistrarBean
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

You may create a bean which implements the ``RegistrarBean`` interfaces. For all beans that implement this interface, subkernels are created. You can access this new kernel within an instance of ``RegistrarBean`` class as ``register(Kernel)`` and ``unregister(Kernel)`` methods are called once the ``RegistrarBean`` instance is created or destroyed.

There is also an interface named ``RegistrarBeanWithDefaultBeanClass``. This interface is very useful if you want or need to create a bean which would allow you to configure many subbeans which will have the same class but different names and you do not know names of those beans before configuration will be set. All you need to do is to implement this interface and in method ``getDefaultBeanClass()`` return class which should be used for all subbeans defined in configuration for which there will be no class configured.

As an example of such use case is ``dataSource`` bean, which allows administrator to easily configure many data sources without passing their class names, ie.

.. code::

   dataSource {
       default () { .... }
       domain1 () { .... }
       domain2 () { .... }
   }

With this config we just defined 3 beans named ``default``, ``domain1`` and ``domain2``. All of those beans will be instances of a class returned by a ``getDefaultBeanClass()`` method of ``dataSource`` bean.

Scope and visibility
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Beans that are registered within a parent kernel are visible to beans registered within the first level of child kernels. However, **beans registered within child kernels are not available to beans registered in a parent kernel** with the exception that they are visible to bean that created the subkernel (an instance of ``RegistrarBean``).

It is possible to export beans so they can be visible outside the first level of child kernels.

To do so, you need to mark the bean as exportable using annotations or by calling the ``exportable()`` method.

**Using annotation.**

.. code:: java

   @Bean(name = "bean1", exportable = true)
   public class Bean1 {
   }

**Calling ``exportable()``.**

.. code:: java

   kernel.registerBean(Bean1.class).exportable().exec();

Dependency graph
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Kernel allows the creation of a dependency graph. The following lines will generate it in a format supported by `Graphviz <http://www.graphviz.org>`__.

.. code:: java

   DependencyGrapher dg = new DependencyGrapher(krnl);
   String dot = dg.getDependencyGraph();

Configuration
----------------

The kernel core does not provide any way to configure created beans. Do do that you need to use the ``DSLBeanConfigurator`` class by providing its instance within configuration and registration of this instances within kernel.

**Example.**

.. code:: java

   Kernel kernel = new Kernel("root");
   kernel.registerBean(DefaultTypesConverter.class).exportable().exec();
   kernel.registerBean(DSLBeanConfigurator.class).exportable().exec();
   DSLBeanConfigurator configurator = kernel.getInstance(DSLBeanConfigurator.class);
   Map<String, Object> cfg = new ConfigReader().read(file);
   configurator.setProperties(cfg);
   // and now register other beans...

DSL and kernel scopes
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

DSL is a structure based format explained in `Tigase XMPP Server Administration Guide: DSL file format section <http://docs.tigase.org/tigase-server/snapshot/Administration_Guide/html/#dslConfig>`__. **It is important to know that kernel and beans structure have an impact on what the configuration in DSL will look like.**

**Example kernel and beans classes.**

.. code:: java

   @Bean(name = "bean1", parent = Kernel.class, active = true )
   public class Bean1 implements RegistrarBean {
     @ConfigField(desc = "V1")
     private String v1;

     public void register(Kernel kernel) {
       kernel.registerBean("bean1_1").asClass(Bean11.class).exec();
     }

     public void unregister(Kernel kernel) {}
   }

   public class Bean11 {
     @ConfigField(desc = "V11")
     private String v11;
   }

   @Bean(name = "bean1_2", parent = Bean1.class, active = true)
   public class Bean12 {
     @ConfigField(desc = "V12")
     private String v12;
   }

   @Bean(name = "bean2", active = true)
   public class Bean2 {
     @ConfigField(desc = "V2")
     private String v2;
   }

   public class Bean3 {
     @ConfigField(desc = "V3")
     private String v3;
   }

   public class Main {
     public static void main(String[] args) {
       Kernel kernel = new Kernel("root");
       kernel.registerBean(DefaultTypesConverter.class).exportable().exec();
       kernel.registerBean(DSLBeanConfigurator.class).exportable().exec();
       DSLBeanConfigurator configurator = kernel.getInstance(DSLBeanConfigurator.class);
       Map<String, Object> cfg = new ConfigReader().read(file);
       configurator.setProperties(cfg);

       configurator.registerBeans(null, null, config.getProperties());

       kernel.registerBean("bean4").asClass(Bean2.class).exec();
       kernel.registerBean("bean3").asClass(Bean3.class).exec();
     }
   }

Following classes will produce following structure of beans:

-  "bean1" of class ``Bean1``

   -  "bean1_1" of class ``Bean11``

   -  "bean1_2" of class ``Bean12``

-  "bean4" of class ``Bean2``

-  "bean3" of class ``Bean3``

.. Note::

   This is a simplified structure, the actual structure is slightly more complex. However. this version makes it easier to explain structure of beans and impact on configuration file structure.

.. Warning::

    Even though ``Bean2`` was annotated with name ``bean2``, it was registered with name ``bean4`` as this name was passed during registration of a bean in ``main()`` method.

.. Tip::

   ``Bean12`` was registered under name ``bean1_2`` as subbean of ``Bean1`` as a result of annotation of ``Bean12``

As mentioned DSL file structure depends on structure of beans, a file to set a config field in each bean to bean name should look like that:

.. code::

   'bean1' () {
       'v1' = 'bean1'

       'bean1_1' () {
           'v11' = 'bean1_1'
       }
       'bean1_2' () {
           'v12' = 'bean1_2'
       }
   }
   'bean4' () {
       'v2' = 'bean4'
   }
   'bean3' () {
       'v3' = 'bean3'
   }