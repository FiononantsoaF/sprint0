package mg.itu.prom16;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URISyntaxException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationError {
    private Map<String, String> errors = new HashMap<>();
    private Map<String, Object> formData = new HashMap<>();

    public void addError(String field, String message) {
        errors.put(field, message);
    }

    public void addFormValue(String field, Object value) {
        formData.put(field, value);
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public Map<String, Object> getFormData() {
        return formData;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}