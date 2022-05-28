.. _Monitor-Component:

Monitor Component
^^^^^^^^^^^^^^^^^^^^^

Tigase includes an **Monitor Component** to help with monitoring has been implemented. This allows you to set thresholds for certain predefined tasks and you or other JIDs can be sent a message when those thresholds are passed. You can even configure a mailer extension to have an E-mail sent to system administrators to let them know an event has occurred! Lets begin with setup and requirements.

Monitor Component is based on eventbus which in turn is based on a limited `PubSub <http://www.xmpp.org/extensions/xep-0060.html>`__ specification. Events are delivered to subscribers as a normal PubSub notification.

Each component or client may subscribe for specific types of events. Only components on cluster nodes are allowed to publish events.

Setup
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Monitor Component is enabled by default on v7.1.0 b4001 and later, so no setup needed!

How it Works
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Events in Eventbus are identified by two elements: name of event and its namespace:

.. code:: xml

   <EventName xmlns="tigase:demo">
     <sample_value>1</sample_value>
   </EventName>

Where event name is ``EventName`` and namespace is ``tigase:demo``.

Listeners may subscribe for a specific event or for all events with specific a namespace. Because in pubsub, only one node name exists, so we have to add a way to convert the event name and namespace to a node name:

::

   nodename = eventname + "|" + namespace

So for example, to subscribe to ``<EventName xmlns="tigase:demo">``, node must be: ``EventName|tigase:demo``. If you wish to subscribe to all events with a specific namespace, use an asterisk (``*``) instead of the event name: ``*|tigase:demo``.

   **Note**

   If client is subscribed to ``*|tigase:demo node``, then events will not be sent from node ``*|tigase:demo``, but from the **real** node (in this case: ``EventName|tigase:demo``).

Available Tasks
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Monitor Component has several pre-defined tasks that can be monitored and set to trigger. What follows is the list of tasks with the options attributed to each task.

-  | **disk-task** - Used to check disk usage.
   | Available Options

   1. ``enabled`` - Enable or disable task, Boolean value.

   2. ``period`` - Period of running check, Integer value.

   3. ``threshold`` - Percentage of used space on disk, Float value.

-  | **cpu-temp-task** - Used to check CPU temperature.
   | Available Options

   1. ``enabled`` - Enable or disable task, Boolean value.

   2. ``period`` - Period of running check, Integer value.

   3. ``cpuTempThreshold`` - Temperature threshold of CPU in °C.

-  | **load-checker-task** - Used to check system load.
   | Available Options

   1. ``enabled`` - Enable or disable task, Boolean value.

   2. ``period`` - Period of running check, Integer value.

   3. ``averageLoadThreshold`` - Average percent load threshold, Long value.

-  | **memory-checker-task** - Used to check memory usage.
   | Available Options

   1. ``enabled`` - Enable or disable task, Boolean value.

   2. ``period`` - Period of running check, Integer value.

   3. ``maxHeapMemUsagePercentThreshold`` - Alarm when percent of used Heap memory is larger than, Integer value.

   4. ``maxNonHeapMemUsagePercentThreshold`` - Alarm when percent of used Non Heap memory is larger than, Integer value.

-  | **logger-task** - Used to transmit log entries depending on level entered.

   1. ``enabled`` - Enable or disable task, Boolean value.

   2. ``levelThreshold`` - Minimal log level that will be the threshold. Possible values are SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, and ALL.

-  | **connections-task** - Used to check users disconnections.
   | **NOTE: The event will be generated only if both thresholds (amount and percentage) will be fulfilled.**

   1. ``enabled`` - Enable or disable task, Boolean value.

   2. ``period`` - Period of running check in ms, Integer value.

   3. ``thresholdMinimal`` - Minimal amount of disconnected users required to generate alarm.

   4. ``threshold`` - Minimal percent of disconnected users required to generate alarm.

Configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Configuration of the monitor can be done one of two ways; either by lines in ``config.tdsl`` file, or by sending XMPP stanzas to the server. You may also send XMPP stanzas VIA HTTP REST. XMPP stanza configurations will override ones in config.tdsl, but they will only last until the server restarts.

