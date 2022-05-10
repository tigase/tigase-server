.. _manualinstall:

Manual Installation in Console Mode
----------------------------------------

Our preferred way to install the Tigase server is using `Web installer <#webinstall>`__ and configuration program which comes with one of the binary packages. Please pick up the latest version of the distribution archive in our `download section <https://tigase.net/downloads>`__.

In many cases however it is not always possible to use the web installer. In many cases you have just an ssh access or even a direct access in console mode only. We are going to provide a text-only installer in one of the next releases but for the time being you can use our binary packages to install the server manually. Please continue reading to learn how to install and setup the server in a few easy steps…​

If you have an old version of the Tigase server running and working and you intend to upgrade it please always backup the old version first.

.. Note::

   Please note that these instructions are for \*nix operating systems, and some modifications may be required for other Operating Systems!


Get the Binary Package
^^^^^^^^^^^^^^^^^^^^^^^^^^^

Have a look at our `download area <https://tigase.net/downloads>`__. Always pick the latest version of the package available. For manual installation either ``zip`` or ``tar.gz`` file is available. Pick one of files with filename looking like: ``tigase-server-<version>-b<build>-<type>.<archive>``, where ``<version>`` is in the form of ``major.minor.bugfix``, ``<type>`` can be either ``dist`` (basic package) or ``dist-max`` (extended set of components) and archive type can be eitehr ``tar.gz`` or ``zip``.


Unpack the Package
^^^^^^^^^^^^^^^^^^^^^^

Unpack the file using command for the tar.gz file:

.. code:: sh

    $ tar -xzvf tigase-server-x.y.z-bv.tar.gz

or for the zip file:

.. code:: sh

    $ unzip tigase-server-x.y.z-bv.zip

