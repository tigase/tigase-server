XEP-0191: Blocking Command
-------------------------------

The simplest security feature, however, inside an XMPP server is the ability to block users and JIDS. `XEP-0191 <http://xmpp.org/extensions/xep-0191>`__ specifies the parameters of simple blocking without using privacy lists. Below is a breakdown and some sample commands you may find helpful. To enable this feature, be sure the following is in your ``config.tdsl`` file:

::

   }
   'sess-man' {
       'urn:xmpp:blocking' () {}
   }

If you have other plugins running, then just add ``'urn:xmpp:blocking' () {}`` to the list to activate this feature.

To confirm if your installation of Tigase supports this feature, a quick disco#info of your server should reveal the following feature:

::

   <feature var='urn:xmpp:blocking'/>

Blocked users are stored on the server on a per-JID basis, so one user may only see his or her blocked JIDs. Lists of blocked JIDs will return as an IQ stanza with a list of <item> fields. To retrieve the blocklist, the following command is issued:

.. code:: xml

   <iq type='get' id='blockedjids'>
     <blocklist xmlns='urn:xmpp:blocking'/>
   </iq>

The server responds:

.. code:: xml

   <iq type='result' id='blockedjids'>
     <blocklist xmlns='urn:xmpp:blocking'>
       <item jid='user1@domain.net'/>
       <item jid='admin@example.com'/>
     </blocklist>
   </iq>

To block a JID, a similar stanza to the one above is sent to the server with the items of the blocked JIDs you wish to add:

.. code:: xml

   <iq from='admin@domain.net' type='set' id='block'>
     <block xmlns='urn:xmpp:blocking'>
       <item jid='user2@domain.net'/>
     </block>
   </iq>

The server will then push an unavailable presence to blocked contacts. Communication between a contact that is blocked, and an entity that blocked it will result in a <not-acceptable> error:

.. code:: xml

   <message type='error' from='user2@domain.net' to='admin@domain.net'>
     <body>Hello, are you online?</body>
     <error type='cancel'>
       <not-acceptable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>
       <blocked xmlns='urn:xmpp:blocking:errors'/>
     </error>
   </message>

Unblocking a contact is just as easy as blocking, send an unblock stanza to the server:

.. code:: xml

   <iq from='admin@domain.net' type='set' id='unblock'>
     <unblock xmlns='urn:xmpp:blocking'>
       <item jid='user2@domain.net'/>
     </unblock>
   </iq>

The server will begin pushing presence information to unblocked contacts and resources so long as permissions have not changed between.

You may also opt to unblock all contacts and essentially clear out your blocked list using the following command:

.. code:: xml

   <iq type='set' id='unblockall'>
     <unblock xmlns='urn:xmpp:blocking'/>
   </iq>

