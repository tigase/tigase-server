Tigase as an External Component
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

There are cases when you want to deploy one or more Tigase components separately from the main server, or perhaps you want to run some Tigase components connecting to a different XMPP server, or perhaps you work on a component and you do not want to restart the main server every time you make a change.

There is a way to run the Tigase server in *external component mode*. In fact you can run any of Tigase’s components as an external component and connect them to the main XMPP server either via `XEP-0114 <http://xmpp.org/extensions/xep-0114.html>`__ or `XEP-0225 <http://xmpp.org/extensions/xep-0225.html>`__ connection.

Let’s look at the examples…​

Usage with shared database (since version 8.0.0)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

When you are using Tigase server 8.0.0 or newer in the "external component mode" while using shared default "user repository" and you have main server also running Tigase XMPP Server 8.0.0 or newer, then you can benefit from the remote management of the component connections from the main server. To use that, you need to enable external component and external component manager on the main server by adding following line to the config file:

.. code:: dsl

   'ext' () {}
   'ext-man' () {}

With that in place you can use Admin UI or ad-hoc commands available at ``ext-man`` component of the main server to configure connection details of the servers running in the ``component`` mode.

In Admin UI you click on ``Configuration`` section and select ``Add new item`` at the ``ext-man`` component, which will present you with a following form to fill in external component connectivity details:

|adminui extman add item form|


A Simple Case - MUC as an External Component

A few assumptions:

1. We want to run a MUC component for a domain: ``muc.devel.tigase.org`` and password ``muc-pass``

2. The main server works at an address: devel.tigase.org and for the same virtual domain

3. We want to connect to the server using `XEP-0114 <http://xmpp.org/extensions/xep-0114.html>`__ protocol and port ``5270``.

There is a special configuration type for this case which simplifies setting needed to run Tigase as an external component:

.. code:: dsl

   'config-type' = 'component'

Knowing that we can now create simple configuration file for Tigase XMPP Server:

.. code:: dsl

   admins = [ 'admin@devel.tigase.org' ]
   'config-type' = 'component'
   debug = [ 'server' ]
   'default-virtual-host' = [ 'devel.tigase.org' ]
   dataSource {
       default () {
           uri = 'master_server_default_database_url'
       }
   }
   userRepository {
       default () {}
   }
   authRepository {
       default () {}
   }
   muc (class: tigase.muc.MUCComponent) {}
   ext () {
   }

where ``master_server_default_database_url`` is the same URL as the one used on the main server for default data source.

With that in place we can use ad-hoc commands or Admin UI on the main server to configure Tigase XMPP Server to accept external component connections and to connect from the external component to the master server.

**Adding external component connection settings to the manager (ext-man) using Admin UI.**

|adminui extman add item form external muc|

You need to pass:

-  Domain name - external component domain name (``muc.devel.tigase.org``)

-  Domain password - password for authentication of the external component connection (``muc-pass``)

-  Connection type - ``accept`` to make component wait for connection or ``connect`` force component to connect to the server (``connect``)

-  Port number - port on which component should wait for connection or on which it try to connect (``5270``)

-  Remote host - host to connect to (``devel.tigase.org``)

-  Protocol - id of protocol used for establishing connection

   -  ``XEP-0114: Jabber Component Protocol (accept)`` - establish connection using `XEP-0114: Jabber Component Protocol <https://xmpp.org/extensions/xep-0114.html>`__

   -  ``XEP-0225: Component Connections`` - establish connection using `XEP-0225: Component Connections <https://xmpp.org/extensions/xep-0225.html>`__

Additional options may be left with defaults.

More Components

Suppose you want to run more than one component as an external components within one Tigase instance. Let’s add another - PubSub component to the configuration above and see how to set it up.

The most straightforward way is just to add another component to the server running in the component mode for the component domain

.. code:: dsl

   admins = [ 'admin@devel.tigase.org' ]
   'config-type' = 'component'
   debug = [ 'server' ]
   'default-virtual-host' = [ 'devel.tigase.org' ]
   dataSource {
       default () {
           uri = 'jdbc:derby:/tigasedb'
       }
   }
   userRepository {
       default () {}
   }
   authRepository {
       default () {}
   }
   muc (class: tigase.muc.MUCComponent) {}
   pubsub (class: tigase.pubsub.PubSubComponent) {}
   ext () {}

and then to add new connection domain to the main server external component settings and to the external component manager settings. You basically do the same thing as you did while adding only MUC component as the external component.

Please note however that we are opening two connections to the same server. This can waste resources and over-complicate the system. For example, what if we want to run even more components? Opening a separate connection for each component is a tad overkill.

In fact there is a way to reuse the same connection for all component domains running as an external component. The property ``bind-ext-hostnames`` contains a comma separated list of all hostnames (external domains) which should reuse the existing connection.

There is one catch however. Since you are reusing connections (hostname binding is defined in `XEP-0225 <http://xmpp.org/extensions/xep-0225.html>`__ only), you must use this protocol for the functionality.

Here is an example configuration with a single connection over the `XEP-0225 <http://xmpp.org/extensions/xep-0225.html>`__ protocol used by both external domains:

.. code:: dsl

   admins = [ 'admin@devel.tigase.org' ]
   'bind-ext-hostnames' = [ 'pubsub.devel.tigase.org' ]
   'config-type' = 'component'
   debug = [ 'server' ]
   'default-virtual-host' = [ 'devel.tigase.org' ]
   dataSource {
       default () {
           uri = 'jdbc:derby:/tigasedb'
       }
   }
   ext () {
   }
   userRepository {
       default () {}
   }
   authRepository {
       default () {}
   }
   muc (class: tigase.muc.MUCComponent) {}
   pubsub (class: tigase.pubsub.PubSubComponent) {}

With this configuration you do not need to configure entries in ``ext-man`` for PubSub component, only for MUC component but you need to user ``client`` as the value for protocol field.

Usage with a separate database
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A Simple Case - MUC as an External Component

A few assumptions:

1. We want to run a MUC component for a domain: ``muc.devel.tigase.org`` and password ``muc-pass``

2. The main server works at an address: devel.tigase.org and for the same virtual domain

3. We want to connect to the server using `XEP-0114 <http://xmpp.org/extensions/xep-0114.html>`__ protocol and port ``5270``.

There is a special configuration type for this case which simplifies setting needed to run Tigase as an external component:

.. code:: dsl

   'config-type' = 'component'

This generates a configuration for Tigase with only one component loaded by default - the component used for external component connection. If you use this configuration type, your ``config.tdsl`` file may look like this:

.. code:: dsl

   admins = [ 'admin@devel.tigase.org' ]
   'config-type' = 'component'
   debug = [ 'server' ]
   'default-virtual-host' = [ 'devel.tigase.org' ]
   dataSource {
       default () {
           uri = 'jdbc:derby:/tigasedb'
       }
   }
   userRepository {
       default () {}
   }
   authRepository {
       default () {}
   }
   muc (class: tigase.muc.MUCComponent) {}
   ext () {
   }

To make this new instance connect to the Tigase XMPP Server, you need to create one more file with external connection configuration at ``etc/externalComponentItems`` which will be loaded to the local database and then removed.

.. code:: text

   muc.devel.tigase.org:muc-pass:connect:5270:devel.tigase.org:accept

.. Warning::

    While loading configuration from ``etc/externalComponentItems`` file is supported, we recommend usage of shared database if possible. In future this method may be deprecated.


More Components

Suppose you want to run more than one component as an external components within one Tigase instance. Let’s add another - PubSub component to the configuration above and see how to set it up.

The most straightforward way is just to add another external component connection to the main server for the component domain using Admin UI or ad-hoc command on the main server.

Then we can use following configuration on the server running in the ``component`` mode:

.. code:: dsl

   admins = [ 'admin@devel.tigase.org' ]
   'config-type' = 'component'
   debug = [ 'server' ]
   'default-virtual-host' = [ 'devel.tigase.org' ]
   dataSource {
       default () {
           uri = 'jdbc:derby:/tigasedb'
       }
   }
   userRepository {
       default () {}
   }
   authRepository {
       default () {}
   }
   muc (class: tigase.muc.MUCComponent) {}
   pubsub (class: tigase.pubsub.PubSubComponent) {}
   ext () {
   }

and we need to create a file with configuration for external component connection which will be loaded to the internal database:

.. code:: text

   muc.devel.tigase.org:muc-pass:connect:5270:devel.tigase.org:accept
   pubsub.devel.tigase.org:pubsub-pass:connect:5270:devel.tigase.org:accept

Please note however that we are opening two connections to the same server. This can waste resources and over-complicate the system. For example, what if we want to run even more components? Opening a separate connection for each component is a tad overkill.

In fact there is a way to reuse the same connection for all component domains running as an external component. The property ``bind-ext-hostnames`` contains a comma separated list of all hostnames (external domains) which should reuse the existing connection.

There is one catch however. Since you are reusing connections (hostname binding is defined in `XEP-0225 <http://xmpp.org/extensions/xep-0225.html>`__ only), you must use this protocol for the functionality.

Here is an example configuration with a single connection over the `XEP-0225 <http://xmpp.org/extensions/xep-0225.html>`__ protocol used by both external domains:

.. code:: dsl

   admins = [ 'admin@devel.tigase.org' ]
   'bind-ext-hostnames' = [ 'pubsub.devel.tigase.org' ]
   'config-type' = 'component'
   debug = [ 'server' ]
   'default-virtual-host' = [ 'devel.tigase.org' ]
   dataSource {
       default () {
           uri = 'jdbc:derby:/tigasedb'
       }
   }
   ext () {
   }
   userRepository {
       default () {}
   }
   authRepository {
       default () {}
   }
   muc (class: tigase.muc.MUCComponent) {}
   pubsub (class: tigase.pubsub.PubSubComponent) {}

and example of the external connections configuration file:

.. code:: text

   muc.devel.tigase.org:muc-pass:connect:5270:devel.tigase.org:client

.. |adminui extman add item form| image:: ../../../../asciidoc/admin/images/admin/adminui_extman_add_item_form.png
.. |adminui extman add item form external muc| image:: ../../../../asciidoc/admin/images/admin/adminui_extman_add_item_form_external_muc.png