config.tdsl

Tasks can be configured in the ``config.tdsl`` file. See `available tasks <#availableTasks>`__ for the tasks that can be setup.

To enable a specific monitor task, use the following line:

.. code::

   monitor {
       '$TASKNAME' {
           setting = value
       }
   }

Where monitor is the component name for ``MonitorComponent``, and ``$TASKNAME`` is one of the `available task names <#availableTasks>`__.

This format will be the same for other settings for tasks, and it’s best to group settings under one heading. For example:

.. code::

   monitor {
       'connections-task' {
           enabled = true
           period = 1000
       }
   }

sets the check period to 1000 milliseconds and enables ``connections-task``.

.. Note::

   Once triggers have been activated, they will become dormant. Think of these as one-shot settings.


Subscription Limitations

To define list of JIDs allowed to subscribe for events:

.. code::

   eventbus {
       affiliations {
           allowedSubscribers = 'francisco@denmark.lit,bernardo@denmark.lit'
       }
   }

If this is not specified, all users can subscribe.

Configuration via XMPP

We can also configure the eventbus monitor component using XMPP stanzas. This allows us to set and change configurations during server runtime. This is done using a series of ``iq`` stanzas send to the monitor component.

We can query each component for its current settings using the following stanza.

.. code:: xml

   <iq type="set" to="monitor@$DOMAIN/disk-task" id="aad0a">
       <command xmlns="http://jabber.org/protocol/commands" node="x-config"/>
   </iq>

The server will return the component current settings which will make things easier if you wish to edit them. In this case, the server has returned the following to us

.. code:: xml

   <iq from="monitor@$DOMAIN/disk-task" type="result" id="aad0a" to="alice@coffeebean.local/Psi+">
       <command xmlns="http://jabber.org/protocol/commands" status="executing" node="x-config"
                sessionid="0dad3436-a029-4082-b0e0-04d838c6c0da">
           <x xmlns="jabber:x:data" type="">
               <title>Task Configuration</title>
               <instructions/>
               <field type="boolean" label="Enabled" var="x-task#enabled">
                   <value>0</value>
               </field>
               <field type="text-single" label="Period [ms]" var="x-task#period">
                   <value>60000</value>
               </field>
               <field type="text-single" label="Disk usage ratio threshold" var="threshold">
                   <value>0.8</value>
               </field>
           </x>
       </command>
   </iq>

This tells us that the disk-task setting is not active, has a period of 60000ms, and will trigger when disk usage is over 80%.

To send new settings to the monitor component, we can send a similar stanza back to the monitor component.

.. code:: xml

   <iq type="set" to="monitor@$DOMAIN/disk-task" id="aad1a">
       <command xmlns="http://jabber.org/protocol/commands" node="x-config"
                sessionid="0dad3436-a029-4082-b0e0-04d838c6c0da">
           <x xmlns="jabber:x:data" type="submit">
               <field type="boolean" var="x-task#enabled">
                   <value>0</value>
               </field>
               <field type="text-single" var="x-task#period">
                   <value>60000</value>
               </field>
               <field type="text-single" var="threshold">
                   <value>0.8</value>
               </field>
           </x>
       </command>
   </iq>

To which a successful update will give you an XMPP success stanza to let you know everything is set correctly.

Alternatively, you can update specific settings by editing a single field without adding anything else. For example, if we just wanted to turn the disk-task on we could send the following stanza:

.. code:: xml

   <iq type="set" to="monitor@$HOSTNAME/disk-task" id="ab53a">
       <command xmlns="http://jabber.org/protocol/commands" node="x-config">
           <x xmlns="jabber:x:data" type="submit">
               <field type="boolean" var="x-task#enabled">
                   <value>1</value>
               </field>
           </x>
       </command>
   </iq>

To set any other values, do not forget that certain parts may need to be changed, specifically the ``<field type="boolean" var=x-task#enabled">`` fields:

-  | Your field type will be defined by the type of variable specified in the `Available Tasks <#availableTasks>`__ section.

