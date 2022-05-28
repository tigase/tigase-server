Configuration
=================

When the user tries to setup the client for the first time he comes across 2 configuration files: ``tigase.conf`` and ``config.tdsl`` in the ``/etc`` folder. Here is a brief explanation what all those files are about and in other sections you can learn all the details needed to configure the server.

1. :ref:`config.tdsl<dslConfig>` file is a simple text file with server parameters in form: **key** = **value**. When the XML configuration file is missing the Tigase server reads ``config.tdsl`` file and uses parameters found there as defaults for generation of the XML file. Therefore if you change the ``config.tdsl`` file you normally have to stop the server, remove the XML file and start the server again. All the settings from the ``config.tdsl`` are read and applied to the XML configuration. The properties file is easy to read and very safe to modify. At the moment this is the recommended way change the server configuration.

2. :ref:`tigase.conf<manualconfig>` is the Tigase server startup configuration. It is actually not used by the server itself. It rather contains operating system settings and environment parameters to correctly run the `Java Virtual Machine <https://www.oracle.com/java/technologies/>`__. It is only useful on the unix-like systems with Bash shell. If you run the server on MS Windows systems ``tigase.bat`` and ``wrapper.conf`` files are used instead. The ``tigase.conf`` file is read and loaded by the ``scripts/tigase.sh`` shell script which also scans the operating system environment for Java VM and other tools needed.

 
.. include:: DSL_configuration.inc
.. include:: Startup_Files.inc
.. include:: Settings_for_High_Load_Systems.inc
.. include:: JVM_settings.inc
.. include:: Session_Manager.inc  
.. include:: Vhosts.inc
.. include:: Logging.inc
.. include:: Advanced_Options.inc










