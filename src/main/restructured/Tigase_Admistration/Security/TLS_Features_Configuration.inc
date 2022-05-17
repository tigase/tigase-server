TLS/SSL encryption features configuration
---------------------------------------------

Tigase allows adjusting the most important parameters used when establishing TLS connections - set of protocols and ciphers that will be used during negotiation of the connection. The single most important is ``hardened-mode`` - it’s the most general configuration and offers three-step adjustment of the settings - please see `??? <#hardenedMode>`__ for details. ``hardened-mode`` can be configured both via TDSL configuration file (either on ``root`` level or for ``sslContextContainer`` for particular connection managers) or on VHost level.

If you want to disable certain protocols or ciphers you can use two options: ``tls-disabled-protocols`` and ``tls-disabled-ciphers`` respectively. They allow, as name suggests, disabling certain items from default sets. They both takes an array of strings, which ten be removed from the lists.

Let’s say you’d like to remove support for ``SSL``, ``SSLv2`` and ``SSLv3`` protocols. You should simply use following configuraiton: ``'tls-disabled-protocols' = ['SSL', 'SSLv2', 'SSLv3']``. Complete list of protocols depends on particular Java version that you use - please refer to the documentation for details. For example for the default Java11 list you can check `SSLContext Algorithms <https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#sslcontext-algorithms>`__

``tls-disabled-ciphers`` follows same format and uses names defined in `JSSE Cipher Suite Names <https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#jsse-cipher-suite-names>`__. It’s also possible to use regular expressions to quickly eliminate groups of ciphers.

If you want to enable only specific protocols or ciphers irrespective of ``hardened-mode`` or above disabling options you can use ``tls-enabled-protocols`` and ``tls-enabled-ciphers`` - those two options take arrays as well and they will configure Tigase to use only those protocols or ciphers that are provided (without support for regular expressions). Therefore if you configure Tigase with ``'tls-enabled-protocols' = [ 'TLSv1.2' ]`` then **only** ``TLSv1.2`` will be supported by Tigase.

The last option that you may be interested in adjusting is ``ephemeral-key-size`` - it follows Java’s configuration capabilities outlined in `Customizing Size of Ephemeral Diffie-Hellman Keys <https://docs.oracle.com/en/java/javase/11/security/java-secure-socket-extension-jsse-reference-guide.html#GUID-D9B216E8-3EFC-4882-B76E-17A87D8F2F9D>`__. Tigase defaults Diffie-Hellman keys of 4096 bits.

.. Important::

   We try to provide the best default set of options therefore **it’s recommendable to use defaults provided by Tigase**. If you want to make your extremely secure (considering possible connectivity issues with installations that may be less secure) then you should only adjust ``hardened-mode`` setting (and switch it to ``strict``).

Testing hosts TLS capabilities
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you run into issues with TLS connectivity it’s helpful to compare if both installations support same set of protocols and ciphers. One of the most versatile and helpful tools is `Mozilla’s CipherScan <https://github.com/mozilla/cipherscan>`__. For example for our installation ``tigase.im`` result would look like this:

::

   $ ./cipherscan --curves -starttls xmpp -servername tigase.im tigase.me:5222
   .......................................................................
   Target: tigase.me:5222

   prio  ciphersuite                  protocols              pfs                 curves
   1     ECDHE-RSA-AES256-GCM-SHA384  TLSv1.2                ECDH,B-571,570bits  sect283k1,sect283r1,sect409k1,sect409r1,sect571k1,sect571r1,secp256k1,prime256v1,secp384r1,secp521r1
   2     ECDHE-RSA-AES256-SHA384      TLSv1.2                ECDH,B-571,570bits  sect283k1,sect283r1,sect409k1,sect409r1,sect571k1,sect571r1,secp256k1,prime256v1,secp384r1,secp521r1
   3     ECDHE-RSA-AES256-SHA         TLSv1,TLSv1.1,TLSv1.2  ECDH,B-571,570bits  sect283k1,sect283r1,sect409k1,sect409r1,sect571k1,sect571r1,secp256k1,prime256v1,secp384r1,secp521r1
   4     DHE-RSA-AES256-GCM-SHA384    TLSv1.2                DH,4096bits         None
   5     DHE-RSA-AES256-SHA256        TLSv1.2                DH,4096bits         None
   6     DHE-RSA-AES256-SHA           TLSv1,TLSv1.1,TLSv1.2  DH,4096bits         None
   7     ECDHE-RSA-AES128-GCM-SHA256  TLSv1.2                ECDH,B-571,570bits  sect283k1,sect283r1,sect409k1,sect409r1,sect571k1,sect571r1,secp256k1,prime256v1
   8     ECDHE-RSA-AES128-SHA256      TLSv1.2                ECDH,B-571,570bits  sect283k1,sect283r1,sect409k1,sect409r1,sect571k1,sect571r1,secp256k1,prime256v1,secp384r1,secp521r1
   9     ECDHE-RSA-AES128-SHA         TLSv1,TLSv1.1,TLSv1.2  ECDH,B-571,570bits  sect283k1,sect283r1,sect409k1,sect409r1,sect571k1,sect571r1,secp256k1,prime256v1,secp384r1,secp521r1
   10    DHE-RSA-AES128-GCM-SHA256    TLSv1.2                DH,4096bits         None
   11    DHE-RSA-AES128-SHA256        TLSv1.2                DH,4096bits         None
   12    DHE-RSA-AES128-SHA           TLSv1,TLSv1.1,TLSv1.2  DH,4096bits         None

   Certificate: trusted, 2048 bits, sha256WithRSAEncryption signature
   TLS ticket lifetime hint: None
   NPN protocols: None
   OCSP stapling: not supported
   Cipher ordering: client
   Curves ordering: client - fallback: no
   Server supports secure renegotiation
   Server supported compression methods: NONE
   TLS Tolerance: yes

TLS 1.3 compatibility
^^^^^^^^^^^^^^^^^^^^^^^

Due to compatibility issues, TLS 1.3 is currently (version 8.1.x) disabled by default. It can be enabled by setting property ``tls-disable-tls13`` of ``sslContextContainer`` bean to ``false``:

::

   sslContextContainer () {
       'tls-disable-tls13' = false
   }
