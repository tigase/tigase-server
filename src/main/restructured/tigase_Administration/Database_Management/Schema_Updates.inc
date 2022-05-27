Schema Updates
--------------------

This is a repository for Schema updates in case you have to upgrade from older installations.

-  `Tigase Server Schema v7.1 Updates <#tigaseServer71>`__ Applies to v7.1.0 and v8.0.0

Changes to Schema in v8.0.0
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

For version 8.0.0 of Tigase XMPP Server, we decided to improve authentication and security that was provided. In order to do this, implementation of repository and database schemas needed to be changed to achieve this goal. This document, as well one in the HTTP API, will describe the changes to the schemas in this new version.

Reasons
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Before version 8.0.0, user passwords were stored in plaintext in ``user_pw`` database field within ``tig_users`` table, but in plaintext. It was possible to enable storage of the MD5 hash of the password instead, however this limited authentication mechanism SASL PLAIN only. However an MD5 hash of a password is not really a secure method as it is possible to revert this mechanism using rainbow tables.

Therefore, we decided to change this and store only encrypted versions of a password in ``PBKDF2`` form which can be easily used for ``SCRAM-SHA-1`` authentication mechanism or ``SCRAM-SHA-256``. SASL PLAIN mechanism can also used these encrypted passwords. The storage of encrypted passwords is now enabled **by default** in v8.0.0 of Tigase.

Summary of changes
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Added support for storage of encrypted password

Passwords are no longer stored in plaintext on any database.

Using same salt for any subsequent authentications

This allows clients to reuse calculated credentials and keep them instead of storing plaintext passwords.

Disabled usage of stored procedure for authentication

In previous versions, Tigase used stored procedures ``TigUserLoginPlainPw`` and ``TigUserLogin`` for SASL PLAIN authentication. From version 8.0.0, those procedures are no longer used, but they are updated to use passwords stored in ``tig_user_credentials`` table.

It is still possible to use this procedures for authentication, but to do that you need add:

.. code::

   'user-login-query' = '{ call TigUserLoginPlainPw(?, ?) }'

to configuration block of **every** authentication repository.

To enable this for default repository, the ``authRepository`` configuration block will look like this:

.. code::

   authRepository () {
       default () {
           'user-login-query' = '{ call TigUserLoginPlainPw(?, ?) }'
       }
   }


Deprecated API

Some methods of ``AuthRepository`` API were deprecated and should not be used. Most of them were used for authentication using stored procedures, retrieval of password in plaintext or for password change.

For most of these methods, new versions based on ``tig_user_credentials`` table and user credentials storage are provided where possible.

Deprecated storage procedures

Stored procedures for authentication and password manipulation were updated to a new form, so that will be possible to use them by older versions of Tigase XMPP Server during rolling updates of a cluster. However, these procedures will not be used any more and will be depreciated and removed in future versions of Tigase XMPP Server.

Usage of MD5 hashes of passwords

If you have changed ``password-encoding`` database property in previous versions of Tigase XMPP Server, then you will need to modify your configuration to keep it working. If you wish only to allow access using old passwords and to store changed passwords in the new form, then you need to enable credentials decoder for the correct authentication repository. In this example we will provided changes required for ``MD5-PASSWORD`` value of ``password-encoding`` database property. If you have used a different one, then just replace ``MD5-PASSWORD`` with ``MD5-USERNAME-PASSWORD`` or ``MD5-USERID-PASSWORD``.

**Usage of MD5 decoder.**

.. code::

   authRepository () {
       default () {
           credentialDecoders () {
               'MD5-PASSWORD' () {}
           }
       }
   }

If you wish to store passwords in MD5 form then use following entries in your configuration file:

**Usage of MD5 encoder.**

.. code::

   authRepository () {
       default () {
           credentialEncoders () {
               'MD5-PASSWORD' () {}
           }
       }
   }


Enabling and disabling credentials encoders/decoders

You may enable which encoders and decoders used on your installation. By enabling encoders/decoders you are deciding in what form the password is stored in the database. Those changes may impact which SASL mechanisms may be allowed to use on your installation.

**Enabling PLAIN decoder.**

.. code::

   authRepository () {
       default () {
           credentialDecoders () {
               'PLAIN' () {}
           }
       }
   }

**Disabling SCRAM-SHA-1 encoder.**

.. code::

   authRepository () {
       default () {
           credentialEncoders () {
               'SCRAM-SHA-1' (active: false) {}
               'SCRAM-SHA-256' (active: false) {}
           }
       }
   }

.. Warning::

    It is strongly recommended not to disable encoders if you have enabled decoder of the same type as it may lead to the authentication issues, if client tries to use a mechanism which that is not available.

Schema changes

This change resulted in a creation of the new table ``tig_user_credentials`` with following fields:

**uid**
   id of a user row in ``tig_users``.

**username**
   username used for authentication (if ``authzid`` is not provided or ``authzid`` localpart is equal to ``authcid`` then row with ``default`` value will be used).

