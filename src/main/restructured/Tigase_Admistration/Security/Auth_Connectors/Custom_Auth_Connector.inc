.. _custonAuthConnector:

Tigase Custom Auth Connector
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The Tigase Custom Auth connector with shortcut name: **tigase-custom** is implemented in the class: `tigase.db.jdbc.TigaseCustomAuth <https://github.com/tigase/tigase-server/tree/master/src/main/java/tigase/db/jdbc/TigaseCustomAuth.java>`__. It allows you to connect to any external database to perform user authentication and use a custom queries for all actions.

You can find more details how to setup a custom connector in the Custom Authentication Connectors guide.

The basic configuration is very simple:

.. code:: bash

   authRepository {
       default () {
           cls = 'tigase.db.jdbc.TigaseCustomAuth'
           'data-source' = 'default-auth'
       }
   }

That’s it.

The connector loads correctly and starts working using predefined, default list of queries. In most cases you also might want to define your own queries in the configuration file. The shortest possible description is the following example of the content from the ``config.tdsl`` file:

This query is used to check connection to the database, whether it is still alive or not

.. code:: dsl

   authRepository {
       default () {
           'conn-valid-query' = 'select 1'
       }
   }

This is database initialization query, normally we do not use it, especially in clustered environment

.. code:: dsl

   authRepository {
       default () {
           'init-db-query' = 'update tig_users set online_status = 0'
       }
   }

.. Note::

   ``online_status`` column does not exist and would need to be added for that query to work.

Below query performs user authentication on the database level. The Tigase server does not need to know authentication algorithm or password encoding type, it simply passes user id (BareJID) and password in form which was received from the client, to the stored procedure. If the authentication was successful the procedure returns user bare JID or null otherwise. Tigase checks whether the JID returned from the query matches JID passed as a parameter. If they match, the authentication is successful.

.. code:: dsl

   authRepository {
       default () {
           'user-login-query' = '{ call TigUserLoginPlainPw(?, ?) }'
       }
   }


.. Note::

   ``TigUserLoginPlainPw`` is no longer part of a Tigase XMPP Server database schema and would need to be created.

Below query returns number of user accounts in the database, this is mainly used for the server metrics and monitoring components.

.. code:: dsl

   authRepository {
       default () {
         'users-count-query' = '{ call TigAllUsersCount() }'
       }
   }

The Below query is used to add a new user account to the database.

.. code:: dsl

   authRepository {
       default () {
           'add-user-query' = '{call TigAddUserPlainPw(?, ?) }'
       }
   }

Below query is used to remove existing account with all user’s data from the database.

.. code:: dsl

   authRepository {
       default () {
           'del-user-query' = '{ call TigRemoveUser(?) }'
       }
   }

This query is used for the user authentication if ``user-login-query`` is not defined, that is if there is no database level user authentication algorithm available. In such a case the Tigase server loads user’s password from the database and compares it with data received from the client.

.. code:: dsl

   authRepository {
       default () {
           'get-password-query' = 'select user_pw from tig_users where user_id = ?'
       }
   }

Below query is used for user password update in case user decides to change his password.

.. code:: dsl

   authRepository {
       default () {
           'update-password-query' = 'update tig_users set user_pw = ? where user_id = ?'
       }
   }

This query is called on user logout event. Usually we use a stored procedure which records user logout time and marks user as offline in the database.

.. code:: dsl

   authRepository {
       default () {
           'update-logout-query' = 'update tig_users, set online_status = online_status - 1 where user_id = ?'
       }
   }


.. Note::

   ``online_status`` column does not exist and would need to be added for that query to work.

This configuration specifies what non-sasl authentication mechanisms to expose to the client

.. code:: dsl

   authRepository {
       default () {
           'non-sasl-mechs' = [ 'password', 'digest' ]
       }
   }

This setting to specify what sasl authentication mechanisms expose to the client

.. code:: dsl

   authRepository {
       default () {
           'sasl-mechs' = 'PLAIN,DIGEST-MD5'
       }
   }

