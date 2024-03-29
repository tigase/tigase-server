Last Activity
----------------

Tigase XMPP Server supports `XEP-0012: Last Activity <https://xmpp.org/extensions/xep-0012.html>`__ extension, which allows retrieval information when particular contact was active last time. It’s not enabled by default.

The functionality itself is split in two plugins:

-  ``jabber:iq:last-marker`` - responsible for updating information about last activity of user

-  ``jabber:iq:last`` - responsible for handling requests to retrieve last activity information (it depends on ``jabber:iq:last-marker`` plugin).

In order to enable functionality you should add both plugins to your configuration file

.. code::

   'sess-man' {
       'jabber:iq:last-marker' (active: true) {
           'jabber:iq:last' (active: true) {}
       }
   }

What updates last activity
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

By default marker plugin will only update last activity information on presence stanza. It’s possible to control whether ``<presence/>`` and/or ``<message/>`` should update with respective options:

.. code::

   'sess-man' {
       'jabber:iq:last-marker' (active: true) {
           message = true
           presence = true
       }
   }

Those settings will cause updating last activity information for both ``<message/>`` and ``<presence/>`` stanzas

Persist everything to repository
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To lower impact on performance, by default last activity information is persisted to repository less frequently. This can yield slightly less accurate results on installations with multiple cluster nodes with users having multiple resources connected. To get more accurate results you should set ``persistAllToRepository`` to ``true``, which will cause all update times to be persisted (please bear in mind that this could cause higher impact on the repository).

.. code::

   'sess-man' {
       'jabber:iq:last-marker' (active: true) {
           persistAllToRepository = true
       }
   }