**mechanism**
   name of mechanism for which this credentials will be used, ie. ``SCRAM-SHA-1`` or ``PLAIN``.

**value**
   serialized value required for mechanism to confirm that credentials match.

.. Warning::

    During execution of ``upgrade-schema`` task, passwords will be removed from ``tig_users`` table from ``user_pw`` field and moved to ``tig_user_credentials`` table.

Added password reset mechanism

As a part of Tigase HTTP API component and Tigase Extras, we developed a mechanism which allows user to reset their password. To use this mechanism HTTP API component and its REST module **must** to be enabled on Tigase XMPP Server installation.

.. Note::

   Additionally this mechanism need to be enabled in the configuration file. For more information about configuration of this mechanism please check Tigase HTTP API component documentation.

Assuming that HTTP API component is configured to run on port 8080 *(default)*, then after accessing address http://localhost:8080/rest/user/resetPassword in the web browser it will present a web form. By filling and submitting this form, the user will initiate a password reset process. During this process, Tigase XMPP Server will send an email to the user’s email address (provided during registration) with a link to the password change form.

Upgrading from v7.1.x
^^^^^^^^^^^^^^^^^^^^^^^^^^

When upgrading from previous versions of Tigase, it is recommended that you first backup the database. Refer to the documentation of your database software to find out how to export a copy. Once the backup is made, it will be time to run the schema upgrade. Be sure that your schema is up to date, and should be v7.1.0 Schema.

To upgrade, use the new ``upgrade-schema`` task of SchemaManager:

-  In linux

   .. code:: bash

      ./scripts/tigase.sh install-schema etc/tigase.conf

-  In Windows

   .. code:: bash

      java -cp "jars/*" tigase.db.util.SchemaManager "install-schema"

You will need to configure the following switches:

-  | ``-T`` Specifies Database Type
   | Possible values are: ``mysql``, ``derby``, ``sqlserver``, ``postgresql``, ``mongodb``

-  | ``-D`` Specifies Databse Name
   | The explicit name of the database you wish to upgrade.

-  | ``-H`` Specifies Host address
   | By default, this is localhost, but may be set to IP address or FQDNS address.

-  | ``-U`` Specifies Username
   | This is the username that is authorized to make changes to the database defined in -D.

-  | ``-P`` Specifies Password
   | The password for username specified in -U.

-  ``-R`` Password for Administrator or Root DB account.

-  ``-A`` Password for Administrator or Root DB account.

-  ``-J`` Jid of user authorized as admin user from Tigase.

-  ``-N`` Password for user specified in -J.

-  | ``-F`` Points to the file that will perform the upgrade.
   | Will follow this form database/{dbname}-server-schema-8.0.0.sql

Tigase Server Schema v7.2 Updates
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

**FOR ALL USERS UPGRADING TO v8.0.0 FROM A v7.0.2 INSTALLATION**
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

| The schema has changed for the main database, and the pubsub repository. In order to upgrade to the new schemas, you will need to do the following:

1. Upgrade the Main database schema to v7.1 using the ``database/${DB_TYPE}-schema-upgrade-to-7-1.sql`` file

2. Upgrade the Pubsub Schema to v3.1.0 using the ``database/${DB_TYPE}-pubsub-schema-3.1.0.sql`` file

3. Upgrade the Pubsub Schema to v3.2.0 using the ``database/${DB_TYPE}-pubsub-schema-3.2.0.sql`` file

4. Upgrade the Pubsub Schema to v3.3.0 using the ``database/${DB_TYPE}-pubsub-schema-3.3.0.sql`` file

All three commands may be done at the same time in that order, it is suggested you make a backup of your current database to prevent data loss.

Tigase Schema Change for v7.1
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Tigase has made changes to its database to include primary keys in the tig_pairs table to improve performance of the Tigase server. This is an auto-incremented column for Primary Key items appended to the previous schema.

.. Warning::

    You MUST update your database to be compliant with the new schema. If you do not, Tigase will not function properly.**

.. Note::

   *This change will affect all users of Tigase using v7.1.0 and newer.*

If you are installing a new version of v8.0.0 on a new database, the schema should automatically be installed.

First, shut down any running instances of Tigase to prevent conflicts with database editing. Then from command line use the DBSchemaLoader class to run the -schema-upgrade-to-7.1.sql file to the database. The command is as follows:

In a linux environment

.. code:: bash

   java -cp "jars/*" tigase.db.util.DBSchemaLoader -dbHostname ${HOSTNAME} -dbType ${DB_TYPE} -rootUser ${ROOT_USER} -dbPass ${DB_USER_PASS} -dbName ${DB_NAME} -schemaVersion ${DB_VERSION} -rootPass ${ROOT_USER_PASS} -dbUser ${DB_USER}  -adminJID "${ADMIN_JID}" -adminJIDpass ${ADMIN_JID_PASS}  -logLevel ALL -file database/${DB_TYPE}-schema-upgrade-to-7-1.sql

