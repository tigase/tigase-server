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

Welcome to Tigase Push component guide


























13.1. Tigase Push Component
----------------------------

Tigase Push component is a Push notifications component implementing `XEP-0357: Push Notifications <https://xmpp.org/extensions/xep-0357.html>`__. It is a gateway between Push Notification services and XMPP servers. It is configured by default to run under name of ``push``.

.. Note::

   Tigase Push component requires at the minimum version 8.0.0 of Tigase XMPP Server.

Push notifications enable messages and pertinent information for clients, even if they are offline as long as they are registered with the push service. Tigase Messenger for iOS and Tigase Messenger for Android both have support for this feature.

13.1.1. Workflow
^^^^^^^^^^^^^^^^^^

The workflow for enabling and using push notifications works as follows:

Enabling notifications
~~~~~~~~~~~~~~~~~~~~~~~

In order to receieve notifications, clients will require registration with a push service. Although this process is mainly invisible to the user, the steps in registration are listed here:

-  The client registers and bootstraps too it’s assicoated push service. This is done automatically.

-  The client registers itself with the push service server which then will dedicate a node for the device.

-  Node information is passed back to the client and is shared with necessary components.

Receiving notifications
~~~~~~~~~~~~~~~~~~~~~~~~

Notifications sent from the server are recieved the following (simplified) way:

-  A message is published on the XMPP node which is then sent to the push service on the same server.

-  The push service will then inform the user agent (an application on the device running in the background) that a push notification has been sent.

-  The user agent will then publish the notification to the client for a user to see, waking up or turning on the client if is not running or suspended.

13.2. Tigase Push Release Notes
--------------------------------

Welcome to Tigase Push 1.2.0! This is a feature release for with a number of fixes and updates.

13.2.1. Tigase Push 1.2.0 Release Notes
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Major Changes
~~~~~~~~~~~~~~~~~

-  Added support for sending VoIP push notifications using PushKit

-  Support for storing APNS certificates in repository instead of filesystem for easier cluster deployments

-  Add priority detection for push notifications to avoid excessive pushes to devices

-  Inclusion of APNS certificate validity task that notifies if it’s about to expire

All Changes
~~~~~~~~~~~~

-  `#push-29 <https://projects.tigase.net/issue/push-29>`__ Added support for sending VoIP push notifications using PushKit

-  `#push-30 <https://projects.tigase.net/issue/push-30>`__ Added REST API handler for quick unregistration of a device

-  `#push-32 <https://projects.tigase.net/issue/push-32>`__ Fixed issue with APNS certificate validation

-  `#push-33 <https://projects.tigase.net/issue/push-33>`__ Added statistics gathering

-  `#push-35 <https://projects.tigase.net/issue/push-35>`__ Added support for APNS certificate in PEM

-  `#push-36 <https://projects.tigase.net/issue/push-36>`__ Improved priority detection of push notifications

-  `#push-37 <https://projects.tigase.net/issue/push-37>`__ Enable APNS certificates to be stored in UserRepository - management is done via ad-hoc command;

-  `#push-39 <https://projects.tigase.net/issue/push-39>`__ Changes to improve error handling

-  `#push-41 <https://projects.tigase.net/issue/push-41>`__ Fixed issue with ``ApnsService`` exceptions not being thown logged

-  `#push-42 <https://projects.tigase.net/issue/push-42>`__ Fixed error type reported back on ``tooManyRequestsForDeviceToken``

-  `#push-47 <https://projects.tigase.net/issue/push-47>`__ Added task to periodically validate SSL certificates for Push notifications

-  `#push-48 <https://projects.tigase.net/issue/push-48>`__ Fixed handling events by APNsBinaryApiProvider

-  `#push-49 <https://projects.tigase.net/issue/push-49>`__ Added enforcement to use HTTP/2 protocol (with use of ALPN)

13.3. Configuration
---------------------

13.3.1. Enabling component
^^^^^^^^^^^^^^^^^^^^^^^^^^^

Push notifications may be sent by Tigase XMPP Server with or without use of Push component. Push Component is only required if you have your own application for mobile devices for which you want to send push notifications.

This component is not loaded and enabled by default as it requires implementations of Push notifications providers and additional configuration (including credentials required to authorize to push services). Following entries will activate component:

.. code:: DSL

   push () {
   }

.. Note::

   You need to enable and configure push providers implementations before it will be possible to send push notifications. For more details about this process, please check documentations of push service provider projects.

13.4. Usage
------------

13.4.1. Sending notifications
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

When you will register a device for a Push Notifications, you will receive name of the PubSub node where you should publish items. Publishing items to this node, as specified in `XEP-0357: Push Notifications <https://xmpp.org/extensions/xep-0357.html>`__ will result in push notifications being delivered to the registered device.


13.4.2. Registering device
^^^^^^^^^^^^^^^^^^^^^^^^^^^

To register a device you need to execute the adhoc command ``register-device`` available at Push Notification component. This command will return a form which needs to be filled.

