Importing & Exporting User Data
--------------------------

Importing User Data from compatible repository
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

You can easily copy data between Tigase compatible repositories that is repositories for which there is a database connector. However, it is not that easy to import data from an external source. Therefore a simple data import functionality has been added to repository utilities package.

You can access repository utilities through command ``./bin/repo.sh`` or ``./scripts/repo.sh`` depending on whether you use a binary package or source distribution.

``-h`` parameter gives you a list of all possible parameters:

.. code:: sh

   ./scripts/repo.sh -h

   Parameters:
    -h          this help message
    -sc class   source repository class name
    -su uri     source repository init string
    -dc class   destination repository class name
    -du uri     destination repository init string
    -dt string  data content to set/remove in repository
    -u user     user ID, if given all operations are only for that ID
                if you want to add user to AuthRepository parameter must
                in form: "user:password"
    -st         perform simple test on repository
    -at         simple test for adding and removing user
    -cp         copy content from source to destination repository
    -pr         print content of the repository
    -n          data content string is a node string
    -kv         data content string is node/key=value string
    -add        add data content to repository
    -del        delete data content from repository
    ------------
    -roster     check the user roster
    -aeg [true|false]  Allow empty group list for the contact
    -import file  import user data from the file of following format:
            user_jid, password, roser_jid, roster_nick, subscription, group



   Note! If you put UserAuthRepository implementation as a class name
         some operation are not allowed and will be silently skipped.
         Have a look at UserAuthRepository to see what operations are
         possible or what operation does make sense.
         Alternatively look for admin tools guide on web site.

The most critical parameters are the source repository class name and the initialization string. Therefore there are a few example preset parameters which you can use and adjust for your system. If you look inside the ``repo.sh`` script you can find at the end of the script following lines:

.. code:: sh

   XML_REP="-sc tigase.db.xml.XMLRepository -su ../testsuite/user-repository.xml_200k_backup"
   MYSQL_REP="-sc tigase.db.jdbc.JDBCRepository -su jdbc:mysql://localhost/tigase?user=root&password=mypass"
   PGSQL_REP="-sc tigase.db.jdbc.JDBCRepository -su jdbc:postgresql://localhost/tigase?user=tigase"

   java $D -cp $CP tigase.util.RepositoryUtils $MYSQL_REP $*

You can see that the source repository has been set to MySQL database with ``tigase`` as the database name, ``root`` the database user and ``mypass`` the user password.

You can adjust these settings for your system.

Now to import data to your repository simply execute the command:

.. code:: sh

   ./bin/repo.sh -import import-file.txt

*Note, the import function is available from* **b895**

The format of the import file is very simple. This is a flat file with comma separated values:

.. code:: bash

   jid,password,roster_jid,roster_nick,subscriptio,group

To create such a file from MySQL database you will have to execute a command like this one:

.. code:: sql

   SELECT a, b, c, d INTO OUTFILE 'import-file.txt'
   FIELDS TERMINATED BY ','
   LINES TERMINATED BY '\n'
   FROM test_table;

Importing User Data with XEP-0227: Portable Import/Export Format
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

You can easily import data exported from another XMPP server (or exported for Tigase XMPP Server, see below) using XEP-0227: Portable Import/Export Format.

To import data, you need to have Tigase XMPP Server configured but stopped and files containing exported data stored in some location (in our case in directory ``xep-0227-data`` stored inside Tigase XMPP Server installation directory) on the same server as Tigase XMPP Server.

Now to import data to your repository you simply execute the command:

.. code:: sh

   scripts/tigase.sh import-data --from=xep-0227-data --include-mix --include-muc --include-pubsub

That will import all data including data from MIX, MUC, and PubSub component as well as data specified in XEP-0227.

*Note, if your data is located at a different place you will need to adjust ``--from`` parameter value.*

To list all of the options available for importing data (ie. allowing to import data from MIX, MUC, PubSub, or excluding MAM data), you should execute following command:

.. code:: sh

   scripts/tigase.sh import-data --help

   Usage:
   	$ scripts/tigase.sh [task] [params-file.conf] [options]
   		if the option defines default then <value> is optional

   Tasks:
   	export-data	-	Export data to XML
   	import-data	-	Import data from XML

   Options:

   	--help,  (optional)
   		Print the help

   	-I, --interactive,  (optional)
   		Enable interactive mode, which will result in prompting for missing parameters

   	--config-file=value,  (optional)
   		Path to Tigase XMPP Server config file (default: etc/config.tdsl)

   	--from=value,
   		Path to omport data from

   	--debug,  (optional)
   		Enable verbose logging (default: false)

   	--exclude-user-mam,  (optional)
   		Exclude users MAM archives (default: false)

   	--plain-credentials,  (optional)
   		Import PLAIN credentials (default: false)

   	--include-mix,  (optional)
   		Include MIX component data (default: false)

   	--include-muc,  (optional)
   		Include MUC component data (default: false)

   	--include-pubsub,  (optional)
   		Include PubSub component data (default: false)


Exporting User Data to XEP-0227: Portable Import/Export Format
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

You can easily export data from Tigase XMPP Server to the XML files using XEP-0227: Portable Import/Export Format.

To export data you need to execute following command in the Tigase XMPP Server installation directory. We are assuming here that files will be exported to ``xep-0227-data`` directory in Tigase XMPP Server installation directory.

Now to export data from your repository you simply execute the command:

.. code:: sh

   scripts/tigase.sh export-data --to=xep-0227-data --include-mix --include-muc --include-pubsub

That will export all data including data from MIX, MUC, and PubSub component as well as data specified in XEP-0227.

*Note, you prefer exported data to be located in a different place you will need to adjust ``--to`` parameter value.*

To list all of the options available for exporting data (ie. allowing to import data from MIX, MUC, PubSub, or excluding MAM data), you should execute following command:

.. code:: sh

   scripts/tigase.sh export-data --help

   Usage:
   	$ scripts/tigase.sh [task] [params-file.conf] [options]
   		if the option defines default then <value> is optional

   Tasks:
   	export-data	-	Export data to XML
   	import-data	-	Import data from XML

   Options:

   	--help,  (optional)
   		Print the help

   	-I, --interactive,  (optional)
   		Enable interactive mode, which will result in prompting for missing parameters

   	--config-file=value,  (optional)
   		Path to Tigase XMPP Server config file (default: etc/config.tdsl)

   	--to=value,
   		Path to export data to

   	--debug,  (optional)
   		Enable verbose logging (default: false)

   	--exclude-user-mam,  (optional)
   		Exclude users MAM archives (default: false)

   	--export-mam-since=value,  (optional)
   		Export MAM archive since

   	--export-mam-batch-size=value,  (optional)
   		Export MAM archive batch size (default: 50000)

   	--plain-credentials,  (optional)
   		Export PLAIN credentials (if any exist) (default: false)

   	--include-mix,  (optional)
   		Include MIX component data (default: false)

   	--include-muc,  (optional)
   		Include MUC component data (default: false)

   	--include-pubsub,  (optional)
   		Include PubSub component data (default: false)

   	--exclude-pep,  (optional)
   		Exclude user PEP data (default: false)