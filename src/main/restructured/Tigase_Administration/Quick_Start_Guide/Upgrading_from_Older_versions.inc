Upgrading Tigase to newer version
------------------------------------

.. Note::

   Depending of the deployment we recommend installing Tigase XMPP Server next to the existing one and following with replace of the service once the upgrade finishes correctly.

Backup your data
^^^^^^^^^^^^^^^^^^^^^

As with any migration it is **highly recommended** that you backup your repository before conducting any upgrade operations. It can be done via simple SQL dump od the database or more elaborate snapshot of the database offered by cloud providers.


Configuration files to migrate
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

During the upgrade the most important files to migrate to newer versions are:

* `etc/tigase.conf`
* `etc/config.tdsl`
* `etc/jmx.access` and `etc/jmx.password`
* `certs/*` (if configured to use local filesystem, though we recommend to use database for storing certificates)

Upgrade Database schema
^^^^^^^^^^^^^^^^^^^^^^^^^

Upgrading database schemas is now possible using the ``upgrade-schema`` option. Do this now.

.. code:: bash

   ./scripts/tigase.sh upgrade-schema etc/tigase.conf


.. Warning::

    Your database schema MUST be v8 or conversion will not occur properly!

You will be asked for rood credentials. Those can be provided as parameters to `/scripts/tigase.sh upgrade-schema` - please check `./scripts/tigase.sh upgrade-schema --help` for more details

Upon success, you should see the following:

.. code:: bash

   =============================================================================
           Schema upgrade finished

     Data source: default with uri
   jdbc:<database>://HOST/DATABASE?user=USERNAME&password=PASSWORD
           Checking connection to database ok
           Checking if database exists     ok
           Loading schema: Tigase XMPP Server (Core), version: x.y.z       ok
           Loading schema: Tigase Message Archiving Component, version: x.y.z      ok
           Loading schema: Tigase MUC Component, version: x.y.z    ok
           Loading schema: Tigase PubSub Component, version: x.y.z ok
           Adding XMPP admin accounts      warning
                   Message: Error: No admin users entered
           Post installation action        ok

   =============================================================================

Start Tigase!


Help?
^^^^^^^^^^^^^^^^^^^^^

Both ``upgrade`` commands also have a build in help function, they can be called if needed from the command line. You can also run these commands for help.

::

   scripts/tigase.sh upgrade-config etc/tigase.conf --help
   scripts/tigase.sh upgrade-schema etc/tigase.conf --help
