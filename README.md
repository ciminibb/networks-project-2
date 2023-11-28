# networks-project-2
Ben Cimini, Blair Bowen, Stetson King

### Tutorial
Instructions for running the application go here!

### Challenges
The first major challenge we encountered in this project was getting started! Much of the
complexity, as far as sockets are concerned, came at the beginning of our work. Being
inexperienced socket programmers, we were confused. So, we referenced Project 1. It provided
effective examples for basic socket programming. The result was a framework which we could
build upon. With a client and server communicating, we needed only slight adjustments to
implement a first, basic message board. Those adjustments were informed by further research.
Afterward, we were ready to begin implementing the features spelled out in part 1.
nbsp;

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