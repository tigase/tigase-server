Tigase Message Archiving 3.0.0 Release Notes
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Major Changes
~~~~~~~~~~~~~

-  Add support for urn:xmpp:mam:2

-  Add support for `XEP-0308: Last Message Correction <https://xmpp.org/extensions/xep-0308.html>`__


All Changes
~~~~~~~~~~~~~

-  `#mam-47 <https://projects.tigase.net/issue/mam-47>`__: Add support for urn:xmpp:mam:2

-  `#mam-49 <https://projects.tigase.net/issue/mam-49>`__: Historical message duplication

-  `#mam-50 <https://projects.tigase.net/issue/mam-50>`__: XEP-0308: Last Message Correction

-  `#mam-51 <https://projects.tigase.net/issue/mam-51>`__: Fix OMEMO encrypted messages are not stored by MA or MAM

-  `#mam-54 <https://projects.tigase.net/issue/mam-54>`__: Fix NPE in MAM/Message Archiving

-  `#mam-55 <https://projects.tigase.net/issue/mam-55>`__: Fix IllegalArgumentException in MessageArchiveVHostItemExtension

-  `#mam-56 <https://projects.tigase.net/issue/mam-56>`__: Fix upgrade-schema failes

-  `#mam-58 <https://projects.tigase.net/issue/mam-58>`__: Change message archiving rules

-  `#mam-60 <https://projects.tigase.net/issue/mam-60>`__: Fix Message carbons stored in MAM

-  `#mam-61 <https://projects.tigase.net/issue/mam-61>`__: Adjust schema to use new primary keys

-  `#mam-65 <https://projects.tigase.net/issue/mam-65>`__: Fix archiveMessage: Data truncation: Data too long for column ``_body``

-  `#mam-66 <https://projects.tigase.net/issue/mam-66>`__: Fix NPE in AbstractMAMProcessor.updatePrefrerences()

-  `#mam-67 <https://projects.tigase.net/issue/mam-67>`__: Fix Incorrect datetime value in JDBCMessageArchiveRepository

-  `#mam-68 <https://projects.tigase.net/issue/mam-68>`__: Add option to disable local MAM archive

-  `#mam-69 <https://projects.tigase.net/issue/mam-69>`__: Fix Data truncation: Data too long for column '_stanzaId'

-  `#mam-70 <https://projects.tigase.net/issue/mam-70>`__: Fix Schema is inconsistent (tigase.org mysql vs clean postgresql)

-  `#mam-72 <https://projects.tigase.net/issue/mam-72>`__: Fix Deadlock on inserting message
