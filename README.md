<p align="center">
  <a href="https://tigase.net/">
    <img
      alt="Highly optimized, extremely modular and very flexible XMPP/Jabber server"
      src="https://github.com/tigase/website-assets/blob/master/tigase/images/tigase-logo.png?raw=true"
      width="300"
    />
  </a>
</p>

<p align="center">
  Highly optimized, extremely modular and very flexible XMPP/Jabber server
</p>

<p align="center">
  <img alt="Tigase Tigase Logo" src="https://github.com/tigase/website-assets/blob/master/tigase/images/tigase-logo.png?raw=true" width="25"/>
  <img src="https://tc.tigase.net/app/rest/builds/buildType:(id:TigaseServer_Build)/statusIcon" width="100"/>
  <a href='https://compliance.conversations.im/server/tigase.im'><img src='https://compliance.conversations.im/badge/tigase.im'></a> 
  <a href='https://xmpp.net/result.php?domain=tigase.im&amp;type=client'><img src='https://xmpp.net/badge.php?domain=tigase.im' alt='xmpp.net score' /></a>
  <img src='https://img.shields.io/github/downloads/tigase/tigase-server/total' alt='GitHub All Releases' />
</p>

# What it is

Tigase XMPP Server is highly optimized, extremely modular and very flexible XMPP/Jabber server written in Java.

This repository contains source code of the main part of the Tigase XMPP Server.

*The project exists since 2004 and we have recently moved it over to GitHub.*

Other Tigase projects related to XMPP:

