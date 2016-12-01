package assignment7;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import javafx.stage.Stage;

public class Client extends Application {
	// IO streams 
	DataOutputStream toServer = null; 
	DataInputStream fromServer = null;
    Thread recieve;
    ServerRecieve sa;
	int clientId;

    Label currentChat;
    Contact currentContact;
    ListView<Label> chatMessages;

    HashMap<Integer,Contact> groups;
    ContactList<Contact> groupsView;
    List<Contact> onlineFriends;
    ContactList<Contact> onlineFriendsView;
    List<Contact> offlineFriends;
    ContactList<Contact> offlineFriendsView;
    List<Contact> onlineStrangers;
    ContactList<Contact> onlineStrangersView;


	@Override // Override the start method in the Application class 
	public void start(Stage loginStage) {

		BorderPane loginPane = new BorderPane();
		loginPane.setPadding(new Insets(5));

		VBox loginBox = new VBox();
		loginBox.setSpacing(10);
		loginBox.setAlignment(Pos.CENTER);

		HBox serverBox = new HBox();
		serverBox.setSpacing(10);
		serverBox.setAlignment(Pos.CENTER);

		Label serverLabel = new Label("Server Hostname: ");
		TextField ipField = new TextField("localhost"); //default
		serverBox.getChildren().addAll(serverLabel,ipField);

		loginBox.getChildren().add(serverBox);

		HBox usernameBox = new HBox();
		usernameBox.setSpacing(10);
		usernameBox.setAlignment(Pos.CENTER);

		Label usernameLabel = new Label("Username: ");
		TextField usernameField = new TextField("demo");    //TODO REMOVE
        addTextLimiter(usernameField,15);
		usernameBox.getChildren().addAll(usernameLabel,usernameField);

		loginBox.getChildren().add(usernameBox);

		HBox passwordBox = new HBox();
		passwordBox.setSpacing(10);
		passwordBox.setAlignment(Pos.CENTER);

		Label passwordLabel = new Label("Password: ");
		PasswordField passwordField = new PasswordField();
        addTextLimiter(passwordField,15);
		passwordField.setText("grantisawesome");    //TODO REMOVE
		passwordBox.getChildren().addAll(passwordLabel,passwordField);

		loginBox.getChildren().add(passwordBox);

		Label statusLabel = new Label("Status: Ready");
		statusLabel.setTextFill(Color.GREEN);
		statusLabel.setAlignment(Pos.BOTTOM_LEFT);
		loginPane.setBottom(statusLabel);

		HBox submitBox = new HBox();
		submitBox.setSpacing(25);
		submitBox.setAlignment(Pos.CENTER);

		Button registerButton = new Button("Register");
		registerButton.setOnAction(e -> {
			boolean success = connectToServer(ipField.getText(),statusLabel);
			if(!success)
				return;
			success = loginToServer(ClientAction.REGISTER,Parser.cleanString(usernameField.getText()),passwordField.getText(),statusLabel);
            if(!success)
                return;
            openChat();
			loginStage.close();
		});
		Button loginButton = new Button("Login");
		loginButton.setOnAction(e -> {  //TODO UNCOMMENT
	//		boolean success = connectToServer(ipField.getText(),statusLabel);
	//		if(!success)
	//			return;
	//		success = loginToServer(ClientAction.LOGIN,Parser.cleanString(usernameField.getText()),passwordField.getText(),statusLabel);
    //      if(!success)
    //          return;
            openChat();
			loginStage.close();
		});
		submitBox.getChildren().addAll(registerButton,loginButton);

		loginBox.getChildren().add(submitBox);

		loginPane.setCenter(loginBox);

		// Create a scene and place it in the stage 
		Scene scene = new Scene(loginPane, 450, 200);
		loginStage.setTitle("Login"); // Set the stage title
		loginStage.setScene(scene); // Place the scene in the stage
		loginStage.show(); // Display the stage
	}

