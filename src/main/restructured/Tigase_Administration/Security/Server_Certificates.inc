Server Certificates
---------------------

-  :ref:`Creating and Loading the Server Certificate in pem Files<certspem>`

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

Now the .csr file will be signed by a Certificate Authority. In this tutorial, we will be self-signging our certificate. This practice however is generally not recommended, and you will receive notifications that your certificate is not trusted. There are commercial offers from companies to sign your certificate from trusted sources. Please see the :ref:`Certificate From Other Providers<OtherSources>` section for more information.

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

   **Tigase requires full certificate chain in PEM file (described above)! Different applications may require pem file with certificates and private key in different order. So the same file may not be necessarily used by other services like Web server or e-mail server. Currently, Tigase can automatically sort certificates in PEM file while loading it.**

.. _InstallingSSLCertificate:

Installing/Loading Certificate To the Tigase Server
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Installing and loading certificates is very easy. The server can load all certificates directly from **pem** files. You just need to create a separate pem file for each of your virtual domains and put the file in a directory accessible by the server. Tigase server can automatically load all **pem** files found in given directory. By default, and to make things easy, we recommend the ``Tigase/certs`` directory.

It’s also possible to use: \* Admin ad-hoc command via XMPP client - you should navigate to Service Discovery of your server and in the list of commands for ``VHost Manager`` component select ``Add SSL Certificate`` and then follow instructions \* Admin WebUI - open ``http://<host>/admin``, navigate to ``Other`` category and in it select ``Add SSL Certificate`` and then follow instructions \* REST API - make a ``POST`` request to http://localhost:8080/rest/adhoc/vhost-man@domain.com with payload containing your certificate; to get the required form fields make ``GET`` request to the same endpoint

.. _OtherSources:

Certificate From Other Providers
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

There is number of certificate providers offering certificates either for free or for money. You can use any of them, however you have to be aware that sometimes certificates might not be recognized by all XMPP servers, especially if it’s one from a new provider. Here is an example list of providers:

-  LetsEncrypt - please see :ref:`Installing LetsEncrypt Certificates in Your Linux System<LetsEncryptCertificate>` for details

-  `CAcert <https://www.cacert.org/>`__ - free certificates with Web GUI. (WARNING: it’s not widely accepted)

-  `Verisign <https://www.verisign.com/>`__ - very expensive certificates comparing to above provides but the provider is recognized by everybody. If you have a certificate from Verisign you can be sure it is identified as a valid certificate.

-  `Comodo Certificate Authority <http://www.comodo.com/business-security/digital-certificates/ssl-certificates.php>`__ offers different kind of commercial certificates

To obtain certificate from a third party authority you have to go to its website and request the certificate using certificate request generated above. I cannot provide any instructions for this as each of the providers listed have different requirements and interfaces.

We **highly** recommend using LetsEncrypt keys to self-sign and secure your domain. Instructions are in the :ref:`next section<LetsEncryptCertificate>`.

Using one certificate for multiple domains
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. Note::

   Tigase tries to be *smart* and automatically detects wildcard domain and alternative domains so it’s not needed to duplicate same certificate in multiple files to match domains - same file will be loaded and make available for all domains (CNames) available in the certificate.

.. _LetsEncryptCertificate:

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

For Tigase XMPP Server, we are only concerned with ``privkey.pem`` and ``fullchain.pem`` (or ``chain.pem`` - please consider actual issuers and certification chain in this case!).

At this point we will need to obtain the root and intermediate certificates, this can be done by downloading these certificates from the `LetsEncrypt Chain of Trust website <https://letsencrypt.org/certificates/>`__.

.. Note::

   Please pay utmost attention to the actual certificate issuers and make sure that the certification chain is maintained!

On the time of the writing, LetsEncrypt root CA is ``ISRG Root X1``. In order to provide complete chain to the root CA ``ISRG Root X1`` root certificate: https://letsencrypt.org/certs/isrgrootx1.pem and combine it with private key and full certificate chain from certbot.

.. code:: bash

   wget https://letsencrypt.org/certs/isrgrootx1.pem

These are the root certificate, and the intermediate certificate signed by root certificate.

.. Note::

   IdenTrust cross-signed certificate will not function properly in the future!

Take the contents of your ``privkey.pem``, certificate, and combine them with the contents of ``isrgrootx1.pem``  into a single pem certificate.

Depending on your configuration you either need to name the file after your domain such as ``mydomain.com.pem`` and place it under ``certs/`` subdirectory of Tigase XMPP Server installation or update it using admin ad-hoc (see :ref:`Storing and managing certificates<certificateStorage>`)

If you moved all certs to a single directory, you may combine them using the following command under \*nix operating systems:.

.. code:: bash

   cat ./cert.pem ./privkey.pem ./isrgrootx1.pem > mydomain.com.pem

Your certificate should look something like this:

.. code::rtificate

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

   Please adjust account credentials used for deployment (``USER``, ``PASS``, ``DOMAIN``) as well as paths to Let’s Encrypt certificates (``LE_CERTS_PATH``, i.e. path pointing to the directory with ``isrgrootx1.pem`` file)

.. Note::

   It's recommended to create dedicated user responsible for updating the certificate configured as domain administrator  to limit the scope of the access the user has on the server.

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

       CERT=`cat "$RENEWED_LINEAGE/fullchain.pem" "$RENEWED_LINEAGE/privkey.pem" "${LE_CERTS_PATH}/isrgrootx1.pem"`

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

.. _certificateStorage:

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