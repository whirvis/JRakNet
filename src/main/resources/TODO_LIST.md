# To-do list
This is a list of things that are planned to be added to JRakNet for the next update

# JRakNet v2.0
The first release of JRakNet v2

[x] Server
	[x] Event system
	[x] Broadcast identifiers
		[x] Raw encoders
		[x] Custom encoders
	[x] Client connection
[x] Client
	[x] Event system
	[x] Server discovery
	[x] Server connection
[x] Protocol
	[x] Custom packets
		[x] Sending
		[x] Receiving
	[x] Encapsulated packets
		[x] Sending
		[x] Receiving
	[x] Acknowledgement packets
	[x] Login
		[x] Client
		[x] Server		
[x] Testing
	[x] Server test
	[x] Client test
	[x] Utilities test
	[x] Server discovery test
	[x] Latency detection test
	[x] Examples
		[x] Server example
		[x] Client example
		[x] Chat example
		
#JRakNet v2.0.1
A minor tweak update that will probably save some memory in the JVM and clean up the javadocs.

[ ] Session
	[ ] Bump nextSequenceNumber even if the packet is a lost packet that was resent
	[ ] When a lost packet is resent, remove it's record and set it to the bumped one
[ ] Documentation
	[ ] Remove all the annoying " - " from @param definitions