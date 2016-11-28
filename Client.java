package assignment7;

import java.io.*; 
import java.net.*; 
import javafx.application.Application; 
import javafx.geometry.Insets; 
import javafx.geometry.Pos; 
import javafx.scene.Scene;
import javafx.scene.control.*;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;


public class Client extends Application { 
	// IO streams 
	DataOutputStream toServer = null; 
	DataInputStream fromServer = null;


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
			//send server register
			//wait for response
			//go to next window
		});
		Button loginButton = new Button("Login");
		registerButton.setOnAction(e -> {
			boolean success = connectToServer(ipField.getText(),statusLabel);
			if(!success)
				return;
			//send login
			//wait for response
			//go to next window
		});
		submitBox.getChildren().addAll(registerButton,loginButton);

		loginBox.getChildren().add(submitBox);

		loginPane.setCenter(loginBox);



/*
		// Panel p to hold the label and text field 
		BorderPane paneForTextField = new BorderPane(); 
		paneForTextField.setPadding(new Insets(5, 5, 5, 5)); 
		paneForTextField.setStyle("-fx-border-color: green"); 
		paneForTextField.setLeft(new Label("Enter a radius: ")); 

		TextField tf = new TextField(); 
		tf.setAlignment(Pos.BOTTOM_RIGHT); 
		paneForTextField.setCenter(tf);

		BorderPane mainPane = new BorderPane(); 
		// Text area to display contents 
		TextArea ta = new TextArea();
		mainPane.setCenter(new ScrollPane(ta)); 
		mainPane.setTop(paneForTextField); 

*/
		// Create a scene and place it in the stage 
		Scene scene = new Scene(loginPane, 450, 200);
		loginStage.setTitle("Login"); // Set the stage title
		loginStage.setScene(scene); // Place the scene in the stage
		loginStage.show(); // Display the stage
/*
		tf.setOnAction(e -> { 
			try { 
				// Get the radius from the text field 
				double radius = Double.parseDouble(tf.getText().trim()); 

				// Send the radius to the server 
				toServer.writeDouble(radius); 
				toServer.flush(); 

				// Get area from the server 
				double area = fromServer.readDouble(); 

				// Display to the text area 
				ta.appendText("Radius is " + radius + "\n"); 
				ta.appendText("Area received from the server is "
						+ area + '\n');

			} 
			catch (IOException ex) { 
				System.err.println(ex); 
			} 
		}); 

		*/
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

	public static void main(String[] args) {
		launch(args);
	}
}