Tigase XMPP Server addons:
* [MUC Component](https://github.com/tigase/tigase-muc) - Multi-User Chat: [XEP-0045](https://xmpp.org/extensions/xep-0045.html)
* [PubSub Component](https://github.com/tigase/tigase-pubsub) - Publish-Subscribe: [XEP-0060](https://xmpp.org/extensions/xep-0060.html) and Personal Eventing Protocol: [XEP-0163](https://xmpp.org/extensions/xep-0163.html)
* [Socks5 Proxy Component](https://github.com/tigase/tigase-socks5) - SOCKS5 Bytestreams: [XEP-0065](https://xmpp.org/extensions/xep-0065.html)
* [STUN Component](https://github.com/tigase/tigase-stun) - [STUN](https://en.wikipedia.org/wiki/STUN) Component for Tigase
* [HTTP API Component](https://github.com/tigase/tigase-http-api) - Component providing easy to use HTTP endpoints for server management and integration based on JDK built-in HTTP server.
* [Jetty HTTP API Component](https://github.com/tigase/tigase-http-api-jetty) - High performance and high load component providing easy to use HTTP endpoints for server management and integration based on [Jetty HTTP Server](https://www.eclipse.org/jetty/).
* [MongoDB Connector](https://github.com/tigase/tigase-mongodb) - Connector adding support for [MongoDB](https://www.mongodb.com) database to Tigase server.
* [Message Archiving Component](https://github.com/tigase/tigase-message-archiving) - Component providing Message Archiving [XEP-0136](https://xmpp.org/extensions/xep-0136.html) and Message Archive Management [XEP-0313](https://xmpp.org/extensions/xep-0313.html) support.

Tools: 
* [Database Migrator Tool](https://github.com/tigase/tigase-database-migrator) - Tools helping with migration from other XMPP servers to Tigase based system.
* [TTS-NG Test Suite](https://github.com/tigase/tigase-tts-ng) - Test Suite to run automated tests for the Tigase XMPP Server
* [Tigase Monitor Console](https://github.com/tigase/tigase-monitor) - Stand-alone application for the Tigase XMPP Server monitoring and management console.
* [Atom DSL Syntax](https://github.com/tigase/tigase-dsl-syntax-highlighter-for-atom) - [Atom](https://atom.io/) DSL syntex highlighter for Tigase XMPP Server configuration files.
* [IntelliJ IDEA DSL Syntax](https://github.com/tigase/tigase-dsl-syntax-highlighter-for-atom) - [IntelliJ IDEA IDE](https://www.jetbrains.com/idea/) DSL syntex highlighter for Tigase XMPP Server configuration files.

Tigase XMPP Clients:
* [StorkIM Client](https://github.com/tigase/stork) - Android XMPP Client
* [SiskinIM Client](https://github.com/tigase/siskin-im) - iOS XMPP Client
* [BeagleIM Client](https://github.com/tigase/beagle-im) - MacOS XMPP Client
* [Swift Library](https://github.com/tigase/tigase-swift) - Tigase Swift XMPP Library
* [Swift OMEMO Plugin](https://github.com/tigase/tigase-swift-omemo) - OMEMO support for Tigase Swift XMPP library

Tigase based IoT:
* [Tigase IoT Framework](https://github.com/tigase/tigase-iot-framework) - Easy to use IoT framework to communicate and control Iot devices over XMPP
* [Tigase IoT Framework - Examples](https://github.com/tigase/tigase-iot-framework-examples) - Examples on how to extend the Tigase IoT Framework with support for different devices
* [Tigase RPi Library](https://github.com/tigase/tigase-rpi) -  Java low-level library to control sensors and devices connected to RasperryPi.

# Features

Tigase XMPP Server contains full support for [RFC 6120 - XMPP CORE](http://xmpp.org/rfcs/rfc6120.html), [RFC 6121 - XMPP IM](http://xmpp.org/rfcs/rfc6120.html) and [RFC 7395 - XMPP over WebSockets](https://tools.ietf.org/html/rfc7395) making it accessible using XMPP client connections:
* over TCP
* over HTTP/HTTPS (BOSH)
* over WebSockets

and over server-to-server connections as well as over XMPP component connections.

Additionally Tigase XMPP Server provides HTTP API for integration with other services unable to communicate over XMPP.

Moveover, Tigase XMPP Server comes with support for Push Notifications making it possible to push notification to mobile devices.

Following features are supported by Tigase XMPP Server:
* [XEP-0016: Flexible Offline Message Retrieval](http://xmpp.org/extensions/xep-0016.html)
* [XEP-0030: Service Discovery](http://xmpp.org/extensions/xep-0030.html)
* [XEP-0045: Multi User Chat](http://xmpp.org/extensions/xep-0045.html)
* [XEP-0060: Publish-Subscribe](http://xmpp.org/extensions/xep-0060.html)
* [XEP-0079: Advanced Message Processing](http://xmpp.org/extensions/xep-0079.html)
* [XEP-0114: Jabber Component Protocol](http://xmpp.org/extensions/xep-0114.html)
* [XEP-0115: Entity Capabilities](http://xmpp.org/extensions/xep-0115.html)
* [XEP-0133: Service Administration](http://xmpp.org/extensions/xep-0133.html)
* [XEP-0136: Message Archiving](http://xmpp.org/extensions/xep-0136.html)
* [XEP-0163: Personal Eventing Protocol](http://xmpp.org/extensions/xep-0163.html)
* [XEP-0198: Stream Management](http://xmpp.org/extensions/xep-0198.html)
* [XEP-0199: XMPP Ping](http://xmpp.org/extensions/xep-0199.html)
* [XEP-0206: XMPP over BOSH](http://xmpp.org/extensions/xep-0206.html)
* [XEP-0225: Component Connections](http://xmpp.org/extensions/xep-0225.html)
* [XEP-0237: Roster Versioning](http://xmpp.org/extensions/xep-0237.html)
* [XEP-0280: Message Carbons](http://xmpp.org/extensions/xep-0280.html)
* [XEP-0313: Message Archive Management](http://xmpp.org/extensions/xep-0313.html)
* [XEP-0357: Push Notifications](http://xmpp.org/extensions/xep-0357.html)
* [XEP-0363: HTTP File Upload](http://xmpp.org/extensions/xep-0363.html)
* and many more...

# Support

When looking for support, please first search for answers to your question in the available online channels:

* Our online documentation: [Tigase Docs](https://docs.tigase.net)
* Existing issues in relevant project, for Tigase Server it's: [Tigase XMPP Server GitHub issues](https://github.com/tigase/tigase-server/issues)

If you didn't find an answer in the resources above, feel free to submit your question as [new issue on GitHub](https://github.com/tigase/tigase-server/issues/new/choose) or, if you have valid support subscription, open [new support ticket](https://tigase.net/technical-support).

# Downloads

You can download distribution version of the Tigase XMPP Server directly from [here](https://github.com/tigase/tigase-server/releases).

If you wish to downloand SNAPSHOT build of the development version of Tigase XMPP Server you can grab it from [here](https://build.tigase.net/nightlies/dists/latest/tigase-server-dist-max.zip).

# Installation and usage

Documentation of the project is part of the Tigase XMPP Server distribution package. Quickstart guide is also available [here](https://docs.tigase.net/en/latest/Tigase_Administration/Quick_Start_Guide/Intro.html).

# Compilation 

Compilation of the project is very easy as it is typical Maven project. All you need to do is to execute
````bash
mvn package test
````
to compile the project and run unit tests.

# License

<img alt="Tigase Tigase Logo" src="https://github.com/tigase/website-assets/blob/master/tigase/images/tigase-logo.png?raw=true" width="25"/> Official <a href="https://tigase.net/">Tigase</a> repository is available at: https://github.com/tigase/tigase-server/.

Copyright (c) 2004 Tigase, Inc.

Licensed under AGPL License Version 3. Other licensing options available upon request.
