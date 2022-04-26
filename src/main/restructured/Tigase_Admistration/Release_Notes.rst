

1. Tigase XMPP Server 8.3.0 Release Notes
==========================================

Welcome to Tigase XMPP Server 8.3.0! This is a feature release a number of fixes and updates. Here is the list of most important features and changes and below the list of all release notes from all included components

1.1. Highlights
----------------

This version requires JDK17 to run

1.2. Other significant changes
------------------------------

1.3. Per-component changes
-----------------------------

1.3.1. Tigase XMPP Server 8.2.0 release notes
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Tigase XMPP Server 8.3.0 Change notes

Major Changes
~~~~~~~~~~~~~

All Minor Features & Behavior Changes
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

1.3.2. Tigase MIX 1.0.0 Release Notes
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Major Changes
~~~~~~~~~~~~~~

This is the introductory version of `MIX specification family <https://xmpp.org/extensions/xep-0369.html#family>`__


All Changes
~~~~~~~~~~~~~~

-  `#mix-2 <https://projects.tigase.net/issue/mix-2>`__:Implement XEP-0369: Mediated Information eXchange (MIX)

-  `#mix-3 <https://projects.tigase.net/issue/mix-3>`__:Implement XEP-0406: Mediated Information eXchange (MIX): MIX Administration

-  `#mix-6 <https://projects.tigase.net/issue/mix-6>`__:Create tests for MIX CORE & Participants Server Requirements

-  `#mix-8 <https://projects.tigase.net/issue/mix-8>`__:Improve caching

-  `#mix-9 <https://projects.tigase.net/issue/mix-9>`__:Add support for MIX-MUC integration

-  `#mix-10 <https://projects.tigase.net/issue/mix-10>`__:Invalid response for disco#items

-  `#mix-14 <https://projects.tigase.net/issue/mix-14>`__:Add configuration to limit who can create channels in component

-  `#mix-15 <https://projects.tigase.net/issue/mix-15>`__:NPE in MAMItemHandler

-  `#mix-16 <https://projects.tigase.net/issue/mix-16>`__:Add MIX to installer as option.

-  `#mix-17 <https://projects.tigase.net/issue/mix-17>`__:Could not parse new configuration of channel: PubSubException: Only participants and information nodes are supported!

-  `#mix-18 <https://projects.tigase.net/issue/mix-18>`__:NPE when sending requests to removed channel nodes

-  `#mix-19 <https://projects.tigase.net/issue/mix-19>`__:MAM:2 is not advertised

-  `#mix-20 <https://projects.tigase.net/issue/mix-20>`__:MIX component is broadcasting messages with bare JID

-  `#mix-21 <https://projects.tigase.net/issue/mix-21>`__:Possibility of duplicated subscription of a node

-  `#mix-22 <https://projects.tigase.net/issue/mix-22>`__:Nickname not returned in response after being set

-  `#mix-23 <https://projects.tigase.net/issue/mix-23>`__:Remove banned participants from participants list and subscriptions

-  `#mix-24 <https://projects.tigase.net/issue/mix-24>`__:NPE in MIXProcessor

-  `#mix-25 <https://projects.tigase.net/issue/mix-25>`__:Create MIX component documentation and publish it

-  `#mix-26 <https://projects.tigase.net/issue/mix-26>`__:Allow installation admins to manager MIX channels if domain admins are allowed

-  `#mix-27 <https://projects.tigase.net/issue/mix-27>`__:MIX-MUC message duplication

-  `#mix-28 <https://projects.tigase.net/issue/mix-28>`__:NPE in ``Affiliations.getSubscriberAffiliation``

-  `#mix-29 <https://projects.tigase.net/issue/mix-29>`__:Weird "open channel" behaviour

1.3.3. Tigase PubSub 5.0.0 Release Notes
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Major Changes
~~~~~~~~~~~~~~

-  Add publishing executor with rate limiting

-  Optimisations and fixes


All Changes
~~~~~~~~~~~~

-  `#pubsub-102 <https://projects.tigase.net/issue/pubsub-102>`__: Add publishing executor with rate limiting

-  `#pubsub-103 <https://projects.tigase.net/issue/pubsub-103>`__: Empty message notification id attribute

-  `#pubsub-105 <https://projects.tigase.net/issue/pubsub-105>`__: NPE in RetrieveItemsModule

-  `#pubsub-106 <https://projects.tigase.net/issue/pubsub-106>`__: NPE in PubsubPublishModule?Eventbus

-  `#pubsub-107 <https://projects.tigase.net/issue/pubsub-107>`__: disco#items feature returned on disco#info request for PubSub node item

-  `#pubsub-108 <https://projects.tigase.net/issue/pubsub-108>`__: Fix Missing notification for published events

