.. _tigaseScriptStart:

Tigase Script Selection
--------------------------

As mentioned in each of the quick start sections, each distribution of Tigase XMPP server comes with a number of scripts that are customized for different versions of Linux.

.. table:: init.d chart

   +------------------+--------------------------------------------------+-----------------------------------------------------------------------------------------------------------------+
   | Operating system | init.d file path                                 | Types of Operating Systems                                                                                      |
   +==================+==================================================+=================================================================================================================+
   | Systemd          | ``tigase-server/scripts/systemd/*``              | Systemd-based distributions                                                                                     |
   +------------------+--------------------------------------------------+-----------------------------------------------------------------------------------------------------------------+
   | Debian           | ``tigase-server/scripts/debian/tigase.init.d``   | Knoppix, Ubuntu (before v15.04), Raspbian or Duvian                                                             |
   +------------------+--------------------------------------------------+-----------------------------------------------------------------------------------------------------------------+
   | Gentoo           | ``tigase-server/scripts/gentoo/init.d/tigase``   | CoreOS (before v94.0.0), Tin Hat Linux or other \*too based systems                                             |
   +------------------+--------------------------------------------------+-----------------------------------------------------------------------------------------------------------------+
   | Mandriva         | ``tigase-server/scripts/mandriva/init.d/tigase`` | Specific init.d file for Mandriva Linux                                                                         |
   +------------------+--------------------------------------------------+-----------------------------------------------------------------------------------------------------------------+
   | Redhat           | ``tigase-server/scripts/redhat/init.d/tigase``   | RedHat (before v7.0) and other RPM based linux derivatives like CentOS (before v.7.14), openSUSE (before v12.2) |
   +------------------+--------------------------------------------------+-----------------------------------------------------------------------------------------------------------------+



.. Note::

   If your operating system is a systemd-based linux distribution, we recommend to use systemd service scripts. It may be possible to use (in this case legacy) ``init.d`` startup files as before, but usage of systemd startup scripts will allow better control of the startup process and will even allow for automatic restart of the Tigase XMPP Server in the case of JVM crash.


Configuration: For Linux Distributions using systemd
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To set up Tigase XMPP Server as a system service it is required to copy ``tigase-server.service`` file to ``/etc/systemd/system/`` directory

.. code:: bash

   sudo cp $SCRIPT_FILE_PATH/tigase-server.service /etc/systemd/system/

This file contains following parameters which may need to be adjusted:

-  ``User`` - Specifies the user that will run the program. This should be a user with SU permissions.

-  ``WorkingDirectory`` - Specifies installation directory *(default: ``/home/tigase/tigase-server``)*

-  ``ExecStart`` - Specifies startup command *(default: runs ``scripts/tigase.sh start etc/tigase.conf`` in the Tigase installation directory)*

-  ``ExecStop`` - Specifies shutdown command *(default: runs ``scripts/tigase.sh stop etc/tigase.conf`` in the Tigase installation directory)*

-  ``PIDFile`` - Specifies location of the PID file *(default: ``logs/tigase.pid`` file in the Tigase installation directory)*

It is also required to copy options file ``tigase-server`` to ``/etc/default/`` directory

.. code:: bash

   sudo cp $SCRIPT_FILE_PATH/tigase-server /etc/default/

With those files in place you need to reload ``systemctl`` daemon

.. code:: bash

   sudo systemctl daemon-reload

..

   **Note**

   If you are upgrading from the previous version of the Tigase XMPP Server which was not running as the systemd system service it is required to uninstall old service and remove old service files.


Configuration: For All Linux Distributions
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Once you’ve located the appropriate distribution scripts (please take a look at the table above), copy it to your system’s init.d folder (usually it’s ``/etc/init.d/``):

.. code:: bash

   sudo cp $SCRIPT_FILE_PATH /etc/init.d/tigase

You may also need to make it executable:

.. code:: bash

   sudo chmod +x /etc/init.d/tigase

It is recommended that you open the script files or configuration files as some have some parameters that you will need to specify.


Gentoo
~~~~~~~~~~

The conf.d script must contain the following parameters:

.. code:: conf

   TIGASE_HOME="/home/tigase/tigase-server"
   TIGASE_USER=tigase
   TIGASE_CONF="etc/tigase.conf"

The following should be configured:

-  ``TIGASE_HOME`` - Specifies the Tigase Server installation directory.

-  ``TIGASE_USER`` - Specifies the user that will run the program. This should be a user with SU permissions.

-  ``TIGASE_CONF`` - The location of tigase.conf file, relative to the ``TIGASE_HOME`` directory.


Mandriva
'''''''''

Mandriva has a single init.d file, however it should be configured:

.. code:: bash

   …
   export JAVA_HOME=/usr/java/jdk1.8.0
   export TIGASE_DIR=/opt/tigase/server/
   tigase=$TIGASE_DIR/scripts/tigase.sh
   prog=tigase
   config=$TIGASE_DIR/etc/tigase.conf
   …

The following should be configured:

-  ``JAVA_HOME`` - The location of your JDK Installation.

-  ``TIGASE_DIR`` - Tigase Server installation directory.

-  ``tigase`` - The location of your tigase.sh script. This should not need adjusting if you maintain the default file structure.

-  ``config`` - The location of your tigase.conf file. This should not need adjusting if you maintain the default file structure.

``pid`` file will be stored in ``/var/run/ser.pid``


Redhat
~~~~~~~~~~

Similar to Mandriva, you will need to configure the init.d file:

.. code:: bash

   …
   JAVA_HOME=/usr/lib/jvm/java/

   USERNAME=tigase
   USERGROUP=tigase
   NAME=tigase
   DESC="Tigase XMPP server"

   TIGASE_HOME=/home/tigase/tigase-server
   TIGASE_LIB=${TIGASE_HOME}/jars
   TIGASE_CONFIG=/etc/tigase.conf
   TIGASE_OPTIONS=
   TIGASE_PARAMS=

   PIDFILE=
   TIGASE_CONSOLE_LOG=
   …

-  ``USERNAME`` - Username running Tigase, should have su permissions.

-  ``USERGROUP`` - The usergroup of the username.

-  ``NAME`` - OS name for Tigase program.

-  ``DESC`` - Optional description.

-  ``TIGASE_HOME`` - The location of your Tigase Server installation directory.

-  ``TIGASE_LIB`` - The location of your Tigase Jars folder, you should not need to adjust this if you set ``TIGASE_HOME`` properly, and maintain the default file structure.

-  ``TIGASE_CONFIG`` - The location of your tigase.conf file relative to ``TIGASE_HOME``

-  ``TIGASE_OPTIONS`` - Legacy options for Tigase, most are now handled in ``config.tdsl`` or tigase.conf.

-  ``TIGASE_PARAMS`` - Parameters passed to command line when launching Tigase.

-  ``PIDFILE`` - Location of Tigase PID file if you wish to use custom directory. Default will be located in /logs or /var/temp directory.

-  ``TIGASE_CONSOLE_LOG`` - Location of Tigase Server console log file if you wish to use a custom directory. Default will be located in /logs directory, failing that /dev/null.

After you’ve copied the script, in order to install sysinit script you have to add it to the configuration:

.. code:: bash

   /sbin/chkconfig --add tigase

Service can be enabled or disabled service with:

.. code:: bash

   /sbin/chkconfig tigase <on|off|reset>

Debian
~~~~~~~~~~

As with other distributions you should copy init.d script to the correct location. Afterwards it should be edited and correct values for variables need to be set:

.. code:: bash

   …
   USERNAME=tigase
   USERGROUP=tigase
   NAME=tigase
   DESC="Tigase XMPP server"

   TIGASE_HOME=/usr/share/tigase
   TIGASE_CONFIG=/etc/tigase/tigase.config
   TIGASE_OPTIONS=
   TIGASE_PARAMS=

   PIDFILE=
   TIGASE_CONSOLE_LOG=
   …

-  ``USERNAME`` - Username running Tigase, should have su permissions.

-  ``USERGROUP`` - The usergroup of the username.

-  ``NAME`` - OS name for Tigase program.

-  ``DESC`` - Optional description.

-  ``TIGASE_HOME`` - The location of your Tigase Server installation directory.

-  ``TIGASE_CONFIG`` - The location of your tigase-server.xml file relative (old configuration format)

-  ``TIGASE_OPTIONS`` - command line arguments passed to Tigase server (which may include path to ``init.properies`` (if correct ``tigase.conf`` configuration will be found then it will translate to ``TIGASE_OPTIONS=" --property-file etc/config.tdsl "``

-  ``TIGASE_PARAMS`` - Parameters passed to command line when launching Tigase.

-  ``PIDFILE`` - Location of Tigase PID file if you wish to use custom directory. Default will be located in ``/var/run/tigase/tigase.pid`` or under (in this case relative to tigase home directory)\ ``logs/tigase.pid``.

-  ``TIGASE_CONSOLE_LOG`` - Location of Tigase Server console log file if you wish to use a custom directory. Default will be located in /logs directory, failing that /dev/null.

Afterwards we need to install service in the system with following command:

.. code:: bash

   update-rc.d tigase defaults


Running Tigase as a system service
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

There are a number of benefits to running Tigase as a service, one of which is to ensure that the program will run even in the event of a power outage or accidental server restart, Tigase will always be up and running.


For systemd-based linux distributions
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Once installation is complete you may start Tigase as a typical systemd service using following command:

.. code:: bash

   sudo systemctl start tigase-server

To stop it, you may run following command:

.. code:: bash

   sudo systemctl stop tigase-server

It is also possible to enable service, to make it start during startup of the operating system:

.. code:: bash

   sudo systemctl enable tigase-server


For other linux distributions
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Once installation is complete, you should be able to start Tigase using the following command:

.. code:: bash

   service tigase start

Tigase should begin running in the background. Since Tigase is now installed as a service, it can be controlled with any of the service commands, such as:

-  ``service tigase stop``

-  ``service tigase restart``