	private void openPassword(){
        Stage passwordStage = new Stage();
        BorderPane passPane = new BorderPane();
        passPane.setPadding(new Insets(10));

        VBox passBox = new VBox();
        passBox.setSpacing(10);
        passBox.setAlignment(Pos.CENTER);

        HBox currentBox = new HBox();
        currentBox.setSpacing(10);
        currentBox.setAlignment(Pos.CENTER);

        Label currentLabel = new Label("Current Pass: ");
        PasswordField currentField = new PasswordField();
        addTextLimiter(currentField,15);
        currentBox.getChildren().addAll(currentLabel,currentField);

        passBox.getChildren().add(currentBox);

        HBox newBox = new HBox();
        newBox.setSpacing(10);
        newBox.setAlignment(Pos.CENTER);

        Label newLabel = new Label("New Pass: ");
        PasswordField newField = new PasswordField();
        addTextLimiter(newField,15);
        newBox.getChildren().addAll(newLabel,newField);

        passBox.getChildren().add(newBox);

        HBox submitBox = new HBox();
        submitBox.setAlignment(Pos.CENTER);

        Button confirmButton = new Button("Confrim");
        confirmButton.setOnAction(e -> {
            String query = Parser.packageStrings(ClientAction.UPDATEPASSWORD,currentField.getText(),newField.getText());
            sendQuery(query);
            passwordStage.close();
        });
        submitBox.getChildren().add(confirmButton);

        passBox.getChildren().add(submitBox);

        passPane.setCenter(passBox);

        Scene scene = new Scene(passPane);
        passwordStage.setTitle("Change Password"); // Set the stage title
        passwordStage.setScene(scene); // Place the scene in the stage
        passwordStage.show(); // Display the stage
    }

	private void openAddGroup(){
        Stage groupStage = new Stage();
        BorderPane groupPane = new BorderPane();
        groupPane.setPadding(new Insets(10));
        //Group Name - Top
        HBox nameBox = new HBox();
        nameBox.setPadding(new Insets(0,0,5,0));
        nameBox.setSpacing(5);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label("Group Name:");
        nameLabel.setAlignment(Pos.CENTER_LEFT);
        TextField nameField = new TextField();
        addTextLimiter(nameField,32);
        HBox.setHgrow(nameField,Priority.ALWAYS);
        nameBox.getChildren().addAll(nameLabel,nameField);
        groupPane.setTop(nameBox);
        //Friends - Left
        VBox friends = new VBox();
        friends.setPadding(new Insets(0,5,0,0));
        friends.setSpacing(5);
        groupPane.setLeft(friends);

        Label groupLabel = new Label("Friends");
        List<Contact> compiled = Stream.concat(onlineFriends.stream(),offlineFriends.stream()).map(c -> new Contact(c.toString())).collect(Collectors.toList());
        ContactList<Contact> items = new ContactList<>(compiled);
        ContactList<Contact> includes = new ContactList<>();
        friends.getChildren().addAll(groupLabel,items);

        //mouse handler
        items.getSelectionModel().selectionModeProperty().removeListener(items.listener);
        items.getSelectionModel().selectedIndexProperty().addListener(e -> swapLists(items,includes));
        includes.getSelectionModel().selectionModeProperty().removeListener(includes.listener);
        includes.getSelectionModel().selectedIndexProperty().addListener(e -> swapLists(includes,items));

        //In Group - Center
        VBox included = new VBox();
        included.setSpacing(5);
        groupPane.setCenter(included);

        Label includedLabel = new Label("Included");
        included.getChildren().addAll(includedLabel,includes);

        //Finish - Bottom
        HBox createBox = new HBox();
        createBox.setPadding(new Insets(5,0,0,0));
        createBox.setAlignment(Pos.BOTTOM_CENTER);
        Button createGroup = new Button("Finish");
        createGroup.setOnAction(event -> {
            List<Contact> inGroup = new ArrayList(includes.getItems());
            if(inGroup.size()>0) {
                String query = Parser.packageStrings(ClientAction.MAKECHAT,nameField.getText(),inGroup);
                sendQuery(query);
            }
            groupStage.hide();
        });
        createGroup.setAlignment(Pos.BOTTOM_CENTER);
        createBox.getChildren().add(createGroup);
        groupPane.setBottom(createBox);

        Scene scene = new Scene(groupPane);
        groupStage.setHeight(500);
        groupStage.setTitle("Create Group"); // Set the stage title
        groupStage.setScene(scene); // Place the scene in the stage
        groupStage.show(); // Display the stage
    }

    private void swapLists(ContactList<Contact> first, ContactList<Contact> second){
        List<Contact> selectList = first.getSelectionModel().getSelectedItems();
        if(selectList.size() > 0) {
            final Contact selected = selectList.get(0);
            if (selected != null) {
                second.addItem(selected);
                Platform.runLater(() -> first.removeItem(selected));
            }
        }
    }