-  `#pubsub-110 <https://projects.tigase.net/issue/pubsub-110>`__: Fix Deadlock in TigPubSubRemoveService SP on MySQL

-  `#pubsub-111 <https://projects.tigase.net/issue/pubsub-111>`__: Fix SQLException: At least one parameter to the current statement is uninitialized.

-  `#pubsub-113 <https://projects.tigase.net/issue/pubsub-113>`__: Fix StackOverflowError in LRUCacheWithFuture

-  `#pubsub-114 <https://projects.tigase.net/issue/pubsub-114>`__: Fix pubsub#persist_items is not advertised

-  `#pubsub-115 <https://projects.tigase.net/issue/pubsub-115>`__: Fix Cannot add or update a child row: a foreign key constraint fails (``tigasedb``.\ ``tig_pubsub_items``, CONSTRAINT ``tig_pubsub_items_ibfk_1`` FOREIGN KEY (``node_id``) REFERENCES ``tig_pubsub_nodes`` (``node_id``))

-  `#pubsub-119 <https://projects.tigase.net/issue/pubsub-119>`__: Fix NPE in DiscoveryModule

-  `#pubsub-120 <https://projects.tigase.net/issue/pubsub-120>`__: Fix Empty element in POST payload is incorrectly parsed

-  `#pubsub-121 <https://projects.tigase.net/issue/pubsub-121>`__: Use String.intern() for PEP CAPS nodes string

-  `#pubsub-124 <https://projects.tigase.net/issue/pubsub-124>`__: Fix PubSub sends notifications about last published item on each presence received from subscriber.

-  `#pubsub-125 <https://projects.tigase.net/issue/pubsub-125>`__: Reported features ``pubsub#metadata`` should be ``pubsub#meta-data``

-  `#pubsub-126 <https://projects.tigase.net/issue/pubsub-126>`__: Fix Deadlocks in MySQL schema

-  `#pubsub-127 <https://projects.tigase.net/issue/pubsub-127>`__: Fix NPE in UserEntry.remove

-  `#pubsub-128 <https://projects.tigase.net/issue/pubsub-128>`__: Fix PatternSyntaxException for users with emoticons in resource


1.3.4. Tigase MUC 3.2.0 Release Notes
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


Major Changes
~~~~~~~~~~~~~~

-  Bring MUC specification support up to date

-  Improve handling of multiple user session using same nickname

-  Fixes and improvements to ad-hoc scripts


All Changes
~~~~~~~~~~~~

-  `#muc-133 <https://projects.tigase.net/issue/muc-133>`__: Add component option to let only admins create rooms

-  `#muc-134 <https://projects.tigase.net/issue/muc-134>`__: Better MUC Converter log

-  `#muc-136 <https://projects.tigase.net/issue/muc-136>`__: MUC specification supported by Tigase MUC is out of data

-  `#muc-137 <https://projects.tigase.net/issue/muc-137>`__: Add support for <iq/> forwarding with multiple resources joined

-  `#muc-138 <https://projects.tigase.net/issue/muc-138>`__: tigase@muc.tigase.org kicks my clients if I use them both

-  `#muc-139 <https://projects.tigase.net/issue/muc-139>`__: Create script to (mass) delete MUC rooms

-  `#muc-140 <https://projects.tigase.net/issue/muc-140>`__: There is no empty ``<subject/>`` element for persistent room sent after re-joining

-  `#muc-141 <https://projects.tigase.net/issue/muc-141>`__: StringIndexOutOfBoundsException in IqStanzaForwarderModule

-  `#muc-142 <https://projects.tigase.net/issue/muc-142>`__: NullPointerException when processing message with subject

-  `#muc-143 <https://projects.tigase.net/issue/muc-143>`__: Fix MUC scripts: "No such property: mucRepository for class: tigase.admin.Script151"

-  `#muc-144 <https://projects.tigase.net/issue/muc-144>`__: No signature of method: tigase.muc.cluster.RoomClustered.addAffiliationByJid()


1.3.5. Tigase HTTP-API 2.2.0 Release Notes
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Major Changes
~~~~~~~~~~~~~~

-  Enable HTTP File Upload by default with additional, optional, AWS S3 compatible backend

-  Improvements to Web Setup to make installation even more straightforward

-  Allow exposing ``.well-known`` in the root context to facilitate `XEP-0156: Discovering Alternative XMPP Connection Methods <https://xmpp.org/extensions/xep-0156.html>`__

-  Add option to redirect requests from http to https



All Changes
~~~~~~~~~~~~~

-  `#http-65 <https://projects.tigase.net/issue/http-65>`__: More detailed logs

-  `#http-86 <https://projects.tigase.net/issue/http-86>`__: Add s3 backend for http-upload

-  `#http-91 <https://projects.tigase.net/issue/http-91>`__: Items in setup on Features screen are misaligned

