3. Tigase Server Extras - mDNS support
========================================

3.1. Overview
--------------

Tigase mDNS component provides you with ability to publish domain name of your XMPP server (ending with ``.local``) in the local network using multicast DNS (also known as DNS-SD, Zeroconf or Bonjour).


3.2. Enabling mDNS
-------------------

To enable this component you need to add mDNS component to your configuration file:

.. code:: dsl

   mdns () {
   }

This lines will enable mDNS support and will start broadcasting hostname of your host in the local network as ``hostname.local`` and will broadcast DNS records for XMPP server hosted at this domain.

3.3. Using different domain name
---------------------------------

If you are hosting different domain than hostname of your server with ``.local`` suffix, then you can set it in mDNS component settings by setting ``serverHost`` property to the name of your domain without suffix ``.local``.

**Example of broadcasting mDNS for domain ``example.local``.**

.. code:: dsl

   mdns () {
       serverHost = 'example'
   }


3.4. Forcing single server for domain
--------------------------------------

It is possible to enforce Tigase mDNS component to check if there is no other host providing services for chosen domain name. By setting property ``singleServer`` to ``true``. If this feature is enabled, then mDNS component checks if chosen domain is already in use (broadcasted in multicast DNS) and if so it stops startup of the server. This feature make it possible to start up Tigase and broadcast XMPP server mDNS information if already existing mDNS information resolves to the IP address of the host on which you are starting Tigase XMPP Server.

**Example enabling single server mode.**

.. code:: dsl

   mdns () {
       singleServer = true
   }

