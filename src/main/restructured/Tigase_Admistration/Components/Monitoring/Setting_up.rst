Setting Up Remote Monitoring in the Server
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Tigase server can be remotely monitored over following protocols: ``JMX/RMI``, ``SNMP`` and ``HTTP``. Even though ``JMX`` offers the biggest control and visibility to the server states, all of the monitoring services give the same basic set of the server statistics:

-  Number of network connections for s2s, c2s and Bosh

-  Last second, last minute and last hour load for all main components: SM, MR, c2s, s2s, Bosh, MUC and PubSub

-  System statistics - memory usage (heap and non heap) and the server uptime in milliseconds and human readable text.

-  Users statistics - number of registered users and number of online user session.

JMX/RMI and SNMP servers offer basic security and can restrict access while the HTTP server doesn’t offer any access restriction mechanisms. Therefore HTTP monitoring is recommended to operate behind a firewall.

The monitoring itself causes very low overhead in terms of the resources and CPU consumption on top of the normal Tigase processing requirements so it can be left on without worrying about performance degradation.

NOTE This works with the Tigase server from version **4.2.0** or build **1418**.

What You Need
~~~~~~~~~~~~~~

Statistics binaries are built-in ``-dist-max`` and no extra files are needed. If you have downloaded ``-dist`` file, you will need tigase-extras[https://github.com/tigase/tigase-extras] built and included in the ``jars/`` directory.

Activation
~~~~~~~~~~~~~~

You can either run the Tigase installer and use the configuration wizard to activate the monitoring or edit etc/config.tdsl file and add following lines:

.. code:: dsl

   monitoring() {
     jmx() {
       port = 9050
     }
     http() {
       port = 9080
     }
     snmp() {
       port = 9060
     }
   }

As you see there is a separate block for each monitoring server you want to activate. Each server is responsible for activation of a different protocol and takes a single parameter - port number. There are following protocols supported right now:

-  ``jmx`` - activating monitoring via JMX/RMI

-  ``http`` - activating monitoring over HTTP protocol

-  ``snmp`` - activating monitoring over SNMP protocol

You can have all protocols active at the same time or any combination of them or none.

Security
~~~~~~~~~~~~~~

Both JMX and SNMP offer security protection to limit access to monitoring data. The security configuration is a bit different for both.

JMX
~~~~~~~~~~~~~~

After the server installation or in the SVN repository you can find 2 files in the ``etc/`` directory: ``jmx.access`` and ``jmx.password``.

-  ``jmx.access`` is a user permission file. You can use it to specify whether the user can access the monitoring data for reading only ``readonly`` or with read-write ``readwrite`` access. There are example entries in the file already and the content may simply look like:

   .. code:: bash

      monitor readonly
      admin readwrite

-  ``jmx.password`` is a user password file. You can set user passwords here and the format again is very simple and the same as for jmx.access. There are example entries already provided for you convenience. Content of the file may look like the example below:

   .. code:: bash

      admin admin_pass
      monitor monitor_pass

Using above to files you can control who and how can access the JMX monitoring services.

SNMP

Access to SNMP monitoring is controlled using ACL (access control lists) which can be configured in the file ``snmp.acl`` located in ``etc/`` directory. It contains lots of detailed instructions how to setup ACL and restrict access per user, host and what kind access is allowed. The simplest possible configuration may look like this:

.. code:: bash

   acl = {
     {
       communities = public, private
       access = read-only
       managers = public.host.com, private.host.com
     }
     {
       communities = admin
       access = read-write
       managers = localhost, admin.host.com
     }
   }

You might also need Tigase MIB definition: `TIGASE-MANAGEMENT-MIB.mib <https://github.com/tigase/tigase-server/blob/master/src/main/resources/mib/JVM-MANAGEMENT-MIB.mib>`__ for the server specific statistics. The MIB contains definition for all the server statistics exposed via SNMP.

HTTP

Access the server at example.com:9080 and you will be presented with an Agent View.