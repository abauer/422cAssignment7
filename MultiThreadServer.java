package assignment7;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;


public class MultiThreadServer extends Application
{ // Text area for displaying contents 
	private TextArea ta = new TextArea();

    // open sockets
    private final ArrayList<Socket> openSockets = new ArrayList<>();

	// Number a client 
	private int clientNo = 0; 

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

                new Thread( () -> {
                    while (true) {
                        System.out.println("Threads alive: "+openSockets.size());
                        // TODO currently unnecessary, as sockets are manually removed from this list
                        synchronized(openSockets) {
                            Iterator<Socket> iter = openSockets.iterator();
                            while (iter.hasNext()) {
                                Socket socket = iter.next();
                                if (socket.isClosed()) {
                                    iter.remove();
                                    System.out.println("EXECUTED");
                                }
                            }
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {}
                    }
                }).start();

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
					new Thread(new HandleAClient(socket)).start();
                    synchronized(openSockets) {
                        openSockets.add(socket);
                    }
				} 
			} 
			catch(IOException ex) { 
				System.err.println(ex);
			}
		}).start();
	}


	// Define the thread class for handling
	class HandleAClient implements Runnable {
		private Socket socket; // A connected socket
		/** Construct a thread */ 
		public HandleAClient(Socket socket) { 
			this.socket = socket;
		}
		/** Run a thread */
		public void run() { 
			try {
				// Create data input and output streams
				DataInputStream inputFromClient = new DataInputStream( socket.getInputStream());
				DataOutputStream outputToClient = new DataOutputStream( socket.getOutputStream());
				// Continuously serve the client
				while (true) { 
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
	
	public static void main(String[] args) {
		launch(args);
	}
}