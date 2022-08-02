.. _Tigase-Tip-Checking-the-Runtime-Environment:

Tips and Tricks
----------------------

The section contains some short tricks and tips helping in different kinds of issues related to the server administration and maintenance.

-  :ref:`Runtime Environment Tip<tigaseTip_RuntimeEnvironment>`

-  :ref:`Best Practices for Connecting to Tigase XMPP server From Web Browser<Best-Practices-for-Connecting-to-Tigase-XMPP-server-From-Web-Browser>`

.. _tigaseTip_RuntimeEnvironment:

Tigase Tip: Checking the Runtime Environment
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

It has happened recently that we have tried very hard to fix a few annoying problems on one of the Tigase installations. Whatever we did, the problem still existed after uploading a new version and restarting the server. It worked fine in our development environment and it just didn’t on the target system.

It turned out that due to specific environment settings on the target system, an old version of Tigase server was always started regardless of what updates uploaded. We finally located the problem by noticing that the logs were not being generated in the proper locations. This led us to finding the issue: improper environment settings.

The best way to check all the environment settings used to start the Tigase server is to use… ``check`` command line parameter:

.. code:: bash

   $ ./scripts/tigase.sh check etc/tigase.conf

   Checking arguments to Tigase
   TIGASE_HOME = .
   TIGASE_JAR = jars/tigase-server.jar
   TIGASE_PARAMS = etc/tigase.conf
   TIGASE_CONFIG = etc/tigase.xml
   TIGASE_RUN = tigase.server.XMPPServer -c etc/tigase.xml --property-file etc/init.properties
   TIGASE_PID = ./logs/tigase.pid
   TIGASE_OPTIONS = --property-file etc/init.properties
   JAVA_OPTIONS = -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 \
       -Djdbc.drivers=com.mysql.jdbc.Driver:org.postgresql.Driver \
       -server -Xms100M -Xmx200M -XX:PermSize=32m -XX:MaxPermSize=256m
   JAVA = /System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/bin/java
   JAVA_CMD =
   CLASSPATH = ./jars/tigase-server.jar:./libs/jdbc-mysql.jar:./libs/jdbc-postgresql.jar:\
       ./libs/tigase-extras.jar:./libs/tigase-muc.jar:./libs/tigase-pubsub.jar:\
       ./libs/tigase-utils.jar:./libs/tigase-xmltools.jar
   TIGASE_CMD = /System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home/bin/java \
       -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 \
       -Djdbc.drivers=com.mysql.jdbc.Driver:org.postgresql.Driver \
       -server -Xms100M -Xmx200M -XX:PermSize=32m -XX:MaxPermSize=256m \
       -cp ./jars/tigase-server.jar:./libs/jdbc-mysql.jar:./libs/jdbc-postgresql.jar:\
       ./libs/tigase-extras.jar:./libs/tigase-muc.jar:./libs/tigase-pubsub.jar:\
       ./libs/tigase-utils.jar:./libs/tigase-xmltools.jar tigase.server.XMPPServer \
       -c etc/tigase.xml --property-file etc/init.properties
   TIGASE_CONSOLE_LOG = ./logs/tigase-console.log

In our case ``TIGASE_HOME`` was set to a fixed location pointing to an old version of the server files. The quick ``check`` command may be a real time saver.

.. _Best-Practices-for-Connecting-to-Tigase-XMPP-server-From-Web-Browser:

Best Practices for Connecting to Tigase XMPP server From Web Browser
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Currently we have 2 ways to connect to Tigase XMPP Server from web browsers:

1. BOSH (Bidirectional-streams Over Synchronous HTTP)

2. WebSocket (XMPP over WebSocket)

You will find more information about these ways for connecting to Tigase XMPP Server with some useful tips below.

BOSH
^^^^^

BOSH protocol specified in `XEP-0124 <http://xmpp.org/extensions/xep-0124.html>`__ is one of first protocols defined to allow to establish XMPP connection to XMPP servers from web browsers due to this protocol being widely supported and used. It is also easy to use in single server mode. It’s enabled by default in Tigase XMPP Server and available at port 5280.

