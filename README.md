# JRakNet
JRakNet is a networking library for Java which implements the UDP based protocol [RakNet](https://github.com/OculusVR/RakNet).
This library was meant to be used for Minecraft: Pocket Edition servers and clients, but can still be used to create game servers
and clients for other video games with ease.

# Task list
These are the features that are in-development or already finished, things can be added or removed at any time.

- [ ] Server
  - [ ] Event system
  - [x] Message identifiers
    - [x] Raw encoders
    - [x] Custom encoders
  - [ ] Client connection
- [ ] Client
- [ ] Protocol
  - [x] Custom packets (Sending)
  - [ ] Custom packets (Receiving)
  - [x] Encapsulated packets (Sending)
  - [ ] Encapsulated packets (Receiving)
  - [ ] Acknowledgement packets
  - [ ] Client login
  - [ ] Server login
  - [x] Server discovery
