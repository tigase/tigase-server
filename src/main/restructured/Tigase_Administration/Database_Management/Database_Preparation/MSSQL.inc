.. _Prepare-the-MS-SQL-Server-Database-for-the-Tigase-Server:

Prepare the MS SQL Server Database for the Tigase Server
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This guide describes how to prepare the MS SQL Server database for connecting the Tigase server to it.

It’s expected that a working installation of Microsoft SQL Server is present. The following guide will describe the necessary configurations required for using MS SQL Server with Tigase XMPP Server.

Preparing MS SQL Server Instance
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

After installation of MS SQL Server an instance needs to be configure to handle incoming JDBC connections. For that purpose it’s required to open *SQL Server Configuration Manager*. In the left-hand side panel navigate to *SQL Server Configuration Manager*, then *SQL Server Network Configuration → Protocols for ${INSTANCE_NAME}*. After selecting instance in the right-hand side panel select TCP/IP and open *Properties*, in the Protocol tab in General section select Yes for Enabled property. In the IP Addresses tab select Yes for Active and Enabled properties of all IP Addresses that you want MS SQL Server to handle. Subsequently set the TCP Port property (if missing) to the default value - 1433. A restart of the instance may be required afterwards.

Configuration using MS SQL Server Management Studio
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In order to prepare the database you can use either a wizard or execute queries directly in the Query Editor. Firstly you need to establish a connection to the MS SQL Server instance. From Object Explorer select Connect and in the Connect to Server dialog enter administrator credentials.

Using Wizards

-  Create Login

   In the left-hand side panel select Security → Logins and from context menu choose New Login, in the Wizard window enter desired Login name, select SQL Server authentication and enter desired password subsequently confirming action with OK

-  Create Database

   From the Object Explorer select Databases node and from context menu select New Database; in the Wizard window enter desired Database name and enter previously created Login name into Owner field; subsequently confirming action with OK.


Using Queries

From the Object Explorer root node’s context menu select New Query. In the Query windows execute following statements adjusting details to your liking:

.. code:: sql

   USE [master]
   GO

   CREATE DATABASE [tigasedb];
   GO

   CREATE LOGIN [tigase] WITH PASSWORD=N'tigase12', DEFAULT_DATABASE=[tigasedb]
   GO

   ALTER AUTHORIZATION ON DATABASE::tigasedb TO tigase;
   GO



Import Schema
''''''''''''''

From the File menu Select Open → File (or use Ctrl+O) and then open following files:

.. code::

   sqlserver-common-0.0.1.sql
   sqlserver-common-0.0.2.sql
   sqlserver-server-7.0.0.sql
   sqlserver-server-7.1.0.sql
   sqlserver-server-8.0.0.sql
   sqlserver-muc-3.0.0.sql
   sqlserver-pubsub-3.1.0.sql
   sqlserver-pubsub-3.2.0.sql
   sqlserver-pubsub-4.0.0.sql
   sqlserver-http-api-2.0.0.sql

.. Note::

   These files must be done sequentially! They are not linked, and so may need to be done one at a time.

Other components may require installation such as:

.. code::

   sqlserver-socks5-2.0.0.sql
   sqlserver-push-1.0.0.sql
   sqlserver-message-archiving-2.0.0.sql
   sqlserver-unified-archive-2.0.0.sql

Subsequently select created database from the list of Available Databases (Ctrl+U) available on the toolbar and execute each of the opened files in the order listed above.

Configuring from command line tool
'''''''''''''''''''''''''''''''''''

Creation of the database and import of schema can be done from command line as well. In order to do that, execute following commands from the directory where Tigase XMPP Server is installed otherwise paths to the schema need to be adjusted accordingly:

.. code:: bash

   sqlcmd -S %servername% -U %root_user% -P %root_pass% -Q "CREATE DATABASE [%database%]"
   sqlcmd -S %servername% -U %root_user% -P %root_pass% -Q "CREATE LOGIN [%user%] WITH PASSWORD=N'%password%', DEFAULT_DATABASE=[%database%]"
   sqlcmd -S %servername% -U %root_user% -P %root_pass% -d %database% -Q "ALTER AUTHORIZATION ON DATABASE::%database% TO %user%;"
   sqlcmd -S %servername% -U %root_user% -P %root_pass% -d %database% -i database\sqlserver-schema-7-1-schema.sql
   sqlcmd -S %servername% -U %root_user% -P %root_pass% -d %database% -i database\sqlserver-schema-7-1-sp.sql
   sqlcmd -S %servername% -U %root_user% -P %root_pass% -d %database% -i database\sqlserver-schema-7-1-props.sql
   sqlcmd -S %servername% -U %root_user% -P %root_pass% -d %database% -i database\sqlserver-pubsub-schema-3.2.0.sql

Above can be automatized with provided script %tigase-server%\scripts\db-create-sqlserver.cmd (note: it needs to be executed from main Tigase XMPP Server directory due to maintain correct paths):

.. code:: sh

   $ scripts\db-create-sqlserver.cmd %database_servername% %database_name% %tigase_username% %tigase_password% %root_username% %root_password%

If no parameters are provided then the following defaults are used:

.. code:: bash

   %database_servername%=localhost
   %database_name%=tigasedb
   %tigase_username%=tigase
   %tigase_password%=tigase12
   %root_username%=root
   %root_password%=root

Tigase configuration - config.tdsl
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Configuration of the MS SQL Server follows general database convention.

.. code:: bash

   dataSource {
       default () {
           uri = 'jdbc:sqlserver://db_hostname:port[;property=val]'
       }
   }

where any number of additional parameters can (and should) consist of:

-  ``databaseName`` - name of the database

-  ``user`` - username configured to access database

-  ``password`` - password for the above username

-  ``schema`` - name of the database schema

-  ``lastUpdateCount`` - 'false' value causes all update counts to be returned, including those returned by server triggers

Example:

.. code::

   dataSource {
       default () {
           uri = 'jdbc:sqlserver://hostname:1433;databaseName=tigasedb;user=tigase;password=tigase12;schema=dbo;lastUpdateCount=false'
       }
   }

JDBC: jTDS vs MS JDBC driver (obsolete)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Previously Tigase XMPP Server was shipped with the jTDS open source driver however since 8.3.0 we switched to the FOSS driver provided by the Microsoft itself. Previous jdbc url will fallback to the Microsoft driver automatically.

-  Microsoft driver:

   .. code::

      dataSource {
          default () {
              uri = 'jdbc:sqlserver://...'
          }
      }
