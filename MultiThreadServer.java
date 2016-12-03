/* WEBSERVER MultiThreadServer.java
 * EE422C Project 7 submission by
 * Anthony Bauer
 * amb6869
 * 16480
 * Grant Uy
 * gau84
 * 16480
 * Slip days used: <1>
 * Fall 2016
 */
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
        primaryStage.setOnCloseRequest(event -> System.exit(0));

		new Thread( () -> { 
			try {  // Create a server socket
				ServerSocket serverSocket = new ServerSocket(8000);
                Platform.runLater(() -> {
                    try {
                        ta.appendText("MultiThreadServer started at IP " + Inet4Address.getLocalHost().getHostAddress() + '\n');
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

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
        private BufferedWriter outputToClient;
		/** Construct a thread */ 
		public ClientHandler(Client client) {
			this.client = client;
		}
		
		private void writeToClient(String s) {
            try {
                System.out.println(s);
                outputToClient.write(s);
                outputToClient.newLine();
                outputToClient.flush();
            } catch (Exception e) {
                //TODO handle exception
            }
        }

		/** Run a thread */
		public void run() {
            String name = "";
			try {
				// Create data input and output streams
				BufferedReader inputFromClient = new BufferedReader(new InputStreamReader( client.getSocket().getInputStream()));
				outputToClient = new BufferedWriter(new OutputStreamWriter(client.getSocket().getOutputStream()));
                // Wait for username/password
                int clientId = -1;
                while (clientId == -1) {
                    String line = inputFromClient.readLine().trim();
                    String[] login = Parser.parseString(line);
                    name = login[1];
                    switch(ClientAction.valueOf(login[0])) {
						case LOGIN: clientId = DatabaseServer.login(login[1], login[2]); break;
						case REGISTER: clientId = DatabaseServer.register(login[1], login[2]); break;
					}
                    writeToClient(Parser.packageStrings(ServerAction.LOGINSUCCESS,clientId));
                    client.setId(clientId);
                    client.setName(name);
                    chatState.triggerUpdate(Parser.packageStrings(ServerAction.COMEONLINE,login[1]));
                    chatState.addObserver(client);
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
                    List<Integer> results;
                    Set<String> onlineUsers;
                    int groupId;
                    switch ((ClientAction.valueOf(action[0]))){
                        case UPDATEPASSWORD:
                            result = DatabaseServer.updatePassword(clientId,action[1]);
                            break;
                        case SENDGROUPMESSAGE:
                            groupId = Integer.parseInt(action[1]);
                            result = DatabaseServer.sendGroupMessage(clientId,groupId,action[2]);
                            if (result > 0) {
                                String msg = String.format("[%s]: %s", client.getName(), action[2]);
                                chatState.triggerUpdate(groupId, Parser.packageStrings(ServerAction.NEWGROUPMESSAGE, groupId, msg));
                            }
                            break;
                        case SENDMESSAGE:
                            result = DatabaseServer.sendMessage(clientId,action[1],action[2]);
                            // TODO fix
                            if (result > 0) {
                                String msg = String.format("[%s]: %s", client.getName(), action[2]);
                                chatState.triggerUpdate(client.getName(), Parser.packageStrings(ServerAction.NEWMESSAGE, action[1], msg));
                                chatState.triggerUpdate(action[1], Parser.packageStrings(ServerAction.NEWMESSAGE, client.getName(), msg));
                            }
                            break;
                        case GETFRIENDS:
                            responses = DatabaseServer.getFriends(clientId);
                            onlineUsers = activeClients.stream()
                                    .filter(Client::isActive)
                                    .map(Client::getName)
                                    .collect(Collectors.toSet());
                            responses = responses.stream()
                                    .filter(onlineUsers::contains)
                                    .collect(Collectors.toList());
                            writeToClient(Parser.packageStrings(ServerAction.FRIENDS,responses));
                            break;
                        case GETOFFLINEFRIENDS:
                            responses = DatabaseServer.getFriends(clientId);
                            onlineUsers = activeClients.stream()
                                    .filter(Client::isActive)
                                    .map(Client::getName)
                                    .collect(Collectors.toSet());
                            responses = responses.stream()
                                    .filter(s -> !onlineUsers.contains(s))
                                    .collect(Collectors.toList());
                            writeToClient(Parser.packageStrings(ServerAction.OFFLINEFRIENDS,responses));
                            break;
                        case GETSTRANGERS: //TODO remove friends
                            responses = activeClients.stream()
                                    .filter(Client::isActive)
                                    .map(Client::getName)
                                    .filter(s -> !s.equals(client.getName()))
                                    .collect(Collectors.toList());
                            writeToClient(Parser.packageStrings(ServerAction.STRANGERS,responses));
                            break;
                        case MAKECHAT:
                            List<String> members = new ArrayList<>(Arrays.asList(action));
                            members.remove(0); members.remove(0);   //ClientAction, groupName
                            members.add(client.getName());
                            results = DatabaseServer.makeChat(clientId,action[1],members);
                            if (results.size() > 1) {
                                groupId = results.get(0); // TODO fix
                                // add everyone to the group
                                results.subList(0, results.size()).forEach(id -> chatState.addGroupAssoc(id, groupId));
                                chatState.triggerUpdate(groupId, Parser.packageStrings(ServerAction.GROUPADDED, groupId, action[1]));
                                chatState.triggerUpdate(groupId, Parser.packageStrings(ServerAction.NEWGROUPMESSAGE, groupId, client.getName() + " created the group."));
                            }
                            break;
                        case ADDFRIEND:
                            responses = DatabaseServer.addFriend(clientId,action[1]);
                            if (responses.size() > 1) {
                                if (responses.get(0).equals("1")) {
                                    String msg = String.format("%s sent %s a friend request.", client.getName(), action[1]);
                                    chatState.triggerUpdate(client.getName(), Parser.packageStrings(ServerAction.NEWMESSAGE, action[1], msg));
                                    chatState.triggerUpdate(action[1], Parser.packageStrings(ServerAction.NEWMESSAGE, client.getName(), msg));
                                } else if (responses.get(0).equals("2")) {
                                    String msg = String.format("%s accepted %s's friend request.", client.getName(), action[1]);
                                    chatState.triggerUpdate(client.getName(), Parser.packageStrings(ServerAction.NEWMESSAGE, action[1], msg));
                                    chatState.triggerUpdate(action[1], Parser.packageStrings(ServerAction.NEWMESSAGE, client.getName(), msg));
                                    chatState.triggerUpdate(client.getName(), Parser.packageStrings(ServerAction.FRIENDADDED, action[1]));
                                    chatState.triggerUpdate(action[1], Parser.packageStrings(ServerAction.FRIENDADDED, client.getName()));
                                }
                            }
                            break;
                        case REMOVEFRIEND:
                            result = DatabaseServer.removeFriend(clientId,action[1]);
                            if (result > 0) {
                                chatState.triggerUpdate(client.getName(), Parser.packageStrings(ServerAction.FRIENDREMOVED, action[1]));
                                chatState.triggerUpdate(action[1], Parser.packageStrings(ServerAction.FRIENDREMOVED, client.getName()));
                            }
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
                            groupId = Integer.parseInt(action[1]);
                            result = DatabaseServer.leaveGroup(clientId,groupId);
                            if (result > 0) {
                                chatState.triggerUpdate(groupId, Parser.packageStrings(ServerAction.NEWGROUPMESSAGE, groupId, client.getName()+" left the group."));
                                chatState.triggerUpdate(client.getName(), Parser.packageStrings(ServerAction.GROUPREMOVED, groupId));
                                chatState.removeGroupAssoc(clientId, groupId);
                            }
                            break;
                        case GETGROUPS:
                            responses = DatabaseServer.getGroups(clientId);
                            if (responses.size() > 1)
                                for (int i=0; i<responses.size(); i+=2)
                                    chatState.addGroupAssoc(clientId, Integer.parseInt(responses.get(i)));
                            writeToClient(Parser.packageStrings(ServerAction.GROUPS,responses));
                            break;
					}
					//outputToClient.writeDouble(area);
				}
			} catch(IOException e) {
                synchronized (activeClients) {
                    activeClients.remove(client);
                    chatState.deleteObserver(client);
                    chatState.triggerUpdate(Parser.packageStrings(ServerAction.WENTOFFLINE,name));
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
        private BufferedWriter outputStream;

        public Client(Socket socket) {
            this.socket = socket;
            try {
                outputStream = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
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
            String name = ((ClientUpdate) clientUpdate).name;
            String update = ((ClientUpdate) clientUpdate).update;
            if ((affectedUsers == null && name == null) ||
                    (affectedUsers != null && affectedUsers.contains(this.id)) ||
                    (name != null && name.equals(this.name))) {
                try {
                    System.out.println(update);
                    outputStream.write(update);
                    outputStream.newLine();
                    outputStream.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

	private class ChatState extends Observable {
        private final Map<Integer, Set<Integer>> sessions = new HashMap<>();

        public void triggerUpdate(int chatSession, String update) {
            Set<Integer> affectedUsers = (chatSession == 0) ? null : sessions.get(chatSession);
            setChanged();
            notifyObservers(new ClientUpdate(affectedUsers, update));
        }

        public void triggerUpdate(String update) {
            triggerUpdate(0, update);
        }

        public void triggerUpdate(String username, String update) {
            setChanged();
            notifyObservers(new ClientUpdate(username, update));
        }

        public void addGroupAssoc(int userId, int groupId) {
            if (!sessions.containsKey(groupId))
                sessions.put(groupId, new HashSet<>());
            sessions.get(groupId).add(userId);
        }

        public void removeGroupAssoc(int userId, int groupId) {
            if (sessions.containsKey(groupId)) {
                sessions.get(groupId).remove(userId);
            }
        }
    }

    private class ClientUpdate {
        Set<Integer> affectedUsers;
        String update;
        String name;

        public ClientUpdate(Set<Integer> affectedUsers, String update) {
            this.affectedUsers = affectedUsers;
            this.update = update;
        }

        public ClientUpdate(String name, String update) {
            this.name = name;
            this.update = update;
        }
    }
	
	public static void main(String[] args) {
		launch(args);
	}
}