Tigase XMPP Server 8.2.0 Change notes
-------------------------------------

Major Changes
^^^^^^^^^^^^^

-  **Improvements to s2s connection**: Version 8.2.0 brings a lot of improvements related to s2s connectivity: support for TLS1.3, improved logic during authentication and stream negotiation solving connectivity issues with various deployments

-  **Better handling of certificates**: It’s now possible to store certificates in the database making it easier to manage them in clustered environment.

-  Deprecation of ``Element`` based events in favour of Object based events

-  Improved performance: reduced memory usage and decrease startup time

All Minor Features & Behavior Changes
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

-  `#server-1050 <https://projects.tigase.net/issue/server-1050>`__: Database installation without root credentials

-  `#server-1062 <https://projects.tigase.net/issue/server-1062>`__: Deprecate Element based Event-bus

-  `#server-1097 <https://projects.tigase.net/issue/server-1097>`__: It’s not possible to configure additional PacketFilters

-  `#server-1101 <https://projects.tigase.net/issue/server-1101>`__: Enabling TLS1.3 causes s2s connections to fail

-  `#server-1102 <https://projects.tigase.net/issue/server-1102>`__: Add possibility to extend MAM to MAM:2

-  `#server-1105 <https://projects.tigase.net/issue/server-1105>`__: Enhance Add SSL Certificate ad-hoc with option to set default

-  `#server-1119 <https://projects.tigase.net/issue/server-1119>`__: Use database for certificate storage instead of filesystem

-  `#server-1120 <https://projects.tigase.net/issue/server-1120>`__: JabberIqRegister should allow enforcing both CAPTCHA and e-mail

-  `#server-1132 <https://projects.tigase.net/issue/server-1132>`__: Don’t use s2s socket if only one-direction works

-  `#server-1142 <https://projects.tigase.net/issue/server-1142>`__: After registration inform the client that the account activation (email) is required

-  `#server-1158 <https://projects.tigase.net/issue/server-1158>`__: Establishing JMX connection to the server causes excessive memory allocation

-  `#server-1162 <https://projects.tigase.net/issue/server-1162>`__: Allow interfaces in @ConfigField

-  `#server-1170 <https://projects.tigase.net/issue/server-1170>`__: TLS infinity loop impacts Tigase XMPP Server performance

-  `#server-1175 <https://projects.tigase.net/issue/server-1175>`__: Connection with diebesban.de stopped with invalid-namespace error

-  `#server-1177 <https://projects.tigase.net/issue/server-1177>`__: Ability to change log level during runtime

-  `#server-1178 <https://projects.tigase.net/issue/server-1178>`__: Remove ``online_status`` from the repository

-  `#server-1179 <https://projects.tigase.net/issue/server-1179>`__: Add support for {clusterNode} in XEP-0215 host field

-  `#server-1181 <https://projects.tigase.net/issue/server-1181>`__: NoSuchElementException in MaxDailyCounterQueue

-  `#server-1182 <https://projects.tigase.net/issue/server-1182>`__: NPE while processing <iq type="result"/> without existing session

-  `#server-1187 <https://projects.tigase.net/issue/server-1187>`__: SchemaLoader should not print passwords in the logs (URL logs)

-  `#server-1192 <https://projects.tigase.net/issue/server-1192>`__: Obfuscate repository passwords

-  `#server-1190 <https://projects.tigase.net/issue/server-1190>`__: Executing EditUser on non-existen’t user causes creation of the user

-  `#server-1193 <https://projects.tigase.net/issue/server-1193>`__: Push notifications are sent for groupchat messages without <body/>

-  `#server-1197 <https://projects.tigase.net/issue/server-1197>`__: Infinite loop while cutting body of encrypted push notification to fit the push notifications limit

-  `#server-1199 <https://projects.tigase.net/issue/server-1199>`__: Don’t send any packets until s2s stream negotiation is finished

-  `#server-1200 <https://projects.tigase.net/issue/server-1200>`__: Use proper size of network buffers for high-throughput connections

-  `#server-1203 <https://projects.tigase.net/issue/server-1203>`__: Handing error packets in CIDConnections.sendPacketsBack

-  `#server-1217 <https://projects.tigase.net/issue/server-1217>`__: Prevent performing schema upgrade concurrently

-  `#server-1219 <https://projects.tigase.net/issue/server-1219>`__: Use all JDBC URI parameters from config.tdsl when performing database upgrade.

-  `#server-1222 <https://projects.tigase.net/issue/server-1222>`__: Add support for XEP-0377: Spam Reporting