-  `#http-93 <https://projects.tigase.net/issue/http-93>`__: Update web-installer documentation

-  `#http-95 <https://projects.tigase.net/issue/http-95>`__: Enable HTTP File Upload by default

-  `#http-96 <https://projects.tigase.net/issue/http-96>`__: Enabling cluster mode / ACS doesn’t add it to resulting configuration file

-  `#http-98 <https://projects.tigase.net/issue/http-98>`__: Setup tests are failing since Septempter

-  `#http-99 <https://projects.tigase.net/issue/http-99>`__: Enforce max-file-size limit

-  `#http-100 <https://projects.tigase.net/issue/http-100>`__: Prevent enabling all Message\* plugins

-  `#http-101 <https://projects.tigase.net/issue/http-101>`__: Prevent enabling all Mobile\* plugins

-  `#http-102 <https://projects.tigase.net/issue/http-102>`__: Last activity plugins handling should be improved

-  `#http-103 <https://projects.tigase.net/issue/http-103>`__: Enabling http-upload should give an info about requirement to set domain/store

-  `#http-105 <https://projects.tigase.net/issue/http-105>`__: Handle forbidden characters in filenames

-  `#http-106 <https://projects.tigase.net/issue/http-106>`__: Can’t remove user for non-existent VHost

-  `#http-107 <https://projects.tigase.net/issue/http-107>`__: Allow exposing ``.well-known`` in the root context

-  `#http-108 <https://projects.tigase.net/issue/http-108>`__: Add option to redirect requests from http to https

-  `#http-109 <https://projects.tigase.net/issue/http-109>`__: openAccess option is missing after migrating the component to TK

-  `#http-110 <https://projects.tigase.net/issue/http-110>`__: Add support for querying and managing uploaded files

-  `#http-111 <https://projects.tigase.net/issue/http-111>`__: DefaultLogic.removeExpired removal of slot failed

-  `#http-113 <https://projects.tigase.net/issue/http-113>`__: Add condition to redirect only if the X-Forwarded-Proto has certain value

-  `#http-114 <https://projects.tigase.net/issue/http-114>`__: TigaseDBException: Could not allocate slot

-  `#http-116 <https://projects.tigase.net/issue/http-116>`__: Limiting list of VHosts doesn’t work for JDK based http-server

-  `#http-117 <https://projects.tigase.net/issue/http-117>`__: Http redirection doesn’t work in docker

-  `#http-119 <https://projects.tigase.net/issue/http-119>`__: Can’t change VHost configuration via Admin WebUI

-  `#http-120 <https://projects.tigase.net/issue/http-120>`__: Improve S3 support for HTTP File Upload to accept custom URL and credentials for S3 storage configuration

-  `#http-121 <https://projects.tigase.net/issue/http-121>`__: Deprecate DnsWebService and rewrite /.well-known/host-meta generator

1.3.6. Tigase Push 1.2.0 Release Notes
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^



Major Changes
~~~~~~~~~~~~~

-  Added support for sending VoIP push notifications using PushKit

-  Support for storing APNS certificates in repository instead of filesystem for easier cluster deployments

-  Add priority detection for push notifications to avoid excessive pushes to devices

-  Inclusion of APNS certificate validity task that notifies if it’s about to expire



All Changes
~~~~~~~~~~~~~

-  `#push-29 <https://projects.tigase.net/issue/push-29>`__ Added support for sending VoIP push notifications using PushKit

-  `#push-30 <https://projects.tigase.net/issue/push-30>`__ Added REST API handler for quick unregistration of a device

-  `#push-32 <https://projects.tigase.net/issue/push-32>`__ Fixed issue with APNS certificate validation

-  `#push-33 <https://projects.tigase.net/issue/push-33>`__ Added statistics gathering

-  `#push-35 <https://projects.tigase.net/issue/push-35>`__ Added support for APNS certificate in PEM

-  `#push-36 <https://projects.tigase.net/issue/push-36>`__ Improved priority detection of push notifications

-  `#push-37 <https://projects.tigase.net/issue/push-37>`__ Enable APNS certificates to be stored in UserRepository - management is done via ad-hoc command;

-  `#push-39 <https://projects.tigase.net/issue/push-39>`__ Changes to improve error handling

-  `#push-41 <https://projects.tigase.net/issue/push-41>`__ Fixed issue with ``ApnsService`` exceptions not being thown logged

-  `#push-42 <https://projects.tigase.net/issue/push-42>`__ Fixed error type reported back on ``tooManyRequestsForDeviceToken``

-  `#push-47 <https://projects.tigase.net/issue/push-47>`__ Added task to periodically validate SSL certificates for Push notifications

-  `#push-48 <https://projects.tigase.net/issue/push-48>`__ Fixed handling events by APNsBinaryApiProvider

