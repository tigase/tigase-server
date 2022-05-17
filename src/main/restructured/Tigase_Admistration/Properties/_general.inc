General
--------

admins
^^^^^^^^^

**Description:** Specifies a list of administrator accounts.

**Default value:** the administration account created when the server is setup. Typically it would be something like ``admins = ['admin@server.com']``.

**Example:** ``admins = [ 'admin@domain.com', 'user2@domain.com' ]``

**Possible values:** Comma seperated values of Bare JIDs.

**Available since:** 2.0.0

Certificate Container
^^^^^^^^^^^^^^^^^^^^^^^^^^^

The certificate container houses all configuration related to SSL certificate configuration. This container replaces a number of former — properties.

ssl-certs-location
^^^^^^^^^^^^^^^^^^^^^^^^^^^

This option allows you to specify the location where SSL certificates are stored. The meaning of this property depends on the SSL container `class implementation <#sslContainerClass>`__. By default it just points to the directory where the server SSL certificates are stored in files in PEM format.

Default location is ``/certs`` however it can be changed using the following setting:

.. code:: dsl

   }
   'certificate-container' {
       'ssl-certs-location' = '/etc/vhost-certs'
   }

This replaces the former ``--ssl-certs-location`` property.

ssl-def-cert-domain
^^^^^^^^^^^^^^^^^^^^^^^^^^^

This property allows you to specify a default alias/domain name for certificate. It is mostly used to load certificates for unknown domain names during the SSL negotiation. Unlike in TLS protocol where the domain name is known at the handshaking time, for SSL domain name is not known, therefore, the server does not know which certificate to use. Specifying a domain name in this property allows you to use a certificate for a specific domain in such case. This property value is also sometimes used if there is no certificate for one of virtual domains and the container does not automatically generate a self-signed certificate, then it can use a default one.

This may be configured as follows:

.. code:: dsl

   }
   'certificate-container' {
       'ssl-def-cert-domain' = 'some.domain.com'
   }

This replaces the former ``--ssl-def-cert-domain`` property.

Component
^^^^^^^^^^^^^^^^^^

**Description:** Container specifying component configuration. All components if they require configuration must be called in the conf.tdsl file in the following manner:

.. code:: dsl

   componentName (class: value) {
       <configuration>
   }

DSL allows for custom naming of the component, and specifying of the class in the same line. This method replaces the old ``comp-class`` and ``comp-name`` style of configuration.

For example, what used to be

.. code:: properties

   --comp-name-1 = socks5
   --comp-class-1 = tigase.socks5.Socks5Component
   --comp-name-2 = stun
   --comp-class-2 = tigase.stun.StunComponent

is now

.. code:: dsl

   socks5 (class: tigase.socks5.Socks5Component) {}
   stun (class: tigase.stun.StunComponent) {}

In fact, if you are using the default class & name for a component, you don’t need to specify it either, so MUC in this is now called by

.. code:: dsl

   socks5 () {}

**Default value:** By default, component configuration runs of default, and does not need to be specified.

There are many many configuration options under each component, which are specified in `component documentation <#loadComponent>`__.

Ports
^^^^^^^^^

The ports property is a subclass of connections, which is used to set a ports list for a connection manager. 'list of ports' is a comma separated list of ports numbers. For example for the server to server connection manager named s2s the property would like like the example below:

.. code:: dsl

   s2s {
       connections {
           ports = [ 5290, 5291 ]
       }
   }

Each port many be individually configured underneath ports

.. code:: dsl

   s2s {
       connections {
           ports = [ 5290, 5291 ]
           5291 {
               type = 'accept'
           }
       }
   }

this replaces the ``--cmpname-ports`` property.

**Available since:** 8.0.0

config-type
^^^^^^^^^^^^^^^^^^

**Description:** This property sets the server type and determines what components are started up without needing to declare and configure all components. Possible values are listed below:

-  ``setup`` - This setting will setup a basic server that is prepared for initial setup after unpacking. This is set by default, and starts up http component as well as basic server components. This should be changed after the server is configured.

-  ``default`` - creates default configuration file. That is configuration which is most likely needed for a typical installation. Components included in configuration are: session manager, client-to-server connection manager and server-to-server connection manager.

-  ``session-manager`` - creates configuration for instance with session manager and external component only. This is useful for distributed installation where you want to have session manager installed on separate machine and components managing network connections on different machines (one or more). Components included in configuration are: sm and ext2s.

-  ``connection-managers`` - creates configuration for instance with components managing network connections. This is useful for distributed installation where you want to have session manager installed on separate machine and components managing network connections on different machines (one or more). Components included in configuration are: c2s, s2s, ext2s.

