Upgrading to v8.0.0 from v7.1.0
------------------------------------

There have been a number of changes to the user and auth databases since v7.1.0. As a result, if you are upgrading from older versions, you will need to follow this guide.

.. Note::

   We recommend installing Tigase XMPP Server 8.0.0 in a separate directory.

Backup your data
^^^^^^^^^^^^^^^^^^^^^

As with any migration it is highly recommended that you backup your repository before conducting any upgrade operations.

For MySQL databases:

.. code:: bash

   mysqldump [dbname] --routines -u [username] -p [password] > [filename].sql


Setup Tigase XMPP Server 8.0.0
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

After downloading Tigase XMPP Server 8.0.0 from our website, or using wget, extract the files to a separate directory.

Copy the ``tigase.conf`` and ``init.properties`` files from the old directory to v8.0.0 directory.

.. code:: bash

   cd tigase-server-8.0.0
   cp ../tigase-server/etc/tigase.conf etc/
   cp ../tigase-server/etc/init.properties etc/

Import the database.

.. code:: bash

   mysql -h [host address] [dbname] -u [username] -p [password] < [filename].sql
   mysql -h 198.27.120.213 tigase_tpub -u USERNAME -p <../tpub.2017-05-30.sql
   Enter password:


Upgrade configuration file
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Tigase XMPP Server has a utility that can be called using ``upgrade-config`` that will update your old ``init.properties`` file and create a new file using DSL.

.. code:: bash

   ./scripts/tigase.sh upgrade-config etc/tigase.conf

When everything is ready it will printout following information

::

   ===============================================================================
     Configuration file etc/init.properties was converted to DSL format.
     Previous version of a configuration file was saved at etc/init.properties.old
   ===============================================================================


Connect new database
^^^^^^^^^^^^^^^^^^^^^

Edit your new ``config.tdsl`` file to connect to the new database you created during the import step.

.. code::

   dataSource {
       default () {
           uri = 'jdbc:mysql://localhost/tigase_tpub?user=tigase_user&password=mypass'
       }
   }
   userRepository {
       default () {}
   }
   authRepository {
       default () {}
   }

Upgrade Database schema
^^^^^^^^^^^^^^^^^^^^^^^^^

Upgrading database schemas is now possible using the ``upgrade-schema`` option. Do this now.

.. code:: bash

   ./scripts/tigase.sh upgrade-schema etc/tigase.conf


.. Warning::

    Your database schema MUST be v8 or conversion will not occur properly!

You will be asked the following prompts:

.. code:: bash

   Database root account username used to create tigase user and database at 198.27.120.213 :

   Database root account password used to create tigase user and database at 198.27.120.213 :

Upon success, you should see the following:

.. code:: bash

   =============================================================================
           Schema upgrade finished

     Data source: default with uri
   jdbc:mysql://HOST/DATABASE?user=USERNAME&password=PASSWORD
           Checking connection to database ok
           Checking if database exists     ok
           Loading schema: Tigase XMPP Server (Core), version: 8.0.0       ok
           Loading schema: Tigase Message Archiving Component, version: 1.3.0      ok
           Loading schema: Tigase MUC Component, version: 2.5.0    ok
           Loading schema: Tigase PubSub Component, version: 3.3.0 ok
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


Upgrade/Restore with a script [experimental!]
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To make upgrade process easier it’s possible to utilize `tigase-upgrade.sh </../master/files/tigase-upgrade.sh>`__ \*nix shell script. It permits upgrading to new version (supports downloading version from provided URL).

It’s usage is as follows:

.. code:: bash

   ./tigase-upgrade.sh {upgrade|rollback} install_package install_directory [tar|dir]

Where: \* ``{upgrade|rollback}`` - defines whether to perform upgrade or rollback to previous version \* ``install_package`` - package to which perform upgrade (can be URL) in case of upgrade or backed-up installation (from which we want to restore) in case of rollback \* ``install_directory`` - destination directory (both in upgrade and rollback); can be symlink in which case it will be preserved with upgraded/restored path as target \* ``[tar|dir]`` - (optional) type of backup (either simply copy directory or also archive it using ``tar`` command); by default ``dir`` is used.

To upgrade installation to version ``tigase-server-8.0.0-SNAPSHOT-b5285-dist-max.tar.gz`` execute the script as follows:

.. code:: bash

   $ ./tigase-upgrade.sh upgrade tigase-server-8.0.0-SNAPSHOT-b5285-dist-max.tar.gz tigase-server

To rollback from ``tigase-server-8.0.0-SNAPSHOT-b5264_backup-18-11-05_1712`` backup execute script as follows:

.. code:: bash

   bash -x ./tigase-upgrade.sh rollback tigase-server-8.0.0-SNAPSHOT-b5264_backup-18-11-05_1712/ tigase-server
