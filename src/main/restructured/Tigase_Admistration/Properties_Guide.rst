Appendix II - Properties Guide
===============================

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

Repository
------------

**Description:** Container specifying authentication repository. This container replaces the old ``auth-db`` property types, and may contain some other configuration values.

**Default value:**

.. code:: dsl

   authRepository {
     <configuration>
   }

This is the basic setup for authRepository, where <configuration> settings are global for all authentication databases. However, you may configure multiple databases individually.

**Example:**

.. code:: dsl

   authRepository {
       'auth-repo-pool-size' = 50
       domain1.com () {
           cls = 'tigase.db.jdbc.JDBCRepository'
           'data-source' = 'domain1'
       }
       domain2.com () {
           cls = 'tigase.db.jdbc.JDBCRepository'
           'data-source' = 'domain2'
           'auth-repo-pool-size' = 30
       }
   }


**Configuration Values:**
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Container has the following options

pool-size
~~~~~~~~~~~~

This property sets the database connections pool size for the associated ``UserRepository``.

   **Note**

   in some cases instead of default for this property setting for ```data-repo-pool-size`` <#dataRepoPoolSize>`__ is used if pool-size is not defined in ``userRepository``. This depends on the repository implementation and the way it is initialized.

.. code:: dsl

   authRepository {
       default ()
         'pool-size' = 10
   }

This is a global property that may be overridden by individual repository settings:

.. code:: dsl

   userRepository {
       default () {
         'pool-size' = 10
       }
       special-repo () {
         'pool-size' = 30
       }
   }


**cls**
~~~~~~~~~~~~

| Defines the class used for repository connection. You can use this to specify specific drivers for different DB types.

Unless specified, the pool class will use the one included with Tigase. You may configure individual repositories in the same way. This replaces the former ``--auth-repo-pool`` property.

.. Note::

   File conversion will not remove and convert this property, it **MUST BE DONE MANUALLY**.

**Available since:** 8.0.0

authRepository
^^^^^^^^^^^^^^^^^

**Description:** Container specifying repository URIs. This container replaces the old ``auth-db-uri`` and ``user-db-uri`` property types.

**Default value:**

