Plugin Development
===================

This is a set of documents explaining details what is a plugin, how they are designed and how they work inside the Tigase server. The last part of the documentation explains step by step creating the code for a new plugin.

-  `Writing Plugin Code <#writePluginCode>`__

-  `Plugin Configuration <#pluginConf>`__

-  `How Packets are Processed by the SM and Plugins <#packetprocess>`__

-  `SASL Custom Mechanisms and Configuration <#saslcmac>`__

.. _writePluginCode:

Writing Plugin Code
---------------------

Stanza processing takes place in 4 steps. A different kind of plugin is responsible for each step of processing:

1. `XMPPPreprocessorIfc <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/xmpp/XMPPPreprocessorIfc.java>`__ - is the interface for packets pre-processing plugins.

2. `XMPPProcessorIfc <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/xmpp/XMPPProcessor.java>`__ - is the interface for packets processing plugins.

3. `XMPPPostprocessorIfc <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/xmpp/XMPPPostprocessorIfc.java>`__ - is the interface for packets post-processing plugins.

4. `XMPPPacketFilterIfc <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/xmpp/XMPPPacketFilterIfc.java>`__ - is the interface for processing results filtering.

If you look inside any of these interfaces you will only find a single method. This is where all the packet processing takes place. All of them take a similar set of parameters and below is a description for all of them:

-  **Packet packet** - packet is which being processed. This parameter may never be null. Even though this is not an immutable object it mustn’t be altered. None of it’s fields or attributes can be changed during processing.

-  **XMPPResourceConnection session** - user session which keeps all the user session data and also gives access to the user’s data repository. It allows for the storing of information in permanent storage or in memory only during the life of the session. This parameter can be null if there is no online user session at the time of the packet processing.

-  **NonAuthUserRepository repo** - this is a user data storage which is normally used when the user session (parameter above) is null. This repository allows for a very restricted access only. It allows for storing some user private data (but doesn’t allow overwriting existing data) like messages for offline users and it also allows for reading user public data like VCards.

-  **Queue<Packet> results** - this a collection with packets which have been generated as input packet processing results. Regardless a response to a user request is sent or the packet is forwarded to it’s destination it is always required that a copy of the input packet is created and stored in the **results** queue.

-  **Map<String, Object> settings** - this map keeps plugin specific settings loaded from the Tigase server configuration. In most cases it is unused, however if the plugin needs to access an external database that this is a way to pass the database connection string to the plugin.

After a closer look in some of the interfaces you can see that they extend another interface: `XMPPImplIfc <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/xmpp/XMPPImplIfc.java>`__ which provides a basic meta information about the plugin implementation. Please refer to `JavaDoc <http://docs.tigase.org/tigase-server/snapshot/javadoc/tigase/xmpp/impl/package-summary.html>`__ documentation for all details.

For purpose of this guide we are implementing a simple plugin for handling all **<message/>** packets that is forwarding packets to the destination address. Incoming packets are forwarded to the user connection and outgoing packets are forwarded to the external destination address. This `message plugin <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/xmpp/impl/Message.java>`__ is actually implemented already and it is available in our Git repository. The code has some comments inside already but this guide goes deeper into the implementation details.

First of all you have to choose what kind of plugin you want to implement. If this is going to be a packet processor you have to implement the **XMPPProcessorIfc** interface, if this is going to be a pre-processor then you have to implement the **XMPPPreprocessorIfc** interface. Of course your implementation can implement more than one interface, even all of them. There are also two abstract helper classes, one of which you should use as a base for all you plugins **XMPPProcessor** or use **AnnotatedXMPPProcessor** for annotation support.

Using annotation support
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The class declaration should look like this (assuming you are implementing just the packet processor):

.. code:: java

   public class Message extends AnnotatedXMPPProcessor
      implements XMPPProcessorIfc

The first thing to create is the plugin **ID**. This is a unique string which you put in the configuration file to tell the server to load and use the plugin. In most cases you can use XMLNS if the plugin wants packets with elements with a very specific name space. Of course there is no guarantee there is no other packet for this specific XML element too. As we want to process all messages and don’t want to spend whole day on thinking about a cool ID, let’s say our ID is: *message*.

A plugin informs about it’s presence using a static **ID** field and **@Id** annotation placed on class:

.. code:: java

   @Id(ID)
   public class Message extends AnnotatedXMPPProcessor
      implements XMPPProcessorIfc {
     protected static final String ID = "message";
   }

As mentioned before, this plugin receives only this kind of packets for processing which it is interested in. In this example, the plugin is interested only in packets with **<message/>** elements and only if they are in the "**jabber:client**" namespace. To indicate all supported elements and namespaces we have to add 2 more annotations:

