package assignment7;

import java.io.*;
import java.net.*;
import java.util.*;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class MultiThreadServer extends Application {
	private TextArea ta = new TextArea(); // Text area for displaying contents
	private final Map<Integer, ChatSession> sessions = new HashMap<>();

	// Number a client 
	private int clientNo = 0;

    // Maintain list of open sockets
    private final List<Socket> openSockets = new ArrayList<>();

	@Override // Override the start method in the Application class 
	public void start(Stage primaryStage) { 
		// Create a scene and place it in the stage 
		Scene scene = new Scene(new ScrollPane(ta), 450, 200); 
		primaryStage.setTitle("MultiThreadServer"); // Set the stage title 
		primaryStage.setScene(scene); // Place the scene in the stage 
		primaryStage.show(); // Display the stage 

		new Thread( () -> { 
			try {  // Create a server socket
				ServerSocket serverSocket = new ServerSocket(8000);
				ta.appendText("MultiThreadServer started at IP " + Inet4Address.getLocalHost().getHostAddress() + '\n');

				while (true) { 
					// Listen for a new connection request
					Socket socket = serverSocket.accept(); 

					// Increment clientNo 
					clientNo++; 

					Platform.runLater( () -> { 
						// Display the client number 
						ta.appendText("Starting thread for client " + clientNo +
								" at " + new Date() + '\n'); 

						// Find the client's host name, and IP address 
						InetAddress inetAddress = socket.getInetAddress();
						ta.appendText("Client " + clientNo + "'s host name is "
								+ inetAddress.getHostName() + "\n");
						ta.appendText("Client " + clientNo + "'s IP Address is " 
								+ inetAddress.getHostAddress() + "\n");	});

					// Create and start a new thread for the connection
					new Thread(new ClientHandler(socket)).start();
                    synchronized(openSockets) {
                        openSockets.add(socket);
                    }
				} 
			} 
			catch(IOException ex) { 
				ex.printStackTrace();
			}
		}).start();
	}


	// Define the thread class for handling
	private class ClientHandler implements Runnable {
		private Socket socket; // A connected socket
		/** Construct a thread */ 
		public ClientHandler(Socket socket) {
			this.socket = socket;
		}
		/** Run a thread */
		public void run() { 
			try {
				// Create data input and output streams
				DataInputStream inputFromClient = new DataInputStream( socket.getInputStream());
				DataOutputStream outputToClient = new DataOutputStream( socket.getOutputStream());
                // Wait for username/password
                int clientId = -1;
                while (clientId == -1) {
                    String username = inputFromClient.readUTF();
                    String password = inputFromClient.readUTF();
                    clientId = DatabaseServer.login(username, password);
                    outputToClient.writeInt(clientId);
                }
				// Continuously serve the client
				while (true) { 
                    // TODO: PUT STUFF HERE

					// Receive radius from the client 
					double radius = inputFromClient.readDouble();
					// Compute area
					double area = radius * radius * Math.PI; 
					// Send area back to the client
					outputToClient.writeDouble(area);
					Platform.runLater(() -> { 
						ta.appendText("radius received from client: " +
								radius + '\n'); 
						ta.appendText("Area found: " + area + '\n');
					});
				}
			} catch(IOException e) {
                synchronized (openSockets) {
                    openSockets.remove(socket);
                }
			}
		}
	}

	private class ChatSession extends Observable {
		private List<String> chatLog = new ArrayList<>(); // all chat
        private Set<Integer> users = new HashSet<>(); // all users in the chat, including offline ones

        public void addMessage(String msg) {
            chatLog.add(msg);
            setChanged();
            notifyObservers();
        }

        public List<String> getMessages() {
            return chatLog;
        }

        public boolean addUser(int userId) {
            if (users.contains(userId)) return false;
            users.add(userId);
            return true;
        }

        public boolean removeUser(int userId) {
            if (!users.contains(userId)) return false;
            users.remove(userId);
            return true;
        }
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}