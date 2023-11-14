import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;

public final class MessageBoardServer {
    private static final ArrayList<PrintWriter> clientWriters = new ArrayList<>();
    private static final Map<Integer, Message> messages = new HashMap<>();
    private static final Set<String> activeUsers = Collections.synchronizedSet(new HashSet<>());
    private static int messageID = 0;
    private static final Map<String, String> groups = new HashMap<>(); // Key/value pairs representing
                                                                       // groups by ID and name. IDs
                                                                       // are strings to join method
                                                                       // simpler.

    public static void main(String[] args) {
        // Hard code 5 groups with the map's put method.
        groups.put("1", "Bengals Fans");
        groups.put("2", "Jujutsu Kaisen Fans");
        groups.put("3", "Lunch Enjoyers");
        groups.put("4", "Classical Studies Discourse");
        groups.put("5", "Stories of Jury Duty"); // Changing these is off limits
        
        int serverPort = 42069; // nice

        try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
            System.out.println("Message Board Server started on port " + serverPort);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                clientWriters.add(out);
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                Thread clientThread = new Thread(clientHandler);
                clientThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler extends Thread {
        private final Socket clientSocket;
        private final PrintWriter out;
        private String username;
        private ArrayList<String> groupsJoined = new ArrayList<>(); // A list, local to each thread
                                                                    // (user), that holds the groups
                                                                    // joined

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Error creating client writer.");
            }
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                // Show new user all groups and prompt them to join some.
                out.println("Enter groups to join, as a comma-separated list, from those below.");
                for (Map.Entry<String, String> group : groups.entrySet()) {
                    out.println(group.getKey() + " " + group.getValue());
                }

                out.println("");

                // Process the groups to be joined.
                String groupsToJoin = in.readLine();
                join(groupsToJoin);

                for (String group : groupsJoined) {
                    out.println(group);
                }
                
                // Show new user all active members.
                if (activeUsers.size() != 0) { // Skip if no active users
                    out.println("Active users:");
                    for (String user : activeUsers) {
                        out.println(user);
                    }

                    out.println("");
                }

                // Show new user the last 2 messages.
                if (messages.size() != 0) { // Skip if no messages
                    out.println("Chat history:");
                    for (int i = 1; i > -1; i--) {
                        out.println(messages.get(messages.size() - i).getDisplayString());
                    }

                    out.println("");
                }
                
                username = in.readLine();
                activeUsers.add(username);
                broadcastUserStatus(username, "joined");

                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("GET_MESSAGE:")) {
                        int requestedID = Integer.parseInt(line.split(":")[1]);
                        Message requestedMessage = messages.get(requestedID);
                        if (requestedMessage != null) {
                            out.println(requestedMessage);
                        } else {
                            out.println("Message not found");
                        }
                    } else {
                        String subject = line;
                        String content = in.readLine();
                        Message message = new Message(++messageID, username, subject, content);
                        messages.put(messageID, message);
                        broadcastMessage(message.getDisplayString());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                activeUsers.remove(username);
                broadcastUserStatus(username, "left");
                clientWriters.remove(out);
            }
        }

        private void broadcastMessage(String message) {
            for (PrintWriter client : clientWriters) {
                client.println(message);
            }
        }

        private void broadcastUserStatus(String username, String status) {
            String statusMessage = "User '" + username + "' " + status + " the group.";
            System.out.println(statusMessage);
            for (PrintWriter client : clientWriters) {
                client.println(statusMessage);
            }
        }

        private void join(String groupsToJoinString) {
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
                        if (group.getValue() == cleanGroup) {
                            cleanGroup = group.getKey();
                        }
                    }
                }

                // Users can only join an existing group that they aren't already in.
                if (groupExists && !groupsJoined.contains(cleanGroup)) {
                    groupsJoined.add(cleanGroup);
                }
            }
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
