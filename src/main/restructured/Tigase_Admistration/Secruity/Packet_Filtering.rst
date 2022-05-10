Packet Filtering
----------------------

Tigase offers different ways to filter XMPP packets flying through the server. The most common use for packet filtering is to restrict users from sending or receiving packets based on the sender or received address.

There are also different possible scenarios: time based filtering, content filtering, volume filtering and so on.

All pages in this section describe different filtering strategies.

Domain Based Packet Filtering
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Domain based packet filtering is a simple filter allowing to restrict user communication based on the source/destination domain name. This is especially useful if we want to limit user communication within a single - own domain only or a list of domains.

A company might not wish to allow employers to chat during work hours with anybody in the world. A company may also have a few different domains used by different branches or departments. An administrator may restrict communication to a list of domains.

Introduction
~~~~~~~~~~~~~~~

The restriction is on a per-user basis. So the administrator can set a different filtering rules for each user. There is also a per-domain configuration and global-installation setting (applied from most general to most specific, i.e. from installation to user).

Regular users can not change the settings. So this is not like a privacy list where the user control the filter. Domain filter can not be changed or controlled by the user. The system administrator can change the settings based on the company policy.

There are predefined rules for packet filtering:

1. ``ALL`` - user can send and receive packets from anybody.

2. ``LOCAL`` - user can send and receive packets within the server installation only and all it’s virtual domains.

3. ``OWN`` - user can send and receive packets within his own domains only

4. ``BLOCK`` - user can’t communicate with anyone. This could be used as a means to temporarily disable account or domain.

5. ``LIST`` - user can send and receive packets within listed domains only (i.e. *whitelist*).

6. ``BLACKLIST`` - user can communicate with everybody (like ``ALL``), except contacts on listed domains.

7. ``CUSTOM`` - user can communicate only within custom created rules set.

Whitelist (``LIST``) and blacklist (``BLACKLIST``) settings are mutually exclusive, i.e. at any given point of time only one of them can be used.

Those rules applicable to particular users are stored in the user repository and are loaded for each user session. If there are no rules stored for a particular user server tries to apply rules for a VHost of particular user, and if there is no VHost filtering policy server uses global server configuration. If there is no filtering policy altogether server applies defaults based on following criteria:

1. If this is **Anonymous** user then ``LOCAL`` rule is applied

2. For all **other** users ``ALL`` rule is applied.

Configuration
~~~~~~~~~~~~~~~

Filtering is performed by the domain filter plugin which must be loaded at startup time. It is loaded by default if the plugins list is not set in the configuration file. However if you have a list of loaded plugins in the configuration file make sure ``domain-filter`` is on the list.

There is no other configuration required for the plugin to work.

Administration, Rules Management
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Although controlling domain filtering rules is possible for each user separately, it is not practical for large installations. In most cases users are stored in the database and a third-party system keeps all the user information.

To change the rule for a single user you can use loadable administration scripts feature and load `UserDomainFilter.groovy <https://github.com/tigase/tigase-server/tree/master/src/main/groovy/tigase/admin/UserDomainFilter.groovy>`__ script. It enables modifying rules for a given user JID.

Implementation
~~~~~~~~~~~~~~~

If you have a third party system which keeps and manages all user information than you probably have your own UserRepository implementation which allows the Tigase server to access user data. Filtering rules are loaded from user repository using following command:

.. code:: java

   repo.getData(user_id, null, DomainFilter.ALLOWED_DOMAINS_KEY, null);
   repo.getData(user_id, null, DomainFilter.ALLOWED_DOMAINS_LIST_KEY, null);

Where ``user_id`` is user Jabber ID without resource part, ``DomainFilter.ALLOWED_DOMAINS_KEY`` is a property key: ``allowed-domains``. The user repository MUST return one of following only:

1. ``ALL`` - if the user is allowed to communicate with anybody

2. ``LOCAL`` - if the user is allowed to communicate with users on the same server installation.

3. ``OWN`` - if the user is allowed to communicate with users within his own domain only.

4. ``LIST`` - list of domains within which the user is allowed to communicate with other users. No wild-cards are supported. User’s own domain should be included too.

5. ``BLACKLIST`` - list of domains within which the user is NOT allowed to communicate with other users. No wild-cards are supported. User’s own domain should NOT be included.

6. ``CUSTOM`` - list of rules defining custom communication permissions (server processes stanza according to first matched rule, similar to XEP-0016) in the following format:

::

   ruleSet = rule1;rule2;ruleX;

   rule = order_number|policy|UID_type[|UID]

   order_number = any integer;
   policy = (allow|deny);
   UID_type = [jid|domain|all];
   UID = user JID or domain, for example pubsub@test.com; if UID_type is ALL then this is ignored.

For example:

::

   1|allow|self;
   2|allow|jid|admin@test2.com;
   3|allow|jid|pubsub@test.com;
   4|deny|all;

1. ``null`` - a java null if there are no settings for the user.

In case of ``LIST`` and ``BLACKLIST`` filtering options, it’s essential to provide list of domains for the whitelisting/blacklisting. ``DomainFilter.ALLOWED_DOMAINS_LIST_KEY`` is a property key: "allowed-domains-list". The user repository MUST return semicolon separated list of domains: ``domain1.com;domain2.com,domain3.org``

The filtering is performed by the ```tigase.xmpp.impl.DomainFilter`` <https://github.com/tigase/tigase-server/tree/master/src/main/java/tigase/xmpp/impl/DomainFilter.java>`__ plugin. Please refer to source code for more implementation details.