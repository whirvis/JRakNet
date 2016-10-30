# JRakNet
JRakNet is a networking library for Java which implements the UDP based protocol [RakNet](https://github.com/OculusVR/RakNet).
This library was meant to be used for Minecraft: Pocket Edition servers and clients, but can still be used to create game servers
and clients for other video games with ease.

- [x] Server
  - [x] Event system
  - [x] Broadcast identifiers
    - [x] Raw encoders
    - [x] Custom encoders
  - [x] Client connection
- [x] Client
  - [x] Event system
  - [x] Server discovery
  - [x] Server connection
- [x] Protocol
  - [x] Custom packets
    - [x] Sending
    - [x] Receiving
  - [x] Encapsulated packets
    - [x] Sending
    - [x] Receiving
  - [x] Acknowledgement packets
  - [x] Login
    - [x] Client
    - [x] Server
- [ ] Test
  - [x] Server test
  - [x] Client test
  - [x] Utilities test
  - [x] Server discovery test
  - [x] Latency detection test
  - [ ] Examples
    - [x] Server example
    - [x] Client example
    - [ ] Chat example