-  ``component`` - generating a configuration with only one component - component managing external components connection, either XEP-0114 or XEP-0225. This is used to deploy a Tigase instance as external component connecting to the main server. You have to add more components handled by this instance, usually these are MUC, PubSub or any other custom components. You have to configure the external component connection, domain name, password, port, etc…​

**Default value:** ``'config-type' = 'setup'``

**Possible values:** ``setup``\ \|\ ``default``\ \|\ ``connection-managers``\ \|\ ``session-manager``\ \|\ ``connection-managers``\ \|\ ``component``

**Available since:** 2.0.0

debug-packages
^^^^^^^^^^^^^^^^^^

**Default value:** No default as Tigase does not expect custom classes out of the box.

**Example:** ``'debug-packages' = [ 'com.company.CustomPlugin' , 'com.company.custom' ]``

**Possible values:** comma separated list of Java packages or classes.

**Description:** This property is used to turn debugging on for any package not located within the default Tigase packages. Be sure class case is correct.

**Available since:** 5.0.0

debug
^^^^^^^^^

**Description:** The ``debug`` property is used to turn on the debug log for the specified Tigase package. For example if you want to turn debug logs on for the ``tigase.server`` package, then you have to use the ``server`` parameter. If you have any problems with your server the best way to get help from the Tigase team is to generate configuration with this enabled at a minimum and run the server. Then from the ``logs/tigase-console.log`` log file I can provide the best information for us to provide assistance. More details about server logging and adjusting logging level is described in the Debugging Tigase article in the admin guide. If you wish to debug packages not compiled with Tigase, use the `debug-packages <#debugPackages>`__ setting.

**Default value:** 'none'

**Example:** ``debug = [ 'server', 'xmpp.impl' ]``

**Possible values:** Comma separated list of Tigase’s package names.

**Available since:** 2.0.0

monitoring
^^^^^^^^^^^^^^^^^^

**Description:** This property activates monitoring interfaces through selected protocols on selected TCP/IP port numbers. For more details please refer to the `monitoring guide <#serverMonitoring>`__ in the user guide for details. Each monitoring protocol should be called in it’s own child bean under ``monitoring ()``. If a protocol is not specified, monitoring under that will not be available.

**Default value:** By default monitoring is disabled.

**Example:**

.. code:: dsl

   monitoring () {
       http () {
           port = '9080'
       }
       jmx () {
           port = '9050'
       }
       snmp () {
           port = '9060'
       }
   }

.. Warning::

    DO NOT CONFUSE monitoring with monitor component.

**Possible values:** 'list of monitoring protocols with port numbers.'

**Available since:** 8.0.0

plugins
^^^^^^^^^^^^^^^^^^

**Description:** The former ``--sm-plugins`` property has been replaced by a new style of formatting with DSL. The former long unbroken string of plusses and minuses have been replaced by a compartmentalized style. Plugins controlled under session manager will now be children of the ``'sess-man'`` bean. For example, to turn on the personal eventing protocol, the following may be used:

.. code:: dsl

   'sess-man' () {
       pep ()
   }

Should any plugin require configuration, those configurations will be under it’s own brackets. For example, this section not only turns on jabber:iq:auth but also sets the treads to 16.

.. code:: dsl

   'sess-man' () {
       'jabber:iq:auth' () {
           threadsNo = 16
       }
   }

As you may have noticed, beans or configuration options that require escape characters such as ``:`` or ``-`` will fall into single quotes to contain any special characters. If no special characters are in the bean name, then no single quotes are not required. If you need to disable certain plugins, you can do so after declaring the bean.

.. code:: dsl

   'sess-man' () {
       pep (active: false) {}
   }

Typically if a bean is called, it is automatically active. Session manager plugins will typically look like a list of plugins without configurations. The example section will show what one will look like.

**Default value:** 'none'

**Example:**

.. code:: dsl

   'sess-man' () {
       'version' () {}
       amp () {}
       'basic-filter' () {}
       'domain-filter' () {}
       'http:' {
            {
               'jabber.org' {
                   protocol {
                       commands () {}
                       stats () {}
                   }
               }
           }
       }
       'jabber:iq:auth' () {
           threadsNo = 16
       }
       'jabber:iq:privacy' () {}
       'jabber:iq:private' () {}
       'jabber:iq:register' () {}
       'jabber:iq:roster' () {}
       'message-archive-xep-0136' () {}
       msgoffline (active: false) {}
       pep () {}
       'presence-state' () {}
       'presence-subscription' () {}
       starttls () {}
       'urn:ietf:params:xml:ns:xmpp-bind' () {}
       'urn:ietf:params:xml:ns:xmpp-sasl' () {}
       'urn:ietf:params:xml:ns:xmpp-session' () {}
       'urn:xmpp:ping' () {}
       'vcard-temp' () {}
       zlib () {}
   }

