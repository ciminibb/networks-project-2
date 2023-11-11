import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;

public final class MessageBoardServer {
    private static final ArrayList<PrintWriter> clientWriters = new ArrayList<>();
    private static final Map<Integer, Message> messages = new HashMap<>();
    private static int messageID = 0;

    public static void main(String[] args) {
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
                username = in.readLine();
                System.out.println("User '" + username + "' connected.");

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
                clientWriters.remove(out);
            }
        }

        private void broadcastMessage(String message) {
            for (PrintWriter client : clientWriters) {
                client.println(message);
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
