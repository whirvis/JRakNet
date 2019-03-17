WaifUPnP - Version 1.1
http://fdossena.com/?p=waifupnp/index.frag

UPnP Port Forwarding for Java couldn't be any easier!
WaifUPnP is an extermely lightweight Java library that allows you to:
	-open/close TCP/UDP ports
	-check if there's an UPnP router available
	-check if a port is already mapped
With LITERALLY 1 LINE OF CODE, as it should be!
It's as easy as
	UPnP.openTCP(<port number here>);

Usage Examples are included.

Usage:
-Import WaifUPnP.jar into your application
-Optionally, import WaifUPnP-javadoc.jar if you need JavaDoc
WaifUPnP-sources.jar contains the source code and should not be imported into your project

Compatibility:
Java 6 and newer

Limitations:
WaifUPnP is a very basic implementation of UPnP, that only scans for the default gateway, and can only open/close ports.
While this is enough for most people, if you need a full implementation of UPnP, you should take a look at Cling (http://4thline.org/projects/cling/)

License:
GNU LGPL v2.1 or newer
