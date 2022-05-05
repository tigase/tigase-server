Security
========

The articles here cover advanced security features built into to Tigase Server, and some options for adding your own levels of security.

XEP-0191: Blocking Command
-------------------------------

The simplest security feature, however, inside an XMPP server is the ability to block users and JIDS. `XEP-0191 <http://xmpp.org/extensions/xep-0191>`__ specifies the parameters of simple blocking without using privacy lists. Below is a breakdown and some sample commands you may find helpful. To enable this feature, be sure the following is in your ``config.tdsl`` file:

::

   }
   'sess-man' {
       'urn:xmpp:blocking' () {}
   }

If you have other plugins running, then just add ``'urn:xmpp:blocking' () {}`` to the list to activate this feature.

To confirm if your installation of Tigase supports this feature, a quick disco#info of your server should reveal the following feature:

::

   <feature var='urn:xmpp:blocking'/>

Blocked users are stored on the server on a per-JID basis, so one user may only see his or her blocked JIDs. Lists of blocked JIDs will return as an IQ stanza with a list of <item> fields. To retrieve the blocklist, the following command is issued:

.. code:: xml

   <iq type='get' id='blockedjids'>
     <blocklist xmlns='urn:xmpp:blocking'/>
   </iq>

The server responds:

.. code:: xml

   <iq type='result' id='blockedjids'>
     <blocklist xmlns='urn:xmpp:blocking'>
       <item jid='user1@domain.net'/>
       <item jid='admin@example.com'/>
     </blocklist>
   </iq>

To block a JID, a similar stanza to the one above is sent to the server with the items of the blocked JIDs you wish to add:

.. code:: xml

   <iq from='admin@domain.net' type='set' id='block'>
     <block xmlns='urn:xmpp:blocking'>
       <item jid='user2@domain.net'/>
     </block>
   </iq>

The server will then push an unavailable presence to blocked contacts. Communication between a contact that is blocked, and an entity that blocked it will result in a <not-acceptable> error:

.. code:: xml

   <message type='error' from='user2@domain.net' to='admin@domain.net'>
     <body>Hello, are you online?</body>
     <error type='cancel'>
       <not-acceptable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>
       <blocked xmlns='urn:xmpp:blocking:errors'/>
     </error>
   </message>

Unblocking a contact is just as easy as blocking, send an unblock stanza to the server:

.. code:: xml

   <iq from='admin@domain.net' type='set' id='unblock'>
     <unblock xmlns='urn:xmpp:blocking'>
       <item jid='user2@domain.net'/>
     </unblock>
   </iq>

The server will begin pushing presence information to unblocked contacts and resources so long as permissions have not changed between.

You may also opt to unblock all contacts and essentially clear out your blocked list using the following command:

.. code:: xml

   <iq type='set' id='unblockall'>
     <unblock xmlns='urn:xmpp:blocking'/>
   </iq>

Account Registration Limits
--------------------------------

In order to protect Tigase servers from DOS attacks, a limit on number of account registrations per second has been implemented. This may be configured by adding the following line in the ``config.tdsl`` file:

