import socket
import threading

class MessageBoardClient:
    def __init__(self, server_address, server_port):
        self.server_address = server_address
        self.server_port = server_port
        self.client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    def connect_to_server(self):
        self.client_socket.connect((self.server_address, self.server_port))

    def send_message(self, message):
        self.client_socket.send(message.encode('utf-8') + b'\n')

    def receive_messages(self):
        try:
            while True:
                message = self.client_socket.recv(1024).decode('utf-8')
                print(message.rstrip('\n'))
        except ConnectionResetError:
            print("Disconnected from the server.")

    def run(self):
        self.connect_to_server()

        username = input("-- Enter your username: ")
        self.send_message(username)

        print("-- You are now connected to the message board.")

        receive_thread = threading.Thread(target=self.receive_messages)
        receive_thread.start()

        while True:
            line = input()
            if line.upper() == 'GET':
                message_id = input("-- Enter message ID: ")
                self.send_message(f"GET_MESSAGE:{message_id}")
            elif line.upper() == 'POST':
                group_id = input("-- Enter group ID for the message: ")
                subject = input("-- Enter message subject: ")
                content = input("-- Enter message content: ")
                self.send_message(f"POST_MESSAGE:{group_id}:{subject}:{content}")
            elif line.upper() == 'JOIN':
                groups = input("-- Enter comma-separated groups: ")
                self.send_message(f"JOIN:{groups}")
            elif line.upper() == 'PUBLICPOST':
                group_id = '0'
                subject = input("-- Enter message subject: ")
                content = input("-- Enter message content: ")
                self.send_message(f"POST_MESSAGE:{group_id}:{subject}:{content}")
            elif line.upper() == 'HELP':
                self.send_message("HELP")
            elif line.upper() == 'LEAVE':
                groups = input("-- Enter comma-separated groups: ")
                self.send_message(f"LEAVE:{groups}")
            elif line.upper() == 'MEMBERS':
                group_id = input("-- Enter group ID to see its members: ")
                self.send_message(f"MEMBERS:{group_id}")
            elif line.upper() == 'HISTORY':
                group_id = input("-- Enter group ID to see its history: ")
                self.send_message(f"HISTORY:{group_id}")
            elif line.upper() == 'DISCONNECT':
                self.send_message("-- Disconnecting from the server.")
                break
            else:
                print("-- Type 'HELP' to see the instruction set!")

if __name__ == "__main__":
    client = MessageBoardClient('localhost', 42069)
    client.run()
    
    # random comment
