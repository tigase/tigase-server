8. Tigase Message Archiving Component
======================================

Welcome to Tigase Message Archiving component guide

8.1. Tigase Message Archiving Component
---------------------------------------

Tigase Message Archiving Component originated as implementation of `XEP-0136: Message Archiving <http://xmpp.org/extensions/xep-0136.html:>`__ to add support for archiving of messages exchanged using Tigase XMPP Server. In current version component supports also `XEP-0313: Message Archive Management <http://xmpp.org/extensions/xep-0313.html>`__ specification to allow easier access to archived messages.

8.2. Tigase Message Archiving Release Notes
--------------------------------------------

Welcome to Tigase Message Archiving 3.0.0! This is a feature release for with a number of fixes and updates.

8.2.1. Tigase Message Archiving 3.0.0 Release Notes
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Major Changes
~~~~~~~~~~~~~~

-  Add support for urn:xmpp:mam:2

-  Add support for `XEP-0308: Last Message Correction <https://xmpp.org/extensions/xep-0308.html>`__

All Changes
~~~~~~~~~~~~~~

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

8.2.2. Previous Releases
^^^^^^^^^^^^^^^^^^^^^^^^^

Tigase Message Archiving 2.x release
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Major changes
''''''''''''''

Tigase Message Archiving component has undergone a few major changes to our code and structure. To continue to use Tigase Message Archiving component, a few changes may be needed to be made to your systems. Please see them below:

Database schema changes

We decided to no longer use *in-code* database upgrade to update database schema of Message Archiving component and rather provide separate schema files for every supported database.

Additionally we moved from *in-code* generated SQL statements to stored procedures which are now part of provided database schema files.

To continue usage of new versions of Message Archiving component it is required to manually load new component database schema, see `??? <#Preparation of database>`__ section for informations about that.

.. Warning::

    Loading of new database schema is required to use new version of Message Archiving component.

New features
'''''''''''''

Support for Message Archive Management protocol

Now Tigase Message Archiving component supports searching of archived message using `XEP-0313: Message Archive Management <http://xmpp.org/extensions/xep-0313.html:>`__ protocol.

For details on how to enable this feature look into `??? <#Support for MAM>`__

Support for using separate database for different domains

Since this version it is possible to use separate archived messages based on domains. This allows you to configure component to store archived message for particular domain to different database.

For more informations please look into `??? <#Using separate store for archived messages>`__

8.3. Additional features
-------------------------

Tigase Message Archiving Component contains few additional features useful for working with message archives.

8.3.1. Querying in all messages
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Feature allows user to search all of his archived messages without a need to specify who was send/receiver of this message. To search in all messages, request sent to retrieve archived messages should not contain ``with`` attribute. As a result, when ``with`` attribute is omitted ``<chat/>`` element of response will not contain ``with`` attribute but every ``<to/>`` and ``<from/>`` element will contain ``with`` attribute.

8.3.2. Querying by part of message body
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This feature allows user to query for messages or collections containing messages which in body of a message contained text passed by user.

To execute request in which user wants to find messages with "test failed" string XMPP client needs to add following element

.. code:: xml

   <query xmlns="http://tigase.org/protocol/archive#query">
       <contains>test failed</contains>
   </query>

as child element of @retrieve@ or @list@ element of request.

Example query requests
~~~~~~~~~~~~~~~~~~~~~~~

Example 1
''''''''''

Retrieving messages with "test failed" string with user ``juliet@capulet.com`` between ``2014-01-01 00:00:00`` and ``2014-05-01 00:00:00``

.. code:: xml

   <iq type="get" id="query2">
       <retrieve xmlns='urn:xmpp:archive'
           with='juliet@capulet.com'
           from='2014-01-01T00:00:00Z'
           end='2014-05-01T00:00:00Z'>
             <query xmlns="http://tigase.org/protocol/archive#query">
                 <contains>test failed</contains>
             </query>
       </retrieve>
   </iq>


Example 2
''''''''''

Retrieving collections containing messages with "test failed" string with user ``juliet@capulet.com`` between ``2014-01-01 00:00:00`` and ``2014-05-01 00:00:00``

.. code:: xml

   <iq type="get" id="query2">
       <list xmlns='urn:xmpp:archive'
           with='juliet@capulet.com'
           from='2014-01-01T00:00:00Z'
           end='2014-05-01T00:00:00Z'>
             <query xmlns="http://tigase.org/protocol/archive#query">
                 <contains>test failed</contains>
             </query>
       </list>
   </iq>