-  ``var=x task#`` will be followed by the property value taken directly from the `Available Tasks <#availableTasks>`__ section.

Getting the Message
''''''''''''''''''''

Without a place to send messages to, monitor will just trigger and shut down. There are two different methods that monitor can deliver alarm messages and relevant data; XMPP messages and using the mailer extension.

XMPP notification

| In order to retrieve notifications, a subscription to the ``eventbus@<VHost>`` user must be made. Keep in mind that subscriptions are not persistent across server restarts, or triggers.
| The monitor schema is very similar to most XMPP subscription requests but with a few tweaks to differentiate it if you wanted to subscribe to a certain task or all of them. Each task is considered a node, and each node has the following pattern: ``eventName|eventXMLNS``. Since each monitoring task has the ``tigase:monitor:event`` event XMLNS, we just need to pick the event name from the list of tasks. So like the above example, our event node for the disk task will be ``disk-task|tigase:monitor:event``. Applied to an XMPP stanza, it will look something like this:

.. code:: xml

   <iq type='set'
       to='eventbus@<VHost>'
       id='sub1'>
     <pubsub xmlns='http://jabber.org/protocol/pubsub'>
       <subscribe node='disk-taskEvent|tigase:monitor:event' jid='$USER_JID'/>
     </pubsub>
   </iq>

Don’t forget to replace ``$USER_JID`` with the bare JID of the user you want to receive those messages. You can even have them sent to a MUC or any component with a JID.

Available events are as follows:

-  DiskUsageMonitorEvent for ``disk-task``

-  LoggerMonitorEvent for ``logger-task``

-  HeapMemoryMonitorEvent for ``memory-checker-task``

-  LoadAverageMonitorEvent for ``load-checker-task``

-  CPUTempMonitorEvent for ``cpu-temp-task``

-  UsersDisconnected for ``connections-task``

Alternatively, you can also subscribe to all events within the eventbus by using a wildcard \* in place of the event XMLNS like this example:

.. code:: xml

   <iq type='set'
       to='eventbus@<VHost>'
       id='sub1'>
     <pubsub xmlns='http://jabber.org/protocol/pubsub'>
       <subscribe node='*|tigase:monitor:event' jid='$USER_JID'/>
     </pubsub>
   </iq>

Sample notification from Monitor

.. code:: xml

   <message from='eventbus.shakespeare.lit' to='francisco@denmark.lit' id='foo'>
     <event xmlns='http://jabber.org/protocol/pubsub#event'>
       <items node='EventName|tigase:demo'>
         <item>
           <EventName xmlns="tigase:demo" eventSource="samplecomponent.shakespeare.lit'" eventTimestamp="1444216850">
             <sample_value>1</sample_value>
           </EventName>
         </item>
       </items>
     </event>
   </message>

Mailer Extension
'''''''''''''''''

*Tigase Server Monitor Mailer Extension* (TSMME) can send messages from the monitor component to a specified E-mail address so system administrators who are not logged into the XMPP server.

For v7.1.0 versions and later, TSMME is already included in your distribution package and no extra installation is needed.

Configuration

Tigase Mailer Extension may be configured via the ``config.tdsl`` file in the following manner:

.. code::

   monitor {
       'mailer-from-address' = 'sender@<VHost>'
       'mailer-smtp-host' = 'mail.tigase.org'
       'mailer-smtp-password' = '********'
       'mailer-smtp-port' = '587'
       'mailer-smtp-username' = 'sender'
       'mailer-to-addresses' = 'receiver@<VHost>,admin@<VHost>'
   }

Here is an explanation of those variables.

-  ``mailer-smtp-host`` - SMTP Server hostname.

-  ``mailer-smtp-port`` - SMTP Server port.

-  ``mailer-smtp-usernam`` - name of sender account.

-  ``mailer-smtp-password`` - password of sender account.

-  ``mailer-from-address`` - sender email address. It will be set in field from in email.

-  ``mailer-to-addresses`` - comma separated notification receivers email addresses.

It is recommended to create a specific e-mail address in your mail server for this purpose only, as the account settings are stored in plaintext without encryption.