.. code:: dsl

   'registration-throttling' () {
       limit = 10

This setting allows for 10 registrations from a single IP per second. If the limit is exceeded, a ``NOT_ALLOWED`` error will be returned.

It is possible to create two separate counters as well:

.. code:: dsl

   'registration-throttling' () {
       limit = 10
   }
   c2s {
       'registration-throttling' (class: tigase.server.xmppclient.RegistrationThrottling) {
           limit = 3
       }
   }

Here we have one for c2s with a limit of 3, and a global for all other connection managers set at 10.

You can also set individual components limits as well:

.. code:: dsl

   ws2s {
       'registration-throttling' (class: tigase.server.xmppclient.RegistrationThrottling) {
           limit = 7
       }
   }

Brute-force attack prevention
---------------------------------

Brute-force Prevention is designed to protect Tigase Server against user password guessing. It counts invalid login tries and when it is above limit, it locks login ability for specific time (soft ban). When invalid login counter reaches second level, account will be disabled permanently.

Configuration
^^^^^^^^^^^^^^^^^

Brute-force Prevention is configured by VHost. There is following lis of configuration parameters:

+-------------------------------------+-------------+---------------------------------------------------------------------------+
| ``brute-force-lock-enabled``        | ``boolean`` | Brute Force Prevention Enabled                                            |
+-------------------------------------+-------------+---------------------------------------------------------------------------+
| ``brute-force-lock-after-fails``    | ``long``    | Number of allowed invalid login                                           |
+-------------------------------------+-------------+---------------------------------------------------------------------------+
| ``brute-force-period-time``         | ``long``    | Time [sec] in what failed login tries are counted                         |
+-------------------------------------+-------------+---------------------------------------------------------------------------+
| ``brute-force-disable-after-fails`` | ``long``    | Threshold beyond which account will be permanently disabled               |
+-------------------------------------+-------------+---------------------------------------------------------------------------+
| ``brute-force-lock-time``           | ``long``    | Time [sec] of soft ban (first threshold)                                  |
+-------------------------------------+-------------+---------------------------------------------------------------------------+
| ``brute-force-mode``                | ``string``  | Working mode (see `Working modes <#bruteForcePrevention_WorkingModes>`__) |
+-------------------------------------+-------------+---------------------------------------------------------------------------+

Detailed statistics
~~~~~~~~~~~~~~~~~~~~~~~

By default, in order not to pollute statistics, Brute-Force locker will only provide details about number of locker IPs and JIDs (and total number of locked attempts). In order to have detailed information about IPs and JIDs that has been locked in statistics you should use following configuration:

::

   'sess-man' () {
       'brute-force-locker' () {
           detailedStatistics = false
       }
   }

Working modes
~~~~~~~~~~~~~~~~~~~~~~~

There are three working modes:

-  ``Ip`` - it counts invalid login tries from IP, and locks login ability (soft ban) for IP what reach the threshold

-  ``IpJid`` - it counts tries from IP to specific user account. Soft ban locks ability of login to specific JID from specific IP.

-  ``Jid``- similar to ``IpJid`` but checks only JID. Soft ban locks ability of login to specific JID from all IPs.

.. **Note**::

   Only in modes ``Jid`` and ``IpJid`` account may be permanently disabled.

Permanent ban
~~~~~~~~~~~~~~~~~~~~~~~

In modes ``Jid`` and ``IpJid``, when invalid login counter reach threshold ``brute-force-disable-after-fails``, account status will be set o ``disabled``. To enable it again you should use `Re-Enable User <https://xmpp.org/extensions/xep-0133.html#reenable-users>`__ Ad-hoc Command.

Server Certificates
---------------------

-  :ref:`Creating and Loading the Server Certificate in pem Files <certspem>`

.. _certspem:

Creating and Loading the Server Certificate in pem Files
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Server Certificates
~~~~~~~~~~~~~~~~~~~~~

Server certificates are needed when you use secure socket connections - SSL/TLS.

For secure socket connection a proper certificate is needed. You can either generate your own self-signed certificate or obtain certificate from trusted third party organization.

Here are steps how to obtain certificate from a trusted organization.

Generating your Own Certificates

Self-signed certificates can be generated easily on a Linux system. Although it may not be considered a 'trusted' certificate authority, it can be useful to test server installations. **We do not recommend regular use of self-signed certificates**.

.. Note:: 

   that Tigase v5.0 and later can automatically create self signed PEM files if needed. However we will cover doing this process by hand.

This tutorial assumes you are running a Linux-based operating system with access to command shell, and the 'Openssl' package is installed on the system.

| The process takes the following steps:
| 1. Create a local private key. This file ends with .key extension. It is recommended to create a new private key for the process.
| 2. Generate a certificate request. This file ends with the .csr extension and is the file sent to the Certificate Authority to be signed.
| 3. CA signs private key. This can be done by your own computer, but can also be done by private CAs for a fee.
| 4. Results are obtained from the CA. This is a ``.crt`` file which contains a separate public certificate.
| 5. Combine the ``.csr`` and ``.crt`` file into a unified ``.pem`` file. Tigase requires keys to be non-password protected PEM files.

**Generate local private key.**

.. code:: sh

   openssl genrsa -out [domain.com.key] 1024

This command generates a private key using a 1024 bit RSA algorithm. ``-out`` designates the name of the file, in this case it will be ``domain.com.key``. The exact name is not important, and the file will be created in whatever directory you are currently in.

**Generate a certificate request:.**

.. code:: sh

   openssl req -nodes -key domain.com.key -out domain.com.csr

This command generates a certificate request using the file specified after ``-key``, and the result file will be ``domain.com.csr``. You will be asked a series of questions to generate the request.

.. code:: sh

   Country Name (2 letter code) [AU]:AU
   State or Province Name (full name) [Some-State]:Somestate
   Locality Name (eg, city) []:Your city name
   Organization Name (eg, company) [Internet Widgits Pty Ltd]:Company name
   Organizational Unit Name (eg, section) []:Department or any unit
   Common Name (eg, YOUR name) []:*.yourdomain.com
   Email Address []:your_email_address@somedomain.com

   Please enter the following 'extra' attributes
   to be sent with your certificate request
   A challenge password []:
   An optional company name []:

**Sign the Certificate Request:.**

Now the .csr file will be signed by a Certificate Authority. In this tutorial, we will be self-signging our certificate. This practice however is generally not recommended, and you will receive notifications that your certificate is not trusted. There are commercial offers from companies to sign your certificate from trusted sources. Please see the `Certificate From Other Providers <#OtherSources>`__ section for more information.

.. code:: bash

   openssl x509 -req -days 365 -in domain.com.csr -signkey domain.com.key -out domain.com.crt

This command signs the certificate for 365 days and generates the ``domain.com.crt`` file. You can, of course use any number of days you like.

**Generate PEM file.**

You should now have the following files in the working directory: ..\\ domain.com.key domain.com.csr domain.com.crt

.. code:: sh

   cat yourdomain.com.cert.pem intermediate.cert.pem root.cert.pem > yourdomain.com.pem

If the certificate is issued by third-party authority you will have to attach the certificate chain, that being certificate of the authority who has generated your certificate. You normally need to obtain certificates for your chain from the authority who has generated your certificate.

The result file should looks similar to:

.. code:: sh

   -----BEGIN CERTIFICATE-----
   MIIG/TCCBeWgAwIBAgIDAOwZMA0GCSqGSIb3DQEBBQUAMIGMMQswCQYDVQQGEwJJ
   .
   .
   .
   pSLqw/PmSLSmUNIr8yQnhy4=
   -----END CERTIFICATE-----
   -----BEGIN RSA PRIVATE KEY-----
   WW91J3JlIGtpZGRpbmchISEKSSBkb24ndCBzaG93IHlvdSBvdXIgcHJpdmF0ZSBr
   .
   .
   .
   ZXkhISEhCkNyZWF0ZSB5b3VyIG93biA7KSA7KSA7KQo=
   -----END RSA PRIVATE KEY-----
   -----BEGIN CERTIFICATE-----
   MIIHyTCCBbGgAwIBAgIBATANBgkqhkiG9w0BAQUFADB9MQswCQYDVQQGEwJJTDEW
   .
   .
   .
   xV/stleh
   -----END CERTIFICATE-----

For Tigase server as well as many other servers (Apache 2.x), the order is following; your domain certificate, your private key, authority issuing your certificate, root certificate.

.. NOTE::

   Tigase requires full certificate chain in PEM file (described above)! Different applications may require pem file with certificates and private key in different order. So the same file may not be necessarily used by other services like Web server or e-mail server. Currently, Tigase can automatically sort certificates in PEM file while loading it.**

Installing/Loading Certificate To the Tigase Server
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Installing and loading certificates is very easy. The server can load all certificates directly from **pem** files. You just need to create a separate pem file for each of your virtual domains and put the file in a directory accessible by the server. Tigase server can automatically load all **pem** files found in given directory. By default, and to make things easy, we recommend the ``Tigase/certs`` directory.

It’s also possible to use: \* Admin ad-hoc command via XMPP client - you should navigate to Service Discovery of your server and in the list of commands for ``VHost Manager`` component select ``Add SSL Certificate`` and then follow instructions \* Admin WebUI - open ``http://<host>/admin``, navigate to ``Other`` category and in it select ``Add SSL Certificate`` and then follow instructions \* REST API - make a ``POST`` request to ``http://localhost:8080/rest/adhoc/vhost-man@domain.com`` with payload containing your certificate; to get the required form fields make ``GET`` request to the same endpoint

Certificate From Other Providers
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

There is number of certificate providers offering certificates either for free or for money. You can use any of them, however you have to be aware that sometimes certificates might not be recognized by all XMPP servers, especially if it’s one from a new provider. Here is an example list of providers:

-  LetsEncrypt - please see `??? <#LetsEncryptCertificate>`__ for details

-  `CAcert <https://www.cacert.org/>`__ - free certificates with Web GUI. (WARNING: it’s not widely accepted)

-  `Verisign <https://www.verisign.com/>`__ - very expensive certificates comparing to above provides but the provider is recognized by everybody. If you have a certificate from Verisign you can be sure it is identified as a valid certificate.

-  `Comodo Certificate Authority <http://www.comodo.com/business-security/digital-certificates/ssl-certificates.php>`__ offers different kind of commercial certificates

To obtain certificate from a third party authority you have to go to its website and request the certificate using certificate request generated above. I cannot provide any instructions for this as each of the providers listed have different requirements and interfaces.

We **highly** recommend using LetsEncrypt keys to self-sign and secure your domain. Instructions are in the `next section <#LetsEncryptCertificate>`__.

Using one certificate for multiple domains
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. Note::

   Tigase tries to be *smart* and automatically detects wildcard domain and alternative domains so it’s not needed to duplicate same certificate in multiple files to match domains - same file will be loaded and make available for all domains (CNames) available in the certificate.

Installing LetsEncrypt Certificates in Your Linux System
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

LetsEncrypt is a trusted CA that provides free security certificates. Unlike previously self-signed certificates, we can use LetsEncrypt Certificates to certify your domains from a trusted source.

Please refer to official `certbot User Guide <https://certbot.eff.org/docs/using.html>`__ for details how to install and operate the tool, choosing desired method of domain authentication (DNS or webserver). After successful execution the certificate with all related files will be stored under ``/etc/letsencrypt/live/$domain``

.. code:: bash

   $ sudo ls  /etc/letsencrypt/live/$domain
   cert.pem  chain.pem  fullchain.pem  privkey.pem  README

In that directory, you will find four files:

-  ``privkey.pem`` - private key for the certificate

-  ``cert.pem`` - contains the server certificate by itself

-  ``chain.pem`` - contains the additional intermediate certificate or certificates

-  ``fullchain.pem`` - all certificates, including server certificate (aka leaf certificate or end-entity certificate). The server certificate is the first one in this file, followed by any intermediates.

For Tigase XMPP Server, we are only concerned with ``privkey.pem`` and ``fullchain.pem`` (or ``chain.pem`` - please consider actual issuers and certification chain!).

At this point we will need to obtain the root and intermediate certificates, this can be done by downloading these certificates from the `LetsEncrypt Chain of Trust website <https://letsencrypt.org/certificates/>`__.

.. Note::

   Please pay utmost attention to the actual certificate issuers and make sure that the certification chain is maintained!

On the time of the writing, LetsEncrypt was providing domain certificates issued by ``R3`` CertificateAuthorigy (CA). In order to provide complete chain to the root CA you should get Let’s Encrypt R3 (``RSA 2048, O = Let’s Encrypt, CN = R3``) certificate. Depending on desired certification chain you have two options: 1) (default and recommended) using own LetsEncrypt CA: a) ``R3`` certificate signed by ISRG Root X1: https://letsencrypt.org/certs/lets-encrypt-r3.pem b) ``ISRG Root X1`` root certificate: https://letsencrypt.org/certs/isrgrootx1.pem 2) (legacy, option more compatible with old systems): cross-signed certificate by IdenTrust: a) ``R3`` certificate cross-signed by IdenTrust: https://letsencrypt.org/certs/lets-encrypt-r3-cross-signed.pem b) ``TrustID X3 Root`` from IdenTrust: https://letsencrypt.org/certs/trustid-x3-root.pem.txt

