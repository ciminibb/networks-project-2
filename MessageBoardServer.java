import java.io.*;
import java.net.*;

public final class MessageBoardServer
{
    public static void main(String[] args)
    {
        int serverPort = 42069; // nice
        
        try (ServerSocket serverSocket = new ServerSocket(serverPort))
        {
            System.out.println("Message Board Server started on port " + serverPort);
            
            while (true)
            {
                Socket clientSocket = serverSocket.accept();
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
        private PrintWriter out;

        public ClientHandler(Socket socket)
        {
            this.clientSocket = socket;
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);)
            {
                this.out = out;

                // Handle the client connection
                String username = in.readLine();
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
            }
        }

        private void broadcastMessage(String message)
        {
            // Implement broadcasting to all connected clients
            // Loop through all client threads and send the message using this.out
        }
    }
}
