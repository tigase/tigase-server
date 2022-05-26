External
---------------

bind-ext-hostnames
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Default value:** none

**Example:** ``'bind-ext-hostnames' = [ 'pubsub.host.domain' ]``

**Possible values:** comma separated list of domains.

**Description:** This property enables setting a list of domains to be bound to the external component connection. Let’s say we have a Tigase instance with only MUC and PubSub components loaded and we want to connect this instance to the main server via external component protocol. Using `--external property <#external>`__ we can define a domain (perhaps muc.devel.tigase.org), password, TCP/IP port, remote host address, connection type, etc…​ This would make one of our components (MUC) visible on the remote server.

To make the second component (PubSub) visible we would need to open another connection with the domain name (pubsub.devel.tigase.org) for the other component. Of course the second connection is redundant as all communication could go through a single connection. This is what this property is used. In our example with 2 components you can just put the 'pubsub.devel.tigase.org' domain as a value to this property and it will bind the second domain to a single connection on top of the domain which has been authenticated during protocol handshaking.

**Available since:** 5.0.0

.. _virtHosts:

default-virtual-host
^^^^^^^^^^^^^^^^^^^^^^^^^^^

**Description:** The ``default-virtual-host`` property allows setting of the name of default virtual host that is served by the installation. It is loaded during startup of the application and stored in the database. **It may only contain single domain name!**

Any additional configuration options or additional virtual hosts domains should be added and configured using ad-hoc commands such as ``Add new item``, ``Update item configuration`` and ``Remove an item`` available at the JID of the ``VHostManager`` component of your installation (``vhost-man@your-server-domain``).

**Available since:** 8.0.0

ext
^^^^^^^^^

**Description:** This property defines parameters for external component connections.

The component is loaded the same way as all other Tigase components. In your ``config.tdsl`` file you need to add the external class:

.. code::

   ext (class: tigase.server.ext.ComponentProtocol) {}

This will load the component with an empty configuration and is practically useless. You have to tell the component on what port to listen to (or on what port to connect to) and external domains list with passwords.

Those values need to be configured while the Tigase XMPP Server is running using XMPP ad-hoc commands such as ``Add new item``, ``Update item configuration`` and ``Remove an item`` available at the JID of the external component which you have just enabled (``ext@your-server-domain``).

**Possible values:** external domains parameters list.

**Available since:** 4.3.0

**Removed in:** 8.0.0