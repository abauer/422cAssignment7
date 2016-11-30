package assignment7;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public static int sendGroupMessage(int client, int groupId, String message){
        Map<String,Object> params = new HashMap<>();
        params.put("type", "send_group_message");
        params.put("groupid", groupId);
        params.put("clientid", client);
        params.put("message", message);
        return DatabaseAccessor.intQuery(params);
    }

    public static int sendMessage(int client, String username, String message){
        Map<String,Object> params = new HashMap<>();
        params.put("type", "send_message");
        params.put("recipient", username);
        params.put("clientid", client);
        params.put("message", message);
        return DatabaseAccessor.intQuery(params);
    }

    public static List<String> getFriends(int client){
        Map<String,Object> params = new HashMap<>();
        params.put("type", "get_friends");
        params.put("clientid", client);
        return DatabaseAccessor.strListQuery(params);
    }

    public static int makeChat(int client,String groupName, List<String> friends){
        Map<String,Object> params = new HashMap<>();
        params.put("type", "make_chat");
        params.put("clientid", client);
        params.put("groupname",groupName);
        params.put("groupmembers",friends.stream().collect(Collectors.joining(",")));
        return DatabaseAccessor.intQuery(params);
    }

    public static int addFriend(int client,String username){
        Map<String,Object> params = new HashMap<>();
        params.put("type", "add_friend");
        params.put("clientid", client);
        params.put("username", username);
        return DatabaseAccessor.intQuery(params);
    }

    public static int removeFriend(int client,String username){
        Map<String,Object> params = new HashMap<>();
        params.put("type", "remove_friend");
        params.put("clientid", client);
        params.put("username", username);
        return DatabaseAccessor.intQuery(params);
    }

    public static List<String> getMessageHistory(int client,String username){
        Map<String,Object> params = new HashMap<>();
        params.put("type", "get_message_history");
        params.put("clientid", client);
        params.put("username", username);
        return DatabaseAccessor.strListQuery(params);
    }

    public static List<String> getGroupMessageHistory(int client,int groupId){
        Map<String,Object> params = new HashMap<>();
        params.put("type", "get_group_message_history");
        params.put("clientid", client);
        params.put("groupid", groupId);
        return DatabaseAccessor.strListQuery(params);
    }

    public static int leaveGroup(int client, int groupId){
        Map<String,Object> params = new HashMap<>();
        params.put("type", "leave_group");
        params.put("clientid", client);
        params.put("groupid", groupId);
        return DatabaseAccessor.intQuery(params);
    }
}
