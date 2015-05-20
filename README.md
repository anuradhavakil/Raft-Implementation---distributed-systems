# Raft-Implementation---distributed-systems
Raft algorithm implemented in Java for building Snapchat like application


This folder has only some part of complete project.
The main objective of the project was to develop Snapchat like application in order to send images within clusters.

The
basic
workflow
is:
A
client
registered
to
a
server
sends
an
image
over
the
network.
The
leader
of
the
cluster
elected
using
the
Raft
Algorithm
sends
this
image
to
all
the
other
clusters
i.e.
broadcasts
the
message.
The
leader
from
other
clusters
broadcasts
the
image
to
its
clients.

Technologies involved:
-NETTY
-PROTOBUF
-JAVA


