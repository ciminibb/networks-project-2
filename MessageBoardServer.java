import java.io.*;
import java.net.*;
import java.util.*;
// import java.util.stream.Collectors;
import java.text.SimpleDateFormat;

public final class MessageBoardServer {
    // Define data structures to manage state.
    private static final ArrayList<PrintWriter> clientWriters = new ArrayList<>(); // Store client writers
    private static final Map<Integer, Message> messages = new HashMap<>(); // Store messages by ID
    private static final Set<String> activeUsers = Collections.synchronizedSet(new HashSet<>()); // Store active users
    private static int messageID = 0; // Counter for message IDs
    private static final Map<String, String> groups = new HashMap<>(); // Store group IDs and names
    private static final Map<String, ArrayList<String>> members = new HashMap<>(); // Store group membership
    private static final Map<String, Set<PrintWriter>> groupClients = new HashMap<>(); // Store clients in each group

    // Main method is where the server application begins.
    public static void main(String[] args) {
        // Hardcode 5 groups + the default, public group.
        groups.put("0", "Message Board");
        groups.put("1", "Bengals Fans");
        groups.put("2", "Jujutsu Kaisen Fans");
        groups.put("3", "Lunch Enjoyers");
        groups.put("4", "Classical Studies Discourse");
        groups.put("5", "Stories of Jury Duty");

        int serverPort = 42069; // Example port

        try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
            System.out.println("-- Message Board Server started on port " + serverPort);

            // Continuously listen for, and accept, incoming client connections.
            while (true) {
                Socket clientSocket = serverSocket.accept();

                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                clientWriters.add(out); // Add client writer to the list
                ClientHandler clientHandler = new ClientHandler(clientSocket, out);
                Thread clientThread = new Thread(clientHandler);
                clientThread.start(); // Start handling client in a separate thread
            }
        } catch (IOException e) {
            e.printStackTrace(); // Print exceptions in cases where they appear
        }
    }

    // This class is called to handle individual client interactions.
    static class ClientHandler implements Runnable {
        // Member variables for handling client interactions.
        private Socket clientSocket;
        private PrintWriter out;
        private String username;
        private ArrayList<String> groupsJoined = new ArrayList<>();

        // Constructor.
        public ClientHandler(Socket clientSocket, PrintWriter out) {
            this.clientSocket = clientSocket;
            this.out = out;
        }

        // Override run method for handling client interactions.
        @Override
        public void run() {
            try (
                // Finally, we define the code for handling client interactions.
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));) {
                this.username = in.readLine();
                activeUsers.add(username);

                // All users are added to the public group upon connecting to the server.
                joinGroup("0");

                // Send the user introductory materials to get them oriented in the application.
                out.println("");
                sendGroupList();
                out.println("");
                out.println("-- Type 'HELP' to see the instruction set!");
                out.println("");

                // The following conditional structure communicates with the client to invoke
                // necessary methods, given client inputs.
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("JOIN:")) {
                        joinGroup(inputLine.substring(5));
                    } else if (inputLine.startsWith("POST_MESSAGE:")) {
                        postMessage(inputLine.substring(13));
                    } else if (inputLine.startsWith("GET_MESSAGE:")) {
                        String messageId = inputLine.substring(12);
                        getMessage(Integer.parseInt(messageId), out);
                    } else if (inputLine.startsWith("HELP")) {
                        printInstructions();
                    } else if (inputLine.startsWith("LEAVE:")) {
                        leaveGroup(inputLine.substring(6));
                    } else if (inputLine.startsWith("MEMBERS:")) {
                        sendMemberList(inputLine.substring(8));
                    } else if (inputLine.startsWith("HISTORY:")) {
                        sendLastTwoMessages(inputLine.substring(8));
                    } else if (inputLine.startsWith("DISCONNECT")) {
                        out.println("-- Disconnecting from the server.");
                        for (String group : groupsJoined) {
                            leaveGroup(group);
                        }
                        clientWriters.remove(out);
                        break; // Break the loop to exit the client handling thread
                    }
                }
            } catch (IOException e) {
                System.out.println("ClientHandler exception: " + e.getMessage());
            } finally {
                // At this point, we clean and close resources.
                if (username != null && !username.isEmpty()) {
                    activeUsers.remove(username);
                }
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println("Socket close exception: " + e.getMessage());
                }
                for (String group : groupsJoined) {
                    leaveGroup(group);
                }
                clientWriters.remove(out);
            }
        }

        // This method prints the available groups to a client.
        private void sendGroupList() {
            out.println("-- Available Groups:");
            for (Map.Entry<String, String> groupEntry : groups.entrySet()) {
                // Output all but the default, public group.
                if (groupEntry.getKey() != "0") {
                    out.println("-- Group ID: " + groupEntry.getKey() + ", Name: " + groupEntry.getValue());
                }
            }
        }

        // This method prints the members of a specific group to the client.
        private void sendMemberList(String groupId) {
            if (!members.containsKey(groupId)) {
                out.printf("-- Users in group %s: empty\n", groupId);
                return;
            }

            // Store the list of members locally, so it can be changed.
            ArrayList<String> memberList = new ArrayList<>(members.get(groupId));

            // Remove current user from the list. They don't need to see themselves in it.
            memberList.remove(username);

            // Account for the fact that any given user may be the first.
            String memberString = String.join(", ", memberList);

            if (memberList.isEmpty()) {
                memberString = "only you";
            }

            // Output the members.
            if (groupId == "0") {
                out.printf("-- Users in the message board: %s\n", memberString);
            } else {
                out.printf("-- Users in group %s: %s\n", groupId, memberString);
            }
        }

        // This method allows users to join multiple private groups, should they choose to.
        private void joinGroup(String groupsToJoinString) {
            // Move each comma-separated group to an array element.
            String[] groupsToJoinArray = groupsToJoinString.split(",");

            for (String dirtyGroup : groupsToJoinArray) {
                // "Clean" groups by removing whitespace.
                String cleanGroup = dirtyGroup.trim();

                // Resolve each <cleanGroup> to an ID. In the process, monitor whether the
                // <cleanGroup> actually exists.
                boolean groupExists = false;
                if (groups.containsKey(cleanGroup)) {
                    groupExists = true;
                }
                if (groups.containsValue(cleanGroup)) {
                    groupExists = true;

                    // Resolve <cleanGroup> to ID.
                    for (Map.Entry<String, String> group : groups.entrySet()) {
                        if (cleanGroup.equals(group.getValue())) {
                            cleanGroup = group.getKey();
                        }
                    }
                }

                // Users can only join an existing group that they aren't already in.
                if (groupExists && !groupsJoined.contains(cleanGroup)) {
                    groupsJoined.add(cleanGroup);

                    // Add user to <members> for the group. If no mapping exists for the group,
                    // this code will create one via a lambda function.
                    members.computeIfAbsent(cleanGroup, k -> new ArrayList<>()).add(username);
                    groupClients.computeIfAbsent(cleanGroup, k -> new HashSet<>()).add(out);

                    // Display the group's members to the user.
                    sendMemberList(cleanGroup);
                    // Display the group's recent activity to the user. This recent activity
                    // should include the last two messages and notification of them joining.
                    out.println("-- History:");
                    sendLastTwoMessages(cleanGroup);
                    joinLeaveNotif(username, "joined", cleanGroup);
                }
            }
        }

        // This method allows users to leave multiple private groups, should they choose to.
        private void leaveGroup(String groupsToLeaveString) {
            // Move each comma-separated group to an array element.
            String[] groupsToLeaveArray = groupsToLeaveString.split(",");

            boolean allGroupsValid = true;
            for (String dirtyGroup : groupsToLeaveArray) {
                // "Clean" groups by removing whitespace.
                String cleanGroup = dirtyGroup.trim();

                // Resolve each <cleanGroup> to an ID. In the process, monitor whether the
                // <cleanGroup> actually exists.
                boolean groupExists = false;
                if (groups.containsKey(cleanGroup)) {
                    groupExists = true;
                }
                if (groups.containsValue(cleanGroup)) {
                    groupExists = true;

                    // Resolve <cleanGroup> to ID.
                    for (Map.Entry<String, String> group : groups.entrySet()) {
                        if (cleanGroup.equals(group.getValue())) {
                            cleanGroup = group.getKey();
                        }
                    }
                }

                // Users can only leave an existing group that they are in.
                if (groupExists && groupsJoined.contains(cleanGroup)) {
                    groupsJoined.remove(cleanGroup);

                    // Remove user from the group's members and clients.
                    members.get(cleanGroup).remove(username);
                    groupClients.get(cleanGroup).remove(out);

                    // Remove group from user's joined groups.
                    groupsJoined.remove(cleanGroup);

                    // Notify other members of the user leaving.
                    joinLeaveNotif(username, "left", cleanGroup);
                } else {
                    allGroupsValid = false;
                }
            }

            // Wrap-up.
            if (allGroupsValid) {
                out.println("-- Successfully left groups " + groupsToLeaveString);
            } else {
                out.println("-- Cannot leave unjoined groups.");
            }
        }

        // This method is used to retrieve message history from a group.
        private void sendLastTwoMessages(String groupId) {
            List<String> groupMembers = members.get(groupId);
            if (groupMembers == null || !groupMembers.contains(username)) {
                out.println("Join group to access or group does not exist.");
                return;
            } else {

                int messageCount = messages.size();
                int numberOfMessages = 0;

                // Get the last two messages
                for (int i = messageCount; i > 0; i--) {
                    Message message = messages.get(i - 1);
                    if (numberOfMessages == 2) {
                        break;
                    } else if (groupId.equals(message.groupId)) {
                        out.println(message.getDisplayString());
                        numberOfMessages++;
                    }
                }
            }
        }

        // This method posts a message and can be invoked on either public or private groups.
        private void postMessage(String messageData) {
            String[] parts = messageData.split(":");
            if (parts.length < 3)
                return;

            String groupId = parts[0];
            String subject = parts[1];
            String content = parts[2];

            if (groupsJoined.contains(groupId)) {
                int messageId = messageID++;
                Message newMessage = new Message(messageId, username, subject, content, groupId);
                messages.put(messageId, newMessage);
                broadcastMessageInGroup(newMessage, groupId);
            } else {
                out.println("Error: Unable to post. The group does not exist or you are not a member. Please join the group or verify the group's existence before posting.");
            }
        }

        // This method retrieves the contents of a message given its ID.
        private void getMessage(int messageId, PrintWriter out) {
            if (messages.containsKey(messageId)) {
                Message msg = messages.get(messageId);
                for (String groupId : groupsJoined) {
                    if (members.get(groupId).contains(msg.sender)) {
                        // Send the complete message details
                        out.println("Content: " + msg.content);
                        break;
                    }
                }
            }
        }

        // This method sends a message to only a specific group of users, those in a certain group.
        private void broadcastMessageInGroup(Message message, String groupId) {
            Set<PrintWriter> clientsInGroup = groupClients.get(groupId);
            if (clientsInGroup != null) {
                for (PrintWriter clientWriter : clientsInGroup) {
                    clientWriter.println(message.getDisplayString());
                }
            }
        }

        // This method notifies all relevant users of somebody having joined or left their group.
        private void joinLeaveNotif(String user, String action, String groupId) {
            // Set notification string.
            String notif = "User '" + user + "' " + action + " group " + groupId;

            // Special notification when joining/leaving the public group.
            if (groupId == "0") {
                notif = "User '" + user + "' " + action + " the message board.";
            }

            // Notify affected group.
            Set<PrintWriter> clientsInGroup = groupClients.get(groupId);
            if (clientsInGroup != null) {
                for (PrintWriter clientWriter : clientsInGroup) {
                    clientWriter.println(notif);
                }
            }
        }

        // This method prints the application's instructions to the screen.
        private void printInstructions() {
            out.println("-- Type 'PUBLICPOST' to post on the public message board.");
            out.println("-- Type 'JOIN' to join a private group.");
            out.println("-- Type 'POST' to post on a private group.");
            out.println("-- Type 'GET' to get the contents of a post.");
            out.println("-- Type 'MEMBERS' to see the members of a group.");
            out.println("-- Type 'HISTORY' to see the chat history of a group.");
            out.println("-- Type 'LEAVE' to leave a private group.");
            out.println("-- Type 'DISCONNECT' to disconnect from the server.");
            out.println("-- Type 'HELP' to see these instructions again!");
        }
    }

    // The <Message> class is an ADT representing a post to the message board.
    static class Message {
        // Attributes and constructor for the <Message> class.
        int id;
        String sender;
        String date;
        String subject;
        String content;
        String groupId;

        public Message(int id, String sender, String subject, String content, String groupId) {
            this.id = id;
            this.sender = sender;
            this.subject = subject;
            this.content = content;
            this.date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            this.groupId = groupId;
        }

        // Method to get a formatted string representation of a message.
        public String getDisplayString() {
            return id + ", " + sender + ", " + date + ", " + subject + ", " + groupId;
        }

        // Override toString method for the <Message> class.
        @Override
        public String toString() {
            return "Content: " + content;
        }
    }
}
