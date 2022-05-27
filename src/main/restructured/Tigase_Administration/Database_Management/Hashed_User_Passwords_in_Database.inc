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
