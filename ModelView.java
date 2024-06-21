package mg.itu.prom16;
import java.util.Map;
import java.util.HashMap;
public class ModelView {
    private String url;
    private Map<String, Object> data = new HashMap<>();

    public ModelView(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void addObject(String key, Object value) {
        data.put(key, value);
    }
}