.. code:: java

   @Id(ID)
   @Handles({
     @Handle(path={ "message" },xmlns="jabber:client")
   })
   public class Message extends AnnotatedXMPPProcessor
      implements XMPPProcessorIfc {
     private static final String ID = "message";
   }

Using older non-annotation based implementation
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The class declaration should look like this (assuming you are implementing just the packet processor):

.. code:: java

   public class Message extends XMPPProcessor
      implements XMPPProcessorIfc

The first thing to create is the plugin **ID** like above.

A plugin informs about it’s ID using following code:

.. code:: java

   private static final String ID = "message";
   public String id() { return ID; }

As mentioned before this plugin receives only this kind of packets for processing which it is interested in. In this example, the plugin is interested only in packets with **<message/>** elements and only if they are in "**jabber:client**" namespace. To indicate all supported elements and namespaces we have to add 2 more methods:

.. code:: java

   public String[] supElements() {
     return new String[] {"message"};
   }

   public String[] supNamespaces() {
     return new String[] {"jabber:client"};
   }

Implementation of processing method
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Now we have our plugin prepared for loading in Tigase. The next step is the actual packet processing method. For the complete code, please refer to the plugin in the Git. I will only comment here on elements which might be confusing or add a few more lines of code which might be helpful in your case.

.. code:: java

   @Override
   public void process(Packet packet, XMPPResourceConnection session,
       NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings)
       throws XMPPException {

       // For performance reasons it is better to do the check
       // before calling logging method.
       if (log.isLoggable(Level.FINEST)) {
           log.log(Level.FINEST, "Processing packet: {0}", packet);
       }

       // You may want to skip processing completely if the user is offline.
       if (session == null) {
           return;
       }    // end of if (session == null)

       try {

           // Remember to cut the resource part off before comparing JIDs
           BareJID id = (packet.getStanzaTo() != null) ? packet.getStanzaTo().getBareJID() : null;

           // Checking if this is a packet TO the owner of the session
           if (session.isUserId(id)) {

               // Yes this is message to 'this' client
               Packet result = packet.copyElementOnly();

               // This is where and how we set the address of the component
               // which should receive the result packet for the final delivery
               // to the end-user. In most cases this is a c2s or Bosh component
               // which keep the user connection.
               result.setPacketTo(session.getConnectionId(packet.getStanzaTo()));

               // In most cases this might be skipped, however if there is a
               // problem during packet delivery an error might be sent back
               result.setPacketFrom(packet.getTo());

               // Don't forget to add the packet to the results queue or it
               // will be lost.
               results.offer(result);

               return;
           }    // end of else

           // Remember to cut the resource part off before comparing JIDs
           id = (packet.getStanzaFrom() != null) ? packet.getStanzaFrom().getBareJID() : null;

           // Checking if this is maybe packet FROM the client
           if (session.isUserId(id)) {

               // This is a packet FROM this client, the simplest action is
               // to forward it to its destination:
               // Simple clone the XML element and....
               // ... putting it to results queue is enough
               results.offer(packet.copyElementOnly());

               return;
           }

           // Can we really reach this place here?
           // Yes, some packets don't even have from or to address.
           // The best example is IQ packet which is usually a request to
           // the server for some data. Such packets may not have any addresses
           // And they usually require more complex processing
           // This is how you check whether this is a packet FROM the user
           // who is owner of the session:
           JID jid = packet.getFrom();

           // This test is in most cases equal to checking getStanzaFrom()
           if (session.getConnectionId().equals(jid)) {

               // Do some packet specific processing here, but we are dealing
               // with messages here which normally need just forwarding
               Element el_result = packet.getElement().clone();

               // If we are here it means FROM address was missing from the
               // packet, it is a place to set it here:
               el_result.setAttribute("from", session.getJID().toString());

               Packet result = Packet.packetInstance(el_result, session.getJID(),
                   packet.getStanzaTo());

               // ... putting it to results queue is enough
               results.offer(result);
           }
       } catch (NotAuthorizedException e) {
           log.warning("NotAuthorizedException for packet: " + packet);
           results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
                   "You must authorize session first.", true));
       }    // end of try-catch
   }

.. _pluginConf:

Plugin Configuration
-----------------------

Plugin configuration is straightforward.

Tell the Tigase server to load or not to load the plugins via the ``config.tdsl`` file. Plugins fall within the ``'sess-man'`` container. To activate a plugin, simply list it among the sess-man plugins.

