Repository
------------

**Description:** Container specifying authentication repository. This container replaces the old ``auth-db`` property types, and may contain some other configuration values.

**Default value:**

.. code:: dsl

   authRepository {
     <configuration>
   }

This is the basic setup for authRepository, where <configuration> settings are global for all authentication databases. However, you may configure multiple databases individually.

**Example:**

.. code:: dsl

   authRepository {
       'auth-repo-pool-size' = 50
       domain1.com () {
           cls = 'tigase.db.jdbc.JDBCRepository'
           'data-source' = 'domain1'
       }
       domain2.com () {
           cls = 'tigase.db.jdbc.JDBCRepository'
           'data-source' = 'domain2'
           'auth-repo-pool-size' = 30
       }
   }


**Configuration Values:**
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Container has the following options

pool-size
~~~~~~~~~~~~

This property sets the database connections pool size for the associated ``UserRepository``.

   **Note**

   in some cases instead of default for this property setting for ```data-repo-pool-size`` <#dataRepoPoolSize>`__ is used if pool-size is not defined in ``userRepository``. This depends on the repository implementation and the way it is initialized.

.. code:: dsl

   authRepository {
       default ()
         'pool-size' = 10
   }

This is a global property that may be overridden by individual repository settings:

.. code:: dsl

   userRepository {
       default () {
         'pool-size' = 10
       }
       special-repo () {
         'pool-size' = 30
       }
   }


**cls**
~~~~~~~~~~~~

| Defines the class used for repository connection. You can use this to specify specific drivers for different DB types.

Unless specified, the pool class will use the one included with Tigase. You may configure individual repositories in the same way. This replaces the former ``--auth-repo-pool`` property.

.. Note::

   File conversion will not remove and convert this property, it **MUST BE DONE MANUALLY**.

**Available since:** 8.0.0

authRepository
^^^^^^^^^^^^^^^^^

**Description:** Container specifying repository URIs. This container replaces the old ``auth-db-uri`` and ``user-db-uri`` property types.

**Default value:**

.. code:: dsl

   dataSource {
       default () {
           uri = 'jdbc:mysql://localhost/tigasedb?user=tigase&password=tigase12'
   }

Once your configuration is setup, you will see the uri of your user database here. If other databases need to be defined, they will be listed in the same dataSource bean.

**Example:**

.. code:: dsl

   dataSource {
       default () {
           uri = 'jdbc:mysql://localhost/tigasedb?user=tigase&password=tigase12'
       }
       'default-auth' () {
           uri = 'jdbc:mysql://localhost/tigasedbath?user=tigase&password=tigase12'
       }
   }

**Possible values:** Broken down list of customized names for DB URIs. Each name must have a defined uri property. DB name can be customized by the bean name.

.. Note::

   URI name may be used as shorthand to define DB location URI in other containers, so be sure to name them uniquely.

.. Note::

   default () URI setting replaces the ``user-db-uri`` as well as the ``auth-repo-uri`` property.

MSSQL
^^^^^^^^^

MSSql support works out of the box, however Tigase provides an open source driver for the database. We recommend using Microsoft’s own driver for better functionality.

.. code:: dsl

   dataSource () {
       default () {
           uri = 'jdbc:jtds:sqlserver://localhost;databaseName=tigasedb;user=tigase_user;password=tigase_pass;schema=dbo;lastUpdateCount=false;cacheMetaData=false'
       }
   }

Where the uri is divided as follows: jdbc:<driver>:sqlserver://<server address>;databaseName=<database name>;user=<username for db>;password=<password for db>;schema=dbo;lastUpdateCount=false;cacheMetaData=false We do not recommend modification of schema and onward unless you are explicit in your changes.

MongoDb
^^^^^^^^^

For using mongoDB as the repository, the setting will look slightly different:

.. code:: dsl

   dataSource () {
       default () {
           uri = 'mongodb://username:password@localhost/dbname'
       }
   }

MySQL
^^^^^^^^^

MySQL support works out of the box, however Tigase uses prepared calls for calling procedures accessing data stored in database. While this works very fast, it takes time during Tigase XMPP Server startup to prepare those prepared calls. Since version 8.2.0, it is possible to enable workaround which will force Tigase XMPP Server to use prepared statements instead of prepared calls, that will improve startup time but may have slight impact on performance during execution of queries and disables startup verification checking if stored procedures and function in database exist and have correct parameter types. To enable this mode you need to set ``useCallableMysqlWorkaround`` to ``true``.

.. code:: dsl

   dataSource {
       default () {
           uri = 'jdbc:mysql://localhost/tigasedb?user=tigase&password=tigase12'
           useCallableMysqlWorkaround = 'true'
       }
   }


pool-size
^^^^^^^^^^^^^^^^^^

``DataSource`` is an abstraction layer between any higher level data access repositories such as ``userRepository`` or ``authRepository`` and SQL database or JDBC driver to be more specific. Many implementations use ``DataSource`` for DB connections and in fact on many installations they also share the same DataRepository instance if they connect to the same DB. In this case it is desired to use a specific connection pool on this level to an avoid excessive number of connections to the database.

To do so, specify the number of number of database connection as an integer:

.. code:: dsl

   dataSource {
       default () {
           uri = 'jdbc:mysql://localhost/tigasedb?user=tigase&password=tigase12'
           'pool-size' = '50'
       }
   }

By default, the number of connections is 10.

**Available since:** 8.0.0