In a windows environment

.. code:: bash

   java -cp jars/* tigase.db.util.DBSchemaLoader -dbHostname ${HOSTNAME} -dbType ${DB_TYPE} -rootUser ${ROOT_USER} -dbPass ${DB_USER_PASS} -dbName ${DB_NAME} -schemaVersion ${DB_VERSION} -rootPass ${ROOT_USER_PASS} -dbUser ${DB_USER}  -adminJID "${ADMIN_JID}" -adminJIDpass ${ADMIN_JID_PASS}  -logLevel ALL -file database/${DB_TYPE}-schema-upgrade-to-7-1.sql

All variables will be required, they are as follows:

-  ``${HOSTNAME}`` - Hostname of the database you wish to upgrade.

-  ``${DB_TYPE}`` - Type of database [derby, mysql, postgresql, sqlserver].

-  ``${ROOT_USER}`` - Username of root user.

-  ``${ROOT_USER_PASS}`` - Password of specified root user.

-  ``${DB_USER}`` - Login of user that can edit database.

-  ``${DB_USER_PASS}`` - Password of the specified user.

-  ``${DB_NAME}`` - Name of the database to be edited.

-  ``${DB_VERSION}`` - In this case, we want this to be 7.1.

-  ``${ADMIN_JID}`` - Bare JID of a database user with admin privileges. Must be contained within quotation marks.

-  ``${ADMIN_JID_PASS}`` - Password of associated admin JID.

Please note that the SQL file for the update will have an associated database with the filename. i.e. postgresql-update-to-7.1.sql for postgresql database.

A finalized command will look something like this:

.. code:: bash

   java -cp "jars/*" tigase.db.util.DBSchemaLoader -dbHostname localhost -dbType mysql -rootUser root -rootPass root -dbUser admin -dbPass admin -schemaVersion 7.1 -dbName Tigasedb -adminJID "admin@local.com" -adminJIDPass adminpass -logLevel ALL -file database/mysql-schema-upgrade-to-7.1.sql

Once this has successfully executed, you may restart you server. Watch logs for any db errors that may indicate an incomplete schema upgrade.

Changes to Pubsub Schema
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Tigase has had a change to the PubSub Schema, to upgrade to PubSub Schema v7.1 without having to reform your databases, use this guide to update your databases to be compatible with the new version of Tigase.

.. Note::

   Current PubSub Schema is v3.3.0, you will need to repeat these instructions for v3.1.0, v3.2.0 and then v3.3.0 before you run Tigase V7.1.0.

The PubSub Schema has been streamlined for better resource use, this change affects all users of Tigase. To prepare your database for the new schema, first be sure to create a backup! Then apply the appropriate PubSub schema to your MySQL and it will add the new storage procedure.

All these files should be in your /database folder within Tigase, however if you are missing the appropriate files, use the links below and place them into that folder.

The MySQL schema can be found `Here <https://github.com/tigase/tigase-pubsub/blob/master/src/main/database/mysql-pubsub-4.1.0.sql>`__.

The Derby schema can be found `Here <https://github.com/tigase/tigase-pubsub/blob/master/src/main/database/derby-pubsub-4.1.0.sql>`__.

The PostGRESQL schema can be found `Here <https://github.com/tigase/tigase-pubsub/blob/master/src/main/database/postgresql-pubsub-4.1.0.sql>`__.

The same files are also included in all distributions of v8.0.0 in [tigaseroot]/database/ . All changes to database schema are meant to be backward compatible.

You can use a utility in Tigase to update the schema using the following command from the Tigase root:

-  Linux

   .. code:: bash

      java -cp "jars/*" tigase.db.util.DBSchemaLoader

-  Windows:

   ::

      java -cp jars/* tigase.db.util.DBSchemaLoader

.. Note::

   **Some variation may be necessary depending on how your java build uses ``-cp`` option**

Use the following options to customize. Options in bold are required.:

-  ``-dbType`` database_type {derby, mysql, postgresql, sqlserver} (*required*)

-  ``-schemaVersion`` schema version {4, 5, 5-1}

-  ``-dbName`` database name (*required*)

-  ``-dbHostname`` database hostname (default is localhost)

-  ``-dbUser`` tigase username

-  ``-dbPass`` tigase user password

-  ``-rootUser`` database root username (*required*)

-  ``-rootPass`` database root password (*required*)

-  ``-file path`` to sql schema file (*required*)

-  ``-query`` sql query to execute

-  ``-logLevel`` java logger Level

-  ``-adminJID`` comma separated list of admin JIDs

-  ``-adminJIDpass`` password (one for all entered JIDs

.. Note::

   Arguments take following precedent: query, file, whole schema

As a result your final command should look something like this:

::

   java -cp "jars/*" tigase.db.util.DBSchemaLoader -dbType mysql -dbName tigasedb -dbUser root -dbPass password -file database/mysql-pubsub-schema-3.1.0.sql
