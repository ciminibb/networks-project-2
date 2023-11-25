import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Set;

public class MessageBoardServerTest {

    private ByteArrayOutputStream outContent;
    private MessageBoardServer.ClientHandler clientHandler;

    @Before
    public void setUp() {
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        // Initialize your ClientHandler for testing
        Socket dummySocket = new Socket();
        PrintWriter dummyPrintWriter = new PrintWriter(System.out, true);
        clientHandler = new MessageBoardServer.ClientHandler(dummySocket, dummyPrintWriter);
    }

    @Test
    public void testJoinLeaveGroup() {
        // Join a group
        clientHandler.joinGroup("1");
        assertTrue(clientHandler.groupsJoined.contains("1"));
        assertTrue(MessageBoardServer.members.get("1").contains(clientHandler.username));

        // Leave the same group
        clientHandler.leaveGroup("1");
        assertFalse(clientHandler.groupsJoined.contains("1"));
        assertFalse(MessageBoardServer.members.get("1").contains(clientHandler.username));
    }

    @Test
    public void testPostMessage() {
        // Join a group
        clientHandler.joinGroup("2");

        // Post a message to the group
        clientHandler.postMessage("2:Test Subject:Test Content");
        assertTrue(MessageBoardServer.messages.size() > 0);

        // Check if the message is broadcasted to the group
        Set<PrintWriter> groupClients = MessageBoardServer.groupClients.get("2");
        assertNotNull(groupClients);
        assertTrue(outContent.toString().contains("Test Subject"));

        // Check if the message is not broadcasted to a different group
        Set<PrintWriter> otherGroupClients = MessageBoardServer.groupClients.get("1");
        assertNull(otherGroupClients);
        assertFalse(outContent.toString().contains("Test Subject"));
    }

    @Test
    public void testSendMemberList() {
        // Join a group
        clientHandler.joinGroup("3");

        // Send member list for the group
        clientHandler.sendMemberList("3");
        assertTrue(outContent.toString().contains("only you"));

        // Another user joins the group
        MessageBoardServer.ClientHandler anotherClient = new MessageBoardServer.ClientHandler(new Socket(), new PrintWriter(System.out, true));
        anotherClient.username = "AnotherUser";
        anotherClient.joinGroup("3");

        // Send member list again
        clientHandler.sendMemberList("3");
        assertTrue(outContent.toString().contains("AnotherUser"));
    }

    // Add more test methods as needed

}

