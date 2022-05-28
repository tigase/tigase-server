.. _Prepare-the-Derby-Database-for-the-Tigase-Server:

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

.. code::

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

.. code::

   derby-socks5-2.0.0.sql
   derby-push-1.0.0.sql
   derby-unified-archive-2.0.0.sql

Connecting Tigase to database
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Once the database is setup, configure the ``config.tdsl`` file in Tigase and add the following configuration:

.. code::

   dataSource {
       default () {
           uri = 'jdbc:derby:{location of derby database};'
       }
   }