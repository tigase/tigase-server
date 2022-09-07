.. _tigase41virtualHosts:

Virtual Hosts in Tigase Server
----------------------------------

Tigase server supports multiple virtual hosts in a single server installation. Virtual hosts can be added or removed, enabled or disabled during runtime without restarting the service or disrupting normal operation.

This document describes how virtual hosts work in Tigase server and how to get the most out of this feature in your installation.

The :ref:`'default-virtual-host'<virtHosts>` property allows to define name of the single vhost domain which will be considered a default vhost domain for this installation. It allows you just to configure the domain name. Any additional configuration needs to be configured using ad-hoc commands.

Virtual hosts should be managed using ad-hoc commands or admin ui, visit :ref:`Add and Manage Domains<addManageDomain>` for description of vhosts management process or visit :ref:`Specification for ad-hoc Commands Used to Manage Virtual Domains<adhocCommands>` for more information about ad-hoc commands.

If you have components that may not be able to handle multiple vhosts or cluster mode, we have developed a virtual component solution as well, details in the :ref:`Virtual Components for the Tigase Cluster<virtualComponents>` section.

You may also want to reference the Vhosts API for additional information: - :ref:`API Description for Virtual Domains Management in Tigase Server<addManageDomain>`.

.. **Tip**::

   If you have a Tigase XMPP Server running in the cluster mode hidden behind some load balancer, or if internal IP or hostname of cluster nodes differ from the DNS name under which it is available from the internet, we would suggest setting a property ``installation-dns-address`` of ``vhost-man`` component to the DNS name which allows you to connect to all cluster nodes (ie. to the DNS name of the load balancer). This will allow Tigase XMPP Server to do proper DNS lookups to verify that DNS domain name of the virtual host which you will try to add or update points to your XMPP installation.


Default VHost configuration
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

It’s possible to specify initial default configuration for all Virtual Host in TDSL configuration file (i.e. ``etc/config.tdsl``) for selected parameters. To do so you should specify each configuration option within ``defaults`` bean belonging to ``vhost-man`` bean:

::

   'vhost-man' () {
       'defaults' () {
           'domain-filter-policy' = null
           's2s-secret' = null
           trusted = null
           'vhost-disable-dns-check' = false
           'vhost-max-users' = 0L
           'vhost-message-forward-jid' = null
           'vhost-presence-forward-jid' = null
           'vhost-register-enabled' = true
           'vhost-tls-required' = false
       }
   }

After initial definition of default configuration or after first startup of Tigase XMPP Server it is possible to configure Virtual Host defaults using ad-hoc commands by modifying values for ``default`` using ad-hoc as described in :ref:`Specification for ad-hoc Commands Used to Manage Virtual Domains<adhocCommands>`.

Alternatively, you may edit default Virtual Host configuration (configuration for domain ``default``) using Admin UI which by default is available at http://localhost:8080/admin/.

.. _adhocCommands:

Specification for ad-hoc Commands Used to Manage Virtual Domains
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

There are 3 ad-hoc commands for virtual domains management in the Tigase server:

1. ``VHOSTS_RELOAD`` used to reload virtual domains list from the repository (database).

2. ``VHOSTS_UPDATE`` used to add a new virtual domain or update information for existing one.

3. ``VHOSTS_REMOVE`` used to remove an existing virtual host from the running server.

Syntax of the commands follows the specification described in `XEP-0050 <http://xmpp.org/extensions/xep-0050.html>`__. Extra information required to complete the command is carried as data forms described in `XEP-0004 <http://xmpp.org/extensions/xep-0004.html>`__.

All commands are accepted by the server only when send by the installation administrator. If the command is sent from any other account ``<not-authorized />`` error is returned. To grant administrator rights to an account you have to set ``admins`` property in the ``config.tdsl`` configuration file.

Commands are sent to the 'vhost-man' server component and the 'to' attribute of the stanza must contain a full JID of the VHostManager on the server. The full **JID** consists of the component name: 'vhost-man' and the local domain, that is domain which is already on the list of virtual domains and is active. Assuming 'existing.domain.com' one of domains already activated for the server installation the **JID** is: 'vhost-man@existing.domain.com'.

Reloading the Domains List from the Database
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In order to reload virtual domains from the permanent repository other than configuration file, you have to send ``VHOSTS_RELOAD`` ad-hoc command to the VHostManager on the server.

The reload command request is of the form:

.. code:: xml

   <iq type="set"
       to="vhost-man@existing.domain.com"
       id="aac8a">
     <command xmlns="http://jabber.org/protocol/commands"
              node="VHOSTS_RELOAD" />
   </iq>

The server sends a response upon successful completion of the command with current number of virtual domains server by the installation:

.. code:: xml

   <iq from="vhost-man@existing.domain.com"
       type="result"
       to="cmd-sender-admin@existing.domain.com"
       id="aac8a">
     <command xmlns="http://jabber.org/protocol/commands"
              status="completed"
              node="VHOSTS_RELOAD">
       <x xmlns="jabber:x:data" type="result">
         <field type="fixed" var="Note">
           <value>Current number of VHosts: 123</value>
         </field>
       </x>
     </command>
   </iq>

If the command is sent from an account other than admin, the server returns an error:

