.. _Watchdog:

Watchdog
-------------

Tigase’s Watchdog was implemented to help Tigase close connections that have become stale or inactive. Sometimes the connection is delayed, maybe dropped packets, or a service interruption. After a time, if that connection is re-established, both server and client (or server and server) will continue on as if nothing happened. However, these gaps in connection can last longer, and some installations will rely on the operating system to detect and close stale connections. Some operating systems or environments can take up to 2 hours or more to determine whether a connection is bad and wait for a response from a foreign entity and may not be configured. This can not only slow down performance, but can lead to security issues as well. To solve this problem, we have introduced Watchdog to monitor connections independent of operating system and environments to keep those broken connections from becoming a problem.

Setup
^^^^^^^

No extra setup is necessary, Watchdog is already included with your build of Tigase (as long as it’s 7.1.0 or newer). Follow the steps in the configuration section.

Watchdog Configuration
^^^^^^^^^^^^^^^^^^^^^^^^^

To configure watchdog, the following lines need to be present or edited in ``config.tdsl`` file:

.. code::

   'watchdog-timeout' = 70000
   'watchdog-delay' = 60000
   'watchdog-ping-type' = 'xmpp'

The three settings are as follows:

-  ``'watchdog-timeout'= 70000`` This setting sets the amount of time that watchdog will consider before it determines a connection may be stale. This setting sets the timeout at 70000ms or 70 seconds.

-  ``'watchdog-delay' = 60000`` This setting sets how often the watchdog should conduct the check, the default delay at 60000ms or 60 seconds.

-  ``'watchdog-ping-type'`` This setting determines the type of ping sent to components when watchdog is testing for activity.

You may, if you choose, to specify individual watchdog settings for specific components by adding them to the component settings, for example if we wanted to change the Client2Server settings to include watchdog, use the following lines in config.tdsl:

.. code::

   c2s {
       watchdog-delay = '1500'
       watchdog-timeout = '3000'
   }

If any settings are not set, the global or settings will be used. ``watchdog-delay`` default is set to 10 min ``watchdog-ping-type`` default is set to XMPP

Logic
^^^^^^^^

Watchdog compares it’s own pings, and records the time it takes for a round trip to different components, clustered connections, and if one variable is larger than the other, watchdog will commence closing that stale connection. Here is a breakdown:

1. A check is performed of a connection(s) on every ``watchdog-delay`` interval.

2. During this check two things occur

   -  If the last transfer time exceeds ``max-inactivity-time`` a stop service command is given to terminate and broadcast unavailable presence.

   -  If the last transfer time is lower than ``max-inactivity-time`` but exceeds ``watchdog-timeout`` watchdog will try to send a ping (of ``watchdog-ping-type``). This ping may be one of two varieties (set in config.tdsl)

      -  ``WHITESPACE`` ping which will yield the time of the last data transfer in any direction.

      -  ``XMPP`` ping which will yield the time of the last received xmpp stanza.

3. If the 2nd option is true, the connection will remain open, and another check will begin after the ``watchdog-delay`` time has expired.

For example, lets draw this out and get a visual representation

::

   -----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-----+-- 
        |     |     |     |     |     |     |     |     |     |     |
   ---+---------------------------------------------------------------- 
      1 2     3     4     5     6
   ---*-*-----*-----*-----*----------- 

-  This line represents how often the check is performed. Each ``-`` (dash) is 10 seconds, so the check is done every 60 seconds (``'watchdog-delay' = 60000``)

-  This line is client activity, here the client sent a message at 40 seconds (marked by ``+``) and has gone idle.

-  The following line represents the watchdog logic, with timeout at 120 seconds and max inactivity timeout at 180 seconds:

   .. code::

      'watchdog-timeout' = 120000
      c2s {
          'max-inactivity-time' = '180000'
      }
      (timeout at 120 seconds and max inactivity timeout at 180 seconds)

How the check is performed:

1. 30 seconds - at this point *last transfer* or *last received* time is updated.

2. 60 seconds - watchdog runs - it check the connection and says: \_ok, last client transfer was 20s ago - but it’s lower than both inactivity (so don’t disconnect) and timeout (so don’t send ping).

3. 120 seconds - 2nd check - last transfer was 100s ago - still lower than both values - do nothing.

4. 180 seconds - 3rd check - last transfer was 160s ago - lower than inactivity but greater than delay - ping it sent.

5. 240 seconds - 4th check - last transfer was 220s ago - client still hasn’t responded, watchdog compares idle time to ``max-inactivity-timeout`` and finds out that it is greater, connection is terminated.

6. 300 seconds - watchdog is run again but given the connection was terminatet there is no XMPP session to check for that particular client.

.. Tip::

   It is possible that the connection is broken, and could be detected during the sending of a ping and the connection would be severed at step 4 instead of waiting for step 5. **NOTE** This MAY cause JVM to throw an exception.

