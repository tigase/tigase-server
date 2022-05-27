Tigase Push 1.2.0 Release Notes
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^



Major Changes
~~~~~~~~~~~~~

-  Added support for sending VoIP push notifications using PushKit

-  Support for storing APNS certificates in repository instead of filesystem for easier cluster deployments

-  Add priority detection for push notifications to avoid excessive pushes to devices

-  Inclusion of APNS certificate validity task that notifies if it’s about to expire



All Changes
~~~~~~~~~~~~~

-  `#push-29 <https://projects.tigase.net/issue/push-29>`__ Added support for sending VoIP push notifications using PushKit

-  `#push-30 <https://projects.tigase.net/issue/push-30>`__ Added REST API handler for quick unregistration of a device

-  `#push-32 <https://projects.tigase.net/issue/push-32>`__ Fixed issue with APNS certificate validation

-  `#push-33 <https://projects.tigase.net/issue/push-33>`__ Added statistics gathering

-  `#push-35 <https://projects.tigase.net/issue/push-35>`__ Added support for APNS certificate in PEM

-  `#push-36 <https://projects.tigase.net/issue/push-36>`__ Improved priority detection of push notifications

-  `#push-37 <https://projects.tigase.net/issue/push-37>`__ Enable APNS certificates to be stored in UserRepository - management is done via ad-hoc command;

-  `#push-39 <https://projects.tigase.net/issue/push-39>`__ Changes to improve error handling

-  `#push-41 <https://projects.tigase.net/issue/push-41>`__ Fixed issue with ``ApnsService`` exceptions not being thown logged

-  `#push-42 <https://projects.tigase.net/issue/push-42>`__ Fixed error type reported back on ``tooManyRequestsForDeviceToken``

-  `#push-47 <https://projects.tigase.net/issue/push-47>`__ Added task to periodically validate SSL certificates for Push notifications

-  `#push-48 <https://projects.tigase.net/issue/push-48>`__ Fixed handling events by APNsBinaryApiProvider

-  `#push-49 <https://projects.tigase.net/issue/push-49>`__ Added enforcement to use HTTP/2 protocol (with use of ALPN)
