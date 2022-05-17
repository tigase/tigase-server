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