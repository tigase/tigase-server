Configuration of SASL EXTERNAL
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

In order to enable SASL External set "Client Certificate CA" (``client-trust-extension-ca-cert-path``) to the path containing Certification Authority (CA) certificate in the VHost (domain) configuration, for example ``/path/to/cacert.pem``

File ``cacert.pem`` contains Certificate Authority certificate which is used to sign clients certificate.

Client certificate must include user’s Jabber ID as ``XmppAddr`` in ``subjectAltName``:

   As specified in RFC 3920 and updated in RFC 6120, during the stream negotiation process an XMPP client can present a certificate (a “client certificate”). If a JabberID is included in a client certificate, it is encapsulated as an id-on-xmppAddr Object Identifier (“xmppAddr”), i.e., a subjectAltName entry of type otherName with an ASN.1 Object Identifier of “id-on-xmppAddr” as specified in Section 13.7.1.4 of RFC 6120, `XEP-0178 <http://xmpp.org/extensions/xep-0178.html#c2s>`__.

It is possible to make client certificate **required** using same VHost configuration and enabling option ``Client Certificate Required`` (``client-trust-extension-cert-required``).

If this option will be enabled, then client **must provide** certificate. This certificate will be verified against ``clientCertCA``. If client does not provide certificate or certificate will be invalid, **TLS handshake will be interrupted and client will be disconnected**.

Using this options does not force client to use SASL EXTERNAL. Client still may authenticate with other SASL mechanisms.