Considering first (recommended) option, you may obtain them using wget:

.. code:: bash

   wget https://letsencrypt.org/certs/isrgrootx1.pem
   wget https://letsencrypt.org/certs/lets-encrypt-r3.pem

These are the root certificate, and the intermediate certificate signed by root certificate.

.. Note::

   IdenTrust cross-signed certificate will not function properly in the future!

Take the contents of your ``privkey.pem``, certificate, and combine them with the contents of ``isrgrootx1.pem`` and ``lets-encrypt-r3.pem`` into a single pem certificate.

Depending on your configuration you either need to name the file after your domain such as ``mydomain.com.pem`` and place it under ``certs/`` subdirectory of Tigase XMPP Server installation or update it using admin ad-hoc (see `??? <#certificateStorage>`__)

If you moved all certs to a single directory, you may combine them using the following command under \*nix operating systems:.

.. code:: bash

   cat ./cert.pem ./privkey.pem ./lets-encrypt-r3.pem ./isrgrootx1.pem > mydomain.com.pem


.. Note::

   If you are using ``isrgrootx1`` root make sure you use ``cert.pem`` file instead of ``fullchain.pem``, which uses different intermediate certificate ( `Let’s Encrypt Authority X3 (IdenTrust cross-signed) <https://letsencrypt.org/certs/lets-encrypt-x3-cross-signed.pem.txt>`__ ) and you will have to use `DST Root CA X3 <https://letsencrypt.org/certs/trustid-x3-root.pem.txt>`__ certificate!

Your certificate should look something like this:

.. code:: certificate

   -----BEGIN PRIVATE KEY-----
   MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDAUAqqKu7Z4odo
   ...
   og89F9AbWr1mNmyRoScyqMXo
   -----END PRIVATE KEY-----
   -----BEGIN CERTIFICATE-----
   cmNoIEdyb3VwMRUwEwYDVQQDEwxJU1JHIFJvb3QgWDEwHhcNMTUwNjA0MTEwNDM4
   ...
   TzELMAkGA1UEBhMCVVMxKTAnBgNVBAoTIEludGVybmV0IFNlY3VyaXR5IFJlc2Vh
   -----END CERTIFICATE-----
   -----BEGIN CERTIFICATE-----
   FhpodHRwOi8vY3BzLmxldHNlbmNyeXB0Lm9yZzCBqwYIKwYBBQUHAgIwgZ4MgZtU
   ...
   bmcgUGFydGllcyBhbmQgb25seSBpbiBhY2NvcmRhbmNlIHdpdGggdGhlIENlcnRp
   -----END CERTIFICATE-----

.. Warning::

    LetsEncrypt certificates expire 90 days from issue and need to be renewed in order for them to remain valid!

You can check your certificate with utility class:

::

   java -cp <path_to_tigase-server_installation>/jars/tigase-utils.jar tigase.cert.CertificateUtil -lc mydomain.com.pem -simple

Let’s encrypt and DNS verification
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The only way to obtain wildcard (``*.domain.com``) certificate is via DNS verification. Certbot support a number of DNS operators - you can check if your DNS provider is listed by executing ``$ certbot plugins``

AWS Route53

If you want to use it with Amazon Cloud you should install plugin for AWS:

::

   pip install certbot-dns-route53

.. Note::

   If you are using certbot under macOS and you installed it via brew then you should use: ``$( brew --prefix certbot )/libexec/bin/pip install certbot-dns-route53``

