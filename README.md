![apm](https://img.shields.io/apm/l/vim-mode.svg) [![Build Status](https://ci.codemc.org/job/JRakNet/job/JRakNet/badge/icon)](https://ci.codemc.org/job/JRakNet/job/JRakNet/)

# JRakNet
JRakNet is a networking library for Java which implements the UDP based protocol [RakNet](https://github.com/OculusVR/RakNet).
This library was meant to be used for Minecraft servers and clients, but can still be used to create game servers and clients for other video games with ease. You can also read the [JavaDocs](https://ci.codemc.org/job/JRakNet/job/JRakNet/javadoc/)

| Protocol Info             | Version |
| --------------------------|:-------:|
| Current protocol          | 10      |
| Supported server protocol | 10      |
| Supported client protocol | 10      |

# Notes
- Always use the newest version of JRakNet (with the exception of snapshots), including bug fix updates as they almost always fix major bugs, add new features, or have optimizations to make the API run faster. As a general rule, it is also not a good idea to fork this repository as it is almost always being updated like stated before. This means it is very possible for it to become out of date very quickly unless you are intending to create a new feature or fixing a bug to be merged back into the original repository through a pull request.
- Some data packet IDs are reserved by RakNet. Because of this, it is recommended that all game packets not relating to RakNet begin with their own special ID (add a byte at the beginning of all packets that is not used as an internal packet ID by RakNet). It is also recommended that game servers and game clients do not use raw packets (the Netty based functions) at all unless it is absolutely necessary.

# How to use with Maven
If you are using a release version, use this dependency:
```xml
<dependency>
    <groupId>com.whirvis</groupId>
    <artifactId>jraknet</artifactId>
    <version>2.12.3</version>
</dependency>
```

If you are wanting to use a snapshot version, use this repository and dependency:
```xml
<repository>
    <id>codemc-repo</id>
    <url>https://repo.codemc.org/repository/maven-public/</url>
</repository>
```
```xml
<dependency>
    <groupId>com.whirvis</groupId>
    <artifactId>jraknet</artifactId>
    <version>2.12.4-SNAPSHOT</version>
</dependency>
```

# How to create a server
Creating a server in JRakNet is extremely easy, all it takes to create one can be seen here:

```java
// Add loopback exemption for Minecraft
if (!UniversalWindowsProgram.MINECRAFT.setLoopbackExempt(true)) {
	System.err.println("Failed to add loopback exemption for Minecraft");
}

// Create server
RakNetServer server = new RakNetServer(19132, 10);
server.setIdentifier(new MinecraftIdentifier("JRakNet Example Server", 354, "1.11", 0, 10,
	server.getGloballyUniqueId(), "New World", "Survival"));

// Add listener
server.addListener(new RakNetServerListener() {

	// Client connected
	@Override
	public void onConnect(RakNetServer server, InetSocketAddress address, ConnectionType connectionType) {
		System.out.println("Client from address " + address + " has connected to the server");
	}
	
	// Client logged in
	@Override
	public void onLogin(RakNetServer server, RakNetClientPeer peer) {
		System.out.println("Client from address " + peer.getAddress() + " has logged in");
	}

	// Client disconnected
	@Override
	public void onDisconnect(RakNetServer server, InetSocketAddress address, RakNetClientPeer peer, String reason) {
		System.out.println("Client from address " + address
						+ " has disconnected from the server for reason \"" + reason + "\"");
	}

	// Packet received
	@Override
	public void handleMessage(RakNetServer server, RakNetClientPeer peer, RakNetPacket packet, int channel) {
		System.out.println("Client from address " + peer.getAddress() + " sent packet with ID "
				+ RakNet.toHexStringId(packet) + " on channel " + channel);
	}

});

// Start server
server.start();
```

This is a simple RakNet server that can be tested through Minecraft by going to the "Friends" tab where the server should show up. Once the server pops up, you should be able to click on it to trigger the connection and packet hooks.

# How to enable loopback exemption
On Windows 10, applications that use the Universal Windows Program framework by default are not able to connect to servers that are
running on the same machine as them. This annoying feature can be disabled by simply creating a ```UniversalWindowsProgram``` object with the first and only parameter being the ID of the application that you are wanting to be able to connect to. An example
would be Microsoft's Edge which is ```Microsoft.MicrosoftEdge_8wekyb3d8bbwe```. So, in order to enable loopback exemption, it would only take this:

```java
UniversalWindowsProgram microsoftEdge = new UniversalWindowsProgram("Microsoft.MicrosoftEdge_8wekyb3d8bbwe");
if(!microsoftEdge.setLoopbackExempt(true)) {
	System.err.println("Failed to enable loopback exemption for Microsoft Edge");
}
```

Simple, right? Feel free to implement this if you are running on a non-Windows 10 machine. This implementation was made specifically to work even if your machine does not run Windows 10 or does not have Windows PowerShell installed. Of course, if you are not on a Windows 10 machine with Windows PowerShell installed there really is no way to properly check if your application is loopback exempted. However, I'm sure that this can be solved with the help of a user that has Windows 10 with Windows PowerShell if needed.

# How to create a client
Creating a client in JRakNet is also very easy. The code required to create a client can be seen here:

```java
// Create client
RakNetClient client = new RakNetClient();
		
// Add listener
client.addListener(new RakNetClientListener() {

	// Connected to server
	@Override
	public void onConnect(RakNetClient client, InetSocketAddress address, ConnectionType connectionType) {
		System.out.println("Successfully connected to server with address " + address);
	}
	
	// Logged into server
	@Override
	public void onLogin(RakNetClient client, RakNetServerPeer peer) {
		System.out.println("Successfully logged into server");
		client.disconnect();
	}

	// Disconnected from server
	@Override
	public void onDisconnect(RakNetClient client, InetSocketAddress address, RakNetServerPeer peer, String reason) {
		System.out.println("Successfully disconnected from server with address " + address + " for reason \"" + reason + "\"");
	}

});

// Connect to server
client.connect("sg.lbsg.net", 19132);
```

This is a simple RakNet client that attempts to connect to the main [LBSG](https://lbsg.net/) server. When it is connected, it closes the connection.

<br>

<a href="http://whirvis.com"><img src="https://i.imgur.com/8c8FwFE.png" width="145" height="145"></a> <a href="https://github.com/JRakNet/JRakNet"><img src="https://i.imgur.com/CWCiNTP.png" width="145" height="145" hspace="50"></a> <a href="https://github.com/OculusVR/RakNet"><img src="https://i.imgur.com/nQo83J4.png" width="145" height="145"></a> <a href="https://www.oculus.com/"><img src="http://i.imgur.com/PmrfSsc.png" height="145" /></a>
