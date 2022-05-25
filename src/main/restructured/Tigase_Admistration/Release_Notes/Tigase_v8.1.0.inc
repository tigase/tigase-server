Tigase XMPP Server 8.1.0 Change notes and announcement
------------------------------------------------------

.. _`_major_changes`:

Major Changes
=============

.. _`_more_xmpp_extensions`:

More XMPP extensions
--------------------

Following XMPP guidelines specified in *Compliance Suites* a number of extensions was included in this release:

-  **XEP-0157**: Contact Addresses for XMPP Services (`server-995 <https://projects.tigase.net/issue/server-995>`__) that can be configured on per VHost basis (`server-1015 <https://projects.tigase.net/issue/server-1015>`__)

-  **XEP-0398**: User Avatar to vCard-Based Avatars Conversion (`server-1017 <https://projects.tigase.net/issue/server-1017>`__)

-  **XEP-0156**: Discovering Alternative XMPP Connection Methods - Tigase already supported handling DNS queries and standardised our ``webservice`` to XEP-0156 (`http-76 <https://projects.tigase.net/issue/http-76>`__)

-  **XEP-0410**: MUC Self-Ping (Schrödinger’s Chat) (`muc-122 <https://projects.tigase.net/issue/http-76>`__)

-  **XEP-0153**: vCard-Based Avatars - added support for setting **vCard avatar for MUC rooms** (`muc-112 <https://projects.tigase.net/issue/http-76>`__)

-  **XEP-0411**: Bookmarks Conversion (`pubsub-79 <https://projects.tigase.net/issue/http-76>`__)

-  **XEP-0157**: Contact Addresses for XMPP Services (`server-995 <https://projects.tigase.net/issue/server-995>`__)

.. _`_improved_connectivity_with_other_servers`:

Improved connectivity with other servers
----------------------------------------

``SASL-EXTERNAL`` mechanism was added for server-to-server (federated, s2s) connections greatly improving compliance with XMPP network. It’s possible to use both SASL-EXTERNAL and Diallback depending on support in other servers.

.. _`_better_security_privacy`:

Better security & privacy
-------------------------

When it comes to connectivity, Tigase XMPP Server sported **Hardened Mode** that adjusted networking security settings (supported protocols, cipher suites and keys' length where applicable). We decided include 3-level configuration option for **Hardened Mode** (roughly following *Mozilla’s SSL Configuration Generator*): ``relaxed``, ``secure`` (default) and ``strict`` and to further eliminate cipher suites that are currently considered insecure.

We also enabled by default our anti-spam plugin and because we like all-things-extensible we created a guide how to create your own pluggable filters for anti-spam-plugin.

.. _`_multiple_domains_vhosts_support_is_even_better`:

Multiple domains (VHosts) support is even better
------------------------------------------------

It was always quite easy to configure and serve multiple domains in Tigase XMPP Server. In this release we made it even better! First of all - we included ``Default`` VHost item, which allows configuring global defaults for the installation on the fly without having to change configuration files and restart the instance.

Internally, we introduced *VHost Extensions* - a mechanism that allows easy addition of configurable options that can be set on per-domain basis.

On top of that we reworked how SSL certificates are handled (especially wildcard ones) and now they are loaded and assigned to correct domain automatically - no need to configure *star*-certificates manually anymore.

.. _`_mobile_first`:

Mobile First
------------

Notifications send to mobile applications via Apple’s and Google’s push servers using **Tigase’s PUSH component** are now encrypted (`#push-25 <https://projects.tigase.net/issue/push-25>`__), requires compatible clients)

MUC component now allows users to register permanent nickname, which makes it possible to receive PUSH notifications even if our client disconnects and is offline (`#muc-115 <https://projects.tigase.net/issue/muc-115>`__)

.. _`_installation_management`:

Installation & management
-------------------------

The (web) installer was simplified making setting up and configuring Tigase even easier (`#http-78 <https://projects.tigase.net/issue/http-78>`__) - now it’s only needed to select desired database, provide it’s details and eventually adjust which components and plugins should be enabled or disabled, but we believe that provided defaults should work well in most of the cases.

After the installation and startup, it’s possible to see basic instance state via web browser either opening ``/server/`` endpoint (`#server-1164 <https://projects.tigase.net/issue/server-1164>`__), or local file from ``logs/server-info.html``) and manage the installation using Admin WebUI, that received slight visual face-lift (`#http-90 <https://projects.tigase.net/issue/http-90>`__)

.. _`_noteworthy`:

Noteworthy
----------

