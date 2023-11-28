# networks-project-2
Ben Cimini, Blair Bowen, Stetson King

### Tutorial
To prepare the application, extract the contents of its ZIP file to a safe location. In a
command line, change directory to that location. First, we'll launch the server. If using a
Windows machine, run the following commands; if not, run your machine's equivalent. Note,
you'll only need to do this once. A single server can sustain the application.
- javac MessageBoardServer.java compiles the *.java file to a *.class file.
- java MessageBoardServer executes the file.

Next, we'll launch a client. You may do this for as many clients as you desire. Again, if
using a Windows machine, run the following command; if not, run the appropriate equivalent.
A new command line is necessary for each client.
- py Client_Python.py executes the file.

Executing the client script will automatically connect you to the server. After entering a
username, you will be added to the public message board. At this point, you navigate the
application with a series of commands. Type 'HELP' to see them. Alternatively, they're
described below.
- Use PUBLICPOST to post in the public message board.
- Use JOIN to join any, or multiple, of 5 private groups.
- Use POST to post in a private group.
- Use GET to get the full contents of a post.
- Use MEMBERS to see the members in any group you have joined.
- Use HISTORY to see the recent chat history in any group you have joined.
- Use LEAVE to leave a group.
- Use DISCONNECT to disconnect from the server entirely.
- Use HELP to see these instructions!

There are a few more pieces of understanding to provide. First, the commands above aren't
case-sensitive, you may enter them however you like! Also, any message from the server will
be prefixed with dashes. Your chat feed, on the other hand, won't be. Messages in it are
displayed in the following format: message number, username, datetime, subject, group number.

### Challenges
The first major challenge we encountered in this project was getting started! Much of the
complexity, as far as sockets are concerned, came at the beginning of our work. Being
inexperienced socket programmers, we were confused. So, we referenced Project 1. It provided
effective examples for basic socket programming. The result was a framework which we could
build upon. With a client and server communicating, we needed only slight adjustments to
implement a first, basic message board. Those adjustments were informed by further research.
Afterward, we were ready to begin implementing the features spelled out in part 1.

Another bottleneck in our development was implementing private groups. There was, simply, a lot
of new code necessary to get the feature up-and-running. For example, new data structures: a
global map to represent groups, a global map of lists to represent group members, and local
lists to represent groups joined on a per-user basis to name a few. As such, the join method
itself is rather complicated. First of all, group entries are allowed by ID or name. To support
such behavior, each entered group must be resolved to an ID - such that the ID and name aren't
joined as if separate groups. Additional conditions for a group's existence and previous
membership must also be checked. In case of an empty group, an list must be dynamically added
to the members map. That tripped us up for a while, but the need was satisfied by a
computeIfAbsent and lambda function. Of course, after all that, we still had to isolate
messages by group. This required a structural change to the message class, adding a group ID
attribute. In response, we had to modify our post method and write a helper:
broadcastMessageInGroup. Essentially, it leveraged a list of clientWriters to send the output
through appropriate streams. All these solutions and more were necessary to make part 2 work!

The last challenge I'd like to talk about came from a slight misinterpretation of the project.
After we'd implemented part 2, part 1 was no longer available! That is, users could only
post in private groups. We had to implement part 1... again. Our solution for this was quite
clever. We included the public message board as a "hidden" group. It's hardcoded into the
groups map, but never shown to users. Rather, they automatically join it upon connection with
the server. Therefore, by default, any user can send a public message. Since it's a group,
though, users who don't wish to see public messages may still leave it. We ended up with a
better implementation on account of the misinterpretation, I believe. 