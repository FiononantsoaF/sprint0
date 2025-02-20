package mg.itu.prom16;
import com.google.gson.Gson;

import mg.itu.prom16.GetAnnotation;
import mg.itu.prom16.AnnotationController;
import mg.itu.prom16.Post;
import mg.itu.prom16.Param;
import mg.itu.prom16.VerbAction;
import mg.itu.prom16.RestApi;


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

import jakarta.servlet.http.Part;

import com.google.gson.Gson;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpSession;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@MultipartConfig
public class FrontController extends HttpServlet {
    private List<String> controllerNames = new ArrayList<>();
    private String controllerPackage;
    HashMap<String, Mapping> urlMaping = new HashMap<>();
    String error = "";

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        controllerPackage = config.getInitParameter("controller-package");
        try {
            if (controllerPackage == null || controllerPackage.isEmpty()) {
                throw new Exception("Le nom du package du contrôleur n'est pas specifie.");
            }
            scanControllers(controllerPackage);
        } catch (Exception e) {
            error = e.getMessage();
        }
    }
    private void processData(HttpServletRequest request) {
        String data = request.getParameter("data");
        if (data != null) {
            System.out.println("Processing data: " + data);
        }
    }
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        StringBuffer requestURL = request.getRequestURL();
       // String currentUrl = request.getRequestURI();
        String[] requestUrlSplitted = requestURL.toString().split("/");
        String controllerSearched = requestUrlSplitted[requestUrlSplitted.length - 1];
        // if (request.getMethod().equals("GET")) {
        //     request.getSession().setAttribute("previousUrl", currentUrl);
        // }

        PrintWriter out = response.getWriter();
        int errorCode = 0;
        String errorMessage = null;
        String errorDetails = null;

        response.setContentType("text/html");

        if (!error.isEmpty()) {
            errorCode = 400;
            errorMessage = "Requete invalide";
            errorDetails = "La requete est mal formee ou contient des parametres non valides.";
            displayErrorPage(out, errorCode, errorMessage, errorDetails);
            return;
        }

        if (!urlMaping.containsKey(controllerSearched)) {
            errorCode = 404;
            errorMessage = "Ressource introuvable";
            errorDetails = "Le chemin specifie ne correspond a aucune ressource disponible.";
            displayErrorPage(out, errorCode, errorMessage, errorDetails);
            return;
        }

        try {
            Mapping mapping = urlMaping.get(controllerSearched);
            Class<?> clazz = Class.forName(mapping.getClassName());
            Object object = clazz.getDeclaredConstructor().newInstance();
            Method method = null;

            if (!mapping.isVerbPresent(request.getMethod())) {
                errorCode = 405;
                errorMessage = "Methode non autorisee";
                errorDetails = "La methode HTTP " + request.getMethod() + " n'est pas permise pour cette ressource.";
                displayErrorPage(out, errorCode, errorMessage, errorDetails);
                return;
            }

            for (Method m : clazz.getDeclaredMethods()) {
                for (VerbAction action : mapping.getVerbActions()) {
                    if (m.getName().equals(action.getMethodeName()) && action.getVerb().equalsIgnoreCase(request.getMethod())) {
                        method = m;
                        break;
                    }
                }
                if (method != null) {
                    break;
                }
            }

            if (method == null) {
                errorCode = 404;
                errorMessage = "Methode introuvable";
                errorDetails = "Aucune methode appropriee n'a ete trouvee pour traiter la requete.";
                displayErrorPage(out, errorCode, errorMessage, errorDetails);
                return;
            }

            // ValidationError validationError = validateAndCastParameters(method, request);
        
            // if (validationError.hasErrors()) {
            //     if (method.isAnnotationPresent(ErrorURL.class)) {
            //         for (Map.Entry<String, String> error : validationError.getErrors().entrySet()) {
            //             request.setAttribute(error.getKey(), error.getValue());
            //         }
            //         for (Map.Entry<String, Object> value : validationError.getFormData().entrySet()) {
            //             request.setAttribute(value.getKey(), value.getValue());
            //         }
                    
            //         String previousUrl = (String) request.getSession().getAttribute("previousUrl");
            //         if (previousUrl != null) {
            //             RequestDispatcher dispatcher = request.getRequestDispatcher(previousUrl);
            //             dispatcher.forward(request, response);
            //             return;
            //         }
            //     }
            // }
    
            // 
            Object[] parameters = getMethodParameters(method, request);
            Object returnValue = method.invoke(object, parameters);

            if (method.isAnnotationPresent(RestApi.class)) {
                response.setContentType("application/json");
                Gson gson = new Gson();
                out.println(gson.toJson(returnValue));
            } else {
                if (returnValue instanceof ModelView) {
                    ModelView modelView = (ModelView) returnValue;
                    // modelView.getData().putAll(validationError.getFormData());
                    for (Map.Entry<String, Object> entry : modelView.getData().entrySet()) {
                        request.setAttribute(entry.getKey(), entry.getValue());
                    }
                    RequestDispatcher dispatcher = request.getRequestDispatcher(modelView.getUrl());
                    dispatcher.forward(request, response);
                } else {
                    out.println("La methode a renvoye : " + returnValue);
                }
            }
        } catch (Exception e) {
            errorCode = 500;
            errorMessage = "Erreur interne du serveur";
            errorDetails = "Une erreur inattendue s'est produite lors du traitement de votre requete : " + e.getMessage();
            displayErrorPage(out, errorCode, errorMessage, errorDetails);
        }
    }
    private void displayErrorPage(PrintWriter out, int errorCode, String errorMessage, String errorDetails) {
        out.println("<!DOCTYPE html>");
        out.println("<html lang='fr'>");
        out.println("<head>");
        out.println("<meta charset='UTF-8'>");
        out.println("<title>Erreur " + errorCode + "</title>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; color: #333; background-color: #f4f4f4; }");
        out.println(".container { max-width: 600px; margin: auto; padding: 20px; background-color: #fff; border: 1px solid #ddd; border-radius: 4px; }");
        out.println("h1 { color: #e74c3c; }");
        out.println("p { line-height: 1.5; }");
        out.println("a { color: #3498db; text-decoration: none; }");
        out.println("a:hover { text-decoration: underline; }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<div class='container'>");
        out.println("<h1>" + errorMessage + "</h1>");
        out.println("<p><strong>Code d'erreur :</strong> " + errorCode + "</p>");
        out.println("<p>" + errorDetails + "</p>");
        out.println("</div>");
        out.println("</body>");
        out.println("</html>");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal error occurred");
        }
    }
    private void validateField(Field field, String paramValue) throws Exception {
        if (field.isAnnotationPresent(NotNull.class) && (paramValue == null || paramValue.isEmpty())) {
            throw new Exception("Le champ " + field.getName() + " ne doit pas être nul.");
        }

        if (field.isAnnotationPresent(DoubleType.class)) {
            try {
                Double.parseDouble(paramValue);
            } catch (NumberFormatException e) {
                throw new Exception("Le champ " + field.getName() + " doit être de type double.");
            }
        }

        if (field.isAnnotationPresent(IntType.class)) {
            try {
                Integer.parseInt(paramValue);
            } catch (NumberFormatException e) {
                throw new Exception("Le champ " + field.getName() + " doit être de type int.");
            }
        }

        if (field.isAnnotationPresent(StringType.class)) {
            if (!(paramValue instanceof String)) {
                throw new Exception("Le champ " + field.getName() + " doit être de type String.");
            }
        }
    }

    public static Object convertParameter(String value, Class<?> type) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            if (type == String.class) {
                return value;
            } else if (type == Integer.class || type == int.class) {
                return Integer.parseInt(value.trim());
            } else if (type == Double.class || type == double.class) {
                return Double.parseDouble(value.trim());
            } else if (type == Boolean.class || type == boolean.class) {
                return Boolean.parseBoolean(value.trim());
            } else if (type == Long.class || type == long.class) {
                return Long.parseLong(value.trim());
            } else if (type == Float.class || type == float.class) {
                return Float.parseFloat(value.trim());
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid format for type " + type.getSimpleName());
        }
        return null;
    }
    private void scanControllers(String controllerPackage) throws Exception {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = controllerPackage.replace('.', '/');
            URL resource = classLoader.getResource(path);

            if (resource == null) {
                throw new Exception("Le package specifie n'existe pas: " + controllerPackage);
            }

            Path classPath = Paths.get(resource.toURI());
            Files.walk(classPath)
                    .filter(f -> f.toString().endsWith(".class"))
                    .forEach(f -> {
                        String className = controllerPackage + "." + f.getFileName().toString().replace(".class", "");
                        try {
                            Class<?> clazz = Class.forName(className);
                            if (clazz.isAnnotationPresent(AnnotationController.class)
                                    && !Modifier.isAbstract(clazz.getModifiers())) {
                                controllerNames.add(clazz.getSimpleName());
                                Method[] methods = clazz.getMethods();
                                for (Method method : methods) {
                                    if (method.isAnnotationPresent(Url.class)) {
                                        Url urlAnnotation = method.getAnnotation(Url.class);
                                        String url = urlAnnotation.value();
                                        String verb = "GET"; 
                                        if (method.isAnnotationPresent(GetAnnotation.class)) {
                                            verb = "GET";
                                        } else if (method.isAnnotationPresent(Post.class)) {
                                            verb = "POST";
                                        }

                                        VerbAction verbAction = new VerbAction(method.getName(), verb);
                                        Mapping map = new Mapping(className);
                                        if (urlMaping.containsKey(url)) {
                                            Mapping existingMap = urlMaping.get(url);
                                            if (existingMap.isVerbAction(verbAction)) {
                                                throw new Exception("Duplicate URL: " + url);
                                            } else {
                                                existingMap.setVerbActions(verbAction);
                                            }
                                        } else {
                                            map.setVerbActions(verbAction);
                                            urlMaping.put(url, map);
                                        }
                                        
                                    }else{
                                        throw new Exception("il faut avoir une annotation url dans le controlleur  "+ className);
                                    }
                                }
                                
                                
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
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
                String paramValue = paramMap.get(paramName)[0]; 
                field.setAccessible(true);
                field.set(paramObject, paramValue);
            }
        }
        return paramObject;
    }
    
    private Object[] getMethodParameters(Method method, HttpServletRequest request)throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] parameterValues = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            
            if (parameters[i].isAnnotationPresent(Param.class)) {
                Param param = parameters[i].getAnnotation(Param.class);
                String paramValue = request.getParameter(param.value());
                if(parameters[i].getType().equals(Part.class)){
                    Part filePart = request.getPart(param.value()); 
                    String fileName = filePart.getSubmittedFileName();
                    String filePath = "G:/S4/MrNaina/files_upload" + fileName;
            
                    // Enregistrer le fichier sur le serveur
                    try (InputStream fileContent = filePart.getInputStream();
                         FileOutputStream fos = new FileOutputStream(new File(filePath))) {
                         
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        
                        while ((bytesRead = fileContent.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    parameterValues[i] = filePart;
                }else{
                    parameterValues[i] = convertParameter(paramValue, parameters[i].getType()); // Assuming all parameters are strings for simplicity
                }
            }
            // Verifie si le parametre est annote avec @RequestObject
            else if (parameters[i].isAnnotationPresent(ParamObject.class)) {
                Class<?> parameterType = parameters[i].getType();  // Recupere le type du parametre (le type de l'objet a creer)
                Object parameterObject = parameterType.getDeclaredConstructor().newInstance();  // Cree une nouvelle instance de cet objet
    
                // Parcourt tous les champs (fields) de l'objet
                for (Field field : parameterType.getDeclaredFields()) {
                    ParamField param = field.getAnnotation(ParamField.class);
                    String fieldName = field.getName();  // Recupere le nom du champ
                    if (param == null) {
                        throw new Exception("Etu002501 ,l'attribut " + fieldName +" dans le classe "+parameterObject.getClass().getSimpleName()+" n'a pas d'annotation ParamField "); 
                    }  
                    String paramName = param.value();
                    String paramValue = request.getParameter(paramName);  // Recupere la valeur du parametre de la requete                      
                    
                    // Verifie si la valeur du parametre n'est pas null (si elle est trouvee dans la requete)
                    if (paramValue != null) {
                        validateField(field, paramValue); 
                        Object convertedValue = convertParameter(paramValue, field.getType());  // Convertit la valeur de la requete en type de champ requis
                        
                        // Construit le nom du setter
                        String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                        Method setter = parameterType.getMethod(setterName, field.getType());  // Recupere la methode setter correspondante
                        setter.invoke(parameterObject, convertedValue);  // Appelle le setter pour definir la valeur convertie dans le champ de l'objet
                    }                                                   
                }
                parameterValues[i] = parameterObject;  
            }else if (parameters[i].isAnnotationPresent(InjectSession.class)) {
                parameterValues[i] = new CustomSession(request.getSession());
            }
            else{

            }
        }

        return parameterValues;
    }

    private void injectSessionIfNeeded(Object controllerInstance, HttpSession session) throws IllegalAccessException {
        Field[] fields = controllerInstance.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(InjectSession.class)) {
                boolean accessible = field.isAccessible();
                field.setAccessible(true);
                field.set(controllerInstance, new CustomSession(session));
                field.setAccessible(accessible);
            }
        }
    }

