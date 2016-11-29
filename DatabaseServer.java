package assignment7;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseServer {
    public static int login(String username, String password) {
        Map<String,Object> params = new HashMap<>();
        params.put("type", "login");
        params.put("username", username);
        params.put("password", password);
        return DatabaseAccessor.intQuery(params);
    }

    public static int register(String username, String password) {
        Map<String,Object> params = new HashMap<>();
        params.put("type", "register");
        params.put("username", username);
        params.put("password", password);
        return DatabaseAccessor.intQuery(params);
    }

    public static int updatePassword(int client, String current, String newPassword){
        Map<String,Object> params = new HashMap<>();
        params.put("type", "update_password");
        params.put("clientid", client);
        params.put("oldpassword", current);
        params.put("newpassword", newPassword);
        return DatabaseAccessor.intQuery(params);
    }

    public static String sendGroupMessage(int client, int groupId, String message){
        Map<String,Object> params = new HashMap<>();
        params.put("type", "send_group_message");
        params.put("groupid", groupId);
        params.put("clientid", client);
        params.put("message", message);
        return DatabaseAccessor.strQuery(params);
    }

    public static String sendMessage(int client, String username, String message){
        Map<String,Object> params = new HashMap<>();
        params.put("type", "send_message");
        params.put("recipient", username);
        params.put("clientid", client);
        params.put("message", message);
        return DatabaseAccessor.strQuery(params);
    }

    public static List<String> getFriends(int client){
        Map<String,Object> params = new HashMap<>();
        params.put("type", "get_friends");
        params.put("clientid", client);
        return DatabaseAccessor.strListQuery(params);
    }

    //not done
    public static int makeChat(int client,List<String> friends){
        Map<String,Object> params = new HashMap<>();
        params.put("type", "get_friends");
        params.put("clientid", client);
        return DatabaseAccessor.intQuery(params);
    }

    public static int addFriend(int client,String username){
        Map<String,Object> params = new HashMap<>();
        params.put("type", "get_friends");
        params.put("clientid", client);
        return DatabaseAccessor.intQuery(params);
    }

    public static int removeFriend(int client,String username){
        Map<String,Object> params = new HashMap<>();
        params.put("type", "get_friends");
        params.put("clientid", client);
        return DatabaseAccessor.intQuery(params);
    }

}
