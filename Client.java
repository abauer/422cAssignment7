package assignment7;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
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
    ListView<Label> chatMessages;

    ArrayList<Contact> groups;
    ContactList<Contact> groupsView;
    ArrayList<Contact> onlineFriends;
    ContactList<Contact> onlineFriendsView;
    ArrayList<Contact> offlineFriends;
    ContactList<Contact> offlineFriendsView;
    ArrayList<Contact> onlineStrangers;
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
			success = loginToServer(ClientAction.REGISTER,Parser.cleanString(usernameField.getText()),passwordField.getText());
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
	//		success = loginToServer(ClientAction.LOGIN,Parser.cleanString(usernameField.getText()),passwordField.getText());
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



        //TODO request lists from server instead of hardcode
        //get online people / friends
        groups = new ArrayList<>();
        groups.add(new Contact("Senior Design",true,this)); groups.add(new Contact("HackDFW",true,this)); groups.add(new Contact("Frist Allo",true,this));

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
        groupsView = new ContactList<>(groups);
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

	private boolean loginToServer(ClientAction clientaction,String user, String pass){
		try {
			// create query
			String query = Parser.packageStrings(clientaction,user,pass);
			//send server register
			sendQuery(query);

			//wait for response
			clientId = fromServer.readInt();
            if(clientId==-1)
                return false;
            //setup new Thread to recieve from Server
            sa = new ServerRecieve(fromServer);
            recieve = new Thread(sa);
            recieve.start();
            return true;
		} catch (IOException ex){
			ex.printStackTrace();
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

	public static void main(String[] args) {
		launch(args);
	}
}

class Contact extends BorderPane{

    Label name;
    boolean friend;
    boolean unread;
    Shape notify;
    Shape addFriend;
    Shape unfriend;
    HBox left;
    HBox right;
    Client owner;

    List<String> chatHistory;

    public Contact(String name){
        super();
        this.name = new Label(name);
        HBox nameBox = new HBox();
        nameBox.setAlignment(Pos.CENTER_LEFT);
        nameBox.getChildren().add(this.name);
        nameBox.setPadding(new Insets(0,0,0,5));
        setCenter(nameBox);
        chatHistory = new ArrayList<>();
    }

    public Contact(String name, boolean friend, Client c){
        this(name);
        this.friend = friend;
        owner = c;

        left = new HBox();
        left.setSpacing(5);
        left.setAlignment(Pos.CENTER_RIGHT);
        right = new HBox();
        right.setSpacing(5);
        right.setAlignment(Pos.CENTER_RIGHT);
        notify = new Circle(8);
        notify.setFill(Color.ORANGE);
        addFriend = new Plus(16,16);
        addFriend.setFill(Color.GREEN);
        unfriend = new Minus(16,16);
        unfriend.setFill(Color.RED);

        setHandlers();

        setLeft(left);
        setRight(right);
        recompileHBox();
    }

    private void setHandlers(){
        right.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if(friend){
                String query = Parser.packageStrings(ClientAction.REMOVEFRIEND,name.getText());
                owner.sendQuery(query);
            }
            else{
                String query = Parser.packageStrings(ClientAction.ADDFRIEND,name.getText());
                owner.sendQuery(query);
            }
            event.consume();
        });
        addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            setActiveChat();
        });
    }

    public void appendChat(String s){
        if(!owner.currentChat.getText().equals(name.getText()))
            setUnread(true);
        chatHistory.add(s);
    }

    public void setChatHistory(List<String> s){
        chatHistory = s;
    }

    public void setActiveChat(){
        if(unread)
            setUnread(false);
        owner.currentChat.setText(name.getText());
        owner.chatMessages.setItems(FXCollections.observableList(chatHistory.stream().map(s->new Label(s)).collect(Collectors.toList())));
    }

    public void setFriend(boolean friend){
        this.friend = friend;
        //TODO CHANGE ARRAY
        recompileHBox();
    }

    public void setUnread(boolean unread) {
        this.unread = unread;
        recompileHBox();
    }

    private void recompileHBox() {
        right.getChildren().clear();
        left.getChildren().clear();
        addFriend();
        addUnread();
    }

    private void addUnread(){
        if(unread){
            left.getChildren().add(notify);
        }
    }

    private void addFriend(){
        if(friend){
            right.getChildren().add(unfriend);
        }
        else{
            right.getChildren().add(addFriend);
        }
    }



    @Override
    public String toString(){
        return name.getText();
    }
}

class ServerRecieve implements Runnable {

    DataInputStream fromServer;

    public ServerRecieve(DataInputStream fs){
        fromServer = fs;
    }

    @Override
    public void run() {
        while(true){
            try {
                String server_response = fromServer.readUTF();
                //parse server response
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