A new directory will be created: **tigase-server-x.y.z-bv/**.

Sometimes after unpacking package on unix system startup script doesn’t have execution permissions. To fix the problem you have to run following command:

.. code:: sh

    $ chmod u+x ./scripts/tigase.sh


Prepare Configuration
^^^^^^^^^^^^^^^^^^^^^^^^^^

If you look inside the new directory, it should like this output:

.. code:: sh

    $ ls -l
   total 88
   drwxr-xr-x 2 tigase tigase  4096 Aug 15 18:17 certs
   -rw-r--r-- 1 tigase tigase     0 Aug 15 18:26 ChangeLog
   drwxr-xr-x 2 tigase tigase 12288 Aug 15 18:17 database
   drwxrwxr-x 4 tigase tigase  4096 Oct 12 09:48 docs
   drwxrwxr-x 2 tigase tigase  4096 Oct 12 09:48 etc
   drwxrwxr-x 2 tigase tigase  4096 Oct 12 09:48 jars
   -rw-r--r-- 1 tigase tigase 34203 Aug 15 18:26 License.html
   drwxr-xr-- 2 tigase tigase  4096 Aug 15 18:26 logs
   -rw-r--r-- 1 tigase tigase  3614 Aug 15 18:26 package.html
   -rw-r--r-- 1 tigase tigase  2675 Aug 15 18:26 README
   drwxr-xr-x 9 tigase tigase  4096 Aug 15 18:17 scripts
   drwxr-xr-x 5 tigase tigase  4096 Aug 15 18:17 tigase
   drwxrwxr-x 4 tigase tigase  4096 Oct 12 09:48 win-stuff

At the moment the most important is the etc/ directory with these files:

.. code:: sh

    $ ls -l etc/
   total 36
   -rw-r--r-- 1 tigase tigase  153 Aug 15 18:11 bosh-extra-headers.txt
   -rw-r--r-- 1 tigase tigase  325 Aug 15 18:11 client-access-policy.xml
   -rw-r--r-- 1 tigase tigase  124 Aug 15 18:11 config.tdsl
   -rw-r--r-- 1 tigase tigase  263 Aug 15 18:11 cross-domain-policy.xml
   -rw-r--r-- 1 tigase tigase 2337 Aug 15 18:11 jmx.access
   -rw-r--r-- 1 tigase tigase 2893 Aug 15 18:11 jmx.password
   -rw-r--r-- 1 tigase tigase  735 Aug 15 18:11 logback.xml
   -rw-r--r-- 1 tigase tigase 3386 Aug 15 18:11 snmp.acl
   -rw-r--r-- 1 tigase tigase 1346 Aug 15 18:11 tigase.conf


Configure tigase.conf
~~~~~~~~~~~~~~~~~~~~~~~~~~

Tigase.conf is a file that contains general program operating parameters, and java settings for Tigase to run. For now, the only setting we need to set is the **JAVA_HOME** directory.

.. code:: sh

   JAVA_HOME="${JDKPath}"

Replace **${JDKPath}** with a path to Java JDK installation on your system.


Configure config.tdsl
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You need also to edit the ``config.tdsl`` file. It contains initial parameters normally set by the configuration program. As this is a manual installation, you will have to edit this document yourself. It contains already a few lines:

.. code:: dsl

   'config-type' = 'setup'

   http () {
       setup () {
           'admin-user' = 'admin'
           'admin-password' = 'tigase'
       }
   }

You will need to set a few things in order to get Tigase up and running.



Step 1: Change config-type


Refer to `config-type <#configType>`__ property description for details, but for most operations, change ``setup`` to ``default``.


Step 2: Set virtual host

Without a virtual host, your XMPP server has no domain with which to operate. To set a virtual host use the following configuration:

.. code:: dsl

   'default-virtual-host' = 'hostname'

You have to replace ``hostname`` with a domain name used for your XMPP installation. Let’s say this is **jabber.your-great.net**. Your setting should look like this:

.. code:: dsl

   'default-virtual-host' = 'jabber.your-great.net'

There are many other settings that can be configured `visit this section for details <#tigase41virtualHosts>`__.


Step 3: Set Administrators


At least one administrator is required, and once the database is setup will have the default password of ``tigase``. Be sure to change this once you have finished setting up your server. To add admins, use the following line in the ``config.tdsl`` file:

.. code:: dsl

   admins = [ 'admin@jabber.your-great.net', 'user2jabber.your-great.net' ]

Step 4: Set databases

You will also need to configure connection to the database. First you have to decide what database you want to use: ``Derby``, ``MySQL``, ``PostgreSQL``, ``MSSQL``, or ``MondoDB``. Each database will have slightly different configurations. If we are using derby, in a directory called ``tigasedb``, your configuration would look like this:

.. code:: dsl

   dataSource () {
       default () {
           uri = 'jdbc:derby:tigasedb;create=true'
       }
   }

Consult `dataSource <#dataSource>`__ property for more configuration info.

This is enough basic configuration to have your Tigase server installation running.


Install Database
^^^^^^^^^^^^^^^^^^^

Creating the database is the next step. Previously, we had scripts to handle this process, but we now have the advantage of functions in the ``tigase.sh`` script that can be used. Setting up the database can now be done using a single command.

.. code:: dsl

   ./scripts/tigase.sh install-schema etc/tigase.conf -T derby -D tigasedb -H localhost -U tigase_user -P tigase_pass -R root -A rootpass -J admin@jabber.your-great.net -N pass

This command will install tigase using a Derby database on one named ``tigasedb`` hosted on ``localhost``. The username and password editing the database is ``tigase_pass`` and ``root``. Note that ``-J`` explicitly adds the administrator, this is highly recommended with the ``-N`` passing the password. You may customize this command as needed, refer to the `install-schema <#install-schema>`__ section of the documentation for more information.

On a windows system, you need to call the program directly:

.. code:: windows

   C:\tigase>java -cp "jars/*" tigase.db.util.SchemaManager "install-schema" -T derby -D tigasedb -H localhost -U tigase_user -P tigase_pass -R root -A rootpass -J admin@jabber.your-great.net -N pass

If this successfully passes, you should see some information printed out

.. code:: bash

   LogLevel: CONFIG
   2017-10-12 20:05:47.987 [main]             DBSchemaLoader.init()                   CONFIG:   Parameters: [ingoreMissingFiles: false, logLevel: CONFIG, adminPassword: pass, admins: [admin@jabber.your-great.net], dbRootPass: rootpass, dbRootUser: root, dbType: derby, dbName: tigasedbx, dbHostname: localhost, dbUser: tigase_user, dbPass: tigase_pass, useSSL: false, useLegacyDatetimeCode: false, serverTimezone: null, file: null, query: null]
   Oct 12, 2017 8:05:48 PM tigase.util.DNSResolverDefault <init>
   WARNING: Resolving default host name: ubuntu took: 7
   Oct 12, 2017 8:05:49 PM tigase.db.util.SchemaManager loadSchemas
   INFO: found 1 data sources to upgrade...
   Oct 12, 2017 8:05:49 PM tigase.db.util.SchemaManager loadSchemas
   INFO: begining upgrade...
   LogLevel: CONFIG
   2017-10-12 20:05:49.877 [main]             DBSchemaLoader.init()                   CONFIG:   Parameters: [ingoreMissingFiles: false, logLevel: CONFIG, adminPassword: pass, admins: [admin@jabber.your-great.net], dbRootPass: rootpass, dbRootUser: root, dbType: derby, dbName: tigasedbx, dbHostname: null, dbUser: null, dbPass: null, useSSL: null, useLegacyDatetimeCode: false, serverTimezone: null, file: null, query: null]
   2017-10-12 20:05:49.877 [main]             DBSchemaLoader.validateDBConnection()   INFO:     Validating DBConnection, URI: jdbc:derby:tigasedbx;create=true
   2017-10-12 20:05:50.932 [main]             DBSchemaLoader.validateDBConnection()   CONFIG:   DriverManager (available drivers): [org.apache.derby.jdbc.AutoloadedDriver@65262308, jTDS 1.3.1, com.mysql.jdbc.Driver@54997f67, com.mysql.fabric.jdbc.FabricMySQLDriver@189633f2, org.postgresql.Driver@76fc5687]
   2017-10-12 20:05:50.932 [main]             DBSchemaLoader.validateDBConnection()   INFO:     Connection OK
   2017-10-12 20:05:50.933 [main]             DBSchemaLoader.validateDBExists()       INFO:     Validating whether DB Exists, URI: jdbc:derby:tigasedbx;create=true
   2017-10-12 20:05:50.936 [main]             DBSchemaLoader.withConnection()         CONFIG:   DriverManager (available drivers): [org.apache.derby.jdbc.AutoloadedDriver@65262308, jTDS 1.3.1, com.mysql.jdbc.Driver@54997f67, com.mysql.fabric.jdbc.FabricMySQLDriver@189633f2, org.postgresql.Driver@76fc5687]
   2017-10-12 20:05:50.937 [main]             DBSchemaLoader.lambda$validateDBExists$283()  INFO: Exists OK
   2017-10-12 20:05:50.939 [main]             DBSchemaLoader.loadSchemaFile()         INFO:     Loading schema from file(s): database/derby-schema-7-2.sql, URI: jdbc:derby:tigasedbx;create=true
   2017-10-12 20:05:50.941 [main]             DBSchemaLoader.withConnection()         CONFIG:   DriverManager (available drivers): [org.apache.derby.jdbc.AutoloadedDriver@65262308, jTDS 1.3.1, com.mysql.jdbc.Driver@54997f67, com.mysql.fabric.jdbc.FabricMySQLDriver@189633f2, org.postgresql.Driver@76fc5687]
   2017-10-12 20:05:51.923 [main]             DBSchemaLoader.lambda$loadSchemaFile$287()  INFO:  completed OK
   2017-10-12 20:05:51.925 [main]             DBSchemaLoader.loadSchemaFile()         INFO:     Loading schema from file(s): database/derby-message-archiving-schema-1.3.0.sql, URI: jdbc:derby:tigasedbx;create=true
   2017-10-12 20:05:51.926 [main]             DBSchemaLoader.withConnection()         CONFIG:   DriverManager (available drivers): [org.apache.derby.jdbc.AutoloadedDriver@65262308, jTDS 1.3.1, com.mysql.jdbc.Driver@54997f67, com.mysql.fabric.jdbc.FabricMySQLDriver@189633f2, org.postgresql.Driver@76fc5687]
   2017-10-12 20:05:52.209 [main]             DBSchemaLoader.lambda$loadSchemaFile$287()  INFO:  completed OK
   2017-10-12 20:05:52.210 [main]             DBSchemaLoader.loadSchemaFile()         INFO:     Loading schema from file(s): database/derby-muc-schema-2.5.0.sql, URI: jdbc:derby:tigasedbx;create=true
   2017-10-12 20:05:52.211 [main]             DBSchemaLoader.withConnection()         CONFIG:   DriverManager (available drivers): [org.apache.derby.jdbc.AutoloadedDriver@65262308, jTDS 1.3.1, com.mysql.jdbc.Driver@54997f67, com.mysql.fabric.jdbc.FabricMySQLDriver@189633f2, org.postgresql.Driver@76fc5687]
   2017-10-12 20:05:52.305 [main]             DBSchemaLoader.lambda$loadSchemaFile$287()  INFO:  completed OK
   2017-10-12 20:05:52.306 [main]             DBSchemaLoader.loadSchemaFile()         INFO:     Loading schema from file(s): database/derby-pubsub-schema-3.3.0.sql, URI: jdbc:derby:tigasedbx;create=true
   2017-10-12 20:05:52.307 [main]             DBSchemaLoader.withConnection()         CONFIG:   DriverManager (available drivers): [org.apache.derby.jdbc.AutoloadedDriver@65262308, jTDS 1.3.1, com.mysql.jdbc.Driver@54997f67, com.mysql.fabric.jdbc.FabricMySQLDriver@189633f2, org.postgresql.Driver@76fc5687]
   2017-10-12 20:05:52.731 [main]             DBSchemaLoader.lambda$loadSchemaFile$287()  INFO:  completed OK
   2017-10-12 20:05:52.732 [main]             DBSchemaLoader.addXmppAdminAccount()    INFO:     Adding XMPP Admin Account, URI: jdbc:derby:tigasedbx;create=true
   2017-10-12 20:05:52.732 [main]             DBSchemaLoader.addXmppAdminAccount()    CONFIG:   RepositoryFactory.getAuthRepository(null, jdbc:derby:tigasedbx;create=true,{data-repo-pool-size=1})
   Oct 12, 2017 8:05:52 PM tigase.db.jdbc.DataRepositoryImpl initialize
   INFO: Table schema found: jdbc:derby:tigasedbx;create=true, database type: derby, database driver: org.apache.derby.jdbc.EmbeddedDriver
   Oct 12, 2017 8:05:52 PM tigase.db.jdbc.DataRepositoryImpl initialize
   INFO: Initialized database connection: jdbc:derby:tigasedbx;create=true
   2017-10-12 20:05:52.884 [main]             DBSchemaLoader.addXmppAdminAccount()    INFO:     All users added
   2017-10-12 20:05:52.884 [main]             DBSchemaLoader.postInstallation()       INFO:     Post Installation, URI: jdbc:derby:tigasedbx;create=true
   2017-10-12 20:05:52.891 [main]             DBSchemaLoader.withConnection()         CONFIG:   DriverManager (available drivers): [org.apache.derby.jdbc.AutoloadedDriver@65262308, jTDS 1.3.1, com.mysql.jdbc.Driver@54997f67, com.mysql.fabric.jdbc.FabricMySQLDriver@189633f2, org.postgresql.Driver@76fc5687]
   2017-10-12 20:05:52.892 [main]             DBSchemaLoader.lambda$postInstallation$286()  INFO: Finalizing...
   2017-10-12 20:05:52.893 [main]             DBSchemaLoader.lambda$postInstallation$286()  INFO:  completed OK
   2017-10-12 20:05:52.895 [main]             DBSchemaLoader.shutdownDerby()          INFO:     Validating DBConnection, URI: jdbc:derby:tigasedbx;create=true
   2017-10-12 20:05:53.129 [main]             DBSchemaLoader.withConnection()         SEVERE:


   =====
   Failure: Database 'tigasedbx' shutdown.
   =====


   Oct 12, 2017 8:05:53 PM tigase.db.util.SchemaManager loadSchemas
   INFO: schema upgrade finished!




     =============================================================================
       Schema installation finished

     Data source: default with uri jdbc:derby:tigasedbx;create=true
       Checking connection to database ok
       Checking if database exists ok
       Loading schema: Tigase XMPP Server (Core), version: 8.0.0   ok
       Loading schema: Tigase Message Archiving Component, version: 1.3.0  ok
       Loading schema: Tigase MUC Component, version: 2.5.0    ok
       Loading schema: Tigase PubSub Component, version: 3.3.0 ok
       Adding XMPP admin accounts  ok
       Post installation action    ok

     Example etc/config.tdsl configuration file:

     'config-type' = 'default'
     debug = [ 'server' ]
     'default-virtual-host' = [ 'ubuntu' ]
     dataSource () {
         default () {
             uri = 'jdbc:derby:tigasedbx;create=true'
         }
     }
     amp () {}
     bosh () {}
     c2s () {}
     eventbus () {}
     http () {}
     'message-archive' () {}
     monitor () {}
     muc () {}
     pubsub () {}
     s2s () {}
     ws2s () {}
     =============================================================================

Note at the end, the script will output a recommended example file. You may use this in conjunction with your written config file, but some settings may not be set using this configuration. Again, it is only an **EXAMPLE**.


Start the Server
^^^^^^^^^^^^^^^^^^^^

You can start the server using the tigase file found in the scripts sub-directory of Tigase server base directory. There, select the type of linux you have, debian, gentoo, mendriva or redhat. In the root server directory type the following command:

.. code:: bash

   ./scripts/{OS}/init.d/tigase start etc/tigase.conf

Where {OS} is your \*nix operating system.

and you should get the output like this:

.. code:: sh

   Starting Tigase:
   nohup: redirecting stderr to stdout
   Tigase running pid=18103

Check if it is Working
^^^^^^^^^^^^^^^^^^^^^^^

The server is started already but how do you know if it is really working and there were no problems. Have a look in the ``logs/`` directory. There should be a few files in there:

.. code:: sh

    $ ls -l logs/
   total 40K
   -rw-r--r-- 1 20K 2009-02-03 21:48 tigase-console.log
   -rw-r--r-- 1 16K 2009-02-03 21:48 tigase.log.0
   -rw-r--r-- 1   0 2009-02-03 21:48 tigase.log.0.lck
   -rw-r--r-- 1   6 2009-02-03 21:48 tigase.pid

The first 2 files are the most interesting for us: **tigase-console.log** and **tigase.log.0**. The first one contains very limited information and only the most important entries. Have a look inside and check if there are any **WARNING** or **SEVERE** entries. If not everything should be fine.

Now you can connect with an XMPP client of your choice with the administrator account you setup earlier.