// sprint 14
    // private ValidationError validateAndCastParameters(Method method, HttpServletRequest request) {
    //     ValidationError validationError = new ValidationError();
    //     Parameter[] parameters = method.getParameters();
    
    //     for (Parameter parameter : parameters) {
    //         if (parameter.isAnnotationPresent(ParamObject.class)) {
    //             Class<?> parameterType = parameter.getType();
                
    //             for (Field field : parameterType.getDeclaredFields()) {
    //                 ParamField paramField = field.getAnnotation(ParamField.class);
    //                 if (paramField != null) {
    //                     String paramName = paramField.value();
    //                     String paramValue = request.getParameter(paramName);
                        
    //                     // Store original value for form repopulation
    //                     validationError.addFormValue(paramName, paramValue);
    
    //                     try {
    //                         // Validate required fields
    //                         if (field.isAnnotationPresent(NotNull.class) && 
    //                             (paramValue == null || paramValue.trim().isEmpty())) {
    //                             validationError.addError(paramName, "Ce champ est obligatoire");
    //                             continue;
    //                         }
    
    //                         // Type casting validation
    //                         if (paramValue != null && !paramValue.trim().isEmpty()) {
    //                             if (field.getType() == Integer.class || field.getType() == int.class) {
    //                                 try {
    //                                     Integer.parseInt(paramValue);
    //                                 } catch (NumberFormatException e) {
    //                                     validationError.addError(paramName, "Doit être un nombre entier");
    //                                 }
    //                             } else if (field.getType() == Double.class || field.getType() == double.class) {
    //                                 try {
    //                                     Double.parseDouble(paramValue);
    //                                 } catch (NumberFormatException e) {
    //                                     validationError.addError(paramName, "Doit être un nombre décimal");
    //                                 }
    //                             }
    //                         }
    //                     } catch (Exception e) {
    //                         validationError.addError(paramName, e.getMessage());
    //                     }
    //                 }
    //             }
    //         }
    //     }
    //     return validationError;
    // }
    //14
}
class Mapping {
    
    private String className;
    private List<VerbAction> verbActions;
    
 
    public Mapping(String className) {
        this.className = className;
        this.verbActions =new ArrayList<>();
    }

    public String getClassName() {
        return className;
    }

    public void setVerbActions(VerbAction verbAction){
        this.verbActions.add(verbAction);
    }

    public List<VerbAction> getVerbActions() {
        return verbActions;
    }

    public void setVerbActions(List<VerbAction> verbActions) {
        this.verbActions = verbActions;
    }

    public boolean isVerbPresent(String verbToCheck) {
        for (VerbAction action : this.verbActions) {
            if (action.getVerb().equalsIgnoreCase(verbToCheck)) {
                return true;
            }
        }
        return false;
    }

    public boolean isVerbAction(VerbAction verbToCheck) {
        for (VerbAction action : this.verbActions) {
            if (action.getVerb().equalsIgnoreCase(verbToCheck.getVerb()) && action.getMethodeName().equalsIgnoreCase(verbToCheck.getMethodeName())) {
                return true;
            }
        }
        return false;
    }



}   