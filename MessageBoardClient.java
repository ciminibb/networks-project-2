import java.io.*;
import java.net.*;
import java.util.Scanner;

public class MessageBoardClient
{
    public static void main(String[] args)
    {
        String serverAddress = "localhost";
        int serverPort = 42069; // nice

        try (Socket socket = new Socket(serverAddress, serverPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in))
        {

            System.out.print("Enter your username: ");
            String username = scanner.nextLine();
            out.println(username);

            System.out.println("You are now connected to the message board. Type messages below:");

            Thread receiveThread = new Thread(() -> {
                try
                {
                    String message;
                    while ((message = in.readLine()) != null)
                    {
                        System.out.println(message);
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            });
            receiveThread.start();

            String message;
            while (true)
            {
                message = scanner.nextLine();
                out.println(message);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
