![apm](https://img.shields.io/apm/l/vim-mode.svg) [![Build Status](https://ci.codemc.org/job/JRakNet/job/JRakNet/badge/icon)](https://ci.codemc.org/job/JRakNet/job/JRakNet/)

# JRakNet
JRakNet is a networking library for Java which implements the UDP based protocol [RakNet](https://github.com/OculusVR/RakNet).
This library was meant to be used for Minecraft servers and clients, but can still be used to create game servers and clients for other video games with ease. You can also read the [JavaDocs](https://ci.codemc.org/job/JRakNet/job/JRakNet/javadoc/)

| Protocol Info             | Version |
| --------------------------|:-------:|
| Current protocol          | 9       |
| Supported server protocol | 9       |
| Supported client protocol | 9       |

# Notes
- Always use the newest version of JRakNet (with the exception of snapshots), including bug fix updates as they almost always fix major bugs, add new features, or have optimizations to make the API run faster. As a general rule, it is also not a good idea to fork this repository as it is almost always being updated like stated before. This means it is very possible for it to become out of date very quickly unless you are intending to create a new feature or fixing a bug to be merged back into the original repository through a pull request.
- Some data packet IDs are reserved by RakNet. Because of this, it is recommended that all game packets not relating to RakNet begin with their own special ID (add a byte at the beginning of all packets that is not used as an internal packet ID by RakNet). It is also recommended that game servers and game clients do not use raw packets (the Netty based functions) at all unless it is absolutely necessary.

# How to use with Maven
If you are using a release version, use this dependency:
```xml
<dependency>
    <groupId>com.whirvis</groupId>
    <artifactId>jraknet</artifactId>
    <version>2.10.5</version>
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
    <version>2.10.6-SNAPSHOT</version>
</dependency>
```

# How to create a server
Creating a server in JRakNet is extremely easy, all it takes to create one can be seen right here

```java
// Add loopback exemption for Minecraft
if (!UniversalWindowsProgram.MINECRAFT.addLoopbackExempt()) {
	log.warn("Failed to add loopback exemption for Minecraft");
}

// Create server
RakNetServer server = new RakNetServer(19132, 10,
		new MinecraftIdentifier("JRakNet Example Server", 137, "1.2", 0, 10,
				new Random().nextLong() /* Server broadcast ID */, "New World", "Survival"));

// Add listener
server.addListener(new RakNetServerListener() {

	// Client connected
	@Override
	public void onClientConnect(RakNetClientSession session) {
		System.out.println("Client from address " + session.getAddress() + " has connected to the server");
	}

	// Client disconnected
	@Override
	public void onClientDisconnect(RakNetClientSession session, String reason) {
		System.out.println("Client from address " + session.getAddress()
				+ " has disconnected from the server for the reason \"" + reason + "\"");
	}

	// Packet received
	@Override
	public void handleMessage(RakNetClientSession session, RakNetPacket packet, int channel) {
		System.out.println("Client from address " + session.getAddress() + " sent packet with ID "
				+ RakNetUtils.toHexStringId(packet) + " on channel " + channel);
	}

});

// Start server
server.start();
```

This is a simple RakNet server that can be tested through Minecraft by going to the "Friends" tab where the server should show up. Once
the server pops up, you should be able to click on it to trigger the connection and packet hooks.

# How to enable loopback exemption
On Windows 10, applications that use the Universal Windows Program framework by default are not able to connect to servers that are
running on the same machine as them. This annoying feature can be disabled by simply creating a ```UniversalWindowsProgram``` object
with the first and only parameter being the ID of the application that you are wanting to be able to connect to. An example
would be Microsoft's Edge which is ```Microsoft.MicrosoftEdge_8wekyb3d8bbwe```. So, in order to enable loopback exemption, it would only
take this:

```java
UniversalWindowsProgram MICROSOFT_EDGE = new UniversalWindowsProgram("Microsoft.MicrosoftEdge_8wekyb3d8bbwe");
if(!MICROSOFT_EDGE.addLoopbackExemption()) {
	System.out.println("Warning: Microsoft Edge is not loopback exempt!"); // It is good practice to make sure that the application managed to become loopback exempted
}
```

Simple, right? Feel free to implement this if you are running on a non-Windows 10 machine. This implementation was made specifically to
work even if your machine does not run Windows 10 or does not have Windows PowerShell installed. Of course, if you are not on a Windows
10 machine with Windows PowerShell installed there really is no way to properly check if your application is loopback exempted. However,
I'm sure that this can be solved with the help of a user that has Windows 10 with Windows PowerShell if needed.

# How to create a client
Creating a client in JRakNet is also very easy. The code required to create a client can be seen here

```java
// Server address and port
String SERVER_ADDRESS = "sg.lbsg.net";
int SERVER_PORT = 19132;

// Create client
RakNetClient client = new RakNetClient();
		
// Add listener
client.addListener(new RakNetClientListener() {

	// Server connected
	@Override
	public void onConnect(RakNetServerSession session) {
		System.out.println("Successfully connected to server with address " + session.getAddress());
		client.disconnect();
	}

	// Server disconnected
	@Override
	public void onDisconnect(RakNetServerSession session, String reason) {
		System.out.println("Successfully disconnected from server with address " + session.getAddress()
				+ " for the reason \"" + reason + "\"");
		client.shutdown();
	}

});

// Connect to server
client.connect(SERVER_ADDRESS, SERVER_PORT);
```

This is a simple RakNet client that attempts to connect to the main [LBSG](https://lbsg.net/) server. When it is connected, it closes
the connection and shuts down.

<br>

<a href="http://whirvis.com"><img src="https://i.imgur.com/HFnmCzb.png" width="145" height="145"></a> <a href="https://github.com/JRakNet/JRakNet"><img src="https://i.imgur.com/heiZXpr.png" width="145" height="145" hspace="50"></a> <a href="https://github.com/OculusVR/RakNet"><img src="http://imgur.com/9p1asD8.png" width="145" height="145"></a> <a href="https://www.oculus.com/"><img src="http://i.imgur.com/PmrfSsc.png" height="145" /></a>