    public static void addTextLimiter(final TextField tf, final int maxLength) {
        tf.textProperty().addListener((ov, oldValue, newValue) -> {
            if (tf.getText().length() > maxLength) {
                String s = tf.getText().substring(0, maxLength);
                tf.setText(s);
            }
        });
    }

	private void openChat(){

    //    prepWaitForServer(ServerAction.GROUPS);
        String query = Parser.packageStrings(ClientAction.GETGROUPS);
        sendQuery(query);
    //    waitForServer(ServerAction.GROUPS);

    //    prepWaitForServer(ServerAction.FRIENDS);
        query = Parser.packageStrings(ClientAction.GETFRIENDS);
        sendQuery(query);
    //    waitForServer(ServerAction.FRIENDS);

    //    prepWaitForServer(ServerAction.OFFLINEFRIENDS);
        query = Parser.packageStrings(ClientAction.GETOFFLINEFRIENDS);
        sendQuery(query);
    //    waitForServer(ServerAction.OFFLINEFRIENDS);

    //    prepWaitForServer(ServerAction.STRANGERS);
        query = Parser.packageStrings(ClientAction.GETSTRANGERS);
        sendQuery(query);
    //    waitForServer(ServerAction.STRANGERS);

        //TODO create contacts in the serverresponse handler
        //get online people / friends
        groups = new HashMap<>();
        groups.put(1,new Contact("Senior Design",true,this)); groups.put(2,new Contact("HackDFW",true,this)); groups.put(3,new Contact("Frist Allo",true,this));

        onlineFriends = new ArrayList<>();
        onlineFriends.add(new Contact("Grant",true,this));
        offlineFriends = new ArrayList<>();
        offlineFriends.add(new Contact("Rony",true,this));

        onlineStrangers = new ArrayList<>();
        onlineStrangers.add(new Contact("BruceBanner",false,this)); onlineStrangers.add(new Contact("BilboBaggins",false,this));
        //finish remove TODO
        Stage chatStage = new Stage();

        BorderPane chatPane = new BorderPane();
        chatPane.setPadding(new Insets(5));

        //Friends - Left
        VBox friends = new VBox();
        friends.setSpacing(5);
        chatPane.setLeft(friends);

        BorderPane groupTitle = new BorderPane();
        VBox groupLabelAlign = new VBox();
        Label groupLabel = new Label("Groups");
        groupLabel.setAlignment(Pos.BOTTOM_CENTER);
        groupLabelAlign.getChildren().add(groupLabel);
        groupLabelAlign.setAlignment(Pos.BOTTOM_CENTER);
        Button addGroupButton = new Button("Add Group");
        addGroupButton.setOnAction(event -> openAddGroup());
        groupTitle.setLeft(groupLabelAlign);
        groupTitle.setRight(addGroupButton);
        friends.getChildren().add(groupTitle);
        groupsView = new ContactList<>(new ArrayList(groups.values()));
        friends.getChildren().add(groupsView);


        Label onlineFriendsLabel = new Label("Online Friends");
        friends.getChildren().add(onlineFriendsLabel);
        onlineFriendsView = new ContactList<>(onlineFriends);
        friends.getChildren().add(onlineFriendsView);

        Label offlineFriendsLabel = new Label("Offline Friends");
        friends.getChildren().add(offlineFriendsLabel);
        offlineFriendsView = new ContactList<>(offlineFriends);
        friends.getChildren().add(offlineFriendsView);

        Label strangerLabel = new Label("Strangers");
        friends.getChildren().add(strangerLabel);
        onlineStrangersView = new ContactList<>(onlineStrangers);
        friends.getChildren().add(onlineStrangersView);

        //Chat - Right
        BorderPane chat = new BorderPane();
        chat.setPadding(new Insets(5));
        chatPane.setCenter(chat);

        BorderPane chatTop = new BorderPane();
        HBox chatNameBox = new HBox();
        chatNameBox.setSpacing(5);
        Label chatName = new Label("Chat name: ");
        currentChat = new Label("");
        chatNameBox.getChildren().addAll(chatName,currentChat);
        chatTop.setLeft(chatNameBox);
        chat.setTop(chatTop);
        Button changePassword = new Button("Update Password");
        chatTop.setRight(changePassword);
        changePassword.setOnAction(event -> openPassword());

        chatMessages = new ListView<>();
        chat.setCenter(chatMessages);

        HBox sendMessageBox = new HBox();
        sendMessageBox.setPadding(new Insets(10,0,0,0));
        sendMessageBox.setSpacing(5);

        TextField sendMessageField = new TextField();
        Button sendMessageButton = new Button("Send");
        sendMessageField.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
            if(keyEvent.getCode()== KeyCode.ENTER){
                submitMessage(sendMessageField.getText());
            }
        });
        sendMessageButton.setOnAction(event -> submitMessage(sendMessageField.getText()));
        sendMessageBox.getChildren().addAll(sendMessageField,sendMessageButton);
        HBox.setHgrow(sendMessageField, Priority.ALWAYS);
        chat.setBottom(sendMessageBox);

        //TODO REMOVE
        chatPane.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("h: "+newValue.getHeight()+" w: "+newValue.getWidth());
        });
        //TODO REMOVE HARDCODE
        onlineFriends.get(0).appendChat("[GRANT] Hey Anthony, can you ...");
        onlineFriends.get(0).appendChat("[ANTHONY] What do you need Grant, id be happy to help!");


        // Create a scene and place it in the stage
        Scene s = new Scene(chatPane,650,500);
        chatStage.setTitle("Chat Window"); // Set the stage title
        chatStage.setScene(s); // Place the scene in the stage
        chatStage.show(); // Display the stage
    }

    private void submitMessage(String message){
        currentContact.submitMessage(message);
    }

	private boolean connectToServer(String ip,Label statusLabel) {
		try {
			// Create a socket to connect to the server
			@SuppressWarnings("resource")
			Socket socket = new Socket(ip, 8000);

			// Create an input stream to receive data from the server
			fromServer = new DataInputStream(socket.getInputStream());

			// Create an output stream to send data to the server
			toServer = new DataOutputStream(socket.getOutputStream());
		}
		catch (IOException ex) {
			statusLabel.setText("Status: Failed");
			statusLabel.setTextFill(Color.FIREBRICK);
			return false;
		}
		return true;
	}

	private boolean loginToServer(ClientAction clientaction,String user, String pass,Label statusLabel){
		try {

            //setup new Thread to receive from Server
            sa = new ServerRecieve(this,fromServer);
            recieve = new Thread(sa);
            recieve.start();

            prepWaitForServer(ServerAction.LOGINSUCCESS);

            // create query
            String query = Parser.packageStrings(clientaction,user,pass);
            //send server register
			sendQuery(query);

            //wait for response
            waitForServer(ServerAction.LOGINSUCCESS);
            if(clientId==-1)
                throw new IOException();

            return true;
		} catch (IOException ex){
            statusLabel.setText("Status: Failed");
            statusLabel.setTextFill(Color.FIREBRICK);
		}
		return false;
	}

	public void sendQuery(String q){
        try{
            toServer.writeChars(q);
            toServer.flush();
        } catch (IOException ex){
            ex.printStackTrace();
        }
    }

    boolean flags[] = new boolean[ServerAction.values().length];

    public void prepWaitForServer(ServerAction sa){
        flags[sa.ordinal()] = true;
    }

    public void waitForServer(ServerAction sa){
        while(flags[sa.ordinal()]){}
    }

	public static void main(String[] args) {
		launch(args);
	}

    public void updateContactList(ContactList<Contact> contactList, Collection<Contact> contacts) {
        contactList.setList(contacts);
    }
}

