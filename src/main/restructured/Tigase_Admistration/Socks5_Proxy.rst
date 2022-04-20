12. Tigase Socks5 Proxy
==========================

Welcome to Tigase Socks5 Proxy guide

Tigase SOCKS5 component allows for file transfers to be made over a SOCKS5 proxy in accordance with `XEP-0065 SOCKS5 Bytestreams <http://xmpp.org/extensions/xep-0065.html>`__. This allows for some useful features such as: - transfer limits per user, domain, or global - recording transfers between users - quotas and credits system implementation

12.1. Overview
-----------------
Tigase Socks5 Proxy is implementation of Socks5 proxy described in `XEP-0065: SOCKS5 Bytestreams, in section 6. Mediated Connection <https://xmpp.org/extensions/xep-0065.html#mediated:>`__ which provides support for Socks5 proxy for file transfers between XMPP client behind NATs to Tigase XMPP Server.

12.1.1. Installation
^^^^^^^^^^^^^^^^^^^^^^^^

Tigase SOCKS5 component comes built into the dist-max archives for Tigase XMPP server, and requires the component to be listed in config.tdsl file:

.. code:: dsl

   proxy {}

You will also need to decide if you wish to use database-based features or not. If you wish to simply run the socks5 proxy without features such as quotas, limits add the following line:

.. code:: dsl

   proxy {
       'verifier-class' = 'tigase.socks5.verifiers.DummyVerifier'
   }

This will enable the SOCKS5 Proxy without any advanced features. If you wish to use those features, see the configuration section below.


12.1.2. Database Preparation
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

In order to use the more advanced features of the SOCKS5 Proxy Component, your database needs to be prepared with the proper schema prior to running the server.

You may either edit an existing database, or create a new database for specific use with the Proxy.

Edit Existing Database
~~~~~~~~~~~~~~~~~~~~~~~~~~

You can add the proper schema to your existing database using the DBSchemaLoader utility included with Tigase. The database folder contains the schema file for your type of database.

First, backup your database before performing any actions and shut down Tigase XMPP Server.

Then from the Tigase installation directory run the following command:

.. code:: bash

   java -cp "jars/*" tigase.db.util.DBSchemaLoader -dbType {derby,mysql,postgresql,sqlserver} - dbHostname {db address} -dbName {dbname} -rootUser root -rootPass root -file database/{dbtype}-socks5-schema.sql

You should see the following dialogue

::

   LogLevel: CONFIG
   tigase.db.util.DBSchemaLoader        <init>            CONFIG     Properties: [{dbHostname=localhost, logLevel=CONFIG, dbType=derby, file=database/derby-socks5-schema.sql, rootUser=root, dbPass=tigase_pass, dbName=tigasedb, schemaVersion=7-1, rootPass=root, dbUser=tigase_user}]
   tigase.db.util.DBSchemaLoader        validateDBConnection    INFO       Validating DBConnection, URI: jdbc:derby:tigasedb;create=true
   tigase.db.util.DBSchemaLoader        validateDBConnection    CONFIG     DriverManager (available drivers): [[jTDS 1.3.1, org.apache.derby.jdbc.AutoloadedDriver@34a245ab, com.mysql.jdbc.Driver@3941a79c, org.postgresql.Driver@6e2c634b]]
   tigase.db.util.DBSchemaLoader        validateDBConnection    INFO       Connection OK
   tigase.db.util.DBSchemaLoader        validateDBExists    INFO       Validating whether DB Exists, URI: jdbc:derby:tigasedb;create=true
   tigase.db.util.DBSchemaLoader        validateDBExists    INFO       Exists OK
   tigase.db.util.DBSchemaLoader        loadSchemaFile      INFO       Loading schema from file: database/derby-socks5-schema.sql, URI: jdbc:derby:tigasedb;create=true
   tigase.db.util.DBSchemaLoader        loadSchemaFile      INFO        completed OK
   tigase.db.util.DBSchemaLoader        shutdownDerby       INFO       Validating DBConnection, URI: jdbc:derby:tigasedb;create=true
   tigase.db.util.DBSchemaLoader        shutdownDerby       WARNING    Database 'tigasedb' shutdown.
   tigase.db.util.DBSchemaLoader        printInfo           INFO

One this process is complete, you may begin using SOCKS5 proxy component.

Create New Database
~~~~~~~~~~~~~~~~~~~~~~~~~

If you want to create a new database for the proxy component and use it as a separate socks5 database, create the database using the appropriate schema file in the database folder. Once this is created, add the following line to your config.tdsl folder.

.. code:: dsl

   proxy {}

For example, a mysql database will have this type of URL: jdbc:mysql://localhost/SOCKS?user=root&password=root to replace database URL. For more options, check the database section of `this documentation <#databasePreperation>`__.

12.2. Configuration
--------------------

12.2.1. Enabling proxy
^^^^^^^^^^^^^^^^^^^^^^^^^


To enable Tigase Socks5 Proxy component for Tigase XMPP Server, you need to activate ``socks5`` component in Tigase XMPP Server configuration file (``etc/config.tdsl``). In simples solution it will work without ability to enforce any limits but will also work without a need of database to store informations about used bandwidth.

**Simple configuration.**

.. code:: dsl

   socks5 () {
       repository {
           default () {
               cls = 'dummy'
           }
       }
   }

**``remote-addresses``**
~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. code:: dsl

   proxy {
       'remote-addresses' = '192.168.1.205,20.255.13.190'
   }

