Access Control Lists in Tigase
-------------------------------------

Tigase offers support for **Access Control List (ACL)** to allow for fine grained access to administration commands on the server.

By default, all administration commands are only accessible (visible through service discovery and can be executed) by the service administrators. Service administrators are existing accounts with JIDs (**BareJIDs**) listed in the ``config.tdsl`` file under ``admins = []`` (please see :ref:`admins <admins>` for details).

Additionally, other XMPP users and entities can be assigned permissions to execute a command or commands using Tigase’s ACL capabilities.

The following is a list of possible ACL modifiers for administrator command accessibility:

-  ``ALL`` - Everybody can execute the command, even users from different federated servers.

-  ``ADMIN`` - Local server administrators can execute the command, this is a default setting if no ACL is set for a command.

-  ``LOCAL`` - All users with accounts on the local server can execute the command. Users from other, federated servers will not be able to execute the command.

-  ``NONE`` - No one will be allowed to execute this command

-  ``DOMAIN_OWNER`` - Only user which is owner of the domain which items are being manipulated is allowed to execute the comment. If script is not checking permissions for the manipulated item, this value will behave in the same way as ``LOCAL``.

-  ``DOMAIN_ADMIN`` - Only user which is one of the domain administrators will be able to execute the command manipulating items related to the domain. If script is not checking permissions for the manipulated item, this value will behave in the same way as ``LOCAL``.

-  ``example.com`` - Only users with accounts on the selected domain will be able to execute the command. It may be useful to setup a domain specifically for admin accounts, and automatically all users within that domain would be able to run the command.

-  ``user@example.com`` - Comma separated list of JIDs of users who can execute the command.

In any case, regardless of ACL settings, any command can be executed and accessed by the designated service wide administrators, that is accounts listed as admins in the ``config.tdsl`` file.

Multiple ACL modifiers can be combined and applied for any command. This may not always makes sense. For example ALL supersedes all other settings, so it does not make sense to combine it with any other modifier. However, most others can be combined with JID to broaden access to specific accounts.

On Tigase server the Access Control List is checked for the first matching modifier. Therefore if you combine ALL with any other modifier, anybody from a local or remote service will always be able to execute the command, no matter what other modifiers are added.

Please note, the ACL lists work on the command framework level. Access is verified before the command is actually executed. There might be additional access restrictions within a command itself. In many cases, even if all local users are permitted to execute a command (LOCAL modifier), some commands allow only to be executed by a domain owner or a domain administrator (and of course by the service-wide administrators as well). All the commands related to a user management such as adding a new user, removing a user, password changes, etc… belong to this category. When conducting domain (vhost) management, creation/registration of a new domain can be done by any local user (if LOCAL ACL modifier is set) but then all subsequent domain management tasks such as removing the vhost, updating its configuration, setting SSL certificate can be done by the domain owner or administrator only.

The ACL list is set for a specific Tigase component and a specific command. Therefore the configuration property must specify all the details. So the general format for configuring ACL for a command is this:

.. code:: dsl

   comp-id () {
       commands {
           'command-id' = [ 'ACL_modifier', 'ACL_modifier', 'ACL_modifier' ]
       }
   }

The breakdown is as such:

-  ``comp-id`` is the Tigase server component ID such as: sess-man, vhost-man, c2s, etc..

-  ``commands`` is a static text which indicates that the property is for component’s command settings.

-  ``command-id`` is a command ID for which we set the ACL such as query-dns, http://jabber.org/protocol/admin#add-user, user-roster-management, etc…

Here are a few examples:

*Allowing local users to create and manage their own domains*

.. code:: dsl

   'vhost-man' () {
       commands {
           'comp-repo-item-add' = 'LOCAL'
           'comp-repo-item-remove' = 'LOCAL'
           'comp-repo-item-update' = 'LOCAL'
           'ssl-certificate-add' = 'LOCAL'
       }
   }

In fact all the commands except item-add can be executed by the domain owner or administrator.

*Allowing local users to execute user management commands:*

.. code:: dsl

   'sess-man' () {
       'commands' {
           'http://jabber.org/protocol/admin#add-user' = 'LOCAL'
           'http://jabber.org/protocol/admin#change-user-password' = 'LOCAL'
           'http://jabber.org/protocol/admin#delete-user' = 'LOCAL'
           'http://jabber.org/protocol/admin#get-online-users-list' = 'LOCAL'
           'http://jabber.org/protocol/admin#get-registered-user-list' = 'LOCAL'
           'http://jabber.org/protocol/admin#user-stats' = 'LOCAL'
           'http://jabber.org/protocol/admin#get-online-users-list' = 'LOCAL'
       }
   }

As in the previous example, the commands will by executed only by local users who are the specific domain administrators.

*Allowing users from a specific domain to execute query-dns command and some other users for given JIDs from other domains:*

.. code:: dsl

   'vhost-man' () {
       'commands' {
           'query-dns' = [ 'tigase.com', 'admin@tigase.org', 'frank@example.com' ]
       }
   }

To be able to set a correct ACL property you need to know component names and command IDs. Component IDs can be found in the service discovery information on running server or in the server logs during startup. A command ID can be found in the command script source code. Each script contains a list of metadata at the very beginning of it’s code. One of them is ``AS:CommandId`` which is what you have to use for the ACL setting.