8.3.3. Querying by tags
^^^^^^^^^^^^^^^^^^^^^^^^

This feature adds support for tagging messages archived by Message Archiving component and by default is disabled (to learn how to enable this feature please see `??? <#Enabling support for tags>`__ section).

Tagging can be done only by user sending message as to tag message tag needs to be included in message content (message body to be exact).

Currently there are 2 types of tags supported:

-  ``hashtag`` - words prefixed by "hash" (#) are stored with prefix and used as tag, i.e. ``#Tigase``

-  ``mention`` - words prefixed by "at" (@) are stored with prefix and used as tag, i.e. ``@Tigase``

Custom feature allows user to query/retrieve messages or collections from archive only containing particular tag or tags. To execute request in which user wants to retrieve only messages tagged with ``@User1`` and ``#People`` XMPP client executing request needs to add following element as child element of ``<retrieve/>`` element or ``<list/>`` element:

.. code:: xml

   <query xmlns="http://tigase.org/protocol/archive#query">
       <tag>#People</tag>
       <tag>@User1</tag>
   </query>

Querying/retrieving list of messages or collections
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Example 1
''''''''''

Request to retrieve messages tagged with ``@User1`` and ``#People`` from chat with user ``juliet@capulet.com`` between ``2014-01-01 00:00:00`` and ``2014-05-01 00:00:00``

.. code:: xml

   <iq type="get" id="query2">
       <retrieve xmlns='urn:xmpp:archive'
           with='juliet@capulet.com'
           from='2014-01-01T00:00:00Z'
           end='2014-05-01T00:00:00Z'>
             <query xmlns="http://tigase.org/protocol/archive#query">
                 <tag>#People</tag>
                 <tag>@User1</tag>
             </query>
       </retrieve>
   </iq>

Example 2:
'''''''''''

Request to retrieve collections containing messages tagged with ``@User1`` and ``#People`` from chat with user ``juliet@capulet.com`` between ``2014-01-01 00:00:00`` and ``2014-05-01 00:00:00``

.. code:: xml

   <iq type="get" id="query2">
       <list xmlns='urn:xmpp:archive'
           with='juliet@capulet.com'
           from='2014-01-01T00:00:00Z'
           end='2014-05-01T00:00:00Z'>
             <query xmlns="http://tigase.org/protocol/archive#query">
                 <tag>#People</tag>
                 <tag>@User1</tag>
             </query>
       </list>
   </iq>


Retrieving list of tags used by user starting with some text
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To search for hashtags or user names already used following request might be used:

.. code:: xml

   <iq type="set" id="query2">
       <tags xmlns="http://tigase.org/protocol/archive#query" like="#test"/>
   </iq>

which will return suggested similar hashtags which where found in database for particular user if following response:

.. code:: xml

   <iq type="result" id="query2">
       <tags xmlns="http://tigase.org/protocol/archive#query" like="#test">
           <tag>#test1</tag>
           <tag>#test123</tag>
           <set xmlns="http://jabber.org/protocol/rsm">
                <first index='0'>0</first>
                <last>1</last>
                <count>2</count>
           </set>
       </tags>
   </iq>


8.3.4. Automatic archiving of MUC messages
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If this feature is enabled MUC messages are stored in Message Archiving repository and are added in same way as for any other messages and ``jid`` of MUC room is used as ``jid`` of message sender, so if MUC message sent from ``test@muc.example.com`` was stored then to retrieve this messages ``test@muc.example.com`` needs to be passed as ``with`` attribute to message retrieve request. Retrieved MUC messages will be retrieved in same format as normal message with one exception - each message will contain ``name`` attribute with name of occupant in room which sent this message.

This feature is by default disabled but it is possible to enable it for particular user. Additionally it is possible to change default setting on installation level and on hosted domain level to enable this feature, disable feature or allow user to decide if user want this feature to be enabled. For more information about configuration of this feature look at `??? <#Configuration of automatic archiving of MUC messages>`__

.. Note::

   -  It is worth to mention that even if more than on user resource joined same room and each resource will receive same messages then only single message will be stored in Message Archving repository.

   -  It is also important to note that MUC messages are archived to user message archive only when user is joined to MUC room (so if message was sent to room but it was not sent to particular user)

8.4. Database
--------------

8.4.1. Preparation of database
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Before you will be able to use Tigase Message Archiving Component and store messages in particular database you need to initialize this database. We provide few schemas for this component for MySQL, PostgreSQL, SQLServer and DerbyDB.

They are placed in ``database/`` directory of installation package and named in ``dbtype-message-archiving-version.sql``, where ``dbname`` in name of database type which this schema supports and ``version`` is version of a Message Archiving Component for which this schema is designed.

You need to manually select schema for correct database and component and load this schema to database. For more information about loading database schema look into `??? <#Database Preparation>`__ section of `??? <#Tigase XMPP Server Administration Guide>`__

8.4.2. Upgrade of database schema
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Database schema for our components may change between versions and if so it needs to be updated before new version may be started. To upgrade schema please follow instructions from `Preparation of database <#_preparation_of_database>`__ section.

.. Note::

   If you use SNAPSHOT builds then schema may change for same version as this are versions we are still working on.

8.4.3. Schema description
^^^^^^^^^^^^^^^^^^^^^^^^^^

Tigase Message Archiving component uses few tables and stored procedures. To make it easier to find them on database level they are prefixed with ``tig_ma_``.

Table ``tig_ma_jids``
~~~~~~~~~~~~~~~~~~~~~

This table stores all jids related to stored messages, ie. from ``to`` and ``from`` attributes of archived stanzas.

+----------+-----------------------------------+----------------------------------------------------------------------------+
| Field    | Description                       | Comments                                                                   |
+==========+===================================+============================================================================+
| jid_id   | Database ID of a JID              |                                                                            |
+----------+-----------------------------------+----------------------------------------------------------------------------+
| jid      | Value of a bare JID               |                                                                            |
+----------+-----------------------------------+----------------------------------------------------------------------------+
| jid_sha1 | SHA1 value of lowercased bare JID | Used for proper bare JID comparison during lookup.                         |
|          |                                   |                                                                            |
|          |                                   | (N/A to PostgreSQL schema)                                                 |
+----------+-----------------------------------+----------------------------------------------------------------------------+
| domain   | Domain part of a bare JID         | Stored for easier lookup of messages owned by users of a particular domain |
+----------+-----------------------------------+----------------------------------------------------------------------------+

Table ``tig_ma_msgs``
~~~~~~~~~~~~~~~~~~~~~

Table stores archived messages.

+---------------+-----------------------------------------------------------------------+------------------------------------------------+
| Field         | Description                                                           | Comments                                       |
+===============+=======================================================================+================================================+
| stable_id     | Database ID of a message                                              | Unique with matching ``owner_id``              |
+---------------+-----------------------------------------------------------------------+------------------------------------------------+
| owner_id      | ID of a bare JID of a message owner                                   | References ``jid_id`` from ``tig_ma_jids``     |
+---------------+-----------------------------------------------------------------------+------------------------------------------------+
| buddy_id      | ID of a bare JID of a message recipient/sender (different than owner) | References ``jid_id`` from ``tig_ma_jids``     |
+---------------+-----------------------------------------------------------------------+------------------------------------------------+
| ts            | Timestamp of a message                                                | Timestamp of archivization or delayed delivery |
+---------------+-----------------------------------------------------------------------+------------------------------------------------+
| body          | Body of a message                                                     |                                                |
+---------------+-----------------------------------------------------------------------+------------------------------------------------+
| msg           | Serialized message                                                    |                                                |
+---------------+-----------------------------------------------------------------------+------------------------------------------------+
| stanza_id     | ID attribute of archived message                                      |                                                |
+---------------+-----------------------------------------------------------------------+------------------------------------------------+
| is_ref        | Marks if message is a reference to other message                      |                                                |
+---------------+-----------------------------------------------------------------------+------------------------------------------------+
| ref_stable_id | ``stable_id`` of referenced message                                   |                                                |
+---------------+-----------------------------------------------------------------------+------------------------------------------------+


Table ``tig_ma_tags``
~~~~~~~~~~~~~~~~~~~~~~

Table stores tags of archived messages. It stores one tag for many messages using ``tig_ma_msgs_tags`` to store relation between tag and a message.

+----------+---------------------------------+------------------------------------------------------------------------+
| Field    | Description                     | Comments                                                               |
+==========+=================================+========================================================================+
| tag_id   | Database ID of a tag            |                                                                        |
+----------+---------------------------------+------------------------------------------------------------------------+
| owner_id | ID of a bare JID of a tag owner | ID of bare JID of owner for which messages with this tag were archived |
+----------+---------------------------------+------------------------------------------------------------------------+
| tag      | Actual tag value                |                                                                        |
+----------+---------------------------------+------------------------------------------------------------------------+

Table ``tig_ma_msgs_tags``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Table stores relations between tags and archived messages with this tags.

+---------------+---------------------------------+------------------------------------------------------------------------+
| Field         | Description                     | Comments                                                               |
+===============+=================================+========================================================================+
| msg_owner_id  | ID of a bare JID of a tag owner | ID of bare JID of owner for which messages with this tag were archived |
+---------------+---------------------------------+------------------------------------------------------------------------+
| msg_stable_id | Database ID of a message        | Unique with matching ``msg_owner_id``                                  |
+---------------+---------------------------------+------------------------------------------------------------------------+
| tag_id        | Database ID of a tag            | References ``tag_id`` from ``tig_ma_tags``                             |
+---------------+---------------------------------+------------------------------------------------------------------------+

8.5. Configuration
-------------------

To enable Tigase Message Archiving Component you need to add following block to ``etc/config.tdsl`` file:

::

   message-archive () {
   }

It will enable component and configure it under name ``message-archive``. By default it will also use database configured as ``default`` data source to store data.

8.5.1. Custom Database
^^^^^^^^^^^^^^^^^^^^^^^

You can specify a custom database to be used for message archiving. To do this, define the archive-repo-uri property.

.. code:: dsl

   'message-archive' () {
       'archive-repo-uri' = 'jdbc:mysql://localhost/messagearchivedb?user=test&password=test'
   }

Here, ``messagearchivedb`` hosted on localhost is used.

8.5.2. XEP-0136 Support
^^^^^^^^^^^^^^^^^^^^^^^^

To be able to use Message Archiving component with `XEP-0136: Message Archiving <http://xmpp.org/extensions/xep-0136.html:>`__ protocol, you additionally need to enable ``message-archive-xep-0136`` SessionManager processor:

::

   sess-man {
       message-archive-xep-0136 () {
       }
   }

This is required for some advanced options.

8.5.3. Support for MAM
^^^^^^^^^^^^^^^^^^^^^^^^

If you want to use Message Archiving component with `XEP-0313: Message Archive Management <http://xmpp.org/extensions/xep-0313.html:>`__ protocol, then you need to enable ``urn:xmpp:mam:1`` SessionManager processor:

::

   sess-man {
       'urn:xmpp:mam:1' () {
       }
   }


8.5.4. Setting default value of archiving level for message on a server
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Setting this property will change default archiving level for messages for every account on server for which per account default archiving level is not set. User will be able to change this value setting default modes as described in `XEP-0136 section 2.4 <http://xmpp.org/extensions/xep-0136.html#pref-default>`__

Possible values are:

**false**
   Messages are not archived

**body**
   Only message body will be stored. Message without a body will not be stored with this value set

**message**
   While message stanza will be archived (if message should be stored, see `Saving Options <#nonBodyStore>`__)

**stream**
   In this mode every stanza should be archived. *(Not supported)*

To set default level to ``message`` you need to set ``default-store-method`` of ``message-archive`` processor to ``message``:

::

   sess-man {
       message-archive {
           default-store-method = 'message'
       }
   }


8.5.5. Setting required value of archiving level for messages on a server
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Setting this property will change required archiving level for messages for every account on server. User will be able to change this to any lower value by setting default modes as described in `XEP-0136 section 2.4 <http://xmpp.org/extensions/xep-0136.html#pref-default>`__ but user will be allowed to set higher archiving level. If this property is set to higher value then default archiving level is set then this setting will be used as default archiving level setting.

Possible values for this setting are the same as values for default archiving level setting, see `Setting default value of archiving level for message on a server <#_setting_default_value_of_archiving_level_for_message_on_a_server>`__ for list of possible values.

To set required level to ``body`` you need to set ``required-store-method`` of ``message-archive`` processor to ``body``:

::

   sess-man {
       message-archive {
           required-store-method = 'body'
       }
   }


8.5.6. Enabling support for tags
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To enable this feature Message Archiving component needs to be configured properly. You need to add ``tags-support = true`` line to ``message-archiving`` configuration section of ``etc/config.tdsl`` file. Like in following example:

::

   message-archive {
       tags-support = true
   }

where:

-  ``message-archive`` - is name of configuration section under which Message Archiving component is configured to run

Saving Options
~~~~~~~~~~~~~~~

By default, Tigase Message Archive will only store the message body with some metadata, this can exclude messages that are lacking a body. If you decide you wish to save non-body elements within Message Archive, you can now can now configure this by setting ``msg-archive-paths`` to list of elements paths which should trigger saving to Message Archive. To additionally store messages with ``<subject/>`` element:

::

   sess-man {
       message-archive {
           msg-archive-paths = [ '-/message/result[urn:xmpp:mam:1]' '/message/body', '/message/subject' ]
       }
   }

Where above will set the archive to store messages with <body/> or <subject/> elements and for message with ``<result xmlns="urn:xmpp:mam:1"/>`` element not to be stored.

.. Warning::

    It is recommended to keep entry for not storing message with ``<result xmlns="urn:xmpp:mam:1"/>`` element as this are results of MAM query and contain messages already stored in archive!

.. Tip::

   Enabling this for elements such as iq, or presence will quickly load the archive. Configure this setting carefully!

8.5.7. Configuration of automatic archiving of MUC messages
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

As mentioned above no additional configuration options than default configuration of Message Archiving component and plugin is needed to let user decide if he wants to enable or disable this feature (but it is disabled by default). In this case user to enable this feature needs to set settings of message archiving adding ``muc-save`` attribute to ``<default/>`` element of request with value set to ``true`` (or to ``false`` to disable this feature).

To configure state of this feature on installation level, it is required to set ``store-muc-messages`` property of ``message-archive`` SessionManager processor:

::

   sess-man {
       message-archive {
           store-muc-messages = 'value'
       }
   }

where ``value`` may be one of following values:

``user``
   allows value to be set on domain level or by user if domain level setting allows that

``true``
   enables feature for every user in every hosted domain (cannot be overridden by on domain or user level)

``false``
   disables feature for every user in every hosted domain (cannot be overridden by on domain or user level)

To configure state of this feature on domain level, you need to execute vhost configuration command. In list of fields to configure domain, field to set this will be available with following values:

``user``
   allows user to stat of this feature (if allowed on installation level)

``true``
   enables feature for users of configured domain (user will not be able to disable)

``false``
   disables feature for users of configured domain (user will not be able to disable)


8.5.8. Purging Information from Message Archive
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This feature allows for automatic removal of entries older than a configured number of days from the Message Archive. It is designed to clean up database and keep its size within reasonable boundaries. If it is set to 1 day and entry is older than 24 hours then it will be removed, i.e. entry from yesterday from 10:11 will be removed after 10:11 after next execution of purge.

There are 3 settings available for this feature: To enable the feature:

.. code:: dsl

   'message-archive' {
       'remove-expired-messages' = true
   }

This setting changes the initial delay after the server is started to begin removing old entries. In other words, MA purging will not take place until the specified time after the server starts. Default setting is PT1H, or one hour.

.. code:: dsl

       'remove-expired-messages-delay' = 'PT2H'

This setting sets how long MA purging will wait between passes to check for and remove old entries. Default setting is P1D which is once a day.

.. code:: dsl

       'remove-expired-messages-period' = 'PT2D'

You may use all settings at once if you wish.

**NOTE** that these commands are also compatible with ``unified-archive`` component, just replace ``message`` with ``unified``.


Configuration of number of days in VHost
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

VHost holds a setting that determines how long a message needs to be in archive for it to be considered old and removed. This can be set independently per Vhost. This setting can be modified by either using the HTTP admin, or the update item execution in adhoc command.

This configuration is done by execution of Update item configuration adhoc command of vhost-man component, where you should select domain for which messages should be removed and then in field XEP-0136 - retention type select value Number of days and in field XEP-0136 - retention period (in days) enter number of days after which events should be removed from MA.

In adhoc select domain for which messages should be removed and then in field XEP-0136 - retention type select value Number of days and in field XEP-0136 - retention period (in days) enter number of days after which events should be removed from MA.

In HTTP UI select Other, then Update Item Configuration (Vhost-man), select the domain, and from there you can set XEP-0136 retention type, and set number of days at XEP-0136 retention period (in days).

8.5.9. Using separate store for archived messages
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

It is possible to use separate store for archived messages, to do so you need to configure new ``DataSource`` in ``dataSource`` section. Here we will use ``message-archive-store`` as a name of a data source. Additionally you need to pass name of newly configured data source to ``dataSourceName`` property of ``default`` repository of Message Archiving component.

Example:

::

   dataSource {
       message-archive-store () {
           uri = 'jdbc:postgresql://server/message-archive-database'
       }
   }

   message-archive {
       repositoryPool {
           default () {
               dataSourceName = 'message-archive-store'
           }
       }
   }

It is also possible to configure separate store for particular domain, i.e. ``example.com``. Here we will configure data source with name ``example.com`` and use it to store data for archive:

::

   dataSource {
       'example.com' () {
           uri = 'jdbc:postgresql://server/example-database'
       }
   }

   message-archive {
       repositoryPool {
           'example.com' () {
             # we may not set dataSourceName as it matches name of domain
           }
       }
   }

.. Note::

   With this configuration messages for other domains than ``example.com`` will be stored in default data source.

8.5.10. Setting Pool Sizes
^^^^^^^^^^^^^^^^^^^^^^^^^^^

There are a high number of prepared statements which are used to process and archive messages as they go through the server, and you may experience an increase in resource use with the archive turned on. It is recommended to decrease the repository connection pool to help balance server load from this component using the `Pool Size <#dataRepoPoolSize>`__ property:

.. code:: dsl

   'message-archive' (class: tigase.archive.MessageArchiveComponent) {
       'archive-repo-uri' = 'jdbc:mysql://localhost/messagearchivedb?user=test&password=test'
       'pool-size' = 15
   }

8.5.11. Message Tagging Support
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Tigase now is able to support querying message archives based on tags created for the query. Currently, Tigase can support the following tags to help search through message archives: - ``hashtag`` Words prefixed by a hash (#) are stored with a prefix and used as a tag, for example #Tigase - ``mention`` Words prefixed by an at (@) are stored with a prefix and used as a tag, for example @Tigase

**NOTE:** Tags must be written in messages from users, they do not act as wildcards. To search for #Tigase, a message must have #Tigase in the <body> element.

This feature allows users to query and retrieve messages or collections from the archive that only contain one or more tags.

Activating Tagging
~~~~~~~~~~~~~~~~~~~

To enable this feature, the following line must be in the config.tdsl file (or may be added with Admin or Web UI)

.. code:: dsl

   'message-archive' (class: tigase.archive.MessageArchiveComponent) {
       'tags-support' = true
   }

Usage
~~~~~~

To execute a request, the tags must be individual children elements of the ``retrieve`` or ``list`` element like the following request:

.. code:: xml

   <query xmlns="http://tigase.org/protocol/archive#query">
       <tag>#People</tag>
       <tag>@User1</tag>
   </query>

You may also specify specific senders, and limit the time and date that you wish to search through to keep the resulting list smaller. That can be accomplished by adding more fields to the retrieve element such as ``'with'``, ``'from’, and ’end'`` . Take a look at the below example:

.. code:: xml

   <iq type="get" id="query2">
       <retrieve xmlns='urn:xmpp:archive'
           with='juliet@capulet.com'
           from='2014-01-01T00:00:00Z'
           end='2014-05-01T00:00:00Z'>
             <query xmlns="http://tigase.org/protocol/archive#query">
                 <tag>#People</tag>
                 <tag>@User1</tag>
             </query>
       </retrieve>
   </iq>

This stanza is requesting to retrieve messages tagged with @User1 and #people from chats with the user juliet@capulet.com between January 1st, 2014 at 00:00 to May 1st, 2014 at 00:00.

**NOTE:** All times are in Zulu or GMT on a 24h clock.

You can add as many tags as you wish, but each one is an **AND** statement; so the more tags you include, the smaller the results.

8.6. Usage
-----------

Now that we have the archive component running, how do we use it? Currently, the only way to activate and modify the component is through XMPP stanzas. Lets first begin by getting our default settings from the component:

.. code:: xml

   <iq type='get' id='prefq'>
     <pref xmlns='urn:xmpp:archive'/>
   </iq>

It’s a short stanza, but it will tell us what we need to know, Note that you do not need a from or a to for this stanza. The result is as follows:

.. code:: xml

   <iq type='result' id='prefq' to='admin@domain.com/cpu'>
   <pref xmlns='urn:xmpp:archive'>
   <auto save='false'/>
   <default otr='forbid' muc-save="false" save="body"/>
   <method use="prefer" type="auto"/>
   <method use="prefer" type="local"/>
   <method use="prefer" type="manual"/>
   </prefq>
   </iq>

See below for what these settings mean.

8.6.1.  XEP-0136 Field Values
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

<**auto**/>
   -  **Required Attributes**

      -  ``save=`` Boolean turning archiving on or off

   -  **Optional Settings**

      -  ``scope=`` Determines scope of archiving, default is ``'stream'`` which turns off after stream end, or may be ``'global'`` which keeps auto save permanent,

<**default**/>
   Default element sets default settings for OTR and save modes, includes an option for archive expiration.

   -  **Required Attribures**

      -  ``otr=`` Specifies setting for Off The Record mode. Available settings are:

         -  ``approve`` The user MUST explicitly approve OTR communication.

         -  ``concede`` Communications MAY be OTR if requested by another user.

         -  ``forbid`` Communications MUST NOT be OTR.

         -  ``oppose`` Communications SHOULD NOT be OTR.

         -  ``prefer`` Communications SHOULD be OTR.

         -  ``require`` Communications MUST be OTR.

      -  ``save=`` Specifies the portion of messages to archive, by default it is set to ``body``.

         -  ``body`` Archives only the items within the <body/> elements.

         -  ``message`` Archive the entire XML content of each message.

         -  ``stream`` Archive saves every byte of communication between server and client. (Not recommended, high resource use)

   -  **Optional Settings**

      -  ``expire=`` Specifies after how many seconds should the server delete saved messages.

<**item**/>
   The Item element specifies settings for a particular entity. These settings will override default settings for the specified JIDS.

   -  **Required Attributes**

      -  ``JID=`` The Jabber ID of the entity that you wish to put these settings on, it may be a full JID, bare JID, or just a domain.

      -  ``otr=`` Specifies setting for Off The Record mode. Available settings are:

         -  ``approve`` The user MUST explicitly approve OTR communication.

         -  ``concede`` Communications MAY be OTR if requested by another user.

         -  ``forbid`` Communications MUST NOT be OTR.

         -  ``oppose`` Communications SHOULD NOT be OTR.

         -  ``prefer`` Communications SHOULD be OTR.

         -  ``require`` Communications MUST be OTR.

      -  ``save=`` Specifies the portion of messages to archive, by default it is set to ``body``.

         -  ``body`` Archives only the items within the <body/> elements.

         -  ``message`` Archive the entire XML content of each message.

         -  ``stream`` Archive saves every byte of communication between server and client. (Not recommended, high resource use)

   -  **Optional Settings**

      -  ``expire=`` Specifies after how many seconds should the server delete saved messages.

<**method**/>
   This element specifies the user preference for available archiving methods.

   -  **Required Attributes**

      -  ``type=`` The type of archiving to set

         -  ``auto`` Preferences for use of automatic archiving on the user’s server.

         -  ``local`` Set to use local archiving on user’s machine or device.

         -  ``manual`` Preferences for use of manual archiving to the server.

      -  ``use=`` Sets level of use for the type

         -  ``prefer`` The selected method should be used if it is available.

         -  ``concede`` This will be used if no other methods are available.

         -  ``forbid`` The associated method MUST not be used.

Now that we have established settings, lets send a stanza changing a few of them:

.. code:: xml

   <iq type='set' id='pref2'>
     <pref xmlns='urn:xmpp:archive'>
       <auto save='true' scope='global'/>
       <item jid='domain.com' otr='forbid' save='body'/>
       <method type='auto' use='prefer'/>
       <method type='local' use='forbid'/>
       <method type='manual' use='concede'/>
     </pref>
   </iq>

This now sets archiving by default for all users on the domain.com server, forbids OTR, and prefers auto save method for archiving.

8.6.2. Manual Activation
^^^^^^^^^^^^^^^^^^^^^^^^^

Turning on archiving requires a simple stanza which will turn on archiving for the use sending the stanza and using default settings.

.. code:: xml

   <iq type='set' id='turnon'>
     <pref xmlns='urn:xmpp:archive'>
       <auto save='true'/>
     </pref>
   </iq>

A sucessful result will yield this response from the server:

.. code:: xml

   <iq type='result' to='user@domain.com' id='turnon'/>

Once this is turned on, incoming and outgoing messages from the user will be stored in ``tig_ma_msgs`` table in the database.

8.7. Limitations
-----------------

-  Component groups messages in collections using date of a messages instead of id of message thread, due to fact that some clients are sending messages with no thread id (ie. Psi, Psi+).

-  Only bare JID is stored of sender or recipient.

