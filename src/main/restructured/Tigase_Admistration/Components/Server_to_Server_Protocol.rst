Server to Server Protocol Settings
------------------------------------

Tigase server-to-server communication component facilitates communication with other XMPP servers (federation) and allows you to tweak it’s configuration to get a better performance in your installation.

S2S (or server to server) protocol is enabled by default with optimal settings chosen. There are however, a set of configuration parameters you can adjust the server behavior to achieve optimal performance on your installation.

This documents describes following elements of the Tigase server configuration:

1. Number of concurrent connections to external servers

2. The connection throughput parameters

3. Maximum waiting time for packets addressed to external servers and the connection inactivity time

4. Custom plugins selecting connection to the remote server

Number of Concurrent Connections
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Normally only one connection to the remote server is required to send XMPP stanza to that server. In some cases however, under a high load, you can get much better throughput and performance if you open multiple connections to the remote server.

This is especially true when the remote server works in a cluster mode. Ideally you want to open a connection to each of the cluster nodes on the remote server. This way you can spread the traffic evenly among cluster nodes and improve the performance for s2s connections.

Tigase server offers 2 different parameters to tweak the number of concurrent, s2s connections:

-  ``max-out-total-conns`` - this property specifies the maximum outgoing connections the Tigase server opens to any remote XMPP server. This is a **per domain** limit, which means that this limit applies to each of the remote domains Tigase connects to. If it is set to ``4`` then Tigase opens a maximum of 4 connections to ``jabber.org`` plus maximum 4 connections to ``muc.jabber.org`` even if this is the same physical server behind the same IP address.

   To adjust the limit you have to add following to the ``config.tdsl`` file:

   .. code:: dsl

      s2s {
          'max-out-total-conns' = 2
      }

-  ``max-out-per-ip-conns`` - this property specifies the maximum outgoing connections Tigase server opens to any remote XMPP server to its single IP address. This too, is **per domain** limit, which means that this limit applies to each of the remote domains Tigase connects to. If it is set to ``1``, and the above limit is set to ``4``, and the remote server is visible behind 1 IP address, then Tigase opens a maximum of 1 connection to ``jabber.org`` plus a maximum of 1 connection to ``muc.jabber.org`` and other subdomains.

   To adjust the limit you have to add following line to the ``config.tdsl`` file:

   .. code:: dsl

      s2s {
          'max-out-per-ip-conns' = 2
      }


Connection Throughput
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Of course everybody wants his server to run with maximum throughput. This comes with a cost on resources, usually increased memory usage. This is especially important if you have large number of s2s connections on your installations. High throughput means lots of memory for network buffers for every single s2s connection. You may soon run out of all available memory.

There is one configuration property which allows you to adjust the network buffers for s2s connections to lower your memory usage or increase data throughput for s2s communication.

More details about are available in the `net-buff-high-throughput <#netBuffHighThroughput>`__ or `net-buff-Standard <#netBuffStandard>`__ property descriptions.

Maximum Packet Waiting Time and Connection Inactivity Time
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

There are 2 timeouts you can set for the component controlling s2s communication.

-  ``max-packet-waiting-time`` - this sets the maximum time for the packets waiting for sending to some remote server. Sometimes, due to networking problems or DNS problems it might be impossible to send message to remote server right away. Establishing a new connection may take time or there might be communication problems between servers or perhaps the remote server is restarted. Tigase will try a few times to connect to the remote server before giving up. This parameter specifies how long the packet is waiting for sending before it is returned to the sender with an error. The timeout is specified in seconds:

   .. code:: dsl

      s2s {
          'max-packet-waiting-time' = 420L
      }

-  ``max-inactivity-time`` - this parameters specifies the maximum s2s connection inactivity time before it is closed. If a connection is not in use for a long time, it doesn’t make sense to keep it open and tie resources up. Tigase closes s2s connection after specified period of time and reconnects when it is necessary. The timeout is specified in seconds:

   .. code:: dsl

      s2s {
          'max-inactivity-time' = 900L
      }

Custom Plugin: Selecting s2s Connection
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Sometimes for very large installations you may want to set larger number of s2s connections to remote servers, especially if they work in cluster of several nodes. In such a case you can also have a control over XMPP packets distribution among s2s connections to a single remote server.

This piece of code is pluggable and you can write your own connection selector. It is enough to implement ``S2SConnectionSelector`` interface and set your class name in the configuration using following parameter in ``config.tdsl`` file:

.. code:: dsl

   s2s {
       's2s-conn-selector' = 'YourSelectorImplementation'
   }

The default selector picks connections randomly.

skip-tls-hostnames
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The ``s2s-skip-tls-hostnames`` property disables TLS handshaking for s2s connections to selected remote domains. Unfortunately some servers (certain versions of Openfire - [`1 <http://community.igniterealtime.org/thread/36206>`__] or [`2 <http://community.igniterealtime.org/thread/30578>`__]) have problems with TLS handshaking over s2s which prevents establishing a usable connection. This completely blocks any communication to these servers. As a workaround you can disable TLS for these domains to get communication back. Enabling this can be done on any vhost, but must be configured under the s2s component.

.. code:: dsl

   s2s {
       'skip-tls-hostnames' = [ 'domain1', 'domain2' ]
   }

ejabberd-bug-workaround
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

This property activates a workaround for a bug in EJabberd in it’s s2s implementation. EJabberd does not send dialback in stream features after TLS handshaking even if the dialback is expected/needed. This results in unusable connection as EJabberd does not accept any packets on this connection either. The workaround is enabled by default right now until the EJabberd version without the bug is popular enough. A disadvantage of the workaround is that dialback is always performed even if the SSL certificate is fully trusted and in theory this dialback could be avoided. By default, this is not enabled.

.. code:: dsl

   s2s {
       dialback () {
           'ejabbered-bug-workaround' = true
           }
   }

This replaces the old ``--s2s-ejabberd-bug-workaround-active`` property.