class ServerRecieve implements Runnable {

    DataInputStream fromServer;
    Client owner;

    public ServerRecieve(Client c, DataInputStream fs){
        owner = c;
        fromServer = fs;
    }

    @Override
    public void run() {
        while(true){
            try {
                String[] action = Parser.parseString(fromServer.readUTF());
                ServerAction sa = ServerAction.valueOf(action[0]);
                owner.flags[sa.ordinal()]=false;    //set flag for blocking
                List<String> messages;
                int groupId;
                switch(sa){  //TODO
                    case LOGINSUCCESS:
                        owner.clientId = Integer.parseInt(action[1]);
                        break;
                    case FRIENDADDED:
                    case FRIENDREMOVED: //TODO check w/grant if server already sends us this information
                        owner.sendQuery(Parser.packageStrings(ClientAction.GETFRIENDS));
                        owner.sendQuery(Parser.packageStrings(ClientAction.GETSTRANGERS));
                        break;
                    case FRIENDS:
                        messages = Arrays.asList(action);
                        messages.remove(0);   //ServerAction,
                        owner.onlineFriends=messages.stream().map(s -> new Contact(s,true,owner)).collect(Collectors.toList());
                        owner.updateContactList(owner.onlineFriendsView,owner.onlineFriends);
                        //TODO GET MESSAGE HISTORY
                        break;
                    case OFFLINEFRIENDS:
                        messages = Arrays.asList(action);
                        messages.remove(0);   //ServerAction,
                        owner.offlineFriends=messages.stream().map(s -> new Contact(s,true,owner)).collect(Collectors.toList());
                        owner.updateContactList(owner.offlineFriendsView,owner.offlineFriends);
                        //TODO GET MESSAGE HISTORY
                        break;
                    case STRANGERS:
                        messages = Arrays.asList(action);
                        messages.remove(0);   //ServerAction,
                        owner.onlineStrangers=messages.stream().map(s -> new Contact(s,false,owner)).collect(Collectors.toList());
                        owner.updateContactList(owner.onlineStrangersView,owner.onlineStrangers);
                        //TODO GET MESSAGE HISTORY
                        break;
                    case GROUPS:
                        messages = Arrays.asList(action);
                        messages.remove(0);   //ServerAction,
                        HashMap<Integer,Contact> groups = new HashMap<>();
                        for (int i = 0; i<messages.size(); i+=2) {
                            groupId = Integer.parseInt(messages.get(i+1));
                            groups.put(groupId,new Group(messages.get(i),groupId,owner));
                        }
                        owner.groups = groups;
                        owner.updateContactList(owner.groupsView,owner.groups.values());
                        //TODO GET MESSAGE HISTORY
                        break;
                    case GROUPMESSAGESENT: //TODO check w/grant if server already sends us this information
                        owner.sendQuery(Parser.packageStrings(ClientAction.GETGROUPMESSAGEHISTORY,action[1]));
                        break;
                    case LEFTGROUP: //TODO check w/grant if server already sends us this information
                    case MAKEGROUP:
                        owner.sendQuery(Parser.packageStrings(ClientAction.GETGROUPS));
                        break;
                    case MESSAGEHISTORY:
                        messages = Arrays.asList(action);
                        messages.remove(0); messages.remove(0);   //ServerAction, username
                        //action[1] is username
                        //TODO find user, add message
                        break;
                    case MESSAGESENT: //TODO check w/grant if server already sends us this information
                        owner.sendQuery(Parser.packageStrings(ClientAction.GETMESSAGEHISTORY,action[1]));
                        break;


                    case UPDATEPASSWORDRESULT:
                        break;

                    case GROUPMESSAGEHISTORY:
                        groupId = Integer.parseInt(action[1]);
                        messages = Arrays.asList(action);
                        messages.remove(0); messages.remove(0);   //ServerAction, groupId
                        owner.groups.get(groupId).setChatHistory(messages);
                        break;
                }
            }
            catch (IOException ex){
                ex.printStackTrace();
            }
        }
    }
}