.. code:: xml

   <iq from="vhost-man@existing.domain.com"
       type="error"
       to="cmd-sender-admin@existing.domain.com"
       id="aac8a">
     <command xmlns="http://jabber.org/protocol/commands"
              node="VHOSTS_RELOAD" />
     <error type="auth" code="401">
       <not-authorized xmlns="urn:ietf:params:xml:ns:xmpp-stanzas" />
       <text xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"
             xml:lang="en">
         You are not authorized for this action.
       </text>
     </error>
   </iq>

The response doesn’t have any special meaning other then end-user information. The client may ignore the response as it is sent after the command has been executed.

Adding a New Domain or Updating Existing One
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In order to add a new domain or update existing one you have to send an ad-hoc command ``VHOSTS_UPDATE`` with at least one domain name in the command data form. You can also specify whether the domain is enabled or disabled but this is optional. Future releases may allow for setting additional parameters for the domain: maximum number of user accounts for this domain, anonymous login enabled/disabled for the domain, registration via XMPP enabled/disabled for this domain and some more parameters not specified yet.

The domain add/update command request is of the form:

.. code:: xml

   <iq type="set"
       to="vhost-man@existing.domain.com"
       id="aacba">
     <command xmlns="http://jabber.org/protocol/commands"
              node="VHOSTS_UPDATE">
       <x xmlns="jabber:x:data" type="submit">
         <field type="text-single"
                var="VHost">
           <value>new-virt.domain.com</value>
         </field>
         <field type="list-single"
                var="Enabled">
           <value>true</value>
         </field>
       </x>
     </command>
   </iq>

Please note: Character case in the command field variable names does matter.

Upon successful completion of the command the server sends a response back to the client with information of the existing number of virtual hosts on the server:

.. code:: xml

   <iq from="vhost-man@existing.domain.com"
       type="result"
       to="cmd-sender-admin@existing.domain.com"
       id="aacba">
     <command xmlns="http://jabber.org/protocol/commands"
              status="completed"
              node="VHOSTS_UPDATE">
       <x xmlns="jabber:x:data" type="result">
         <field type="fixed" var="Note">
           <value>Current number of VHosts: 124</value>
         </field>
       </x>
     </command>
   </iq>

Removing a Virtual Domain From the Server
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In order to remove a virtual domain you have to send ``VHOSTS_REMOVE`` command to the server with the domain name.

The domain remove command is sent by the client:

.. code:: xml

   <iq type="set"
       to="vhost-man@existing.domain.com"
       id="aacba">
     <command xmlns="http://jabber.org/protocol/commands"
              node="VHOSTS_REMOVE">
       <x xmlns="jabber:x:data" type="submit">
         <field type="text-single"
                var="VHost">
           <value>virt-nn.domain.com</value>
         </field>
       </x>
     </command>
   </iq>

Upon successful completion of the command the server sends a response back to the client with information of the existing number of virtual hosts on the server:

.. code:: bash

   <iq from="vhost-man@existing.domain.com"
       type="result"
       to="cmd-sender-admin@existing.domain.com"
       id="aacba">
     <command xmlns="http://jabber.org/protocol/commands"
              status="completed"
              node="VHOSTS_REMOVE">
       <x xmlns="jabber:x:data" type="result">
         <field type="fixed" var="Note">
           <value>Current number of VHosts: 124</value>
         </field>
       </x>
     </command>
   </iq>

.. _virtualComponents:

Virtual Components for the Cluster Mode
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Let’s assume you have a cluster installation and you want to include a component in your installation which doesn’t support the cluster mode yet. If you put it on all nodes as a separate instances they will work out of sync and overall functionality might be useless. If you put on one node only it will work correctly but it will be visible to users connected to this one node only.

Ideally you would like to have a mechanism to install it on one node and put some redirections on other nodes to forward all packets for this component to a node where this component is working. Redirection on it’s own is not enough because the component must be visible in service discovery list and must be visible somehow to users connected to all nodes.

This is where the virtual components are handy. They are visible to users as a local normal component, they seem to be a real local component but in fact they just forward all requests/packets to a cluster node where the real component is working.

Virtual component is a very lightweight ServerComponent implementation in Tigase server. It can pretend to be any kind of component and can redirect all packets to a given address. They can mimic native Tigase components as well as third-party components connected over external component protocol (XEP-0114).

Configuration is very simple and straightforward, in fact it is very similar to configuration of any Tigase component. You set a real component name as a name of the component and a virtual component class name to load. Let’s say we want to deploy MUC component this way. The MUC component is visible as ``muc.domain.oug`` in the installation. Thus the name of the component is: ``muc``

.. code::

   muc (class: tigase.cluster.VirtualComponent) {}

This is pretty much all you need to load a virtual component. A few other options are needed to point to correct destination addresses for packets forwarding and to set correct service discovery parameters:

.. code::

   }
   muc (class: tigase.cluster.VirtualComponent) {
       'disco-category' = 'conference'
       'disco-features' = 'http://jabber.org/protocol/muc'
       'disco-name' = 'Multi User Chat'
       'disco-node' = ''
       'disco-type' = 'text'
       'redirect-to' = 'muc@cluster-node-with-real-muc.domain.our'
   }

That’s it.

