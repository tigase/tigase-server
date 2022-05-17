Settings for Custom Logging in Tigase
----------------------------------------

Logging can be an important tool to monitor your server’s health and performance. Logging may be controlled and customized on a per-component basis.

A ``logging`` bean has been implemented to allow more flexible configuration of logging in the Tigase XMPP Server.

Configuring logging
^^^^^^^^^^^^^^^^^^^^^^^

In the config file
~~~~~~~~~~~~~~~~~~~~~~~

Default logging configuration for your installation is kept in the config file and it may be adjusted there.

.. code:: config

   logging () {
       rootLevel = CONFIG
       'packet-debug-full' = true
       loggers = {
           'tigase.server' = {
               level = ALL
           }
           'tigase.conf' = {
               level = FINEST
           }
       }
       handlers = {
           ' java.util.logging.FileHandler' = {
               level = ALL
               append = true
               count = 5
               formatter = 'tigase.util.LogFormatter'
               limit = 10000000
               pattern = 'logs/tigase.log'
           }
           'java.util.logging.ConsoleHandler' = {
               level = WARNING
               formatter = 'tigase.util.LogFormatter'
           }
       }
   }

You only need to specify the settings you wish to customize, otherwise they will be left as default.

-  ``packet-debug-full`` - controls whether log entries should be obfuscated (all CData of all elements will be replaced by ``CData size: <length in bytes of the replaced string>``) or not; default: ``false``.

-  ``rootLevel`` - Defines the root level of logging for all components not otherwise defined. Default is CONFIG

-  ``loggers`` - Defines the level of logging for packages running in tigase server. This is similar to the --debug setting, however you must use ``tigase.{package}`` format. Default is NONE.

-  ``handlers`` - Defines the level of logging for File output and Console output.

   1. ``FileHandler`` - is the file output for log files, with the following options:

      a. ``level`` - specifies the level of logs to be written, default is ALL.

      b. ``append`` - whether to append to the log or replace it during restart. Default is true.

      c. ``count`` - number of individual log files to keep at set limit. Default is 5. (default settings will continue appending logs until 5 files at 10MB are reached, then the oldest file will be overwritten.)

      d. ``formatter`` - specifies the package to format logging output. Default is tigase.util.LogFormatter.

      e. ``limit`` - Byte limit for each log file. Default is 10000000 or 10MB.

      f. ``pattern`` - Directory and filename of the log file with respect to the Tigase installation directory. Default is logs/tigase.log.

   2. ``ConsoleHandler`` - Determines the formatting for Tigase output to console.

      a. ``level`` - specifies the level of logs to be written, default is WARNING.

      b. ``formatter`` - specifies the package to format logging output. Default is tigase.util.LogFormatter.


Disabling colored output
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If for some reason you don’t want colored output in the logs you can disable it by setting ``disable_logger_color`` to ``true``. For convenience, you can uncomment in ``etc/tigase.conf`` following line:

.. code:: bash

   #DISABLE_LOGGER_COLOR=" -Ddisable_logger_color=true "

Ad-hoc changes to the logging configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

It is also possible to use ad-hoc command named **Set package logging** with id ``logging-set`` available at ``message-router@domain`` (where domain is your server name) to reconfigure logging level of packets at runtime without requirement of restarting the Tigase XMPP Server.

   **Note**

   Those changes will be applied to this single cluster node.

.. **Note**::

   Those changes will not be saved in the Tigase XMPP Server configuration file and will be lost after restart of Tigase XMPP Server.

Using Admin UI

If your Tigase XMPP Server is running with HTTP server and with Admin UI enabled, then the easiest way to change logging configuration is by using Admin UI. After logging into web interface, open ``Configuration`` section and select ``Set package logging`` command. This will bring to you a form which you need to fill in with following fields:

-  ``Package name`` - should contain Java package or class name for which you wish to change logging level

-  ``Level`` - select a logging level you wish to apply to entered package name *(``OFF`` means that logging will be disabled)*

After pressing ``Submit`` your form will be passed to the server for validation and selected changes will be applied.


Using ad-hoc command

If you have access to the XMPP admin account of Tigase XMPP Server and XMPP client which supports ad-hoc command execution, you may connect with your XMPP client to the Tigase XMPP Server and look for adh-hoc commands available at ``message-router@domain`` (where domain is your server name). Within found ad-hoc commands you should find command named ``Set package logging`` or ``logging-set`` (that depends what your XMPP client is showing, id or name of the command) and you should execute it. Tigase XMPP Server will return a form which you need to fill in with following fields:

-  ``Package name`` - should contain Java package or class name for which you wish to change logging level

-  ``Level`` - select a logging level you wish to apply to entered package name *(``OFF`` means that logging will be disabled)*

After submitting the form, Tigase XMPP Server will validate your request and update logging configuration.


Using REST API

If you have Tigase XMPP Server with REST API enabled, you can use it for configuring logging of Tigase XMPP Server as well.

   **Note**

   As with all HTTP REST API requests you will require a valid API key and in this case a valid admin credentials to authenticate a HTTP request using Basic HTTP Authentication.

All you need to to is to send a HTTP POST request to ``/rest/adhoc/message-router@domain.com`` (where domain is your server name) with ``Contect-Type`` set to ``application/xml`` and a following XML as a payload to set logging level of ``tigase.server`` package to ``ALL``.

.. code:: xml

   <command>
     <node>logging-set</node>
     <fields>
       <item>
         <var>package-name</var>
         <value>tigase.server</value>
       </item>
       <item>
         <var>level</var>
         <value>ALL</value>
       </item>
     </fields>
   </command>

.. **Tip**::

   You can set logging levels also using JSON payload. For more details please refer to `Tigase HTTP API Guide <https://docs.tigase.net/index.html#collapse-tigase-http-api-master-snapshot-head:>`__, to section ``Executing example ad-hoc commands`` in ``REST API - usage examples``.

Alternate loggers in Tigase - Logback
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

It’s possible to use Logback for logging purposes, which offers certain interesting features (async logging, better control over log rotation, on the fly changing logging configuration)

Requirements: \* slf4j-api.jar (provided in ``-dist-max`` package) \* jul-to-slf4j.jar (provided in ``-dist-max`` package) \* desired logger libraries (for logback it’s ``logback-classic.jar`` and ``logback-core.jar`` (provided in -dist-max).

Configuration boils down to adding slf4j bridge handler to the list of build-in Java Logger handlers configuration, which in Tigase translates to adding following line to ``etc/config.tdsl``:

.. code:: bash

   logging () {
       rootHandlers = [ 'java.util.logging.ConsoleHandler', 'java.util.logging.FileHandler', 'org.slf4j.bridge.SLF4JBridgeHandler' ]
   }

After that ``etc/logback.xml`` configuration file will be used.

As stated in [jul-to-slf4j bridge documentation](\ http://www.slf4j.org/legacy.html#jul-to-slf4j) it’s essential to include ``LevelChangePropagator`` to eliminate translation overhead for disabled log statements:

.. code:: xml

   <configuration debug="true">
     <contextListener class="ch.qos.logback.classic.jul.LevelChangePropagator"/>
     ...
   </configuration>

NOTE, that it may be prudent to remove configuration of all old JUL logger by appending following to ``etc/logback.xml`` configuration:

.. code:: xml

   <configuration debug="true">
     <contextListener class="ch.qos.logback.classic.jul.LevelChangePropagator"/>
       <resetJUL>true</resetJUL>
   </configuration>