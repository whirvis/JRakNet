# JRakNet
JRakNet is a networking library for Java which implements the UDP based protocol [RakNet](https://github.com/OculusVR/RakNet).
This library was meant to be used for Minecraft servers and clients, but can still be used to create game servers and clients for other video games with ease. You can also read the [JavaDocs](http://htmlpreview.github.io/?https://github.com/JRakNet/JRakNet/blob/master/doc/index.html)

| Protocol Info             | Version |
| --------------------------|:-------:|
| Current protocol          | 8       |
| Supported server protocol | 8       |
| Supported client protocol | 8       |

# Notes
- Always use the newest version of JRakNet, including bug fix updates as they almost always fix major bugs, add new features, or have optimizations to make the API run faster. As a general rule, it is also not a good idea to fork this repository as it is almost always being updated like stated before. This means it is very possible for it to become out of date very quickly unless you are intending to create a new feature or fixing a bug to be merged back into the original repository through a pull request.
- Some data packet IDs are reserved by RakNet. Because of this, it is recommended that all game packets not relating to RakNet begin with their own special ID (add a byte at the beginning of all packets that is not used as an internal packet ID by RakNet). It is also recommended that game servers and game clients do not use raw packets (the netty based functions) at all unless it is absolutely necessary.
- Since people are always creating issues on the repo about this, Minecraft clients do not work with JRakNet servers if you are on the same machine as the server. As a result, you will have to use another device (iPhone, Android, XBOX One, etc.) due to an error on Mojang's part. However, JRakNet clients work fine with Minecraft servers running on the same machine.

# How to use with Maven
In order to add this project to your maven project, you will need to add the maven repository and then the actual dependency:
```xml
<repositories>
  <repository>
    <id>maven-repo</id>
    <url>https://raw.githubusercontent.com/JRakNet/MavenRepository/master</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.whirvis</groupId>
    <artifactId>jraknet</artifactId>
    <version>2.9.1</version>
  </dependency>
</dependencies>
```

# How to create a server
Creating a server in JRakNet is extremely easy, all it takes to create one can be seen right here

```java
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

This is a simple RakNet server that can be tested through Minecraft by going to the "Friends" tab where the server should show up. Once the server pops up, you should be able to click on it to trigger the connection and packet hooks.

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

This is a simple RakNet client that attempts to connect to the main [LBSG](http://lbsg.net/) server. When it is connected, it closes the connection and shuts down.

<br>

<a href="http://whirvis.com"><img src="http://i.imgur.com/LhUiCjL.png" width="135" height="145"></a> <a href="https://github.com/JRakNet/JRakNet"><img src="https://i.imgur.com/heiZXpr.png" width="135" height="145" hspace="50"></a> <a href="https://github.com/OculusVR/RakNet"><img src="http://imgur.com/9p1asD8.png" width="135" height="145"></a> <a href="https://www.oculus.com/"><img src="http://i.imgur.com/PmrfSsc.png" height="145" /></a>
