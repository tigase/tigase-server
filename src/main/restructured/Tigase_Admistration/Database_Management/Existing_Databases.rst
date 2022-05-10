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
