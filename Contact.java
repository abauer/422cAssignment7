package assignment7;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Anthony on 11/29/2016.
 */
public class Contact extends BorderPane {

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

//        getMessageHistory();
    }

    protected void getMessageHistory() {
        owner.prepWaitForServer(ServerAction.MESSAGEHISTORY);
        String query = Parser.packageStrings(ClientAction.GETMESSAGEHISTORY,name.getText());
        owner.sendQuery(query);
        owner.waitForServer(ServerAction.MESSAGEHISTORY);
    }

    protected void setHandlers(){
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

    protected void submitMessage(String message){
        String query = Parser.packageStrings(ClientAction.SENDMESSAGE,name.getText(),message);
        owner.sendQuery(query);
    }

    public void appendChat(String message){
        chatHistory.add(message);
        if(!owner.currentChat.getText().equals(name.getText()))
            setUnread(true);
        else{
            owner.chatMessages.setItems(FXCollections.observableList(chatHistory.stream().map(s->new Label(s)).collect(Collectors.toList())));
        }

    }

    public void setChatHistory(List<String> s){
        chatHistory = s;
    }

    public void setActiveChat(){
        if(unread)
            setUnread(false);
        owner.currentChat.setText(name.getText());
        owner.currentContact = this;
        owner.chatMessages.setItems(FXCollections.observableList(chatHistory.stream().map(s->new Label(s)).collect(Collectors.toList())));
    }

    public void setFriend(boolean friend){
        this.friend = friend;
        recompileHBox();
    }

    public void setUnread(boolean unread) {
        if(owner.currentContact.equals(this))
            return;
        this.unread = unread;
        recompileHBox();
    }

    protected void recompileHBox() {
        right.getChildren().clear();
        left.getChildren().clear();
        addFriend();
        addUnread();
    }

    protected void addUnread(){
        if(unread){
            left.getChildren().add(notify);
        }
    }

    protected void addFriend(){
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

    @Override
    public boolean equals(Object o) {return ((Contact)o).name.getText().equals(name.getText());}
}

class Group extends Contact{

    int groupId;

    public Group(String name,int groupId, Client c) {
        super("[Group] "+name,true,c);
        this.groupId = groupId;
    }

    protected void getMessageHistory() {
        owner.prepWaitForServer(ServerAction.GROUPMESSAGEHISTORY);
        String query = Parser.packageStrings(ClientAction.GETGROUPMESSAGEHISTORY,groupId);
        owner.sendQuery(query);
        owner.waitForServer(ServerAction.GROUPMESSAGEHISTORY);
    }

    public void setFriend(boolean friend){
        this.friend = friend;
        if(!friend)
            leaveGroup();
        //TODO CHANGE ARRAY
        recompileHBox();
    }

    protected void submitMessage(String message){
        String query = Parser.packageStrings(ClientAction.SENDGROUPMESSAGE,groupId,message);
        owner.sendQuery(query);
    }

    protected void setHandlers(){
        right.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            String query = Parser.packageStrings(ClientAction.LEAVEGROUP,groupId);
            owner.sendQuery(query);
            event.consume();
        });
        addEventHandler(MouseEvent.MOUSE_CLICKED, event -> setActiveChat());
    }

    void leaveGroup(){

    }
}
