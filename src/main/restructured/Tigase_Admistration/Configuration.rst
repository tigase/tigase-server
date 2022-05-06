Configuration
=================

When the user tries to setup the client for the first time he comes across 2 configuration files: ``tigase.conf`` and ``config.tdsl`` in the ``/etc`` folder. Here is a brief explanation what all those files are about and in other sections you can learn all the details needed to configure the server.

1. `config.tdsl <#dslConfig>`__ file is a simple text file with server parameters in form: **key** = **value**. When the XML configuration file is missing the Tigase server reads ``config.tdsl`` file and uses parameters found there as defaults for generation of the XML file. Therefore if you change the ``config.tdsl`` file you normally have to stop the server, remove the XML file and start the server again. All the settings from the ``config.tdsl`` are read and applied to the XML configuration. The properties file is easy to read and very safe to modify. At the moment this is the recommended way change the server configuration.

2. `tigase.conf <#manualconfig>`__ is the Tigase server startup configuration. It is actually not used by the server itself. It rather contains operating system settings and environment parameters to correctly run the `Java Virtual Machine <http://java.sun.com/>`__. It is only useful on the unix-like systems with Bash shell. If you run the server on MS Windows systems ``tigase.bat`` and ``wrapper.conf`` files are used instead. The ``tigase.conf`` file is read and loaded by the ``scripts/tigase.sh`` shell script which also scans the operating system environment for Java VM and other tools needed.

DSL file format
-------------------

In previous Tigase XMPP Server releases configuration was stored in properties based configuration file. From Tigase XMPP Server 8.0.0 release it will be required to use new DSL based configuration file format. This file format was inspired by Groovy language syntax and new core feature of Tigase XMPP Server - Tigase Kernel Framework.

why new format?
^^^^^^^^^^^^^^^^^^^^^

In properties configuration format each line contained key and value with optional definition of type of stored value:

.. code::

   c2s/ports[i]=5222,5223

where ``c2s/ports`` was name of property, ``[i]`` defined that type of value is array of integers, and ``5222,5223`` was comma separated list of values.

This format worked but in fact ``c2s/ports`` was not name of property you configured but key which was later split on ``/`` char to parts which defined by names path to property which name was in last part.From that you can see that it was domain based setting of properties.

Except from this multi-part keys we also used properties starting with ``--`` which were global properties accessible for every part of application, i.e.: to add new component and set some properties you needed to write:

.. code::

   --comp-name-1=pubsub
   --comp-class-1=tigase.pubsub.PubSubComponent
   pubsub/test[B]=true
   pubsub/pubsub-repo-url="jdbc:XXXX:XXXX/db_name"

This lead to mistakes like duplicated definition of name and class for same number of component or redefined property value in other place of a configuration file - especially in cases where configuration was big.

In this configuration structure it was hard to tell where is configuration for particular component or what databases this installation uses. This could be defined all over the file.

In this release we are introducing Tigase Kernel Framework, which allows to configure beans in configuration file and even define usage of new beans loaded from external jars which can modify behavior of Tigase components. This would make configuration file even more complex, difficult and less readable.

What is DSL?
^^^^^^^^^^^^^^^

DSL stands for domain-specific language - in this case language created for storage of configuration.

Now we use domain based configuration which means that our configuration file is not a flat key=value storage but it defines objects, it's properties and assigned values.

To illustrate it better let's start with a simple example. In properties file in order to configure PubSub component named ``pubsub`` you would use following properties:

.. code::

   --comp-name-1=pubsub
   --comp-class-1=tigase.pubsub.PubSubComponent
   pubsub/test[B]=true

In DSL based configuration this would be replaced by following block

.. code::

   pubsub (class: tigase.pubsub.PubSubComponent) {
       # comment
       test = true
   }

in which we define bean with name `pubsub` and set it's class inside ``()`` block to ``tigase.pubsub.PubSubComponent``.
We also use block between ``{}`` chars to define properties which are related to bean.
Which means this properties will be passed only to this instance of Tigase PubSub Component, same as it was before where we needed to add prefix.
Entries after ``\#`` are comments, to pass ``#`` you need to wrap whole part containing it in ``''``, ie. ``'test#242'``

.. WARNING:: 
    If a string value assigned to a property contains any char from a following list ``=:,[]#+-*/`` it needs to be wrapped in a ``''``.

Why DSL?
^^^^^^^^^

DSL configuration format provides a number of advantages over the old system of configuration. All configurations for components are related in a single block, so they are not spread out over several different lines. No need for long property names, no longer have to invoke a long string of settings for multiple values. Support is provided for environment variables. No longer need to escape certain characters, making settings far more readable at a glance. Values may be set using basic calculations, such as ``100 * 200 * 2`` rather than ``40000``. Parameter type values are no longer necessary, no more [i], [S], [B] etc.. Comma separated values can now be simplified lists with separate entries being able to be in multiple lines.

Although the format may seem more complex, looking like a section of java code, the formatting is consistent and can be far more readable. After some experience with DSL format, you'll find it's far more intuitive and user friendly than it may appear. Of course if there's any real confusion, Tigase can automatically convert old style properties files to the DSL format using the following command:

.. code-block:: bash

   ./scripts/tigase.sh upgrade-config etc/tigase.conf

Setting property
~~~~~~~~~~~~~~~~~~

To set property you just write property name followed by `=` and value to set. This is always done in context of bean which configuration property you want to set.

.. code-block::

   test=true

It is also possible to set property in main context by placing property outside of any context.
This sets property which value is available to access by any bean.

Setting global property
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Like in properties file it is still possible to use property names starting with ``--`` without any context or any other properties at global scope. Format is the same as in case of setting property but they are defined without scope (in global scope). This properties are global and accessible by any bean but also set as system property in JVM.

Defining bean
~~~~~~~~~~~~~~~~~~

You can configure bean by using following format:

.. code:: bash

   beanName (class: className, active: activeValue, exportable: exportableValue) {
       # scope of bean properties
   }

where ``beanName`` is name under which you want to configure bean.
`beanName` must be wrapped in ``''``, if ``beanName`` contains characters like ``=:,[]#+-*/`` and is recommended, if ``beanName`` is numeric only.

Inside block between ``(` and `)`` you can define:

* ``class`` which will be used as a bean, in example above we set class as ``className``. *_(default: if you try to configure bean under name which has default class assigned with it in Tigase framework then this assigned class will be used. In other case you need to pass name of class to use as a bean)_*
* ``active`` (boolean) whether you want the bean to be active or not (beans with ``active`` set to ``false`` are not loaded). *_(default: true)_*
* ``exportable`` (boolean) defines if this bean should be exported and available for use for beans in inner scopes. This is advanced option in most cases it is recommended to omit this field in configuration. *_(default: false)_*

Spaces between ``beanName`` and block between ``()`` is optional as well as space between block ``()`` and block ``{}``.
It is recommended that properties of bean would be placed in separate lines with indentation and first property will be placed in new line.

.. important::

   Usage of ``()`` block is very important. When this block is used in configuration it automatically sets ``active`` property of bean definition for bean for which it is used to to `true`. This is done due to fact that default value of ``active`` is ``true``.

   If you omit it in configuration, you will set bean configuration but it may remain ``inactive``. In this state bean will not be loaded and as a result will not be used by Tigase XMPP Server.

Configuring bean
~~~~~~~~~~~~~~~~~~

If you know that bean is defined and you do not want to change it's activity or class then you can just pass properties to configure bean in following way:

.. code::

   beanName {
       # scope of bean properties
       test = true
   }


where ``beanName`` is name of bean to configure and `test` is name of property to set to ``true`` in this bean.

Format of values
~~~~~~~~~~~~~~~~~~

In properties based configuration file every property was defined as a string and only by defining expected format it was properly converted to expected value. In DSL it is possible to set values in two ways:

as an object
   Using this format you set list as a list and integer is set as an integer.

.. table::

