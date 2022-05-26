SASL Mechanisms
----------------------

XMPP protocol supports many authentication methods, but most of them are used as `SASL <https://tools.ietf.org/html/rfc4422>`__ mechanisms. Tigase XMPP Server provides many SASL-based authentication mechanisms such as:

-  PLAIN *(enabled)*

-  ANONYMOUS

-  EXTERNAL

-  SCRAM-SHA-1 *(enabled)*

-  SCRAM-SHA-256 *(enabled)*

-  SCRAM-SHA-512

Most of them are enabled by default on default Tigase XMPP Server installation.

Enabling and disabling SASL mechanisms (credentials encoder/decoder)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you want to enable or disable one of password-based authentication mechanism such as ``SCRAM-SHA-1``, ``SCRAM-SHA-256`` or ``SCRAM-SHA-512`` you can do that by enabling or disabling encoders and decoders used on your installation. By enabling encoders/decoders you are deciding in what form the password is stored in the database. Those changes may (and in most cases will) impact which SASL mechanisms may be allowed to use on your installation.

.. Note::

   In most cases you should enable or disable both (credentials encoder and decoder) of the same type at the same time. The only exception of this rule is when you are changing those on already working installation. In this case you should only enable encoder of the type which you want to enable and request users to change their passwords. Then, after users will change their passwords, you should reconfigure server to enable decoder of the particular type. *(in other case user may loose a way to log in to your installation as system will reject their credentials as it may not have matching credentials for particular SASL mechanism)*.

**Enabling SCRAM-SHA-512 encoder**
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. code::

   authRepository () {
       default () {
           credentialEncoders () {
               'SCRAM-SHA-512' () {}
           }
       }
   }

**Disabling SCRAM-SHA-1 decoder**
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
.. code::

   authRepository () {
       default () {
           credentialDecoders () {
               'SCRAM-SHA-1' (active: false) {}
           }
       }
   }

.. Warning::

    It is strongly recommended not to disable encoders if you have enabled decoder of the same type as it may lead to the authentication issues, if client tries to use a mechanism which that is not available.