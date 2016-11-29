package assignment7;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
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

import javax.swing.event.ChangeListener;


public class Client extends Application {
	// IO streams 
	DataOutputStream toServer = null; 
	DataInputStream fromServer = null;
	int userId;
    ArrayList<Contact> groups;
    ArrayList<Contact> onlineFriends;
    ArrayList<Contact> offlineFriends;
    ArrayList<Contact> onlineStrangers;


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
		TextField ipField = new TextField("localhost");
		serverBox.getChildren().addAll(serverLabel,ipField);

		loginBox.getChildren().add(serverBox);

		HBox usernameBox = new HBox();
		usernameBox.setSpacing(10);
		usernameBox.setAlignment(Pos.CENTER);

		Label usernameLabel = new Label("Username: ");
		TextField usernameField = new TextField("demo");
		usernameBox.getChildren().addAll(usernameLabel,usernameField);

		loginBox.getChildren().add(usernameBox);

		HBox passwordBox = new HBox();
		passwordBox.setSpacing(10);
		passwordBox.setAlignment(Pos.CENTER);

		Label passwordLabel = new Label("Password: ");
		PasswordField passwordField = new PasswordField();
		passwordField.setText("grantisawesome");
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
			loginToServer(ClientAction.REGISTER,usernameField.getText(),passwordField.getText());
            openChat();
			loginStage.close();
		});
		Button loginButton = new Button("Login");
		registerButton.setOnAction(e -> {
	//		boolean success = connectToServer(ipField.getText(),statusLabel);
	//		if(!success)
	//			return;
	//		loginToServer(ClientAction.LOGIN,usernameField.getText(),passwordField.getText());
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

	private void openChat(){

        groups = new ArrayList<>();
        groups.add(new Contact("Senior Design",true)); groups.add(new Contact("HackDFW",true)); groups.add(new Contact("Frist Allo",true));

        onlineFriends = new ArrayList<>();
        onlineFriends.add(new Contact("Grant",true));

        offlineFriends = new ArrayList<>();
        offlineFriends.add(new Contact("Rony",true));

        onlineStrangers = new ArrayList<>();
        onlineStrangers.add(new Contact("BruceBanner",false)); onlineStrangers.add(new Contact("BilboBaggins",false));

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
        groupTitle.setLeft(groupLabelAlign);
        groupTitle.setRight(addGroupButton);
        friends.getChildren().add(groupTitle);
        ContactList<Contact> groupsView = new ContactList<>(groups);
        friends.getChildren().add(groupsView);


        Label onlineFriendsLabel = new Label("Online Friends");
        friends.getChildren().add(onlineFriendsLabel);
        ContactList<Contact> onlineFriendsView = new ContactList<>(onlineFriends);
        friends.getChildren().add(onlineFriendsView);

        Label offlineFriendsLabel = new Label("Offline Friends");
        friends.getChildren().add(offlineFriendsLabel);
        ContactList<Contact> offlineFriendsView = new ContactList<>(offlineFriends);
        friends.getChildren().add(offlineFriendsView);

        Label strangerLabel = new Label("Strangers");
        friends.getChildren().add(strangerLabel);
        ContactList<Contact> onlineStrangersView = new ContactList<>(onlineStrangers);
        friends.getChildren().add(onlineStrangersView);

        //Chat - Right
        BorderPane chat = new BorderPane();
        chat.setPadding(new Insets(5));
        chatPane.setCenter(chat);

        ListView<Label> messages = new ListView<>();
        chat.setCenter(messages);

        HBox sendMessageBox = new HBox();
        sendMessageBox.setPadding(new Insets(10,0,0,0));

        TextField sendMessageField = new TextField();
        Button sendMessageButton = new Button("Send");
        sendMessageBox.getChildren().addAll(sendMessageField,sendMessageButton);
        HBox.setHgrow(sendMessageField, Priority.ALWAYS);
        chat.setBottom(sendMessageBox);

        chatPane.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("h: "+newValue.getHeight()+" w: "+newValue.getWidth());
        });

        // Create a scene and place it in the stage
        Scene s = new Scene(chatPane,650,490);
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

	private void loginToServer(ClientAction clientaction,String user, String pass){
		try {
			// create query
			String query = clientaction + " " + user + " " + pass;

			//send server register
			toServer.writeChars(query);
			toServer.flush();

			//wait for response
			userId = fromServer.readInt();
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
    HBox notifications;

    public Contact(String name, boolean friend){
        this.name = new Label(name);
        notifications = new HBox();
        notifications.setSpacing(5);
        Contact myself = this;
        addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            System.out.println("i was clicked: i am "+myself.name.getText());
            toggleUnread();
        });
        notify = new Circle(5);
        notify.setFill(Color.ORANGERED);
        addFriend = new Plus(5,5);
        addFriend.setFill(Color.GREEN);
        unfriend = new Minus(5,5);
        unfriend.setFill(Color.RED);
        this.friend = friend;
        setLeft(this.name);
        setRight(notifications);
        recompileHBox();
    }

    public void toggleFriend(){
        friend = !friend;
        recompileHBox();
    }

    public void toggleUnread() {
        unread = !unread;
        recompileHBox();
    }

    private void recompileHBox() {
        notifications.getChildren().clear();
        addFriend();
        addUnread();
    }

    private void addUnread(){
        if(unread){
            notifications.getChildren().add(notify);
        }
    }

    private void addFriend(){
        if(friend){
            notifications.getChildren().add(unfriend);
        }
        else{
            notifications.getChildren().add(addFriend);
        }
    }



    @Override
    public String toString(){
        return name.getText();
    }
}

class ContactList<E> extends ListView<E> {

    public ContactList(ArrayList<E> items){
        super();
        ContactList<E> myself = this;
        myself.getSelectionModel().selectedIndexProperty().addListener(e -> Platform.runLater(() -> myself.getSelectionModel().select(-1)));
        setItems(FXCollections.observableArrayList(items));
    }
}

class Plus extends Polygon{
    public Plus(double width, double height) {
        width-=2;
        height-=2;
        getPoints().addAll(
                0.0, height,
                width/2, 0.0,
                width, height
        );
    }
}

class Minus extends Polygon{
    public Minus(double width, double height) {
        width-=2;
        height-=2;
        getPoints().addAll(
                0.0, height/4,
                width/4, height/4,
                width/2, 0.0,
                3*width/4, height/4,
                width, height/4,
                3*width/4, 2*height/3,
                7*width/8, height,
                width/2, 3*height/4,
                width/8, height,
                width/4, 2*height/3
        );
    }
}