+-------------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| Type        | Description                                                                                                                                                                                                            |
+=============+========================================================================================================================================================================================================================+
| **string**  | Wrap it in ``''``, ie. to set ``test`` as string you use ``'test'``                                                                                                                                                    |
+-------------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| **integer** | Just put value, ie. to set ``543`` use ``543``                                                                                                                                                                         |
+-------------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| **long**    | Put value and follow it with ``L``, ie. to set ``23645434`` as long use ``23645434L``                                                                                                                                  |
+-------------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| **float**   | Put value and follow it with ``f``, ie. to set ``231.342`` use ``231.342f``                                                                                                                                            |
+-------------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| **boolean** | To set value just use ``true`` or ``false``                                                                                                                                                                            |
+-------------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| **list**    | Lists can be of many types and to make it simple we decided to use as a comma separated list of values in proper format wrapped in ``[]``.                                                                             |
|             |                                                                                                                                                                                                                        |
|             | -  of strings - ``[ 'alfa', 'beta', 'gamma' ]``                                                                                                                                                                        |
|             |                                                                                                                                                                                                                        |
|             | -  of integers - ``[ 1, 2, 3, 4]``                                                                                                                                                                                     |
|             |                                                                                                                                                                                                                        |
|             | You can write it in multiple lines if you want:                                                                                                                                                                        |
|             |                                                                                                                                                                                                                        |
|             | ::                                                                                                                                                                                                                     |
|             |                                                                                                                                                                                                                        |
|             |    [                                                                                                                                                                                                                   |
|             |        'alfa'                                                                                                                                                                                                          |
|             |        'beta'                                                                                                                                                                                                          |
|             |        'gamma'                                                                                                                                                                                                         |
|             |    ]                                                                                                                                                                                                                   |
+-------------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
| **map**     | Maps can be written as a block of properties wrapped in ``{}``. This format of map is the same as used for passing configuration to bean properties. Keys and values can be written in separate lines *(recommended)*: |
|             |                                                                                                                                                                                                                        |
|             | ::                                                                                                                                                                                                                     |
|             |                                                                                                                                                                                                                        |
|             |    {                                                                                                                                                                                                                   |
|             |        test = true                                                                                                                                                                                                     |
|             |        ssl = false                                                                                                                                                                                                     |
|             |        ssl-certificate = '/test/cert.pem'                                                                                                                                                                              |
|             |        another-map = {                                                                                                                                                                                                 |
|             |            key = 'value'                                                                                                                                                                                               |
|             |        }                                                                                                                                                                                                               |
|             |    }                                                                                                                                                                                                                   |
|             |                                                                                                                                                                                                                        |
|             | or in single line *(separation with spaces is not required)*:                                                                                                                                                          |
|             |                                                                                                                                                                                                                        |
|             | ::                                                                                                                                                                                                                     |
|             |                                                                                                                                                                                                                        |
|             |    { test = true, ssl = false, ssl-certificate = '/test/cert.pem' }                                                                                                                                                    |
+-------------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+

as a plain string
   Very similar to properties based configuration, in fact values are passed in same format and later are converted to correct type by checking type expected by bean. *(Not recommended)*

.. table::

+-------------+-----------------------------------------------------------------------------------------------+
| Type        | Description                                                                                   |
+=============+===============================================================================================+
| **string**  | Just put value, ie. to set ``test`` use ``test``                                              |
+-------------+-----------------------------------------------------------------------------------------------+
| **integer** | Just put value, ie. to set ``543`` use ``543``                                                |
+-------------+-----------------------------------------------------------------------------------------------+
| **long**    | Put value, ie. to set ``23645434`` as long use ``23645434``                                   |
+-------------+-----------------------------------------------------------------------------------------------+
| **float**   | Put value, ie. to set ``231.342`` use ``231.342``                                             |
+-------------+-----------------------------------------------------------------------------------------------+
| **boolean** | To set value just use ``true`` or ``false``                                                   |
+-------------+-----------------------------------------------------------------------------------------------+
| **list**    | List needs to be written as comma separated list of values, ie. ``test,abc,efg`` or ``1,2,3`` |
+-------------+-----------------------------------------------------------------------------------------------+
| **map**     | Not possible                                                                                  |
+-------------+-----------------------------------------------------------------------------------------------+


Using values from System Properties and Environment Variables
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Now it is possible to use values of `system properties <https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html>`__ and `environment variables <https://docs.oracle.com/javase/tutorial/essential/environment/env.html>`__ and assign them to bean properties. For this purpose we added functions which can be used in DSL and which will return values of:

system property
   ``prop('property-name')`` or ``prop('property-name','default value')``

environment variable
   ``env('variable-name')``

**Example of setting value of system property and environment variable to bean ``user``.**

.. code::

   user {
     name = env('USER')
     home = prop('user.home')
     paths = [ prop('user.home'), prop('user.dir') ]
   }


.. Warning::

    For properties which accepts lists it is not allowed to set value using variable/property with comma separated values like ``value1,value2`` wrapped in ``[]``, ie. ``property = [ env('some-variable') ]``. It needs to be set in following way ``property = env('some-variable')``
 
Computed values
~~~~~~~~~~~~~~~~~~

With DSL configuration format we introduce support for computable values for properties. It is now possible to set value which is result of a computation, ie. concatenation of a strings or very simple mathematical expression. We currently support only following mathematical operations:

-  add

-  subtract

-  multiply

-  divide

**Example of setting environment variable related path and computed timeout.**

.. code::

   bean {
     # setting path to `some-subdirectory` of user home directory
     path = prop('user.home') + '/some-subdirectory/'

     # setting timeout to 5 minutes (setting value in milliseconds)
     timeout = 5L * 60 * 1000
     # previously it would need to be configured in following way:
     # timeout = 300000L
   }

.. warning::

    For properties which accepts lists it is not allowed to set value using computed values with comma separated values like ``value1,value2`` wrapped in ``[]``, ie. ``property = [ env('some-variable') + ',other-value' ]``. It needs to be set in following way ``property = env('some-variable') + ',other-value'``.

Period / Duration values
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Some configuration options allow control of execution of tasks with particular period or within certain duration. DSL file format accepts strings denoting particular amount of time, which follows Java’s native structures (see: `Period <https://docs.oracle.com/javase/8/docs/api/java/time/Period.html#parse-java.lang.CharSequence->`__ and `Duration <https://docs.oracle.com/javase/8/docs/api/java/time/Duration.html#parse-java.lang.CharSequence->`__ for detailed explanation).

-  ``Duration`` formats accepted are based on the ISO-8601 duration format ``PnDTnHnMn.nS`` with days considered to be exactly 24 hours, for example:

   -  ``PT20.345S`` - 20.345 seconds

   -  ``PT15M`` - 15 minutes (where a minute is 60 seconds)

   -  ``PT10H`` - 10 hours (where an hour is 3600 seconds)

   -  ``P2D`` - 2 days (where a day is 24 hours or 86400 seconds)

   -  ``P2DT3H4M`` - 2 days, 3 hours and 4 minutes

-  ``Period`` format is based on the ISO-8601 period formats PnYnMnD and PnW, for example, the following are valid inputs:

   -  ``P2Y`` - 2 years

   -  ``P3M`` - 3 months

   -  ``P4W`` - 4 weeks

   -  ``P5D`` - 5 days

   -  ``P1Y2M3D`` - 1 year, 2 months, 3 days

   -  ``P1Y2M3W4D`` - 1 year, 2 months, 3 weeks, 4 days


Example configuration file in DSL
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code:: dsl

   # Enable cluster mode
   --cluster-mode = true
   # Enable debugging for server and xmpp.impl
   --debug = 'server,xmpp.impl'
   # Set list of virtual hosts (old way)
   --virt-hosts = 'example.com,test-1.example.com,test-2.example.com'

   # Configure list of administrator jids
   admins = [ 'admin@zeus', 'http@macbook-pro-andrzej.local' ]
   # Set config type
   config-type = '--gen-config-def'

   # Configure dataSource bean with database configuration
   dataSource {
       # Configure default data source (using default implementation so class is omitted)
       default () {
           uri = 'jdbc:postgresql://127.0.0.1/tigase?user=test&password=test&autoCreateUser=true'
       }

       # Configure data source with name exaple.com (will be used by domain example.com)
       'example.com' () {
           uri = 'jdbc:mysq://127.0.0.1/example?user=test&password=test&autoCreateUser=true'
       }
   }

   # Configure C2S component
   c2s {
       # Enable Stream Management bean
       'urn:xmpp:sm:3' () {}

       # Register tigase.server.xmppclient.SeeOtherHostDualIP as seeOtherHost bean
       seeOtherHost (class: tigase.server.xmppclient.SeeOtherHostDualIP) {}

       # Add additional port 5224 which is SSL port and disable port 5223
       connections () {
           '5224' () {
                socket = ssl
             }
           '5223' (active: false) {}
       }
   }

   # Configure HTTP API component
   http {
       # Set list of API keys
       api-keys = [ 'test1234', 'test2356' ]
       rest {
           # Set value of environment property as a path to look for REST scripts
           rest-scripts-dir = env('TIGASE_REST_SCRIPTS_DIR')
       }
   }

   # Register pubsub-2 (class is passed as pubsub-2 name do not have default class assigned)
   pubsub-2 (class: tigase.pubsub.cluster.PubSubComponentClustered) {
       # Set configuration bean properties
       pubsubConfig {
           persistentPep = true
       }
       # Use tigase.pubsub.cluster.ClusteredNodeStrategy as advanced clustering strategy
       strategy (class: tigase.pubsub.cluster.ClusteredNodeStrategy) {}
   }

   # Configure Session Manager
   sess-man {
       # Here we enable pep, urn:xmpp:mam:1 processors and disable message-archive-xep-0136 processor
       pep () {}
       'urn:xmpp:mam:1' () {}
       message-archive-xep-0136 (active: false) {}

       # Define class used as clustering strategy (it is different than default so class is required)
       strategy (class: tigase.server.cluster.strategy.OnlineUsersCachingStrategy) {}
   }

Default configuration
^^^^^^^^^^^^^^^^^^^^^^^^^^

Tigase XMPP Server is packaged with a basic ``config.tdsl`` file that tells the server to start up in setup mode.

.. code:: dsl

   'config-type' = 'setup'

   http () {
       setup () {
           'admin-user' = 'admin'
       'admin-password' = 'tigase'
       }
   }

