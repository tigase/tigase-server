.. _PreparingTigaseforMongoDB:

Preparing Tigase for MongoDB
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Tigase now supports MongoDB for auth, settings, and storage repositories. If you wish to use MongoDB for Tigase, please use this guide to help you.

Dependencies
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To run Tigase MongoDB support library requires drivers for MongoDB for Java which can be downloaded from `here <https://github.com/mongodb/mongo-java-driver/releases>`__. This driver needs to be placed in ``jars/`` directory located in Tigase XMPP Server installation directory. If you are using a dist-max distribution, it is already included.

Configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Note that fresh installations of MongoDB do not come with users or databases installed. Once you have setup MongoDB you will need to create a user to be used with Tigase. To do this, bring up the mongo console by running mongo.exe in a cmd window for windows, or run mongo in linux. Once connected, enter then following:

.. code:: bash

   use admin
   db.createUser( { user: "tigase",
                    pwd: "password",
                    customData: { employeeId: 12345 },
                    roles: [ "root" ]
                   }
                 )

Be sure to give this user a ``root`` role in order to properly write to the database. Once you receive a ``user successfully created`` message, you are ready to install tigase on MongoDB.

Configuration of user repository for Tigase XMPP Server

To configure Tigase XMPP Server to use MongoDB you need to set ``dataSource`` in etc/config.tdsl file to proper MongoDB URI pointing to which MongoDB database should be used (it will be created by MongoDB if it does not exist). ``userRepository`` property should not be set to let Tigase XMPP Server auto-detect proper implementation of ``UserRepository``. Tigase XMPP Server will create proper collections in MongoDB if they do not exist so no schema files are necessary.

Example configuration of XMPP Server pointing to MongoDB database ``tigase_test`` in a local instance:

.. code::

   dataSource {
       default () {
           uri = 'mongodb://user:pass@localhost/tigase_test'
       }
   }
   userRepository {
       default () {}
   }
   authRepository {
       default () {}
   }

If Tigase Server is not able to detect a proper storage layer implementation, it can be forced to use one provided by Tigase using the following lines in ``etc/config.tdsl`` file:

.. code::

   userRepository {
       default () {
           cls = 'tigase.mongodb.MongoRepository'
       }
   }
   authRepository {
       default () {
           cls = 'tigase.mongodb.MongoRepository'
       }
   }

Every component should be able to use proper implementation to support MongoDB using this URI. Also MongoDB URI can be passed as any URI in configuration of any component.

Configuration for MUC

By default, MUC component will use MongoDB to store data if Tigase is configured to use it as a default store. However, if you would like to use a different MongoDB database to store MUC message archive, you can do this by adding the following lines to ``etc/config.tdsl`` file:

.. code::

   muc {
       'history-db-uri' = 'mongodb://user:pass@localhost/tigase_test'
   }

If MUC components fails to detect and use a proper storage layer for MongoDB, you can force it to use one provided by Tigase by using the following line in the ``config.tdsl`` file:

.. code::

   muc {
       'history-db' = 'tigase.mongodb.muc.MongoHistoryProvider'
   }


Configuration for PubSub

By default, PubSub component will use MongoDB to store data if Tigase is configured to use it as a default store. However, if you would like to use a different MongoDB database to store PubSub component data, you can do this by adding the following lines to ``etc/config.tdsl`` file:

.. code::

   pubsub {
       'pubsub-repo-url' = 'mongodb://user:pass@localhost/tigase_test'
   }

If the PubSub components fails to detect and use a proper storage layer for MongoDB, you can force it to use one provided by Tigase by using the following line in the ``config.tdsl`` file:

.. code::

   pubsub {
       'pubsub-repo-class' = 'tigase.mongodb.pubsub.PubSubDAOMongo'
   }


Configuration for Message Archiving

By default, the Message Archiving component will use MongoDB to store data if Tigase is configured to use it as a default store. However, if you would like to use a different MongoDB database to store message archives, you can do this by adding the following lines to ``etc/config.tdsl`` file:

.. code::

   'message-archive' {
       'archive-repo-uri' = 'mongodb://user:pass@localhost/tigase_test'
   }

If Message Archiving component fails to detect and use a proper storage layer for MongoDB, you can force it to use one provided by Tigase by using the following line in the ``config.tdsl`` file:

