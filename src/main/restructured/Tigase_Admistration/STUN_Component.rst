14. Tigase STUN Component
=========================

Welcome to Tigase STUN component guide

14.1. Tigase STUN Component
------------------------------

Tigase STUN Component allows for the use of a STUN server to handle XMPP and related communications to allow for smoother server operations behind a NAT.

14.1.1. What is STUN?
^^^^^^^^^^^^^^^^^^^^^^^^^

STUN stands for Simple Traversal of UDP[User Datagram Protocol] Through NAT[Network Address Translators]. It allows for computers behind a NAT router to host and provide UDP information without having to create rule exceptions on the router, or provide specific information to the NAT service. When specified within Tigase, XMPP and UDP communications can be directed to a specific STUN server which will then handle incoming requests to your network. You may use a public, or your own STUN server with Tigase.

14.1.2. Requirements
^^^^^^^^^^^^^^^^^^^^^^

The only requirement (aside from configuration) is that you are operating on a network that is not a Symmetric NAT as STUN by itself will not function correctly.

14.2. Configuration
--------------------

Below is an example configuration for STUN component. Note that the 2 ``stun-primary`` and 2 ``stun-secondary`` settings are required, where external settings are not.

.. code:: dsl

   stun (class: tigase.stun.StunComponent) {
       'stun-primary-ip' = '10.0.0.1'
       'stun-primary-port' = 3478
       'stun-secondary-ip' = '10.0.0.2'
       'stun-secondary-port' = 7001
       'stun-primary-external-ip' = '172.16.0.22'
       'stun-primary-external-port' = 3479
       'stun-secondary-external-ip' = '172.16.0.23'
       'stun-secondary-external-port' = 7002
   }

.. Note::

   Primary port should be set to 3478 as it is default port for STUN servers.

14.2.1. Setting descriptions
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

1. ``stun-primary-ip`` - primary IP address of STUN server used for binding (and sending to client if stun-primary-external-ip)

2. ``stun-primary-port`` - primary port of STUN server used for binding (and sending to client if stun-primary-external-port)

3. ``stun-secondary-ip`` - secondary IP address of STUN server used for binding (and sending to client if stun-secondary-external-ip)

4. ``stun-secondary-ip`` - secondary port of STUN server used for binding (and sending to client if stun-secondary-external-port)

If you wish to have a secondary STUN server as a backup, or to provide multiple addresses for STUN services, the following may be used.

1. ``stun-primary-external-ip`` - primary external IP address of STUN server used for sending to client if set

2. ``stun-primary-external-port`` - primary external port of STUN server used for sending to client if set

3. ``stun-secondary-external-ip`` - secondary external IP address of STUN server used for sending to client if set

4. ``stun-secondary-external-port`` - secondary external port of STUN server used for sending to client if set


14.2.2. Logback configuration
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

You man want to use logback for STUN server to append normal server logs. To do this, specify the logback xml file within java options in the ``tigase.conf`` file.

.. code:: config

   JAVA_OPTIONS="-Dlogback.configurationFile=etc/logback.xml"

You may configure the logback by editing the xml included with distributions at logback.xml.

What is included is a basic logback configuration that just adds the stun logging.

.. code:: xml

   <configuration  scan="true">

     <appender name="STDOUT"
       class="ch.qos.logback.core.ConsoleAppender">
       <encoder>
         <pattern>
           %d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
        </pattern>
       </encoder>
     </appender>

     <logger name="de.javawi.jstun.header.MessageHeader" level="INFO" />

     <root level="DEBUG">
       <appender-ref ref="STDOUT" />
     </root>

   </configuration>