This tells Tigase to operate in a setup mode, and tells the http component to allow login with the username and password admin/tigase. With this you can enter the setup process that is covered in `this section <#webinstall>`__.

There are other options for config-type: ``default``, ``session-manager``, ``connection-managers``, and ``component``. For more information, visit `Config Type <#configType>`__ property description.

Startup File for tigase.sh - tigase.conf
--------------------------------------------

Property file names for ``tigase.sh`` startup script is a second parameter for the startup script. It can be skipped if environmental variables are set in different location or in different way.

Config file for startup script simply sets number of environment variables with the location of required components. Possible variables to set in this file are:

-  ``JAVA_HOME`` - location of Java installation home directory. **Must be set**.

-  ``TIGASE_HOME`` - location of Tigase installation home directory. *By default script try to find this location by searching directories from the location where the script has been run.*

-  ``TIGASE_CONSOLE_LOG`` - file to which all console messages will be redirected if server is run in background. By default it will be: ``TIGASE_HOME/logs/tigase-console.log``. **If this file/directory is not writable by Tigase process all console messages will be redirected to /dev/null**

-  ``TIGASE_PID`` location of the file with server PID number. By default it will be ``TIGASE_HOME/logs/tigase.pid``.

-  ``JAVA_OPTIONS`` - options for JVM like size of RAM allocated for the JVM, properties and so on.

-  ``TIGASE_OPTIONS`` - (optional) additional options for Tigase server program. You can tweak initial parameters for your environment here. If you want to specify custom location of your configuration file you should use ``--config-file <path/to/config.tdsl>`` configuration

Sample file to run **Tigase** with **PostgreSQL** database may look like:

.. code:: bash

   ENC="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
   DRV="-Djdbc.drivers=org.postgresql.Driver"
   JAVA_OPTIONS="${ENC} ${DRV} -server -Xms100M -Xmx100M "
   CLASSPATH=""
   TIGASE_CONFIG="tigase-pgsql.xml"
   TIGASE_OPTIONS=" "

Please note encoding settings. JVM by default uses encoding set in operating system environment. XMPP protocol, however uses ``UTF-8`` for all data processing. So the ENC settings enforces ``UTF-8`` encoding for all operations.

