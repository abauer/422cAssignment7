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
					String[] login = Parser.parseString(inputFromClient.readUTF());
                    switch(ClientAction.valueOf(login[1])) {
						case LOGIN: clientId = DatabaseServer.login(login[1], login[2]); break;
						case REGISTER: clientId = DatabaseServer.register(login[1], login[2]); break;
					}
                    outputToClient.writeInt(clientId);
                    //TODO update list of people online
                    //TODO update all other sockets with this list
                }
				// Continuously serve the client
				while (true) {
					String[] action = Parser.parseString(inputFromClient.readUTF());
                    int result;
                    String response;
                    List<String> responses;
					switch ((ClientAction.valueOf(action[0]))){
                        case UPDATEPASSWORD:
                            result = DatabaseServer.updatePassword(clientId,action[1],action[2]);
                            outputToClient.writeChars(Parser.packageStrings(ServerAction.UPDATEPASSWORDRESULT,result));
                            break;
                        case SENDGROUPMESSAGE:
                            result = DatabaseServer.sendGroupMessage(clientId,Integer.parseInt(action[1]),action[2]);
                            outputToClient.writeChars(Parser.packageStrings(ServerAction.GROUPMESSAGESENT,result));
                            break;
                        case SENDMESSAGE:
                            result = DatabaseServer.sendMessage(clientId,action[1],action[2]);
                            outputToClient.writeChars(Parser.packageStrings(ServerAction.MESSAGESENT,result));
                            break;
                        case GETFRIENDS:
                            responses = DatabaseServer.getFriends(clientId);
                            outputToClient.writeChars(Parser.packageStrings(ServerAction.FRIENDS,responses));
                            break;
                        case GETOFFLINEFRIENDS:

                            break;
                        case GETSTRANGERS: //remove self

                            break;
                        case MAKECHAT:
                            List<String> members = Arrays.asList(action);
                            members.remove(0);
                            result = DatabaseServer.makeChat(clientId,members);
                            outputToClient.writeChars(Parser.packageStrings(ServerAction.MAKEGROUP,result));
                            break;
                        case ADDFRIEND:
                            result = DatabaseServer.addFriend(clientId,action[1]);
                            outputToClient.writeChars(Parser.packageStrings(ServerAction.FRIENDADDED,result));
                            break;
                        case REMOVEFRIEND:
                            result = DatabaseServer.removeFriend(clientId,action[1]);
                            outputToClient.writeChars(Parser.packageStrings(ServerAction.FRIENDREMOVED,result));
                            break;
                        case GETMESSAGEHISTORY:
                            responses = DatabaseServer.getMessageHistory(clientId,action[1]);
                            outputToClient.writeChars(Parser.packageStrings(ServerAction.MESSAGEHISTORY,responses));
                            break;
                        case GETGROUPMESSAGEHISTORY:
                            responses = DatabaseServer.getGroupMessageHistory(clientId,Integer.parseInt(action[1]));
                            outputToClient.writeChars(Parser.packageStrings(ServerAction.GROUPMESSAGEHISTORY,responses));
                            break;
					}
					//outputToClient.writeDouble(area);
				}
			} catch(IOException e) {
                synchronized (openSockets) {
                    openSockets.remove(socket);
                    //TODO update list of people online
                    //TODO update all other sockets with this list
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