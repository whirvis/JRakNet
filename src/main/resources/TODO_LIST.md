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
		
#JRakNet v2.1.0
A minor tweak update that will probably save some memory in the JVM and clean up the javadocs.

[x] Session
	[x] Add onNotAcknowledge method
[x] Protocol
	[x] Fix ConnectionBanned (0x17) packet by adding server GUID (long) to it
	[x] Give packet's data inputs and outputs
[x] Documentation
	[x] Remove all the annoying " - " from @param definitions
	
#JRakNet v2.2.0
[x] Packet
	[x] Fix IPv6 reading/writing
[x] Identifiers
	[x] Fix MCPEIdentifier, timestamp should've been server GUID
	[x] Fix identifier inconsistencies between tests and examples
[x] Examples
	[x] Add client bundle example
	[x] Add server bundle example
	
#JRakNet v2.2.1
[ ] Session
	[ ] Instead of using pings and pongs, use DETECT_LOST_CONNECTIONS (0x14)
[ ] Tests
	[ ] Add acknowledgement test