You should store your credentials in ``~/.aws/credentials`` (you may want to create dedicated policy for updating DNS as described in `plugin’s documentation <https://certbot-dns-route53.readthedocs.io/en/stable/>`__:

.. code:: bash

   [default]
   aws_access_key_id = <key_id>
   aws_secret_access_key = <key>

And afterward you should execute ``certbot`` with ``--dns-route53`` parameter

Certbot update hook and Tigase API
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

For greater automation it’s possible to automate updating certificate obtained with ``certbot`` in Tigase XMPP Server. You should use following deploy hook - either add it to ``/etc/letsencrypt/renewal-hooks/deploy/`` or use it directly in ``certboot`` commandline with ``--deploy-hook`` parameter (in the latter case, it will be added to particular domain configuration so it’s not necessary to specify UPDATE_DOMAINS).

.. Note::

   Please adjust account credentials used for deployment (``USER``, ``PASS``, ``DOMAIN``) as well as paths to Let’s Encrypt certificates (*ISRG Root X1* named ``isrgrootx1.pem`` and *Let’s Encrypt Authority X3* named ``letsencryptauthorityx3.pem``)

.. code:: bash

   #!/bin/bash

   set -e

   ## Configuration START

   USER="admin_username"
   PASS="admin_password"
   DOMAIN="my_domain.tld"
   HOST=${DOMAIN}
   #UPDATE_DOMAINS=(${DOMAIN})
   # PORT=":8080"
   # APIKEY="?api-key=mySecretKey"
   LE_CERTS_PATH="/path/to/letsencrypt/CA/certificates/"

   ## Configuration END

   fail_count=0

   for domain in ${RENEWED_DOMAINS[@]}; do
       if [[ $domain == "*."* ]]; then
           CERT_DOMAIN=${domain#*\*.}
       else
           CERT_DOMAIN=${domain}
       fi

       if [[ ! -z "${UPDATE_DOMAINS}" ]] ; then
           match=0
           for dn in "${UPDATE_DOMAINS[@]}"; do
               if [[ $dn = "$CERT_DOMAIN" ]]; then
                   match=1
                   break
               fi
           done
           if [[ $match = 0 ]]; then
               echo "Skipping updating ${domain} because it's not in the list of supported domains: ${UPDATE_DOMAINS[@]}"
               continue
           fi
       fi

       CERT=`cat "$RENEWED_LINEAGE/cert.pem" "$RENEWED_LINEAGE/privkey.pem" ${LE_CERTS_PATH}/isrgrootx1.pem ${LE_CERTS_PATH}/letsencryptauthorityx3.pem`

       REQUEST="
       <command>
         <node>ssl-certificate-add</node>
         <fields>
           <item>
             <var>Certificate in PEM format</var>
             <value>${CERT}</value>
           </item>
           <item>
             <var>command-marker</var>
             <value>command-marker</value>
           </item>
           <item>
             <var>VHost</var>
             <value>${CERT_DOMAIN}</value>
           </item>
           <item>
             <var>Save to disk</var>
             <value>true</value>
           </item>
         </fields>
       </command>"

       response=`curl -s -L -H "Content-Type: text/xml" -X POST  http://${USER}%40${DOMAIN}:${PASS}@${HOST}${PORT}/rest/adhoc/vhost-man@${DOMAIN}${APIKEY} -d "${REQUEST}"`

       if [[ ! ${response} = *"loaded successfully"* ]] ; then
           echo -e "Server returned error while updating   ${domain}   certificate:\n ${response}"
           fail_count=$((${fail_count}+1))
       else
           echo "Correctly updated ${domain} certificate"
       fi
   done

   exit ${fail_count}

.. Note::

   If you are not using wildcard certificate when you have to provide certificate for main domain as well as certificates for subdomains that mach all components that you want to expose (muc, pubsub, push, etc…)

Storing and managing certificates
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Filesystem
~~~~~~~~~~~~~~

By default Tigase loads and stores certificates in ``certs/`` subdirectory. Each *domain* certificate should be stored in a file which filename consists of domain name and ``.pem`` extension, i.e. ``<domain>.pem``. For example for domain tigase.net it would be ``certs/tigase.net.pem``.

.. Note::

   Tigase tries to be *smart* and automatically detects wildcard domain and alternative domains so it’s not needed to duplicate same certificate in multiple files to match domains.

Database repository
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Alternatively it’s possible to use database as a storage for the certificates. Upon enabling it certificates won’t be read nor stored to the filesystem. You can enable it by adding ``repository () {}`` bean to ``'certificate-container' () {}`` in your TDSL configuration file:

::

   'certificate-container' () {
       repository () {}
   }

If you are using database repository then you manage/update certificates using either ad-hoc command ``Add SSL certificate`` from *VHost Manager* or via HTTP REST API.

Custom Authentication Connectors
-------------------------------------

This article presents configuration options available to the administrator and describe how to set Tigase server up to use user accounts data from a different database.

The first thing to know is that Tigase server always opens 2 separate connections to the database. One connection is used for user login data and the other is for all other user data like the user roster, vCard, private data storage, privacy lists and so on…​

In this article we still assume that Tigase server keeps user data in it’s own database and only login data is retrieved from the external database.

At the moment Tigase offers following authentication connectors:

-  ``mysql``, ``pgsql``, ``derby`` - standard authentication connector used to load user login data from the main user database used by the Tigase server. In fact the same physical implementation is used for all JDBC databases.

-  ``drupal`` - is the authentication connector used to integrate the Tigase server with `Drupal CMS <http://drupal.org/>`__.

-  ``tigase-custom`` - is the authentication connector which can be used with any database. Unlike the 'tigase-auth' connector it allows you to define SQL queries in the configuration file. The advantage of this implementation is that you don’t have to touch your database. You can use either simple plain SQL queries or stored procedures. The configuration is more difficult as you have to enter carefully all SQL queries in the config file and changing the query usually involves restarting the server. For more details about this implementation and all configuration parameters please refer to `Tigase Custom Auth documentation <#custonAuthConnector>`__.

-  ``tigase-auth`` (**DEPRECATED**) - is the authentication connector which can be used with any database. It executes stored procedures to perform all actions. Therefore it is a very convenient way to integrate the server with an external database if you don’t want to expose the database structure. You just have to provide a set of stored procedures in the database. While implementing all stored procedures expected by the server might be a bit of work it allows you to hide the database structure and change the SP implementation at any time. You can add more actions on user login/logout without restarting or touching the server. And the configuration on the server side is very simple. For detailed description of this implementation please refer to `Tigase Auth documentation <#tigaseAuthConnector>`__.

As always the simplest way to configure the server is through the ``config.tdsl`` file. In the article describing this file you can find long list with all available options and all details how to handle it. For the authentication connector setup however we only need 2 options:

.. code:: dsl

   dataSource {
       'default-auth' () {
           uri = 'database connection url'
       }
   }
   authRepository {
       default () {
           cls = 'connector'
           'data-source' = 'default-auth'
       }
   }

For example if you store authentication data in a ``drupal`` database on ``localhost`` your settings would be:

.. code:: dsl

   dataSource {
       'default-auth' () {
           uri = 'jdbc:mysql://localhost/drupal?user=user&password=passwd'
       }
   }
   authRepository {
       default () {
           'data-source' = 'default-auth'
       }
   }

You have to use a class name if you want to attach your own authentication connector.

Default is:

.. code:: dsl

   authRepository {
       default {
           cls = 'tigase.db.jdbc.TigaseAuth'
       }
   }

In the same exact way you can setup connector for any different database type.

For example, drupal configuration is below

.. code:: dsl

   authRepository {
       default {
           cls = 'tigase.db.jdbc.DrupalWPAuth'
       }
   }

Or tigase-custom authentication connector.

.. code:: dsl

   authRepository {
       default {
           cls = 'tigase.db.jdbc.TigaseCustomAuth'
       }
   }

The different ``cls`` or classes are:

-  Drupal - ``tigase.db.jdbc.DrupalWPAuth``

-  MySQL, Derby, PostgreSQL, MS SQL Server - ``tigase.db.jdbc.JDBCRepository``

You can normally skip configuring connectors for the default Tigase database format: ``mysql``, ``pgsql`` and ``derby``, ``sqlserver`` as they are applied automatically if the parameter is missing.

One more important thing to know is that you will have to modify ``authRepository`` if you use a custom authentication connector. This is because if you retrieve user login data from the external database this external database is usually managed by an external system. User accounts are added without notifying Tigase server. Then, when the user logs in and tries to retrieve the user roster, the server can not find such a user in the roster database.

.. Important::

   To keep user accounts in sync between the authentication database and the main user database you have to add following option to the end of the database connection URL: ``autoCreateUser=true``.

For example:

.. code:: dsl

   dataSource {
       default () {
           uri = 'jdbc:mysql://localhost/tigasedb?user=nobody&password=pass&autoCreateUser=true'
       }
   }

If you are interested in even further customizing your authentication connector by writing your own queries or stored procedures, please have a look at the following guides:

- :ref:`Tigase Auth guide <tigaseAuthConnector>`

- :ref:`Tigase Custom Auth guide <custonAuthConnector>`

.. _tigaseAuthConnector:

Tigase Auth Connector (DEPRECATED)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

.. **Warning**::

    Tigase Auth connector is **DEPRECATED** as of version 8.0.0 and will be removed in future releases

The Tigase Auth connector with shortcut name: **tigase-auth** is implemented in the class: `tigase.db.jdbc.TigaseAuth <https://github.com/tigase/tigase-server/blob/master/src/main/java/tigase/db/jdbc/TigaseAuth.java>`__. It allows you to connect to any external database to perform user authentication. You can find more details how to setup a custom connector in the `Custom Authentication Connectors <#customAuthentication>`__ guide.

To make this connector working you have to prepare your database to offer set of stored procedures for Tigase server to perform all the authentication actions. The best description is the example schema with all the stored procedures defined - please refer to the Tigase repositories for the schema definition files (each component has it’s dedicated schema). For example:

-  `tigase-server <https://github.com/tigase/tigase-server/tree/master/src/main/database>`__

-  `tigase-pubsub <https://github.com/tigase/tigase-pubsub/tree/master/src/main/database>`__

-  `tigase-muc <https://github.com/tigase/tigase-muc/tree/master/src/main/database>`__

-  `tigase-message-archiving <https://github.com/tigase/tigase-message-archiving/tree/master/src/main/database>`__

-  `tigase-socks5 <https://github.com/tigase/tigase-socks5/tree/master/src/main/database>`__

The absolute minimum of stored procedures you have to implement is:

-  ``TigUserLoginPlainPw`` - to perform user authentication. The procedure is always called when the user tries to login to the XMPP server. This is the only procedure which must be implemented and actually must work.

-  ``TigUserLogout`` - to perform user logout. The procedure is always called when the user logouts or disconnects from the server. This procedure must be implemented but it can be empty and can do nothing. It just needs to exist because Tigase expect it to exist and attempts to call it.

With these 2 above stored procedures you can only perform user login/logouts on the external database. You can’t register a user account, change user password or remove the user. In many cases this is fine as all the user management is handled by the external system.

If you however want to allow for account management via XMPP you have to implement also following procedures:

-  ``TigAddUserPlainPw`` - to add a new user account

-  ``TigRemoveUser`` - to remove existing user account

-  ``TigUpdatePasswordPlainPw`` - to change a user password for existing account

.. _custonAuthConnector:

Tigase Custom Auth Connector
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The Tigase Custom Auth connector with shortcut name: **tigase-custom** is implemented in the class: `tigase.db.jdbc.TigaseCustomAuth <https://github.com/tigase/tigase-server/tree/master/src/main/java/tigase/db/jdbc/TigaseCustomAuth.java>`__. It allows you to connect to any external database to perform user authentication and use a custom queries for all actions.

You can find more details how to setup a custom connector in the Custom Authentication Connectors guide.

The basic configuration is very simple:

.. code:: bash

   authRepository {
       default () {
           cls = 'tigase.db.jdbc.TigaseCustomAuth'
           'data-source' = 'default-auth'
       }
   }

That’s it.

The connector loads correctly and starts working using predefined, default list of queries. In most cases you also might want to define your own queries in the configuration file. The shortest possible description is the following example of the content from the ``config.tdsl`` file:

This query is used to check connection to the database, whether it is still alive or not

.. code:: dsl

   authRepository {
       default () {
           'conn-valid-query' = 'select 1'
       }
   }

This is database initialization query, normally we do not use it, especially in clustered environment

.. code:: dsl

   authRepository {
       default () {
           'init-db-query' = 'update tig_users set online_status = 0'
       }
   }

.. Note::

   ``online_status`` column does not exist and would need to be added for that query to work.

Below query performs user authentication on the database level. The Tigase server does not need to know authentication algorithm or password encoding type, it simply passes user id (BareJID) and password in form which was received from the client, to the stored procedure. If the authentication was successful the procedure returns user bare JID or null otherwise. Tigase checks whether the JID returned from the query matches JID passed as a parameter. If they match, the authentication is successful.

.. code:: dsl

   authRepository {
       default () {
           'user-login-query' = '{ call TigUserLoginPlainPw(?, ?) }'
       }
   }


.. Note::

   ``TigUserLoginPlainPw`` is no longer part of a Tigase XMPP Server database schema and would need to be created.

Below query returns number of user accounts in the database, this is mainly used for the server metrics and monitoring components.

.. code:: dsl

   authRepository {
       default () {
         'users-count-query' = '{ call TigAllUsersCount() }'
       }
   }

The Below query is used to add a new user account to the database.

.. code:: dsl

   authRepository {
       default () {
           'add-user-query' = '{call TigAddUserPlainPw(?, ?) }'
       }
   }

Below query is used to remove existing account with all user’s data from the database.

.. code:: dsl

   authRepository {
       default () {
           'del-user-query' = '{ call TigRemoveUser(?) }'
       }
   }

This query is used for the user authentication if ``user-login-query`` is not defined, that is if there is no database level user authentication algorithm available. In such a case the Tigase server loads user’s password from the database and compares it with data received from the client.

.. code:: dsl

   authRepository {
       default () {
           'get-password-query' = 'select user_pw from tig_users where user_id = ?'
       }
   }

Below query is used for user password update in case user decides to change his password.

.. code:: dsl

   authRepository {
       default () {
           'update-password-query' = 'update tig_users set user_pw = ? where user_id = ?'
       }
   }

This query is called on user logout event. Usually we use a stored procedure which records user logout time and marks user as offline in the database.

.. code:: dsl

   authRepository {
       default () {
           'update-logout-query' = 'update tig_users, set online_status = online_status - 1 where user_id = ?'
       }
   }


.. Note::

   ``online_status`` column does not exist and would need to be added for that query to work.

This configuration specifies what non-sasl authentication mechanisms to expose to the client

.. code:: dsl

   authRepository {
       default () {
           'non-sasl-mechs' = [ 'password', 'digest' ]
       }
   }

This setting to specify what sasl authentication mechanisms expose to the client

.. code:: dsl

   authRepository {
       default () {
           'sasl-mechs' = 'PLAIN,DIGEST-MD5'
       }
   }

Queries are defined in the configuration file and they can be either plain SQL queries or stored procedures. If the query starts with characters: ``{ call`` then the server assumes this is a stored procedure call, otherwise it is executed as a plain SQL query. Each configuration value is stripped from white characters on both ends before processing.

Please don’t use semicolon ``;`` at the end of the query as many JDBC drivers get confused and the query may not work.

Some queries can take arguments. Arguments are marked by question marks ``?`` in the query. Refer to the configuration parameters description for more details about what parameters are expected in each query.

This example shows how to use a stored procedure to add a user as a query with 2 required parameters (username, and password).

.. code:: dsl

   authRepository {
       default () {
           'add-user-query' = '{call TigAddUserPlainPw(?, ?) }'
       }
   }

The same query with plain SQL parameters instead:

.. code:: dsl

   'add-user-query' = 'insert into users (user_id, password) values (?, ?)'

The order of the query arguments is important and must be exactly as described in specification for each parameter.

+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------+-------------------------------------------------------------------------------+
| Query Name                | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               | Arguments                                        | Example Query                                                                 |
+===========================+===========================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================================+==================================================+===============================================================================+
| ``conn-valid-query``      | Query executed periodically to ensure active connection with the database.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | Takes no arguments.                              | ``select 1``                                                                  |
+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------+-------------------------------------------------------------------------------+
| ``init-db-query``         | Database initialization query which is run after the server is started.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | Takes no arguments.                              | ``update tig_users set online_status = 0``                                    |
+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------+-------------------------------------------------------------------------------+
| ``add-user-query``        | Query adding a new user to the database.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | Takes 2 arguments: ``(user_id (JID), password)`` | ``insert into tig_users (user_id, user_pw) values (?, ?)``                    |
+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------+-------------------------------------------------------------------------------+
| ``del-user-query``        | Removes a user from the database.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | Takes 1 argument: ``(user_id (JID))``            | ``delete from tig_users where user_id = ?``                                   |
+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------+-------------------------------------------------------------------------------+
| ``get-password-query``    | Retrieves user password from the database for given user_id (JID).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        | Takes 1 argument: ``(user_id (JID))``            | ``select user_pw from tig_users where user_id = ?``                           |
+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------+-------------------------------------------------------------------------------+
| ``update-password-query`` | Updates (changes) password for a given user_id (JID).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     | Takes 2 arguments: ``(password, user_id (JID))`` | ``update tig_users set user_pw = ? where user_id = ?``                        |
+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------+-------------------------------------------------------------------------------+
| ``user-login-query``      | Performs user login. Normally used when there is a special SP used for this purpose. This is an alternative way to a method requiring retrieving user password. Therefore at least one of those queries must be defined: ``user-login-query`` or ``get-password-query``. If both queries are defined then ``user-login-query`` is used. Normally this method should be only used with plain text password authentication or sasl-plain. Tigase expects a result set with user_id to be returned from the query if login is successful and empty results set if the login is unsuccessful. | Takes 2 arguments: ``(user_id (JID), password)`` | ``select user_id from tig_users where (user_id = ?) AND (user_pw = ?)``       |
+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------+-------------------------------------------------------------------------------+
| ``user-logout-query``     | This query is called when user logs out or disconnects. It can record that event in the database.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | Takes 1 argument: ``(user_id (JID))``            | ``update tig_users, set online_status = online_status - 1 where user_id = ?`` |
+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------+-------------------------------------------------------------------------------+
| ``non-sasl-mechs``        | Comma separated list of NON-SASL authentication mechanisms. Possible mechanisms are: ``password`` and ``digest``. The digest mechanism can work only with ``get-password-query`` active and only when password are stored in plain text format in the database.                                                                                                                                                                                                                                                                                                                           |                                                  |                                                                               |
+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------+-------------------------------------------------------------------------------+
| ``sasl-mechs``            | Comma separated list of SASL authentication mechanisms. Possible mechanisms are all mechanisms supported by Java implementation. The most common are: ``PLAIN``, ``DIGEST-MD5``, ``CRAM-MD5``. "Non-PLAIN" mechanisms will work only with the ``get-password-query`` active and only when passwords are stored in plain text format in the database.                                                                                                                                                                                                                                      |                                                  |                                                                               |
+---------------------------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+--------------------------------------------------+-------------------------------------------------------------------------------+

Drupal Authentication
^^^^^^^^^^^^^^^^^^^^^^^^^

Currently, we can only check authentication against a **Drupal** database at the moment. Full **Drupal** authentication is not implemented as of yet.

As **Drupal** keeps encrypted passwords in database the only possible authorization protocols are those based on PLAIN passwords.

To protect your passwords **Tigase** server must be used with SSL or TLS encryption.

Implementation of a **Drupal** database based authorization is located in ``tigase.db.jdbc.DrupalAuth`` class. Although this class is capable of adding new users to the repository I recommend to switch in-band registration off due to the caching problems in **Drupal.** Changes in database are not synchronized with **Drupal** yet. Functionality for adding new users is implemented only to ease user accounts migration from different repository types from earlier **Tigase** server installations.

The purpose of that implementation was to allow all accounts administration tasks from **Drupal** like: account creation, all accounts settings, like e-mail, full name, password changes and so on.

**Tigase** server uses following fields from **Drupal** database: name (user account name), pass (user account password), status (status of the account). Server picks up all changes instantly. If user status is not 1 then server won’t allow user to login trough XMPP even if user provides valid password.

There is no *Roster* management in **Drupal** yet. So Roster management have to be done from the XMPP client.

LDAP Authentication Connector
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Tigase XMPP Server offers support for authenticating users against an LDAP server in **Bind** **Authentication** mode.

Configuration for the LDAP support is really simple you just have to add a few lines to your ``config.tdsl`` file.

.. code:: tdsl

   authRepository {
       default () {
           cls = 'tigase.db.ldap.LdapAuthProvider'
           uri = 'ldap://ldap.tigase.com:389'
           'user-dn-pattern' = 'cn=USER_ID,ou=people,dc=tigase,dc=org'
       }
   }

Please note the ``USER_ID`` element, this is a special element of the configuration which is used to authenticate particular user. Tigase LDAP connector replaces it with appropriate data during authentication. You can control what Tigase should put into this part. In your configuration you must replace this string with one of the following:

1. ``%1$s`` - use user name only for authentication (JabberID’s localpart)

2. ``%2$s`` - use domain name only for authentication (JabberID’s domain part)

3. ``%3$s`` - use the whole Jabber ID (JID) for authentication

.. Important::

   Please make sure that you included ``autoCreateUser=true`` in your main data source (UserRepository and **not** above AuthRepository) as outlined in `??? <#autoCreateUser>`__ - otherwise you may run into problems with data access.

Configuration of SASL EXTERNAL
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

In order to enable SASL External set "Client Certificate CA" (``client-trust-extension-ca-cert-path``) to the path containing Certification Authority (CA) certificate in the VHost (domain) configuration, for example ``/path/to/cacert.pem``

File ``cacert.pem`` contains Certificate Authority certificate which is used to sign clients certificate.

Client certificate must include user’s Jabber ID as ``XmppAddr`` in ``subjectAltName``:

   As specified in RFC 3920 and updated in RFC 6120, during the stream negotiation process an XMPP client can present a certificate (a “client certificate”). If a JabberID is included in a client certificate, it is encapsulated as an id-on-xmppAddr Object Identifier (“xmppAddr”), i.e., a subjectAltName entry of type otherName with an ASN.1 Object Identifier of “id-on-xmppAddr” as specified in Section 13.7.1.4 of RFC 6120, `XEP-0178 <http://xmpp.org/extensions/xep-0178.html#c2s>`__.

It is possible to make client certificate **required** using same VHost configuration and enabling option ``Client Certificate Required`` (``client-trust-extension-cert-required``).

If this option will be enabled, then client **must provide** certificate. This certificate will be verified against ``clientCertCA``. If client does not provide certificate or certificate will be invalid, **TLS handshake will be interrupted and client will be disconnected**.

Using this options does not force client to use SASL EXTERNAL. Client still may authenticate with other SASL mechanisms.

SASL Mechanisms
----------------------

XMPP protocol supports many authentication methods, but most of them are used as `SASL <https://tools.ietf.org/html/rfc4422>`__ mechanisms. Tigase XMPP Server provides many SASL-based authentication mechanisms such as:

-  PLAIN *(enabled)*

-  ANONYMOUS

-  EXTERNAL

-  SCRAM-SHA-1 *(enabled)*

-  SCRAM-SHA-256 *(enabled)*

-  SCRAM-SHA-512

Most of them are enabled by default on default Tigase XMPP Server installation.

Enabling and disabling SASL mechanisms (credentials encoder/decoder)
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If you want to enable or disable one of password-based authentication mechanism such as ``SCRAM-SHA-1``, ``SCRAM-SHA-256`` or ``SCRAM-SHA-512`` you can do that by enabling or disabling encoders and decoders used on your installation. By enabling encoders/decoders you are deciding in what form the password is stored in the database. Those changes may (and in most cases will) impact which SASL mechanisms may be allowed to use on your installation.

.. Note::

   In most cases you should enable or disable both (credentials encoder and decoder) of the same type at the same time. The only exception of this rule is when you are changing those on already working installation. In this case you should only enable encoder of the type which you want to enable and request users to change their passwords. Then, after users will change their passwords, you should reconfigure server to enable decoder of the particular type. *(in other case user may loose a way to log in to your installation as system will reject their credentials as it may not have matching credentials for particular SASL mechanism)*.

**Enabling SCRAM-SHA-512 encoder**
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. code:: tdsl

   authRepository () {
       default () {
           credentialEncoders () {
               'SCRAM-SHA-512' () {}
           }
       }
   }

**Disabling SCRAM-SHA-1 decoder**
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
.. code:: tdsl

   authRepository () {
       default () {
           credentialDecoders () {
               'SCRAM-SHA-1' (active: false) {}
           }
       }
   }

.. Warning::

    It is strongly recommended not to disable encoders if you have enabled decoder of the same type as it may lead to the authentication issues, if client tries to use a mechanism which that is not available.

Application passwords
-------------------------

In recent versions of Tigase XMPP Server it is possible to create and use multiple username and password pairs to authorize connection to the single XMPP account.

With that in place it is now possible to have multiple password for a multiple clients accessing the same account what can be used to increase security of the account as even if one password compromised you can still log in and block lost or compromised device.

Adding application password
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To add new username-password pair you need to execute ``Add user credentials`` ad-hoc command (command node ``auth-credentials-add`` at ``sess-man``) while logged in the XMPP account for which you want to add a new application password.

During execution for a command you will be provided with a form to fill in with following fields:

-  The Jabber ID for the account (``jid``) - bare JID of your account

-  Credential ID (``credentialId``) - username for the new application password

-  Password (``password``) - a new password

After submitting this form a new credential will be added.

Login in with application password
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To log in with new password the XMPP client can use any SASL mechanism but it needs to provide (in SASL message):

-  ``authzid`` - account JID

-  ``authcid`` - username for application password

-  ``passwd`` - application password

With proper values, you application will be able to log in using application password.

Removing application password
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

If your device is compromised or lost and you want to remove application password, you need to use a different device and log in on your XMPP account. Then you need to execute ``Delete user credentials`` ad-hoc command (command node ``auth-credentials-delete`` at ``sess-man``).

During execution for a command you will be provided with a form to fill in with following fields:

-  The Jabber ID for the account (``jid``) - bare JID of your account

-  Credential ID (``credentialId``) - username for the application password which you want to remove

After submitting this form a credential will be removed.

Packet Filtering
----------------------

Tigase offers different ways to filter XMPP packets flying through the server. The most common use for packet filtering is to restrict users from sending or receiving packets based on the sender or received address.

There are also different possible scenarios: time based filtering, content filtering, volume filtering and so on.

All pages in this section describe different filtering strategies.

Domain Based Packet Filtering
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Domain based packet filtering is a simple filter allowing to restrict user communication based on the source/destination domain name. This is especially useful if we want to limit user communication within a single - own domain only or a list of domains.

A company might not wish to allow employers to chat during work hours with anybody in the world. A company may also have a few different domains used by different branches or departments. An administrator may restrict communication to a list of domains.

Introduction
~~~~~~~~~~~~~~~

The restriction is on a per-user basis. So the administrator can set a different filtering rules for each user. There is also a per-domain configuration and global-installation setting (applied from most general to most specific, i.e. from installation to user).

Regular users can not change the settings. So this is not like a privacy list where the user control the filter. Domain filter can not be changed or controlled by the user. The system administrator can change the settings based on the company policy.

There are predefined rules for packet filtering:

1. ``ALL`` - user can send and receive packets from anybody.

2. ``LOCAL`` - user can send and receive packets within the server installation only and all it’s virtual domains.

3. ``OWN`` - user can send and receive packets within his own domains only

4. ``BLOCK`` - user can’t communicate with anyone. This could be used as a means to temporarily disable account or domain.

5. ``LIST`` - user can send and receive packets within listed domains only (i.e. *whitelist*).

6. ``BLACKLIST`` - user can communicate with everybody (like ``ALL``), except contacts on listed domains.

7. ``CUSTOM`` - user can communicate only within custom created rules set.

Whitelist (``LIST``) and blacklist (``BLACKLIST``) settings are mutually exclusive, i.e. at any given point of time only one of them can be used.

Those rules applicable to particular users are stored in the user repository and are loaded for each user session. If there are no rules stored for a particular user server tries to apply rules for a VHost of particular user, and if there is no VHost filtering policy server uses global server configuration. If there is no filtering policy altogether server applies defaults based on following criteria:

1. If this is **Anonymous** user then ``LOCAL`` rule is applied

2. For all **other** users ``ALL`` rule is applied.

Configuration
~~~~~~~~~~~~~~~

Filtering is performed by the domain filter plugin which must be loaded at startup time. It is loaded by default if the plugins list is not set in the configuration file. However if you have a list of loaded plugins in the configuration file make sure ``domain-filter`` is on the list.

There is no other configuration required for the plugin to work.

Administration, Rules Management
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Although controlling domain filtering rules is possible for each user separately, it is not practical for large installations. In most cases users are stored in the database and a third-party system keeps all the user information.

To change the rule for a single user you can use loadable administration scripts feature and load `UserDomainFilter.groovy <https://github.com/tigase/tigase-server/tree/master/src/main/groovy/tigase/admin/UserDomainFilter.groovy>`__ script. It enables modifying rules for a given user JID.

Implementation
~~~~~~~~~~~~~~~

If you have a third party system which keeps and manages all user information than you probably have your own UserRepository implementation which allows the Tigase server to access user data. Filtering rules are loaded from user repository using following command:

.. code:: java

   repo.getData(user_id, null, DomainFilter.ALLOWED_DOMAINS_KEY, null);
   repo.getData(user_id, null, DomainFilter.ALLOWED_DOMAINS_LIST_KEY, null);

Where ``user_id`` is user Jabber ID without resource part, ``DomainFilter.ALLOWED_DOMAINS_KEY`` is a property key: ``allowed-domains``. The user repository MUST return one of following only:

1. ``ALL`` - if the user is allowed to communicate with anybody

2. ``LOCAL`` - if the user is allowed to communicate with users on the same server installation.

3. ``OWN`` - if the user is allowed to communicate with users within his own domain only.

4. ``LIST`` - list of domains within which the user is allowed to communicate with other users. No wild-cards are supported. User’s own domain should be included too.

5. ``BLACKLIST`` - list of domains within which the user is NOT allowed to communicate with other users. No wild-cards are supported. User’s own domain should NOT be included.

6. ``CUSTOM`` - list of rules defining custom communication permissions (server processes stanza according to first matched rule, similar to XEP-0016) in the following format:

::

   ruleSet = rule1;rule2;ruleX;

   rule = order_number|policy|UID_type[|UID]

   order_number = any integer;
   policy = (allow|deny);
   UID_type = [jid|domain|all];
   UID = user JID or domain, for example pubsub@test.com; if UID_type is ALL then this is ignored.

For example:

::

   1|allow|self;
   2|allow|jid|admin@test2.com;
   3|allow|jid|pubsub@test.com;
   4|deny|all;

1. ``null`` - a java null if there are no settings for the user.

In case of ``LIST`` and ``BLACKLIST`` filtering options, it’s essential to provide list of domains for the whitelisting/blacklisting. ``DomainFilter.ALLOWED_DOMAINS_LIST_KEY`` is a property key: "allowed-domains-list". The user repository MUST return semicolon separated list of domains: ``domain1.com;domain2.com,domain3.org``

The filtering is performed by the ```tigase.xmpp.impl.DomainFilter`` <https://github.com/tigase/tigase-server/tree/master/src/main/java/tigase/xmpp/impl/DomainFilter.java>`__ plugin. Please refer to source code for more implementation details.

Access Control Lists in Tigase
-------------------------------------

Tigase offers support for **Access Control List (ACL)** to allow for fine grained access to administration commands on the server.

By default, all administration commands are only accessible (visible through service discovery and can be executed) by the service administrators. Service administrators are existing accounts with JIDs (**BareJIDs**) listed in the ``config.tdsl`` file under ``admins = []`` (please see :ref:`admins <admins>` for details).

Additionally, other XMPP users and entities can be assigned permissions to execute a command or commands using Tigase’s ACL capabilities.

The following is a list of possible ACL modifiers for administrator command accessibility:

-  ``ALL`` - Everybody can execute the command, even users from different federated servers.

-  ``ADMIN`` - Local server administrators can execute the command, this is a default setting if no ACL is set for a command.

-  ``LOCAL`` - All users with accounts on the local server can execute the command. Users from other, federated servers will not be able to execute the command.

-  ``NONE`` - No one will be allowed to execute this command

-  ``DOMAIN_OWNER`` - Only user which is owner of the domain which items are being manipulated is allowed to execute the comment. If script is not checking permissions for the manipulated item, this value will behave in the same way as ``LOCAL``.

-  ``DOMAIN_ADMIN`` - Only user which is one of the domain administrators will be able to execute the command manipulating items related to the domain. If script is not checking permissions for the manipulated item, this value will behave in the same way as ``LOCAL``.

-  ``example.com`` - Only users with accounts on the selected domain will be able to execute the command. It may be useful to setup a domain specifically for admin accounts, and automatically all users within that domain would be able to run the command.

-  ``user@example.com`` - Comma separated list of JIDs of users who can execute the command.

In any case, regardless of ACL settings, any command can be executed and accessed by the designated service wide administrators, that is accounts listed as admins in the ``config.tdsl`` file.

Multiple ACL modifiers can be combined and applied for any command. This may not always makes sense. For example ALL supersedes all other settings, so it does not make sense to combine it with any other modifier. However, most others can be combined with JID to broaden access to specific accounts.

On Tigase server the Access Control List is checked for the first matching modifier. Therefore if you combine ALL with any other modifier, anybody from a local or remote service will always be able to execute the command, no matter what other modifiers are added.

Please note, the ACL lists work on the command framework level. Access is verified before the command is actually executed. There might be additional access restrictions within a command itself. In many cases, even if all local users are permitted to execute a command (LOCAL modifier), some commands allow only to be executed by a domain owner or a domain administrator (and of course by the service-wide administrators as well). All the commands related to a user management such as adding a new user, removing a user, password changes, etc… belong to this category. When conducting domain (vhost) management, creation/registration of a new domain can be done by any local user (if LOCAL ACL modifier is set) but then all subsequent domain management tasks such as removing the vhost, updating its configuration, setting SSL certificate can be done by the domain owner or administrator only.

The ACL list is set for a specific Tigase component and a specific command. Therefore the configuration property must specify all the details. So the general format for configuring ACL for a command is this:

.. code:: dsl

   comp-id () {
       commands {
           'command-id' = [ 'ACL_modifier', 'ACL_modifier', 'ACL_modifier' ]
       }
   }

The breakdown is as such:

-  ``comp-id`` is the Tigase server component ID such as: sess-man, vhost-man, c2s, etc..

-  ``commands`` is a static text which indicates that the property is for component’s command settings.

-  ``command-id`` is a command ID for which we set the ACL such as query-dns, http://jabber.org/protocol/admin#add-user, user-roster-management, etc…

Here are a few examples:

*Allowing local users to create and manage their own domains*

.. code:: dsl

   'vhost-man' () {
       commands {
           'comp-repo-item-add' = 'LOCAL'
           'comp-repo-item-remove' = 'LOCAL'
           'comp-repo-item-update' = 'LOCAL'
           'ssl-certificate-add' = 'LOCAL'
       }
   }

In fact all the commands except item-add can be executed by the domain owner or administrator.

*Allowing local users to execute user management commands:*

.. code:: dsl

   'sess-man' () {
       'commands' {
           'http://jabber.org/protocol/admin#add-user' = 'LOCAL'
           'http://jabber.org/protocol/admin#change-user-password' = 'LOCAL'
           'http://jabber.org/protocol/admin#delete-user' = 'LOCAL'
           'http://jabber.org/protocol/admin#get-online-users-list' = 'LOCAL'
           'http://jabber.org/protocol/admin#get-registered-user-list' = 'LOCAL'
           'http://jabber.org/protocol/admin#user-stats' = 'LOCAL'
           'http://jabber.org/protocol/admin#get-online-users-list' = 'LOCAL'
       }
   }

As in the previous example, the commands will by executed only by local users who are the specific domain administrators.

*Allowing users from a specific domain to execute query-dns command and some other users for given JIDs from other domains:*

.. code:: dsl

   'vhost-man' () {
       'commands' {
           'query-dns' = [ 'tigase.com', 'admin@tigase.org', 'frank@example.com' ]
       }
   }

To be able to set a correct ACL property you need to know component names and command IDs. Component IDs can be found in the service discovery information on running server or in the server logs during startup. A command ID can be found in the command script source code. Each script contains a list of metadata at the very beginning of it’s code. One of them is ``AS:CommandId`` which is what you have to use for the ACL setting.

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
