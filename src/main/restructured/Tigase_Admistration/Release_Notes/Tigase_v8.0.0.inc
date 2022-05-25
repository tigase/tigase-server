Tigase XMPP Server 8.0.0 Change notes and announcement
------------------------------------------------------

.. _`_major_changes`:

Major Changes
=============

.. _`_kernel_and_beans_configuration`:

Kernel and beans configuration
------------------------------

Tigase now operates using a Kernel and Beans style of programming. What does this mean for Tigase and You? Good news, really. Tigase XMPP Server is now working as a Kernel program, which will operate on it’s own and handle all the core functionality of the server. Component, and non-essential functionality will now be loaded as Beans. As a user, your experience will not change all that much. However, beans can be loaded and unloaded without having to restart Tigase, meaning that the program will behave more dynamically. This means a smaller footprint on memory on resources when components are not needed, and longer uptimes without having to rest art the program! This also allows for greater flexibility for Tigase XMPP Server to be better customized for unique solutions.

.. _`_new_configuration_file_format`:

New Configuration File Format
-----------------------------

With the change of Tigase to a Kernel and Beans style of programming, we have also changed how the configuration file is managed. Although you will still edit the ``config.tdsl`` file like a plaintext file, a new style of formatting will be used known as DSL. Domain Specific Language may add more lines, but is a cleaner format, and provides a more secure configuration design since validation of the configuration is done at the domain level. For more information on this format and how to configure Tigase, visit `DSL Configuration Guide <#dslConfig>`__.

.. _`_cluster_node_shutdown_changes`:

Cluster Node Shutdown Changes
-----------------------------

Starting with Tigase XMPP Server 8.0.0, users connected on clustered nodes will be able use a ``see-other-host`` strategy when a node is being shutdown. **Note: This may not be compatible with all clients.** The Ad-hoc command is designed for a graceful shutdown of cluster nodes as a groovy script ``Shutdown.groovy``. This script also allows for the -timeout setting which will delay shutdown of the node, and alert all users (via a headline message) that the server will be shutdown after a time. User clients that are compatible with the command will then detect other connected clusters and maintain their connections.

If the command is being sent to shut down the whole cluster, no ``see-other-host`` implementation will be sent, however timeout settings may still be used.

The script may be activated by an ad-hoc command, or sent using REST from remote or Tigase Admin UI.

.. _`_significant_cleanup_of_code_and_repositories`:

Significant cleanup of code and repositories
--------------------------------------------

Multiple changes have been made to the structure and coding for v8, many related to trimming size of repositories and old calls. Some of these improvements are listed here:

-  Empty JavaDocs that do not convey values have been removed.

-  All code is reformatted to be compliant with out `codestyle guidelines <#tigaseCodeStyle>`__.

-  Calls to ``System.out.print*()`` and ``printStackTrace()`` have been removed from code.

-  Depreciated and unused classes have been removed.

.. _`_bouncycastle_being_used_for_starttls`:

BouncyCastle being used for StartTLS
------------------------------------

`BouncyCastle <https://www.bouncycastle.org/java.html>`__ Crypto API has now been employed to handle StartTLS negotiation. By doing this, Tigase now supports ``tls-unique`` within the SCRAM PLUS authentication implementation. This API is may be employed by calling the class in your configuration file:

.. code:: dsl

   c2s () {
       sslContextContainer(class: tigase.extras.bcstarttls.BCSSLContextContainer) {}
   }

The BouncyCastle classes are included in the dist-max archives.

.. _`_default_virtual_host_property_changes`:

default-virtual-host property changes
-------------------------------------

Default virtual hosts property is now able to be configured only as a domain name instead of the list of virtual host domains with options. Additional virtual host domains and their options need to be configured using ad-hoc commands or web AdminUI. Reference `Virtual-Hosts Configuration <#virtHosts>`__ for more details.

.. _`_all_artifacts_are_signed`:

All artifacts are signed
------------------------

Since work began on v8.0.0 Tigase has required that all changes to Tigase XMPP Server and dependencies be signed with known certificates. This version marks the first to be totally signed.

.. _`_scaled_down_installation_methods`:

Scaled Down Installation Methods
--------------------------------

