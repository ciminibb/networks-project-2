import java.io.*;
import java.net.*;
import java.util.*;

public class MessageBoardClient {
    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 42069; // nice

        try (Socket socket = new Socket(serverAddress, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            System.out.print("Enter your username: ");
            String username = scanner.nextLine();
            out.println(username);

            System.out.println("You are now connected to the message board.");
            System.out.println("Type 'GET' to retrieve a message by ID or type your message subject below:");

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
                    System.out.print("Enter message ID: ");
                    String id = scanner.nextLine();
                    out.println("GET_MESSAGE:" + id);
                    System.out.println("Type 'GET' to retrieve a message by ID or type your message subject below:");
                } else {
                    out.println(line); // Send subject
                    System.out.println("Enter your message:");
                    String content = scanner.nextLine();
                    out.println(content); // Send content
                    System.out.println("Type 'GET' to retrieve a message by ID or type your message subject below:");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
