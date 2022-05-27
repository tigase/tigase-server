Startup File for tigase.sh - tigase.conf
--------------------------------------------

Property file names for ``tigase.sh`` startup script is a second parameter for the startup script. It can be skipped if environmental variables are set in different location or in different way.

Config file for startup script simply sets number of environment variables with the location of required components. Possible variables to set in this file are:

-  ``JAVA_HOME`` - location of Java installation home directory. **Must be set**.

-  ``TIGASE_HOME`` - location of Tigase installation home directory. *By default script try to find this location by searching directories from the location where the script has been run.*

-  ``TIGASE_CONSOLE_LOG`` - file to which all console messages will be redirected if server is run in background. By default it will be: ``TIGASE_HOME/logs/tigase-console.log``. **If this file/directory is not writable by Tigase process all console messages will be redirected to /dev/null**

-  ``TIGASE_PID`` location of the file with server PID number. By default it will be ``TIGASE_HOME/logs/tigase.pid``.

-  ``JAVA_OPTIONS`` - options for JVM like size of RAM allocated for the JVM, properties and so on.

-  ``TIGASE_OPTIONS`` - (optional) additional options for Tigase server program. You can tweak initial parameters for your environment here. If you want to specify custom location of your configuration file you should use ``--config-file <path/to/config.tdsl>`` configuration

Sample file to run **Tigase** with **PostgreSQL** database may look like:

.. code:: bash

   ENC="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
   DRV="-Djdbc.drivers=org.postgresql.Driver"
   JAVA_OPTIONS="${ENC} ${DRV} -server -Xms100M -Xmx100M "
   CLASSPATH=""
   TIGASE_CONFIG="tigase-pgsql.xml"
   TIGASE_OPTIONS=" "

Please note encoding settings. JVM by default uses encoding set in operating system environment. XMPP protocol, however uses ``UTF-8`` for all data processing. So the ENC settings enforces ``UTF-8`` encoding for all operations.

Another significant setting is \\'**CLASSPATH**'. It is intentionally set to an empty string. The **tigase.sh** startup script builds the **CLASSPATH** on it’s own from files found in **jars/** and **libs/** directories. It is advised to set the **CLASSPATH** to the empty string because the Tigase server scans all available classes to find all components and plugins implementation. If the **CLASSPATH** contains lots of libraries which are not used anyway it can cause a long startup time and high system loads.