.. Note::

   Global settings may not be ideal for every setup. Since each component has its own settings for ``max-inactivity-time`` you may find it necessary to design custom watchdog settings, or edit the inactivity times to better suit your needs. Below is a short list of components with thier default settings:

   .. code::

      bosh {
          'max-inactivity-time' = 600L
      }
      c2s {
          'max-inactivity-time' = 86400L
      }
      'cl-comp' {
          'max-inactivity-time' = 180L
      }
      s2s {
          'max-inactivity-time' = 900L
      }
      ws2s {
          'max-inactivity-time' = 86400L
      }

.. Important::

   Again remember, for Watchdog to properly work, the ``max-inactivity-time`` MUST be longer than the ``watchdog-timeout`` setting

Testing
~~~~~~~~

The tigase.log.0 file can reveal some information about watchdog and how it is working (or how it might be fighting your settings). To do so, enter the following line into your ``config.tdsl`` file:

::

   debug = [ 'server', 'xmpp.init' ]

This will set debug mode for your log, and enable some more information about what Tigase is doing. These logs are truncated for simplicity. Lets look at the above scenario in terms of the logs:

**Stage Two.**

::

   2015-10-16 08:00:00.000 [Watchdog - c2s]   ConnectionManager$Watchdog$1.check()  FINEST: Testing service: c2s@xmpp.domain.com/192.168.0.150_5222_192.168.0.201_50368, type: accept, Socket: TLS: c2s@xmpp.domain.com/192.168.0.150_5222_192.168.0.201_50368 Socket[addr=/192.168.0.201,port=50368,localport=5222], jid: user@xmpp.domain.org/mobile, sinceLastTransfer: 20,000, maxInactivityTime: 180,000, watchdogTimeout: 120,000, watchdogDelay: 60,000, watchdogPingType: XMPP

**Stage Three.**

::

   2015-10-16 08:01:00.000 [Watchdog - c2s]   ConnectionManager$Watchdog$1.check()  FINEST: Testing service: c2s@xmpp.domain.com/192.168.0.150_5222_192.168.0.201_50368, type: accept, Socket: TLS: c2s@xmpp.domain.com/192.168.0.150_5222_192.168.0.201_50368 Socket[addr=/192.168.0.201,port=50368,localport=5222], jid: user@xmpp.domain.org/mobile, sinceLastTransfer: 100,000, maxInactivityTime: 180,000, watchdogTimeout: 120,000, watchdogDelay: 60,000, watchdogPingType: XMPP

**Stage Four.**

::

   2015-10-16 08:02:00.000 [Watchdog - c2s]   ConnectionManager$Watchdog$1.check()  FINEST: Testing service: c2s@xmpp.domain.com/192.168.0.150_5222_192.168.0.201_50368, type: accept, Socket: TLS: c2s@xmpp.domain.com/192.168.0.150_5222_192.168.0.201_50368 Socket[addr=/192.168.0.201,port=50368,localport=5222], jid: user@xmpp.domain.org/mobile, sinceLastTransfer: 160,000, maxInactivityTime: 180,000, watchdogTimeout: 120,000, watchdogDelay: 60,000, watchdogPingType: XMPP
   2015-10-16 08:02:00.697 [Watchdog - c2s]   ConnectionManager$Watchdog$1.check()  FINEST: c2s@xmpp.domain.com/192.168.0.150_5222_192.168.0.201_50368, type: accept, Socket: TLS: c2s@xmpp.domain.com/192.168.0.150_5222_192.168.0.201_50368 Socket[addr=/192.168.0.201,port=50368,localport=5222], jid: user@xmpp.domain.org/mobile, sending XMPP ping from=null, to=null, DATA=<iq from="xmpp.domain.com" id="tigase-ping" to="user@xmpp.domain.com/mobile" type="get"><ping xmlns="urn:xmpp:ping"/></iq>, SIZE=134, XMLNS=null, PRIORITY=NORMAL, PERMISSION=NONE, TYPE=get

**Stage Five.**

::

   2015-10-16 08:03:00.000 [Watchdog - c2s]   ConnectionManager$Watchdog$1.check()  FINEST: Testing service: c2s@xmpp.domain.com/192.168.0.150_5222_192.168.0.201_50368, type: accept, Socket: TLS: c2s@xmpp.domain.com/192.168.0.150_5222_192.168.0.201_50368 Socket[addr=/192.168.0.201,port=50368,localport=5222], jid: user@xmpp.domain.org/mobile, sinceLastTransfer: 100,000, maxInactivityTime: 180,000, watchdogTimeout: 120,000, watchdogDelay: 60,000, watchdogPingType: XMPP
   2015-10-16 08:03:00.248 [pool-20-thread-6]  ConnectionManager.serviceStopped()  FINER:  [[c2s]] Connection stopped: c2s@xmpp./domain.com/192.168.0.150_5222_192.168.0.201_50368, type: accept, Socket: TLS: c2s@lenovo-z585/192.168.0.150_5222_192.168.0.201_50368 Socket[unconnected], jid: user@xmpp.domain.com
   2015-10-16 08:03:00.248 [pool-20-thread-6]  ClientConnectionManager.xmppStreamClosed()  FINER: Stream closed: c2s@xmpp.domain.com/192.168.0.150_5222_192.168.0.201_50368