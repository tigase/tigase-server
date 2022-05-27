Tigase PubSub 5.0.0 Release Notes
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