.. code:: dsl

   dataSource {
       default () {
           uri = 'jdbc:mysql://localhost/tigasedb?user=tigase&password=tigase12'
   }

Once your configuration is setup, you will see the uri of your user database here. If other databases need to be defined, they will be listed in the same dataSource bean.

**Example:**

.. code:: dsl

   dataSource {
       default () {
           uri = 'jdbc:mysql://localhost/tigasedb?user=tigase&password=tigase12'
       }
       'default-auth' () {
           uri = 'jdbc:mysql://localhost/tigasedbath?user=tigase&password=tigase12'
       }
   }

**Possible values:** Broken down list of customized names for DB URIs. Each name must have a defined uri property. DB name can be customized by the bean name.

.. Note::

   URI name may be used as shorthand to define DB location URI in other containers, so be sure to name them uniquely.

.. Note::

   default () URI setting replaces the ``user-db-uri`` as well as the ``auth-repo-uri`` property.

MSSQL
^^^^^^^^^

MSSql support works out of the box, however Tigase provides an open source driver for the database. We recommend using Microsoft’s own driver for better functionality.

.. code:: dsl

   dataSource () {
       default () {
           uri = 'jdbc:jtds:sqlserver://localhost;databaseName=tigasedb;user=tigase_user;password=tigase_pass;schema=dbo;lastUpdateCount=false;cacheMetaData=false'
       }
   }

Where the uri is divided as follows: jdbc:<driver>:sqlserver://<server address>;databaseName=<database name>;user=<username for db>;password=<password for db>;schema=dbo;lastUpdateCount=false;cacheMetaData=false We do not recommend modification of schema and onward unless you are explicit in your changes.

MongoDb
^^^^^^^^^

For using mongoDB as the repository, the setting will look slightly different:

.. code:: dsl

   dataSource () {
       default () {
           uri = 'mongodb://username:password@localhost/dbname'
       }
   }

MySQL
^^^^^^^^^

MySQL support works out of the box, however Tigase uses prepared calls for calling procedures accessing data stored in database. While this works very fast, it takes time during Tigase XMPP Server startup to prepare those prepared calls. Since version 8.2.0, it is possible to enable workaround which will force Tigase XMPP Server to use prepared statements instead of prepared calls, that will improve startup time but may have slight impact on performance during execution of queries and disables startup verification checking if stored procedures and function in database exist and have correct parameter types. To enable this mode you need to set ``useCallableMysqlWorkaround`` to ``true``.

.. code:: dsl

   dataSource {
       default () {
           uri = 'jdbc:mysql://localhost/tigasedb?user=tigase&password=tigase12'
           useCallableMysqlWorkaround = 'true'
       }
   }


pool-size
^^^^^^^^^^^^^^^^^^

``DataSource`` is an abstraction layer between any higher level data access repositories such as ``userRepository`` or ``authRepository`` and SQL database or JDBC driver to be more specific. Many implementations use ``DataSource`` for DB connections and in fact on many installations they also share the same DataRepository instance if they connect to the same DB. In this case it is desired to use a specific connection pool on this level to an avoid excessive number of connections to the database.

To do so, specify the number of number of database connection as an integer:

.. code:: dsl

   dataSource {
       default () {
           uri = 'jdbc:mysql://localhost/tigasedb?user=tigase&password=tigase12'
           'pool-size' = '50'
       }
   }

By default, the number of connections is 10.

**Available since:** 8.0.0

Cluster
-----------

cl-comp
^^^^^^^^^

**Description:** Container specifying cluster component configuration.

**Default value:** By default, the cl-comp container is not listed in the ``config.tdsl`` file.

**Example:**

.. code:: dsl

   'cl-comp' {
       <configuration>
   }

connect-all
~~~~~~~~~~~~~~~

The ``cluster-connect-all`` property is used to open active connections to all nodes listed in the `cluster-nodes <#clusterNodes>`__ configuration property. This property should be used only on the node which is added to the live cluster at later time. Normally this new cluster node is not listed in the configuration of the existing cluster nodes. This is why they can not open connections the new node. The new node opens connection to all existing nodes instead. False is the default value and you can skip this option if you want to have it switched off which it is by default.

**Example**

.. code:: dsl

   'cl-comp' {
       'connect-all' = true
   }

This replaces the ``--cluster-connect-all`` property.

**Available since:** 8.0.0

cluster-mode
^^^^^^^^^^^^^^^^^^

**Description:** The property is used to switch cluster mode on. The default value is ``false`` so you can normally skip the parameter if you don’t want the server to run in cluster mode. You can run the server in the cluster mode even if there is only one node running. The performance impact is insignificant and you will have the opportunity to connect mode cluster nodes at any time without restarting the server.

**Default value:** ``false`` Tigase by default does not run in clustered mode.

**Example:** ``'cluster-mode' = 'true'``

**Possible values:** ``true|false``

**Available since:** 8.0.0

cluster-nodes
^^^^^^^^^^^^^^^^^^

**Default value:** none

**Example:** ``'cluster-nodes' = [ 'node1.domain:pass:port' , 'node2.domain:pass:port' , 'node3.domain:pass:port' ]``

**Possible values:** a comma separated list of hostnames.

**Description:** The property is used to specify a list of cluster nodes running on your installation. The node is the full DNS name of the machine running the node. Please note the proper DNS configuration is critical for the cluster to work correctly. Make sure the 'hostname' command returns a full DNS name on each cluster node. Nodes don’t have to be in the same network although good network connectivity is also a critical element for an effective cluster performance.

All cluster nodes must be connected with each other to maintain user session synchronization and exchange packets between users connected to different nodes. Therefore each cluster node opens a 'cluster port' on which it is listening for connections from different cluster nodes. As there is only one connection between each two nodes Tigase server has to decide which nodes to connect to and which has to accept the connection. If you put the same list of cluster nodes in the configuration for all nodes this is not a problem. Tigase server has a way to find and void any conflicts that are found. If you however want to add a new node later on, without restarting and changing configuration on old nodes, there is no way the old nodes will try to establish a connection to the new node they don’t know them. To solve this particular case the next parameter is used.

.. Note::

   Cluster nodes are not required to be configured, as they can automatically find/add/remove cluster nodes. This is for installations where nodes will be limited and static!

**Available since:** 8.0.0

User connectivity
--------------------

bosh-close-connection
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``false``

**Example:** ``'bosh-close-connection' = true``

**Possible values:** ``true|false``

**Description:** This property globally disables Bosh keep-alive support for Tigase server. It causes the Bosh connection manager to force close the HTTP connection each time data is sent to the Bosh client. To continue communication the client must open a new HTTP connection.

This setting is rarely required but on installations where the client cannot control/disable keep-alive Bosh connections and keep-alive does not work correctly for some reason.

**Available since:** 8.0.0

bosh-extra-headers-file
^^^^^^^^^^^^^^^^^^^^^^^^^^^

*Default value:** ``'etc/bosh-extra-headers.txt'``

**Example:** ``'bosh-extra-headers-file' = ''/path/to/file.txt'``

**Possible values:** 'path to a file on the filesystem.'

**Description:** This property allows you to specify a path to a text file with additional HTTP headers which will be sent to a Bosh client with each request. This gives some extra flexibility for Bosh clients running on some systems with special requirements for the HTTP headers and some additional settings.

By default a file distributed with the installation contains following content:

.. code:: bash

   Access-Control-Allow-Origin: *
   Access-Control-Allow-Methods: GET, POST, OPTIONS
   Access-Control-Allow-Headers: Content-Type
   Access-Control-Max-Age: 86400

This can be modified, removed or replaced with a different content on your installation.

**Available since:** 8.0.0

client-access-policy-file
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``etc/client-access-policy.xml``

**Example:** ``'client-access-policy-file' = ''/path/to/access-policy-file.xml'``

**Possible values:** path to a file on the filesystem.

**Description:** The ``client-access-policy-file`` property allows control of the cross domain access policy for Silverlight based web applications. The cross domain policy is controlled via XML file which contains the policy and rules.

By default Tigase is distributed with an example policy file which allows for full access from all sources to the whole installation. This is generally okay for most Bosh server installations. The configuration through the property and XML files allows for a very easy and flexible modification of the policy on any installation.

**Available since:** 5.2.0

client-port-delay-listening
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Description:** The property allows to enabled or disable delaying of listening for client connections **in cluster mode** until the cluster is correctly connected.

**Default value:** ``true``

**Example:**

.. code:: dsl

   <component> {
       'port-delay-listening' = false
     }

**Possible values:** ``true``, ``false``

In cluster mode, in order to ensure correct user status broadcast, we are delaying opening client ports (components: ``c2s``, ``ws2s``, ``bosh``) and enable those only after cluster is fully and correctly connected (i.e. either there is only single node or in case of multiple nodes all nodes connected correctly).

It’s possible to enable/disable this on per-component basis with the following configuration:

.. code:: dsl

   bosh {
       'port-delay-listening' = true
   }
   c2s {
       'port-delay-listening' = true
   }
   ws2s {
       'port-delay-listening' = true
   }

Maximum delay time depends on the component and it’s multiplication of ``ConnectionManager`` default connection delay times ``30s`` - in case of client connection manager this delay equals 60s.

.. Note::

   Only applicable if **Cluster Mode** is active!

**Available since:** 7.1.0

cross-domain-policy-file
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``etc/cross-domain-policy.xml``

**Example:** ``'cross-domain-policy-file' = ''/path/to/cross-domain-policy.xml'``

**Possible values:** path to a file on the file system.

**Description:** This property allows you to set a path to a file with cross domain access policy for flash based clients. This is a standard XML file which is sent to the flash client upon request.

A default file distributed with Tigase installations allows for full access for all. This is good enough for most use cases but it can be changed by simply editing the file.

This is a global property that can also be overridden by configuring connection managers [ c2s, s2s, ws2s, bosh, ext, etc] and they may all have their own policies.

.. code:: dsl

   c2s {
       'cross-domain-policy-file' = '/path/to/cross-domain-policy.xml'
   }

**Available since:** 5.1.0

domain-filter-policy
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``ALL``

**Example:** ``domain-filter-policy' = 'LOCAL``

**Possible values:** ``ALL|LOCAL|OWN|BLOCK|LIST=domain1;domain2|BLACKLIST=domain1;domain2``

**Description:** The ``domain-filter-policy`` property is a global setting for setting communication filtering for vhosts. This function is kind of an extension of the same property which could be set on a single user level. However, in many cases it is desired to control users communication not on per user-level but on the domain level. Domain filtering (communication filtering) allows you to specify with whom users can communicate for a particular domain. It enables restriction of communications for a selected domain or for the entire installation. A default value ``ALL`` renders users for the domain (by default for all domains) able to communicate with any user on any other domains. Other possible values are:

1. ``ALL`` a default value allowing users to communicate with anybody on any other domain, including external servers.

2. ``LOCAL`` allows users to communicate with all users on the same installation on any domain. It only blocks communication with external servers.

3. ``OWN`` allows users to communicate with all other users on the same domain. Plus it allows users to communicate with subdomains such as **muc.domain**, **pubsub.domain**, etc…

4. ``BLOCK`` value completely blocks communication for the domain or for the user with anybody else. This could be used as a means to temporarily disable account or domain.

5. ``LIST`` property allows to set a list of domains (users' JIDs) with which users on the domain can communicate (i.e. *whitelist*).

6. ``BLACKLIST`` - user can communicate with everybody (like ``ALL``), except contacts on listed domains.

This is a global property which is overridden by settings for particular VHosts (see `??? <#addManageDomain>`__).

A default settings for all virtual hosts for which the configuration is not defined. This settings is useful mostly for installations with many virtual hosts listed in the init.property file for which there is no individual settings specified. It allows default value for all of servers, instead of having to provide individual configuration for each vhost.

``ALL`` is also applied as a default value for all new vhosts added at run-time.

**Available since:** 5.2.0

see-other-host
^^^^^^^^^^^^^^^^^^

--cmSeeOtherHost has been replaced with using ``seeOtherHost`` setting, and can be configured for each connection manager (c2s, s2s, etc..)

**Default value:** ``tigase.server.xmppclient.SeeOtherHostHashed``

**Example:**

.. code:: dsl

   <connectionManager> {
     seeOtherHost (class: value) { }
   }

**Possible values:** 'none' 'or class implementing SeeOtherHostIfc.'

**Description:** Allows you to specify a load balancing mechanism by specifying SeeOtherHostIfc implementation. More details about functionality and implementation details can be found in Tigase Load Balancing documentation.

**Available since:** 8.0.0

watchdog_timeout
^^^^^^^^^^^^^^^^^^

**Default value:** ``1740000``

**Example:** ``watchdog_timeout=60000``

**Possible values:** ``any integer.``

**Description:** The ``watchdog_timeout`` property allows for fine-tuning ConnectionManager Watchdog (service responsible for detecting broken connections and closing them). Timeout property relates to the amount of time (in miliseconds) after which lack of response/activity on a given connection will considered such connection as broken an close it. In addition to global configuration presented above a per component configuration is possible:

.. code:: dsl

   <ConnectionManager> {
       watchdog_timeout = 60000L
   }

for example (for C2SConnectionManager):

.. code:: dsl

   c2s {
       watchdog_timeout = 150000L
   }

All related configuration options:

-  `watchdog_Ping_Type <#watchdogPingType>`__

-  `watchdog_delay <#watchdogDelay>`__

-  watchdog_timeout

**Available since:** 8.0.0

watchdog_delay
^^^^^^^^^^^^^^^^^^

**Default value:** ``600000``

**Example:** ``watchdog_delay = '30000'``

**Possible values:** 'any integer.'

**Description:** ``watchdog_delay`` configuration property allows configuring delay (in milliseconds) between subsequent checks that ConnectionManager Watchdog (service responsible for detecting broken connections and closing them) will use to verify the connection. In addition to global configuration presented above a per component configuration is possible:

.. code:: dsl

   <ConnectionManager> {
     watchdog_delay = 60000L
   }

for example (for ClusterConnectionManager):

.. code:: dsl

   'cl-comp' {
       watchdog_delay = 150000L
   }

All related configuration options:

-  `watchdog_Ping_Type <#watchdogPingType>`__

-  watchdog_delay

-  `watchdog_timeout <#watchdogTimeout>`__

**Available since:** 8.0.0

watchdog_ping_type
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``whitespace``

**Example:** ``watchdog_ping_type = 'XMPP'``

**Possible values:** ``WHITESPACE``,\ ``XMPP``

**Description:** ``watchdog_ping_type`` configuration property allows configuring of the type of ping that ConnectionManager Watchdog (service responsible for detecting broken connections and closing them) will use to check the connection. In addition to global configuration presented above a per component configuration is possible:

.. code:: dsl

   <ConnectionManager> {
     watchdog_ping_type = 'XMPP'
   }

for example (for ClusterConnectionManager):

.. code:: dsl

   cl-comp {
       watchdog_ping_type = 'WHITESPACE'
   }

All related configuration options:

-  watchdog_ping_type

-  `watchdog_Delay <#watchdogDelay>`__

-  watchdog_timeout

**Available since:** 8.0.0

ws-allow-unmasked-frames
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``false``

**Example:** ``'ws-allow-unmasked-frames' = true``

**Possible values:** ``true|false``

**Description:** RFC 6455 specifies that all clients must mask frames that it sends to the server over Websocket connections. If unmasked frames are sent, regardless of any encryption, the server must close the connection. Some clients however, may not support masking frames, or you may wish to bypass this security measure for development purposes. This setting, when enabled true, will allow connections over websocket to be unmasked to the server, and may operate without Tigase closing that connection.

**Available since:** 8.0.0

External
---------------

bind-ext-hostnames
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** none

**Example:** ``'bind-ext-hostnames' = [ 'pubsub.host.domain' ]``

**Possible values:** comma separated list of domains.

**Description:** This property enables setting a list of domains to be bound to the external component connection. Let’s say we have a Tigase instance with only MUC and PubSub components loaded and we want to connect this instance to the main server via external component protocol. Using `--external property <#external>`__ we can define a domain (perhaps muc.devel.tigase.org), password, TCP/IP port, remote host address, connection type, etc…​ This would make one of our components (MUC) visible on the remote server.

To make the second component (PubSub) visible we would need to open another connection with the domain name (pubsub.devel.tigase.org) for the other component. Of course the second connection is redundant as all communication could go through a single connection. This is what this property is used. In our example with 2 components you can just put the 'pubsub.devel.tigase.org' domain as a value to this property and it will bind the second domain to a single connection on top of the domain which has been authenticated during protocol handshaking.

**Available since:** 5.0.0

default-virtual-host
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Description:** The ``default-virtual-host`` property allows setting of the name of default virtual host that is served by the installation. It is loaded during startup of the application and stored in the database. **It may only contain single domain name!**

Any additional configuration options or additional virtual hosts domains should be added and configured using ad-hoc commands such as ``Add new item``, ``Update item configuration`` and ``Remove an item`` available at the JID of the ``VHostManager`` component of your installation (``vhost-man@your-server-domain``).

**Available since:** 8.0.0

ext
^^^^^^^^^

**Description:** This property defines parameters for external component connections.

The component is loaded the same way as all other Tigase components. In your ``config.tdsl`` file you need to add the external class:

.. code:: dsl

   ext (class: tigase.server.ext.ComponentProtocol) {}

This will load the component with an empty configuration and is practically useless. You have to tell the component on what port to listen to (or on what port to connect to) and external domains list with passwords.

Those values need to be configured while the Tigase XMPP Server is running using XMPP ad-hoc commands such as ``Add new item``, ``Update item configuration`` and ``Remove an item`` available at the JID of the external component which you have just enabled (``ext@your-server-domain``).

**Possible values:** external domains parameters list.

**Available since:** 4.3.0

**Removed in:** 8.0.0

Performance
-------------------

cm-ht-traffic-throttling
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``xmpp:25k:0:disc,bin:200m:0:disc``

**Example:** ``'cm-ht-traffic-throttling' = 'xmpp:25k:0:disc,bin:200m:0:disc'``

**Possible values:** comma separated list of traffic limits settings.

**Description:** This property is used to specify traffic limit of non-user connections, that is s2s, external components and other high traffic server connections. The meaning of the property and values encoded are in the same way as for the `cm-traffic-throttling property <#cmTrafficThrottling>`__.

**Available since:** 8.0.0

cm-traffic-throttling
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``xmpp:2500:0:disc,bin:20m:0:disc``

**Example:** ``'cm-traffic-throttling' = 'xmpp:2500:0:disc,bin:20m:0:disc'``

**Possible values:** comma separated list of traffic limits settings.

**Description:** The ``cm-traffic-throttling`` property allows you to limit traffic on user connections. These limits are applied to each user connection and if a limit is exceeded then a specified action is applied.

The property value is a comma separated list of traffic limits settings. For example the first part: ``xmpp:2500:0:disc`` specifies traffic limits for XMPP data to 2,500 packets allowed within last minute either sent to or received from a user and unlimited (0) total traffic on the user connection, in case any limit is exceeded the action is to **disconnect** the user.

-  **[xmpp|bin]** traffic type, xmpp - XMPP traffic, that is limits refer to a number of XMPP packets transmitted, bin - binary traffic, that is limits refer to a number of bytes transmitted.

-  **2500** maximum traffic allowed within 1 minute. 0 means unlimited, or no limits.

-  **0** maximum traffic allowed for the life span of the connection. 0 means unlimited or no limits.

-  **[disc|drop]** action performed on the connection if limits are exceeded. disc - means disconnect, drop - means drop data.

**Available since:** 5.1.3

elements-number-limit
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``1000``

**Possible values:** any integer.

**Description:** ``elements-number-limit`` configuration property allows configuring a Denial of Service protection mechanism which limits number of elements sent in stanza. It must be configured on a per ConnectionManager basis:

.. code:: bash

   '<ConnectionManager>' {
       'elements-number-limit' = ###
   }

for example (for ClusterConnectionManager):

.. code:: bash

   'cl-comp' {
       'elements-number-limit' = 100000

**Available since:** 5.2.0

hardened-mode
^^^^^^^^^^^^^^^^^^

**Default value:** ``secure``

**Example:** ``'hardened-mode' = secure``

**Possible values:** ``relaxed|secure|strict``

**Description:** Adjusting hardened mode affects handling of security aspects within Tigase. The higher the level the more strict are the rules: \* ``relaxed`` - uses default security capabilities from installed JVM; \* ``secure`` - disables old SSLv2 and SSLv3, disables weak cyphers; \* ``strict`` - in addition to ``secure`` level changes it also disables ``TLSv1`` and ``TLSv1.1`` as well as ciphers that don’t support Forward secrecy.

On older JVM versions it required `UnlimitedJCEPolicyJDK <http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html>`__ installed. It’s not required with OpenJDK8 and newer an OracleJVM 11 and newer.

**Available since:** 5.2.0

max-queue-size
^^^^^^^^^^^^^^^^^^

**Default value:** default queue size is variable depending on RAM size.

**Example:** ``'max-queue-size' = 10000``

**Possible values:** integer number.

**Description:** The ``max-queue-size`` property sets internal queues maximum size to a specified value. By default Tigase sets the queue size depending on the maximum available memory to the Tigase server process. It set’s 1000 for each 100MB memory assigned for JVM. This is enough for most cases. If you have however, an extremely busy service with Pubsub or MUC component generating huge number of packets (presence or messages) this size should be equal or bigger to the maximum expected number of packets generated by the component in a single request. Otherwise Tigase may drop packets that it is unable to process.

**Available since:** 5.1.0

net-buff-high-throughput
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``64k``

**Example:** ``'net-buff-high-throughput' = '256k'``

**Possible values:** network buffer size as integer.

**Description:** The ``net-buff-high-throughput`` property sets the network buffer for high traffic connections like s2s or connections between cluster nodes. The default is ``64k`` and is optimal for medium traffic websites. If your cluster installation can not cope with traffic between nodes try to increase this number.

**Available since:** 4.3.0

net-buff-standard
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``2k``

**Example:** ``'net-buff-standard' = '16k'``

**Possible values:** network buffer size as integer.

**Description:** This property sets the network buffer for standard (usually c2s) connections, default value is 2k and is optimal for most installations.

**Available since:** 4.3.0

nonpriority-queue
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``false``

**Example:** ``'nonpriority-queue' =  true``

**Possible values:** ``true|false``

**Description:** The ``nonpriority`` property can be used to switch to non-priority queues usage in Tigase server (value set to 'true'). Using non-priority queues prevents packets reordering. By default Tigase uses priority queues which means that packets with highest priority may take over packets with lower priority (presence updates) which may result in packets arriving out of order.

This may happen however only for packets of different types. That is, messages may take over presence packets. However, one message never takes over another message for the same user. Therefore, out of order packet delivery is not an issue for the most part.

**Available since:** 5.0.0

VHost / domain
--------------------

vhost-anonymous-enabled
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``true``

**Example:** ``'vhost-anonymous-enabled' = 'false'``

**Possible values:** ``true|false``

**Description:** The ``vhost-anonymous-enabled`` property specifies whether anonymous user logins are allowed for the installation for all vhosts.

This is a global property which is overridden by settings for particular VHost (see ` Add and Manage Domains (VHosts)`__).

Default settings for all virtual hosts are used when this property is not defined. This settings is useful mostly for installations with many virtual hosts listed in the ``config.tdsl`` file for which there is no individual settings specified. It allows the configuration of default values for all of them, instead of having to provide individual configuration for each VHost.

**Available since:** 8.0.0

vhost-disable-dns-check
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``false``

**Example:** ``'vhost-disable-dns-check' = 'true'``

**Possible values:** ``true|false``

**Description:** This property disables DNS validation when adding or editing vhosts in Tigase server. This also exempts administrative accounts from validation. With this property enabled, you will not benefit from seeing if proper SRV records are set so other people can connect to specific vhosts from outside your network.

This is a global property which is overridden by settings for particular VHost (see `??? <#addManageDomain>`__).

**Available since:** 8.0.0

vhost-max-users
^^^^^^^^^^^^^^^^^^

**Default value:** ``0``

**Example:** ``'vhost-max-users' = '1000'``

**Possible values:** integer number.

**Description:** The ``vhost-max-users`` property specifies how many user accounts can be registered on the installations for all vhosts.

**0 - zero** means unlimited and this is a default. Otherwise greater than zero value specifies accounts number limit.

This is a global property which is overridden by settings for particular vhost.

The default setting is used for all virtual hosts for which the configuration is not defined. This settings is most useful for installations with many virtual hosts listed in the ``init.property`` file for which there is no individual settings specified. It provides an ability to use default values for all of them, instead of having to provide individual configuration for each vhost.

This is a global property which is overridden by settings for particular VHost (see `??? <#addManageDomain>`__).

**Available since:** 8.0.0

vhost-message-forward-jid
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** <null>

**Example:** ``'vhost-message-forward-jid' = 'archive@domain.com'``

**Possible values:** 'valid JID'

**Description:** This is a global property for message forwarding for the installation. This property is normally specified on the vhost configuration level, however if you want to forward all messages on your installation and you have many virtual domains this property allows to set message forwarding for all of them. A valid JID must be specified as the forwarding destination. Also a message forwarding plugin must be loaded and activated on the installation for the message forwarding to work.

The null value is used as a default when no configuration is set. This setting is mostly useful for installations with many virtual hosts listed in the ``init.property`` file for which there is no individual settings specified. It provides the ability to configure a default values for all of them, instead of having to provide individual configuration for each vhost.

It is also applied as a default value for all new vhosts added at run-time.

This is a global property which is overridden by settings for particular VHost (see `??? <#addManageDomain>`__).

**Available since:** 8.0.0

vhost-presence-forward-jid
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``<null>``

**Example:** ``'vhost-presence-forward-jid' = 'presence-collector@domain.com'``

**Possible values:** valid JID.

**Description:** This is a global property for presence forwarding function for the installation. All user status presences will be forwarded to given XMPP address which can be a component or any other XMPP entity. If the destination entity is a bot connected via c2s connection it probably should be addressed via full JID (with resource part) or the standard XMPP presence processing would refuse to deliver presences from users who are not in the contact list.

This is a global property which is overridden by settings for particular vhost.

The null value is used as a default when no configuration is set. This settings is useful for installations with many virtual hosts listed in the ``init.property`` file for which there is no individual settings specified. It enables the ability to configure default values for all of them, instead of having to provide individual configuration for each vhost.

It is also applied as a default value for all new vhosts added at run-time.

This may be used on a per-VHost (see `??? <#addManageDomain>`__).

**Available since:** 8.0.0

vhost-register-enabled
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``true``

**Example:** ``'vhost-register-enabled' = false``

**Possible values:** ``true|false``

**Description:** ``vhost-register-enabled`` is a global property which allows you to switch on/off user registration on the installation. Setting this property to ``false`` does not disable the registration plugin on the server. You can enable registration for selected domains in the domain configuration settings.

This is a global property which is overridden by settings for particular vhost.

The ``true`` value is used as a default when no configuration is set. This settings is useful for installations with many virtual hosts listed in the ``init.property`` file for which there is no individual settings specified. It allows admins to configure default values for all of them, instead of having to provide individual configuration for each vhost.

It is also applied as a default value for all new vhosts added at run-time.

This may be used on a per-VHost (see `??? <#addManageDomain>`__).

**Available since:** 8.0.0

vhost-tls-required
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** ``false``

**Example:** ``'vhost-tls-required' = true``

**Possible values:** ``true|false``

**Description:** This property is a global settings to switch on/off TLS required mode on the Tigase installation. Setting this property to ``false`` does not turn TLS off. TLS is still available on the server but as an option and this is the client’s decision whether to use encryption or not. If the property is set to true the server will not allow for user authentication or sending any other user data before TLS handshaking is completed.

This is a global property which is overridden by settings for particular vhost.

The ``false`` value is used as a default when no configuration is set. This settings is useful for installations with many virtual hosts listed in the ``init.property`` file for which there is no individual settings specified. It allows admins to configure default values for all of them, instead of having to provide individual configuration for each vhost.

It is also applied as a default value for all new vhosts added at run-time.

This may be used on a per-VHost (see `??? <#addManageDomain>`__).

**Available since:** 8.0.0



