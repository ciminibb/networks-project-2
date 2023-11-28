import unittest
from unittest.mock import patch, MagicMock
from io import StringIO
from Client_Python import MessageBoardClient

class TestMessageBoardClient(unittest.TestCase):
    def setUp(self):
        self.client = MessageBoardClient('localhost', 42069)

    @patch('sys.stdout', new_callable=StringIO)
    def assert_stdout(self, expected_output, mock_stdout):
        self.client.run()
        self.assertEqual(mock_stdout.getvalue().strip(), expected_output)

    @patch('builtins.input', side_effect=['Alice', 'GET', '1'])
    def test_receive_messages(self, mock_input):
        # Mock the client socket
        mock_socket = MagicMock()
        mock_socket.recv.side_effect = ['Message 1\n', 'Message 2\n', ConnectionResetError]

        with patch('socket.socket', return_value=mock_socket):
            self.client.run()

        # Check if the messages are printed as expected
        expected_output = "Message 1\nMessage 2\nDisconnected from the server."
        self.assert_stdout(expected_output)

    @patch('builtins.input', side_effect=['Alice', 'HELP'])
    def test_help_command(self, mock_input):
        # Mock the client socket
        mock_socket = MagicMock()
        with patch('socket.socket', return_value=mock_socket):
            expected_output = "-- Type 'PUBLICPOST' to post on the public message board.\n" \
                              "-- Type 'JOIN' to join a private group.\n" \
                              "-- Type 'LEAVE' to leave a private group.\n" \
                              "-- Type 'POST' to post on a private group.\n" \
                              "-- Type 'GET' to get the contents of a post.\n" \
                              "-- Type 'MEMBERS' to see the members of a group.\n" \
                              "-- Type 'HELP' to see these instructions again!"
            self.assert_stdout(expected_output)

if __name__ == '__main__':
    unittest.main()

