.. _Debuging-Tigase:

Debuging Tigase
--------------------

If something goes wrong and you can’t find out why it is not working as expected, you might want more detailed debugging options switched on.

Tigase is a Java application and it uses Java logging library, this gives you the flexibility to switch logging on for selected Java packages or even for a single Java class.

Logs files are stored in ``logs/`` directory. ``tigase-console.log`` stores basic log data, but is the main log file. ``tigase.log.N`` files keep all the detailed logging entries. So this is the place where you should look in case of problems.

Configuration
^^^^^^^^^^^^^^^^

By default, Tigase has the old ``debug = ['server']`` setting is turned on and does not need to be added.

However, people want to see what is going on the network level. That is what has been sent and what has been received by the server - the actual character data. The class which would print all received and sent character data is: ``tigase.xmpp.XMPPIOService``. To enable all debugging info for this class you have to modify the debug line:

.. code::

   debug = [ 'xmpp.XMPPIOService' ]

You can also have debugging switched on for many packages/classes at the same time:

.. code::

   debug = [ 'cluster' , 'xmpp.XMPPIOService' ]

Other packages you might be interested in are:

-  ``io`` can print out what is going on a very low level network level including TLS/SSL stuff.

-  ``xml`` would print the XML parser debugging data.

-  ``cluster`` would print all the clustering related stuff.

-  ``xmpp.impl`` would print logs from all plugins loaded to Tigase server.

Non-Tigase packages
~~~~~~~~~~~~~~~~~~~~~~~~

To enable logging for your own packages from those different than Tigase, you have to use another option which has been made available for this:

.. code:: bash

   debug-packages = [ your.com.package ]