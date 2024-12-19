Importing Existing Data
--------------------------

Information about importing user data from other databases.

Connecting the Tigase Server to MySQL Database
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Please before continuing reading of this manual have a look at the :ref:`initial MySQL database setup<Prepare-the-MySQL-Database-for-the-Tigase-Server>`. It will help you with database preparation for connecting with Tigase server.

This guide describes MySQL database connection parameters.

This guide is actually very short as there are example configuration files which can be used and customized for your environment.

.. code::

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

.. code::

   dataSource {
       default () {
           uri = 'postgresql://localhost/tigasdb?user=tigase'
       }
   }
