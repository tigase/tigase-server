Account Registration Limits
--------------------------------

In order to protect Tigase servers from DOS attacks, a limit on number of account registrations per second has been implemented. This may be configured by adding the following line in the ``config.tdsl`` file:

.. code::

   'registration-throttling' () {
       limit = 10
   }

This setting allows for 10 registrations from a single IP per second. If the limit is exceeded, a ``NOT_ALLOWED`` error will be returned.