import java.util.HashMap;
import java.util.Map;

public class UserAuthentication {
    private Map<String, String> userCredentials = new HashMap<>();

    public UserAuthentication() {
        // Predefined usernames and passwords
        userCredentials.put("Jomarc", "pass1");
        userCredentials.put("Mike", "pass2");
        userCredentials.put("Jemuel", "pass3");
    }

    public boolean authenticate(String username, String password) {
        return userCredentials.containsKey(username) && userCredentials.get(username).equals(password);
    }
}