Queries are defined in the configuration file and they can be either plain SQL queries or stored procedures. If the query starts with characters: ``{ call`` then the server assumes this is a stored procedure call, otherwise it is executed as a plain SQL query. Each configuration value is stripped from white characters on both ends before processing.

Please don’t use semicolon ``;`` at the end of the query as many JDBC drivers get confused and the query may not work.

Some queries can take arguments. Arguments are marked by question marks ``?`` in the query. Refer to the configuration parameters description for more details about what parameters are expected in each query.

This example shows how to use a stored procedure to add a user as a query with 2 required parameters (username, and password).

.. code:: dsl

   authRepository {
       default () {
           'add-user-query' = '{call TigAddUserPlainPw(?, ?) }'
       }
   }

The same query with plain SQL parameters instead:

.. code:: dsl

   'add-user-query' = 'insert into users (user_id, password) values (?, ?)'

The order of the query arguments is important and must be exactly as described in specification for each parameter.

+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------+-------------------------------------------------------------------------------+
| Query Name                | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               | Arguments                                        | Example Query                                                                 |
+===========================+===========================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================+==================================================+===============================================================================+
| ``conn-valid-query``      | Query executed periodically to ensure active connection with the database.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | Takes no arguments.                              | ``select 1``                                                                  |
+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------+-------------------------------------------------------------------------------+
| ``init-db-query``         | Database initialization query which is run after the server is started.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | Takes no arguments.                              | ``update tig_users set online_status = 0``                                    |
+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------+-------------------------------------------------------------------------------+
| ``add-user-query``        | Query adding a new user to the database.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | Takes 2 arguments: ``(user_id (JID), password)`` | ``insert into tig_users (user_id, user_pw) values (?, ?)``                    |
+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------+-------------------------------------------------------------------------------+
| ``del-user-query``        | Removes a user from the database.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | Takes 1 argument: ``(user_id (JID))``            | ``delete from tig_users where user_id = ?``                                   |
+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------+-------------------------------------------------------------------------------+
| ``get-password-query``    | Retrieves user password from the database for given user_id (JID).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        | Takes 1 argument: ``(user_id (JID))``            | ``select user_pw from tig_users where user_id = ?``                           |
+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------+-------------------------------------------------------------------------------+
| ``update-password-query`` | Updates (changes) password for a given user_id (JID).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     | Takes 2 arguments: ``(password, user_id (JID))`` | ``update tig_users set user_pw = ? where user_id = ?``                        |
+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------+-------------------------------------------------------------------------------+
| ``user-login-query``      | Performs user login. Normally used when there is a special SP used for this purpose. This is an alternative way to a method requiring retrieving user password. Therefore at least one of those queries must be defined: ``user-login-query`` or ``get-password-query``. If both queries are defined then ``user-login-query`` is used. Normally this method should be only used with plain text password authentication or sasl-plain. Tigase expects a result set with user_id to be returned from the query if login is successful and empty results set if the login is unsuccessful. | Takes 2 arguments: ``(user_id (JID), password)`` | ``select user_id from tig_users where (user_id = ?) AND (user_pw = ?)``       |
+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------+-------------------------------------------------------------------------------+
| ``user-logout-query``     | This query is called when user logs out or disconnects. It can record that event in the database.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | Takes 1 argument: ``(user_id (JID))``            | ``update tig_users, set online_status = online_status - 1 where user_id = ?`` |
+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------+-------------------------------------------------------------------------------+
| ``non-sasl-mechs``        | Comma separated list of NON-SASL authentication mechanisms. Possible mechanisms are: ``password`` and ``digest``. The digest mechanism can work only with ``get-password-query`` active and only when password are stored in plain text format in the database.                                                                                                                                                                                                                                                                                                                           |                                                  |                                                                               |
+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------+-------------------------------------------------------------------------------+
| ``sasl-mechs``            | Comma separated list of SASL authentication mechanisms. Possible mechanisms are all mechanisms supported by Java implementation. The most common are: ``PLAIN``, ``DIGEST-MD5``, ``CRAM-MD5``. "Non-PLAIN" mechanisms will work only with the ``get-password-query`` active and only when passwords are stored in plain text format in the database.                                                                                                                                                                                                                                      |                                                  |                                                                               |
+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------+-------------------------------------------------------------------------------+