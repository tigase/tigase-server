Tigase XMPP Server 8.3.0 Change notes
-------------------------------------

Major Changes
^^^^^^^^^^^^^

All Minor Features & Behavior Changes
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

* Increased network socket buffer from 2K to 4K to improve performance when reading data from socket. It can increase somewhat memory usage proportionally to number of concurrent user connections. It's possible to configure size of this buffer using `socket-buffer-size` property - please see documentation.