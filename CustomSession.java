package mg.itu.prom16;

import java.util.HashMap;

public class CustomSession {
    private HashMap<String, Object> values;

    public CustomSession() {
        values = new HashMap<>();
    }

    public void add(String key, Object value) {
        if (!values.containsKey(key)) {
            values.put(key, value);
        } else {
            System.out.println("Key already exists. Use update method to change the value.");
        }
    }

    public void update(String key, Object value) {
        if (values.containsKey(key)) {
            values.put(key, value);
        } else {
            System.out.println("Key does not exist. Use add method to add the key-value pair.");
        }
    }

    public void remove(String key) {
        if (values.containsKey(key)) {
            values.remove(key);
        } else {
            System.out.println("Key does not exist.");
        }
    }

    public Object get(String key) {
        return values.get(key);
    }
}
