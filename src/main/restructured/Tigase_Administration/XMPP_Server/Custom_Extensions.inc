Tigase Custom Extensions
----------------------------

General features
^^^^^^^^^^^^^^^^^^^

.. table:: tabel 6.Monitoring

   +---------+--------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | Support | Name                           | Comment                                                                                                                                                                           |
   +---------+--------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓ [#]_  | AuditLog                       | Ability functionality to log important events in a system (loggins, message exchanges, calls)                                                                                     |
   +---------+--------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | Anti Abuse                     | Fight stanza SPAM, DoS, brute-force attacks and other threats                                                                                                                     |
   +---------+--------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | Virtual domains                | Ability to create and manage multiple virtual domains from a single instance and restart-less management                                                                          |
   +---------+--------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | MUC subscribe for offline push | Option to register permanently to the room to receive push notifications about new messages.                                                                                      |
   +---------+--------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | Scripting API                  | Supports the Java Scripting API JSR-223                                                                                                                                           |
   +---------+--------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | JMX monitoring                 | Advanced monitoring the server via JMX protocol with an API for connecting custom monitors and TCP/IP end-point for connecting general purpose JMX tools                          |
   +---------+--------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | HTTP monitoring                | Basic monitoring via HTTP protocol                                                                                                                                                |
   +---------+--------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | XMPP Monitoring                | Pluggable, active monitoring via XMPP, retrieving detailed server statistics, receiving automatic notifications about possible problems discovered by the self-monitor mechanisms |
   +---------+--------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | SNMP Monitoring                | Advanced server monitoring via SNMP.                                                                                                                                              |
   +---------+--------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | Bosh Cache                     | Bosh Session Cache - a feature to quickly reload user data - roster, presences and messages history by the web client (for example after web page reload)                         |
   +---------+--------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | Clustering                     | Full clustering support for HA and LB with pluggabble clustering strategies for perfect optimising the cluster to the client’s system                                             |
   +---------+--------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | Advanced Clustering Strategy   | Dedicated, specialised clustering strategy for best possible performance                                                                                                          |
   +---------+--------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | MUC Clustered                  | Support for clustering group chatrooms with various, pluggable strategies                                                                                                         |
   +---------+--------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | PubSub Clustered               | Support for clustering PubSub component with various, pluggable strategies                                                                                                        |
   +---------+--------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | Mobile optimisations           | Optimizations designed for Mobile Devices                                                                                                                                         |
   +---------+--------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | OSGi                           | Support for running in OSGi environment, i.e. as embedded XMPP server in advanced application server                                                                              |
   +---------+--------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | Dynamic rosters                | Ability to create users' rosters entries on the fly based on data retrieved from any sources                                                                                      |
   +---------+--------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | Command line admin tools       | Commandline utility to manage server                                                                                                                                              |
   +---------+--------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | Unified Archive                | An extension to XEP-0313 Message Archive Management, with greatly improved flexibility in terms of what can be archived.                                                          |
   +---------+--------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+


Repositories/Databases
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. table:: Table 7.Repositories/Databases

   +---------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | Support | Name          | Comment                                                                                                                                                                 |
   +---------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | DB per domain | Ability to have multiple databases for specific domains.                                                                                                                |
   +---------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | PostgreSQL    | Full support for PostgreSQL database with database schemas excluding dedicated DB schema for PubSub component                                                           |
   +---------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | MySQL         | Full support for MySQL database with database schemas, dedicated DB schema for PubSub component                                                                         |
   +---------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | SQL Server    | Full support for MS SQL Server database with database schemas excluding dedicated DB schema for PubSub component, only in Tigase server version 3.x                     |
   +---------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | Derby DB      | Full support for built-in Derby database with database schemas excluding dedicated DB schema for PubSub component                                                       |
   +---------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | JDBC          | Support for all JDBC enabled databases, although the database schemas are available for some databases                                                                  |
   +---------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------+
   | ✓       | LDAP-Auth     | LDAP Authentication Connector Supported                                                                                                                                 |
   +---------+---------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------+

.. [#]
   Requires commercial license