Form consists of following fields:

**provider**
   ID of a provider for which you want to register a device. It contains a list of available providers and you need to select a proper one.

**device-token**
   Unique token which your application retrieved from a device or client library and which should be used to identify device you want to register for push notifications.

When you submit this form, it will be processed and will respond with a ``result`` type form. Within this form you will find a ``node`` field which will contain a PubSub node name created by the Push Notifications component, to which you should publish notification items. This returned node with jid of the Push Notifications Component should be passed to your XMPP server as the address of the XMPP Push Service.

13.4.3. Unregistering device
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To unregister a device, you need to execute the adhoc command ``unregister-device`` available within the Push Notification component. This command will return a form which needs to be filled out.

This form consists of the following fields:

**provider**
   ID of a provider for which your devices was registered.

**device-token**
   Unique token which your application retrieved from a device or client library and was registered at this push notifications component.

When you submit this form, it will be processed and will respond with a ``result`` form to notify you that device was successfully unregistered from the push notifications component.

13.4.4. Unregistering device via HTTPS
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

There is REST API handler (in form of ``UnregisterDeviceHandler.groovy`` script) which placed in ``/scripts/rest/push/`` directory in Tigase XMPP Server installation directory will enable endpoint (documented in Development Guide) allowing client to disable their push notifications even without authentication to their XMPP server.

.. Note::

   It is recommended to not expose this endpoint using HTTP but only with HTTPS.

13.5. Providers
----------------

Providers availability depends on the deployed binaries, by default Tigase includes following providers:

13.5.1. Tigase Push Component - FCM provider
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

13.5.2. Overview
^^^^^^^^^^^^^^^^^

Tigase Push Component - FCM provider is an implementation of FCM provider for Tigase Push Component. It allows Tigase Push Component to connect to Firebase Cloud Messaging and send notifications using this service.

13.5.3. Configuration
^^^^^^^^^^^^^^^^^^^^^^^^

Enabling provider
~~~~~~~~~~~~~~~~~

To enable this provider, you need to enable fcm-xmpp-api bean within push component configuration scope.

**Example.**

.. code:: DSL

   push () {
       'fcm-xmpp-api' () {
           # FCM configuration here
       }
   }

.. Note::

   You need to pass FCM configuration parameters to make it work, see below.

Setting FCM credentials
~~~~~~~~~~~~~~~~~~~~~~~~

FCM XMPP API provider will not work properly without API key and project id as this values are required for authorization by FCM. You need to get information from FCM account.

When you have this data, you need to pass sender id as sender-id property and server key as server-key property.

**Example.**

.. code:: DSL

   push () {
       'fcm-xmpp-api' () {
           'sender-id' = 'your-sender-id'
           'server-key' = 'your-server-key'
       }
   }

Connection pool
~~~~~~~~~~~~~~~~

By default this provider uses single client to server connection to FCM for sending notifications. If in your use case it is to small (as you need better performance), you should adjust value of pool-size configuration property. Setting it to ``5`` will open five connections to FCM for better performance.

**Example.**

.. code:: DSL

   push () {
       'fcm-xmpp-api' () {
           'pool-size' = 5
       }
   }

13.5.4. Tigase Push Component - APNs provider
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
13.5.5. Overview
^^^^^^^^^^^^^^^^^

Tigase Push Component - APNs provider is an implementation of APNs provider for Tigase Push Component. It allows Tigase Push Component to connect to Apple Push Notification service and send notifications using this service.

13.5.6. Configuration
^^^^^^^^^^^^^^^^^^^^^

Enabling provider
~~~~~~~~~~~~~~~~~~

To enable this provider, you need to enable apns-binary-api bean within push component configuration scope.

**Example.**

.. code:: DSL

   push () {
       'apns-binary-api' () {
           # APNs configuration here
       }
   }

.. Note::

   You need to pass APNs configuration parameters to make it work, see below.

Setting APNs credentials
~~~~~~~~~~~~~~~~~~~~~~~~

APNs binary API provider will not work properly without certificate file required for authorization by APNs and password to decrypt this certificate file. You need to get certificate using Apple Developer Account.

When you have this certificate, you need to pass path to certificate file as cert-file property, password as cert-password and APNS topic (bundle id) as apns-topic.

**Example for /etc/apns-cert.p12, Pa$$word and com.bundle.id.**

.. code:: DSL

   push () {
       'apns-binary-api' () {
           'cert-file' = '/etc/apns-cert.p12'
           'cert-password' = 'Pa$$w0rd'
           'apns-topic' = 'com.bundle.id'
       }
   }

Alternatively, certificate can be stored in the database and in that case the TDSL configuration file should only contain ``'apns-topic'`` entry and the certificate and the password should be updated via ad-hoc command (Service discovery → Push component → Set APNS certificate). In the ad-hoc you should select the APNS provider from the list and include base64 encoded certificate obtained from Apple (``.p12`` file), for example:

.. code:: bash

   base64 -w 0 PushCertificate.p12