If you do not wish to use this method to find out what plugins are running, there are two ways you can identify if a plugin is running. One is the log file: logs/tigase-console.log. If you look inside you can find following output:

.. code:: bash

   Loading plugin: jabber:iq:register ...
   Loading plugin: jabber:iq:auth ...
   Loading plugin: urn:ietf:params:xml:ns:xmpp-sasl ...
   Loading plugin: urn:ietf:params:xml:ns:xmpp-bind ...
   Loading plugin: urn:ietf:params:xml:ns:xmpp-session ...
   Loading plugin: roster-presence ...
   Loading plugin: jabber:iq:privacy ...
   Loading plugin: jabber:iq:version ...
   Loading plugin: http://jabber.org/protocol/stats ...
   Loading plugin: starttls ...
   Loading plugin: vcard-temp ...
   Loading plugin: http://jabber.org/protocol/commands ...
   Loading plugin: jabber:iq:private ...
   Loading plugin: urn:xmpp:ping ...

and this is a list of plugins which are loaded in your installation.

Another way is to look inside the session manager source code which has the default list hardcoded:

.. code:: java

   private static final String[] PLUGINS_FULL_PROP_VAL =
     {"jabber:iq:register", "jabber:iq:auth", "urn:ietf:params:xml:ns:xmpp-sasl",
      "urn:ietf:params:xml:ns:xmpp-bind", "urn:ietf:params:xml:ns:xmpp-session",
      "roster-presence", "jabber:iq:privacy", "jabber:iq:version",
      "http://jabber.org/protocol/stats", "starttls", "msgoffline",
      "vcard-temp", "http://jabber.org/protocol/commands", "jabber:iq:private",
      "urn:xmpp:ping", "basic-filter", "domain-filter"};

In you wish to load a plugin outside these defaults, you have to edit the list and add your plugin IDs as a value to the plugin list under 'sess-man'. Let’s say our plugin ID is **message** as in our all examples:

.. code:: bash

   'sess-man' () {
       'jabber:iq:register' () {}
       'jabber:iq:auth' () {}
       message () {}
   }

Assuming your plugin class is in the classpath it will be loaded and used at the runtime. You may specify class by adding ``class: class.implementing.plugin`` within the parenthesis of the plugin.

.. Note::

   If your plugin name has any special characters (-,:\|/.) it needs to be encapsulated in single quotation marks.

There is another part of the plugin configuration though. If you looked at the `Writing Plugin Code <#writePluginCode>`__ guide you can remember the **Map settings** processing parameter. This is a map of properties you can set in the configuration file and these setting will be passed to the plugin at the processing time.

Again **config.tdsl** is the place to put the stuff. These kind of properties start under your **plugin ID** and each key and value will be a child underneath:

.. code::

   'sess-man' () {
       pluginID {
         key1 = 'val1'
         key2 = 'val2'
         key3 = 'val3'
       }
   }

.. Note::

   From v8.0.0 you will no longer be able to specify one value for multiple keys, you must set each one individually.

Last but not least - in case you have **omitted plugin ID**:

.. code:: bash

   'sess-man' () {
       key1 = 'val1'
   }

then the configured key-value pair will be a global/common plugin setting available to all loaded plugins.

.. _packetprocess:

How Packets are Processed by the SM and Plugins
---------------------------------------------------

For Tigase server plugin development it is important to understand how it all works. There are different kind of plugins responsible for processing packets at different stages of the data flow. Please read the introduction below before proceeding to the actual coding part.

Introduction
^^^^^^^^^^^^^^^^^

In Tigase server **plugins** are pieces of code responsible for processing particular XMPP stanzas. A separate plugin might be responsible for processing messages, a different one for processing presences, a separate plugins responsible for iq roster, and a different one for iq version and so on.

A plugin provides information about what exact XML element(s) name(s) with xmlns it is interested in. So you can create a plugin which is interested in all packets containing caps child.

There might be no plugin for a particular stanza element, in this case the default action is used which is simple forwarding stanza to a destination address. There might be also more than one plugin for a specific XML element and then they all process the same stanza simultaneously in separate threads so there is no guarantee on the order in which the stanza is processed by a different plugins.

Each stanza goes through the Session Manager component which processes packets in a few steps. Have a look at the picture below:

|Consumer|

The picture shows that each stanza is processed by the session manager in 4 steps:

1. Pre-processing - All loaded pre-processors receive the packet for processing. They work within session manager thread and they have no internal queue for processing. As they work within Session Manager thread it is important that they limit processing time to absolute minimum as they may affect the Session Manager performance. The intention for the pre-processors is to use them for packet blocking. If the pre-processing result is 'true' then the packet is blocked and no further processing is performed.

