package mg.itu.prom16;

import com.thoughtworks.paranamer.BytecodeReadingParanamer;
import com.thoughtworks.paranamer.Paranamer;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.itu.prom16.AnnotationController;
import mg.itu.prom16.GetAnnotation;
import mg.itu.prom16.ModelView;
import mg.itu.prom16.Param;
import mg.itu.prom16.Post;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FrontController extends HttpServlet {
    private List<String> controller = new ArrayList<>();
    private String controllerPackage;
    private boolean checked = false;
    private HashMap<String, Mapping> lien = new HashMap<>();
    private String error = "";

    @Override
    public void init() throws ServletException {
        super.init();
        controllerPackage = getInitParameter("controller-package");
        try {
            this.scan();
        } catch (Exception e) {
            error = e.getMessage();
        }
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        String[] requestUrlSplitted = request.getRequestURL().toString().split("/");
        String controllerSearched = requestUrlSplitted[requestUrlSplitted.length - 1];

        response.setContentType("text/html");
        if (!error.isEmpty()) {
            out.println(error);
        } else if (!lien.containsKey(controllerSearched)) {
            out.println("<p>Méthode non trouvée.</p>");
        } else {
            try {
                Mapping mapping = lien.get(controllerSearched);
                Class<?> clazz = Class.forName(mapping.getClassName());
                Method method = null;

                // Find the method that matches the request type (GET or POST)
                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.getName().equals(mapping.getMethodeName())) {
                        if (request.getMethod().equalsIgnoreCase("GET") && m.isAnnotationPresent(GetAnnotation.class)) {
                            method = m;
                            break;
                        } else if (request.getMethod().equalsIgnoreCase("POST") && m.isAnnotationPresent(Post.class)) {
                            method = m;
                            break;
                        }
                    }
                }

                if (method == null) {
                    out.println("<p>Aucune méthode correspondante trouvée.</p>");
                    return;
                }
                Object[] parameters = getMethodParameters(method, request, response);

                Object object = clazz.getDeclaredConstructor().newInstance();
                Object returnValue = method.invoke(object, parameters);

                if (returnValue instanceof String) {
                    out.println("Méthode trouvée dans " + returnValue);
                } else if (returnValue instanceof ModelView) {
                    ModelView modelView = (ModelView) returnValue;
                    for (Map.Entry<String, Object> entry : modelView.getData().entrySet()) {
                        request.setAttribute(entry.getKey(), entry.getValue());
                    }
                    RequestDispatcher dispatcher = request.getRequestDispatcher(modelView.getUrl());
                    dispatcher.forward(request, response);
                } else {
                    out.println("Type de données non reconnu");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        out.close();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    public void scan() throws Exception {
        try {
            String classesPath = getServletContext().getRealPath("/WEB-INF/classes");
            String decodedPath = URLDecoder.decode(classesPath, "UTF-8");
            String packagePath = decodedPath + "\\" + controllerPackage.replace('.', '\\');
            File packageDirectory = new File(packagePath);
            if (!packageDirectory.exists() || !packageDirectory.isDirectory()) {
                throw new Exception("Package n'existe pas");
            } else {
                File[] classFiles = packageDirectory.listFiles((dir, name) -> name.endsWith(".class"));
                if (classFiles != null) {
                    for (File classFile : classFiles) {
                        String className = controllerPackage + '.'
                                + classFile.getName().substring(0, classFile.getName().length() - 6);
                        try {
                            Class<?> classe = Class.forName(className);
                            if (classe.isAnnotationPresent(AnnotationController.class)) {
                                controller.add(classe.getSimpleName());

                                Method[] methodes = classe.getDeclaredMethods();

                                for (Method methode : methodes) {
                                    if (methode.isAnnotationPresent(GetAnnotation.class)) {
                                        Mapping map = new Mapping(className, methode.getName());
                                        String valeur = methode.getAnnotation(GetAnnotation.class).value();
                                        if (lien.containsKey(valeur)) {
                                            throw new Exception("double url" + valeur);
                                        } else {
                                            lien.put(valeur, map);
                                        }
                                    } else if (methode.isAnnotationPresent(Post.class)) {
                                        Mapping map = new Mapping(className, methode.getName());
                                        String valeur = methode.getAnnotation(Post.class).value();
                                        if (lien.containsKey(valeur)) {
                                            throw new Exception("double url" + valeur);
                                        } else {
                                            lien.put(valeur, map);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            throw e;
                        }

                    }
                } else {
                    throw new Exception("le package est vide");
                }
            }
        } catch (Exception e) {
            throw e;
        }
    }

    private Object[] getMethodParameters(Method method, HttpServletRequest request, HttpServletResponse response) {
        Paranamer paranamer = new BytecodeReadingParanamer();
        String[] parameterNames = paranamer.lookupParameterNames(method);

        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] parameterValues = new Object[parameterNames.length];

        for (int i = 0; i < parameterNames.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            String paramName = parameterNames[i];

            if (parameterType.equals(HttpServletRequest.class)) {
                parameterValues[i] = request;
            } else if (parameterType.equals(HttpServletResponse.class)) {
                parameterValues[i] = response;
            } else 
                Annotation[] annotations = method.getParameterAnnotations()[i];
                for (Annotation annotation : annotations) {
                    if (annotation instanceof Param) {
                        String paramValue = request.getParameter(((Param) annotation).value());

                        // Convertir les types si nécessaire (ici, int pour la page)
                        if (parameterType.equals(int.class)) {
                            parameterValues[i] = Integer.parseInt(paramValue);
                        } else {
                            parameterValues[i] = paramValue;
                        }
                    }
                }
            }
        }

        return parameterValues;
    }
}

class Mapping {
    private String className;
    private String methodeName;

    public Mapping(String className, String methodeName) {
        this.className = className;
        this.methodeName = methodeName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodeName() {
        return methodeName;
    }

    public void setMethodeName(String methodeName) {
        this.methodeName = methodeName;
    }
}
