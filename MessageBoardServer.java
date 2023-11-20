import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;

public final class MessageBoardServer {
    private static final ArrayList<PrintWriter> clientWriters = new ArrayList<>();
    private static final Map<Integer, Message> messages = new HashMap<>();
    private static final Set<String> activeUsers = Collections.synchronizedSet(new HashSet<>());
    private static int messageID = 0;
    private static final Map<String, String> groups = new HashMap<>();
    private static final Map<String, ArrayList<String>> members = new HashMap<>();
    private static final Map<String, Set<PrintWriter>> groupClients = new HashMap<>();

    public static void main(String[] args) {
        // Hardcode 5 groups
        groups.put("1", "Bengals Fans");
        groups.put("2", "Jujutsu Kaisen Fans");
        groups.put("3", "Lunch Enjoyers");
        groups.put("4", "Classical Studies Discourse");
        groups.put("5", "Stories of Jury Duty");

        int serverPort = 42069; // Example port

        try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
            System.out.println("Message Board Server started on port " + serverPort);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                clientWriters.add(out);
                ClientHandler clientHandler = new ClientHandler(clientSocket, out);
                Thread clientThread = new Thread(clientHandler);
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private PrintWriter out;
        private String username;
        private ArrayList<String> groupsJoined = new ArrayList<>();

        public ClientHandler(Socket clientSocket, PrintWriter out) {
            this.clientSocket = clientSocket;
            this.out = out;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));) {
                this.username = in.readLine();
                activeUsers.add(username);

                sendGroupList();
                out.println("");

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("JOIN:")) {
                        joinGroup(inputLine.substring(5));

                        // Notify joined groups.
                        joinLeaveNotifs(username, "joined", groupsJoined);

                        // Show user members of their groups.
                        for (String group : groupsJoined) {
                            out.printf("Members of group %s: ", group);
                            out.println(getMembers(group));
                        }
                        out.println("");
                    } else if (inputLine.startsWith("POST_MESSAGE:")) {
                        postMessage(inputLine.substring(13));
                    } else if (inputLine.startsWith("GET_MESSAGE:")) {
                        String messageId = inputLine.substring(12);
                        getMessage(Integer.parseInt(messageId), out);
                    }
                }
            } catch (IOException e) {
                System.out.println("ClientHandler exception: " + e.getMessage());
            } finally {
                if (username != null && !username.isEmpty()) {
                    activeUsers.remove(username);
                }
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println("Socket close exception: " + e.getMessage());
                }
                joinLeaveNotifs(username, "left", groupsJoined);
                clientWriters.remove(out);
            }
        }

        private void sendGroupList() {
            out.println("Available Groups:");
            for (Map.Entry<String, String> groupEntry : groups.entrySet()) {
                out.println("Group ID: " + groupEntry.getKey() + ", Name: " + groupEntry.getValue());
            }
        }

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

                    // Send the last two messages of this group to the client
                    sendLastTwoMessages(cleanGroup);
                }
            }
        }

        private void sendLastTwoMessages(String groupId) {
            List<Message> groupMessages = messages.values().stream()
                    .filter(m -> members.get(groupId).contains(m.sender))
                    .sorted(Comparator.comparingInt(m -> m.id))
                    .collect(Collectors.toList());

            // Get the last two messages
            int messageCount = groupMessages.size();
            for (int i = Math.max(messageCount - 2, 0); i < messageCount; i++) {
                Message message = groupMessages.get(i);
                out.println(message.getDisplayString());
            }
        }

        private void postMessage(String messageData) {
            String[] parts = messageData.split(":");
            if (parts.length < 3)
                return;

            String groupId = parts[0];
            String subject = parts[1];
            String content = parts[2];

            if (groupsJoined.contains(groupId)) {
                int messageId = messageID++;
                Message newMessage = new Message(messageId, username, subject, content);
                messages.put(messageId, newMessage);
                broadcastMessageInGroup(newMessage, groupId);
            }
        }

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

        private void broadcastMessageInGroup(Message message, String groupId) {
            Set<PrintWriter> clientsInGroup = groupClients.get(groupId);
            if (clientsInGroup != null) {
                for (PrintWriter clientWriter : clientsInGroup) {
                    clientWriter.println(message.getDisplayString());
                }
            }
        }

        private void joinLeaveNotifs(String user, String action, ArrayList<String> groupsAffected) {
            for (String groupAffected : groupsAffected) {
                // Set notification string.
                String notif = "User '" + user + "' " + action + " group " + groupAffected;

                // Notify affected group.
                Set<PrintWriter> clientsInGroup = groupClients.get(groupAffected);
                if (clientsInGroup != null) {
                    for (PrintWriter clientWriter : clientsInGroup) {
                        clientWriter.println(notif);
                    }
                }
            }
        }

        private String getMembers(String groupId) {
            return String.join(", ", members.get(groupId));
        }
    }

    static class Message {
        int id;
        String sender;
        String date;
        String subject;
        String content;

        public Message(int id, String sender, String subject, String content) {
            this.id = id;
            this.sender = sender;
            this.subject = subject;
            this.content = content;
            this.date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        }

        public String getDisplayString() {
            return id + ", " + sender + ", " + date + ", " + subject;
        }

        @Override
        public String toString() {
            return "Content: " + content;
        }
    }
}
