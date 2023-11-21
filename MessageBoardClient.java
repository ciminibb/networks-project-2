import java.io.*;
import java.net.*;
import java.util.*;

public class MessageBoardClient {
    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 42069; // Example port

        try (
            Socket socket = new Socket(serverAddress, serverPort);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner scanner = new Scanner(System.in)
        ) {
            System.out.print("-- Enter your username: ");
            String username = scanner.nextLine();
            out.println(username);

            System.out.println("-- You are now connected to the message board.");

            Thread receiveThread = new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            receiveThread.start();

            while (true) {
                String line = scanner.nextLine();
                if (line.equalsIgnoreCase("GET")) {
                    System.out.print("-- Enter message ID: ");
                    String id = scanner.nextLine();
                    out.println("GET_MESSAGE:" + id);
                } else if (line.equalsIgnoreCase("POST")) {
                    System.out.print("-- Enter group ID for the message: ");
                    String groupId = scanner.nextLine();
                    System.out.print("-- Enter message subject: ");
                    String subject = scanner.nextLine();
                    System.out.print("-- Enter message content: ");
                    String content = scanner.nextLine();
                    out.println("POST_MESSAGE:" + groupId + ":" + subject + ":" + content);
                } else if (line.equalsIgnoreCase("JOIN")) {
                    System.out.print("-- Enter comma-separated groups: ");
                    String groups = scanner.nextLine();
                    out.println("JOIN:" + groups);
                } else if (line.equalsIgnoreCase("PUBLICPOST")) {
                    String groupId = "0";
                    System.out.print("-- Enter message subject: ");
                    String subject = scanner.nextLine();
                    System.out.print("-- Enter message content: ");
                    String content = scanner.nextLine();
                    out.println("POST_MESSAGE:" + groupId + ":" + subject + ":" + content);
                } else if (line.equalsIgnoreCase("HELP")) {
                    out.println("HELP");
                } else if (line.equalsIgnoreCase("LEAVE")) {
                    System.out.print("-- Enter comma-separated groups: ");
                    String groups = scanner.nextLine();
                    out.println("LEAVE:" + groups);
                } else {
                    System.out.println("-- Type 'HELP' to see the instruction set!");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
