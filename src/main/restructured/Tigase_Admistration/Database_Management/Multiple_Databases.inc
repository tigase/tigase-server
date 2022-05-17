Tigase Server and Multiple Databases
-----------------------------------------

Splitting user authentication data from all other XMPP information such as roster, vcards, etc…​ was almost always possible in Tigase XMPP Server. Possible and quite simple thing to configure. Also it has been always possible and easy to assign a different database for each Tigase component (MUC, PubSub, AMP), for recording the server statistics. Almost every data type or component can store information in a different location, simple and easy to setup through the configuration file.

However it is much less known that it is also possible to have a different database for each virtual domain. This applies to both the user repository and authentication repository. This allows for very interesting configuration such as user database sharing where each shard keeps users for a specific domain, or physically split data based on virtual domain if each domain refers to a different customer or group of people.

How can we do that then?

This is very easy to do through the Tigase’s configuration file.

.. code:: dsl

   dataSource {
       default () {
           uri = 'jdbc:mysql://db2.tigase/dbname?user&password'
       }
       'default-auth' () {
           uri = 'jdbc:mysql://db1.tigase/dbname?user&password'
       }
   }
   userRepository {
       default () {}
   }
   authRepository {
       default () {
           cls = 'tigase.db.jdbc.TigaseCustomAuth'
           'data-source' = 'default-auth'
       }
   }

This configuration defines just a default databases for both user repository and authentication repository. Default means it is used when there is no repository specified for a particular virtual domain. However, you can have a separate, both user and authentication repository for each virtual domain.

Here is, how it works:

First, let’s define our default database for all VHosts

.. code:: dsl

   dataSource {
       default () {
           uri = 'jdbc:mysql://db2.tigase/dbname?user&password'
       }
       'default-auth' () {
           uri = 'jdbc:mysql://db1.tigase/dbname?user&password'
       }
   }
   userRepository {
       default () {}
   }
   authRepository {
       default () {
           cls = 'tigase.db.jdbc.TigaseCustomAuth'
           'data-source' = 'default-auth'
       }
   }

Now, we have VHost: domain1.com User authentication data for this VHost is stored in Drupal database

.. code:: dsl

   dataSource {
     'domain1.com-auth' () {
       uri = jdbc:mysql://db7.tigase/dbname?user&password'
     }
   }
   authRepository {
     domain1.com () {
       cls = 'tigase/db/jdbc.TigaseCustomAuth'
       'data-source' = 'domain1.com-auth'
     }
   }

All other user data is stored in Tigase’s standard database in MySQL

.. code:: dsl

   dataSource {
     'domain1.com' () {
       uri = jdbc:mysql://db4.tigase/dbname?user&password'
     }
   }
   userRepository {
     domain1.com () {}
   }

Next VHost: domain2.com User authentication is in LDAP server but all other user data is stored in Tigase’s standard database

.. code:: dsl

   authRepository {
       domain2.com () {
           cls = 'tigase.db.ldap.LdapAuthProvider'
           uri = 'ldap://ldap.domain2.com:389'
           'data-source' = 'default'
       }
   }

Now is something new, we have a custom authentication repository and separate user settings for a single domain. Please note how we define the VHost for which we set custom parameters

.. code:: dsl

   authRepository {
       domain2.com {
           'user-dn-pattern' = 'cn=,ou=,dc=,dc='
       }
   }

All other user data is stored in the same as default repository

.. code:: dsl

   userRepository {
       domain2.com () {}
   }
   dataSource {
       domain2.com () {
           uri = 'jdbc:mysql://db2.tigase/dbname?user&password'
       }
   }

When combined, the DSL output should look like this:

.. code:: dsl

   dataSource {
       domain2.com () {
           uri = 'jdbc:mysql://db2.tigase/dbname?user&password'
       }
   }
   userRepository {
       domain2.com () {}
   }
   authRepository {
       domain2.com () {
           cls = 'tigase.db.ldap.LdapAuthProvider'
           uri = 'ldap://ldap.domain2.com:389'
           'user-dn-pattern' = 'cn=,ou=,dc=,dc='
       }
   }

Next VHost: domain3.com Again user authentication is in LDAP server but pointing to a different LDAP server with different access credentials and parameters. User information is stored in a postgreSQL database.

.. code:: dsl

   dataSource {
       domain3.com () {
           uri = 'jdbc:pgsql://db.domain3.com/dbname?user&password'
       }
   }
   userRepository {
       domain3.com () {}
   }
   authRepository {
       domain3.com () {
           cls = 'tigase.db.ldap.LdapAuthProvider'
           uri = 'ldap://ldap.domain3.com:389'
           'user-dn-pattern' = 'cn=,ou=,dc=,dc='
       }
   }

For VHost: domain4.com all the data, both authentication and user XMPP data are stored on a separate MySQL server with custom stored procedures for both user login and user logout processing.

.. code:: dsl

   dataSource {
       domain4.com () {
           uri = 'jdbc:mysql://db14.domain4.com/dbname?user&password'
       }
   }
   userRepository {
       domain4.com () {}
   }
   authRepository {
       domain4.com () {
           cls = 'tigase.db.jdbc.TigaseCustomAuth'
           'user-login-query' = '{ call UserLogin(?, ?) }'
           'user-logout-query' = '{ call UserLogout(?) }'
           'sasl-mechs' = [ 'PLAIN', 'DIGEST-MD5' ]
       }
   }

As you can see, it requires some writing but flexibility is very extensive and you can setup as many separate databases as you need or want. If one database (recognized by the database connection string) is shared among different VHosts, Tigase still uses a single connection pool, so it won’t create an excessive number of connections to the database.