In clustered mode we can deploy it with load balancer deployed with guarantees that each BOSH connection from web browser will be forwarded to same Tigase XMPP Server instance. So in clustered mode if we have two XMPP server ``t1`` and ``t2`` which are hosting domain ``example.com`` we would need to have load balancer which will respond for HTTP request to domain ``example.com`` and forward all requests from same IP address to same node of a cluster (i.e. all request from ``192.168.122.32`` should be forwarded always to node ``t1``.


Tip #1 - BOSH in Cluster Mode Without Load Balancer

There is also a way to use BOSH without load balancer enabled. In this case the XMPP client needs to have more logic and knowledge about all available cluster nodes (with names of nodes which will identify particular cluster nodes from internet). Using this knowledge XMPP client should select one random node from list of available nodes and always establish BOSH connections to this particular node. In case if BOSH connection fails due to network connection issues, the XMPP client should randomly pick other node from list of rest of available nodes.

*Solution:*

Tigase XMPP Server by default provides server side solution for this issue by sending additional ``host`` attribute in ``body`` element of BOSH response. As value of this attribute Tigase XMPP Server sends domain name of server cluster node to which client connected and to which next connections of this session should be opened. It is possible to disable this custom feature by addition of of following line to ``etc/config.tdsl`` config file:

.. code::

   bosh {
       'send-node-hostname' = false
   }

*Example:*

We have servers ``t1.example.com`` and ``t2.example.com`` which are nodes of a cluster hosting domain ``example.com``. Web client retrieves list of cluster nodes from web server and then when it needs to connect to the XMPP server it picks random host from list of retrieved cluster nodes (i.e. ``t2.example.com``) and tries to connect using BOSH protocol to host ``t2.example.com`` but it should send ``example.com`` as name of the server it tries to connect to (``example.com`` should be value of ``to`` attribute of XMPP stream).

WebSocket
^^^^^^^^^^^^^^

WebSocket protocol is newly standardized protocol which is supported by many of current versions of browsers. Currently there is a draft of protocol `draft-ietf-xmpp-websocket-00 <https://datatracker.ietf.org/doc/draft-ietf-xmpp-websocket/>`__ which describes usage of WebSocket to connect to XMPP servers. Tigase XMPP Server implementation of WebSocket protocol to connect to XMPP server is very close to this draft of this specification. By default Tigase XMPP Server has XMPP-over-WebSocket protocol enabled without encryption on port 5290. To use this protocol you need to use library which supports XMPP-over-WebSocket protocol.


Tip #1 - Encrypted WebSocket Connection

It is possible to enable encrypted WebSocket connection in Tigase XMPP Server. To do this you need to add following lines to ``etc/config.tdsl`` config file:

.. code::

   ws2s {
       connections {
           ports = [ 5290, 5291 ]
           5290 {
               socket = 'ssl'
               type = 'accept'
           }
           5291 {
               socket = 'plain'
               type = 'accept'
           }
       }
   }

In this example we enabled WebSocket endpoint on port 5290 allowing unencrypted connections, and encrypted WebSocket endpoint on port 5291. As this is TLS/SSL connection (no STARTTLS) it uses default certificate installed in Tigase XMPP Server instance. This certificate is located in ``certs/default.pem``.

.. Note::

   There is no default configuration for non-default ports. All ports outside 443 MUST be configured.

Tip #2 - Encrypted WebSocket Connection - Dealing With Multiple VHosts

As mentioned in Tip #1 WebSocket endpoint is plain TLS/SSL port, so it always serves default certificate for Tigase XMPP Server instance. That is ok if we are hosting single domain and if default certificate matches matches our domain. But If we host multiple domain we cannot use ``wss://example1.com:5291/`` connection URL, if our default certificate is for domain ``example2.com``. In this situation it is recommended to use the default certificate for the domain under which the server is accessible from the internet. This domain should identify this server, so this domain would not point to two nodes of a cluster. After we deploy separate certificate for each of cluster nodes, we should follow same tip as Tip #1 for BOSH. Our web-based XMPP client should have knowledge about each node of a cluster and when it needs to connect it should randomly select one node from list of available cluster nodes and try to connect using connection URL that would contain name of server under which it can be identified from internet.

*Example:*

We have servers ``t1.example1.com`` and ``t2.example1.com`` which are nodes of a cluster in hosting domain ``example2.com``. Each of our nodes contains default SSL certificate with domain names matching the cluster node. Web client retrieves list of cluster nodes from web server and then when it needs to connect to XMPP server it picks random host from list of retrieved cluster nodes (i.e. ``t2.example1.com``) and tries to connect using WebSocket encrypted protocol to host ``t2.example1.com`` using the following URL: ``wss://t2.example1.com:5291/``. Upon connection the client should still send example2.com as name of server to which it tries to connect (``example2.com`` should be value of to attribute of XMPP stream). This will allow browser to validate certificate as it will be for the same domain to which browser connects, and it will allow XMPP client to connect to domain ``example2.com``, which is one of hosted vhosts.