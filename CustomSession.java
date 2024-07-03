package mg.itu.prom16;

import jakarta.servlet.http.HttpSession;

public class CustomSession {
    HttpSession session;

    public CustomSession(HttpSession session) {
        this.session = session;
    }

    public Object get(String key) {
        return session.getAttribute(key);
    }

    public void add(String key, Object object) {
        session.setAttribute(key, object);
    }

    public void delete(String key) {
        session.removeAttribute(key);
    }
}
