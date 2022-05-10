LDAP Authentication Connector
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Tigase XMPP Server offers support for authenticating users against an LDAP server in **Bind** **Authentication** mode.

Configuration for the LDAP support is really simple you just have to add a few lines to your ``config.tdsl`` file.

.. code:: tdsl

   authRepository {
       default () {
           cls = 'tigase.db.ldap.LdapAuthProvider'
           uri = 'ldap://ldap.tigase.com:389'
           'user-dn-pattern' = 'cn=USER_ID,ou=people,dc=tigase,dc=org'
       }
   }

Please note the ``USER_ID`` element, this is a special element of the configuration which is used to authenticate particular user. Tigase LDAP connector replaces it with appropriate data during authentication. You can control what Tigase should put into this part. In your configuration you must replace this string with one of the following:

1. ``%1$s`` - use user name only for authentication (JabberID’s localpart)

2. ``%2$s`` - use domain name only for authentication (JabberID’s domain part)

3. ``%3$s`` - use the whole Jabber ID (JID) for authentication

.. Important::

   Please make sure that you included ``autoCreateUser=true`` in your main data source (UserRepository and **not** above AuthRepository) as outlined in `??? <#autoCreateUser>`__ - otherwise you may run into problems with data access.