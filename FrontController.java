package mg.itu.prom16;
import com.google.gson.Gson;

import mg.itu.prom16.AnnotationController;
import mg.itu.prom16.GetAnnotation;
import mg.itu.prom16.Post;
import mg.itu.prom16.Param;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLDecoder;

import mg.itu.prom16.ModelView; 
import mg.itu.prom16.AnnotationClass;
import java.lang.reflect.Field;  
import java.io.*;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ExecutionException;
import javax.servlet.RequestDispatcher; 
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class FrontController extends HttpServlet {
    private List<String> controller = new ArrayList<>();
    private String controllerPackage;
    boolean checked = false;
    HashMap<String, Mapping> lien = new HashMap<>();
    String error = "";

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
    
                Object[] parameters = getMethodParameters(method, request);
                Object object = clazz.getDeclaredConstructor().newInstance();
                Object returnValue = method.invoke(object, parameters);
    
                // Vérifier l'annotation RestAPI
                if (method.isAnnotationPresent(Restapi.class)) {
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
    
                    Gson gson = new Gson();
                    if (returnValue instanceof ModelView) {
                        // Si c'est un ModelView, convertir uniquement l'attribut "data"
                        ModelView modelView = (ModelView) returnValue;
                        response.getWriter().write(gson.toJson(modelView.getData()));
                    } else {
                        // Si ce n'est pas un ModelView, convertir tout le résultat en JSON
                        response.getWriter().write(gson.toJson(returnValue));
                    }
                } else {
                    // Comportement existant si l'annotation n'est pas présente
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

    private Object createRequestBodyParameter(Parameter parameter, Map<String, String[]> paramMap) throws Exception {
        Class<?> paramType = parameter.getType();
        Object paramObject = paramType.getDeclaredConstructor().newInstance();
        for (Field field : paramType.getDeclaredFields()) {
            String paramName = field.getName();
            if (paramMap.containsKey(paramName)) {
                String paramValue = paramMap.get(paramName)[0]; // Assuming single value for simplicity
                field.setAccessible(true);
                field.set(paramObject, paramValue);
            }
        }
        return paramObject;
    }
    
    private Object[] getMethodParameters(Method method, HttpServletRequest request) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] parameterValues = new Object[parameters.length];
    
        HttpSession session = request.getSession();
    
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.getType() == CustomSession.class) {
                parameterValues[i] = new CustomSession(session);
            } else if (parameter.isAnnotationPresent(AnnotationClass.class)) {
                parameterValues[i] = createRequestBodyParameter(parameter, request.getParameterMap());
            } else if (parameter.isAnnotationPresent(Param.class)) {
                Param param = parameter.getAnnotation(Param.class);
                parameterValues[i] = request.getParameter(param.value()); 
            } else {
                throw new IllegalArgumentException("Paramètre non supporté pour cette méthode");
            }
        }
    
        return parameterValues;
    }
    
}

class Mapping {
    String className;
    String methodeName;

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
