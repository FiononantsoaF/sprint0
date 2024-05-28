package mg.itu.prom16;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.lang.*;

public class FrontController extends HttpServlet {
    private final Map<String, Mapping> urlMapping = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        scanControllers(config);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    private synchronized void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("<html>");
            out.println("<head>");
            out.println("<title>FrontController</title>");
            out.println("</head>");
            out.println("<body>");
            // out.println("<h1 style='color:blue'>URL actuelle :</h1>");
            // out.println("<p>" + request.getRequestURL() + "</p>");
    
            String path = request.getPathInfo();
            Mapping mapping = null;
    
            // Vérifie si l'URL contient un identifiant après '/'
            if (path != null && path.length() > 1) {
                String identifier = path.substring(1); // Ignorer le '/' initial
                mapping = urlMapping.get(identifier);
            }
    
            if (mapping != null) {
                try {
                    Object controllerInstance = mapping.getControllerClass().getDeclaredConstructor().newInstance();
                    Object result = mapping.getMethod().invoke(controllerInstance);
                    out.println("<h2>Résultat de la méthode : " + result + "</h2>");
                } catch (Exception e) {
                    out.println("<h2 style='color:red'>Erreur lors de l'invocation de la méthode : " + e.getMessage() + "</h2>");
                    e.printStackTrace(out);
                }

                // out.println("<h2>Mapping trouvé pour l'URL : " + path + "</h2>");
                // out.println("<p>URL: " + mapping.getKey() + "</p>");
                // out.println("<p>Classe : " + mapping.getControllerClass().getName() + "</p>");
                // out.println("<p>Méthode : " + mapping.getMethod().getName() + "</p>");
            } else {
                out.println("<h2 style='color:red'>Aucun mapping trouvé pour l'URL : " + path + "</h2>");
            }
      
            out.println("</body>");
            out.println("</html>");
        }
    }
    
    private void scanControllers(ServletConfig config) {
        String controllerPackage = config.getInitParameter("controller-package");
        System.out.println("Scanning package: " + controllerPackage);

        try {
            String path = "WEB-INF/classes/" + controllerPackage.replace('.', '/');
            File directory = new File(getServletContext().getRealPath(path));
            if (directory.exists()) {
                scanDirectory(directory, controllerPackage);
            } else {
                System.out.println("Directory does not exist: " + directory.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanDirectory(File directory, String packageName) {
        System.out.println("Scanning directory: " + directory.getAbsolutePath());

        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(AnnotationController.class)) {
                        for (Method method : clazz.getDeclaredMethods()) {
                            if (method.isAnnotationPresent(GetAnnotation.class)) {
                                GetAnnotation requestMapping = method.getAnnotation(GetAnnotation.class);
                                String key = requestMapping.value(); 
                                urlMapping.put(key, new Mapping(key, clazz, method)); 
                                System.out.println("Mapped URL: " + key + " to " + clazz.getName() + "." + method.getName());
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

class Mapping {
    private final String key; 
    private final Class<?> controllerClass;
    private final Method method;

    public Mapping(String key, Class<?> controllerClass, Method method) {
        this.key = key;
        this.controllerClass = controllerClass;
        this.method = method;
    }

    public String getKey() {
        return key;
    }

    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public Method getMethod() {
        return method;
    }
}
