package mg.itu.prom16;

public class VerbAction {
    private String verb;  
    private String method;  

    public VerbAction(String verb, String method) {
        this.verb = verb;
        this.method = method;
    }

    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }

    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public String toString() {
        return "VerbAction{" +
                "verb='" + verb + '\'' +
                ", method='" + method + '\'' +
                '}';
    }
}
