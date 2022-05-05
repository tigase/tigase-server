Database Management
====================

Tigase is coded to perform with multiple database types and numbers. Owing to it’s versatile nature, there are some tools and procedures that may be of use to certain administrators.

Recommended database versions
--------------------------------

As of v8.0.0 here are the minimum and recommended versions of databases for use with Tigase:

+------------+---------------------+-----------------+------------------------------------------------------------------------------------------------------------------------------------+
| Database   | Recommended Version | Minimum Version | Additional Information                                                                                                             |
+============+=====================+=================+====================================================================================================================================+
| DerbyDB    | 10.12.1.1           | 10.12.1.1       | Included with Tigase XMPP Server                                                                                                   |
+------------+---------------------+-----------------+------------------------------------------------------------------------------------------------------------------------------------+
| MySQL      | 5.7                 | 5.6.4           | Required to properly support timestamp storage with millisecond precision                                                          |
+------------+---------------------+-----------------+------------------------------------------------------------------------------------------------------------------------------------+
| SQLServer  | 2014                | 2012            | 2012 needed so we can count use fetch-offset pagination feature.                                                                   |
+------------+---------------------+-----------------+------------------------------------------------------------------------------------------------------------------------------------+
| PostgreSQL | 13.0                | 9.4             | New UA schema requires at least 9.4; if using version older than 13 manual installation of ``uuid-ossp`` extension is required (1) |
+------------+---------------------+-----------------+------------------------------------------------------------------------------------------------------------------------------------+
| MongoDB    | 3.2                 | 3.0             |                                                                                                                                    |
+------------+---------------------+-----------------+------------------------------------------------------------------------------------------------------------------------------------+
| MariaDB    | ?                   | 10.0.12         | Basic features works with 10.0.12-MariaDB Homebrew, but is not fully tested.                                                       |
+------------+---------------------+-----------------+------------------------------------------------------------------------------------------------------------------------------------+

.. Note::

   (1) For PostgreSQL version older than 13.0 manual installation of ``uuid-ossp`` by the superuser to the *created database* is required:

   .. code:: postgresql

      CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

Although Tigase may support other versions of databases, these are the ones we are most familiar with in offering support and advice. Use of databases outside these guidelines may result in unforeseen errors.

Database Watchdog
--------------------

It is possible to have Tigase test availability and existence of database periodically only when db connections are idle. By default this ping is sent once every 60 minutes to each connected repository. However this can be overridden as a part of the dataSource property:

.. code:: properties

   dataSource {
       default () {
           uri = '....'
       }
       'test' () {
           uri =  '...'
           'watchdog-frequency' = 'PT30M'
       }
   }

This setting changes frequency to 30 minutes.

.. code:: properties

   dataSource {
       default () {
           uri = '...'
       }
       'watchdog-frequency' = 'PT15M'
   }

This one changes to 15 minutes.

.. Note::

   see `Period / Duration values <#Period-Duration-values>`__ for format details

Using modified database schema
--------------------------------

If you are using Tigase XMPP Server with modified schema (changed procedures or tables) and you do not want Tigase XMPP Server to maintain it and automatically upgrade, you can disable ``schema-management`` for any data source. If ``schema-management`` is disable for particular data source then Tigase XMPP Server will not update or modify database schema in any way. Moreover it will not check if schema version is correct or not.

**Disabling ``schema-management`` for ``default`` data source**

.. code:: tdsl

   dataSource {
       default () {
           uri = '...'
           'schema-management' = false
       }
   }

.. Warning::

    If ``schema-management`` is disabled, then it is administrator responsibility to maintain database schema and update it if needed (ie. if Tigase XMPP Server schema was changed).

Schema files maintenance
--------------------------------

This document describes schema files layout and assumptions about it. In addition it describes how and when it should be updated.

Assumptions
^^^^^^^^^^^^^^

Following assumptions are in place:

-  All schema files are *loadable* multiple times - this is by far most important assumptions and it’s allow to get away without explicit and detailed checking of loaded version (it’s already handled on the schema level as of version 8.0.0)

-  Required schema version is calculated from the component version (which is set in the project configuration file - usually ``pom.xml``, but it’s possible to override it in code via annotations - please see Developer Guild in Server documentation for details)

-  we will maintain *"3 versions schema files"*, i.e. in the distribution package we will provide schema versions for the ``current_version`` and two major versions behind (and all maintenance version schema files) - this will allow *quick upgrade* even from rather older versions

-  ``SNAPSHOT`` versions will print a log entry indicating that there may have been changes in schema and it’s recommended to run the upgrade (we are aiming at frequent releases thus mandatory schema version check will be done only with final version)

Checks
^^^^^^^^^^^^^^

We will check:

-  if it’s possible to upgrade the schema (based on the current schema version in the database and available SQL files and their respective versions - if );

-  if it’s required to upgrade the schema during server startup (until 7.1.x [inclusive] it was done only for tigase-server, will be done by all components)

-  if it’s required to upgrade the schema during run of ``upgrade-schema``) (if schema is already in the latest required version, executing all SQL files is not required hence speeding up upgrade)

   -  During startup of ``SNAPSHOT`` version, even if the schema version match, a prompt to re-run ``upgrade-schema`` will be printed in the ``logs/tigase-console.log``

Schema files layout
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Filename layout
~~~~~~~~~~~~~~~~~~~~

Basic schema filename layout consists of 3 basic parts:

-  name of relational database management system (RDBMS) for which it’s intended (e.g. ``derby``, ``mysql``, ``postgresql``, ``sqlserver``);

-  name of the Tigase component for which it’s intended;

-  version of the schema file.

For each component and version it’s possible (but not mandatory) to split all database related functionality into multiple files but it’s essential that they would be linked/included in the base file for particular database/component/version file. This allows separating Stored Procedures (``-sp``), base schema (``-schema``) and setting properties (``-props``). In principle the filename pattern looks as follows

::

   <RDBMS_name>-<tigase_component>-schema-<version>[-<sub_schema>].sql

For example schema file for version 7.0.0 of Tigase Server for Derby looks as follow:

::

   derby-server-schema-7.0.0-schema.sql


Files structure
~~~~~~~~~~~~~~~~~~~~

As mentioned before, we should support all versions matching ``old-stable``, ``stable`` and ``master``, which translates to two main versions behind *current-version*, that is version: *current-version - 2*). This results in having 3 versions of the schema in the repository at any given time (two of them being \``upgrades'' to the oldest, base schema):

-  ``current-version`` *minus* 2: base schema

-  ``current-version`` *minus* 1: all changes from ``current-version`` *minus* 2 to ``current-version`` *minus* 1

-  ``current-version``: all changes from ``current-version`` *minus* 1 to ``current-version``

.. Note::

   ``current-version`` *MUST* always match version of the component (defined in pom.xml).

 .. Note::

   It’s possible to have multiple files within version (related to smaller, maintenance upgrade) as the SchemaLoader would collect all files which version falls within range and .

For example with the release of version 8.0.0 this will translate to following versions:

-  ``7.0.0``: base schema

-  ``7.1.0``: all changes from ``7.0.0`` to ``7.1.0``

-  ``8.0.0``: all changes from ``7.1.0`` to ``8.0.0``

.. Note::

   All schema files must be stored under ``src/main/database/``

Handling of changes in the schema
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

There are two main workflows defined

During release of the version
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

As we keep at the most only 3 versions of the schema, after release of the version we need to adjust (flatten) the files to maintain structure defined in *Files structure* (it may happen, that there wouldn’t be any changes in the schema for particular version which will result in relatively empty ``current-version`` schema file – only setting current version for component with ``setVersion('component','<current-version></current-version>');`` ).

For example we are about to release version ``8.0.0``. This results in the following versions of the schema (in the example for the server) in the repository:

-  ``<database>-server-schema-7.0.0.sql``: base schema

-  ``<database>-server-schema-7.1.0.sql``: including changes for ``7.1.0``

