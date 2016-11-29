package assignment7;

import java.util.HashMap;
import java.util.Map;

enum DatabaseQuery {
    LOGIN
}

public class DatabaseServer {
    public static int login(String username, String password) {
        Map<String,Object> params = new HashMap<>();
        params.put("type", DatabaseQuery.LOGIN.ordinal());
        params.put("username", username);
        params.put("password", password);
        return DatabaseAccessor.intQuery(params);
    }
}