**Possible values:** DSL format plugins list and configurations.

**Available since:** 8.0.0

priority-queue-implementation
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``tigase.util.PriorityQueueRelaxed``

**Example:** ``'priority-queue-implementation' = 'tigase.util.PriorityQueueStrict``

**Possible values:** class name extending ``tigase.util.PriorityQueueAbstract``.

**Description:** The ``priority-queue-implementation`` property sets Tigase’s internal queue implementation. You can choose between already available and ready to use or you can create own queue implementation and let Tigase load it instead of the default one. Currently following queue implementations are available:

1. **tigase.util.workqueue.PriorityQueueRelaxed** - specialized priority queue designed to efficiently handle very high load and prevent packets loss for higher priority queues. This means that sometimes, under the system overload packets may arrive out of order in cases when they could have been dropped. Packets loss (drops) can typically happen for the lowest priority packets (presences) under a very high load.

2. **tigase.util.workqueue.PriorityQueueStrict** - specialized priority queue designed to efficiently handle very high load but prefers packet loss over packet reordering. It is suitable for systems with a very high load where the packets order is the critical to proper system functioning. This means that the packets of the same priority with the same source and destination address are never reordered. Packets loss (drops) can typically happen for all packets with the same probability, depending which priority queue is overloaded.

3. **tigase.util.workqueue.NonpriorityQueue** - specialized non-priority queue. All packets are stored in a single physical collection, hence they are never reordered. Packets are not prioritized, hence system critical packets may have to wait for low priority packets to be processed. This may impact the server functioning and performance in many cases. Therefore this queue type should be chosen very carefully. Packets of the same type are never reordered. Packets loss (drops) can typically happen for all packets which do not fit into the single queue.

.. Note::

   *Since the packets are processed by plugins in the SessionManager component and each plugin has own thread-pool with own queues packet reordering may happen regardless what queue type you set. The reordering may only happen, however between different packet types. That is 'message' may take over 'iq' packet or 'iq' packet may take over 'presence' packet and so on…​ This is unpredictable.*

**Available since:** 5.1.0

roster-implementation
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``RosterFlat.class.getCanonicalName()``

**Example:** ``'roster-implementation' = 'my.pack.CustomRosterImpl'``

**Possible values:** Class extending tigase.xmpp.impl.roster.RosterAbstract.

**Description:** The ``roster-implementation`` property allows you to specify a different RosterAbstract implementation. This might be useful for a customized roster storage, extended roster content, or in some cases for some custom logic for certain roster elements.

**Available since:** 5.2.0

s2s-secret
^^^^^^^^^^^^^^^^^^

**Default value:** ``none``

**Example:**

.. code:: dsl

   'vhost-man' {
       defaults {
           's2s-secret' = 'some-s2s-secret'
       }
   }

**Possible values:** 'ascii string.'

**Description:** This property is a global setting for s2s secrets to generate dialback keys on the Tigase installation. By default it is null, which means the secret is automatically generated for each s2s connection and handshake.

This is a global property which is overridden by settings for each VHost (see `??? <#addManageDomain>`__)

As in the example provided, 'defaults' settings for all virtual hosts for which the configuration is not defined. This settings is useful mostly for installations with many virtual hosts listed in the init.property file for which there is no individual settings specified. It allows to configure a default values for all of them, instead of having to provide individual configuration for each vhost.

**Available since:** 5.2.0

scripts-dir
^^^^^^^^^^^^^^^^^^

**Default value:** ``scripts/admin``

**Example:** ``'scripts-dir' = ''/opt/admin-scripts'``

**Possible values:** path to a directory on the file system.

**Description:** This property sets the directory where all administrator scripts for ad-hoc commands are stored.

**Available since:** 4.3.0

ssl-container-class
^^^^^^^^^^^^^^^^^^^^^^^^^^^

*Default value:** ``tigase.io.SSLContextContainer``

**Example:** ``rootSslContextContainer (class: class.implementing.SSLContextContainerIFC) {}``

**Possible values:** a class implementing tigase.io.SSLContectContainerIfc.

**Description:** The ``rootSslContextContainer`` property allows you to specify a class implementing storage for SSL/TLS certificates. The class presented in the example to this description allows for loading certificates from PEM files which is a common storage used on many systems.

