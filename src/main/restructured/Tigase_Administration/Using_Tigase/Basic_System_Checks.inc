.. _Basic-System-Checks:

Basic System Checks
----------------------

Previously, a configuration article is available about `Linux settings for high load systems <#linuxhighload>`__. This has a description of basic settings which are essential to successfully run XMPP service for hundreds or thousands of online users.

Of course, high load and high traffic systems require much more tuning and adjustments. If you use selinux you have to be careful as it can interfere with the service while it is under a high load. Also some firewall settings may cause problems as the system may decide it is under a DDOS attack and can start blocking incoming connections or throttle the traffic.

In any case, there are some basic checks to do every time you deploy XMPP service to make sure it will function properly. I am trying to keep the article mentioned above up to date and add all the settings and parameters I discover while working with different installations. *If you have some suggestions for different values or different parameters to add, please let me know.*

If you want to run a service on a few cluster nodes (5 or even 10), then manually checking every machine and adjusting these settings is time consuming and it is very easy to forget about.

To overcome this problem I started to work on a shell script which would run all the basic checks and report problems found. Ideally it should be also able to adjust some parameters for you.

Inside the Tigase server `scripts/ <https://github.com/tigase/tigase-server/blob/master/scripts/>`__ repository find a script called ``machine-check.sh``. It performs all the basic checks from the article and also tries to adjust them when necessary. Have a `look at the code <https://github.com/tigase/tigase-server/blob/master/scripts/machine-check.sh>`__ and run for yourself.

Any comments or suggestions, as usual, are very much appreciated.