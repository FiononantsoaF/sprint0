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
    

    private synchronized void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html;charset=UTF-8");
        String path = request.getPathInfo();
        if (path != null && path.length() > 1) {
            String identifier = path.substring(1);
            Mapping mapping = urlMapping.get(identifier);
            if (mapping != null) {
                try {
                    Object controllerInstance = mapping.getControllerClass().getDeclaredConstructor().newInstance();
                    Object result = mapping.getMethod().invoke(controllerInstance);

                    if (result instanceof ModelView) {
                        ModelView modelView = (ModelView) result;
                        for (Map.Entry<String, Object> entry : modelView.getData().entrySet()) {
                            request.setAttribute(entry.getKey(), entry.getValue());
                        }
                        request.getRequestDispatcher(modelView.getUrl()).forward(request, response);
                    } else {
                        PrintWriter out = response.getWriter();
                        out.println("<h2>Résultat de la méthode : " + result + "</h2>");
                    }
                } catch (Exception e) {
                    PrintWriter out = response.getWriter();
                    out.println("<h2 style='color:red'>Erreur lors de l'invocation de la méthode : " + e.getMessage() + "</h2>");
                    e.printStackTrace(out);
                }
            } else {
                PrintWriter out = response.getWriter();
                out.println("<h2 style='color:red'>Aucun mapping trouvé pour l'URL : " + path + "</h2>");
            }
        } else {
            PrintWriter out = response.getWriter();
            out.println("<h2 style='color:red'>Aucun path fourni</h2>");
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
