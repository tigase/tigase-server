Account Registration Limits
--------------------------------

In order to protect Tigase servers from DOS attacks, a limit on number of account registrations per second has been implemented. This may be configured by adding the following line in the ``config.tdsl`` file:

.. code:: dsl

   'registration-throttling' () {
       limit = 10

This setting allows for 10 registrations from a single IP per second. If the limit is exceeded, a ``NOT_ALLOWED`` error will be returned.

It is possible to create two separate counters as well:

.. code:: dsl

   'registration-throttling' () {
       limit = 10
   }
   c2s {
       'registration-throttling' (class: tigase.server.xmppclient.RegistrationThrottling) {
           limit = 3
       }
   }

Here we have one for c2s with a limit of 3, and a global for all other connection managers set at 10.

You can also set individual components limits as well:

.. code:: dsl

   ws2s {
       'registration-throttling' (class: tigase.server.xmppclient.RegistrationThrottling) {
           limit = 7
       }
   }
