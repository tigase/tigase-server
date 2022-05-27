Tigase Advanced Options
-----------------------------

This section is designed to include a number of advanced configuration options available within Tigase, but may not have a relevant section yet to house them.

Using CAPTCHA for in-band registration
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To reduce false or spam registrations to Tigase XMPP Server, there is now the ability to add CAPTCHA forms as a part of the in-band registration. The CAPTCHA will generate a random math equation and ask the user registering a new account to answer it. This may be enabled as a sub-option of enabling registration in config.tdsl:

.. code::

   'sess-man' {
       'jabber:iq:register' {
           captchaRequired = 'true'
       }
   }

.. **Note**::

   Because some clients do not refresh a registration form after an unsuccessful attempt, this option allows 3 retries with the same CAPTCHA.

3 unsuccessful attempts will result in the captcha being invalidated and a client will receive an error message.


Enabling Empty Nicknames
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Tigase can now support users with empty nicknames. This can be enabled by adding the following code in config.tdsl.

.. code::

   'sess-man' {
       'jabber:iq:roster' {
           empty_name_enabled = 'true'
       }
   }


Enable Silent Ignore on Packets Delivered to Unavailable Resources
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

You can now have Tigase ignore packets delivered to unavailable resources to avoid having a packet bounce around and create unnecessary traffic. You may set this globally, within standard message handling only, or within the AMP component using the following settings:

Globally:

.. code::

   'sess-man' {
       'silently-ignore-message' = 'true'
   }

Message Processing Only:

.. code::

   'sess-man' {
       message {
           'silently-ignore-message' = 'true'
       }
   }

AMP Component:

.. code::

   'sess-man' {
       amp () {
           'silently-ignore-message' = 'true'
   }

Mechanism to count errors within Tigase
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A new processor within statistics has been added to count the number of errors that Tigase returns. This processor, named ``error-counter``, will count all errors returned by Tigase, however by default the number is always zero if it is not enabled. It can be found as an MBean object in JMX under ``ErrorStatistics`` and contains values for packets with ``ERROR`` and grouped by type. To enable counting of these errors, you must ensure the processor is included in your ``sess-man`` configuration:

.. code::

   'sess-man' {
       'error-counter' () {}
   }

Including stream errors
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Stream ``ERROR`` packets are not included in the above counter by default as they are processed separately. To enable this to be added to the counter, the following line must be in your ``config.tdsl`` file.

.. code::

   c2s {
       'stream-error-counter' () {
           active = true
       }
   }

Stream resumption default & max-timeout
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

``SteamManagementIOProcessor`` now has a setting that can be used to change the maximum timeout time it will wait for reconnection if a client does not send a time to wait. Two settings are now available:

.. code::

   c2s {
       'urn:xmpp:sm:3' {
           'resumption-timeout' = 90
       }
   }

The above setting in ``config.tdsl`` file will change the default timeout period to 90 seconds.

.. code::

   c2s {
       'urn:xmpp:sm:3' {
           'max-resumption-timeout' = 900
       }
   }

This setting will set the maximum time allowed for stream resumption to 900 seconds. This can be handy if you expect a number of mobile phones to connect to your server and want to avoid duplicate messages being sent back and forth.

Automatic subscription approval
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You may setup a server to automatically approve presence subscriptions or roster authorizations for all users. Say you were hosting bots and wanted to automate the process. This can be done with the following settings:

.. code::

   'sess-man' () {
       'jabber:iq:roster' {
           'auto-authorize' = 'true'
       }
       'presence-subscription' () {
           'auto-authorize' = 'true'
       }
   }

Both of these settings are false by default, and you may use them together or separately.

.. **Note**::

   ``presence-subscription`` is current default plugin. If you are using old ``presence`` then you should configure the option with correct plugin name.

The following behavior is followed when they are both activated:

-  Upon sending a subscription request - Both contacts will each others' subscription and be added to each others' roster. Presence information will immediately be exchanged between both parties.

-  Upon sending presence with type either unsubscribe or unsubscribed follows the rules defined in RFC regarding processing of these stanzas (i.e. adjusting subscription type of user/contact), but without forwarding those stanzas to the receiving entity to avoid any notifications to the client. However, a roster push is generated to reflect changes to presence in user roster in a seamless manner.

-  Simply adding an item to the roster (i.e. with <iq/> stanza with correct semantics) will also cause an automatic subscription between the user and the contact in a matter explained above.

Abuse Contacts
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Tigase has support for `XEP-0128: Service Discovery Extensions <https://xmpp.org/extensions/xep-0128.html>`__ for providing additional information to the server and component discovery information. One of the important usages for this feature is `XEP-0157: Contact Addresses for XMPP Services <https://xmpp.org/extensions/xep-0157.html>`__ which describes usage of this feature for providing contact information to server administrators or abuse response team.

To set abuse contact details you should set ``disco-extensions`` in property in ``etc/config.tdsl`` file with subproperty ``abuse-addresses`` set to your abuse address URI (for email you need to prefix it with ``mailto:`` and for XMPP address you need to prefix it with ``xmpp``):

.. code::

   'disco-extensions' = {
       'abuse-addresses' = [ 'mailto:abuse@localhost', 'xmpp:abuse@localhost' ]
   }

Push Notifications
~~~~~~~~~~~~~~~~~~~~~~~~~~~

Tigase XMPP Server comes with support for `XEP-0357: Push Notifications <https://xmpp.org/extensions/xep-0357.html>`__ allowing user to receive notifications for messages received while his XMPP client is not connected enabled by default.


Disabling notifications

You can disable this feature with following settings:

.. code::

   'sess-man' {
       'urn:xmpp:push:0' (active: false) {}
   }

Removing body and sender from notifications

If you wish Tigase XMPP Server not to forward body of the message or sender details in the push notification you can disable that with following settings:

.. code::

   'sess-man' {
       'urn:xmpp:push:0' () {
           'with-body' = false
           'with-sender' = false
       }
   }

Overriding body of notifications

If you wish Tigase XMPP Server to override forward body of the encrypted message in the push notification (for example to avoid indicating that there is an "error") you can do that with following settings:

.. code::

   'sess-man' {
       'urn:xmpp:push:0' () {
           'encryptedMessageBody' = "You have a new secure message. Open to see the message"
       }
   }


Enabling push notifications for messages received when all resources are AWAY/XA/DND

Push notifications may also be sent by Tigase XMPP Server when new message is received and all resources of recipient are in AWAY/XA/DND state. To enable this type of notifications you need to enable additional push delivery extension named ``away`` in default push processor:

.. code::

   'sess-man' () {
       'urn:xmpp:push:0' () {
           'away' () {}
       }
   }

As this behaviour may not be expected by users and users need a compatible XMPP client to properly handle this notifications (XMPP client needs to retrieve message history from server to get actual message), in addition to enabling this plugin on the server, XMPP clients need to explicitly activate this feature. They can do that by including ``away`` attribute with value of ``true`` in push ``enable`` element send to the server, as in following example:

**Enabling Push notifications for away/xa/dnd account.**

.. code:: xml

   <iq type='set' id='x43'>
     <enable xmlns='urn:xmpp:push:0' away='true' jid='push-5.client.example' node='yxs32uqsflafdk3iuqo'>
       <x xmlns='jabber:x:data' type='submit'>
           ....
       </x>
     </enable>
   </iq>

If later on, user decides to disable notification for account in away/xa/dnd state, it may disable push notifications or once again send stanza to enable push notification but without ``away`` attribute being set:

.. code:: xml

   <iq type='set' id='x43'>
     <enable xmlns='urn:xmpp:push:0' away='true' jid='push-5.client.example' node='yxs32uqsflafdk3iuqo'>
       <x xmlns='jabber:x:data' type='submit'>
           ....
       </x>
     </enable>
   </iq>