**Available since:** 5.0.0

stats
^^^^^^^^^

The stats block contains settings for statistics collection. To begin the stats block, use the following:

.. code:: dsl

   stats {}

**Default value:** 'By default, stats is not listed in the ``config.tdsl`` file'

**Description**

Tigase XMPP Server can store server statistics internally for a given period of time. This allows you to connect to a running system and collect all the server metrics along with historic data which are stored on the server. This is very useful when something happens on your production system you can connect and see when exactly this happened and what other metrics looked around this time. **Please be aware that Tigase XMPP Server produces about 1,000 different metrics of the system. Therefore caching large number of statistics sets requires lots of memory.**

stats-history-size
~~~~~~~~~~~~~~~~~~~~

Stats-history defines the size of the history buffer. That is how many complete sets of historic metrics to store in memory.

.. code:: dsl

   stats {
       'stats-history-size' = '2160'
   }

stats-history-interval
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Sets the interval for which statistics will be gathered from the server.

.. code:: dsl

   stats {
       'stats-history-interval' = '10'
   }


stats-logger
~~~~~~~~~~~~~~~~~~~~

Allow enabling and configuring components responsible for storing statistic information. Note that this controls the logging system for retrieving using JMX, clients, or ad-hoc commands.

.. code:: dsl

   stats {
       'stats-logger' (class: value) {
           <other settings>
       }
   }

Currently following classes are available:

-  ``tigase.stats.CounterDataArchivizer`` - every execution put current basic server metrics (CPU usage, memory usage, number of user connections, uptime) into database (overwrites previous entry)

-  ``tigase.stats.CounterDataLogger`` - every execution insert new row with new set of number of server statistics (CPU usage, memory usage, number of user connections per connector, number of processed packets of different types, uptime, etc) into the database

-  ``tigase.stats.CounterDataFileLogger`` - every execution store all server statistics into separate file.

frequency

stats-logger may also be controlled using frequency, which is the time interval between executions of the archiver ``.execute()`` method in seconds.

.. code:: dsl

   stats {
       'stats-logger' (class: tigase.stats.CounterDataLogger) {
           repository() {
               'default'() {
                   'data-source' = 'default';
               }
           }
           frequency = '60'
       }
   }


stats-file-logger
~~~~~~~~~~~~~~~~~~~~

This allows configuring of statistics gathering to an external file. This only has one class, and may be controlled independently from the internal statistics.

.. code:: dsl

   stats {
       'stats-file-logger' (class: tigase.stats.CounterDataFileLogger) {
           <other settings>
       }
   }


frequency
~~~~~~~~~~~~~~~~~~~~

stats-file-logger may also be controlled using frequency, which is the time interval between executions of the archiver ``.execute()`` method in seconds.

.. code:: dsl

   stats {
       'stats-file-logger' (class: tigase.stats.CounterDataFileLogger) {
           frequency = '60'
       }
   }


file configuration

You can customize the file output for stats-file-logger using the following setting options, these are all optional.

