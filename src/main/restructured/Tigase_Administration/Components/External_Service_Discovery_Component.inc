Tigase External Service Discovery
-------------------------------------

Welcome to the Tigase External Service Discovery component user guide. Component provides support for `XEP-0215: External Service Discovery <http://xmpp.org/extensions/xep-0215.html>`__ which allows discovery of external services which are not accessible using XMPP protocol.

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
