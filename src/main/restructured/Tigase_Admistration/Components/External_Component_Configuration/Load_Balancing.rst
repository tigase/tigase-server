Load Balancing External Components in Cluster Mode
------------------------------------------------------

This document describes how to load balance any external components using Tigase XMPP Server and how to make Tigase’s components work as external components in a cluster mode.

*Please note, all configuration options described here apply to Tigase XMPP Server version 8.0.0 or later.*

These are actually 2 separate topics:

1. One is to distribute load over many instances of a single component to handle larger traffic, or perhaps for high availability.

2. The second is to make Tigase’s components work as an external component and make it work in a cluster mode, even if the component itself does not support cluster mode.

Here are step by step instructions and configuration examples teaching how to achieve both goals.

Load Balancing External Component
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The first, and most simple scenario is to connect multiple instances of an external component to a single Tigase XMPP Server to distribute load.

There are at least 2 reasons why this would be an optimal solution: one would be to spread load over more instances/machines and the second is to improve reliability in case one component fails the other one can take over the work.

So here is a simple picture showing the use case.

|ExternalCompClustering002|

We have a single machine running Tigase XMPP Server and 2 instances of the MUC component connecting to Tigase.

On the server side we will enable ``ComponentProtocol`` component as we need to do to enable external component without clustering support.

Then using Admin UI we will add a new external component connection settings using ``Add item`` position for ``ext`` component in ``Configuration`` section of the web page just as it is described in `External Component Configuration <#tigaseExternalComponent>`__ section.

|adminui ext add item form_1|

The only change here is that we will specify value for field ``Load balancer class`` and we will use ``ReceiverBareJidLB`` as a value.

The configuration for both instances of the MUC component (identical for both of them) can be done in the same way as it is done for a single instance of the MUC component. There is nothing to change here.

The difference is one small element in the server configuration. At the value of ``Load balancer class`` field in ``Add item`` form is set to **ReceiverBareJidLB**.

This is the load balancing plugin class. Load balancing plugin decides how the traffic is distributed among different component connections that is different component instances. For the MUC component it makes sense to distribute the traffic based on the receiver bare JID because this is the MUC room address. This way we just distribute MUC rooms and traffic over different MUC component instances.

This distribution strategy does not always work for all possible components however. For transports for example this would not work at all. A better way to spread load for transports would be based on the source bare JID. And it is possible if you use plugin with class name: **SenderBareJidLB**.

This are two basic load distribution strategies available now. For some use cases none of them is good enough. If you have PubSub, then you probably want to distribute load based on the PubSub node. There is no plugin for that yet but it is easy enough to write one and put the class name in configuration.

External Component and Cluster
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you want to use Tigase’s component in a cluster mode which does not have clustering implemented yet there is a way to make it kind of cluster-able. In the previous section we connected many MUC components to a single Tigase server. Now we want to connect a single MUC component to many Tigase servers (or many Tigase cluster nodes).

Let’s say we have Tigase XMPP Server working for domain: **xmpp-test.org** and the server is installed on three cluster nodes: **red.xmpp-test.org,** **green.xmpp-test.org** and **blue.xmpp-test.org.**

|ExternalCompClustering003 0|

We want to make it possible to connect the MUC component to all nodes. To do so, we are configuring Tigase XMPP Server to run in the cluster mode and on each of cluster nodes we need to enable ``ComponentProtocol`` component.

This can be simply done by adding following line to the server configuration file:

.. code:: dsl

   ext () {}

After this is done we need to add a new external component connection settings using ``Add item`` position for ``ext`` component in ``Configuration`` section of the web page just as it is described in `External Component Configuration <#tigaseExternalComponent>`__ section.

As you can see there is nothing special here. The most interesting part comes on the MUC side, but it is only a very small change from the configuration of the component to use with single node Tigase XMPP Server installation.

When you are adding/configuring external component settings using Admin UI (``Add item`` or ``Update item configuration`` for ``ext-man`` component) or using separate configuration file (when you are not using shared database) then you need to pass as a value for ``Remote host`` field a semicolon separated list of all of the cluster nodes to which external component should connect.

In our case it would be:

::

   red.xmpp-test.org;green.xmpp-test.org;blue.xmpp-test.org

As you can see remote host name is not a simple domain but a character string with a few comma separated parts. The first part is our remote domain and the rest are addresses of the host to connect to. This can be a list of domain names or IP addresses.

Of course it is possible to connect multiple external component to all cluster nodes, this way the whole installation would be really working in the cluster and also load balanced.

.. |ExternalCompClustering002| image:: ../../../../asciidoc/admin/images/admin/ExternalCompClustering002.png
.. |adminui ext add item form_1| image:: ../../../../asciidoc/admin/images/admin/adminui_ext_add_item_form.png
.. |ExternalCompClustering003 0| image:: ../../../../asciidoc/admin/images/admin/ExternalCompClustering003_0.png