.. code::

   'message-archive' {
       'archive-repo-class' = 'tigase.mongodb.archive.MongoMessageArchiveRepository'
   }


Schema Description
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This description contains only basic description of schema and only basic part of it. More collections may be created if additional components of Tigase XMPP Server are loaded and configured to use MongoDB.

Tigase XMPP Server Schema
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Basic schema for UserRespository and AuthRepository consists of two collections: . tig_users - contains list of users . tig_nodes - contains data related to users in tree-like way

``tig_users`` collection contains the following fields:

.. table:: Table 9. tig_users

   +----------+--------------------------------------------------------------------+
   | Name     | Description                                                        |
   +==========+====================================================================+
   | \_id     | id of user which is SHA256 hash of users jid (raw byte array).     |
   +----------+--------------------------------------------------------------------+
   | user_id  | contains full user jid.                                            |
   +----------+--------------------------------------------------------------------+
   | domain   | domain to which user belongs for easier lookup of users by domain. |
   +----------+--------------------------------------------------------------------+
   | password | password of user (will be removed after upgrade to 8.0.0).         |
   +----------+--------------------------------------------------------------------+

``tig_nodes`` collection contains the following fields

.. table:: Table 10. tig_nodes

   +-------+--------------------------------------------------------------------------+
   | Name  | Description                                                              |
   +=======+==========================================================================+
   | \_id  | id of row auto-generated by MongoDB.                                     |
   +-------+--------------------------------------------------------------------------+
   | uid   | id of user which is SHA256 hash of users jid (raw byte array).           |
   +-------+--------------------------------------------------------------------------+
   | node  | full path of node in tree-like structure separated by / (may not exist). |
   +-------+--------------------------------------------------------------------------+
   | key   | key for which value for node is set.                                     |
   +-------+--------------------------------------------------------------------------+
   | value | value which is set for node key.                                         |
   +-------+--------------------------------------------------------------------------+

Tigase XMPP Server also uses additional collections for storage of Offline Messages

.. table:: Table 11. msg_history collection

   +-----------+-----------------------------------------------------------------------------+
   | Name      | Description                                                                 |
   +===========+=============================================================================+
   | from      | full user jid of message sender.                                            |
   +-----------+-----------------------------------------------------------------------------+
   | from_hash | SHA256 hash of message sender jid as raw byte array.                        |
   +-----------+-----------------------------------------------------------------------------+
   | to        | full users jid of message recipient.                                        |
   +-----------+-----------------------------------------------------------------------------+
   | to_hash   | SHA256 hash of message recipient full jid as raw byte array.                |
   +-----------+-----------------------------------------------------------------------------+
   | ts        | timestamp of message as date.                                               |
   +-----------+-----------------------------------------------------------------------------+
   | message   | serialized XML stanza containing message.                                   |
   +-----------+-----------------------------------------------------------------------------+
   | expire-at | timestamp of expiration of message (if message contains AMP expire-at set). |
   +-----------+-----------------------------------------------------------------------------+

Due to changes in authentication and credentials storage in AuthRepository, we moved ``password`` field from ``tig_users`` collection to a newly created collection called ``tig_user_credentials``.

This new collection has following fields:

+----------------+----------------------------------------------------------------------------------+
| Name           | Description                                                                      |
+================+==================================================================================+
| \_id           | id of document automatically generated by MongoDB                                |
+----------------+----------------------------------------------------------------------------------+
| uid            | SHA256 hash of a user for which credentails are stored                           |
+----------------+----------------------------------------------------------------------------------+
| username       | username provided during authentication (or ``default``)                         |
+----------------+----------------------------------------------------------------------------------+
| account_status | name of an account state (copy of value stored in user document from`tig_users`) |
+----------------+----------------------------------------------------------------------------------+

Additionally for each mechanism we store separate field in this object, so for:

-  ``PLAIN`` we have ``PLAIN`` field with value for this mechanism

-  ``SCRAM-SHA-1`` we have ``SCRAM-SHA-1`` field with value for this mechanism

-  etc…​

Upgrade is not done in one step, and rather will be done once a particular user will log in. During authentication if there is no data in ``tig_user_credentials``, Tigase XMPP Server will check if ``password`` field in ``tig_user`` exists. If it does, and it is filled credentials will be migrated to the new collection.