class ContactList<E> extends ListView<E> {

    ChangeListener listener;

    public ContactList(){
        super();
        listener = (a,b,c) -> Platform.runLater(() -> getSelectionModel().select(-1));
        getSelectionModel().selectedIndexProperty().addListener(listener);
    }

    public ContactList(List<E> items){
        this();
        setList(items);
    }

    public void setList(Collection<E> items){
        setItems(FXCollections.observableArrayList(items));
    }

    public void removeItem(E item){
        ArrayList<E> items = new ArrayList<>(getItems());
        items.remove(item);
        setItems(FXCollections.observableArrayList(items));
    }

    public void addItem(E item){
        ArrayList<E> items = new ArrayList<>(getItems());
        items.add(item);
        setItems(FXCollections.observableArrayList(items));
    }
}

class Plus extends Polygon{
    public Plus(double width, double height) {
        getPoints().addAll(
                0.0, height/4,
                width/4,height/4,
                width/4,0.0,
                3*width/4,0.0,
                3*width/4,height/4,
                width,height/4,
                width,3*height/4,
                3*width/4,3*height/4,
                3*width/4,height,
                width/4,height,
                width/4,3*height/4,
                0.0,3*height/4
        );
    }
}

class Minus extends Polygon{
    public Minus(double width, double height) {
        getPoints().addAll(
                0.0, height/4,
                width,height/4,
                width,3*height/4,
                0.0,3*height/4
        );
    }
}
