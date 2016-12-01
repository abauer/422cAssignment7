package assignment7;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class MultiThreadServer extends Application {
	private TextArea ta = new TextArea(); // Text area for displaying contents

	// Number a client 
	private int clientNo = 0;

    // Maintain list of open sockets
    private final List<Client> activeClients = new ArrayList<>();

    private final ChatState chatState = new ChatState();

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
                    Client client = new Client(socket);
					new Thread(new ClientHandler(client)).start();
                    synchronized(activeClients) {
                        activeClients.add(client);
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
		private Client client; // contains socket
        private DataOutputStream outputToClient;
		/** Construct a thread */ 
		public ClientHandler(Client client) {
			this.client = client;
		}
		
		private void writeToClient(String s) {
            try {
                outputToClient.writeUTF(s+"\n");
            } catch (Exception e) {
                //TODO handle exception
            }
        }

		/** Run a thread */
		public void run() { 
			try {
				// Create data input and output streams
				BufferedReader inputFromClient = new BufferedReader(new InputStreamReader( client.getSocket().getInputStream()));
				outputToClient = new DataOutputStream( client.getSocket().getOutputStream());
                // Wait for username/password
                int clientId = -1;
                while (clientId == -1) {
                    System.out.println("Starting loop");
                    String line = inputFromClient.readLine().trim();
                    System.out.println(line);
                    String[] login = Parser.parseString(line);
                    System.out.println(Arrays.toString(login));
                    switch(ClientAction.valueOf(login[0])) {
						case LOGIN: clientId = DatabaseServer.login(login[1], login[2]); break;
						case REGISTER: clientId = DatabaseServer.register(login[1], login[2]); break;
					}
                    writeToClient(Parser.packageStrings(ServerAction.LOGINSUCCESS,clientId));
                    client.setId(clientId);
                    client.setName(login[1]);
                    //TODO update list of people online
                    //TODO update all other sockets with this list
                }
				// Continuously serve the client
				while (true) {
                    String line = inputFromClient.readLine().trim();
                    if (line.isEmpty()) continue;
                    System.out.println(line);
                    String[] action = Parser.parseString(line);
                    int result;
                    String response;
                    List<String> responses;
					switch ((ClientAction.valueOf(action[0]))){
                        case UPDATEPASSWORD:
                            result = DatabaseServer.updatePassword(clientId,action[1],action[2]);
                            //writeToClient(Parser.packageStrings(ServerAction.UPDATEPASSWORDRESULT,result));
                            break;
                        case SENDGROUPMESSAGE:
                            result = DatabaseServer.sendGroupMessage(clientId,Integer.parseInt(action[1]),action[2]);
                            //writeToClient(Parser.packageStrings(ServerAction.GROUPMESSAGESENT,result));
                            break;
                        case SENDMESSAGE:
                            result = DatabaseServer.sendMessage(clientId,action[1],action[2]);
                            chatState.triggerUpdate(result, Parser.packageStrings(ServerAction.NEWMESSAGE, action[1], action[2]));
                            //writeToClient(Parser.packageStrings(ServerAction.MESSAGESENT,result));
                            break;
                        case GETFRIENDS:
                            responses = DatabaseServer.getFriends(clientId);
                            //TODO confirm these friends are online
                            writeToClient(Parser.packageStrings(ServerAction.FRIENDS,responses));
                            break;
                        case GETOFFLINEFRIENDS:
                            responses = DatabaseServer.getFriends(clientId);
                            //TODO Using list of sockets/clientids get offlinefriends
                            //responses = new ArrayList<>();
                            writeToClient(Parser.packageStrings(ServerAction.OFFLINEFRIENDS,responses));
                            break;
                        case GETSTRANGERS: //remove self (clientId)
                            //TODO remove self, using list of sockets/clientids get strangers
                            responses = activeClients.stream()
                                    .filter(Client::isActive)
                                    .map(Client::getName)
                                    //filter self
                                    .collect(Collectors.toList());
                            System.out.println("***");
                            responses.stream().forEachOrdered(System.out::println);
                            System.out.println("****");
                            System.out.println(Parser.packageStrings(ServerAction.STRANGERS,responses));
                            writeToClient(Parser.packageStrings(ServerAction.STRANGERS,responses));
                            break;
                        case MAKECHAT:
                            List<String> members = new ArrayList<>(Arrays.asList(action));
                            members.remove(0); members.remove(0);   //ClientAction, groupName
                            result = DatabaseServer.makeChat(clientId,action[1],members);
                            //writeToClient(Parser.packageStrings(ServerAction.MAKEGROUP,result));
                            break;
                        case ADDFRIEND:
                            result = DatabaseServer.addFriend(clientId,action[1]);
                            //writeToClient(Parser.packageStrings(ServerAction.FRIENDADDED,result));
                            break;
                        case REMOVEFRIEND:
                            result = DatabaseServer.removeFriend(clientId,action[1]);
                            //writeToClient(Parser.packageStrings(ServerAction.FRIENDREMOVED,result));
                            break;
                        case GETMESSAGEHISTORY:
                            responses = DatabaseServer.getMessageHistory(clientId,action[1]);
                            writeToClient(Parser.packageStrings(ServerAction.MESSAGEHISTORY,action[1],responses));
                            break;
                        case GETGROUPMESSAGEHISTORY:
                            responses = DatabaseServer.getGroupMessageHistory(clientId,Integer.parseInt(action[1]));
                            writeToClient(Parser.packageStrings(ServerAction.GROUPMESSAGEHISTORY,action[1],responses));
                            break;
                        case LEAVEGROUP:
                            result = DatabaseServer.leaveGroup(clientId,Integer.parseInt(action[1]));
                            //writeToClient(Parser.packageStrings(ServerAction.LEFTGROUP,result));
                            break;
                        case GETGROUPS:
                            responses = DatabaseServer.getGroups(clientId);
                            writeToClient(Parser.packageStrings(ServerAction.GROUPS,responses));
                            break;
					}
					//outputToClient.writeDouble(area);
				}
			} catch(IOException e) {
                synchronized (activeClients) {
                    activeClients.remove(client);
                    //TODO update list of people online
                    //TODO update all other sockets with this list
                }
			}
		}
	}

	private class Client implements Observer {
        private int id = 0;
        private String name = null;
        private Socket socket;
        DataOutputStream outputStream;

        public Client(Socket socket) {
            this.socket = socket;
            try {
                outputStream = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                // this is handled by the ClientHandler already
            }
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Socket getSocket() {
            return socket;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getId(int id) {
            return id;
        }

        public boolean isActive() {
            return name != null && id > 0;
        }

        public void update(Observable obs, Object clientUpdate) {
            Set<Integer> affectedUsers = ((ClientUpdate) clientUpdate).affectedUsers;
            String update = ((ClientUpdate) clientUpdate).update;
            if (!affectedUsers.contains(this.id))
                return;
            try {
                outputStream.writeUTF(update);
            } catch (Exception e) {
                // this is handled by the ClientHandler already
            }
        }
    }

	private class ChatSession {
		private List<String> chatLog = new ArrayList<>(); // all chat
        private Set<Integer> users = new HashSet<>(); // all users in the chat, including offline ones

        public void addMessage(String msg) {
            chatLog.add(msg);
//            setChanged();
//            notifyObservers();
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

        public Set<Integer> getUsers() {
            return users;
        }
	}

	private class ChatState extends Observable {
        private final Map<Integer, Set<Integer>> sessions = new HashMap<>();

        public void triggerUpdate(int chatSession, String update) {
            setChanged();
            notifyObservers(new ClientUpdate(sessions.get(chatSession), update));
        }
    }

    private class ClientUpdate {
        Set<Integer> affectedUsers;
        String update;

        public ClientUpdate(Set<Integer> affectedUsers, String update) {
            this.affectedUsers = affectedUsers;
            this.update = update;
        }
    }
	
	public static void main(String[] args) {
		launch(args);
	}
}