Comma seperated list of IP addresses that will be accessible VIA the Socks5 Proxy. This can be useful if you want to specify a specific router address to allow external traffic to transfer files using the proxy to users on an internal network.


Port settings
~~~~~~~~~~~~~~

If socks5 is being used as a proxy, you may configure a specific ports for the proxy using the following line in config.tdsl:

.. code:: dsl

   proxy {
       'connections' {
           'ports' = [ 1080 ]
         }
   }

Enabling limits
~~~~~~~~~~~~~~~~~

To enable limits you need to import schema files proper for your database and related to Tigase Socks5 Proxy component from ``database`` directory. To do this, refer to the previous section.

With that setup, it is possible to enable limits verifier by replacing entries related to Tigase Socks5 Proxy component configuration with following entries. This will use default database configured to use with Tigase XMPP Server.


``DummyVerifier``
''''''''''''''''''

-  Class Name: ``tigase.socks5.verifiers.DummyVerifier``

This accepts file transfers VIA SOCKS5 proxy from any user and does not check limitations against the database.

.. code:: dsl

   socks5 () {
       verifier (class: tigase.socks5.verifiers.DummyVerifier) {
       }
   }


``LimitsVerifier``
'''''''''''''''''''

-  Class Name: ``tigase.socks5.verifiers.LimitsVerifier``

Uses the database to store limits and record the amount of data transferred VIA the proxy.


Configuring limits


Following properties are possible to be set for ``LimitsVerifier``:

.. code:: dsl

   proxy {
       'verifier-class' = 'tigase.socks5.verifiers.LimitsVerifier'
       tigase.socks5.verifiers.LimitsVerifier {
           'transfer-update-quantization' = '1000'
           'instance-limit' = '3000'
       }
   }

Parameters for ``LimitsVerifier`` which will override the defaults. All of these limits are on a per calendar month basis. For example, a user is limited to 10MB for all transfers. If he transfers 8MB between the 1st and the 22nd, he only has 2MB left in his limit. On the 1st of the following month, his limit is reset to 10MB.

Available parameters:

-  ``transfer-update-quantization`` which value is used to quantitize value to check if value of transferred bytes should be updated in database or not. By default it is 1MB. (Low value can slow down file transfer while high value can allow to exceed quota)

-  ``global-limit`` - Transfer limit for all domains in MB per month.

-  ``instance-limit`` - Transfer limit for server instance in MB per month.

-  ``default-domain-limit`` - The Default transfer limit per domain in MB per month.

-  ``default-user-limit`` - The default transfer limit per user in MB per month.

-  ``default-file-limit`` - The default transfer limit per file in MB per month.

.. Note::

   Low values can slow down file transfers, while high values can allow for users to exceed quotas.


Individual Limits


Using the default database schema in table tig_socks5_users limits can be specified for individual users.

Value of the field *user_id* denotes the scope of the limitation:

-  *domain_name* defines limits for users which JIDs are within that domain;

-  *JID* of the user defines limit for this exact user.

Value of the limit bigger than 0 defines an exact value. If value is equal 0 limit is not override and more global limit is used. If value equals -1 proxy will forbid any transfer for this user. It there is no value for user in this table new row will be created during first transfer and limits for domain or global limits will be used.

Socks5 database is setup in this manner:

.. table:: Table 13. tig_socks5_users

   +-----+-----------------+------------------------------------------+------------+------------------------------------------+----------------+-------------------------+---------------------------+
   | uid | user_id         | sha1_user_id                             | domain     | sha1_domain                              | filesize_limit | transfer_limit_per_user | transfer_limit_per_domain |
   +=====+=================+==========================================+============+==========================================+================+=========================+===========================+
   | 1   | user@domain.com | c35f2956d804e01ef2dec392ef3adae36289123f | domain.com | e1000db219f3268b0f02735342fe8005fd5a257a | 0              | 3000                    | 0                         |
   +-----+-----------------+------------------------------------------+------------+------------------------------------------+----------------+-------------------------+---------------------------+
   | 2   | domain.com      | e1000db219f3268b0f02735342fe8005fd5a257a | domain.com | e1000db219f3268b0f02735342fe8005fd5a257a | 500            | 0                       | 0                         |
   +-----+-----------------+------------------------------------------+------------+------------------------------------------+----------------+-------------------------+---------------------------+

This example table shows that user@domain.com is limited to 3000MB per transfer whereas all users of domain.com are limited to a max file size of 500MB. This table will populate as users transfer files using the SOCKS5 proxy, once it begins population, you may edit it as necessary. A second database is setup tig_socks5_connections that records the connections and transmissions being made, however it does not need to be edited.


12.2.2. Using a separate database
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To use separate database with Tigase Socks5 Proxy component you need to configure new ``DataSource`` in ``dataSource`` section. Here we will use ``socks5-store`` as name of newly configured data source. Additionally you need to pass name of newly configured data source to ``dataSourceName`` property of ``default`` repository of Tigase Socks5 Proxy component.

.. code:: dsl

   dataSource {
       socks5-store () {
           uri = 'jdbc:db_server_type://server/socks5-database'
       }
   }

   socks5 () {
       repository {
           default () {
               dataSourceName = 'socks5-store'
           }
       }
       ....
   }



12.3. Performance
------------------

Tigase Socks5 Proxy component was tested with 100 concurrent transfers. Maximal traffic processed by component was 21,45MB/s on loopback interface. All XMPP clients and Tigase XMPP Server used in test were running on the single machine.