2. Processing - This is the next step the packet gets through if it wasn’t blocked by any of the pre-processors. It gets inserted to all processors queues with requested interest in this particular XML element. Each processor works in a separate thread and has own internal fixed size processing queue.

3. Post-processing - If there is no processor for the stanza then the packet goes through all post-processors. The last post-processor that is built into session manager post-processor tries to apply a default action to a packet which hasn’t been processed in step 2. Normally the default action is just forwarding the packet to a destination. Most commonly it is applied to <message/> packets.

4. Finally, if any of above 3 steps produced output/result packets all of them go through all filters which may or may not block them.

An important thing to note is that we have two kinds or two places where packets may be blocked or filtered out. One place is before packet is processed by the plugin and another place is after processing where filtering is applied to all results generated by the processor plugins.

It is also important to note that session manager and processor plugins act as packet consumers. The packet is taken for processing and once processing is finished the packet is destroyed. Therefore to forward a packet to a destination one of the processor must create a copy of the packet, set all properties and attributes and return it as a processing result. Of course processor can generate any number of packets as a result. Result packets can be generated in any of above 4 steps of the processing. Have a look at the picture below:

|User Send to Comp|

If the packet P1 is sent from outside of the server, for example to a user on another server or to some component (MUC, PubSub, transport), then one of the processor must create a copy (P2) of the packet and set all attributes and destination addresses correctly. Packet P1 has been consumed by the session manager during processing and a new packet has been generated by one of the plugins.

The same of course happens on the way back from the component to the user:

|Comp Sends to User|

The packet from the component is processed and one of the plugins must generate a copy of the packet to deliver it to the user. Of course packet forwarding is a default action which is applied when there is no plugin for the particular packet.

It is implemented this way because the input packet P1 can be processed by many plugins at the same time therefore the packet should be in fact immutable and must not change once it got to the session manager for processing.

The most obvious processing work flow is when a user sends request to the server and expects a response from the server:

|User Request Response|

This design has one surprising consequence though. If you look at the picture below showing communication between 2 users you can see that the packet is copied twice before it is delivered to a final destination:

|User Sends to User|

The packet has to be processed twice by the session manager. The first time it is processed on behalf of the User A as an outgoing packet and the second time it is processed on behalf of the User B as an incoming packet.

This is to make sure the User A has permission to send a packet out and all processing is applied to the packet and also to make sure that User B has permission to receive the packet and all processing is applied. If, for example, the User B is offline there is an offline message processor which should send the packet to a database instead of User B.

.. |Consumer| image:: /images/devguide/sm-consumer.png
.. |User Send to Comp| image:: /images/devguide/user-sends-to-comp.png
.. |Comp Sends to User| image:: /images/devguide/comp-sends-to-user.png
.. |User Request Response| image:: /images/devguide/user-request-response.png
.. |User Sends to User| image:: /images/devguide/user-sends-to-user.png

.. _saslcmac:

SASL Custom Mechanisms and Configuration
----------------------------------------------

**This API is available from Tigase XMPP Server version 5.2.0 or later on our current master branch.**

**In version 8.0.0 there was a major change to the API and configuration of custom SASL mechanisms.**

*Note that API is under active development. This description may be updated at any time.*

Basic SASL Configuration
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

SASL implementation in Tigase XMPP Server is compatible with Java API, the same exact interfaces are used.

The SASL implementation consists of following parts:

1. mechanism

2. CallbackHandler

Mechanisms Configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To add a new mechanism, a new factory for the mechanism has to be implemented and registered.

The simplest way to add register a new factory is to annotate its class with ``@Bean`` annotation:

**Example of the registration of a SASL mechanism factory with an annotation setting id of the factory to ``customSaslFactory``.**

.. code:: java

   @Bean(name="customSaslFactory", parent = TigaseSaslProvider.class, active = true)
   public class OwnFactory implements SaslServerFactory {}

It can also be done by specifying the class directly for bean ``customSaslFactory`` in the ``config.tdsl`` file like in the example below:

**Example of the registration of a SASL mechanism factory with TDSL setting id of the factory to ``customSaslFactory``.**

.. code::

   'sess-man' () {
       'sasl-provider' () {
           customSaslFactory(class: com.example.OwnFactory) {}
       }
   }

The class must implement the ``SaslServerFactory`` interface and has public constructor without any arguments. All mechanisms returned by ``getMechanismNames()`` method will be registered automatically.

