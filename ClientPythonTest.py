import socket

def main():
    server_address = "localhost"  # Change this to the server's IP or hostname
    server_port = 42069  # Change this to the server's port

    client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    client_socket.connect((server_address, server_port))

    try:
        username = input("Enter your username: ")
        client_socket.send((username + "\n").encode())

        print("You are now connected to the message board. Type messages below:")
        while True:
            message = input()
            client_socket.send((message + "\n").encode())
    except KeyboardInterrupt:
        print("Disconnected.")
    except Exception as e:
        print("An error occurred:", e)
    finally:
        client_socket.close()

if __name__ == "__main__":
    main()
