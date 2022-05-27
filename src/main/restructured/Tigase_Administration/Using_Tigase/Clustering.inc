Tigase Clustering
-----------------------

Tigase Clustering allows the use of a number of servers to be unified in delivering, from what a client or user sees, a single unified platform. There are two typical reasons why clustering should be employed:

-  High Availability

   ::

      By using clustering, services can be provided with a high reliability and redundancy. By using a network of multiple servers, content or services can be served on any of the clustered servers maintaining a consistent uptime without relying on one machine.

-  Load Balancing

   ::

      This type of cluster helps to distribute a workload over a number of servers to reduce bottlenecking from heavy resource loads on a particular server.

With Tigase, you don’t have to choose between either/or!

**Tigase Clustering** offers **Full Redundancy** and **Automatic Load Balancing** allowing addition of new nodes at runtime with a simple configuration. All without a severe tax on resource consumption.

All basic components support clustering configuration, and some may be turned on or off.

Configuration
^^^^^^^^^^^^^^^^

To enable Clustering on Tigase servers, use the following line in your ``config.tdsl`` file:

.. code::

   'cluster-mode' = true

That’s it!

Custom Ports
~~~~~~~~~~~~~~~~

You can customize ports for the cluster component, just be sure that each clustered server also has the same settings so they can communicate.

.. code::

   cl-comp {
       connections {
           4250 {}
           3540 {}
       }
   }

You can fine tune each port configuration, however this is not typically needed.

Custom Port Configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Each port has it’s own details that can be manipulated via the following ports. Again **THIS IS OPTIONAL**

.. code::

   'cl-comp' {
       connections {
           4250 {
               ifc = [ '*' ]
               'remote-host' = 'localhost'
               socket = 'plain'
               type = 'accept'
               connections {
                   tls {
                       required = false
                   }
               }
           }
       }
   }


Multi-node configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Each node should have ``'cluster-mode' = true`` enabled that you wish to connect to the cluster. They will automatically discover other nodes to connect to VIA Server to Server traffic. Nodes that are added or removed will be periodically updated.

Traffic Control
~~~~~~~~~~~~~~~~~~

You can customize the traffic going between clustered servers with a few options.

cm-ht-traffic-throttling

This setting will control the number of bytes sent over non-user connections. Namely, Server to Server or S2S connections.

.. code::

   'cm-ht-traffic-throttling' = 'xmpp:25k:0:disc,bin:200m:0:disc'

The format is as follows: ``{traffic-type}:{maximum-traffic}:{max-lifespan-traffic}:{action}``

**traffic-type**
   Specifies the type of traffic controlled. This can either be **XMPP** or **bin**. XMPP limits the number of packets transferred, whereas bin limits the number of bytes transferred.

**maximum-traffic**
   Specifies how many bytes or packets may be sent within one minute.

**max-lifespan-traffic**
   Specifies how many bytes or packets may be sent within the lifetime of the connection. 0 means unlimited.

**action**
   Specifies the action to be taken which can be **disc** which disconnects the connection, or **drop** which will drop any data exceeding the thresholds.

cm-see-other-host

This allows the specific use of a load balancing mechanism by selecting ``SeeOtherHostIfc`` implementation. For more details, see `Tigase Load Balancing <#loadBalancing>`__ documentation.

Old configuration method
^^^^^^^^^^^^^^^^^^^^^^^^^^^

While these options are still available these settings CAN be less reliable. **Use ONLY if you need specific setups that cannot be accommodated by the automatic cluster mode**.


Specifying Specific nodes
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You can still use the old method of specifying every node on each server. Server 3 needs the following set

.. code::

   'cluster-nodes' = [ 'serv1.xmpp-test.org' , 'serv2.xmpp-test.org' ]

Server 2 needs

.. code::

   'cluster-nodes' = [ 'serv1.xmpp-test.org' , 'serv3.xmpp-test.org' ]

and so on…​

However, we do not recommend this.

Password and Port configuration

You may specify a password and port to specific cluster servers if that is required. To do so, you will need to add {password}:{port} to the domain, like this example:

.. code::

   'cluster-nodes' = [ 'serv1.xmpp-test.org:domainpass:5600' ]

Checking Cluster Connections
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

After setting up clustering you may want to verify that the clusters are operational. Right now it can be done in two manners - first by checking that there are actual network connections established between cluster nodes. The other is to check internal status of the server.

Established connections
~~~~~~~~~~~~~~~~~~~~~~~~~~~

There are number of ways to check for opened connections, simplest one use command line. (Tigase uses port *5277* for cluster connections)

-  Linux

   .. code:: sh

      $ lsof -iTCP:5277 -sTCP:ESTABLISHED -P -n

-  Windows

   .. code:: sh

      C:\WINNT>netstat -anp tcp | find ":5277 "


Cluster nodes connected (using XMPP)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Verifying clustering connectivity over XMPP protocol requires any XMPP client capable of `XEP-0030: Service Discovery <http://xmpp.org/extensions/xep-0030.html>`__. It’s essential to remember that only an administrator (a user whose JID is configured as administrative) has access.

Psi XMPP Client

For the purpose of this guide a `Psi <http://psi-im.org/>`__ client will be used. After successfully configuring and connecting to account with administrative privileges we need to access *Service Discovery*, either from application menu or from context menu of the particular account account:

|roster-discovery|

In the *Service Discovery* window we need to find *Cluster Connection Manager* component. After expanding the tree node for the component a list of all cluster nodes will be presented with the current status (either *connected* or *disconnected*). Node column will contain actual hostname of the cluster node:

|discovery-nodes|

.. |roster-discovery| image:: /images/admin/monitoring_xmpp_1.png
.. |discovery-nodes| image:: /images/admin/monitoring_clustering.png
