# JRakNet
JRakNet is a networking library for Java which implements the UDP based protocol [RakNet](https://github.com/OculusVR/RakNet).
This library was meant to be used for Minecraft: Pocket Edition servers and clients, but can still be used to create game servers and clients for other video games with ease.

| Protocol Info             | Version |
| --------------------------|:-------:|
| Current Protocol          | 8       |
| Supported Server Protocol | 8       |
| Supported Client Protocol | 8       |

# How to create a server

```java
// Create server and add hooks
RakNetServer server = new RakNetServer(19132, 10, "MCPE;A RakNet Server;80;0.15.0;0;10;" + RakNetUtils.getRakNetID());

// Client connected
server.addHook(Hook.SESSION_CONNECTED, (Object[] parameters) -> {
	RakNetSession session = (RakNetSession) parameters[0];
	System.out.println("Client from address " + session.getSocketAddress() + " has connected to the server");
});

// Client disconnected
server.addHook(Hook.SESSION_DISCONNECTED, (Object[] parameters) -> {
	RakNetSession session = (RakNetSession) parameters[0];
	String reason = parameters[1].toString();
	System.out.println("Client from address " + session.getSocketAddress() + " has disconnected from the server for the reason \"" + reason + "\"");
});

// Start server
server.start();	
```
A simple RakNet server, this can be tested using a Minecraft: Pocket Edition client. Simply launch the game and click on "Play", then go to the "Friends" tab. Then, "A RakNet Server" should pop up, just like when someone else is playing on the same network and their name pops up.


# How to create a client

```java
public static final String SERVER_ADDRESS = "sg.lbsg.net";
public static final int SERVER_PORT = 19132;

// Create client and add hooks
RakNetClient client = new RakNetClient();

// Server connected
client.addHook(Hook.SESSION_CONNECTED, (Object[] parameters) -> {
	RakNetSession session = (RakNetSession) parameters[0];
	System.out.println("Successfully connected to server with address " + session.getSocketAddress());
	client.disconnect();
});

// Server disconnected
client.addHook(Hook.SESSION_DISCONNECTED, (Object[] parameters) -> {
	RakNetSession session = (RakNetSession) parameters[0];
	String reason = parameters[1].toString();
	System.out.println("Successfully disconnected from server with address " + session.getSocketAddress() + " for the reason \"" + reason + "\"");
	System.exit(0);
});

// Attempt to connect to server
client.connect(new InetSocketAddress(SERVER_ADDRESS, SERVER_PORT));
```
A simple RakNet client, this example attempts to connect to the main [LBSG](http://lbsg.net/) server. When it is connected, it closes the connection and shuts down.

# How to contact
This project has a twitter page, [@JRakNet](https://twitter.com/JRakNet). There all github commits and releases are tweeted. There is also a G-Mail account, [jraknet@gmail.com](https://gmail.com) for anything related specifically to JRakNet :)

# Notes
Some DataPacket ID's are reserved by RakNet. Because of this, it is recommended that all game packets not relating to RakNet begin with their own special ID, Minecraft: Pocket Edition does this. It is also recommended that game servers and game clients do not use raw packets at all.

<img src="http://i.imgur.com/w0EZCZS.png" width="135" height="145">