We have cleaned up installation methods for Tigase and now recommend the use of web-installer method. IzPack installer (files ``tigase-server-<version>-b<build>.jar`` installation methods have been removed and will no longer be produced for v8.0.0 and later. Manual installation is still available for those unable to use HTTP or browser access. Visit our `Quick Start <#quickstart>`__ guide for instructions on these other methods.

.. _`_emojis_now_supported_on_tigase_xmpp_servers`:

Emojis now supported on Tigase XMPP Servers
-------------------------------------------

Emojis are now supported on MySQL databases, however some settings may be need to be changed, although they won’t affect existing databases. `Visit this section <#emojisupportSQL>`__ for details.

.. _`_xep_0215_external_service_discovery_now_supported`:

XEP-0215 External Service Discovery now supported
-------------------------------------------------

Tigase now supports `XEP-0215 - External Service Discovery <https://xmpp.org/extensions/xep-0215.html>`__ allowing Tigase to discover services that are not available VIA the XMPP Protocol. For setup and configuration information visit `External Service Discovery Component <#_tigase_external_service_discovery>`__ documentation.

.. _`_xep_0313_message_archive_management_now_supported`:

XEP-0313 Message Archive Management now supported
-------------------------------------------------

`XEP-0313 - Message Archive Management <https://xmpp.org/extensions/xep-0313.html>`__ is now supported by Tigase featuring custom enhancements like full-text search and searching by tags. MAM requires Tigase’s message archive to be enabled in the ``config.tdsl`` file, and the schema (XEP-0136 or XEP-0313) must be configured in session manager settings. To turn on MAM, see configuration guide `located here <#_support_for_mam>`__.

.. _`_xep_0363_http_file_upload_now_supported`:

XEP-0363 HTTP File Upload now supported
---------------------------------------

`XEP-0363 - HTTP File Upload <https://xmpp.org/extensions/xep-0363.html>`__ is now supported using Tigase HTTP API component now allowing for a more robust one-to-many file uploading option. Configuration details are available at the `HTTP File Upload Component <#XEP0363>`__ section of documentation.

.. _`_startup_now_uses_bootstrapping`:

Startup now uses bootstrapping
------------------------------

Tigase now uses bootstrapping to startup, which will load configuration from ``config.tdsl`` file like before. Then Tigase will begin it’s normal operations with the configuration options. All startup functions for Tigase will now run under the ``bootstrap`` bean.

.. _`_captcha_system_now_available_for_in_band_registration`:

CAPTCHA system now available for in-band registration
-----------------------------------------------------

`XEP-0077 In band registration <https://xmpp.org/extensions/xep-0077.html>`__ can use Data Forms as an option to process new registrations. Now you can secure these registrations by employing a CAPTCHA solution. By enabling this option you can reduce the number of potential spammers and bots on your server.

.. _`_schema_changes`:

Schema changes
--------------

Now each component has it’s own schema for databases, they are no longer tied into Tigase XMPP server versions making changes and updates to individual components easier, and may not disrupt all users not using certain components. See the `schema update section <#schemaChangev800>`__ for more details.

.. _`_shrinkable_statistics_history`:

Shrinkable Statistics History
-----------------------------

Statistics history can now be automatically made smaller if a systems memory resources are above a certain amount. By default this is enabled and will trigger when over 95% of memory is in use. Half of all existing entries will be removed at this time. The same pattern will continue to halve the available records every time the threshold is met. A hard-set minimum of 5 entries is set, so you will always have the last 5 entries. This setting may be adjusted by adding the following setting to your ``config.tdsl`` file and adjusting the integer value:

.. code:: dsl

   stats() {
     'stats-high-memory-level' = 95
   }

.. _`_statistics_now_available_for_all_modules`:

Statistics now available for all modules
----------------------------------------

For any bean, you may enable statistics by using the following

.. code:: dsl

   bean (class) {
     statistics = true
   }

.. _`_spam_protection`:

Spam Protection
---------------

Tigase XMPP Server v8.0.0 now includes some efforts to prevent spam bot accounts from running on servers.

.. _`_account_registration_limits_expanded`:

Account Registration Limits Expanded
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Account registration limits have been expanded and now you can set separate counters, or configure components individually for their own limits. Visit `this section <#accountRegLimit>`__ for configuration details.

.. _`_accounts_created_using_in_band_registration_now_will_use_confirmation_e_mail`:

Accounts created using in-band registration now will use confirmation E-mail
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In an effort to create a more secure method for implementing ``JabberIqRegister`` Tigase XMPP Server will now require the use of a confirmation E-mail by default in the process. The E-mail must be valid, and accounts will be made into pending status until a user clicks the generated URI in the E-mail and activates the account. This is a plugin and must be enabled in the ``config.tdsl`` file by using the following code:

.. code:: properties

   'account-registration-email-validator'() {}

.. _`_further_spam_prevention`:

Further Spam prevention
~~~~~~~~~~~~~~~~~~~~~~~

Tigase-spam component is now in ``dist-max`` distribution package, and has a number of features described here `in this section <#tigase_spam_filter>`__.

.. _`_changes_in_password_storage`:

Changes in password storage
---------------------------

Before version 8.0.0, user passwords were stored in plaintext in the ``user_pw`` database field within ``tig_users`` table, but in plaintext. It was possible to enable storage of the MD5 hash of the password instead, however this limited authentication mechanism SASL PLAIN only. However an MD5 hash of a password is not really a secure method as it is possible to revert this mechanism using rainbow tables.

Therefore, we decided to change this and store only encrypted versions of a password in ``PBKDF2`` form which can be easily used for ``SCRAM-SHA-1`` authentication mechanism or ``SCRAM-SHA-256``. ``SASL PLAIN`` mechanism can also use these encrypted passwords.

The storage of encrypted passwords is now enabled **by default** in v8.0.0 of Tigase.

.. _`_dynamic_tls_buffer`:

Dynamic TLS Buffer
------------------

Memory Buffer for TLS no longer remains at highest buffer size needed for the server session. Buffer will now free memory during idle connections. Thus drastically improving program footprint.

.. _`_xep_305_quickstart_now_supported`:

XEP-305 Quickstart now supported
--------------------------------

It’s now possible to establish connection faster due to implementation of `XEP-0305: XMPP Quickstart <https://xmpp.org/extensions/xep-0305.html>`__ (`#1936 <https://projects.tigase.net/issues?q=Redmine%20ID:%201936>`__). Feature is only available for ``c2s`` Connection Manager (i.e. connections on port 5222) and needs to be enabled in ``config.tdsl``

.. code:: dsl

   c2s () {
       'pipelining' = true
   }

.. _`_database_timestamps`:

Database Timestamps
-------------------

Timestamps in database will be stored using UTC time.

.. _`_config_type_properties_have_changed`:

Config-type properties have changed
-----------------------------------

Config-type is now configured using DSL format. Visit `this section <#configType>`__ for more information. The names of different config-type properties have changed: ``default`` replaces ``--gen-config-def``, ``--gen=config-all``, and ``--gen-config-default`` configuration types. ``session-manager`` replaces ``--gen-config-sm``. ``connection-managers`` replaces ``--gen-config-cs``. ``component`` replaces ``--gen-config-comp``. ``setup`` - is a new type of config created for initial configuration of Tigase XMPP Server.

.. note::

   Old versions are no longer supported, you HAVE to replace old versions with the new ones manually when upgrading to v8.0.0.

.. _`_database_watchdog_implemented`:

Database Watchdog implemented
-----------------------------

It is now possible to set connection testing to databases when connections are idle and customize the frequency with which this is done. Visit `this section <#databaseWatchdog>`__ for more details.

.. _`_packet_statistics_expanded`:

Packet statistics expanded
--------------------------

Packet statistics both retrieved VIA XMPP and during graceful shutdown have now been separated to a per-XMLNS basis. This may be disabled by adding the following line to ``config.tdsl`` file:

.. code:: dsl

   'detailed-other-statistics' = false

.. _`_xep_0016_behavior_changes`:

XEP-0016 Behavior changes
-------------------------

XEP states that Privacy lists should be used when no user session exists in addition to when there is. Previously, Tigase would only filter results when retrieving messages, allowing blocked users to store offline messages. This has now been changed to reflect the XEP properly, and messages will be filtered while there is no user session. If however, you wish to use the previous version, where offline messages are cached first and then filtered, you may use the following configuration:

.. code:: dsl

   'sess-man' {
       'jabber:iq:privacy' () {
           privacyListOfflineCache (active: true) {
             size = 20000
           }
       }
   }

By default, the cache has a limit of 10000 entries, that may be set by using size bean as seen above.

.. _`_access_control_list_has_new_acl_modifiers`:

Access Control List has new ACL modifiers
-----------------------------------------

New permissions have been added to ACL including ``DOMAIN_OWNER`` and ``DOMAIN_ADMIN`` to reduce permissions checking, and add another level of fine-grained permissions. For more details, please see `Tigase ACL <#accessControlList>`__ configuration for more details.

.. _`_option_to_ignore_schema_version_check_added`:

Option to ignore schema-version check added
-------------------------------------------

You can now skip the schema check phase for individual databases. To do this, add the following do the datasource configuration block:

.. code:: dsl

   DataSource () {
     default () {
       'schema-management' = false
     }
   }

This will do the following:

-  Print a warning during repository startup.

-  Skip schema upgrades for the source.

-  Skip schema destruction for the source.

.. _`_protection_against_brute_force_attacks`:

Protection against brute-force attacks
--------------------------------------

Version 8.0.0 improves security by preventing brute-force attacks. Feature needs to be explicitly enabled and configured (on per VHost basis). Detailed configuration is described in `??? <#bruteForcePrevention>`__ (`#8160 <https://projects.tigase.net/issues?q=Redmine%20ID:%208160>`__)

.. _`_new_minor_features_behavior_changes`:

New Minor Features & Behavior Changes
=====================================

-  `#611 <https://projects.tigase.net/issues?q=Redmine%20ID:%20611>`__ Support for Message of the Day is now enabled in Tigase XMPP Server and can be administered using `XEP-0133 Service Administration <http://xmpp.org/extensions/xep-0133.html#set-motd>`__.

-  `#1569 <https://projects.tigase.net/issues?q=Redmine%20ID:%201569>`__ Re-implemented XEP-0133 Service Administration Scripts ``4.3 Disable User`` and ``4.4 Re-enable User``.

-  `#1449 <https://projects.tigase.net/issues?q=Redmine%20ID:%201449>`__ Monitoring modules now works in OSGi mode.

-  `#1706 <https://projects.tigase.net/issues?q=Redmine%20ID:%201706>`__ ``auto-authorize`` of presence subscriptions can now be set for individual vhosts.

-  `#1968 <https://projects.tigase.net/issues?q=Redmine%20ID:%201968>`__ Added a Proxy Wrapper to handle reconnections to database connection pool to help prevent deadlocking threads.

-  `#3511 <https://projects.tigase.net/issues?q=Redmine%20ID:%203511>`__ Mechanism responsible for closing XMPP in SessionManager has been changed to process all packets from TCP connection before closing connection.

-  `#3802 <https://projects.tigase.net/issues?q=Redmine%20ID:%203802>`__ Implementation and API of LocalEventBus and ClusteredEventBus has been unified and is now available as EventBus.

-  `#3918 <https://projects.tigase.net/issues?q=Redmine%20ID:%203918>`__ Session Establishment Advertisement is now optional, bringing session establishment in line with `RFC 6121 <https://tools.ietf.org/html/rfc6121>`__.

-  `#4111 <https://projects.tigase.net/issues?q=Redmine%20ID:%204111>`__ Changed input buffer sizing to use a ratio of 2 to 1 based on input capacity. No longer using a constant value.

-  `#4212 <https://projects.tigase.net/issues?q=Redmine%20ID:%204212>`__ Database schema files have been flattened and made for better organization.

-  `#4501 <https://projects.tigase.net/issues?q=Redmine%20ID:%204501>`__ ``CounterDataFileLogger`` now has an upper limit and will be default be shrunk to 75% if available disk space is 5% or less than 100MB.

-  `#4654 <https://projects.tigase.net/issues?q=Redmine%20ID:%204654>`__ PubSub component has been updated and new schema uses UTF-8 encoding when hashing database lookup.

-  `#4776 <https://projects.tigase.net/issues?q=Redmine%20ID:%204776>`__ Tigase ``DbSchemaLoader`` now prompts for password if one is missing from command line.

-  `#4788 <https://projects.tigase.net/issues?q=Redmine%20ID:%204788>`__ Push component added to dist-max archive.

-  `#4814 <https://projects.tigase.net/issues?q=Redmine%20ID:%204814>`__ SASL-SCRAM will now be automatically disabled if auth database uses encoded passwords.

-  `#4844 <https://projects.tigase.net/issues?q=Redmine%20ID:%204844>`__ External components can now have SSL socket connections assigned to them.

-  `#4859 <https://projects.tigase.net/issues?q=Redmine%20ID:%204859>`__ Tigase ``DbSchemaLoader`` now can support using SSL when connecting to databases.

-  `#4874 <https://projects.tigase.net/issues?q=Redmine%20ID:%204874>`__ Tigase Test Suite has been updated to correspond to all changes for v8.0.0.

-  `#4877 <https://projects.tigase.net/issues?q=Redmine%20ID:%204877>`__ In-memory repository implemented for **testing ONLY**.

-  `#4880 <https://projects.tigase.net/issues?q=Redmine%20ID:%204880>`__ Tigase config-type settings have been reduced and changed. See `this section <#configType>`__ for more details.

-  `#4908 <https://projects.tigase.net/issues?q=Redmine%20ID:%204908>`__ Limited Ad-hoc execution to admin only within monitor component.

-  `#5005 <https://projects.tigase.net/issues?q=Redmine%20ID:%205005>`__ Detailed logging configuration is now available in DSL format. See xref:[customLogging] for more details.

-  `#5069 <https://projects.tigase.net/issues?q=Redmine%20ID:%205069>`__ Packet processed statistics now separates results based on XML Namespaces.

-  `#5079 <https://projects.tigase.net/issues?q=Redmine%20ID:%205079>`__ Tigase ``DbSchemaLoader`` can now process multiple .sql files in one command by using a comma separated list when calling.

-  `#5086 <https://projects.tigase.net/issues?q=Redmine%20ID:%205086>`__ Tigase server monitor is loaded after delay to prevent NPE during startup.

-  `#5149 <https://projects.tigase.net/issues?q=Redmine%20ID:%205149>`__ ``StanzaReceiver`` and ``StanzaSender`` Components have been deprecated and are no longer part of Tigase XMPP Server. Related SQL tables ``xmpp_stanza`` and ``short_news`` have also been removed from schemas.

-  `#5150 <https://projects.tigase.net/issues?q=Redmine%20ID:%205150>`__ All TigaseDB tables now use the ``tig_`` prefix.

-  `#5214 <https://projects.tigase.net/issues?q=Redmine%20ID:%205214>`__ Check has been added if recipient exists before storing offline messages for local jid.

-  `#5293 <https://projects.tigase.net/issues?q=Redmine%20ID:%205293>`__ ``DbSchemaLoader`` now will fail execution instead of skipping when encountering missing files.

-  `#5379 <https://projects.tigase.net/issues?q=Redmine%20ID:%205379>`__ Server ready detection has been improved in testrunner.sh.

-  `#5397 <https://projects.tigase.net/issues?q=Redmine%20ID:%205397>`__ Webhelp Documentation will no longer be built.

-  `#5422 <https://projects.tigase.net/issues?q=Redmine%20ID:%205422>`__ Errors with Beans will now result in compact and more readable StackTrace print in console log.

-  `#5423 <https://projects.tigase.net/issues?q=Redmine%20ID:%205423>`__ System configuration will now be printed to log file as ``ConfigHolder.loadConfiguration`` output.

-  `#5425 <https://projects.tigase.net/issues?q=Redmine%20ID:%205425>`__ ``GetAnyFile`` and ``GetConfigFile`` scripts moved to message-router instead of basic-conf.

-  `#5429 <https://projects.tigase.net/issues?q=Redmine%20ID:%205429>`__ Adjusted settings for Dynamic Rostering now can use separate beans for multiple implementations.

-  `#5430 <https://projects.tigase.net/issues?q=Redmine%20ID:%205430>`__ ``BindResource`` is now set to FINER log level to reduce console output verbosity.

-  `#5475 <https://projects.tigase.net/issues?q=Redmine%20ID:%205475>`__ Setting default environment variables is now possible in ``config.tdsl`` file using ``env('env-1', 'def-value')`` lines. Details available `in DSL Configuration <#dslEnv>`__ section.

-  `#5496 <https://projects.tigase.net/issues?q=Redmine%20ID:%205496>`__ ``Destroy Schema`` task now added to schema manager.

-  `#5583 <https://projects.tigase.net/issues?q=Redmine%20ID:%205583>`__ Error messages now properly sent when offline message storage is full.

-  `#5674 <https://projects.tigase.net/issues?q=Redmine%20ID:%205674>`__ All components now use UTC timestamp when interacting with databases.

-  `#5800 <https://projects.tigase.net/issues?q=Redmine%20ID:%205800>`__ Better annotation of deprecated code, cleanup and removal code previously marked as deprecated.

-  `#5964 <https://projects.tigase.net/issues?q=Redmine%20ID:%205964>`__ Server version is now added to JMX statistics.

-  `#5982 <https://projects.tigase.net/issues?q=Redmine%20ID:%205982>`__ Remote JVM debugging configuration added to tigase.conf file, commented by default.

-  `#6038 <https://projects.tigase.net/issues?q=Redmine%20ID:%206038>`__ Data Source pool connections are now initialized concurrently instead of one at a time, dropping initializing time.

-  `#6103 <https://projects.tigase.net/issues?q=Redmine%20ID:%206103>`__ :literal:`RosterElement`no longer keeps `XMPPResourceConnection` instance as it is cached elsewhere. Removal results in net improvement in memory footprint.

-  `#6133 <https://projects.tigase.net/issues?q=Redmine%20ID:%206133>`__ Tigase now checks components against server version to ensure compatibility.

-  `#6163 <https://projects.tigase.net/issues?q=Redmine%20ID:%206163>`__ Groovy plugin updated to v2.4.12.

-  `#6206 <https://projects.tigase.net/issues?q=Redmine%20ID:%206206>`__ Separated TigaseXMLTools and TigaseUtil packages for better compatibility with JDK v9.

-  `#6216 <https://projects.tigase.net/issues?q=Redmine%20ID:%206216>`__ MongoDB Driver now updated to v3.5.0.

-  `#6560 <https://projects.tigase.net/issues?q=Redmine%20ID:%206560>`__ tigase anti-spam component now included in tigase dist-max archive.

-  `#6821 <https://projects.tigase.net/issues?q=Redmine%20ID:%206821>`__ Improved error reporting when errors from ``ConfigReader``.

-  `#6842 <https://projects.tigase.net/issues?q=Redmine%20ID:%206842>`__ ``DefaultTypesConverter`` no longer requires case sensitive enums.

-  `#7082 <https://projects.tigase.net/issues?q=Redmine%20ID:%207082>`__ ``ClassUtilBean`` now handles packet filtering for packets part of Tigase Server but not containing beans, other improvements to mDNS.

-  `#7433 <https://projects.tigase.net/issues?q=Redmine%20ID:%207433>`__ ``SeeOtherHost`` no longer uses ``PropertiesBeanConfigurator`` to parse configuration.

-  `#7446 <https://projects.tigase.net/issues?q=Redmine%20ID:%207446>`__ User credentials can now be managed with Ad-hoc commands.

-  `#7743 <https://projects.tigase.net/issues?q=Redmine%20ID:%207743>`__ Improved error message when repository is not found.

-  `#7773 <https://projects.tigase.net/issues?q=Redmine%20ID:%207773>`__ Ad-hoc commands can now by executed asynchronously.

-  `#2341 <https://projects.tigase.net/issues?q=Redmine%20ID:%202341>`__ allow specifying SubscriptionType when adding buddy to avoid calling separately .setBuddySubscription() thus eliminating saving roster twice to database if not needed

.. _`_fixes`:

Fixes
=====

-  `#2750 <https://projects.tigase.net/issues?q=Redmine%20ID:%202750>`__ Multiple artifact and depreciated file cleanup. Massive code cleanup and javadoc cleaning.

-  `#3582 <https://projects.tigase.net/issues?q=Redmine%20ID:%203582>`__ Schema files streamlined, and no longer embedded in code.

-  `#3611 <https://projects.tigase.net/issues?q=Redmine%20ID:%203611>`__ Fixed TheadExceptionHandler caused by ACS unable to read PubSub schema changes.

-  `#3686 <https://projects.tigase.net/issues?q=Redmine%20ID:%203686>`__ Issues with processing XHTML-IM have been fixed, and now render correctly messages with multiple CData items.

-  `#3689 <https://projects.tigase.net/issues?q=Redmine%20ID:%203689>`__ Packets returned from CM no longer bear the original senders' jid.

-  `#3803 <https://projects.tigase.net/issues?q=Redmine%20ID:%203803>`__ New call ``RouteEvent`` has been added to check to list and check events and determine which should be forwarded to other nodes.

-  `#3822 <https://projects.tigase.net/issues?q=Redmine%20ID:%203822>`__ Error is now thrown if listener is registered for an event that is not found in EventBus.

-  `#3910 <https://projects.tigase.net/issues?q=Redmine%20ID:%203910>`__ Fixed NPE in SessionManager when session is closed during execution of everyMinute method.

-  `#3911 <https://projects.tigase.net/issues?q=Redmine%20ID:%203911>`__ Fixed issue of dropping connections during thread load distribution.

-  `#4185 <https://projects.tigase.net/issues?q=Redmine%20ID:%204185>`__ Fixed an error where messages would be duplicated on stream resumption due to a counter being reset upon reconnection.

-  `#4447 <https://projects.tigase.net/issues?q=Redmine%20ID:%204447>`__ Fixed condition where expired messages in offline store would cause locks.

-  `#4547 <https://projects.tigase.net/issues?q=Redmine%20ID:%204547>`__ config.dump file now is fully compatible with init.tdsl file and DSL file formatting.

-  `#4672 <https://projects.tigase.net/issues?q=Redmine%20ID:%204672>`__ Fixed ``UnsupportedOperationException`` occurring during configuration of ``WebSocketConnectionClustered``.

-  `#4776 <https://projects.tigase.net/issues?q=Redmine%20ID:%204776>`__ ``DBSchemaLoader`` now asks for user credentials if parameter is missing. Exceptions are no longer thrown if file specified is not found.

-  `#4885 <https://projects.tigase.net/issues?q=Redmine%20ID:%204885>`__ ``client-port-delay-listening`` no longer causes exception when called.

-  `#4973 <https://projects.tigase.net/issues?q=Redmine%20ID:%204973>`__ Changed Message History query to now include a limit when selecting items, preventing an SQLTimeoutException.

-  `#5005 <https://projects.tigase.net/issues?q=Redmine%20ID:%205005>`__ Fixed an issue where disabling components would result in server shutdown.

-  `#5042 <https://projects.tigase.net/issues?q=Redmine%20ID:%205042>`__ Fixed issue when implementing custom SASL providers, mechanisms and callback handler factories.

-  `#5066 <https://projects.tigase.net/issues?q=Redmine%20ID:%205066>`__ Fixed issue initializing databases using MongoDB.

-  `#5076 <https://projects.tigase.net/issues?q=Redmine%20ID:%205076>`__ last_login and last_logout values are now properly updated while using SASL SCRAM authentication.

-  `#5084 <https://projects.tigase.net/issues?q=Redmine%20ID:%205084>`__ SCRAM now checks to see if account is disabled before retrieving password.

-  `#5085 <https://projects.tigase.net/issues?q=Redmine%20ID:%205085>`__ Fixed ``too many beans implemented`` error in Monitor Component.

-  `#5088 <https://projects.tigase.net/issues?q=Redmine%20ID:%205088>`__ Removed unnecessary SASL request processing after session is closed.

-  `#5118 <https://projects.tigase.net/issues?q=Redmine%20ID:%205118>`__ Fixed NPE during query of privacy lists then ``type`` is missing.

-  `#5303 <https://projects.tigase.net/issues?q=Redmine%20ID:%205303>`__ Fixed beans not being overridden by configuration if they were registered in ``RegistrarBean`` or ``AbstractKernelBasedComponent``.

-  `#5311 <https://projects.tigase.net/issues?q=Redmine%20ID:%205311>`__ Offline messages are no longer dumped from MongoDB when restarting server.

-  `#5394 <https://projects.tigase.net/issues?q=Redmine%20ID:%205394>`__ Loading main Derby schema no longer throws exceptions.

-  `#5428 <https://projects.tigase.net/issues?q=Redmine%20ID:%205428>`__ Fixed parsing of v-host per domain limit property.

-  `#5450 <https://projects.tigase.net/issues?q=Redmine%20ID:%205450>`__ Server no longer automatically shuts down when default or other db can not be found or accessed.

-  `#5458 <https://projects.tigase.net/issues?q=Redmine%20ID:%205458>`__ Fixed potential timeout arising from ``XMPPIOService::xmppStreamOpened()`` method.

-  `#5480 <https://projects.tigase.net/issues?q=Redmine%20ID:%205480>`__ Fixed issue in Derby DB where obtaining offline messages results in SQLException.

-  `#5525 <https://projects.tigase.net/issues?q=Redmine%20ID:%205525>`__ Fixed S2S ``invalid-namespace`` error being returned during connection establishment.

-  `#5587 <https://projects.tigase.net/issues?q=Redmine%20ID:%205587>`__ Fixed unclosed ``ResultSet`` when storing a message to AMP-offline database in Derby causing deadlock.

-  `#5645 <https://projects.tigase.net/issues?q=Redmine%20ID:%205645>`__ Added fix for possible NPE when failing to retrieve beans.

-  `#5670 <https://projects.tigase.net/issues?q=Redmine%20ID:%205670>`__ config-dump now prints configuration for inactive components and beans to log.

-  `#5692 <https://projects.tigase.net/issues?q=Redmine%20ID:%205692>`__ Messages sent with negative priority were being occasionally dropped and not processed to ``OfflineMessageHandler``.

-  `#5727 <https://projects.tigase.net/issues?q=Redmine%20ID:%205727>`__ Fixed potential issue with MySQL procedures not being killed properly.

-  `#5750 <https://projects.tigase.net/issues?q=Redmine%20ID:%205750>`__ Statistics now filter out zero-value results unless FINEST level is requested.

-  `#5831 <https://projects.tigase.net/issues?q=Redmine%20ID:%205831>`__ Fixed occurrence of ``OutOfMemory`` error.

-  `#5864 <https://projects.tigase.net/issues?q=Redmine%20ID:%205864>`__ Fixed NPE when executing BOSH pre-bind script.

-  `#5867 <https://projects.tigase.net/issues?q=Redmine%20ID:%205867>`__ Fixed NPE occurring during configuration dump.

-  `#6000 <https://projects.tigase.net/issues?q=Redmine%20ID:%206000>`__ Fixed a few issues with dynamic rosters properly handling presence subscription requests.

-  `#6006 <https://projects.tigase.net/issues?q=Redmine%20ID:%206006>`__ Improved configuration file and DB Schema handling.

-  `#6041 <https://projects.tigase.net/issues?q=Redmine%20ID:%206041>`__ Fixed potential issue where vhosts DB could be overwritten by vhosts configuration in ``init.config``.

-  `#6078 <https://projects.tigase.net/issues?q=Redmine%20ID:%206078>`__ Fixed ``ClusterConnectionManager`` to use custom_elements_limit instead of a fixed value.

-  `#6080 <https://projects.tigase.net/issues?q=Redmine%20ID:%206080>`__ Fixed Packet Filtering to not filter cluster node information requests.

-  `#6083 <https://projects.tigase.net/issues?q=Redmine%20ID:%206083>`__ Fixed clustered mode shutting down server when certain components are disabled.

-  `#6135 <https://projects.tigase.net/issues?q=Redmine%20ID:%206135>`__ Tigase now properly enabled selective TLS if not enabled globally.

-  `#6140 <https://projects.tigase.net/issues?q=Redmine%20ID:%206140>`__ Fixed issue while sending server welcome message.

-  `#6141 <https://projects.tigase.net/issues?q=Redmine%20ID:%206141>`__ Fixed NPE at startup.

-  `#6234 <https://projects.tigase.net/issues?q=Redmine%20ID:%206234>`__ Fixed an error where an error message would repeat unnecessarily.

-  `#6284 <https://projects.tigase.net/issues?q=Redmine%20ID:%206284>`__ Ad-hoc commands now refresh SSL Certificate, and restart is no longer required.

-  `#6293 <https://projects.tigase.net/issues?q=Redmine%20ID:%206293>`__ Server no longer sends no response upon setting empty photo in vCard.

-  `#6263 <https://projects.tigase.net/issues?q=Redmine%20ID:%206263>`__ Fixed missing namespaces in responses from adhoc commands.

-  `#6400 <https://projects.tigase.net/issues?q=Redmine%20ID:%206400>`__ Added a proper error when max-queue-size is too small and server cannot start.

-  `#6408 <https://projects.tigase.net/issues?q=Redmine%20ID:%206408>`__ Fixed an issue where single WebSocket frames contained multiple XML stanzas instead of one per frame.

-  `#6411 <https://projects.tigase.net/issues?q=Redmine%20ID:%206411>`__ Main kernel is now called to smooth shutdown. Further, timeout periods are opened up for large instances.

-  `#6574 <https://projects.tigase.net/issues?q=Redmine%20ID:%206574>`__ SSL certificate upload handling is now fixed within cluster mode.

-  `#6598 <https://projects.tigase.net/issues?q=Redmine%20ID:%206598>`__ Fixed EventBus Registration connection issues between cluster nodes.

-  `#6658 <https://projects.tigase.net/issues?q=Redmine%20ID:%206658>`__ Cluster connections no longer potentially keep open connection after cluster is no longer connected or available.

-  `#6749 <https://projects.tigase.net/issues?q=Redmine%20ID:%206749>`__ Fixed schema parsing for DerbyDB.

-  `#6776 <https://projects.tigase.net/issues?q=Redmine%20ID:%206776>`__ Fixed failing Websocket connections if header contains more than one value.

-  `#6875 <https://projects.tigase.net/issues?q=Redmine%20ID:%206875>`__ Fixed an issue where C2S connections could be accepted before SessionManager was initialized.

-  `#7037 <https://projects.tigase.net/issues?q=Redmine%20ID:%207037>`__ Fixed error while parsing negative values from ``config.tdsl`` file.

-  `#7055 <https://projects.tigase.net/issues?q=Redmine%20ID:%207055>`__ Improvements to metaspace use and other memory use tweaks.

-  `#7304 <https://projects.tigase.net/issues?q=Redmine%20ID:%207304>`__ Virtual host logs now properly follow log size limits.

-  `#7431 <https://projects.tigase.net/issues?q=Redmine%20ID:%207431>`__ AdHoc requests between the same user with different resources are no longer dropped with \`NoConnectionIdExecption`error.

-  `#7434 <https://projects.tigase.net/issues?q=Redmine%20ID:%207434>`__ Adjusted ``SeeOtherHotDualIP`` to use new table name in cluster nodes database.

-  `#7491 <https://projects.tigase.net/issues?q=Redmine%20ID:%207491>`__ Stacktraces from ``CertificateContainer`` are no longer printed to tigase-console.log, but will be printed to tigase.log.

-  `#7687 <https://projects.tigase.net/issues?q=Redmine%20ID:%207687>`__ Fixed an error where connections failed after authentication timeout were marked as active after cleanup.

-  `#7747 <https://projects.tigase.net/issues?q=Redmine%20ID:%207747>`__ Fixed ``ClusterRepoItemEvent`` serialization issues causing unsupported conversion error in cluster mode.

-  `#7495 <https://projects.tigase.net/issues?q=Redmine%20ID:%207495>`__ fix issue with not all logs being obfuscated, added testcase, documentation

-  `#8305 <https://projects.tigase.net/issues?q=Redmine%20ID:%208305>`__ fix issue with SeeOtherHostDualIP when using MongoDB

.. _`_component_changes`:

Component Changes
=================

.. _`_amp`:

AMP
---

-  `#7301 <https://projects.tigase.net/issues?q=Redmine%20ID:%207301>`__ Tigase AMP component now uses multiple processing threads.

.. _`_pubsub`:

PubSub
------

-  `#5033 <https://projects.tigase.net/issues?q=Redmine%20ID:%205033>`__ PubSub now compatible with using emojis in pubsub items.

-  `#5693 <https://projects.tigase.net/issues?q=Redmine%20ID:%205693>`__ Fixed parsing configuration of SessionManager processors.

-  `#5766 <https://projects.tigase.net/issues?q=Redmine%20ID:%205766>`__ PubSub now writes to all databases with UTC timestamp.

-  `#5953 <https://projects.tigase.net/issues?q=Redmine%20ID:%205953>`__ Fixed presences not being removed from ``presenceByService`` collection if client disconnects without ``<unavailable/>`` presence being sent.

-  `#6176 <https://projects.tigase.net/issues?q=Redmine%20ID:%206176>`__ version changed to PubSub v4.0.0.

-  `#7707 <https://projects.tigase.net/issues?q=Redmine%20ID:%207707>`__ Fixed potential NPE in PubSub.

.. _`_http_api`:

http-api
--------

-  `#4873 <https://projects.tigase.net/issues?q=Redmine%20ID:%204873>`__ Support added to display timestamp fields as data, time, and timezone fields.

-  `#4876 <https://projects.tigase.net/issues?q=Redmine%20ID:%204876>`__ Implemented using XML repository for new setups, and updated default config to use this.

-  `#4888 <https://projects.tigase.net/issues?q=Redmine%20ID:%204888>`__ ``http-api`` now is enabled by default.

-  `#5209 <https://projects.tigase.net/issues?q=Redmine%20ID:%205209>`__ Updated visual styling of pages hosted by component.

-  `#5290 <https://projects.tigase.net/issues?q=Redmine%20ID:%205290>`__ Fixed invalid property name.

-  `#5316 <https://projects.tigase.net/issues?q=Redmine%20ID:%205316>`__ Account Registration now can now require and send confirmation E-mails.

-  `#5415 <https://projects.tigase.net/issues?q=Redmine%20ID:%205415>`__ Web Setup now checks configuration for message archive conflicts.

-  `#5460 <https://projects.tigase.net/issues?q=Redmine%20ID:%205460>`__ MongoDB now supported through web-setup.

-  `#5717 <https://projects.tigase.net/issues?q=Redmine%20ID:%205717>`__ Fixed default values of check-boxes in admin UI not being shown.

-  `#5950 <https://projects.tigase.net/issues?q=Redmine%20ID:%205950>`__ Supported added for `XEP-0363: HTTP File Upload <https://xmpp.org/extensions/xep-0363.html>`__.

-  `#6159 <https://projects.tigase.net/issues?q=Redmine%20ID:%206159>`__ Fixed NPE thrown if scripts directory is not present.

-  `#6176 <https://projects.tigase.net/issues?q=Redmine%20ID:%206176>`__ version changed to tigase-http-api v2.0.0.

-  `#6212 <https://projects.tigase.net/issues?q=Redmine%20ID:%206212>`__ Added mechanism for password changing through HTTP API.

-  `#7307 <https://projects.tigase.net/issues?q=Redmine%20ID:%207307>`__ Fixed scripts returning 404 while handling rest/user/ requests even though user exists.

-  `#7178 <https://projects.tigase.net/issues?q=Redmine%20ID:%207178>`__ Ad-hoc commands are now categorized in groups for better organization.

-  `#7568 <https://projects.tigase.net/issues?q=Redmine%20ID:%207568>`__ Added timeout reading for HTTP request headers, added configurable ``accept-timeout``.

.. _`_message_archive`:

message-archive
---------------

-  `#4867 <https://projects.tigase.net/issues?q=Redmine%20ID:%204867>`__ fixed issue when changing MA jid.

-  `#4888 <https://projects.tigase.net/issues?q=Redmine%20ID:%204888>`__ ``message-archive`` is enabled by default.

-  `#5033 <https://projects.tigase.net/issues?q=Redmine%20ID:%205033>`__ Update message archive to be compatible with emojis.

-  `#5391 <https://projects.tigase.net/issues?q=Redmine%20ID:%205391>`__ Added missing query statement block starts and ends to be compatible with SQL Server.

-  `#5604 <https://projects.tigase.net/issues?q=Redmine%20ID:%205604>`__ Modified access to static fields and functions.

-  `#5681 <https://projects.tigase.net/issues?q=Redmine%20ID:%205681>`__ Fixed duplication of groupchat messages with different ids by modifying hash algorithm.

-  `#6176 <https://projects.tigase.net/issues?q=Redmine%20ID:%206176>`__ version changed to message-archive v2.0.0.

-  `#7615 <https://projects.tigase.net/issues?q=Redmine%20ID:%207615>`__ ``feature-not-implemented`` response no longer occurs when removing stored messages.

.. _`_muc`:

MUC
---

-  `#4888 <https://projects.tigase.net/issues?q=Redmine%20ID:%204888>`__ ``muc`` now is enabled by default.

-  `#5033 <https://projects.tigase.net/issues?q=Redmine%20ID:%205033>`__ MUC component is now compatible with emojis.

-  `#5066 <https://projects.tigase.net/issues?q=Redmine%20ID:%205066>`__ Fixed issues working with MongoDB repository.

-  `#5085 <https://projects.tigase.net/issues?q=Redmine%20ID:%205085>`__ Removed invalid annotation parameter values.

-  `#5559 <https://projects.tigase.net/issues?q=Redmine%20ID:%205559>`__ Fixed NPE while changing default room configuration.

-  `#5666 <https://projects.tigase.net/issues?q=Redmine%20ID:%205666>`__ User may add more than one ``<item/>`` elements to query when querying room members.

-  `#5715 <https://projects.tigase.net/issues?q=Redmine%20ID:%205715>`__ Welcome messages may now be disabled globally, or in individual room configurations.

-  `#5736 <https://projects.tigase.net/issues?q=Redmine%20ID:%205736>`__ Rooms with no subject now return empty ``<subject/>`` element, as per `XEP-0048 7.2.16 <https://xmpp.org/extensions/xep-0045.html#enter-subject>`__.

-  `#5813 <https://projects.tigase.net/issues?q=Redmine%20ID:%205813>`__ Fixed NPE during room creation.

-  `#6176 <https://projects.tigase.net/issues?q=Redmine%20ID:%206176>`__ version changed to tigase-muc v3.0.0.

-  `#6395 <https://projects.tigase.net/issues?q=Redmine%20ID:%206395>`__ Fixed ``tigase.db.UserNotFoundException`` during retrieval of MUC user.

-  `#6734 <https://projects.tigase.net/issues?q=Redmine%20ID:%206734>`__ Introduced ``muc#roomconfig_maxresources`` to allow configuration of max number of resources for a single occupant.

-  `#7443 <https://projects.tigase.net/issues?q=Redmine%20ID:%207443>`__ Disabled XEP-0091 by default, added history attribute validation.

.. _`_socks5_proxy`:

socks5 Proxy
------------

-  `#2750 <https://projects.tigase.net/issues?q=Redmine%20ID:%202750>`__ Cleanup of code and removal of empty javadocs.

-  `#5867 <https://projects.tigase.net/issues?q=Redmine%20ID:%205867>`__ Fixed NPE during configuration dump when component is disabled.

-  `#6176 <https://projects.tigase.net/issues?q=Redmine%20ID:%206176>`__ version changed to tigase-socks5 v2.0.0.

.. _`_stats`:

stats
-----

-  `#5206 <https://projects.tigase.net/issues?q=Redmine%20ID:%205206>`__ Fixed exception causing duplicate error entry.

-  `#5728 <https://projects.tigase.net/issues?q=Redmine%20ID:%205728>`__ Fixed ``MySQLIntegrityConstraintViolationException`` in upload handler.

-  `#6161 <https://projects.tigase.net/issues?q=Redmine%20ID:%206161>`__ Removed usage of classes from javax.xml.ws package for JDKv9 compatibility.

.. _`_stun_server`:

STUN Server
-----------

-  `#6176 <https://projects.tigase.net/issues?q=Redmine%20ID:%206176>`__ version changed to tigase-stun v2.0.0.

.. _`_websocket`:

WebSocket
---------

-  `#6481 <https://projects.tigase.net/issues?q=Redmine%20ID:%206481>`__ Websocket component has been improved to be more compliant with `rfc6455 <https://tools.ietf.org/html/rfc6455>`__
