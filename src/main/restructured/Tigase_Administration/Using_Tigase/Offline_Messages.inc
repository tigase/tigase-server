Offline Messages
---------------------

Tigase like any XMPP server supports storing of messages for users who are offline so that they may receive messages sent to them while they were not logged in.

By default, Tigase ``MessageAmp`` processor is responsible for storing offline messages, and will automatically store offline messages. This guide has multiple sections for setting limits globally, per user, and others.

Many of the features listed here require the use of the Advanced Message Processor Plugin which is turned on by default. To ensure AMP is turned on your system, view your ``config.tdsl`` file and be sure the following is there in your plugins line:

.. code::

   'sess-man' {
       amp () {}
   }

Messages will be delivered to intended recipients when they first login after roster exchange.

Offline Message Limits
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Support for limiting number of stored offline messages on a per-user basis has now been added to Tigase as of v7.1.0. By default, Tigase comes with a limit of stored offline messages which is set for every user. This limit by default is 100 offline messages for barejid-barejid pair. This value can be changed by the ``store-limit`` property. To change to 200 messages on barejid-barejid paid, add the following entries to the ``config.tdsl`` file:

.. code::

   amp {
       'store-limit' = 200L
   }
   'sess-man' {
       amp () {
           'store-limit' = 200L
       }
   }

This setting applies to every user.

User Limit
~~~~~~~~~~~~~~~~~

Each user is able to configure the number of offline messages which should be stored for him. To enable this feature, the following lines need to be entered into the ``config.tdsl`` file:

.. code::

   amp {
       'user-store-limit-enable' = true
   }
   'sess-man' {
       amp () {
           'user-store-limit-enable' = true
       }
   }

Values of user-specific limits will be stored in UserRepository under subnode of ``offline-msgs`` and key ``store-limit``. Data storage will be stored in ``tig_pairs`` key with the value and a proper record from ``tig_nodes`` points to this record.

Handling of Offline Messages Exceeding Limits
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

There are two possible ways to handle offline messages that exceed the limitations: . ``error`` sending message with error type back to sender. . ``drop`` drop of message without notifications to sender.

By default, Tigase sends a message back to the original sender with an error type of ``service-unavailable`` with a proper description of error according to `XEP-0160 <http://www.xmpp.org/extensions/xep-0160.html>`__. However, it is possible to change this behavior to better suit your needs. This is done by adding the following line to your ``config.tdsl`` file.

.. code::

   'sess-man' {
       amp () {
           'quota-exceeded' = 'drop'
       }
   }

This will force Tigase to drop packets that exceed the offline message limit.

Setting the Limits by User
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Users wishing to set a custom limit of stored offline messages for barejid-barejid pairs needs to send the following XMPP stanza to the server:

.. code:: xml

   <iq type="set" id="${random-id}">
     <msgoffline xmlns="msgoffline" limit="${limit}"/>
   </iq>

Where: . ``${random-id}`` is a random ID of the stanza (can be any string). . ``${limit}`` is the integer value of the offline message limit. This can be set to ``false`` to disable offline message limits.

In response, the server will send back an ``iq`` stanza with a result type:

.. code:: xml

   <iq type="result" id="${random-id}">
     <msgoffline xmlns="msgoffline" limit="${limit}"/>
   </iq>

Example of Setting Limit of Stored Offline Messages to 10

XMPP client sends the following to the server:

.. code:: xml

   <iq type="set" id="aabba">
     <msgoffline xmlns="msgoffline" limit="10"/>
   </iq>

Server response:

.. code:: xml

   <iq type="result" id="aabba">
     <msgoffline xmlns="msgoffline" limit="10"/>
   </iq>

Example of Disabling Offline Message Limit

XMPP client sends the following to the server:

.. code:: xml

   <iq type="set" id="aabbb">
     <msgoffline xmlns="msgoffline" limit="false"/>
   </iq>

Server response:

.. code:: xml

   <iq type="result" id="aabbb">
     <msgoffline xmlns="msgoffline" limit="false"/>
   </iq>

Storing offline messages without body content
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Tigase can now store offline messages without ``<body/>`` content.

See `XEP-0334 <http://xmpp.org/extensions/xep-0334.html>`__ for protocol details.

This can include message receipts, and messages with specific ``do-not-store`` tags.

Support has been added to set a list of paths and xmlns to trigger and place storage of offline messages using the following settings in ``config.tdsl``:

.. code::

   'sess-man' {
       amp () {
           'msg-store-offline-paths' = [ '/message/received[urn:xmpp:receipts]', '/message/store-offline' ]
       }
   }

This example results in two settings:

``/message/received[urn:xmpp:receipts]``
   Results in storage of messages with a ``recieved`` subelement and with the xlmns set to ``urn:xmpp:receipts``

``/message/store-offline``
   Results in storing messages with a ``store-offline`` subelement without checking xmlns.

Filtering of offline storage
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

It is possible to set storage of other types to save:

.. code::

   'sess-man' {
       amp () {
           'msg-store-offline-paths' = [ '/message/store-offline', '-/message/do-not-store' ]
       }
   }

The above setting in the ``config.tdsl`` file will cause that:

-  messages with ``<store-offline>`` subelement will be stored without checking for associated xmlns.

-  messages with ``<do-not-store>`` element **will not** be saved.

Any of these can be adjusted for your installation, remember that a '-' will stop storage of messages with the indicated property. Messages will be checked by these matchers and if any of them result in a positive they will override default settings.

For example, if you wanted to store messages with <received> element, but not ones with <plain> element, your filter will look like this:

.. code::

   'sess-man' {
       amp () {
           'msg-store-offline-paths' = [ '/message/received', '-/message/plain' ]
       }
   }

However…​.

.. Note::

   **THE ABOVE STATEMENT WILL NOT WORK** As it will just store all messages with <received> subelement.

The below statement will properly filter your results.

.. code::

   'sess-man' {
       amp () {
           'msg-store-offline-paths' = [ '-/message/plain', '-/message/received' ]
       }
   }

Filtering logic is done in order from left to right. Matches on the first statement will ignore or override matches listed afterwards.

Disabling Offline Messages
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you wish to disable the storing of offline messages, use the following line in your ``config.tdsl`` file. This will not disable other features of the AMP plugin.

.. code::

   'sess-man' {
       amp () {
           msgoffline (active: false) {}
       }
   }