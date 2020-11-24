<p align="center">
  <a href="https://tigase.net/">
    <img
      alt="Tigase MIX Component (Mediated Information eXchange)"
      src="https://github.com/tigaseinc/website-assets/raw/master/tigase/images/tigase-logo.png?raw=true"
      width="300"
    />
  </a>
</p>

<p align="center">
  The MIX (Mediated Information eXchange) component for Tigase XMPP Server.
</p>

<p align="center">
  <img alt="Tigase Logo" src="https://github.com/tigaseinc/website-assets/raw/master/tigase/images/tigase-logo.png?raw=true" width="25"/>
  <img src="https://tc.tigase.net/app/rest/builds/buildType:(id:TigaseMix_Build)/statusIcon" width="100"/>
</p>

# What it is

Tigase MIX Component is XMPP component (based on [Tigase PubSub Component](https://github.com/tigase/tigase-pubsub/)) for Tigase XMPP Server providing support for [XEP-0369: Mediated Information eXchange (MIX)](https://xmpp.org/extensions/xep-0369.html)

# Features

It provides support to Tigase XMPP Server for following features:
* [XEP-0369: Mediated Information eXchange (MIX)](https://xmpp.org/extensions/xep-0369.html)
* [XEP-0403: Mediated Information eXchange (MIX): Presence Support](https://xmpp.org/extensions/xep-0403.html) *(only support for relaying IQ stanzas)*
* [XEP-0406: Mediated Information eXchange (MIX): MIX Administration](https://xmpp.org/extensions/xep-0406.html)
* [XEP-0407: Mediated Information eXchange (MIX): Miscellaneous Capabilities](https://xmpp.org/extensions/xep-0407.html) *(only support for avatar publishing and message retraction)*
* [XEP-0408: Mediated Information eXchange (MIX): Co-existence with MUC](https://xmpp.org/extensions/xep-0408.html) *(same component under the same JID handles MIX and has basic support for MUC)*

# Downloads

You can download distribution version of Tigase XMPP Server which contains Tigase MIX Component directly from [here](https://github.com/tigaseinc/tigase-server/releases).

If you wish to downloand SNAPSHOT build of the development version of Tigase XMPP Server which contains Tigase MIX Component you can grab it from [here](https://build.tigase.net/nightlies/dists/latest/tigase-server-dist-max.zip).

# Compilation 

Compilation of the project is very easy as it is typical Maven project. All you need to do is to execute
````bash
mvn package test
````
to compile the project and run unit tests.

# License

<img alt="Tigase Tigase Logo" src="https://github.com/tigase/website-assets/blob/master/tigase/images/tigase-logo.png?raw=true" width="25"/> Official <a href="https://tigase.net/">Tigase</a> repository is available at: https://github.com/tigase/tigase-mix/.

Copyright (c) 2004 Tigase, Inc.

Licensed under AGPL License Version 3. Other licensing options available upon request.
