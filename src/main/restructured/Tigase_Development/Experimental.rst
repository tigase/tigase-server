Experimental
=============

The guide contains description of non-standard or experimental functionality of the server. Some of them are based on never published extensions, some of them are just test implementation for new ideas or performance improvements.

-  :ref:`Dynamic Rosters<dynamicRosters>`

-  :ref:`Mobile Optimizations<mobileoptimizations>`

-  :ref:`Bosh Session Cache<boshsessioncache>`


.. _dynamicRosters:

Dynamic Rosters
----------------

Problem Description
^^^^^^^^^^^^^^^^^^^^^^

Normal roster contacts stored and created as **dynamic roster parts** are delivered to the end user transparently. The XMPP client doesn’t really know what contacts come from its own **static** roster created manually by the user and what contacts come from a **dynamic** roster part; contacts and groups generated dynamically by the server logic.

Some specialized clients need to store extra bits of information about roster contacts. For the normal user **static** roster information can be stored as private data and is available only to this single user. In some cases however, clients need to store information about contacts from the dynamic roster part and this information must be available to all users accessing **dynamic** roster part.

The protocol defined here allows the exchange of information, saving and retrieving extra data about the contacts.

Syntax and Semantics
^^^^^^^^^^^^^^^^^^^^^^

Extra contact data is accessed using IQ stanzas, specifically by means of a child element qualified by the **jabber:iq:roster-dynamic** namespace. The child element MAY contain one or more children, each describing a unique contact item. Content of the element is not specified and is implementation dependent. From Tigase’s point of view it can contain any valid XML data. Whole element is passed to the DynamicRoster implementation class as is and without any verification. Upon retrieving the contact extra data the DynamicRoster implementation is supposed to provide a valid XML element with all the required data for requested **jid**.

The **jid** attribute specifies the Jabber Identifier (JID) that uniquely identifies the roster item. Inclusion of the **jid** attribute is **REQUIRED**.

Following actions on the extra contact data are allowed:

-  **set** - stores extra information about the contact

-  **get** - retrieves extra information about the contact

Retrieving Contact Data
^^^^^^^^^^^^^^^^^^^^^^^^^

Upon connecting to the server and becoming an active resource, a client can request the extra contact data. This request can be made either before or after requesting the user roster. The client’s request for the extra contact data is **OPTIONAL**.

Example: Client requests contact extra data from the server using **get** request:

.. code:: xml

   <iq type='get' id='rce_1'>
   <query xmlns='jabber:iq:roster-dynamic'>
   <item jid='archimedes@eureka.com'/>
   </query>
   </iq>

Example: Client receives contact extra data from the server, but there was either no extra information for the user, or the user was not found in the dynamic roster:

.. code:: xml

   <iq type='result' id='rce_1'>
   <query xmlns='jabber:iq:roster-dynamic'>
   <item jid='archimedes@eureka.com'/>
   </query>
   </iq>

Example: Client receives contact extra data from the server, and there was some extra information found about the contact:

.. code:: xml

   <iq type='result' id='rce_1'>
   <query xmlns='jabber:iq:roster-dynamic'>
   <item jid='archimedes@eureka.com'>
   <phone>+12 3234 322342</phone>
   <note>This is short note about the contact</note>
   <fax>+98 2343 3453453</fax>
   </item>
   </query>
   </iq>

Updating/Saving Extra Information About the Contact
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

At any time, a client **MAY** update extra contact information on the server.

Example: Client sends contact extra information using **set** request.

.. code:: xml

   <iq type='set' id='a78b4q6ha463'>
   <query xmlns='jabber:iq:roster-dynamic'>
   <item jid='archimedes@eureka.com'>
   <phone>+22 3344 556677</phone>
   <note>he is a smart guy, he knows whether the crown is made from pure gold or not.</note>
   </item>
   </query>
   </iq>

Client responds to the server:

.. code:: xml

   <iq type='result' id='a78b4q6ha463'/>

A client **MAY** update contact extra information for more than a single item in one request:

Example: Client sends contact extra information using **set** request with many <item/> elements.

.. code:: xml

   <iq type='set' id='a78b4q6ha464'>
   <query xmlns='jabber:iq:roster-dynamic'>
   <item jid='archimedes@eureka.com'>
   <phone>+22 3344 556677</phone>
   <note>he is a smart guy, he knows whether the crown is made from pure gold or not.</note>
   </item>
   <item jid='newton@eureka.com'>
   <phone>+22 3344 556688</phone>
   <note>He knows how heavy I am.</note>
   </item>
   <item jid='pascal@eureka.com'>
   <phone>+22 3344 556699</phone>
   <note>This guy helped me cure my sickness!</note>
   </item>
   </query>
   </iq>

