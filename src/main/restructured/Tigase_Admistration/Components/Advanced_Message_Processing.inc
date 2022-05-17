Advanced Message Processing - AMP XEP-0079
----------------------------------------------

Tigase server offers support for `XEP-0079: Advanced Message Processing <http://xmpp.org/extensions/xep-0079.html>`__ (often abbreviated to AMP).

It is enabled by default but there are several configuration options that you may tweak.

Configuration of AMP is not very complex, but as it is implemented as a component in the Tigase server it does needs a few settings to get it right.

Here is a first, brief overview of the AMP configuration and later detailed explanation of each parameter.

.. code:: dsl

   'sess-man' {
       amp () {
           'amp-jid' = 'amp@your-domain.tld'
       }
       message (active: false) {}
       msgoffline (active: false) {}
   }
   'amp-security-level' = 'STRICT'

First of all: plugins
^^^^^^^^^^^^^^^^^^^^^^^^^^

Even though the whole functionality is implemented inside the component you need a way to forward messages with ``AMP`` payload to that component. This is what the ``amp`` plugin does. The ``amp`` plugin intercepts all ``<message/>`` packets even without AMP payload, redirecting some of the to the ``AMP`` component and others processing in a standard way. Therefore you no longer need ``message`` plugin or ``msgoffline`` plugin. Those are all functions are offered by the ``amp`` plugin now. Hence you have to switch ``message`` and ``msgoffline`` plugins off (the ``amp`` plugin is loaded by default):

.. code:: dsl

   'sess-man' {
       amp () {}
       message (active: false) {}
       msgoffline (active: false) {}
   }

The ``amp`` plugin needs to know where to forward all the ``AMP`` packets. By default plugin uses hostname of the given machine as this is true to the most installations. However, this is configured by the last line of the example configuration, which forwards all packets to the address ``amp@your-domain.tld``:

.. code:: dsl

   'sess-man' {
       amp () {
           'amp-jid' = 'amp@your-domain.tld'
       }
   }

Secondly: component
^^^^^^^^^^^^^^^^^^^^^^^^^^

By default Tigase loads the component with the standard name ``amp``

Optional parameters
^^^^^^^^^^^^^^^^^^^^^^^^^^

There is also one parameter shared between the component and the plugin. Connection to the database where offline messages are stored. The AMP component has a dedicated schema for storing offline messages designed for a high traffic and high load installations. It does not use ``UserRepository`` for storing messages.

By default the same physical database as for ``UserRepository`` is used but you can change it and store messages in a completely separate location to reduce performance degradation of rest of the system. You can set a database connection string using following property:

.. code:: dsl

   dataSource {
       'default-amp' () {
           uri = 'jdbc:mysql://localhost/tigasedb?user=db_usr&password=db_pwd'
       }
   }

The `XEP-0079 <http://xmpp.org/extensions/xep-0079.html>`__ specification has a `Section 9. - Security Considerations <http://xmpp.org/extensions/xep-0079.html#security>`__. As it describes, in some cases the AMP protocol can be used to reveal user’s presence information by other users who are not authorized for presence updates. There are a few possible ways to prevent this.

Tigase’s implementation offers 3 modes to handle ``AMP`` requests to prevent revealing user’s status to non-authorized users:

.. code:: dsl

   'amp-security-level' = 'STRICT'

In this mode the server performs strict checking. The ``AMP`` specification is fully handled. This however involves roster loading for each offline user, hence it may impact the service performance. It may not be feasible or possible to run in this mode for services under a high load with lots of AMP messages.

In the XEP this mode is described in the following way:

*Accept the relevant condition only if the sender is authorized to receive the receiver’s presence, as a result of which the server MUST reply with a <not-acceptable/> error condition if the sender is not so authorized; this is the RECOMMENDED behavior. This is also the default in Tigase.*

.. code:: dsl

   'amp-security-level' = 'PERFORMANCE'

Dummy checking is performed efficiently by just returning an error response every time there is a chance that the default action may reveal user status without looking into the user’s roster. This does not affect performance but it does impact the ``AMP`` compliance.

In the XEP this mode is described in the following way:

*Accept the relevant condition only if the action is "drop", as a result of which the server MUST reply with a <not-acceptable/> error condition if the action is "alert", "error", or "notify"; this is slightly less restrictive but still unnecessarily restricts the functionality of the system, so is NOT RECOMMENDED.*

It does not do any checking. It acts like all users are authorized to receive notifications, even if it may reveal user status to unauthorized users. It does not impact the server performance and it offers full AMP compliance.

.. code:: dsl

   'amp-security-level' = 'NONE'
