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

.. code::

   <component> {
       'port-delay-listening' = false
     }

**Possible values:** ``true``, ``false``

In cluster mode, in order to ensure correct user status broadcast, we are delaying opening client ports (components: ``c2s``, ``ws2s``, ``bosh``) and enable those only after cluster is fully and correctly connected (i.e. either there is only single node or in case of multiple nodes all nodes connected correctly).

It’s possible to enable/disable this on per-component basis with the following configuration:

.. code::

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

.. code::

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

.. code::

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

.. code::

   <ConnectionManager> {
       watchdog_timeout = 60000L
   }

for example (for C2SConnectionManager):

.. code::

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

.. code::

   <ConnectionManager> {
     watchdog_delay = 60000L
   }

for example (for ClusterConnectionManager):

.. code::

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

.. code::

   <ConnectionManager> {
     watchdog_ping_type = 'XMPP'
   }

for example (for ClusterConnectionManager):

.. code::

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