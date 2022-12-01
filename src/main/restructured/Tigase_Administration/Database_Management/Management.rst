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
| MySQL      | 5.7                 | 5.7             | Required to properly support timestamp storage with millisecond precision                                                          |
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

.. code::

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

.. code::

   dataSource {
       default () {
           uri = '...'
       }
       'watchdog-frequency' = 'PT15M'
   }

This one changes to 15 minutes.

.. Note::

   see :ref:`Period / Duration values<PeriodDurationvalues>` for format details

Using modified database schema
--------------------------------

If you are using Tigase XMPP Server with modified schema (changed procedures or tables) and you do not want Tigase XMPP Server to maintain it and automatically upgrade, you can disable ``schema-management`` for any data source. If ``schema-management`` is disable for particular data source then Tigase XMPP Server will not update or modify database schema in any way. Moreover it will not check if schema version is correct or not.

**Disabling ``schema-management`` for ``default`` data source**

.. code::

   dataSource {
       default () {
           uri = '...'
           'schema-management' = false
       }
   }

.. Warning::

    If ``schema-management`` is disabled, then it is administrator responsibility to maintain database schema and update it if needed (ie. if Tigase XMPP Server schema was changed).

.. _Schemafilesmaintenance:

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

.. include:: Database_Preparation.inc
.. include:: Hashed_User_Passwords_in_Database.inc
.. include:: Multiple_Databases.inc
.. include:: Importing_User_Data.inc
.. include:: Existing_Databases.inc
.. include:: Schema_Updates.inc