The default factory that is available and registered by default is ``tigase.auth.TigaseSaslServerFactory`` which provides ``PLAIN``, ``ANONYMOUS``, ``EXTERNAL``, ``SCRAM-SHA-1``, ``SCRAM-SHA-256`` and ``SCRAM-SHA-512`` mechanisms.

CallbackHandler Configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The ``CallbackHandler`` is a helper class used for loading/retrieving authentication data from data repository and providing them to a mechanism.

To register a new callback handler you need to create a new class extending ``tigase.auth.CallbackHandlerFactory`` (if you wish to keep existing SASL callback handlers) or implementing ``tigase.auth.CallbackHandlerFactoryIfc``. You will need to override ``create()`` method to return an instance of your custom ``CallbackHandler`` when appropriate.

Next you need to register new implementation of ``CallbackHandlerFactoryIfc``. The ``config.tdsl`` file should include:

.. code::

   'sess-man' () {
       'sasl-provider' () {
           callback-handler-factory(class: com.example.OwnCallbackHandlerFactory) {}
       }
   }

During the authentication process, Tigase server always checks for asks callback handler factory for specific handler to selected mechanisms, and if there is no specific handler the default one is used.

Selecting Mechanisms Available in the Stream
'''''''''''''''''''''''''''''''''''''''''''''

The ``tigase.auth.MechanismSelector`` interface is used for selecting mechanisms available in a stream. Method ``filterMechanisms()`` should return a collection with mechanisms available based on:

1. all registered SASL factories

2. XMPP session data (from ``XMPPResourceConnection`` class)

The default selector returns mechanisms from all mechanism factories registered in ``sasl-provider`` ``(TigaseSaslProvider)``.

It is possible to use a custom selector by specifying it’s class int the ``config.tdsl`` file:

.. code::

   'sess-man' () {
       'sasl-provider' () {
           'mechanism-selector'(class: com.example.OwnSelector) {}
       }
   }

Logging/Authentication
^^^^^^^^^^^^^^^^^^^^^^^^^

After the XMPP stream is opened by a client, the server checks which SASL mechanisms are available for the XMPP session. Depending on whether the stream is encrypted or not, depending on the domain, the server can present different available authentication mechanisms. ``MechanismSelector`` is responsible for choosing mechanisms. List of allowed mechanisms is stored in the XMPP session object.

When the client/user begins the authentication procedure it uses one particular mechanism. It must use one of the mechanisms provided by the server as available for this session. The server checks whether mechanisms used by the client is on the list of allowed mechanisms. It the check is successful, the server creates ``SaslServer`` class instance and proceeds with exchanging authentication information. Authentication data is different depending on the mechanism used.

When the SASL authentication is completed without any error, Tigase server should have authorized user name or authorized BareJID. In the first case, the server automatically builds user’s JID based on the domain used in the stream opening element in ``to`` attribute.

If, after a successful authentication, method call: ``getNegotiatedProperty("IS_ANONYMOUS")`` returns ``Boolean.TRUE`` then the user session is marked as anonymous. For valid and registered users this can be used for cases when we do not want to load any user data such as roster, vcard, privacy lists and so on. This is a performance and resource usage implication and can be useful for use cases such as support chat. The authorization is performed based on the client database but we do not need to load any XMPP specific data for the user’s session.

More details about implementation can be found in the `custom mechanisms development <#cmd>`__ section.

Custom Mechanisms Development
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Mechanism**
~~~~~~~~~~~~~~~~~~

``getAuthorizationID()`` method from ``SaslServer`` class **should** return bare JID authorized user. In case that the method returns only user name such as **romeo** for example, the server automatically appends domain name to generate a valid BareJID: *romeo@example.com*. In case the method returns a full, valid BareJID, the server does not change anything.

``handleLogin()`` method from ``SessionManagerHandler`` will be called with user’s Bare JID provided by ``getAuthorizationID()`` (or created later using stream domain name).

**CallbackHandler**
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

For each session authorization, the server creates a new and separate empty handler. Factory which creates handler instance allows to inject different objects to the handler, depending on interfaces implemented by the handler class:

-  ``AuthRepositoryAware`` - injects ``AuthRepository;``

-  ``DomainAware`` - injects domain name within which the user attempts to authenticate

-  ``NonAuthUserRepositoryAware`` - injects ``NonAuthUserRepository``

General Remarks
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

``JabberIqAuth`` used for non-SASL authentication mechanisms uses the same callback as the SASL mechanisms.

Methods ``auth`` in ``Repository`` interfaces will be deprecated. These interfaces will be treated as user details providers only. There will be new methods available which will allow for additional login operations on the database such as last successful login recording.
