import java.io.*;
import java.net.*;
import java.util.ArrayList;

public final class MessageBoardServer
{
    private static final ArrayList<PrintWriter> clientWriters = new ArrayList<>();

    public static void main(String[] args)
    {
        int serverPort = 42069; // nice
        
        try (ServerSocket serverSocket = new ServerSocket(serverPort))
        {
            System.out.println("Message Board Server started on port " + serverPort);
            
            while (true)
            {
                Socket clientSocket = serverSocket.accept();

                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                clientWriters.add(out);
                // new ClientHandler(clientSocket).start();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                Thread clientThread = new Thread(clientHandler);
                clientThread.start();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    static class ClientHandler extends Thread
    {
        private final Socket clientSocket;
        private final PrintWriter out;
        private String username;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            try
            {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
            }
            catch (IOException e)
            {
                e.printStackTrace();
                throw new RuntimeException("Error creating client writer.");
            }
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())))
            {

                // Handle the client connection
                username = in.readLine();
                System.out.println("User '" + username + "' connected.");

                String message;
                while ((message = in.readLine()) != null)
                {
                    System.out.println(username + ": " + message);
                    // Broadcast the message to all connected clients
                    broadcastMessage(username + ": " + message);
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    clientSocket.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                clientWriters.remove(out);
            }
        }

        private void broadcastMessage(String message)
        {
            for (PrintWriter client : clientWriters)
            {
                client.println(message);
            }
        }
    }
}