-  Startup time was significantly reduced due to improvements of creating repository pools (`#server-1149 <https://projects.tigase.net/issue/server-1149>`__)

-  Multi-thread, highly concurrent script execution was improved (`#server-1154 <https://projects.tigase.net/issue/server-1154>`__)

-  StreamManagement was available, but in this version we decided to enabled it by default.

-  More places offers support for `XEP-0059: Result Set Management <https://xmpp.org/extensions/xep-0059.html>`__ - namely PubSub nodes discovery and ``jabber:iq:serach``

-  `Publishing Options <https://xmpp.org/extensions/xep-0060.html#publisher-publish-options>`__ were added to PubSub (`#pubsub-75 <https://projects.tigase.net/issue/pubsub-75>`__)

.. _`_new_minor_features_behavior_changes`:

New Minor Features & Behavior Changes
=====================================

-  `server-918 <https://projects.tigase.net/issue/server-918>`__: AWS obtain public IP and/or DNS address of the EC2 instance

-  `server-985 <https://projects.tigase.net/issue/server-985>`__: Add support for SCRAM-SHA-512(-PLUS)

-  `spam-8 <https://projects.tigase.net/issue/spam-8>`__: Enable spam processor by default

-  `server-1012 <https://projects.tigase.net/issue/server-1012>`__: UserDomainFilter.groovy fails to load

-  `server-1014 <https://projects.tigase.net/issue/server-1014>`__: Can’t upgrade from 8.0.0GA to 8.1.0-SNAPSHOT

-  `server-798 <https://projects.tigase.net/issue/server-798>`__: Limit number of messages that are stored in DB per user within a period of time

-  `server-827 <https://projects.tigase.net/issue/server-827>`__: Seperate Component-based statistics

-  `server-1026 <https://projects.tigase.net/issue/server-1026>`__: NPE: in JabberIqRegister/EmailConfirmationSender

-  `pubsub-82 <https://projects.tigase.net/issue/pubsub-82>`__: NPE in RetrieveItemsModule

-  `tigaseim-78 <https://projects.tigase.net/issue/tigaseim-78>`__: IPv6 connectivity issue

-  `server-239 <https://projects.tigase.net/issue/server-239>`__: OSGi mode - exceptions in logs

-  `server-1020 <https://projects.tigase.net/issue/server-1020>`__: Enable stream management by default

-  `pubsub-83 <https://projects.tigase.net/issue/pubsub-83>`__: NPE in PublishItemModule

-  `pubsub-81 <https://projects.tigase.net/issue/pubsub-81>`__: Exception during execution of event: tigase.pubsub.modules.PresenceCollectorModule.PresenceChangeEvent

-  `server-1021 <https://projects.tigase.net/issue/server-1021>`__: NPE: Cannot update BruteForceLocker

-  `server-826 <https://projects.tigase.net/issue/server-826>`__: UserRepository caches force synchronization even if caching is disabled

-  `server-958 <https://projects.tigase.net/issue/server-958>`__: Add timeout for opened TCP connections

-  `server-1029 <https://projects.tigase.net/issue/server-1029>`__: Read receipients are not copied via carbons

-  `server-1015 <https://projects.tigase.net/issue/server-1015>`__: Allow configuring XEP-0157: Contact Addresses on per VHost basis

-  `pubsub-65 <https://projects.tigase.net/issue/pubsub-65>`__: RSM and jabber:search for pubsub discovery

-  `server-1030 <https://projects.tigase.net/issue/server-1030>`__: NPE in VCardTemp when processing initial presence

-  `http-72 <https://projects.tigase.net/issue/http-72>`__: Change Content-Disposition from attachment to inline

-  `server-1045 <https://projects.tigase.net/issue/server-1045>`__: NPE in DiscoExtensionsForm

-  `server-1048 <https://projects.tigase.net/issue/server-1048>`__: Update parent pom and information about suggested JDK

-  `push-23 <https://projects.tigase.net/issue/push-23>`__: [JDK12] Can’t establish encrypted connection with Push/FCM

-  `server-978 <https://projects.tigase.net/issue/server-978>`__: Improve VHost configuration / extending

-  `server-1068 <https://projects.tigase.net/issue/server-1068>`__: Improve LogFormat readability (and maybe performance)

-  `server-1070 <https://projects.tigase.net/issue/server-1070>`__: Improve privacy list loggging

-  `server-1071 <https://projects.tigase.net/issue/server-1071>`__: NPE in IOService.accept

-  `server-710 <https://projects.tigase.net/issue/server-710>`__: Registration improvements

-  `pubsub-79 <https://projects.tigase.net/issue/pubsub-79>`__: XEP-0411: Bookmarks Conversion

