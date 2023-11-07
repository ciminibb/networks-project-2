# Project Overview
Make a message board with a basic client-server implementation. Some high-level objectives are thorough
documentation, pure unicast sockets, and no library assistance for networking.

### Part 1
Include the following features.
- All users belong to one public group.
- The server keeps track of all users that join or leave the group.
- When a user joins or leaves the group, all others get notified.
- When a user joins the group, they can see only two previous messages.
- A list of members is displayed to new users.

Note that messages should be displayed as "ID, Sender, Date, Subject." Content is retrieved by
providing an ID to the server.

### Part 2
Extend the following features.
- Users can join multiple private groups.
- Once a user connects, the server displays a list of groups.
- The user selects a group to join by name or number.
- Multiple groups can be joined at once.

Note that users cannot see messages from groups they haven't joined.