-  ``<database>-server-schema-8.0.0.sql``: including changes for ``8.0.0``

.. Note::

   It’s possible that there will be maintenance versions in the list as well, e.g.: ``<database>-server-schema-7.1.1.sql`` and ``<database>-server-schema-7.1.2.sql``

After the release we specify the version of the next release in pom.xml (for example ``8.1.0`` and the same version will be the ``current-version`` making the oldest available version ``7.1.0``. Because of that we *MUST* incorporate all the changes in ``7.1.0`` onto ``7.0.0`` creating new base file with version ``7.1.0``, i.e.:

-  ``<database>-server-schema-7.1.0.sql``: base schema

-  ``<database>-server-schema-8.0.0.sql``: including changes for ``8.0.0``

-  ``<database>-server-schema-8.1.0.sql``: including changes for ``8.1.0``

Maintenance releases
~~~~~~~~~~~~~~~~~~~~~~~~~

Following cases will be discussed with solid-version examples. Comments will be provided in-line Following assumptions are made:

-  Version succession: ``5.1.0``, ``5.2.0``, ``7.0.0``, ``7.1.0``, ``8.0.0``

-  Versions mapping: ``master`` (``8.0.0``), ``stable`` (``7.1.0``), ``old-stable`` (``7.0.0``):

   -  schema files in ``old-stable`` branch

      -  5.1.0 (base)

      -  5.2.0 (upgrade)

      -  7.0.0 (upgrade)

   -  schema files in ``stable`` branch

      -  5.2.0 (base)

      -  7.0.0 (upgrade)

      -  7.1.0 (upgrade)

   -  schema files in ``master`` branch

      -  7.0.0 (base)

      -  7.1.0 (upgrade)

      -  8.0.0 (upgrade)


Making a change in ``old-stable`` (and ``stable``)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If we made a schema change in ``old-stable`` version (and it’s branch) we must:

-  create a new file with upgraded version number;

-  propagate the change to the ``stable`` and ``master`` branch.

Repository changes:

-  schema files in ``old-stable`` branch

   -  5.1.0 (base)

   -  5.2.0 (upgrade)

   -  7.0.0 (upgrade)

   -  7.0.1 (upgrade) **←** making a *change* here results in the schema version being bumped to 7.0.1

-  schema files in ``stable`` branch

   -  5.2.0 (base)

   -  7.0.0 (upgrade)

   -  7.0.1 (upgrade) **←** we must port the *change* here

   -  7.1.0 (upgrade)

-  schema files in ``master`` branch

   -  7.0.0 (base)

   -  7.0.1 (upgrade) **←** we must port the *change* here

   -  7.1.0 (upgrade)

   -  8.0.0 (upgrade)


Making a change in ``master``
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If we made a schema change in ``master`` version we don’t propagate the change to the ``stable`` and ``old-stable`` branch.

-  schema files in ``old-stable`` branch

   -  5.1.0 (base)

   -  5.2.0 (upgrade)

   -  7.0.0 (upgrade)

-  schema files in ``stable`` branch

   -  5.2.0 (base)

   -  7.0.0 (upgrade)

   -  7.1.0 (upgrade)

-  schema files in ``master`` branch

   -  7.0.0 (base)

   -  7.1.0 (upgrade)

   -  8.0.0 (upgrade) **←** we make the *change* here, as this is the development version schema version remains the same.


Implementation details
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

In-file control
~~~~~~~~~~~~~~~~~

There are two main control instructions (intended for ``SchemaLoader``):

-  denoting Queries with ``-- QUERY START:`` and ``-- QUERY END:`` - each must be placed in own, separate file with the query being enclosed by the two of them, for example:

   .. code:: sql

      -- QUERY START:
      call TigPutDBProperty('schema-version', '5.1');
      -- QUERY END:

-  sourcing other file with ``-- LOAD FILE: <path to .sql file>`` - path must be on the same line, following control instruction, for example:

   .. code:: sql

      -- LOAD FILE: database/mysql-server-schema-7.0.0-schema.sql

Storing version in the database
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Each repository will have a table ``tig_schema_versions`` with the information about all installed components and it’s versions in that particular repository. There will be an associated stored procedure to obtain and set version:

-  table:

   .. code:: sql

      tig_schema_versions (
        component varchar(100) NOT NULL,
        version varchar(100) NOT NULL,
        last_update timestamp NOT NULL,
        primary key (component)
      );

-  stored procedures ``get/setVersion(‘component’,'version');``

It will be stored and maintained in the file named ``<RDBMS_name>-common-schema-<version>.sql``

Database Preparation
-----------------------

Tigase uses generally the same database schema and the same set of stored procedures and functions on every database. However, the schema creation scripts and code for stored procedures is different for each database. Therefore the manual process to prepare database is different for each database system.

Starting with v8.0.0, most of the database tasks have been automated and can be called using simple text, or using interactive question and answer style. We **DO NOT RECOMMEND** going through manual operation, however we have kept manual activation of different databases to the Appendix. If you are interested in how we manage and update our database schemas, you may visit the ` Schema files maintenance <#Schema-files-maintenance>`__ section of our Redmine installation for more detailed information.

-  `The DBSchemaLoader Utility <#Schema-Utility>`__

-  `Hashed User Passwords in Database <#Hashed-User-Passwords-in-Database>`__

-  `Support for MongoDB <#Preparing-Tigase-for-MongoDB>`__

Appendix entries

-  `Manual installtion for MySQL <#Prepare-the-MySQL-Database-for-the-Tigase-Server>`__

-  `Manual installtion for Derby <#Prepare-the-Derby-Database-for-the-Tigase-Server>`__

-  `Manual installtion for SQLServer <#Prepare-the-MS-SQL-Server-Database-for-the-Tigase-Server>`__

-  `Manual installtion for PostGRESQL <#Prepare-the-PostgreSQL-Database-for-the-Tigase-Server>`__

Schema Utility
^^^^^^^^^^^^^^^

With the release of v8.0.0 calling the Tigase dbSchemaLoader utility now can be done using tasks instead of calling the specific method. Support for Derby, MySQL, PostgreSQL, MSSQL, and MongoDB is available.

In order to use this utility with any of the databases, you will need to first have the database environment up and running, and have established user credentials. You may use root or an account with administrator write privileges.

Operation & Variables
~~~~~~~~~~~~~~~~~~~~~~~~~

Operation

Operating the schema utility is quite easy! To use it run this command from the installation directory:

.. code:: command

   ./scripts/tigase.sh [task] [params_file.conf] [options]

Operations are now converted to tasks, of which there are now three: ``install-schema``, ``upgrade-schema``, and ``destroy-schema``.

-  ``upgrade-schema``: Upgrade the schema of the database specified in your ``config.tdsl`` configuration file. (options are ignored for this option)

-  ``install-schema``: Install a schema to database.

-  ``destroy-schema``: Destroy database and schemas. **DANGEROUS**

Options

Use the following options to customize. Options in bold are required, *{potential options are in brackets}*:

-  ``--help`` Prints the help for the task.

-  ``-I`` or ``--interactive`` - enables interactive mode which will prompt for parameters not defined.

-  ``-T`` or ``--dbType`` - database type {derby, mongodb, mysql, postgresql, sqlserver}.

-  ``-C`` or ``--components`` - Allows the specification of components for use when installing a schema.

Usage
~~~~~~~

upgrade-schema

This task will locate any schema versions above your current one, and will install them to the database configured in the ``config.tdsl`` file.

.. Note::

   To use this utility, you must have Tigase XMPP server fully setup with a configured configuration file.

.. code:: command

   ./scripts/tigase.sh upgrade-schema etc/tigase.conf

Windows users will need to run the command using the following command:

.. code:: windows

   java -cp "jars/*" tigase.db.util.SchemaManager "upgrade-schema" --config-file=etc/config.tdsl

install-schema

This task will install a schema using the parameters provided.

**If you are setting up a server manually, we HIGHLY recommend using this method**

.. code:: command

   ./scripts/tigase.sh install-schema [Options]

This command will install tigase using a Derby database on one named ``tigasedb`` hosted on ``localhost``. The username and password editing the database is ``tigase_pass`` and ``root``. Note that ``-J`` explicitly adds the administrator, this is highly recommended with the ``-N`` passing the password.

If you are using a windows system, you need to call the program directly:

.. code:: windows

   java -cp "jars/*" tigase.db.util.SchemaManager "install-schema" [options]


Options

Options for schema installation are as follows, required options are in bold

-  ``--help``, Outputs the help.

-  ``-I``, ``--interactive`` - enables interactive mode, which will result in prompting for any missing parameters.

-  ``-C``, ``--components=`` - list of enabled components identifiers (+/-), possible values: [``amp``, ``bosh``, ``c2s``, ``eventbus``, ``ext-disco``, ``http``, ``mdns``, ``message-archive``, ``monitor``, ``muc``, ``pubsub``, ``push``, ``s2s``, ``socks5``, ``test``, ``unified-archive``, ``upload``, ``ws2s``] (default: amp,bosh,c2s,eventbus,http,message-archive,monitor,muc,pubsub,s2s,ws2s). **This is required for certain components like socks5.**

-  ``-T``, ``--dbType=`` - database server type, possible values are: [``derby``, ``mongodb``, ``mysql``, ``postgresql``, ``sqlserver``] (*required*)

-  ``-D``, ``--dbName=`` - name of the database that will be created (by default it is ``tigasedb``). (*required*)

-  ``-H``, ``--dbHostname=`` - address of the database instance (by default it is ``localhost``). (*required*)

-  ``-U``, ``--dbUser=`` - name of the user that will be created specifically to access Tigase XMPP Server database (default is ``tigase_user``). (*required*)

-  ``-P``, ``--dbPass=`` - password of the user that will be created specifically to access Tigase XMPP Server database (default is ``tigase_pass``). (*required*)

-  ``-R``, ``--rootUser=`` - database root account username used to create user and database (default is ``root``). (*required*)

-  ``-A``, ``--rootPass=`` - database root account password used to create user and database (default is ``root``). (*required*)

-  ``-S``, ``--useSSL`` - enable SSL support for database connection (if the database supports it) (default is false).

-  ``-F``, ``--file=`` - comma separated list of SQL files that will be processed.

-  ``-Q``, ``--query=`` - custom queries to be executed, see `Query function <#queryschema>`__ for details.

-  ``-L``, ``--logLevel=`` - logger level used during loading process (default is ``CONFIG``).

-  ``-J``, ``--adminJID=`` - comma separated list of administrator JID(s).

-  ``-N``, ``--adminJIDpass=`` - password that will be used for the entered JID(s) - one password for all configured JIDs.

-  ``--getURI=`` - generate database URI (default is ``false``).

-  ``--ignoreMissingFiles=`` - force ignoring missing files errors (default is ``false``).

Query function

Should you decide to customize your own functions, or have specific information you want to put into the database, you can use the -query function to perform a single query step.

.. code:: cmd

   ./scripts/tigase.sh install-schema -T mysql -D tigasedb -R root -A root -Q "CREATE TABLE tigasedb.EXTRA_TABLE (id INT(6) UNSIGNED AUTO_INCREMENT PRIMARY KEY, name VARCHAR(10) NOT NULL)"

Of course this would break the schema for tigasedb by adding an unexpected table, you will receive the following message:

::

   tigase.db.util.DBSchemaLoader       printInfo          WARNING       Database schema is invalid

But this is a demonstration how you may run a query through the database without the need to use another tool. Note that you will need to select the specific database for each query.

destroy-schema


This will destroy the database specified in the configuration file.

.. Warning::

    **THIS ACTION IS NOT REVERSIBLE**

.. code:: cmd

   ./scripts/tigase.sh destroy-schema etc/config.tdsl

Only use this if you wish to destroy a database and not have the information recoverable.

Windows users will need to call the method directly:

.. code:: cmd

   java -cp "jars/*" tigase.db.util.SchemaManager "destroy-schema" etc/config.tdsl


A note about MySQL

If you are using these commands, you may result in the following error:

.. code:: bash

   tigase.util.DBSchemaLoader       validateDBConnection    WARNING    Table 'performance_schema.session_variables' does not exist

If this occurs, you will need to upgrade your version of MySQL using the following command:

.. code:: bash

   mysql_upgrade -u root -p --force

After entering the password and upgrading MySQL the schema error should no longer show when working with Tigase databases.

Prepare the MySQL Database for the Tigase Server
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This guide describes how to prepare MySQL database for connecting Tigase server.

The MySQL database can be prepared in many ways. Most Linux distributions contain tools which allow you to go through all steps from the shell command line. To make sure it works on all platforms in the same way, we will first show how to do it under MySQL command line client.

Configuring from MySQL command line tool
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Run the MySQL command line client in either Linux or MS Windows environment and enter following instructions from the Tigase installation directory:

.. code:: sql

   mysql -u root -p

Once logged in, create the database for the Tigase server:

.. code:: sql

   mysql> create database tigasedb;

Add the ``tigase_user`` user and grant him access to the ``tigasedb`` database. Depending on how you plan to connect to the database (locally or over the network) use one of following commands or all if you are not sure:

-  Grant access to tigase_user connecting from any network address.

   .. code:: sql

      mysql> GRANT ALL ON tigasedb.* TO tigase_user@'%'
                  IDENTIFIED BY 'tigase_passwd';

-  Grant access to tigase_user connecting from localhost.

   .. code:: sql

      mysql> GRANT ALL ON tigasedb.* TO tigase_user@'localhost'
                  IDENTIFIED BY 'tigase_passwd';

-  Grant access to tigase_user connecting from local machine only.

   .. code:: sql

      mysql> GRANT ALL ON tigasedb.* TO tigase_user
                  IDENTIFIED BY 'tigase_passwd';

And now you can update user permission changes in the database:

.. code:: sql

   mysql> FLUSH PRIVILEGES;

.. Important::

   It’s essential to enable `log_bin_trust_function_creators <https://dev.mysql.com/doc/refman/8.0/en/replication-options-binary-log.html#sysvar_log_bin_trust_function_creators>`__ option in MySQL server, for example by running:

   .. code:: sql

      mysql> SET GLOBAL log_bin_trust_function_creators = 1;

Installing Schemas

Starting with v8.0.0 the Schemas are no longer linked, and will need to manually be installed in the following order.

Switch to the database you have created:

.. code:: sql

   mysql> use tigasedb;

..  Note::

   We are assuming you run the mysql client in Linux from the Tigase installation directory, so all file links will be relative.

Next install the schema files:

.. code:: sql

   mysql> source database/mysql-common-0.0.1.sql;

You will need to repeat this process for the following files:

.. code:: list

   mysql-common-0.0.1.sql
   mysql-common-0.0.2.sql
   mysql-server-7.0.0.sql
   mysql-server-7.1.0.sql
   mysql-server-8.0.0.sql
   mysql-muc-3.0.0.sql
   mysql-pubsub-3.1.0.sql
   mysql-pubsub-3.2.0.sql
   mysql-pubsub-4.0.0.sql
   mysql-http-api-2.0.0.sql

Other components may require installation such as:

.. code:: list

   mysql-socks5-2.0.0.sql
   mysql-push-1.0.0.sql
   mysql-message-archiving-2.0.0.sql
   mysql-unified-archive-2.0.0.sql


Windows instructions:

On Windows you have probably to enter the full path, assuming Tigase is installed in C:\Program Files\Tigase:

.. code:: sql

   mysql> source c:/Program Files/Tigase/database/mysql-common-0.0.1.sql;
   mysql> source c:/Program Files/Tigase/database/mysql-common-0.0.2.sql;
   mysql> source c:/Program Files/Tigase/database/mysql-server-7.0.0.sql;
   and so on...


Configuring From the Linux Shell Command Line
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Follow steps below to prepare the MySQL database:

Create the database space for the Tigase server:

.. code:: sql

   mysqladmin -p create tigasedb

Add the ``tigase_user`` user and grant access to the tigasedb database. Depending on how you plan to connect to the database (locally or over the network) use one of following commands or all if you are not sure:

Selective access configuration

Grant access to tigase_user connecting from any network address.

.. code:: sql

   echo "GRANT ALL ON tigasedb.* TO tigase_user@'%' \
               IDENTIFIED BY 'tigase_passwd'; \
               FLUSH PRIVILEGES;" | mysql -u root -pdbpass mysql


Grant access to tigase_user connecting from localhost.

.. code:: sql

   echo "GRANT ALL ON tigasedb.* TO tigase_user@'localhost' \
               IDENTIFIED BY 'tigase_passwd'; \
               FLUSH PRIVILEGES;" | mysql -u root -pdbpass mysql


Grant access to tigase_user connecting from local machine only.

.. code:: sql

   echo "GRANT ALL ON tigasedb.* TO tigase_user \
               IDENTIFIED BY 'tigase_passwd'; \
               FLUSH PRIVILEGES;" | mysql -u root -pdbpass mysql


Schema Installation

Load the proper mysql schemas into the database.

.. code:: sql

   mysql -u dbuser -p tigasedb < mysql-common-0.0.1.sql
   mysql -u dbuser -p tigasedb < mysql-common-0.0.2.sql
   etc..

You will need to repeat this process for the following files:

.. code:: list

   mysql-common-0.0.1.sql
   mysql-common-0.0.2.sql
   mysql-server-7.0.0.sql
   mysql-server-7.1.0.sql
   mysql-server-8.0.0.sql
   mysql-muc-3.0.0.sql
   mysql-pubsub-3.1.0.sql
   mysql-pubsub-3.2.0.sql
   mysql-pubsub-4.0.0.sql
   mysql-http-api-2.0.0.sql

Other components may require installation such as:

.. code:: list

   mysql-socks5-2.0.0.sql
   mysql-push-1.0.0.sql
   mysql-message-archiving-2.0.0.sql
   mysql-unified-archive-2.0.0.sql


Configuring MySQL for UTF-8 Support
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In my.conf put following lines:

.. code:: bash

   [mysql]
   default-character-SET=utf8

   [client]
   default-character-SET=utf8

   [mysqld]
   init_connect='SET collation_connection = utf8_general_ci; SET NAMES utf8;'
   character-set-server=utf8
   default-character-SET=utf8
   collation-server=utf8_general_ci
   skip-character-set-client-handshake

Then connect to the database from the command line shell check settings:

.. code:: sql

   SHOW VARIABLES LIKE 'character_set_database';
   SHOW VARIABLES LIKE 'character_set_client';

If any of these shows something else then 'utf8' then you need to fix it using the command:

.. code:: sql

   ALTER DATABASE tigasedb DEFAULT CHARACTER SET utf8;

You can now also test your database installation if it accepts UTF-8 data. The easiest way to ensure this is to just to create an account with UTF-8 characters:

.. code:: sql

   call TigAddUserPlainPw('żółw@some.domain.com', 'żółw');

And then check that the account has been created:

.. code:: sql

   SELECT * FROM tig_users WHERE user_id = 'żółw@some.domain.com';

If the last command gives you no results it means there is still something wrong with your settings. You might also want to check your shell settings to make sure your command line shell supports UTF-8 characters and passes them correctly to MySQL:

.. code:: sh

   export LANG=en_US.UTF-8
   export LOCALE=UTF-8
   export LESSCHARSET='utf-8'

It seems that MySQL 5.0.x also needs extra parameters in the connection string: '&useUnicode=true&characterEncoding=UTF-8' while MySQL 5.1.x seems to not need it but it doesn’t hurt to have it for both versions. You have to edit ``etc/config.tdsl`` file and append this to the database connection string.

For MySQL 5.1.x, however, you need to also update code for all database stored procedures and functions used by the Tigase. They are updated for Tigase version 4.4.x and up, however if you use an older version of the Tigase server, you can reload stored procedures using the file from SVN.

Other MySQL Settings Worth Considering
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

There are a number of other useful options, especially for performance improvements. Please note, you will have to review them as some of them may impact data reliability and are useful for performance or load tests installations only.

.. code:: bash

   # InnoDB seems to be a better choice
   # so lets make it a default DB engine
   default-storage-engine = innodb

Some the general MySQL settings which mainly affect performance:

.. code:: bash

   key_buffer = 64M
   max_allowed_packet = 32M
   sort_buffer_size = 64M
   net_buffer_length = 64K
   read_buffer_size = 16M
   read_rnd_buffer_size = 16M
   thread_stack = 192K
   thread_cache_size = 8
   query_cache_limit = 10M
   query_cache_size = 64M

InnoDB specific settings:

.. code:: bash

   # Keep data in a separate file for each table
   innodb_file_per_table = 1
   # Allocate memory for data buffers
   innodb_buffer_pool_size = 1000M
   innodb_additional_mem_pool_size = 100M
   # A location of the MySQL database
   innodb_data_home_dir = /home/databases/mysql/
   innodb_log_group_home_dir = /home/databases/mysql/
   # The main thing here is the 'autoextend' property
   # without it your data file may reach maximum size and
   # no more records can be added to the table.
   innodb_data_file_path = ibdata1:10M:autoextend
   innodb_log_file_size = 10M
   innodb_log_buffer_size = 32M
   # Some other performance affecting settings
   innodb_flush_log_at_trx_commit = 2
   innodb_lock_wait_timeout = 50
   innodb_thread_concurrency = 16

These settings may not be fully optimized for your system, and have been only tested on our systems. If you have found better settings for your systems, feel free to `let us know <http://tigase.net/contact>`__.


Support for emoji and other icons

Tigase Database Schema can support emojis and other icons, however by using UTF-8 in ``mysqld`` settings will not allow this. To employ settings to support emojis and other icons, we recommend you use the following in your MySQL configuration file:

.. code:: properties

   [mysqld]
   character-set-server = utf8mb4
   collation-server = utf8mb4_bin
   character-set-client-handshake = FALSE

Doing this, Tigase XMPP Server Database will still use ``utf8`` character set, with ``utf8_general_ci`` as collation, and only fields which require support for emojis will be converted to ``utf8mb4``.

.. Note::

   If for some reason, with above settings applied to your MySQL instance, you still receive :literal:`java.sql.SQLException: Incorrect string value: ` you should add to your database URI passed in Tigase XMPP Server following configuration `&useUnicode=true&characterEncoding=UTF-8`. If even this fails too, then you may try adding ``&connectionCollation=utf8mb4_bin`` as a last resort. This changes situation from previous versions that shipped older MySQL JDBC connector.

.. Note::

   Tigase XMPP Server databases should be created with ``utf8_general_ci`` collation as it will work properly and is fastest from ``utf8mb4_general_ci`` collations supported by MySQL

Prepare the Derby Database for the Tigase Server
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This guide describes how to prepare Derby database for connecting the Tigase server.

Basic Setup
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Preparation of Derby database is quite simple, but the following assumptions are made

-  ``DerbyDB`` - Derby database name

-  ``database/`` directory contains all necessary schema files

-  ``jars/`` and ``libs/`` directories contains Tigase and Derby binaries

General Approach

From the main Tigase directory execute following commands (Linux and Windows accordingly)

.. Note::

   You must use these sql files on order FIRST!

**Linux**

.. code:: sh

   java -Dij.protocol=jdbc:derby: -Dij.database="DerbyDB;create=true" -cp libs/derby.jar:libs/derbytools.jar:jars/tigase-server.jar org.apache.derby.tools.ij database/derby-common-0.0.1.sql

**Windows**

.. code:: sh

   java -Dij.protocol=jdbc:derby: -Dij.database="DerbyDB;create=true" -cp libs\derby.jar;libs\derbytools.jar;jars\tigase-server.jar org.apache.derby.tools.ij "database\derby-common-0.0.1.sql"

This will create Derby database named DerbyDB in the main Tigase directory and load common version for common v0.1.

You will need to repeat this process again in for following order:

.. code:: list

   derby-common-0.0.1.sql
   derby-common-0.0.2.sql
   derby-server-7.0.0.sql
   derby-server-7.1.0.sql
   derby-server-8.0.0.sql
   derby-muc-3.0.0.sql
   derby-pubsub-3.1.0.sql
   derby-pubsub-3.2.0.sql
   derby-pubsub-4.0.0.sql
   derby-http-api-2.0.0.sql

Other components may require installation such as:

.. code:: list

   derby-socks5-2.0.0.sql
   derby-push-1.0.0.sql
   derby-unified-archive-2.0.0.sql

Connecting Tigase to database
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Once the database is setup, configure the ``config.tdsl`` file in Tigase and add the following configuration:

.. code:: properties

   dataSource {
       default () {
           uri = 'jdbc:derby:{location of derby database};'
       }
   }

Prepare the MS SQL Server Database for the Tigase Server
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This guide describes how to prepare the MS SQL Server database for connecting the Tigase server to it.

It’s expected that a working installation of Microsoft SQL Server is present. The following guide will describe the necessary configurations required for using MS SQL Server with Tigase XMPP Server.

Preparing MS SQL Server Instance
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

After installation of MS SQL Server an instance needs to be configure to handle incoming JDBC connections. For that purpose it’s required to open *SQL Server Configuration Manager*. In the left-hand side panel navigate to *SQL Server Configuration Manager*, then *SQL Server Network Configuration → Protocols for ${INSTANCE_NAME}*. After selecting instance in the right-hand side panel select TCP/IP and open *Properties*, in the Protocol tab in General section select Yes for Enabled property. In the IP Addresses tab select Yes for Active and Enabled properties of all IP Addresses that you want MS SQL Server to handle. Subsequently set the TCP Port property (if missing) to the default value - 1433. A restart of the instance may be required afterwards.

Configuration using MS SQL Server Management Studio
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In order to prepare the database you can use either a wizard or execute queries directly in the Query Editor. Firstly you need to establish a connection to the MS SQL Server instance. From Object Explorer select Connect and in the Connect to Server dialog enter administrator credentials.

Using Wizards

-  Create Login

   In the left-hand side panel select Security → Logins and from context menu choose New Login, in the Wizard window enter desired Login name, select SQL Server authentication and enter desired password subsequently confirming action with OK

-  Create Database

   From the Object Explorer select Databases node and from context menu select New Database; in the Wizard window enter desired Database name and enter previously created Login name into Owner field; subsequently confirming action with OK.


Using Queries

From the Object Explorer root node’s context menu select New Query. In the Query windows execute following statements adjusting details to your liking:

.. code:: sql

   USE [master]
   GO

   CREATE DATABASE [tigasedb];
   GO

   CREATE LOGIN [tigase] WITH PASSWORD=N'tigase12', DEFAULT_DATABASE=[tigasedb]
   GO

   ALTER AUTHORIZATION ON DATABASE::tigasedb TO tigase;
   GO



Import Schema
''''''''''''''

From the File menu Select Open → File (or use Ctrl+O) and then open following files:

.. code:: list

   sqlserver-common-0.0.1.sql
   sqlserver-common-0.0.2.sql
   sqlserver-server-7.0.0.sql
   sqlserver-server-7.1.0.sql
   sqlserver-server-8.0.0.sql
   sqlserver-muc-3.0.0.sql
   sqlserver-pubsub-3.1.0.sql
   sqlserver-pubsub-3.2.0.sql
   sqlserver-pubsub-4.0.0.sql
   sqlserver-http-api-2.0.0.sql

.. Note::

   These files must be done sequentially! They are not linked, and so may need to be done one at a time.

Other components may require installation such as:

.. code:: list

   sqlserver-socks5-2.0.0.sql
   sqlserver-push-1.0.0.sql
   sqlserver-message-archiving-2.0.0.sql
   sqlserver-unified-archive-2.0.0.sql

Subsequently select created database from the list of Available Databases (Ctrl+U) available on the toolbar and execute each of the opened files in the order listed above.

Configuring from command line tool
'''''''''''''''''''''''''''''''''''

Creation of the database and import of schema can be done from command line as well. In order to do that, execute following commands from the directory where Tigase XMPP Server is installed otherwise paths to the schema need to be adjusted accordingly:

.. code:: bash

   sqlcmd -S %servername% -U %root_user% -P %root_pass% -Q "CREATE DATABASE [%database%]"
   sqlcmd -S %servername% -U %root_user% -P %root_pass% -Q "CREATE LOGIN [%user%] WITH PASSWORD=N'%password%', DEFAULT_DATABASE=[%database%]"
   sqlcmd -S %servername% -U %root_user% -P %root_pass% -d %database% -Q "ALTER AUTHORIZATION ON DATABASE::%database% TO %user%;"
   sqlcmd -S %servername% -U %root_user% -P %root_pass% -d %database% -i database\sqlserver-schema-7-1-schema.sql
   sqlcmd -S %servername% -U %root_user% -P %root_pass% -d %database% -i database\sqlserver-schema-7-1-sp.sql
   sqlcmd -S %servername% -U %root_user% -P %root_pass% -d %database% -i database\sqlserver-schema-7-1-props.sql
   sqlcmd -S %servername% -U %root_user% -P %root_pass% -d %database% -i database\sqlserver-pubsub-schema-3.2.0.sql

Above can be automatized with provided script %tigase-server%\scripts\db-create-sqlserver.cmd (note: it needs to be executed from main Tigase XMPP Server directory due to maintain correct paths):

.. code:: sh

   $ scripts\db-create-sqlserver.cmd %database_servername% %database_name% %tigase_username% %tigase_password% %root_username% %root_password%

If no parameters are provided then the following defaults are used:

.. code:: bash

   %database_servername%=localhost
   %database_name%=tigasedb
   %tigase_username%=tigase
   %tigase_password%=tigase12
   %root_username%=root
   %root_password%=root

Tigase configuration - config.tdsl
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Configuration of the MS SQL Server follows general database convention.

.. code:: bash

   dataSource {
       default () {
           uri = 'jdbc:[jtds:]sqlserver://db_hostname:port[;property=val]'
       }
   }

where any number of additional parameters can (and should) consist of:

-  ``databaseName`` - name of the database

-  ``user`` - username configured to access database

-  ``password`` - password for the above username

-  ``schema`` - name of the database schema

-  ``lastUpdateCount`` - 'false' value causes all update counts to be returned, including those returned by server triggers

Example:

.. code:: dsl

   dataSource {
       default () {
           uri = 'jdbc:sqlserver://hostname:1433;databaseName=tigasedb;user=tigase;password=tigase12;schema=dbo;lastUpdateCount=false'
       }
   }

JDBC: jTDS vs MS JDBC driver
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Tigase XMPP Server supports two JDBC drivers intended to be used with Microsoft SQL Server - one created and provided by Microsoft itself and the alternative implementation - jTDS. Tigase is shipped with the latter in the distribution packages. Starting with the version 7.1.0 we recommend using jDTS driver that is shipped with Tigase as JDBC driver created by Microsoft can cause problems with some components in cluster installations. MS driver can be downloaded form the website: `JDBC Drivers 4.0, 4.1 for SQL Server <http://www.microsoft.com/en-us/download/details.aspx?displaylang=en&id=11774>`__ then unpack the archive. Copy sqljdbc_4.0/enu/sqljdbc4.jar file to ${tigase-server}/jars directory.

Depending on the driver used ``uri`` needs to be configured accordingly.

-  Microsoft driver:

   .. code:: dsl

      dataSource {
          default () {
              uri = 'jdbc:sqlserver://...'
          }
      }

-  jDTS driver

   .. code:: bash

      dataSource {
          default () {
              uri = 'jdbc:jdts://...'
          }
      }

Prepare the PostgreSQL Database for the Tigase Server
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This guide describes how to prepare PostgreSQL database for connecting to Tigase server.

The PostgreSQL database can be prepared in many ways. Below are presented two possible ways. The following assumptions apply to both methods:

-  ``admin_db_user`` - database user with admin rights

-  ``tigase_user`` - database user for Tigase

-  ``tigasedb`` - database for Tigase

Configuring from PostgreSQL Command Line Tool
''''''''''''''''''''''''''''''''''''''''''''''

Run the PostgreSQL command line client and enter following instructions:

Add the ``tigase_user``:

.. code:: sql

   psql=# create role tigase_user with login password 'tigase123';

Next, Create the database for the Tigase server with ``tigase_user`` as owner of database:

.. code:: sql

   psql=# create database tigasedb owner tigase_user;

Schema Installation

Load database schema to initialize the Tigase server from the file that corresponds to the version of Tigase you want to use. First you need to switch to ``tigasedb``.

.. code:: sql

   psql=# \connect tigasedb

Begin by applying the basic Schema

.. code:: sql

   psql=# \i database/postgresql-common-0.0.1.sql

Continue by adding the schema files listed below:

.. code:: list

   postgresql-common-0.0.1.sql
   postgresql-common-0.0.2.sql
   postgresql-server-7.0.0.sql
   postgresql-server-7.1.0.sql
   postgresql-server-8.0.0.sql
   postgresql-muc-3.0.0.sql
   postgresql-pubsub-3.1.0.sql
   postgresql-pubsub-3.2.0.sql
   postgresql-pubsub-4.0.0.sql
   postgresql-http-api-2.0.0.sql

Other components may require installation such as:

.. code:: list

   postgresql-socks5-2.0.0.sql
   postgresql-push-1.0.0.sql
   postgresql-message-archiving-2.0.0.sql
   postgresql-unified-archive-2.0.0.sql

Configuring From the Linux Shell Command Line
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Follow steps below to prepare the PostgreSQL database:

First, add the ``tigase_user``:

.. code:: sql

   createuser -U admin_db_user -W -D -R -S -P tigase_user

You will be asked for credentials for admin_db_user and password for new database user.

Create the database for the Tigase server with tigase_user as owner of database:

.. code:: sql

   createdb -U admin_db_user -W -O tigase_user tigasedb

Database Schema Installation

Load database schema to initialize the Tigase server

.. code:: sql

   psql -q -U tigase_user -W tigasedb -f database/postgresql-common-0.0.1.sql
   psql -q -U tigase_user -W tigasedb -f database/postgresql-common-0.0.2.sql
   etc..

Continue by adding the schema files listed below:

.. code:: list

   postgresql-common-0.0.1.sql
   postgresql-common-0.0.2.sql
   postgresql-server-7.0.0.sql
   postgresql-server-7.1.0.sql
   postgresql-server-8.0.0.sql
   postgresql-muc-3.0.0.sql
   postgresql-pubsub-3.1.0.sql
   postgresql-pubsub-3.2.0.sql
   postgresql-pubsub-4.0.0.sql
   postgresql-http-api-2.0.0.sql

Other components may require installation such as:

.. code:: list

   postgresql-socks5-2.0.0.sql
   postgresql-push-1.0.0.sql
   postgresql-message-archiving-2.0.0.sql
   postgresql-unified-archive-2.0.0.sql

.. Note::

   The above commands should be executed from the main Tigase directory. The initialization schema file should be also available locally in database/ directory of your Tigase installation.

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

.. code:: dsl

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

.. code:: dsl

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

.. code:: dsl

   muc {
       'history-db-uri' = 'mongodb://user:pass@localhost/tigase_test'
   }

If MUC components fails to detect and use a proper storage layer for MongoDB, you can force it to use one provided by Tigase by using the following line in the ``config.tdsl`` file:

.. code:: dsl

   muc {
       'history-db' = 'tigase.mongodb.muc.MongoHistoryProvider'
   }


Configuration for PubSub

By default, PubSub component will use MongoDB to store data if Tigase is configured to use it as a default store. However, if you would like to use a different MongoDB database to store PubSub component data, you can do this by adding the following lines to ``etc/config.tdsl`` file:

.. code:: dsl

   pubsub {
       'pubsub-repo-url' = 'mongodb://user:pass@localhost/tigase_test'
   }

If the PubSub components fails to detect and use a proper storage layer for MongoDB, you can force it to use one provided by Tigase by using the following line in the ``config.tdsl`` file:

.. code:: dsl

   pubsub {
       'pubsub-repo-class' = 'tigase.mongodb.pubsub.PubSubDAOMongo'
   }


Configuration for Message Archiving

By default, the Message Archiving component will use MongoDB to store data if Tigase is configured to use it as a default store. However, if you would like to use a different MongoDB database to store message archives, you can do this by adding the following lines to ``etc/config.tdsl`` file:

.. code:: dsl

   'message-archive' {
       'archive-repo-uri' = 'mongodb://user:pass@localhost/tigase_test'
   }

If Message Archiving component fails to detect and use a proper storage layer for MongoDB, you can force it to use one provided by Tigase by using the following line in the ``config.tdsl`` file:

.. code:: dsl

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

Hashed User Passwords in Database
--------------------------------------

.. Warning::

   This feature is still available, but passwords are stored encrypted by default since v8.0.0. We do not recommend using these settings.

By default, user passwords are stored in plain-text in the Tigase’s database. However, there is an easy way to have them encoded in either one of already supported ways or to even add a new encoding algorithm on your own.

Storing passwords in hashed format in the database makes it possible to avoid using a plain-text password authentication mechanism. You cannot have hashed passwords in the database and non-plain-text password authentication. On the other hand, the connection between the server and the client is almost always secured by SSL/TLS so the plain-text password authentication method is perhaps less of a problem than storing plain-text passwords in the database.

Nevertheless, it is simple enough to adjust this in Tigase’s database.

Shortcut
^^^^^^^^^^^^

Connect to your database from a command line and execute following statement for MySQL database:

.. code:: sql

   call TigPutDBProperty('password-encoding', 'encoding-mode');

Where encoding mode is one of the following:

-  ``MD5-PASSWORD`` the database stores MD5 hash code from the user’s password.

-  ``MD5-USERID-PASSWORD`` the database stores MD5 hash code from concatenated user’s bare JID and password.

-  ``MD5-USERNAME-PASSWORD`` the database stores MD5 hash code from concatenated user’s name (localpart) and password.

For example:

.. code:: sql

   call TigPutDBProperty('password-encoding', 'MD5-PASSWORD');

Full Route
^^^^^^^^^^^^

The way passwords are stored in the DB is controlled by Tigase database schema property. Properties in the database schema can be set by a stored procedure called: ``TigPutDBProperty(key, value)``. Properties from the DB schema can be retrieved using another stored function called: ``TigGetDBProperty(key)``.

The simplest way to call them is via command-line interface to the database.

For the purpose of this guide let’s say we have a MySQL database and a test account: ``test@example.com`` with password ``test77``.

By default, most of DB actions for Tigase, are performed using stored procedures including user authentication. So, the first thing to do is to make sure the stored procedures are working correctly.

Create a Test User Account
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To add a new user account we use a stored procedure: ``TigAddUserPlainPw(bareJid, password)``. As you can see there is this strange appendix to the procedure name: ``PlainPw``. This procedure accepts plain passwords regardless how it is stored in the database. So it is safe and easy to use either for plain-text passwords or hashed in the DB. There are also versions of procedures without this appendix but they are sensitive on the data format and always have to pass password in the exact format it is stored in the database.

So, let’s add a new user account:

.. code:: sql

   call TigAddUserPlainPw('test@example.com', 'test77');

If the result was 'Query OK', then it means the user account has been successfully created.

Test User Authentication
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

We can now test user authentication:

.. code:: sql

   call TigUserLoginPlainPw('test@example.com', 'test77');

If authentication was successful the result looks like this:

.. code:: sql

   +--------------------+
   | user_id            |
   +--------------------+
   | 'test@example.com' |
   +--------------------+
   1 row in set (0.01 sec)

   Query OK, 0 rows affected (0.01 sec)

If authentication was unsuccessful, the result looks like this:

.. code:: sql

   +---------+
   | user_id |
   +---------+
   |    NULL |
   +---------+
   1 row in set (0.01 sec)

   Query OK, 0 rows affected (0.01 sec)

Password Encoding Check
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

``TigGetDBProperty`` is a function, not a procedure in MySQL database so we have to use select to call it:

.. code:: sql

   select TigGetDBProperty('password-encoding');

Most likely output is this:

.. code:: sql

   +---------------------------------------+
   | TigGetDBProperty('password-encoding') |
   +---------------------------------------+
   | NULL                                  |
   +---------------------------------------+
   1 row in set, 1 warning (0.00 sec)

Which means a default password encoding is used, in plain-text and thus no encoding. And we can actually check this in the database directly:

.. code:: sql

   select uid, user_id, user_pw from tig_users where user_id = 'test@example.com';

And expected result with plain-text password format would be:

.. code:: sql

   +-----+--------------------+---------+
   | uid | user_id            | user_pw |
   +-----+--------------------+---------+
   |  41 | 'test@example.com' | test77  |
   +-----+--------------------+---------+
   1 row in set (0.00 sec)

Password Encoding Change
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Now let’s set password encoding to MD5 hash:

.. code:: sql

   call TigPutDBProperty('password-encoding', 'MD5-PASSWORD');

'Query OK', means the password encoding has been successfully changed. Of course we changed the property only. All the existing passwords in the database are still in plain-text format. Therefore we expect that attempt to authenticate the user would fail:

.. code:: sql

   call TigUserLoginPlainPw('test@example.com', 'test777');
   +---------+
   | user_id |
   +---------+
   |    NULL |
   +---------+
   1 row in set (0.00 sec)

   Query OK, 0 rows affected (0.00 sec)

We can fix this by updating the user’s password in the database:

.. code:: sql

   call TigUpdatePasswordPlainPw('test@example.com', 'test777');
   Query OK, 1 row affected (0.01 sec)

   mysql> call TigUserLoginPlainPw('test@example.com', 'test777');
   +--------------------+
   | user_id            |
   +--------------------+
   | 'test@example.com' |
   +--------------------+
   1 row in set (0.00 sec)

   Query OK, 0 rows affected (0.00 sec)

Tigase Server and Multiple Databases
-----------------------------------------

Splitting user authentication data from all other XMPP information such as roster, vcards, etc…​ was almost always possible in Tigase XMPP Server. Possible and quite simple thing to configure. Also it has been always possible and easy to assign a different database for each Tigase component (MUC, PubSub, AMP), for recording the server statistics. Almost every data type or component can store information in a different location, simple and easy to setup through the configuration file.

However it is much less known that it is also possible to have a different database for each virtual domain. This applies to both the user repository and authentication repository. This allows for very interesting configuration such as user database sharing where each shard keeps users for a specific domain, or physically split data based on virtual domain if each domain refers to a different customer or group of people.

How can we do that then?

This is very easy to do through the Tigase’s configuration file.

.. code:: dsl

   dataSource {
       default () {
           uri = 'jdbc:mysql://db2.tigase/dbname?user&password'
       }
       'default-auth' () {
           uri = 'jdbc:mysql://db1.tigase/dbname?user&password'
       }
   }
   userRepository {
       default () {}
   }
   authRepository {
       default () {
           cls = 'tigase.db.jdbc.TigaseCustomAuth'
           'data-source' = 'default-auth'
       }
   }

This configuration defines just a default databases for both user repository and authentication repository. Default means it is used when there is no repository specified for a particular virtual domain. However, you can have a separate, both user and authentication repository for each virtual domain.

Here is, how it works:

First, let’s define our default database for all VHosts

.. code:: dsl

   dataSource {
       default () {
           uri = 'jdbc:mysql://db2.tigase/dbname?user&password'
       }
       'default-auth' () {
           uri = 'jdbc:mysql://db1.tigase/dbname?user&password'
       }
   }
   userRepository {
       default () {}
   }
   authRepository {
       default () {
           cls = 'tigase.db.jdbc.TigaseCustomAuth'
           'data-source' = 'default-auth'
       }
   }

Now, we have VHost: domain1.com User authentication data for this VHost is stored in Drupal database

.. code:: dsl

   dataSource {
     'domain1.com-auth' () {
       uri = jdbc:mysql://db7.tigase/dbname?user&password'
     }
   }
   authRepository {
     domain1.com () {
       cls = 'tigase/db/jdbc.TigaseCustomAuth'
       'data-source' = 'domain1.com-auth'
     }
   }

All other user data is stored in Tigase’s standard database in MySQL

.. code:: dsl

   dataSource {
     'domain1.com' () {
       uri = jdbc:mysql://db4.tigase/dbname?user&password'
     }
   }
   userRepository {
     domain1.com () {}
   }

Next VHost: domain2.com User authentication is in LDAP server but all other user data is stored in Tigase’s standard database

.. code:: dsl

   authRepository {
       domain2.com () {
           cls = 'tigase.db.ldap.LdapAuthProvider'
           uri = 'ldap://ldap.domain2.com:389'
           'data-source' = 'default'
       }
   }

Now is something new, we have a custom authentication repository and separate user settings for a single domain. Please note how we define the VHost for which we set custom parameters

.. code:: dsl

   authRepository {
       domain2.com {
           'user-dn-pattern' = 'cn=,ou=,dc=,dc='
       }
   }

All other user data is stored in the same as default repository

.. code:: dsl

   userRepository {
       domain2.com () {}
   }
   dataSource {
       domain2.com () {
           uri = 'jdbc:mysql://db2.tigase/dbname?user&password'
       }
   }

When combined, the DSL output should look like this:

.. code:: dsl

   dataSource {
       domain2.com () {
           uri = 'jdbc:mysql://db2.tigase/dbname?user&password'
       }
   }
   userRepository {
       domain2.com () {}
   }
   authRepository {
       domain2.com () {
           cls = 'tigase.db.ldap.LdapAuthProvider'
           uri = 'ldap://ldap.domain2.com:389'
           'user-dn-pattern' = 'cn=,ou=,dc=,dc='
       }
   }

Next VHost: domain3.com Again user authentication is in LDAP server but pointing to a different LDAP server with different access credentials and parameters. User information is stored in a postgreSQL database.

.. code:: dsl

   dataSource {
       domain3.com () {
           uri = 'jdbc:pgsql://db.domain3.com/dbname?user&password'
       }
   }
   userRepository {
       domain3.com () {}
   }
   authRepository {
       domain3.com () {
           cls = 'tigase.db.ldap.LdapAuthProvider'
           uri = 'ldap://ldap.domain3.com:389'
           'user-dn-pattern' = 'cn=,ou=,dc=,dc='
       }
   }

For VHost: domain4.com all the data, both authentication and user XMPP data are stored on a separate MySQL server with custom stored procedures for both user login and user logout processing.

.. code:: dsl

   dataSource {
       domain4.com () {
           uri = 'jdbc:mysql://db14.domain4.com/dbname?user&password'
       }
   }
   userRepository {
       domain4.com () {}
   }
   authRepository {
       domain4.com () {
           cls = 'tigase.db.jdbc.TigaseCustomAuth'
           'user-login-query' = '{ call UserLogin(?, ?) }'
           'user-logout-query' = '{ call UserLogout(?) }'
           'sasl-mechs' = [ 'PLAIN', 'DIGEST-MD5' ]
       }
   }

As you can see, it requires some writing but flexibility is very extensive and you can setup as many separate databases as you need or want. If one database (recognized by the database connection string) is shared among different VHosts, Tigase still uses a single connection pool, so it won’t create an excessive number of connections to the database.

Importing User Data
--------------------------

You can easily copy data between Tigase compatible repositories that is repositories for which there is a database connector. However, it is not that easy to import data from an external source. Therefore a simple data import functionality has been added to repository utilities package.

You can access repository utilities through command ``./bin/repo.sh`` or ``./scripts/repo.sh`` depending on whether you use a binary package or source distribution.

``-h`` parameter gives you a list of all possible parameters:

.. code:: sh

   ./scripts/repo.sh -h

   Parameters:
    -h          this help message
    -sc class   source repository class name
    -su uri     source repository init string
    -dc class   destination repository class name
    -du uri     destination repository init string
    -dt string  data content to set/remove in repository
    -u user     user ID, if given all operations are only for that ID
                if you want to add user to AuthRepository parameter must
                in form: "user:password"
    -st         perform simple test on repository
    -at         simple test for adding and removing user
    -cp         copy content from source to destination repository
    -pr         print content of the repository
    -n          data content string is a node string
    -kv         data content string is node/key=value string
    -add        add data content to repository
    -del        delete data content from repository
    ------------
    -roster     check the user roster
    -aeg [true|false]  Allow empty group list for the contact
    -import file  import user data from the file of following format:
            user_jid, password, roser_jid, roster_nick, subscription, group



   Note! If you put UserAuthRepository implementation as a class name
         some operation are not allowed and will be silently skipped.
         Have a look at UserAuthRepository to see what operations are
         possible or what operation does make sense.
         Alternatively look for admin tools guide on web site.

The most critical parameters are the source repository class name and the initialization string. Therefore there are a few example preset parameters which you can use and adjust for your system. If you look inside the ``repo.sh`` script you can find at the end of the script following lines:

.. code:: sh

   XML_REP="-sc tigase.db.xml.XMLRepository -su ../testsuite/user-repository.xml_200k_backup"
   MYSQL_REP="-sc tigase.db.jdbc.JDBCRepository -su jdbc:mysql://localhost/tigase?user=root&password=mypass"
   PGSQL_REP="-sc tigase.db.jdbc.JDBCRepository -su jdbc:postgresql://localhost/tigase?user=tigase"

   java $D -cp $CP tigase.util.RepositoryUtils $MYSQL_REP $*

You can see that the source repository has been set to MySQL database with ``tigase`` as the database name, ``root`` the database user and ``mypass`` the user password.

You can adjust these settings for your system.

Now to import data to your repository simply execute the command:

.. code:: sh

   ./bin/repo.sh -import import-file.txt

*Note, the import function is available from* **b895**

The format of the import file is very simple. This is a flat file with comma separated values:

.. code:: bash

   jid,password,roster_jid,roster_nick,subscriptio,group

To create such a file from MySQL database you will have to execute a command like this one:

.. code:: sql

   SELECT a, b, c, d INTO OUTFILE 'import-file.txt'
   FIELDS TERMINATED BY ','
   LINES TERMINATED BY '\n'
   FROM test_table;

Importing Existing Data
--------------------------

Information about importing user data from other databases.

Connecting the Tigase Server to MySQL Database
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Please before continuing reading of this manual have a look at the `initial MySQL database setup <#prepareMysql>`__. It will help you with database preparation for connecting with Tigase server.

This guide describes MySQL database connection parameters.

This guide is actually very short as there are example configuration files which can be used and customized for your environment.

.. code:: dsl

   dataSource {
       default () {
           uri = 'jdbc:mysql://localhost/tigasedb?user=tigase_user&password=mypass'
       }
   }
   userRepository {
       default () {}
   }
   authRepository {
       default () {}
   }

This is the basic setup for setting up an SQL repository for Tigase. dataSource contains the uri for ``default`` which is the mysql database. MySQL connector requires connection string in the following format: ``jdbc:mysql://[hostname]/[database name]?user=[user name]&password=[user password]``

Edit the ``config.tdsl`` file for your environment.

Start the server using following command:

.. code:: sh

   ./scripts/tigase.sh start etc/tigase.conf

Integrating Tigase Server with Drupal
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

First of all, Tigase can authenticate users against a Drupal database which means you have the same user account for both Drupal website and the XMPP server. Moreover in such a configuration all account management is done via Drupal web interface like account creation, password change update user details and so on. Administrator can temporarily disable user account and this is followed by Tigase server too.

Connecting to Drupal Database
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The best way to setup Tigase with Drupal database is via the ``config.tdsl`` file where you can put initial setting for Tigase configuration.

If you look in ``etc/`` directory of your Tigase installation you should find a the file there.

All you need to connect to Drupal database is set the following:

.. code:: dsl

   dataSource {
       'default-auth' () {
           uri = 'jdbc:mysql://localhost/drupal?user=drupalusr&password=drupalpass'
       }
   }
   authRepository {
       default () {
           cls = 'tigase.db.jdbc.DrupalWPAuth'
           'data-source' = 'default-auth'
       }
   }

Typically, you will need to have drupal for authentication, and another for user repository. In this case, we will use SQL for user DB.

.. code:: dsl

   dataSource {
       default () {
           uri = 'jdbc:mysql://localhost/tigasedb?user=tigase_user&password=mypass'
       }
       'default-auth' () {
           uri = 'jdbc:mysql://localhost/drupal?user=drupalusr&password=drupalpass'
       }
   }
   userRepository {
       default () {}
   }
   authRepository {
       default () {
           cls = 'tigase.db.jdbc.DrupalWPAuth'
           'data-source' = 'default-auth'
       }
   }

In theory you can load Tigase database schema to Drupal database and then both ``db-uris`` would have the same database connection string. More details about setting up and connecting to MySQL database can be found in the `MySQL guide <#prepareMysql>`__.

Now run the Tigase server.

.. code:: sh

   ./scripts/tigase.sh start etc/tigase.conf

Now you can register an account on your Drupal website and connect with an XMPP client using the account details.

.. Note::

   You have to enable plain password authentication in your XMPP client to connect to Tigase server with Drupal database.

PostgreSQL Database Use
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This guide describes how to configure Tigase server to use `PostgreSQL <http://www.postgresql.org/>`__ database as a user repository.

If you used an XML based user repository before you can copy all user data to PostgreSQL database using repository management tool. All steps are described below.

PostgreSQL Database Preparation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Create new database user account which will be used to connect to your database:

.. code:: sh

   # createuser
   Enter name of user to add: tigase
   Shall the new user be allowed to create databases? (y/n) y
   Shall the new user be allowed to create more new users? (y/n) y

Now using new database user account create database for your service:

.. code:: sh

   # createdb -U tigase tigasedb
   CREATE DATABASE

Now you can load the database schema:

.. code:: sh

   # psql -U tigase -d tigasedb -f postgresql-schema.sql

Now the database is ready for Tigase server to use.

Server Configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Server configuration is almost identical to MySQL database setup. The only difference is the connection string which usually looks like:

.. code:: dsl

   dataSource {
       default () {
           uri = 'postgresql://localhost/tigasdb?user=tigase'
       }
   }

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

.. code:: tdsl

   'user-login-query' = '{ call TigUserLoginPlainPw(?, ?) }'

to configuration block of **every** authentication repository.

To enable this for default repository, the ``authRepository`` configuration block will look like this:

.. code:: tdsl

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

.. code:: tdsl

   authRepository () {
       default () {
           credentialDecoders () {
               'MD5-PASSWORD' () {}
           }
       }
   }

If you wish to store passwords in MD5 form then use following entries in your configuration file:

**Usage of MD5 encoder.**

.. code:: tdsl

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

.. code:: tdsl

   authRepository () {
       default () {
           credentialDecoders () {
               'PLAIN' () {}
           }
       }
   }

**Disabling SCRAM-SHA-1 encoder.**

.. code:: tdsl

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
