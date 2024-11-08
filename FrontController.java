package mg.itu.prom16;
import com.google.gson.Gson;

import mg.itu.prom16.AnnotationController;
import mg.itu.prom16.GetAnnotation;
import mg.itu.prom16.Post;
import mg.itu.prom16.Param;
import mg.itu.prom16.VerbAction;

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
import jakarta.servlet.RequestDispatcher; 
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
        throws Exception {
        StringBuffer requestURL = request.getRequestURL();
        String[] requestUrlSplitted = requestURL.toString().split("/");
        String controllerSearched = requestUrlSplitted[requestUrlSplitted.length - 1];

        PrintWriter out = response.getWriter();
        int errorCode = 0;
        String errorMessage = null;
        String errorDetails = null;

        response.setContentType("text/html");

        // Erreur de requete invalide
        if (!error.isEmpty()) {
            errorCode = 400;
            errorMessage = "Requete invalide";
            errorDetails = "La requete est mal formee ou contient des parametres non valides.";
            displayErrorPage(out, errorCode, errorMessage, errorDetails);
            return;
        }

        // Contrôleur non trouve
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

            // Methode HTTP non autorisee
            if (!mapping.isVerbPresent(request.getMethod())) {
                errorCode = 405;
                errorMessage = "Methode non autorisee";
                errorDetails = "La methode HTTP " + request.getMethod() + " n'est pas permise pour cette ressource.";
                displayErrorPage(out, errorCode, errorMessage, errorDetails);
                return;
            }

            // Verification de l'existence de la methode correspondante
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

            // Execution de la methode trouvee
            Object[] parameters = getMethodParameters(method, request);
            Object returnValue = method.invoke(object, parameters);

            // Gerer la reponse selon le type de retour de la methode
            if (method.isAnnotationPresent(RestApi.class)) {
                response.setContentType("application/json");
                Gson gson = new Gson();
                out.println(gson.toJson(returnValue));
            } else {
                if (returnValue instanceof ModelView) {
                    ModelView modelView = (ModelView) returnValue;
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
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getContentType() != null && request.getContentType().startsWith("multipart/form-data")) {
            handleFileUpload(request, response);
        } else {
            processRequest(request, response);
        }
    }
    public static Object convertParameter(String value, Class<?> type) {
        if (value == null) {
            return null;
        }
        if (type == String.class) {
            return value;
        } else if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        // Ajoutez d'autres conversions necessaires ici
        return null;
    }

    private void scanControllers(String packageName) throws Exception {
        try {
            // Charger le package et parcourir les classes
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            URL resource = classLoader.getResource(path);

            // Verification si le package n'existe pas
            if (resource == null) {
                throw new Exception("Le package specifie n'existe pas: " + packageName);
            }

            Path classPath = Paths.get(resource.toURI());
            Files.walk(classPath)
                    .filter(f -> f.toString().endsWith(".class"))
                    .forEach(f -> {
                        String className = packageName + "." + f.getFileName().toString().replace(".class", "");
                        try {
                            Class<?> clazz = Class.forName(className);
                            if (clazz.isAnnotationPresent(AnnotationControlleur.class)
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
                    String filePath = "G:/S4/MrNaina/work/files_Upload" + fileName;
            
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
                        throw new Exception("Etu002635 ,l'attribut " + fieldName +" dans le classe "+parameterObject.getClass().getSimpleName()+" n'a pas d'annotation ParamField "); 
                    }  
                    String paramName = param.value();
                    String paramValue = request.getParameter(paramName);  // Recupere la valeur du parametre de la requete

                    // Verifie si la valeur du parametre n'est pas null (si elle est trouvee dans la requete)
                    if (paramValue != null) {
                        Object convertedValue = convertParameter(paramValue, field.getType());  // Convertit la valeur de la requete en type de champ requis

                        // Construit le nom du setter
                        String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                        Method setter = parameterType.getMethod(setterName, field.getType());  // Recupere la methode setter correspondante
                        setter.invoke(parameterObject, convertedValue);  // Appelle le setter pour definir la valeur convertie dans le champ de l'objet
                    }
                }
                parameterValues[i] = parameterObject;  // Stocke l'objet cree dans le tableau des arguments
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