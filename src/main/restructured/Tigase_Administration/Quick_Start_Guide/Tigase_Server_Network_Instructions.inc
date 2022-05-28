.. _setupTigaseServer:

Tigase Server Network Instructions
------------------------------------

One you have installed Tigase XMPP Server on a machine, you’re going to want to use it. If you are just using for local communications on a network behind a router, you’re all set. Enjoy and use!

However, if you want to have people from other computers outside your network connect to your server, you’re going to have to go through a few more steps to show your server out to the public.

   **Note**

   This guide is merely a recommendation of how to get a local server to be open to incoming communications. Any time you open ports, or take other security measures you risk compromising your network security. These are only recommendations, and may not be appropriate for all installations. Please consult your IT Security expert for securing your own installation.

XMPP, being a decentralized communication method, relies on proper DNS records to figure out where and how an XMPP server is setup. Operating an XMPP Server will require you to properly setup DNS routing so not only can clients connect to you, but if you decide to run a federated server and enable server to server communication, you will need to do the same. If you already have a DNS server already, you should have little issue adding these records. If you do not have a DNS setup pointing to your server, you may use a free dynamic name service such as dynu.com.

A Records
^^^^^^^^^^

You will not be able to use an IP Address or a CNAME record to setup an XMPP Server. While it’s not required, an A record can provide some other benefits such serving as a backup in case the SRV record is not configured right.

SRV Records
^^^^^^^^^^^^

You will need to set SRV records both for client-to-server (c2s) communication and, if you plan to use it, server to server (s2s) communication. We recommend both records are entered for every server as some resources or clients will check for both records. For this example we will use tigase.org is our domain, and xmpp as the xmpp server subdomain.

SRV records have the following form:

::

   _service._protocol.name. TTL class SRV Priority weight port target.

The key is as follows:

-  ``service``: is the symbolic name of the desired service, in this case it would be *xmpp-client* or *xmpp-server*.

-  ``protocol``: is the transport protocol, either TCP or UDP, XMPP traffic will take place over *TCP*.

-  ``name``: the domain name where the server resides, in this case *tigase.org*.

-  ``TTL``: a numeric value for DNS time to live in milliseconds, by default use *86400*.

-  ``class``: DNS class field, this is always *IN*.

-  ``priority``: the priority of the target host with lower numbers being higher priority. Since we are not setting up multiple SRV records, we can use *0*.

-  ``weight``: the relative weight for records with the same priority. We can use *5*.

-  ``port``: the specific TCP or UDP port where the service can be found. In this case it will be *5222* or *5269*.

-  ``target``: the hostname of the machine providing the service, here we will use *xmpp.tigase.org*.

For our example server, the SRV records will then look like this:

::

   _xmpp-client._TCP.tigase.org 86400 IN SRV 0 5 5222 xmpp.tigase.org
   _xmpp-server._TCP.tigase.org 86400 IN SRV 0 5 5269 xmpp.tigase.org

Tigase and Vhosts
^^^^^^^^^^^^^^^^^^^^

If you are running multiple vhosts or subdomains that you wish to separate, you will need another record. In this case an A record will be all you need if you are using default ports. If you are using custom ports, you will need to have a new SRV record for each subdomain.

Hosting via Tigase.me
^^^^^^^^^^^^^^^^^^^^^^^

If you don’t want to do all the hosting yourself, you can still have an XMPP service running in your own domain. The only condition right now is proper DNS service record (SRV) configuration that point to the following DNS address: ``tigase.me``.

We highly encourage using SRV records. If you want to register: **your-domain.tld** on our XMPP service make sure that it resolves correctly:

+----------------------------------------------+----------+--------------------------+--------------------------------------+
| Service                                      | DNS Type | DNS record               | Comment                              |
+----------------------------------------------+----------+--------------------------+--------------------------------------+
| ``_xmpp-client._tcp.your-domain.tld``        | SRV      | ``10 0 5222 tigase.me.`` | Basic XMPP                           |
+----------------------------------------------+----------+--------------------------+--------------------------------------+
| ``_xmpps-client._tcp.your-domain.tld``       | SRV      | ``10 0 5223 tigase.me.`` | DirectTLS                            |
+----------------------------------------------+----------+--------------------------+--------------------------------------+
| ``_xmpp-server._tcp.your-domain.tld``        | SRV      | ``10 0 5269 tigase.me.`` | Federation / s2s connection          |
+----------------------------------------------+----------+--------------------------+--------------------------------------+
| ``_xmpp-server._tcp.muc.your-domain.tld``    | SRV      | ``10 0 5269 tigase.me.`` | Federation / s2s connection (MUC)    |
+----------------------------------------------+----------+--------------------------+--------------------------------------+
| ``_xmpp-server._tcp.mix.your-domain.tld``    | SRV      | ``10 0 5269 tigase.me.`` | Federation / s2s connection (MIX)    |
+----------------------------------------------+----------+--------------------------+--------------------------------------+
| ``_xmpp-server._tcp.pubsub.your-domain.tld`` | SRV      | ``10 0 5269 tigase.me.`` | Federation / s2s connection (PubSub) |
+----------------------------------------------+----------+--------------------------+--------------------------------------+

