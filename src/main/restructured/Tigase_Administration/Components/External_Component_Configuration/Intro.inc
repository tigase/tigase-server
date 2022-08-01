External Component Configuration
-------------------------------------

Tigase can connect to external components, this guide will show you how this can be accomplished.

Configuration follows the same standards as all other components. It is also much more powerful as a single Tigase instance can control many TCP/IP ports and many external components on each port and even allows for multiple connections for the same component. It supports both XEP-0114 and XEP-0225 with protocol auto-detection mechanisms. Protocols are pluggable so more protocols can be supported or custom extensions to existing protocols can be added.

The implementation also supports a scripting API and new domains with passwords can be added at run-time using ad-hoc commands. New scripts can be loaded to even further control all connected external components.

Pages in this guide describe in details all the administration aspects of setting up and managing external components.

-  :ref:`External Component Configuration<External-Component-Configuration>`

-  :ref:`Tigase as an External Component<Tigase-as-an-External-Component>`

-  :ref:`Load Balancing External Components in Cluster Mode<Load-Balancing-External-Components-in-Cluster-Mode>`