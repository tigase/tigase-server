Importing User Data
--------------------------

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
