Tigase HTTP-API 2.2.0 Release Notes
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Major Changes
~~~~~~~~~~~~~~

-  Enable HTTP File Upload by default with additional, optional, AWS S3 compatible backend

-  Improvements to Web Setup to make installation even more straightforward

-  Allow exposing ``.well-known`` in the root context to facilitate `XEP-0156: Discovering Alternative XMPP Connection Methods <https://xmpp.org/extensions/xep-0156.html>`__

-  Add option to redirect requests from http to https



All Changes
~~~~~~~~~~~~~

-  `#http-65 <https://projects.tigase.net/issue/http-65>`__: More detailed logs

-  `#http-86 <https://projects.tigase.net/issue/http-86>`__: Add s3 backend for http-upload

-  `#http-91 <https://projects.tigase.net/issue/http-91>`__: Items in setup on Features screen are misaligned

-  `#http-93 <https://projects.tigase.net/issue/http-93>`__: Update web-installer documentation

-  `#http-95 <https://projects.tigase.net/issue/http-95>`__: Enable HTTP File Upload by default

-  `#http-96 <https://projects.tigase.net/issue/http-96>`__: Enabling cluster mode / ACS doesn’t add it to resulting configuration file

-  `#http-98 <https://projects.tigase.net/issue/http-98>`__: Setup tests are failing since Septempter

-  `#http-99 <https://projects.tigase.net/issue/http-99>`__: Enforce max-file-size limit

-  `#http-100 <https://projects.tigase.net/issue/http-100>`__: Prevent enabling all Message\* plugins

-  `#http-101 <https://projects.tigase.net/issue/http-101>`__: Prevent enabling all Mobile\* plugins

-  `#http-102 <https://projects.tigase.net/issue/http-102>`__: Last activity plugins handling should be improved

-  `#http-103 <https://projects.tigase.net/issue/http-103>`__: Enabling http-upload should give an info about requirement to set domain/store

-  `#http-105 <https://projects.tigase.net/issue/http-105>`__: Handle forbidden characters in filenames

-  `#http-106 <https://projects.tigase.net/issue/http-106>`__: Can’t remove user for non-existent VHost

-  `#http-107 <https://projects.tigase.net/issue/http-107>`__: Allow exposing ``.well-known`` in the root context

-  `#http-108 <https://projects.tigase.net/issue/http-108>`__: Add option to redirect requests from http to https

-  `#http-109 <https://projects.tigase.net/issue/http-109>`__: openAccess option is missing after migrating the component to TK

-  `#http-110 <https://projects.tigase.net/issue/http-110>`__: Add support for querying and managing uploaded files

-  `#http-111 <https://projects.tigase.net/issue/http-111>`__: DefaultLogic.removeExpired removal of slot failed

-  `#http-113 <https://projects.tigase.net/issue/http-113>`__: Add condition to redirect only if the X-Forwarded-Proto has certain value

-  `#http-114 <https://projects.tigase.net/issue/http-114>`__: TigaseDBException: Could not allocate slot

-  `#http-116 <https://projects.tigase.net/issue/http-116>`__: Limiting list of VHosts doesn’t work for JDK based http-server

-  `#http-117 <https://projects.tigase.net/issue/http-117>`__: Http redirection doesn’t work in docker

-  `#http-119 <https://projects.tigase.net/issue/http-119>`__: Can’t change VHost configuration via Admin WebUI

-  `#http-120 <https://projects.tigase.net/issue/http-120>`__: Improve S3 support for HTTP File Upload to accept custom URL and credentials for S3 storage configuration

-  `#http-121 <https://projects.tigase.net/issue/http-121>`__: Deprecate DnsWebService and rewrite /.well-known/host-meta 