Client responds to the server:

.. code:: xml

   <iq type='result' id='a78b4q6ha464'/>

Configuration
^^^^^^^^^^^^^^^^^^^^^^

DynamicRoster implementation class should be configured in the **config.tdsl** file:

.. code::

   'sess-man' () {
       'dynamic-rosters' () {
           class (class: package.custom.DynamicRosterImplementation) {}
       }
   }

If you want to pass configuration to your implementation simply use ``@ConfigField`` annotation on your variable (see :ref:`Component implementation - Lesson 2 - Configuration<cil2>` for more details).

.. _mobileoptimizations:

Mobile Optimizations
-----------------------------

Problem Description
^^^^^^^^^^^^^^^^^^^^^^^^

In default configuration stanzas are sent to the client when processing is finished, but in mobile environment sending or receiving data drains battery due to use of the radio.

To save energy data should be sent to client only if it is important or client is waiting for it.

Solution
^^^^^^^^^^

When mobile client is entering inactive state it notifies server about it by sending following stanza:

.. code:: xml

   <iq type="set" id="xx">
   <mobile
     xmlns="http://tigase.org/protocol/mobile#v3"
     enable="true"/>
   </iq>

After receiving stanza server starts queuing stanza which should be send to mobile client. What kind of queued stanzas depends on the plugins used and in case of **Mobile v3** presence stanzas are queued as well as message stanzas which are Message Carbons. Any other stanza (such as iq or plain message) is sent immediately to the client and every stanza from queue is also sent at this time.

When mobile client is entering active state it notifies server by sending following stanza:

.. code:: xml

   <iq type="set" id="xx">
   <mobile
     xmlns="http://tigase.org/protocol/mobile#v3"
     enable="false"/>
   </iq>

After receiving stanza server sends all queued stanzas to the client.

Also all stanzas from queue will be sent if number of stanzas in queue will reach queue size limit. By default this limit is set to 50.

Queuing Algorithms
^^^^^^^^^^^^^^^^^^^^^

There are three mobile optimization plugins for Tigase:

-  **Mobile v1** - all presence stanzas are kept in queue

-  **Mobile v2** - only last presence from each source is kept in queue

-  **Mobile v3** - only last presence from each source is kept in queue, also Message Carbons are queued

If you wish to activate you Mobile v1 plugin you need to send presented above with xmlns attribute value replaced with http://tigase.org/protocol/mobile#v1

If you wish to activate you Mobile v2 plugin you need to send presented above with xmlns attribute value replaced with http://tigase.org/protocol/mobile#v2

Configuration
^^^^^^^^^^^^^^^^^

Mobile plugins are not activated by default thus additional entry in the ``config.tdsl`` is required:

.. code::

   'sess-man' () {
       mobile_v1 () {}
   }

You may substitute ``mobile_v1`` with ``mobile_v2`` or ``mobile_v3`` depending on which algorithm you wish to use.

.. Note::

   USE ONLY ONE PLUGIN AT A TIME!

.. _boshsessioncache:

Bosh Session Cache
--------------------------------

Problem Description
^^^^^^^^^^^^^^^^^^^^^^^

Web clients have no way to store any data locally, on the client side. Therefore after a web page reload the web clients loses all the context it was running in before the page reload.

Some elements of the context can be retrieved from the server like the roster and all contacts presence information. Some other data however, can not be restored easily like opened chat windows and the chat windows contents. Even if the roster restoring is possible, this operation is very expensive in terms of time and resources on the server side.

On of possible solutions is to allow web client to store some data in the Bosh component cache on the server side for the time while the Bosh session is active. After the page reloads, if the client can somehow retrieve SID (stored in cookie or provided by the web application running the web client) it is possible to reload all the data stored in the Bosh cache to the client.

Bosh session context data are: roster, contacts presence information, opened chat windows, chat windows content and some other minor data. Ideally the web client should be able to store any data in the Bosh component cache it wants.


Bosh Session Cache Description
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The Bosh Session Cache is divided into 2 parts - automatic cache and dynamic cache.

The reason for splitting the cache into 2 parts is that some data can be collected automatically by the Bosh component and it would be very inefficient to require the client to store the data in the Bosh cache. The best example for such data is the Roster and contacts presence information.

-  **automatic cache** - is the cache part which is created automatically by the Bosh component without any interaction with the client. The client, however, can access the cache at any time. I would say this is a read-only cache but I don’t want to stop client from manipulating the cache if it needs. The client usually, only retrieves data from this part of the cache as all changes should be automatically updated by the Bosh component. The general idea for the automatic cache is that the data stored there are accessible in the standard XMPP form. So no extra code is needed for processing them.

