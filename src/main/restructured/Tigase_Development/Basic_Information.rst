Basic Information
========================

Tigase Architecture
---------------------

The most important thing to understand is that Tigase is very modular and you can have multiple components running inside single instance. However one of the most important components is MessageRouter, which sits in the centre and serves as a, as name suggest, packet router directing packets to the appropriate components.

There is also a group of specialised component responsible for handling users connections: ``ConnectionManagers`` (``c2s``, ``s2s``, ``ws2s``, ``bosh``). They receive packets from the incoming connection, then subsequently they forward processed packet to ``MessageRouter``. Most of the time, especially for packets coming from user connections, packet is routed to ``SessionManager`` component (with the session object referring to appropriate user in case of client to server connection). After processing in ``SessionManager`` packet goes back to ``MessageRouter`` and then, based on the stanza addressing\` can go to different component (muc, pubsub) or if it’s addressed to another user it can go through:

-  ``SessionManager`` (again), ``MessageRouter`` and then (user) ``ConnectionManagers`` or,

-  ``s2s`` (*server to server connection manager*) if the user or component is on the different, federated, xmpp server;

In a very broad view this can be depicted with a following graph:

|Tigase architecture|

Tigase Server Elements
--------------------------

To make it easier to get into the code below are defined basic terms in the Tigase server world and there is a brief explanation how the server is designed and implemented. This document also points you to basic interfaces and implementations which can be used as example code reference.

Logically all server code can be divided into 3 kinds of modules: **components**, **plug-ins** and **connectors**.

1. **Components** are the main element of Tigase server. Components are a bigger piece of code which can have separate address, receive and send stanzas, and be configured to respond to numerous events. Sample components implemented for Tigase server are: *c2s connection manager*, *s2s connection manager*, *session manager*, *XEP-0114 - external component connection manager*, *MUC - multi user char rooms*.

2. **Plug-ins** are usually small pieces of code responsible for processing specific XMPP stanzas. They don’t have their own address. As a result of stanza processing they can produce new XMPP stanzas. Plug-ins are loaded by *session manager* component or the *c2s connection manager* component. Sample plug-ins are: *vCard* stanza processing, *jabber:iq:register* to register new user accounts, *presence* stanza processing, and *jabber:iq:auth* for non-sasl authentication.

3. **Connectors** are modules responsible for access to data repositories like databases or LDAP to store and retrieve user data. There are 2 kinds of connectors: authentication connectors and user data connectors. Both of them are independent and can connect to different data sources. Sample connectors are: *JDBC database* connector, *XMLDB - embedded database* connector, *Drupal database* connector.

There is an API defined for each kind of above modules and all you have to do is enable the implementation of that specific interface. Then the module can be loaded to the server based on it’s configuration settings. There is also abstract classes available, implementing these interfaces to make development easier.

Here is a brief list of all interfaces to look at and for more details you have to refer to the guide for specific kind of module.

Components
^^^^^^^^^^^^^^^^

This is list of interfaces to look at when you work on a new component:

1. **tigase.server.ServerComponent** - This is the very basic interface for component. All components must implement it.

2. **tigase.server.MessageReceiver** - This interface extends ``ServerComponent`` and is required to implement by components which want to receive data packets like *session manager* and *c2s connection manager*.

3. **tigase.conf.Configurable** - Implementing this interface is required to make it configurable. For each object of this type, configuration is pushed to it at any time at runtime. This is necessary to make it possible to change configuration at runtime. Be careful to implement this properly as it can cause issues for modules that cannot be configured.

4. **tigase.disco.XMPPService** - Objects using this interface can respond to "ServiceDiscovery" requests.

5. **tigase.stats.StatisticsContainer** - Objects using this interface can return runtime statistics. Any object can collect job statistics and implementing this interface guarantees that statistics will be presented in consisted way to user who wants to see them.

Instead of implementing above interfaces directly, it is recommended to extend one of existing abstract classes which take care of the most of "dirty and boring" stuff. Here is a list the most useful abstract classes:

-  **tigase.server.AbstractMessageReceiver** - Implements 4 basic interfaces:

   ``ServerComponent``, ``MessageReceiver``, ``Configurable`` and ``StatisticsContainer``. AbstractMessageReceiver also manages internal data queues using it’s own threads which prevents dead-locks from resource starvation. It offers even-driven data processing which means whenever packet arrives the ``abstract void processPacket(Packet packet);`` method is called to process it. You have to implement this abstract method in your component, if your component wants to send a packet (in response to data it received for example).

   .. code:: java

      boolean addOutPacket(Packet packet)

-  **tigase.server.ConnectionManager** - This is an extension of ``AbstractMessageReceiver`` abstract class. As the name says this class takes care of all network connection management stuff. If your component needs to send and receive data directly from the network (like c2s connection, s2s connection or external component) you should use this implementation as a basic class. It takes care of all things related to networking, I/O, reconnecting, listening on socket, connecting and so on. If you extend this class you have to expect data coming from to sources:

   From the ``MessageRouter`` and this is when the ``abstract void processPacket(Packet packet);`` method is called and second, from network connection and then the ``abstract Queue processSocketData(XMPPIOService serv);`` method is called.

Plug-ins
^^^^^^^^^

All Tigase plugins currently implemented are located in package: tigase.xmpp.impl. You can use this code as a sample code base. There are 3 types of plug-ins and they are defined in interfaces located in ``tigase.xmpp`` package:

1. **XMPPProcessorIfc** - The most important and basic plug-in. This is the most common plug-in type which just processes stanzas in normal mode. It receives packets, processes them on behalf of the user and returns resulting stanzas.

2. **XMPPPreprocessorIfc** - This plugin performs pre-processing of the packet, intended for the pre-processors to setup for packet blocking.

3. **XMPPPostprocessorIfc** - This plugin performs processing of packets for which there was no specific processor.

Connector
------------

Data, Stanzas, Packets - Data Flow and Processing
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Data received from the network are read from the network sockets as bytes by code in the ``tigase.io`` package. Bytes then are changed into characters in classes of ``tigase.net`` package and as characters they are sent to the XML parser (``tigase.xml``) which turns them to XML DOM structures.

All data inside the server is exchanged in XML DOM form as this is the format used by XMPP protocol. For basic XML data processing (parsing characters stream, building DOM, manipulate XML elements and attributes) we use `Tigase XML parser and DOM builder <https://github.com/tigase/tigase-xmltools>`__.

Each stanza is stored in the ``tigase.xml.Element`` object. Every Element can contain any number of **Child Elements** and any number of attributes. You can access all these data through the class API.

To simplify some, most common operations Element is wrapped in ``tigase.server.Packet`` class which offers another level of API for the most common operations like preparation of response stanza based on the element it contains (swap to/from values, put type=result attribute and others).

.. |Tigase architecture| image:: /images/devguide/tigase-architecture.svg
