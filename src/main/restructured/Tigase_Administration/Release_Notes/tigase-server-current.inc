Tigase XMPP Server 8.4.0 Change notes
-------------------------------------

.. Note::

   This version requires JDK17!

.. Note::

   Complete list of changes available in [`tigase-server-8.4.0` milestone](https://tigase.dev/tigase/_server/~milestones/11)

Major Changes
^^^^^^^^^^^^^

* Add support for XEP-0227: Portable Import/Export Format for XMPP-IM Servers (tigase/_server/server-core#1456)
* Add support for showing accounts with push notifications enabled as `away` (tigase/_server/server-core#1527)
* Add support for banning users with support of xmppbl.org (tigase/_server/tigase-muc#156)
* Add support for XEP-0404: Mediated Information eXchange (MIX): JID Hidden Channels
* Add ad-hoc commands to manage MIX channel
* Initial rewrite of HTTP-API from Groovy to Java and switch to jte for templating for improve performance
* Add dashboard to easily manage users (tigase/_server/tigase-http-api#132)
* Added support for opt-in for ProtoXEP: PubSub Server Information
* Add support for authentication of APNS provider using P8 (tigase-push)
* Allow overriding default "networkaddress.cache.ttl" configuration (tigase/_server/tigase-utils#26)
* Deprecate MD5*CredentialsEntry (tigase/_server/server-core#1359)
* Add support for message retraction by admins/owners (tigase/_server/tigase-mix#34)
* Add support for XEP-0425: Message Moderation (tigase/_server/tigase-muc#152)


All Minor Features & Behavior Changes
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

- Fix issue where exception thrown within StoredProcedure that has transaction can cause a lock (tigase/_server/server-core#1347)
- Improve caching (tigase/_server/tigase-pubsub#92)
- Improve handling of database failovers (connecting to read-only instance due to DNS propagation being to slow on AWS part) (tigase/_server/server-core#1354)
- Update MongoDB driver (tigase/_server/tigase-mongodb#34)
- Implement support for XEP-0404: Mediated Information eXchange (MIX): JID Hidden Channels (tigase/_server/tigase-mix#37)
- Remove JaXMPP dependency in tigase-push-fcm (tigase/_server/tigase-push#50)
- Improved and refactor HTTP API implementation (tigase/_server/tigase-http-api#122)
- Expose account status value in endpoint to retrieve user details (tigase/_server/tigase-http-api#131)
- Setup not working (NPE because of missing JTE compiler) (tigase/_server/tigase-http-api#135)
- Add method to generate tokens/QR codes for authentication (tigase/_server/tigase-http-api#133)
- http-api should delay listening on port until server finished startup (tigase/_server/tigase-http-api#130)
- Add ad-hoc commands to manage MIX channel (tigase/_server/tigase-mix#36)
- Add support for exporting/importing MIX channel data (tigase/_server/tigase-mix#38)
- Add support for XEP-0227: Portable Import/Export Format (tigase/_server/tigase-message-archiving#81)
- XEP-0227: Portable Import/Export Format for XMPP-IM Servers (tigase/_server/tigase-pubsub#138)
- Fix disco#items of PubSub node ignores permission checking (tigase/_server/tigase-pubsub#137)
- NumberFormatException: For input string: "max" (tigase/_server/tigase-pubsub#134)
- RSM sends <max/> element in response (tigase/_server/tigase-utils#27)
- Unexpected `<build/>` element in XEP-0092: Software Version (tigase/_server/server-core#1522)
- Add support for exporting MUC rooms and history to XML (tigase/_server/tigase-muc#161)
- Registration captcha makes registration impossible (tigase/_server/server-core#1510)
- Return better SASL error for accounts pending confirmation (tigase/_server/server-core#1511)
- Missing escaping of some chars in encrypted Push Notifications (tigase/_server/server-core#1512)
- SCRAM *-PLUS mechanisms unavailable after StartTLS (tigase/_server/server-core#1508)
- Rename 'captcha' field to 'qa' (tigase/_server/server-core#1516)
- Added support for opt-in for ProtoXEP: PubSub Server Information (tigase/_server/server-core#1515)
- Adjust TLS ciphers and options values used by default (tigase/_server/server-core#1517)
- Add support for token/HMAC based authentication (tigase/_server/server-core#1520)
- Not XML-well-formed stanza being accepted by server and routed to recipient (tigase/_server/tigase-xmltools#16)
- Dockerize with Jib (tigase/_server/tigase-xmpp-server-docker#12)
- Fix issue where it's not possible to configure seeOtherHost.defaultHost (tigase/_server/server-core#1364)
- Add support for testing S2S connectivity over IPv6 (tigase/_server/server-core#1363)
- Add support for setting user as admin using REST API (tigase/_server/tigase-http-api#127)
- Create pre-processor that would filter out error messages (tigase/_server/server-core#1344)
- Make it easier to disable BruteForceLocker (tigase/_server/server-core#1345)
- Cache usage statistics are not collected/computed correctly (tigase/_server/tigase-pubsub#135)
- Improve handling for MAM:1 queries with old IDs (tigase/_server/tigase-muc#153)
- Undelivered stanzas are processed before stream closure commands (tigase/_server/server-core#1343)
- [Setup] Add support for env-variables exposed in docker start.sh script (tigase/_server/tigase-http-api#142)
- Add API-KEY repo implementation that would be based on environment variables (tigase/_server/tigase-http-api#97)
- Add support for certificates private key using `ecdsa` (tigase/_server/tigase-utils#28)