-  `#server-1229 <https://projects.tigase.net/issue/server-1229>`__: Enabling CAPTCHA or e-mail for JabberIqRegister breaks password changing functionality.

-  `#server-1229 <https://projects.tigase.net/issue/server-1229>`__: Enabling CAPTCHA or e-mail for JabberIqRegister breaks password changing functionality.

-  `#server-1233 <https://projects.tigase.net/issue/server-1233>`__: Add option to CertificateRepository to load certificates from the filesystem

-  `#server-1234 <https://projects.tigase.net/issue/server-1234>`__: Roster API improvements

-  `#server-1237 <https://projects.tigase.net/issue/server-1237>`__: Rework CertificateRepository so items are stored individually

-  `#server-1238 <https://projects.tigase.net/issue/server-1238>`__: Can’t set MOTD via ad-hoc.

-  `#server-1243 <https://projects.tigase.net/issue/server-1243>`__: Include wait-for-it.sh script in base distribution

-  `#server-1245 <https://projects.tigase.net/issue/server-1245>`__: MethodStatistics doesn’t work well for interfaces with overloaded methods

-  `#server-1251 <https://projects.tigase.net/issue/server-1251>`__: Can’t initialise MAM processor with default installation

-  `#server-1252 <https://projects.tigase.net/issue/server-1252>`__: Remove select row_count() from Tig_OfflineMessages_DeleteMessage

-  `#server-1253 <https://projects.tigase.net/issue/server-1253>`__: It seems that 'expired-processor' doesn’t remove periodically expired messages

-  `#server-1254 <https://projects.tigase.net/issue/server-1254>`__: Fix slow startup and shutdown

-  `#server-1258 <https://projects.tigase.net/issue/server-1258>`__: Allow beans to be instantiated without the requirement to reference/inject them

-  `#server-1260 <https://projects.tigase.net/issue/server-1260>`__: UserConnectedEvent should be a cluster event

-  `#server-1261 <https://projects.tigase.net/issue/server-1261>`__: Revise and improve EventBus developer guide

-  `#server-1269 <https://projects.tigase.net/issue/server-1269>`__: SSL issues are hidden by default making it difficult to identify

-  `#server-1273 <https://projects.tigase.net/issue/server-1273>`__: Add option to limit number of concurrently connected resources

-  `#server-1277 <https://projects.tigase.net/issue/server-1277>`__: Fix HUGE out queue in StreamManagementIOProcessor

-  `#server-1278 <https://projects.tigase.net/issue/server-1278>`__: NPE in StreamManagementIOProcessor.serviceStopped

-  `#server-1282 <https://projects.tigase.net/issue/server-1282>`__: XMPPProcessorAbstract.processToUserPacket() responds to IQ result with error

-  `#server-1284 <https://projects.tigase.net/issue/server-1284>`__: Add validation to JabberIqAuth

-  `#server-1285 <https://projects.tigase.net/issue/server-1285>`__: Wrong field type for XEP-0157 entries

-  `#server-1290 <https://projects.tigase.net/issue/server-1290>`__: Improve StringPrep to actually forbid space in localpart/domain as per rfc7622

-  `#server-1292 <https://projects.tigase.net/issue/server-1292>`__: TLS connectivity issue with search.jabber.network

-  `#server-1297 <https://projects.tigase.net/issue/server-1297>`__: Add option to push plugin that would allow to overwrite unencrypted part in (OMEMO) encrypted messages

-  `#server-1303 <https://projects.tigase.net/issue/server-1303>`__: Better handling of "The target is unavailable at this time." / PacketInvalidTypeException

-  `#server-1305 <https://projects.tigase.net/issue/server-1305>`__: Allow creation of admin user (if not exist) during ``upgrade-schema`` task

-  `#server-1306 <https://projects.tigase.net/issue/server-1306>`__: Fix farge amount of direct memory being used.

-  `#server-1307 <https://projects.tigase.net/issue/server-1307>`__: Fix disconnection on MAM sync

-  `#extras-3 <https://projects.tigase.net/issue/extras-3>`__: Add AWS logback and documentation how to use it

-  `#extras-4 <https://projects.tigase.net/issue/extras-4>`__: Unescape and normalise logs in mail notifications before sending them

-  `#extras-7 <https://projects.tigase.net/issue/extras-7>`__: Add email validation during in-band-registration; better handling of mail sending exceptions regarding to non-existent addresses

-  `#extras-9 <https://projects.tigase.net/issue/extras-9>`__: Deprecate mDNS implementation

-  `#serverdist-8 <https://projects.tigase.net/issue/serverdist-8>`__: Remove DNS resolution part from XEP-0156 implementation