-  `pubsub-75 <https://projects.tigase.net/issue/pubsub-75>`__: Add support for Publishing Options

-  `server-1017 <https://projects.tigase.net/issue/server-1017>`__: XEP-0398: User Avatar to vCard-Based Avatars Conversion

-  `server-994 <https://projects.tigase.net/issue/server-994>`__: Add server support for Entity Capabilities: Stream Feature

-  `server-995 <https://projects.tigase.net/issue/server-995>`__: XEP-0157: Contact Addresses for XMPP Services

-  `http-76 <https://projects.tigase.net/issue/http-76>`__: Standardise DNS webservice to XEP-0156

-  `server-1109 <https://projects.tigase.net/issue/server-1109>`__: Add recommended JDK version to documentation

-  `push-28 <https://projects.tigase.net/issue/push-28>`__: Non-tigase notifications should use high priority (APNS)

-  `server-1114 <https://projects.tigase.net/issue/server-1114>`__: Can’t register on sure.im with StorkIM

-  `server-1005 <https://projects.tigase.net/issue/server-1005>`__: Flatten schema to match versioning document

-  `server-1116 <https://projects.tigase.net/issue/server-1116>`__: account_status is not checked

-  `server-1074 <https://projects.tigase.net/issue/server-1074>`__: Hardened Mode improvements

-  `server-1125 <https://projects.tigase.net/issue/server-1125>`__: StatsDumper.groovy doesn’t work in documentation in 8.x

-  `http-85 <https://projects.tigase.net/issue/http-85>`__: Pasword resset doesn’t work

-  `server-1128 <https://projects.tigase.net/issue/server-1128>`__: Possible vulnerability in XML parser

-  `server-1130 <https://projects.tigase.net/issue/server-1130>`__: NPE i JabberIqAuth

-  `http-84 <https://projects.tigase.net/issue/http-84>`__: Configurable ``resetPassword`` endpoint hostname

-  `server-1129 <https://projects.tigase.net/issue/server-1129>`__: BOSH timeouts on GET requests

-  `prv-436 <https://projects.tigase.net/issue/prv-436>`__: Conversations compliance - contact developers

-  `server-1100 <https://projects.tigase.net/issue/server-1100>`__: CAAS and WS testers fail to connect to wss://tigase.im:5291

-  `server-1047 <https://projects.tigase.net/issue/server-1047>`__: Add SASL-EXTERNAL on s2s conections

-  `server-1103 <https://projects.tigase.net/issue/server-1103>`__: High priority PUSH notifications are sent for all messages

-  `pubsub-93 <https://projects.tigase.net/issue/pubsub-93>`__: NPE in CapsChangeEvent

-  `server-1137 <https://projects.tigase.net/issue/server-1137>`__: Don’t require setting JAVA_HOME to start server

-  `server-1136 <https://projects.tigase.net/issue/server-1136>`__: upgrade-schema --help not available

-  `utils-19 <https://projects.tigase.net/issue/utils-19>`__: tigase-utils doesn’t compile with JDK12

-  `server-1138 <https://projects.tigase.net/issue/server-1138>`__: Schema files are not sorted correctly during loading

-  `pubsub-98 <https://projects.tigase.net/issue/pubsub-98>`__: Resources with emoji chars are causing issues with MySQL backend

-  `server-1110 <https://projects.tigase.net/issue/server-1110>`__: Disabling TLS in VHost configuration doesn’t work

-  `server-1078 <https://projects.tigase.net/issue/server-1078>`__: Don’t send root CA certificate in chain

-  `server-1113 <https://projects.tigase.net/issue/server-1113>`__: Don’t advertise SASL-EXTERNAL if own certificate is not valid

-  `http-78 <https://projects.tigase.net/issue/http-78>`__: Simplify installer

-  `server-1133 <https://projects.tigase.net/issue/server-1133>`__: Not able to connect via S2S to server with incorrect SSL certificate

-  `serverdistribution-2 <https://projects.tigase.net/issue/serverdistribution-2>`__: MUC upgrade not linked correctly in global tigase guide

-  `server-1149 <https://projects.tigase.net/issue/server-1149>`__: Reduce startup time with a lot of database connections

-  `server-1148 <https://projects.tigase.net/issue/server-1148>`__: "ERROR! Component <x> schema version is not loaded in the database or it is old!" during shutdown

-  `server-1153 <https://projects.tigase.net/issue/server-1153>`__: Refactor Credentials related ``username`` to ``credentialId`` to avoid confussion

