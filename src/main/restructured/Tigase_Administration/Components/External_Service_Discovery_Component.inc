Tigase External Service Discovery
-------------------------------------

Welcome to the Tigase External Service Discovery component user guide. Component provides support for `XEP-0215: External Service Discovery <http://xmpp.org/extensions/xep-0215.html>`__ which allows discovery of external services which are not accessible using XMPP protocol.

.. _extdisco-setup
Setup & Configuration
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Component (which is implemented in class ``tigase.server.extdisco.ExternalServiceDiscoveryComponent``) is by default registered under name ``ext-disco`` and disabled. To enable it you need to enable it in configuration. Example:

-  in DSL format:

   .. code::

      ext-disco () { }

Additionally you need to activate ``urn:xmpp:extdisco:2`` XMPP processor in ``SessionManager`` by:

-  in DSL - enable subbean of ``sess-man``:

   .. code::

      sess-man {
          'urn:xmpp:extdisco:2'() {}
      }

List of external services returned by server is configurable using ad-hoc commands provided for this component. AdHoc commands are accessible only for server administrator using XMPP client with support for AdHoc commands or using Tigase Admin UI. Usage of AdHoc commands provides easiest and flexible way to add, modify or remove entries for services which will be returned by discovery.

Example configuration for TURN/STUN server
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

We are assuming that you have installed your preferred TURN server and created an account on the TURN server for use by your XMPP server users and that you have installed and configured Tigase XMPP Server with External Service Discovery enabled (see :ref:`extdiso-setup<Setup & Configuration>`).

Start with opening Admin UI by heading to ``http://<your-xmpp-server-and-port>/admin/`` (for example: https://localhost:8080). When prompted, log in by providing admin user credentials as the user and password. On the presented screen open **Configuration** group on the left by clicking on it.
Click on **Add New Item** which has **ext-disco@...** in subtitle, to open the form you need to fill.

.. image:: /images/web-admin-add-new-turn-item.png

In this form you will find following fields:

* **Service* - ID of the service which will be used for identification by Tigase XMPP Server *(eg. turn@example.com)*
* **Service name** - name of the service which may be presented to the user *(eg. TURN server)*
* **Host** - fully qualified domain name of the TURN server or its IP address *(eg. turn.example.com)*
* **Port** - port at which TURN server listens *(eg. 3478)*
* **Type** - type of the server, enter ``turn``
* **Transport** - type of transport used for communication with the server ``udp`` or ``tcp`` *(usually ``udp`` but item can be added for both)*
* **Requires username and password** - for notifying XMPP client that this service requires its username and password for XMPP service *(leave unchecked)*
* **Username** - username required for authentication for TURN server *(ie. turn-user)*
* **Password** - password required for authentication for TURN server *(ie. turn-password)*

After filling the form press ``Submit`` button to send form and add STUN/TURN server.

Example CoTURN installation
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

You can quickly setup CoTURN server using Docker. Please follow Docker installation on your operating system and then install CoTURN using `Docker Hub <https://hub.docker.com/r/instrumentisto/coturn>`__ (instrumentisto/coturn). The bare minimum required to run it looks like that (please update realm with your domain and external-ip with IP on which server should be accessible):

.. code-block::
    sudo docker run \
        --name coturn \
        -p 3478:3478 \
        -p 3478:3478/udp \
        -p 5349:5349 \
        -p 5349:5349/udp \
        -p 49160-49200:49160-49200 \
        coturn/coturn \
        --log-file=stdout \
        --min-port=49160 \
        --max-port=49200 \
        --realm localhost \
        --user tigase:tigase \
        --lt-cred-mech \
        --fingerprint \
        --external-ip=$$(detect-external-ip)

It uses ``tigase`` as username/password and localhost as realm - please adjust if needed.