-  `#push-49 <https://projects.tigase.net/issue/push-49>`__ Added enforcement to use HTTP/2 protocol (with use of ALPN)



1.3.7. Tigase Message Archiving 3.0.0 Release Notes
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^



Major Changes
~~~~~~~~~~~~~

-  Add support for urn:xmpp:mam:2

-  Add support for `XEP-0308: Last Message Correction <https://xmpp.org/extensions/xep-0308.html>`__



All Changes
~~~~~~~~~~~~~

-  `#mam-47 <https://projects.tigase.net/issue/mam-47>`__: Add support for urn:xmpp:mam:2

-  `#mam-49 <https://projects.tigase.net/issue/mam-49>`__: Historical message duplication

-  `#mam-50 <https://projects.tigase.net/issue/mam-50>`__: XEP-0308: Last Message Correction

-  `#mam-51 <https://projects.tigase.net/issue/mam-51>`__: Fix OMEMO encrypted messages are not stored by MA or MAM

-  `#mam-54 <https://projects.tigase.net/issue/mam-54>`__: Fix NPE in MAM/Message Archiving

-  `#mam-55 <https://projects.tigase.net/issue/mam-55>`__: Fix IllegalArgumentException in MessageArchiveVHostItemExtension

-  `#mam-56 <https://projects.tigase.net/issue/mam-56>`__: Fix upgrade-schema failes

-  `#mam-58 <https://projects.tigase.net/issue/mam-58>`__: Change message archiving rules

-  `#mam-60 <https://projects.tigase.net/issue/mam-60>`__: Fix Message carbons stored in MAM

-  `#mam-61 <https://projects.tigase.net/issue/mam-61>`__: Adjust schema to use new primary keys

-  `#mam-65 <https://projects.tigase.net/issue/mam-65>`__: Fix archiveMessage: Data truncation: Data too long for column ``_body``

-  `#mam-66 <https://projects.tigase.net/issue/mam-66>`__: Fix NPE in AbstractMAMProcessor.updatePrefrerences()

-  `#mam-67 <https://projects.tigase.net/issue/mam-67>`__: Fix Incorrect datetime value in JDBCMessageArchiveRepository

-  `#mam-68 <https://projects.tigase.net/issue/mam-68>`__: Add option to disable local MAM archive

-  `#mam-69 <https://projects.tigase.net/issue/mam-69>`__: Fix Data truncation: Data too long for column '_stanzaId'

-  `#mam-70 <https://projects.tigase.net/issue/mam-70>`__: Fix Schema is inconsistent (tigase.org mysql vs clean postgresql)

-  `#mam-72 <https://projects.tigase.net/issue/mam-72>`__: Fix Deadlock on inserting message


1.3.8. Tigase Advanced Clustering Strategy (ACS) 3.2.0 Release Note
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^



Major Changes
~~~~~~~~~~~~~

-  Deprecate Deprecate PartitionedStrategy in ACS-PubSub



All Changes
~~~~~~~~~~~~~

-  `#acs-8 <https://projects.tigase.net/issue/acs-8>`__: Fix NotAuthorizedException: Session has not been yet authorised. in OnlineUsersCachingStrategy

-  `#acsmix-1 <https://projects.tigase.net/issue/acsmix-1>`__: Implement clustering support for MIX

-  `#acsmix-3 <https://projects.tigase.net/issue/acsmix-3>`__: Fix NPE in DefaultPubSubLogic

-  `#acsmix-4 <https://projects.tigase.net/issue/acsmix-4>`__: Fix NPE in DefaultPubSubLogic.subscribersOfNotifications()

-  `#acsmuc-23 <https://projects.tigase.net/issue/acsmuc-23>`__: Fix NPE in ClusteredRoomStrategyV2

-  `#acsmuc-25 <https://projects.tigase.net/issue/acsmuc-25>`__: Fix NPE in OccupantChangedPresenceCmd

-  `#acspubsub-20 <https://projects.tigase.net/issue/acspubsub-20>`__: Fix NPE in pubsub-nodes-changed-cmd

-  `#acspubsub-21 <https://projects.tigase.net/issue/acspubsub-21>`__: Fix Multiple notifications for single publication

-  `#acspubsub-22 <https://projects.tigase.net/issue/acspubsub-22>`__: Fix Presences informations are kept indefinitely

-  `#acspubsub-24 <https://projects.tigase.net/issue/acspubsub-24>`__: Fix caps-changed-cmd not processed correctly

-  `#acspubsub-25 <https://projects.tigase.net/issue/acspubsub-25>`__: Deprecate PartitionedStrategy

-  `#acspubsub-27 <https://projects.tigase.net/issue/acspubsub-27>`__: Review and improve clustering documentation
