.. _Prepare-the-MySQL-Database-for-the-Tigase-Server:

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

.. code::

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

.. code::

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

.. code::

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

.. code::

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

.. code::

   [mysqld]
   character-set-server = utf8mb4
   collation-server = utf8mb4_bin
   character-set-client-handshake = FALSE

Doing this, Tigase XMPP Server Database will still use ``utf8`` character set, with ``utf8_general_ci`` as collation, and only fields which require support for emojis will be converted to ``utf8mb4``.

.. Note::

   If for some reason, with above settings applied to your MySQL instance, you still receive :literal:`java.sql.SQLException: Incorrect string value: ` you should add to your database URI passed in Tigase XMPP Server following configuration `&useUnicode=true&characterEncoding=UTF-8`. If even this fails too, then you may try adding ``&connectionCollation=utf8mb4_bin`` as a last resort. This changes situation from previous versions that shipped older MySQL JDBC connector.

.. Note::

   Tigase XMPP Server databases should be created with ``utf8_general_ci`` collation as it will work properly and is fastest from ``utf8mb4_general_ci`` collations supported by MySQL