-  **dynamic cache** - is the cache part which is or can be modified at any time by the client. Client can store, retrieve, delete and modify data in this part of the cache.

Cache Protocol
^^^^^^^^^^^^^^^^^^^^^^^

All the Bosh Session Cache actions are executed using additional ``<body/>`` element attributes: ``cache`` and ``cache-id``. Attribute cache stores the action performed on the Bosh ``cache`` and the ``cache-id`` attribute refers to the ``cache`` element if the action attribute needs it. ``cache-id`` is optional. There is a default cache ID (empty one) associated with the elements for which the ``cache-id`` is not provided.

If the ``<body/>`` element contains the cache attribute it means that all data included in the ``<body/>`` refer to the cache action. It is not allowed, for example to send a message in the body and have the cache action set to **get**. The ``<body/>`` element with cache action **get**, **get_all**, **on**, **off**, **remove** must be empty. The ``<body/>`` element with actions **set** or **add** must contain data to store in the cache.

Cache Actions
~~~~~~~~~~~~~~~

-  **on** or **off** - the client can switch the cache on or off at any time during the session. It is recommended, however that the client switches the cache **on** in the first body packet, otherwise some information from the automatic cache may be missing. The automatic cache is created from the stream of data passing the Bosh component. Therefore if the cache is switched on after the roster retrieval is completed then the roster information will be missing in the cache. If the cache is set to **off** (the default value) all requests to the cache are ignored. This is to ensure backward compatibility with the original Bosh specification and to make sure that in a default environment the Bosh component doesn’t consume any extra resources for cache processing and storing as the cache wouldn’t be used by the client anyway.

-  **get** - retrieves the cache element pointing by the cache-id from the Bosh cache. Note there is no **result** cache action. The ``<body/>`` sent as a response from the server to the client may contain cache results for a given cache-id and it may also contain other data received by the Bosh component for the client. It may also happen that large cached data are split into a few parts and each part can be sent in a separate ``<body/>`` element. It may usually happen for the Roster data.

-  **get_all** - retrieves all the elements kept in the Bosh cache. That action can can be performed after the page reload. The client doesn’t have to request every single cached item one by one. It can retrieve all cache items in one go. It doesn’t mean however the whole cache is sent to the client in a single ``<body/>`` element. The cache content will be divided into a smaller parts of a reasonable size and will be sent to the client in a separate ``<body/>`` elements. It may also happen that the **``<body/>``** element contain the cache elements as well as the new requests sent to the user like new messages or presence information.

-  **set** - sends data to the Bosh Session cache for later retrieval. The client can store any data it wants in the cache. The Bosh components stores in the cache under the selected ID all the data inside the ``<body/>`` element. The only restriction is that the cached data must be a valid XML content. The data are returned to the client in exactly the same form as they were received from the server. The **set** action replaces any previously stored data under this ID.

-  **add** - adds new element to the cache under the given ID. This action might be useful for storing data for the opened chat window. The client can add new elements for the chat window, like new messages, icons and so on…​

-  **remove** - removes the cached element for the given cache ID.

Cache ID
~~~~~~~~~~~~

Cache ID can be an any character string. There might be some IDs reserved for a special cases, like for the Roster content. To avoid any future ID conflicts reserved ID values starts with: **bosh** - string.

There is a default cache ID - en empty string. Thus cache-id attribute can be omitted and then the requests refers to data stored under the default (empty) ID.

Reserved Cache ID Names
~~~~~~~~~~~~~~~~~~~~~~~~~

Here is a list of reserved Cache IDs:

-  **bosh-roster** - The user roster is cached in the Bosh component in exactly the same form as it was received from the core server. The Bosh Cache might or might not do optimizations on the roster like removing elements from the cached roster if the roster **remove** has been received or may just store all the roster requests and then send them all to the client. There is a one mandatory optimization the Bosh Cache must perform. It must remember the last (and only the last) presence status for each roster item. Upon roster retrieving from the cache the Bosh component must send the roster item first and then the presence for the item. If the presence is missing it means an offline presence. If the roster is small it can be sent to the client in a single packet but for a large roster it is recommended to split contact lists to batches of max 100 elements. The Bosh component may send all roster contacts first and then all presences or it can send a part of the roster, presences for sent items, next part of the roster, presences for next items and so on.

-  **bosh-resource-bind** - The user resource bind is also cached to allow the client quickly retrieve information about the full JID for the established Bosh session.
