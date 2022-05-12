.. _customAuthentication:

Custom Authentication Connectors
-------------------------------------

This article presents configuration options available to the administrator and describe how to set Tigase server up to use user accounts data from a different database.

The first thing to know is that Tigase server always opens 2 separate connections to the database. One connection is used for user login data and the other is for all other user data like the user roster, vCard, private data storage, privacy lists and so on…​

In this article we still assume that Tigase server keeps user data in it’s own database and only login data is retrieved from the external database.

At the moment Tigase offers following authentication connectors:

-  ``mysql``, ``pgsql``, ``derby`` - standard authentication connector used to load user login data from the main user database used by the Tigase server. In fact the same physical implementation is used for all JDBC databases.

-  ``drupal`` - is the authentication connector used to integrate the Tigase server with `Drupal CMS <http://drupal.org/>`__.

-  ``tigase-custom`` - is the authentication connector which can be used with any database. Unlike the 'tigase-auth' connector it allows you to define SQL queries in the configuration file. The advantage of this implementation is that you don’t have to touch your database. You can use either simple plain SQL queries or stored procedures. The configuration is more difficult as you have to enter carefully all SQL queries in the config file and changing the query usually involves restarting the server. For more details about this implementation and all configuration parameters please refer to :ref:`Tigase Custom Auth documentation<custonAuthConnector>`.

-  ``tigase-auth`` (**DEPRECATED**) - is the authentication connector which can be used with any database. It executes stored procedures to perform all actions. Therefore it is a very convenient way to integrate the server with an external database if you don’t want to expose the database structure. You just have to provide a set of stored procedures in the database. While implementing all stored procedures expected by the server might be a bit of work it allows you to hide the database structure and change the SP implementation at any time. You can add more actions on user login/logout without restarting or touching the server. And the configuration on the server side is very simple. For detailed description of this implementation please refer to :ref:`Tigase Auth documentation<tigaseAuthConnector>`.

As always the simplest way to configure the server is through the ``config.tdsl`` file. In the article describing this file you can find long list with all available options and all details how to handle it. For the authentication connector setup however we only need 2 options:

.. code:: dsl

   dataSource {
       'default-auth' () {
           uri = 'database connection url'
       }
   }
   authRepository {
       default () {
           cls = 'connector'
           'data-source' = 'default-auth'
       }
   }

For example if you store authentication data in a ``drupal`` database on ``localhost`` your settings would be:

.. code:: dsl

   dataSource {
       'default-auth' () {
           uri = 'jdbc:mysql://localhost/drupal?user=user&password=passwd'
       }
   }
   authRepository {
       default () {
           'data-source' = 'default-auth'
       }
   }

You have to use a class name if you want to attach your own authentication connector.

Default is:

.. code:: dsl

   authRepository {
       default {
           cls = 'tigase.db.jdbc.TigaseAuth'
       }
   }

In the same exact way you can setup connector for any different database type.

For example, drupal configuration is below

.. code:: dsl

   authRepository {
       default {
           cls = 'tigase.db.jdbc.DrupalWPAuth'
       }
   }

Or tigase-custom authentication connector.

.. code:: dsl

   authRepository {
       default {
           cls = 'tigase.db.jdbc.TigaseCustomAuth'
       }
   }

The different ``cls`` or classes are:

-  Drupal - ``tigase.db.jdbc.DrupalWPAuth``

-  MySQL, Derby, PostgreSQL, MS SQL Server - ``tigase.db.jdbc.JDBCRepository``

You can normally skip configuring connectors for the default Tigase database format: ``mysql``, ``pgsql`` and ``derby``, ``sqlserver`` as they are applied automatically if the parameter is missing.

One more important thing to know is that you will have to modify ``authRepository`` if you use a custom authentication connector. This is because if you retrieve user login data from the external database this external database is usually managed by an external system. User accounts are added without notifying Tigase server. Then, when the user logs in and tries to retrieve the user roster, the server can not find such a user in the roster database.

.. Important::

   To keep user accounts in sync between the authentication database and the main user database you have to add following option to the end of the database connection URL: ``autoCreateUser=true``.

For example:

.. code:: dsl

   dataSource {
       default () {
           uri = 'jdbc:mysql://localhost/tigasedb?user=nobody&password=pass&autoCreateUser=true'
       }
   }

If you are interested in even further customizing your authentication connector by writing your own queries or stored procedures, please have a look at the following guides:

- :ref:`Tigase Auth guide<tigaseAuthConnector>`

- :ref:`Tigase Custom Auth guide<custonAuthConnector>`

.. include:: Auth_Connectors/Tigase_Auth_Connector.rst
.. include:: Auth_Connectors/Custom_Auth_Connector.rst
.. include:: Auth_Connectors/Drupal_Auth.rst
.. include:: Auth_Connectors/LDAP_Auth.rst
.. include:: Auth_Connectors/SASL_EXTERNAL.rst