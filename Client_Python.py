import socket
import threading

class MessageBoardClient:
    def __init__(self, server_address, server_port):
        # Initialize the client with the server address and port.
        self.server_address = server_address
        self.server_port = server_port
        self.client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM) # Create a socket

    def connect_to_server(self):
        # Connect the client to the server using the specified address and port.
        self.client_socket.connect((self.server_address, self.server_port))

    def send_message(self, message):
        # Send a message to the server via the established socket connection.
        self.client_socket.send(message.encode('utf-8') + b'\n')

    def receive_messages(self):
        try:
            while True:
                # Continuously receive messages from the server output stream and print 
                # them to the console.
                message = self.client_socket.recv(1024).decode('utf-8') # Receive and decode
                print(message.rstrip('\n')) # Print without newline
        except ConnectionResetError:
            print("Disconnected from the server.")

    def run(self):
        # Establish connection to the server.
        self.connect_to_server()

        # Prompt the user to enter a username, then send it to the server.
        username = input("-- Enter your username: ")
        self.send_message(username)

        print("-- You are now connected to the message board.") # Provide confirmation of connection

        # Start a separate thread to host the continuous receipt messages from the server.
        receive_thread = threading.Thread(target=self.receive_messages)
        receive_thread.start()

        while True:
            # Continuously listen for user input and handle different commands.
            line = input()
            if line.upper() == 'GET':
                # Request contents of a certain message by ID.
                message_id = input("-- Enter message ID: ")
                self.send_message(f"GET_MESSAGE:{message_id}")
            elif line.upper() == 'POST':
                # Post a message to a certain group.
                group_id = input("-- Enter group ID for the message: ")
                subject = input("-- Enter message subject: ")
                content = input("-- Enter message content: ")
                self.send_message(f"POST_MESSAGE:{group_id}:{subject}:{content}")
            elif line.upper() == 'JOIN':
                # Join specified groups all at once.
                groups = input("-- Enter comma-separated groups: ")
                self.send_message(f"JOIN:{groups}")
            elif line.upper() == 'PUBLICPOST':
                # Post to the public message board, group 0.
                group_id = '0' # Public group is hard coded.
                subject = input("-- Enter message subject: ")
                content = input("-- Enter message content: ")
                self.send_message(f"POST_MESSAGE:{group_id}:{subject}:{content}")
            elif line.upper() == 'HELP':
                # Request operating instruction set.
                self.send_message("HELP")
            elif line.upper() == 'LEAVE':
                # Leave all specified groups at once.
                groups = input("-- Enter comma-separated groups: ")
                self.send_message(f"LEAVE:{groups}")
            elif line.upper() == 'MEMBERS':
                # Get a list of the members from a certain group.
                group_id = input("-- Enter group ID to see its members: ")
                self.send_message(f"MEMBERS:{group_id}")
            elif line.upper() == 'HISTORY':
                # Get the chat history from a certain group. Of course, the chat
                # history only contains the last two message + join/leave notifs.
                group_id = input("-- Enter group ID to see its history: ")
                self.send_message(f"HISTORY:{group_id}")
            elif line.upper() == 'DISCONNECT':
                # Disconnect from the server.
                self.send_message("-- Disconnecting from the server.")
                break
            else:
                # Prompt the user to ask for help if they enter an invalid command.
                print("-- Type 'HELP' to see the instruction set!")

if __name__ == "__main__":
    # Instantiate the MessageBoardClient with server address and port, then run the client.
    client = MessageBoardClient('localhost', 42069)
    client.run()