.. code:: dsl

   stats {
       'stats-history-size' = '2160'
       'stats-update-interval' = '10'
       'stats-file-logger' (class: tigase.stats.CounterDataFileLogger) {
           frequency = '60'
           'stats-datetime' = 'true'
           'stats-datetime-format' = 'HH:mm:ss'
           'stats-directory' = 'logs/server_statistics'
           'stats-filename' = 'stat'
           'stats-level' = 'FINEST'
           'stats-unixtime' = 'false'

-  **'stats-datetime'** - Whether to include date & time timestamp.

-  **'stats-datetime-format'** - Specifies the formatting of datetime timestamp.

-  **'stats-directory'** - The directory to which the statistics file should be saved.

-  **'stats-filename'** - The filename prefix to name the output statistics file.

-  **'stats-level'** - Sets the level of statistics to be gathered.

-  **'stats-unixtime'** - Control the format of the timestamp to use java DateFormat pattern.

which configures accordingly: directory to which files should be saved, filename prefix, whether to include or not unix timestamp in filename, whether to include or not datetime timestamp, control format of timestamp (using java DateFormat pattern) and also set level of the statistics we want to save (using java Logger.Level)

Database logger
^^^^^^^^^^^^^^^^^^^^^

This allows configuring of statistics gathering to a database. Without additional configuration ``default`` data source will be used but it’s possible to store statistics in any database - simply define new data source and configure logger with it’s name.

   **Note**

   After enabling the component it’s database schema should be loaded by executing ``./scripts/tigase.sh upgrade-schema etc/tigase.conf`` from the main Tigase directory

.. code:: dsl

   stats {
       'stats-logger' (class: tigase.stats.CounterDataLogger) {
           repository() {
               'default'() {
                   'data-source' = 'customDataSourceName';
               }
           }
           frequency = '60'
       }
   }


Example configuration block
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. code:: dsl

   stats {
       'stats-history-size' = '2160'
       'stats-update-interval' = '10'
       'stats-file-logger' (class: tigase.stats.CounterDataFileLogger) {
           frequency = '120'
           'stats-datetime' = 'false'
           'stats-datetime-format' = 'HH:mm:ss'
           'stats-directory' = 'logs/statistics'
           'stats-filename' = 'output'
           'stats-level' = 'WARNING'
           'stats-unixtime' = 'true'
       }
       'stats-logger' (class: tigase.stats.CounterDataLogger) {
           repository() {
               'default'() {
                   'data-source' = 'default';
               }
           }
           frequency = '60'
       }
   }

**Available since:** 8.0.0

stream-error-counter
^^^^^^^^^^^^^^^^^^^^^

**Description:** Add stream-error-counter to comma separated processors of components for which you wish to count the number of stream errors made. Without enabling this, statistics will return 0. This setting turns on stream-error-counter for both c2s and ws2s:

.. code:: dsl

   c2s {
       'stream-error-counter' () {
           active = true
       }
   }
   ws2s {
       'stream-error-counter' () {
           active = true
         }
   }

You may if you wish turn off stream error counters by setting ``active = false``.

**Default value:** Stream error counters are not turned on by default, thus no default value is set.

**Example:**

.. code:: dsl

   <component> {
       'stream-error-counter' () {
           active = true
       }

**Available since:** 7.1.0

stringprep-processor
^^^^^^^^^^^^^^^^^^^^^

**Description:** The ``'stringprep-processor'`` property sets the stringprep processor for all JIDs handled by Tigase. The default 'simple' implementation uses regular expressions to parse and check the user JID. Although it does not fulfill the RFC-3920 requirements, it also puts much less stress on the server CPU, hence impact on the performance is very low.

Other possible values are:

``'libidn'`` - provides full stringprep processing exactly as described in the RFC-3920. It requires lots of CPU power and significantly impacts performance.

``'empty'`` - doesn’t do anything to JIDs. JIDs are accepted in the form they are received. No impact on the performance and doesn’t use any CPU. This is suitable for use in automated systems where JIDs are generated by some algorithm, hence there is no way incorrect JIDs may enter the system.

**Default value:** ``simple``

**Example:** ``'stringprep-processor' = 'libidn'``

**Possible values:** ``simple|libidn|empty``

**Available since:** 8.0.0

test
^^^^^^^^^^^^^^^^^^^^^

*Default value:** By default test mode is disabled.

**Description:** This property sets the server for test mode, which means that all logging is turned off, offline message storage is off, and possibly some other changes to the system configuration are being made.

The idea behind this mode is to test Tigase XMPP Server with minimal performance impact from environment such as hard drive, database and others…​

Test function has been replaced by the following setting:

.. code:: dsl

   logging {
       rootLevel = 'WARNING'
   }

**Available since:** 8.0.0

tls-jdk-nss-bug-workaround-active
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``false``

**Example:** ``'tls-jdk-nss-bug-workaround-active' = true``

**Possible values:** ``true|false``

**Description:** This is a workaround for TLS/SSL bug in new JDK7 using the native library for keys generation and connection encryption used with new version of nss library.

This caused a number of problems with Tigase installed on systems with JDK7 and the new library installed, such as hanging connections, or broken SSL/TLS. Our earlier suggestion was to avoid using either JDK7 or the problematic native library. Now we have a proper fix/workaround which allows you to run Tigase with JDK7.

-  http://stackoverflow.com/q/10687200/427545

-  http://bugs.sun.com/bugdatabase/view_bug.do;jsessionid=b509d9cb5d8164d90e6731f5fc44?bug_id=6928796

Note, while this setting is still supported, the issues mentioned above are fixed in v8 JDK.

**Available since:** 8.0.0

trusted
^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``none``

**Example:** ``trusted = [ 'user@domain.com' , 'user-2@domain2.com' ]``

**Possible values:** comma separated list of user bare JIDs.

**Description:** The ``trusted`` property allows users to specify a list of accounts which are considered as trusted, thus whom can perform some specific actions on the server. They can execute some commands, send a broadcast message, set MOTD and so on. The configuration is similar to ```admins`` <#admins>`__ setting.

**Available since:** 8.0.0