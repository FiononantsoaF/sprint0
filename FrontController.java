package mg.itu.prom16;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

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

    private void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String path = request.getPathInfo();
        if (path != null && path.length() > 1) {
            String identifier = path.substring(1);
            Mapping mapping = urlMapping.get(identifier);
            if (mapping != null) {
                handleMethodInvocation(out, mapping, request, response);
                displayMappingDetails(out, mapping);
            } else {
                out.println("<h2 style='color:red'>Aucun mapping trouvé pour l'URL : " + path + "</h2>");
            }
        } else {
            out.println("<h2 style='color:red'>Aucun path fourni</h2>");
        }
        out.close();
    }

    private void handleMethodInvocation(PrintWriter out, Mapping mapping, HttpServletRequest request, HttpServletResponse response) {
        try {
            Object controllerInstance = mapping.getControllerClass().getDeclaredConstructor().newInstance();
            Object result = mapping.getMethod().invoke(controllerInstance);

            if (result instanceof String) {
                displayStringResult(out, (String) result);
            } else if (result instanceof ModelView) {
                displayModelViewResult(out, (ModelView) result, request, response);
            } else {
                out.println("<p>Résultat non reconnu : " + result + "</p>");
            }
        } catch (Exception e) {
            out.println("<p style='color:red'>Erreur lors de l'invocation de la méthode: " + e.getMessage() + "</p>");
            e.printStackTrace(out);
        }
        out.println("<hr>");
    }

    private void displayStringResult(PrintWriter out, String result) {
        out.println("<p>Résultat de la méthode : " + result + "</p>");
    }

    private void displayModelViewResult(PrintWriter out, ModelView mv, HttpServletRequest request, HttpServletResponse response) {
        for (Map.Entry<String, Object> entry : mv.getData().entrySet()) {
            request.setAttribute(entry.getKey(), entry.getValue());
        }
        dispatchModelView(out, mv, request, response);
    }

    private void dispatchModelView(PrintWriter out, ModelView mv, HttpServletRequest request, HttpServletResponse response) {
        try {
            request.getRequestDispatcher(mv.getUrl()).forward(request, response);
        } catch (ServletException | IOException e) {
            out.println("<p style='color:red'>Erreur lors de la redirection vers la page de vue: " + e.getMessage() + "</p>");
            e.printStackTrace(out);
        }
    }

    private void displayMappingDetails(PrintWriter out, Mapping mapping) {
        out.println("<h3>Détails du mapping :</h3>");
        out.println("<p>Controller : " + mapping.getControllerClass().getName() + "</p>");
        out.println("<p>Méthode : " + mapping.getMethod().getName() + "</p>");
        out.println("<p>Type de retour : " + mapping.getMethod().getReturnType().getSimpleName() + "</p>");
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