-  `servers-312 <https://projects.tigase.net/issue/servers-312>`__: No cluster connection to send a packet

-  `server-1154 <https://projects.tigase.net/issue/server-1154>`__: Multi-thread script execution yields wrong results

-  `servers-294 <https://projects.tigase.net/issue/servers-294>`__: Can’t connect from tigase.im to rsocks.net

-  `server-1111 <https://projects.tigase.net/issue/server-1111>`__: Can’t establish s2s to upload.pouet.ovh

-  `server-1143 <https://projects.tigase.net/issue/server-1143>`__: S2S connectivity issue with OpenFire when SASL external is used

-  `servers-309 <https://projects.tigase.net/issue/servers-309>`__: Issue when connecting to xabber.org: not-authorized: self signed certificate

-  `tigaseim-80 <https://projects.tigase.net/issue/tigaseim-80>`__: Siskin IM push server is not accessible

-  `server-1080 <https://projects.tigase.net/issue/server-1080>`__: After updating certificate via ad-hoc/rest only main certificate is updated

-  `http-88 <https://projects.tigase.net/issue/http-88>`__: Improve REST documentation

-  `http-87 <https://projects.tigase.net/issue/http-87>`__: "request accept time exceeded" for every request when using ``JavaStandaloneHttpServer``

-  `server-1151 <https://projects.tigase.net/issue/server-1151>`__: BruteForceLockerExtension (and possibly others) settings are not correctly retrieved

-  `http-89 <https://projects.tigase.net/issue/http-89>`__: Drop result/error packages received by HTTP-API if no connection present to write response to

-  `pubsub-99 <https://projects.tigase.net/issue/pubsub-99>`__: Notifications are not sent for +notify from nodes with whitelist access mode

-  `pubsub-79 <https://projects.tigase.net/issue/pubsub-79>`__: XEP-0411: Bookmarks Conversion

-  `server-1157 <https://projects.tigase.net/issue/server-1157>`__: SCRAM-SHA512 not working

-  `server-1159 <https://projects.tigase.net/issue/server-1159>`__: Improve handling establishing and terminating of the session

-  `server-1152 <https://projects.tigase.net/issue/server-1152>`__: Cleanup warnings from JDBCMsgRepository

-  `server-1112 <https://projects.tigase.net/issue/server-1112>`__: Fallback to diallback if SASL-EXTERNAL fails

-  `servers-292 <https://projects.tigase.net/issue/servers-292>`__: S2S connectivity issues

-  `acspubsub-19 <https://projects.tigase.net/issue/acspubsub-19>`__: REST execution fails on other nodes

-  `server-1145 <https://projects.tigase.net/issue/server-1145>`__: Race condition during storing/loading of offline messages

-  `http-90 <https://projects.tigase.net/issue/http-90>`__: Add direct links to most useful task in AdminUI main page

-  `spam-10 <https://projects.tigase.net/issue/spam-10>`__: Add documentation for creation of a custom filter

-  `server-1163 <https://projects.tigase.net/issue/server-1163>`__: Review and update ``SASL Custom Mechanisms and Configuration`` documentation

-  `server-1164 <https://projects.tigase.net/issue/server-1164>`__: After-installation report - installation status

-  `systems-76 <https://projects.tigase.net/issue/systems-76>`__: Fix issue with StackOverflow due to recursive call in TLSIO; improve debug log

-  `server-1082 <https://projects.tigase.net/issue/server-1082>`__: Sec-WebSocket-Accept not calculated correctly

-  `server-1083 <https://projects.tigase.net/issue/server-1083>`__: Messages sent to full jid are returned with error

-  `push-25 <https://projects.tigase.net/issue/push-25>`__: Add support for sending encrypted PUSHes

-  `server-1085 <https://projects.tigase.net/issue/server-1085>`__: Improve retrieval of values for all keys in a node in UserRepository

-  `muc-115 <https://projects.tigase.net/issue/muc-115>`__: Add support for MUC and offline message delivery

-  `muc-122 <https://projects.tigase.net/issue/muc-122>`__: XEP-0410: MUC Self-Ping (Schrödinger’s Chat)

-  `muc-112 <https://projects.tigase.net/issue/muc-112>`__: Support for setting vCard avatar for room

-  `http-83 <https://projects.tigase.net/issue/http-83>`__: Issue with multithreading access to HttpExchange instance

-  `httpapijetty-3 <https://projects.tigase.net/issue/httpapijetty-3>`__: Support for HTTP/2

-  `httpapijetty-6 <https://projects.tigase.net/issue/httpapijetty-6>`__: Update Jetty version