Another significant setting is \\'**CLASSPATH**'. It is intentionally set to an empty string. The **tigase.sh** startup script builds the **CLASSPATH** on it’s own from files found in **jars/** and **libs/** directories. It is advised to set the **CLASSPATH** to the empty string because the Tigase server scans all available classes to find all components and plugins implementation. If the **CLASSPATH** contains lots of libraries which are not used anyway it can cause a long startup time and high system loads.

Linux Settings for High Load Systems
---------------------------------------

There are a few basic settings you have to adjust for high load systems to make sure the server has enough resources to handle a big number of network connections.

The main parameter is a maximum number of opened files allowed for the process to keep at the same time. Each network connection uses a file handler, therefore if the limit is too low you can quickly run out of handlers and the server can not accept any more connections.

This limit is set on 2 levels - on the kernel level (``fs.file-max``) and on the system level (``nofile``).

Another kernel property which can be important in certain configurations (like transports installations or when you use proxy for Bosh connections) is: ``net.ipv4.ip_local_port_range``. This parameter can be set the same way as the ``fs.file-max`` property.

fs.file-max
^^^^^^^^^^^^^^

The ``fs.file-max`` kernel property is set via sysctl command. You can see current settings by executing the command:

.. code:: sh

   # sysctl fs.file-max
   fs.file-max = 358920

If you plan to run high load service with large number of server connections, then this parameter should be at least as twice big as the number of network connections you expect to support. You can change this setting by executing the command:

::

   # sysctl -w fs.file-max=360000
   fs.file-max = 360000

net.ipv4.ip_local_port_range
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

You can see current settings by executing the command:

.. code:: sh

   # sysctl net.ipv4.ip_local_port_range
   net.ipv4.ip_local_port_range = 32768    61000

You can change this setting by executing the command:

.. code:: sh

   # sysctl -w net.ipv4.ip_local_port_range="1024 65000"
   net.ipv4.ip_local_port_range = 1024 65000

TCP_keepalive
^^^^^^^^^^^^^^

According to `Using TCP keepalive to Detect Network Errors <http://www.gnugk.org/keepalive.html>`__ and `TCP Keepalive HOWTO <https://tldp.org/HOWTO/TCP-Keepalive-HOWTO/usingkeepalive.html>`__ some keepalive settings should be changed to improve reliability - it will enable keep alive functionality (checking if the connection is established and valid) and, by decreasing times and interval - will make detection of broken connections faster.

.. code:: sh

   # sysctl -w net.ipv4.tcp_keepalive_time="60"
   net.ipv4.tcp_keepalive_time = 60
   # sysctl -w net.ipv4.tcp_keepalive_probes="3"
   net.ipv4.tcp_keepalive_probes = 3
   # sysctl -w net.ipv4.tcp_keepalive_intvl="90"
   net.ipv4.tcp_keepalive_intvl = 90
   # sysctl -w net.ipv4.tcp_retries2=4
   net.ipv4.tcp_retries2 = 4


/etc/sysctl.conf
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The above commands let the system remember new settings until the next system restart. If you want to make the change permanent you have to edit the file: ``/etc/sysctl.conf`` and add the property at the end of the file:

.. code:: sh

   fs.file-max=360000
   net.ipv4.ip_local_port_range=1024 65000
   net.ipv4.tcp_keepalive_time=60
   net.ipv4.tcp_keepalive_probes=3
   net.ipv4.tcp_keepalive_intvl=90
   net.ipv4.tcp_retries2=4

It will be automatically loaded next time you start the server.

Command:

.. code:: sh

   # sysctl -p

Causes the ``/etc/systcl.conf`` to be reloaded which is useful when you have added more parameters to the file and don’t want to restart the server.

nofile
^^^^^^^^^^^^^^

This is the property used by the system limits. For example running the command ``ulimit -a`` shows you all limits set for the current user:

.. code:: sh

   # ulimit -a
   core file size          (blocks, -c) 0
   data seg size           (kbytes, -d) unlimited
   file size               (blocks, -f) unlimited
   pending signals                 (-i) 38912
   max locked memory       (kbytes, -l) 32
   max memory size         (kbytes, -m) unlimited
   open files                      (-n) 40960
   pipe size            (512 bytes, -p) 8
   POSIX message queues     (bytes, -q) 819200
   stack size              (kbytes, -s) 8192
   cpu time               (seconds, -t) unlimited
   max user processes              (-u) 38912
   virtual memory          (kbytes, -v) unlimited
   file locks                      (-x) unlimited

To make it even more interesting and more complex, there are 2 types of system limits: **soft limit** which can be temporarily exceeded by the user and **hard limit** which can not be exceeded. To see your **hard limit** execute command:

.. code:: sh

   # ulimit -a -H
   core file size          (blocks, -c) unlimited
   data seg size           (kbytes, -d) unlimited
   file size               (blocks, -f) unlimited
   pending signals                 (-i) 38912
   max locked memory       (kbytes, -l) 32
   max memory size         (kbytes, -m) unlimited
   open files                      (-n) 40960
   pipe size            (512 bytes, -p) 8
   POSIX message queues     (bytes, -q) 819200
   stack size              (kbytes, -s) unlimited
   cpu time               (seconds, -t) unlimited
   max user processes              (-u) 38912
   virtual memory          (kbytes, -v) unlimited
   file locks                      (-x) unlimited

The hard limits are usually bigger then the soft limits or sometimes the same.

For us the most important parameter is: **open files**. You can change the property in file: ``/etc/security/limits.conf``. You have to append 2 following lines to the end of the file:

.. code:: sh

   jabber               soft    nofile         350000
   jabber               hard    nofile         350000

Where the ``jabber`` is the user name of the account running you IM service. You can also set the limits for all users on the machine in a following way:

.. code:: sh

   *               soft    nofile         350000
   *               hard    nofile         350000

For those changes to make an effect you have to logout from the modified account and login again. New limits should be applied.


su and init script
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If one intends to use init scripts for startup purposes (or simply wants to be able to start the server utilizing su command) it’s necessary to adjust PAM configuration by modifying /etc/pam.d/su file and uncomment following line:

.. code:: sh

   session    required   pam_limits.so

Afterwards the init scripts will respect configured limits.

JVM settings and recommendations
------------------------------------

Tigase configuration file ``tigase.conf`` (described in more detail in `??? <#manualconfig>`__) mentioned a couple of environmental variables which are related to the operation of the JVM. In this guide we would like to expound on those configuration options and provide hints for the optimal settings.

Settings included in the ``etc/tigase.conf`` are as follows:

.. code:: bash

   #GC="-XX:+UseBiasedLocking -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:NewRatio=2 -XX:+CMSIncrementalMode -XX:-ReduceInitialCardMarks -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly"
   #EX="-XX:+OptimizeStringConcat -XX:+DoEscapeAnalysis -XX:+UseNUMA"

   #GC_DEBUG=" -XX:+PrintTenuringDistribution -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -Xloggc:logs/jvm.log -verbose:gc "

   #PRODUCTION_HEAP_SETTINGS=" -Xms5G -Xmx5G " # heap memory settings must be adjusted on per deployment-base!
   JAVA_OPTIONS="${GC} ${GC_DEBUG} ${EX} ${ENC} ${DRV} ${JMX_REMOTE_IP} -server ${PRODUCTION_HEAP_SETTINGS} ${DNS_RESOLVER} ${INTERNAL_IP} ${EXTERNAL_IP}  -XX:MaxDirectMemorySize=128m "

And while this file utilizes bash variables, JVM configuration options can be used in the same manner on all operating systems.

The guide will consists of two main parts - memory settings and Garbage Collector tweaks descriptions and hints.

We recommend using ``-server`` JVM parameter in all cases.

Heap Sizing
^^^^^^^^^^^^^^^^

For the non-production deployments (development or stating environments) we recommend using default memory settings of the JVM (which depends on the underlaying operating system), which result i automatic memory allocation and, by the rule of thumb - are the safest in such environments.

For the production environments we recommend a fixed size HEAP - both initial and maximum size, which can be set with (respectively)``-Xms`` and ``-Xmx`` JVM flags - ideally to the same value (which should be roughly 95% of the available memory, if Tigase will be the only service on the machine) to avoid allocation and deallocation.

For convenience it’s possible to uncomment line with ``PRODUCTION_HEAP_SETTINGS`` and adjust parameters accordingly.

Memory consideration - total usage
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The HEAP size is not the only thing that affects JVM memory usage. When trying to size accordingly for your usage and machine specification you have to consider other factors that count towards total: loaded classes, threads' stack, JIT code cache, garbage collector and others. In principle consider following equation:

::

   Maximum memory usage = [-Xmx] + [-XX:MaxMetaspaceSize] + number_of_threads * [-Xss] + [-XX:MaxDirectMemorySize]
                          (heap)   (classes)                (threads' stack)             (direct memory)


.. **Note**::

   before Java8 memory dedicated to loaded classes was configured with ``-XX:PermSize`` and ``-XX:MaxPermSize`` instead of, respectively, ``-XX:MetaspaceSize`` and ``-XX:MaxMetaspaceSize``

In case of Tigase XMPP Server, apart from heap we limit remaining factors:

-  direct memory to **128** MB

-  loaded classes to **128** MB

-  single thread’s stack size to **228** KB (number of threads depends on number of CPU cores and may vary from 500 to couple of thousands)

In principle, in addition to HEAP’s maximum size defined by ``-Xmx`` you should add roughly **512** MB

If you are interested in detailed tracking of memory take a look at [Memory footprint of the JVM](\ https://spring.io/blog/2019/03/11/memory-footprint-of-the-jvm/), [Native Memory Tracking in JVM](\ https://www.baeldung.com/native-memory-tracking-in-jvm) or [Why does my Java process consume more memory than Xmx?](\ https://plumbr.io/blog/memory-leaks/why-does-my-java-process-consume-more-memory-than-xmx)


GC settings
^^^^^^^^^^^^^^

Let’s start with stating that there is no "one to rule them all" - each deployment and use-case is different, however we will try to give a couple of pointers and recommendations proceed with short introduction to GC itself.

XMPP is quite specific in terms of memory allocation - short-lived objects (various types of stanzas) usually exceed number of long-lived objects (user connections and related data). This is important bit of information in the context of how usually JVM HEAP is organized and how Garbage Collector works. On the most basic level Heap is separated into couple of regions:

Generations
~~~~~~~~~~~~~~

-  **Young Generation**, which is further divided in to:

   -  **Eden** - the region when the objects are usually allocated when they are created;

   -  **Survivor Spaces** - (*to* and *from* - one of which is always empty) - responsible for storing all live object remaining after collecting **Young Generation** (process is repeated several times until objects are finally considered *old enough*);

-  **Old Generation** - (*Tenured Space*) - responsible for live objects remaining after running GC on **Survivor Spaces** - those would be *long-lived* objects (usually user connections and associated data);

Minor, Major and Full GC - optimizing
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

General thinking suggests that:

-  **Minor GC** cleans Young generation;

-  **Major GC** cleans Tenured space;

-  **Full GC** cleans all heap.

However, while we can certainly state that Minor GC cleans Young generation it’s a bit more difficult to differentiate Major and Full GC, especially considering that Major GC can be quite often triggered by Minor GC and some garbage collectors can perform cleaning concurrently. Instead of focusing of distinguishing phases one should pay closer attention to actual operations of Garbage Collector itself - uncommenting the line ``GC_DEBUG=" -XX:+PrintTenuringDistribution -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -Xloggc:logs/jvm.log -verbose:gc "`` in ``etc/tigase.conf`` (or adding same properties to the java commandline) and subsequently analyzing the results should prove more helpful. In addition monitoring GC operation using for example VisualVM (with VisualGC plugin) will also be helpful.

Settings for XMPP
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Ideally we should limit both number of GC pauses as well as their duration. After running rather tests following conclusions were made:

-  Garbage Collection is the faster the more dead objects occupies given space, therefore on high-traffic installation it’s better to have rather large YoungGen resulting in lower promotion of the objects to the OldGen;

-  with JVM8 default sizing of Young / Old generation changed, even tho NewRatio is still defaulting to “2” - setting it explicitly to "2" brought back previous sizing;

-  Concurrent Mark and Sweep (CMS) enabled (applies to Tenured space only) with explicit configuration of NewRatio set to default value of 2 (i.e. ``-XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:NewRatio=2``) in general behaves best;

-  For small installations (few core CPU, less memory) with low traffic default Parallel collector may be a better solution;

-  Using Heap size adjusted to the actual usage is better as the larger the heap the larger are spaces over which collection needs to be performed thus resulting in longer pauses; in case of huge heaps G1 collector may be better solution to avoid longer pauses;

Considering all of the above using following options should be a good starting point toward further optimizing of Garbage Collection:

``GC="-XX:+UseBiasedLocking -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:+CMSIncrementalMode -XX:-ReduceInitialCardMarks -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly"``

GC settings worth considering
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In addition to the general recommendation to use CMS collector, following options (or changes to the options) may be worth considering:

-  ``-XX:NewRatio=2`` - defines the ratio between the young and tenured generation is 1:2. In other words, the combined size of the eden and survivor spaces will be one-third of the total heap size. The parameters NewSize and MaxNewSize bound the young generation size from below and above. Setting these to the same value fixes the young generation, just as setting -Xms and -Xmx to the same value fixes the total heap size.

-  ``-XX:CMSInitiatingOccupancyFraction=percent`` - sets the percentage of the old generation occupancy (0 to 100) at which to start a CMS collection cycle.

-  ``-XX:+UseCMSInitiatingOccupancyOnly`` - instructs the JVM not to base its decision when to start a CMS cycle on run time statistics but instead it uses the value of CMSInitiatingOccupancyFraction for every CMS cycle.

-  ``-XX:ParallelGCThreads=x`` - sets the number of threads used for parallel garbage collection in the young and old generations. The default value depends on the number of CPUs available to the JVM. If the Tigase JMV is the only one running on the installation default value is recommended.

-  ``-XX:ConcGCThreads=x`` - sets the number of threads used for concurrent GC. The default value depends on the number of CPUs available to the JVM. If the Tigase JMV is the only one running on the installation default value is recommended.

-  ``-XX:+UseBiasedLocking`` and ``-XX:+DoEscapeAnalysis`` - designed to eliminate locking overhead, however their effect on performance is unpredictable therefore testing is required; reduced locking should improve concurrency and, on current multi-core hardware, improve throughput.

-  ``-XX:+OptimizeStringConcat`` - enables the optimization of String concatenation operations. This option is enabled by default.

-  ``-XX:+UseNUMA`` - enables performance optimization of an application on a machine with nonuniform memory architecture (NUMA - most modern computers are based on NUMA architecture) by increasing the application’s use of lower latency memory. By default, this option is disabled and no optimization for NUMA is made. The option is only available when the parallel garbage collector is used (-XX:+UseParallelGC).

-  ``-XX:-UseCompressedOops`` — disables the use of compressed pointers. By default, this option is enabled, and compressed pointers are used when Java heap sizes are less than 32 GB. When this option is enabled, object references are represented as 32-bit offsets instead of 64-bit pointers, which typically increases performance when running the application with Java heap sizes less than 32 GB. This option works only for 64-bit JVMs.


What to use with Machine x, y, z?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Server class machine (non-VM), > 16GB, >= 8 core CPU
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

For such setup enabling CMS garbage collector is recommended. Depending on the traffic usage and particular use-case adjusting NewRatio may be needed. Adjusting Xms and Xms sizes for actual available memory is needed (or better yet, for the actual traffic!). Following should be used:

.. code:: bash

   GC="-XX:+UseBiasedLocking -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:NewRatio=2 -XX:+CMSIncrementalMode -XX:-ReduceInitialCardMarks -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly"
   EX="-XX:+OptimizeStringConcat -XX:+DoEscapeAnalysis -XX:+UseNUMA"

   #GC_DEBUG=" -XX:+PrintTenuringDistribution -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -Xloggc:logs/jvm.log -verbose:gc "

   PRODUCTION_HEAP_SETTINGS=" -Xms15G -Xmx15G " # heap memory settings must be adjusted on per deployment-base!
   JAVA_OPTIONS="${GC} ${GC_DEBUG} ${EX} ${ENC} ${DRV} ${JMX_REMOTE_IP} -server ${PRODUCTION_HEAP_SETTINGS} ${DNS_RESOLVER} ${INTERNAL_IP} ${EXTERNAL_IP}  -XX:MaxDirectMemorySize=128m "

For installation with lot of available memory and intention to utilize it all, using G1GC collector may be a better idea :

.. code:: bash

   GC="-XX:+UseG1GC -XX:ConcGCThreads=4 -XX:G1HeapRegionSize=2 -XX:InitiatingHeapOccupancyPercent=35 -XX:MaxGCPauseMillis=100"
   EX="-XX:+OptimizeStringConcat -XX:+DoEscapeAnalysis -XX:+UseNUMA"

   #GC_DEBUG=" -XX:+PrintTenuringDistribution -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -Xloggc:logs/jvm.log -verbose:gc "

   PRODUCTION_HEAP_SETTINGS=" -Xms60G -Xmx60G " # heap memory settings must be adjusted on per deployment-base!
   JAVA_OPTIONS="${GC} ${GC_DEBUG} ${EX} ${ENC} ${DRV} ${JMX_REMOTE_IP} -server ${PRODUCTION_HEAP_SETTINGS} ${DNS_RESOLVER} ${INTERNAL_IP} ${EXTERNAL_IP}  -XX:MaxDirectMemorySize=128m "


VM machine, 8GB of RAM, 4 core CPU equivalent
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

For such setup enabling CMS garbage collector is also recommended. Depending on the traffic usage and particular use-case adjusting NewRatio may be needed (and configuring NewRatio is a must!). Adjusting Xms and Xms sizes for actual available memory is needed (or better yet, for the actual traffic!). Following should be used:

.. code:: bash

   GC="-XX:+UseBiasedLocking -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:NewRatio=2 -XX:+CMSIncrementalMode -XX:-ReduceInitialCardMarks -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly"
   EX="-XX:+OptimizeStringConcat -XX:+DoEscapeAnalysis -XX:+UseNUMA"

   #GC_DEBUG=" -XX:+PrintTenuringDistribution -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -Xloggc:logs/jvm.log -verbose:gc "

   PRODUCTION_HEAP_SETTINGS=" -Xms7G -Xmx7G " # heap memory settings must be adjusted on per deployment-base!
   JAVA_OPTIONS="${GC} ${GC_DEBUG} ${EX} ${ENC} ${DRV} ${JMX_REMOTE_IP} -server ${PRODUCTION_HEAP_SETTINGS} ${DNS_RESOLVER} ${INTERNAL_IP} ${EXTERNAL_IP}  -XX:MaxDirectMemorySize=128m "


VM machine with 4GB or less of RAM, and less than 4 core CPU equivalent
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Small installations with limited resources could operate better with default (for JVM versions up to 8, which is the most current at the moment of the writing). Again - depending on the traffic usage and particular use-case adjusting NewRatio may be needed. Adjusting Xms and Xms sizes for actual available memory is recommended (or better yet, for the actual traffic!). Following should be used (i.e. ``GC`` line should be commented so the defaults will be used):

.. code:: bash

   #GC="-XX:+UseBiasedLocking -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:NewRatio=2 -XX:+CMSIncrementalMode -XX:-ReduceInitialCardMarks -XX:CMSInitiatingOccupancyFraction=70 -XX:+UseCMSInitiatingOccupancyOnly"
   EX="-XX:+OptimizeStringConcat -XX:+DoEscapeAnalysis -XX:+UseNUMA"

   #GC_DEBUG=" -XX:+PrintTenuringDistribution -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -Xloggc:logs/jvm.log -verbose:gc "

   PRODUCTION_HEAP_SETTINGS=" -Xms3G -Xmx3G " # heap memory settings must be adjusted on per deployment-base!
   JAVA_OPTIONS="${GC} ${GC_DEBUG} ${EX} ${ENC} ${DRV} ${JMX_REMOTE_IP} -server ${PRODUCTION_HEAP_SETTINGS} ${DNS_RESOLVER} ${INTERNAL_IP} ${EXTERNAL_IP}  -XX:MaxDirectMemorySize=128m "


Additional resources
^^^^^^^^^^^^^^^^^^^^^^^^

-  `Sizing the Generations <https://docs.oracle.com/javase/8/docs/technotes/guides/vm/gctuning/sizing.html>`__

-  `About Java, parallel garbage collection and processor sets <http://www.c0t0d0s0.org/archives/6617-About-Java,-parallel-garbage-collection-and-processor-sets.html>`__

-  `GC Threads <http://hiroshiyamauchi.blogspot.cl/2009/12/gc-threads.html>`__

-  `GCViewer readme <https://github.com/chewiebug/GCViewer#readme>`__

-  `Java HotSpot™ Virtual Machine Performance Enhancements <http://docs.oracle.com/javase/7/docs/technotes/guides/vm/performance-enhancements-7.html>`__

-  `Java Garbage Collection handbook <https://plumbr.eu/java-garbage-collection-handbook>`__

-  Useful JVM Flags

   -  `Part 1 - JVM Types and Compiler Modes <https://blog.codecentric.de/en/2012/07/useful-jvm-flags-part-1-jvm-types-and-compiler-modes/>`__

   -  `Part 2 - Flag Categories and JIT Compiler Diagnostics) <https://blog.codecentric.de/en/2012/07/useful-jvm-flags-part-2-flag-categories-and-jit-compiler-diagnostics/>`__

   -  `Part 3 - Printing all XX Flags and their Values <https://blog.codecentric.de/en/2012/07/useful-jvm-flags-part-3-printing-all-xx-flags-and-their-values/>`__

   -  `Part 4 - Heap Tuning <https://blog.codecentric.de/en/2012/07/useful-jvm-flags-part-4-heap-tuning/>`__

   -  `Part 5 - Young Generation Garbage Collection <https://blog.codecentric.de/en/2012/08/useful-jvm-flags-part-5-young-generation-garbage-collection/>`__

   -  `Part 6 - Throughput Collector <https://blog.codecentric.de/en/2013/01/useful-jvm-flags-part-6-throughput-collector/>`__

   -  `Part 7 - CMS Collector <https://blog.codecentric.de/en/2013/10/useful-jvm-flags-part-7-cms-collector/>`__

   -  `Part 8 - GC Logging <https://blog.codecentric.de/en/2014/01/useful-jvm-flags-part-8-gc-logging/>`__

Session Manager
-----------------

Tigase Session Manager is where most of Tigase basic options can be configured, and where many operations are controlled from. Changes to session manager can effect operations throughout an entire XMPP installation, so care must be made when changing settings here.

.. _sessManMobileOpts:

Mobile Optimizations
^^^^^^^^^^^^^^^^^^^^^^

By default, Tigase employs XEP-0352 Client State Indication which allows for a more streamlined mobile experiencing by allowing the XMPP server to suppress or reduce the number of updates sent to a client thereby reducing the number of stanzas sent to a mobile client that is inactive. This employment is contained within the processor ``ClientStateIndication`` and is independent from the MobileV1, MobileV2, MobileV3 settings.

However, this can be fine tuned by using mobile plugins from Tigase which can be used at the same time by adding the following line to the ``config.tdsl`` file:

.. code:: properties

   }
   'sess-man' {
       'urn:xmpp:csi:0' {
           logic = 'tigase.xmpp.impl.MobileV1'
       }
   }

Logic Options are:

MobileV1
~~~~~~~~~~~~

Keeps all presence stanzas in queue until client is active.

::

   logic = 'tigase.xmpp.impl.MobileV1'


MobileV2
~~~~~~~~~~~~

This setting delays delivery of presences while client is in inactive state, but only keeps the last presence for each full jid. **This is the default setting for CSI logic**.

::

   logic = 'tigase.xmpp.impl.MobileV2'


MobileV3
~~~~~~~~~~~~

Keeps the same presence logic as MobileV2, but also queues Message Carbons. **Currently not supported by CSI processor, will cause issues**.

::

   logic = 'tigase.xmpp.impl.MobileV3'


Disabling CSI
~~~~~~~~~~~~~~~

If you wish to not use the ``ClientStateIndication`` processor, set the following in your ``config.tdsl`` file:

.. code:: dsl

   'sess-man' () {
       'urn:xmpp:csi:0' (active: false) {}
   }


A note about Mobile Plugins
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Previously, you could enable Mobile optimization logic using by enabling ``Mobile_V1 (){}`` bean to Session Manager: ``sess-man () {}`` bean.

If you have used these in the past, it is recommended you change your system to use the CSI processor with the appropriate mobile processing logic.

If you require v3 logic, or do not wish to use CSI, be sure to disable it using the above option.


threads-pool
^^^^^^^^^^^^^^^^

The ``threadsNo`` property allows you to fine-tune the SM plugin’s (processors) thread pool. With the default settings every plugin gets his own thread pool. This guarantees the best performance and optimal resource usage. The downside of this setting is that packets can arrive out of order if they are processed within different thread pools.

We can even fine tune this packet processing. Let’s say you want most of the plugins to be executed within a single thread pool to preserve packet ordering for them, but for some selected plugins that should execute within separate thread pools to improve performance. Let’s say, authentication packets and user registration can be actually executed in a separate thread pools as we do not worry about an order for them. Users cannot send or receive anything else before they authenticates anyway. The solution is to specify a number of threads for the selected plugin. For example, setting a common thread pool for all plugins but registration and authentication can be done with following configuration:

.. code:: dsl

   'sess-man' () {
       'amp' () {
           threadsNo = 30
       }
       'presence-state' () {
           threadsNo = 27
       }
   }

This replaces the old ``--sm-threads-pool`` property, as well as specifying thread pools in ``--sm-plugins``.


Thread Pool factor
^^^^^^^^^^^^^^^^^^^^^^^

Session manager can control the number of available thread pools for each processor. By adding the following line to the ``config.tdsl`` file, the global thread pool can be increased by a specified factor:

.. code:: dsl

   'sess-man' () {
       'sm-threads-factor' = 3
   }

In this case, the global thread pools is increased by a factor or 3.


Strategy
^^^^^^^^^^^^^^^^^^^^^^^

The ``Strategy`` property allows users to specify Clustering Strategy class which should be used for handling clustering environment; by default ``SMNonCachingAllNodes`` is used.

Any class implementing ``tigase.cluster.strategy.ClusteringStrategyIfc`` interface may be used for this setting.

Example:

.. code:: dsl

   'sess-man' () {
       strategy (class: tigase.cluster.strategy.SMCachingAllNodes)
   }

This replaces the old ``--sm-cluster-strategy-class`` setting from v7.1.

Virtual Hosts in Tigase Server
----------------------------------

Tigase server supports multiple virtual hosts in a single server installation. Virtual hosts can be added or removed, enabled or disabled during runtime without restarting the service or disrupting normal operation.

This document describes how virtual hosts work in Tigase server and how to get the most out of this feature in your installation.

The `'default-virtual-host' <#virtHosts>`_ property allows to define name of the single vhost domain which will be considered a default vhost domain for this installation. It allows you just to configure the domain name. Any additional configuration needs to be configured using ad-hoc commands.

Virtual hosts should be managed using ad-hoc commands or admin ui, visit `Add and Manage Domains <#addManageDomain>` for description of vhosts management process or visit `Specification for ad-hoc Commands Used to Manage Virtual Domains <#adhocCommands>` for more information about ad-hoc commands.

If you have components that may not be able to handle multiple vhosts or cluster mode, we have developed a virtual component solution as well, details in the `Virtual Components for the Tigase Cluster <#virtualComponents>`__ section.

You may also want to reference the Vhosts API for additional information: - `API Description for Virtual Domains Management in Tigase Server <#addManageDomain>`.

.. **Tip**::

   If you have a Tigase XMPP Server running in the cluster mode hidden behind some load balancer, or if internal IP or hostname of cluster nodes differ from the DNS name under which it is available from the internet, we would suggest setting a property ``installation-dns-address`` of ``vhost-man`` component to the DNS name which allows you to connect to all cluster nodes (ie. to the DNS name of the load balancer). This will allow Tigase XMPP Server to do proper DNS lookups to verify that DNS domain name of the virtual host which you will try to add or update points to your XMPP installation.


Default VHost configuration
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

It’s possible to specify initial default configuration for all Virtual Host in TDSL configuration file (i.e. ``etc/config.tdsl``) for selected parameters. To do so you should specify each configuration option within ``defaults`` bean belonging to ``vhost-man`` bean:

::

   'vhost-man' () {
       'defaults' () {
           'domain-filter-policy' = null
           's2s-secret' = null
           trusted = null
           'vhost-disable-dns-check' = false
           'vhost-max-users' = 0L
           'vhost-message-forward-jid' = null
           'vhost-presence-forward-jid' = null
           'vhost-register-enabled' = true
           'vhost-tls-required' = false
       }
   }

After initial definition of default configuration or after first startup of Tigase XMPP Server it is possible to configure Virtual Host defaults using ad-hoc commands by modifying values for ``default`` using ad-hoc as described in `Specification for ad-hoc Commands Used to Manage Virtual Domains <#adhocCommands>`__.

Alternatively, you may edit default Virtual Host configuration (configuration for domain ``default``) using Admin UI which by default is available at ``http://localhost:8080/admin/``.

.. _adhocCommands:

Specification for ad-hoc Commands Used to Manage Virtual Domains
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

There are 3 ad-hoc commands for virtual domains management in the Tigase server:

1. ``VHOSTS_RELOAD`` used to reload virtual domains list from the repository (database).

2. ``VHOSTS_UPDATE`` used to add a new virtual domain or update information for existing one.

3. ``VHOSTS_REMOVE`` used to remove an existing virtual host from the running server.

Syntax of the commands follows the specification described in `XEP-0050 <http://xmpp.org/extensions/xep-0050.html>`__. Extra information required to complete the command is carried as data forms described in `XEP-0004 <http://xmpp.org/extensions/xep-0004.html>`__.

All commands are accepted by the server only when send by the installation administrator. If the command is sent from any other account ``<not-authorized />`` error is returned. To grant administrator rights to an account you have to set ``admins`` property in the ``config.tdsl`` configuration file.

Commands are sent to the 'vhost-man' server component and the 'to' attribute of the stanza must contain a full JID of the VHostManager on the server. The full **JID** consists of the component name: 'vhost-man' and the local domain, that is domain which is already on the list of virtual domains and is active. Assuming 'existing.domain.com' one of domains already activated for the server installation the **JID** is: 'vhost-man@existing.domain.com'.

Reloading the Domains List from the Database
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In order to reload virtual domains from the permanent repository other than configuration file, you have to send ``VHOSTS_RELOAD`` ad-hoc command to the VHostManager on the server.

The reload command request is of the form:

.. code:: xml

   <iq type="set"
       to="vhost-man@existing.domain.com"
       id="aac8a">
     <command xmlns="http://jabber.org/protocol/commands"
              node="VHOSTS_RELOAD" />
   </iq>

The server sends a response upon successful completion of the command with current number of virtual domains server by the installation:

.. code:: xml

   <iq from="vhost-man@existing.domain.com"
       type="result"
       to="cmd-sender-admin@existing.domain.com"
       id="aac8a">
     <command xmlns="http://jabber.org/protocol/commands"
              status="completed"
              node="VHOSTS_RELOAD">
       <x xmlns="jabber:x:data" type="result">
         <field type="fixed" var="Note">
           <value>Current number of VHosts: 123</value>
         </field>
       </x>
     </command>
   </iq>

If the command is sent from an account other than admin, the server returns an error:

.. code:: xml

   <iq from="vhost-man@existing.domain.com"
       type="error"
       to="cmd-sender-admin@existing.domain.com"
       id="aac8a">
     <command xmlns="http://jabber.org/protocol/commands"
              node="VHOSTS_RELOAD" />
     <error type="auth" code="401">
       <not-authorized xmlns="urn:ietf:params:xml:ns:xmpp-stanzas" />
       <text xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"
             xml:lang="en">
         You are not authorized for this action.
       </text>
     </error>
   </iq>

The response doesn’t have any special meaning other then end-user information. The client may ignore the response as it is sent after the command has been executed.

Adding a New Domain or Updating Existing One
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In order to add a new domain or update existing one you have to send an ad-hoc command ``VHOSTS_UPDATE`` with at least one domain name in the command data form. You can also specify whether the domain is enabled or disabled but this is optional. Future releases may allow for setting additional parameters for the domain: maximum number of user accounts for this domain, anonymous login enabled/disabled for the domain, registration via XMPP enabled/disabled for this domain and some more parameters not specified yet.

The domain add/update command request is of the form:

.. code:: xml

   <iq type="set"
       to="vhost-man@existing.domain.com"
       id="aacba">
     <command xmlns="http://jabber.org/protocol/commands"
              node="VHOSTS_UPDATE">
       <x xmlns="jabber:x:data" type="submit">
         <field type="text-single"
                var="VHost">
           <value>new-virt.domain.com</value>
         </field>
         <field type="list-single"
                var="Enabled">
           <value>true</value>
         </field>
       </x>
     </command>
   </iq>

Please note: Character case in the command field variable names does matter.

Upon successful completion of the command the server sends a response back to the client with information of the existing number of virtual hosts on the server:

.. code:: xml

   <iq from="vhost-man@existing.domain.com"
       type="result"
       to="cmd-sender-admin@existing.domain.com"
       id="aacba">
     <command xmlns="http://jabber.org/protocol/commands"
              status="completed"
              node="VHOSTS_UPDATE">
       <x xmlns="jabber:x:data" type="result">
         <field type="fixed" var="Note">
           <value>Current number of VHosts: 124</value>
         </field>
       </x>
     </command>
   </iq>

Removing a Virtual Domain From the Server
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In order to remove a virtual domain you have to send ``VHOSTS_REMOVE`` command to the server with the domain name.

The domain remove command is sent by the client:

.. code:: xml

   <iq type="set"
       to="vhost-man@existing.domain.com"
       id="aacba">
     <command xmlns="http://jabber.org/protocol/commands"
              node="VHOSTS_REMOVE">
       <x xmlns="jabber:x:data" type="submit">
         <field type="text-single"
                var="VHost">
           <value>virt-nn.domain.com</value>
         </field>
       </x>
     </command>
   </iq>

Upon successful completion of the command the server sends a response back to the client with information of the existing number of virtual hosts on the server:

.. code:: bash

   <iq from="vhost-man@existing.domain.com"
       type="result"
       to="cmd-sender-admin@existing.domain.com"
       id="aacba">
     <command xmlns="http://jabber.org/protocol/commands"
              status="completed"
              node="VHOSTS_REMOVE">
       <x xmlns="jabber:x:data" type="result">
         <field type="fixed" var="Note">
           <value>Current number of VHosts: 124</value>
         </field>
       </x>
     </command>
   </iq>

.. _virtualComponents:

Virtual Components for the Cluster Mode
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Let’s assume you have a cluster installation and you want to include a component in your installation which doesn’t support the cluster mode yet. If you put it on all nodes as a separate instances they will work out of sync and overall functionality might be useless. If you put on one node only it will work correctly but it will be visible to users connected to this one node only.

Ideally you would like to have a mechanism to install it on one node and put some redirections on other nodes to forward all packets for this component to a node where this component is working. Redirection on it’s own is not enough because the component must be visible in service discovery list and must be visible somehow to users connected to all nodes.

This is where the virtual components are handy. They are visible to users as a local normal component, they seem to be a real local component but in fact they just forward all requests/packets to a cluster node where the real component is working.

Virtual component is a very lightweight ServerComponent implementation in Tigase server. It can pretend to be any kind of component and can redirect all packets to a given address. They can mimic native Tigase components as well as third-party components connected over external component protocol (XEP-0114).

Configuration is very simple and straightforward, in fact it is very similar to configuration of any Tigase component. You set a real component name as a name of the component and a virtual component class name to load. Let’s say we want to deploy MUC component this way. The MUC component is visible as ``muc.domain.oug`` in the installation. Thus the name of the component is: ``muc``

.. code:: dsl

   muc (class: tigase.cluster.VirtualComponent) {}

This is pretty much all you need to load a virtual component. A few other options are needed to point to correct destination addresses for packets forwarding and to set correct service discovery parameters:

.. code:: dsl

   }
   muc (class: tigase.cluster.VirtualComponent) {
       'disco-category' = 'conference'
       'disco-features' = 'http://jabber.org/protocol/muc'
       'disco-name' = 'Multi User Chat'
       'disco-node' = ''
       'disco-type' = 'text'
       'redirect-to' = 'muc@cluster-node-with-real-muc.domain.our'
   }

That’s it.

Settings for Custom Logging in Tigase
----------------------------------------

Logging can be an important tool to monitor your server’s health and performance. Logging may be controlled and customized on a per-component basis.

A ``logging`` bean has been implemented to allow more flexible configuration of logging in the Tigase XMPP Server.

Configuring logging
^^^^^^^^^^^^^^^^^^^^^^^

In the config file
~~~~~~~~~~~~~~~~~~~~~~~

Default logging configuration for your installation is kept in the config file and it may be adjusted there.

.. code:: config

   logging () {
       rootLevel = CONFIG
       'packet-debug-full' = true
       loggers = {
           'tigase.server' = {
               level = ALL
           }
           'tigase.conf' = {
               level = FINEST
           }
       }
       handlers = {
           ' java.util.logging.FileHandler' = {
               level = ALL
               append = true
               count = 5
               formatter = 'tigase.util.LogFormatter'
               limit = 10000000
               pattern = 'logs/tigase.log'
           }
           'java.util.logging.ConsoleHandler' = {
               level = WARNING
               formatter = 'tigase.util.LogFormatter'
           }
       }
   }

You only need to specify the settings you wish to customize, otherwise they will be left as default.

-  ``packet-debug-full`` - controls whether log entries should be obfuscated (all CData of all elements will be replaced by ``CData size: <length in bytes of the replaced string>``) or not; default: ``false``.

-  ``rootLevel`` - Defines the root level of logging for all components not otherwise defined. Default is CONFIG

-  ``loggers`` - Defines the level of logging for packages running in tigase server. This is similar to the --debug setting, however you must use ``tigase.{package}`` format. Default is NONE.

-  ``handlers`` - Defines the level of logging for File output and Console output.

   1. ``FileHandler`` - is the file output for log files, with the following options:

      a. ``level`` - specifies the level of logs to be written, default is ALL.

      b. ``append`` - whether to append to the log or replace it during restart. Default is true.

      c. ``count`` - number of individual log files to keep at set limit. Default is 5. (default settings will continue appending logs until 5 files at 10MB are reached, then the oldest file will be overwritten.)

      d. ``formatter`` - specifies the package to format logging output. Default is tigase.util.LogFormatter.

      e. ``limit`` - Byte limit for each log file. Default is 10000000 or 10MB.

      f. ``pattern`` - Directory and filename of the log file with respect to the Tigase installation directory. Default is logs/tigase.log.

   2. ``ConsoleHandler`` - Determines the formatting for Tigase output to console.

      a. ``level`` - specifies the level of logs to be written, default is WARNING.

      b. ``formatter`` - specifies the package to format logging output. Default is tigase.util.LogFormatter.


Disabling colored output
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If for some reason you don’t want colored output in the logs you can disable it by setting ``disable_logger_color`` to ``true``. For convenience, you can uncomment in ``etc/tigase.conf`` following line:

.. code:: bash

   #DISABLE_LOGGER_COLOR=" -Ddisable_logger_color=true "

Ad-hoc changes to the logging configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

It is also possible to use ad-hoc command named **Set package logging** with id ``logging-set`` available at ``message-router@domain`` (where domain is your server name) to reconfigure logging level of packets at runtime without requirement of restarting the Tigase XMPP Server.

   **Note**

   Those changes will be applied to this single cluster node.

.. **Note**::

   Those changes will not be saved in the Tigase XMPP Server configuration file and will be lost after restart of Tigase XMPP Server.

Using Admin UI

If your Tigase XMPP Server is running with HTTP server and with Admin UI enabled, then the easiest way to change logging configuration is by using Admin UI. After logging into web interface, open ``Configuration`` section and select ``Set package logging`` command. This will bring to you a form which you need to fill in with following fields:

-  ``Package name`` - should contain Java package or class name for which you wish to change logging level

-  ``Level`` - select a logging level you wish to apply to entered package name *(``OFF`` means that logging will be disabled)*

After pressing ``Submit`` your form will be passed to the server for validation and selected changes will be applied.


Using ad-hoc command

If you have access to the XMPP admin account of Tigase XMPP Server and XMPP client which supports ad-hoc command execution, you may connect with your XMPP client to the Tigase XMPP Server and look for adh-hoc commands available at ``message-router@domain`` (where domain is your server name). Within found ad-hoc commands you should find command named ``Set package logging`` or ``logging-set`` (that depends what your XMPP client is showing, id or name of the command) and you should execute it. Tigase XMPP Server will return a form which you need to fill in with following fields:

-  ``Package name`` - should contain Java package or class name for which you wish to change logging level

-  ``Level`` - select a logging level you wish to apply to entered package name *(``OFF`` means that logging will be disabled)*

After submitting the form, Tigase XMPP Server will validate your request and update logging configuration.


Using REST API

If you have Tigase XMPP Server with REST API enabled, you can use it for configuring logging of Tigase XMPP Server as well.

   **Note**

   As with all HTTP REST API requests you will require a valid API key and in this case a valid admin credentials to authenticate a HTTP request using Basic HTTP Authentication.

All you need to to is to send a HTTP POST request to ``/rest/adhoc/message-router@domain.com`` (where domain is your server name) with ``Contect-Type`` set to ``application/xml`` and a following XML as a payload to set logging level of ``tigase.server`` package to ``ALL``.

.. code:: xml

   <command>
     <node>logging-set</node>
     <fields>
       <item>
         <var>package-name</var>
         <value>tigase.server</value>
       </item>
       <item>
         <var>level</var>
         <value>ALL</value>
       </item>
     </fields>
   </command>

.. **Tip**::

   You can set logging levels also using JSON payload. For more details please refer to `Tigase HTTP API Guide <https://docs.tigase.net/index.html#collapse-tigase-http-api-master-snapshot-head:>`__, to section ``Executing example ad-hoc commands`` in ``REST API - usage examples``.

Alternate loggers in Tigase - Logback
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

It’s possible to use Logback for logging purposes, which offers certain interesting features (async logging, better control over log rotation, on the fly changing logging configuration)

Requirements: \* slf4j-api.jar (provided in ``-dist-max`` package) \* jul-to-slf4j.jar (provided in ``-dist-max`` package) \* desired logger libraries (for logback it’s ``logback-classic.jar`` and ``logback-core.jar`` (provided in -dist-max).

Configuration boils down to adding slf4j bridge handler to the list of build-in Java Logger handlers configuration, which in Tigase translates to adding following line to ``etc/config.tdsl``:

.. code:: bash

   logging () {
       rootHandlers = [ 'java.util.logging.ConsoleHandler', 'java.util.logging.FileHandler', 'org.slf4j.bridge.SLF4JBridgeHandler' ]
   }

After that ``etc/logback.xml`` configuration file will be used.

As stated in [jul-to-slf4j bridge documentation](\ http://www.slf4j.org/legacy.html#jul-to-slf4j) it’s essential to include ``LevelChangePropagator`` to eliminate translation overhead for disabled log statements:

.. code:: xml

   <configuration debug="true">
     <contextListener class="ch.qos.logback.classic.jul.LevelChangePropagator"/>
     ...
   </configuration>

NOTE, that it may be prudent to remove configuration of all old JUL logger by appending following to ``etc/logback.xml`` configuration:

.. code:: xml

   <configuration debug="true">
     <contextListener class="ch.qos.logback.classic.jul.LevelChangePropagator"/>
       <resetJUL>true</resetJUL>
   </configuration>

Tigase Advanced Options
-----------------------------

This section is designed to include a number of advanced configuration options available within Tigase, but may not have a relevant section yet to house them.

Using CAPTCHA for in-band registration
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To reduce false or spam registrations to Tigase XMPP Server, there is now the ability to add CAPTCHA forms as a part of the in-band registration. The CAPTCHA will generate a random math equation and ask the user registering a new account to answer it. This may be enabled as a sub-option of enabling registration in config.tdsl:

.. code:: dsl

   'sess-man' {
       'jabber:iq:register' {
           captchaRequired = 'true'
       }
   }

.. **Note**::

   Because some clients do not refresh a registration form after an unsuccessful attempt, this option allows 3 retries with the same CAPTCHA.

3 unsuccessful attempts will result in the captcha being invalidated and a client will receive an error message.


Enabling Empty Nicknames
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Tigase can now support users with empty nicknames. This can be enabled by adding the following code in config.tdsl.

.. code:: dsl

   'sess-man' {
       'jabber:iq:roster' {
           empty_name_enabled = 'true'
       }
   }


Enable Silent Ignore on Packets Delivered to Unavailable Resources
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

You can now have Tigase ignore packets delivered to unavailable resources to avoid having a packet bounce around and create unnecessary traffic. You may set this globally, within standard message handling only, or within the AMP component using the following settings:

Globally:

.. code:: dsl

   'sess-man' {
       'silently-ignore-message' = 'true'
   }

Message Processing Only:

.. code:: dsl

   'sess-man' {
       message {
           'silently-ignore-message' = 'true'
       }
   }

AMP Component:

.. code:: dsl

   'sess-man' {
       amp () {
           'silently-ignore-message' = 'true'
   }

Mechanism to count errors within Tigase
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A new processor within statistics has been added to count the number of errors that Tigase returns. This processor, named ``error-counter``, will count all errors returned by Tigase, however by default the number is always zero if it is not enabled. It can be found as an MBean object in JMX under ``ErrorStatistics`` and contains values for packets with ``ERROR`` and grouped by type. To enable counting of these errors, you must ensure the processor is included in your ``sess-man`` configuration:

.. code:: dsl

   'sess-man' {
       'error-counter' () {}
   }

Including stream errors
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Stream ``ERROR`` packets are not included in the above counter by default as they are processed separately. To enable this to be added to the counter, the following line must be in your ``config.tdsl`` file.

.. code:: dsl

   c2s {
       'stream-error-counter' () {
           active = true
       }
   }

Stream resumption default & max-timeout
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

``SteamManagementIOProcessor`` now has a setting that can be used to change the maximum timeout time it will wait for reconnection if a client does not send a time to wait. Two settings are now available:

.. code:: dsl

   c2s {
       'urn:xmpp:sm:3' {
           'resumption-timeout' = 90
       }
   }

The above setting in ``config.tdsl`` file will change the default timeout period to 90 seconds.

.. code:: dsl

   c2s {
       'urn:xmpp:sm:3' {
           'max-resumption-timeout' = 900
       }
   }

This setting will set the maximum time allowed for stream resumption to 900 seconds. This can be handy if you expect a number of mobile phones to connect to your server and want to avoid duplicate messages being sent back and forth.

Automatic subscription approval
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You may setup a server to automatically approve presence subscriptions or roster authorizations for all users. Say you were hosting bots and wanted to automate the process. This can be done with the following settings:

.. code:: dsl

   'sess-man' () {
       'jabber:iq:roster' {
           'auto-authorize' = 'true'
       }
       'presence-subscription' () {
           'auto-authorize' = 'true'
       }
   }

Both of these settings are false by default, and you may use them together or separately.

.. **Note**::

   ``presence-subscription`` is current default plugin. If you are using old ``presence`` then you should configure the option with correct plugin name.

The following behavior is followed when they are both activated:

-  Upon sending a subscription request - Both contacts will each others' subscription and be added to each others' roster. Presence information will immediately be exchanged between both parties.

-  Upon sending presence with type either unsubscribe or unsubscribed follows the rules defined in RFC regarding processing of these stanzas (i.e. adjusting subscription type of user/contact), but without forwarding those stanzas to the receiving entity to avoid any notifications to the client. However, a roster push is generated to reflect changes to presence in user roster in a seamless manner.

-  Simply adding an item to the roster (i.e. with <iq/> stanza with correct semantics) will also cause an automatic subscription between the user and the contact in a matter explained above.

Abuse Contacts
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Tigase has support for `XEP-0128: Service Discovery Extensions <https://xmpp.org/extensions/xep-0128.html>`__ for providing additional information to the server and component discovery information. One of the important usages for this feature is `XEP-0157: Contact Addresses for XMPP Services <https://xmpp.org/extensions/xep-0157.html>`__ which describes usage of this feature for providing contact information to server administrators or abuse response team.

To set abuse contact details you should set ``disco-extensions`` in property in ``etc/config.tdsl`` file with subproperty ``abuse-addresses`` set to your abuse address URI (for email you need to prefix it with ``mailto:`` and for XMPP address you need to prefix it with ``xmpp``):

.. code:: dsl

   'disco-extensions' = {
       'abuse-addresses' = [ 'mailto:abuse@localhost', 'xmpp:abuse@localhost' ]
   }

Push Notifications
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Tigase XMPP Server comes with support for `XEP-0357: Push Notifications <https://xmpp.org/extensions/xep-0357.html>`__ allowing user to receive notifications for messages received while his XMPP client is not connected enabled by default.


Disabling notifications

You can disable this feature with following settings:

.. code:: dsl

   'sess-man' {
       'urn:xmpp:push:0' (active: false) {}
   }

Removing body and sender from notifications

If you wish Tigase XMPP Server not to forward body of the message or sender details in the push notification you can disable that with following settings:

.. code:: dsl

   'sess-man' {
       'urn:xmpp:push:0' () {
           'with-body' = false
           'with-sender' = false
       }
   }

Overriding body of notifications

If you wish Tigase XMPP Server to override forward body of the encrypted message in the push notification (for example to avoid indicating that there is an "error") you can do that with following settings:

.. code:: dsl

   'sess-man' {
       'urn:xmpp:push:0' () {
           'encryptedMessageBody' = "You have a new secure message. Open to see the message"
       }
   }


Enabling push notifications for messages received when all resources are AWAY/XA/DND

Push notifications may also be sent by Tigase XMPP Server when new message is received and all resources of recipient are in AWAY/XA/DND state. To enable this type of notifications you need to enable additional push delivery extension named ``away`` in default push processor:

.. code:: dsl

   'sess-man' () {
       'urn:xmpp:push:0' () {
           'away' () {}
       }
   }

As this behaviour may not be expected by users and users need a compatible XMPP client to properly handle this notifications (XMPP client needs to retrieve message history from server to get actual message), in addition to enabling this plugin on the server, XMPP clients need to explicitly activate this feature. They can do that by including ``away`` attribute with value of ``true`` in push ``enable`` element send to the server, as in following example:

**Enabling Push notifications for away/xa/dnd account.**

.. code:: xml

   <iq type='set' id='x43'>
     <enable xmlns='urn:xmpp:push:0' away='true' jid='push-5.client.example' node='yxs32uqsflafdk3iuqo'>
       <x xmlns='jabber:x:data' type='submit'>
           ....
       </x>
     </enable>
   </iq>

If later on, user decides to disable notification for account in away/xa/dnd state, it may disable push notifications or once again send stanza to enable push notification but without ``away`` attribute being set:

.. code:: xml

   <iq type='set' id='x43'>
     <enable xmlns='urn:xmpp:push:0' away='true' jid='push-5.client.example' node='yxs32uqsflafdk3iuqo'>
       <x xmlns='jabber:x:data' type='submit'>
           ....
       </x>
     </enable>
   </iq>