.. **Note**::

   If you want to have MUC, MIX and PubSub available under your domain as subdomains, you have to setup DNS for your ``muc.your-domain.tld``, ``mix.your-domain.tld`` and ``pubsub.your-domain.tld`` domains too but they are optional.

You can check if the configuration is correct by issuing following commands:

.. code:: sh

   $ host -t SRV _xmpp-client._tcp.your-domain.tld
   $ host -t SRV _xmpps-client._tcp.your-domain.tld
   $ host -t SRV _xmpp-server._tcp.your-domain.tld
   $ host -t SRV _xmpp-server._tcp.muc.your-domain.tld
   $ host -t SRV _xmpp-server._tcp.pubsub.your-domain.tld

Now, how do you register your domain with our service?

There are a few ways. We recommend checking with the `Add and Manage Domains <#addManageDomain>`__ section of the documentation on setting that up. If you cannot or don’t want to do it on your own, the way described in the guide please send us a message, either via XMPP to admin@tigase.im or the contact form requesting new domain. User registration is available via in-band registration protocol. You can also specify whether you want to allow anonymous authentication to be available for your domain and you can specify maximum number of users for your domain.


Providing certificate
~~~~~~~~~~~~~~~~~~~~~~~~~

It’s also encouraged to provide dedicated SSL certificate - there are various ways to do it and they are described in :ref:`Installing/Loading Certificate To the Tigase Server<InstallingSSLCertificate>`. You may want to take advantage of free Let’s Encrypt certificates and automate whole upload and renewal process as described in :ref:`Installing LetsEncrypt Certificates in Your Linux System<LetsEncryptCertificate>`


Checking setup
^^^^^^^^^^^^^^^^^^^

If you have a cell phone on a separate network with an XMPP client, you can now try to login to test the server. If that is not handy, you can use an online tool to check proper DNS records such as kingant’s: https://kingant.net/check_xmpp_dns/ and it will tell you if anything is missing.


Ports description
^^^^^^^^^^^^^^^^^^^


Once your server is setup, you may need to open at least two ports. By default XMPP communication happens on ports 5222/5269, to which point SRV records. Other ports used by the server are:

-  ``3478`` - TURN or STUN, plain socket, TCP and UDP

-  ``5349`` - TURN or STUN, over TLS, TCP and UDP

-  ``5222`` - incoming client to server XMPP connections

-  ``5223`` - incoming client to server XMPP connections over TLS/SSL, including DirectTLS

-  ``5269`` - default s2s port, i.e.: federation support

-  ``5277`` - inter-cluster communication

-  ``5280`` - default BOSH connections

-  ``5290`` - default WebSocket connections

-  ``5291`` - default WebSocket connections over TLS/SSL

-  ``8080`` - for HTTP server (web-based setup, REST API, file upload extension, etc.)

-  ``9050`` - JMX Monitoring

If for any reason you can’t use default ports and have to change them it’s possible to point SRV records those ports. Please keep in mind, that you have to open those ports for incoming connections in your firewall. In case you are using ``iptables`` you can use following command to include those ports in your rules:

.. code:: bash

   iptables -A INPUT -p tcp -m tcp --dport 5222 -j ACCEPT
   iptables -A INPUT -p tcp -m tcp --dport 5223 -j ACCEPT
   iptables -A INPUT -p tcp -m tcp --dport 5269 -j ACCEPT
   iptables -A INPUT -p tcp -m tcp --dport 5277 -j ACCEPT
   iptables -A INPUT -p tcp -m tcp --dport 5280 -j ACCEPT
   iptables -A INPUT -p tcp -m tcp --dport 5290 -j ACCEPT
   iptables -A INPUT -p tcp -m tcp --dport 8080 -j ACCEPT
   iptables -A INPUT -p tcp -m tcp --dport 9050 -j ACCEPT

Both ports should be setup to use TCP only. If for any reason you want to make service available for different ports you can:

1. change ports in Tigase configuration and update DNS SRV records;

2. forward those ports to default Tigase ports (this is especially useful under \*nix operating system if you want to utilize ports lower than ``1024`` while running, as recommended, Tigase service from user account - there is a limitation and user accounts can bind to ports lower than ``1024``), for example using ``iptables`` rules (in following example we are making available Tigase SSL websocket port available under port ``443``, which is usually opened in corporate firewalls):

   .. code:: bash

      iptables -t nat -A PREROUTING -p tcp --dport 443 -j REDIRECT --to-ports 5291

