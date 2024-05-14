package mg.itu.prom16;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class FrontController extends HttpServlet {
    private boolean checked = false;
    private List<String> listeControllers = new ArrayList<>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        processRequest(request, response);
    }

    private synchronized void processRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head>");
        out.println("<title>FrontController</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>URL actuelle :</h1>");
        out.println("<p>" + request.getRequestURL() + "</p>");

        if (!checked) {
            scanControllers();
            checked = true;
        }

        out.println("<h2>Liste des contrôleurs :</h2>");
        for (String controller : listeControllers) {
            out.println("<p>" + controller + "</p>");
        }

        out.println("</body>");
        out.println("</html>");
        out.close();
    }
    private void scanControllers() {
        ServletConfig config = getServletConfig();
        String controllerPackage = config.getInitParameter("controller-package");

        List<Class<?>> classes = ClassFinder.find(controllerPackage);

        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(WebServlet.class)) {
                // Si l'annotation WebServlet est présente, ajoutez le nom de la classe à la liste
                listeControllers.add(clazz.getName());
            }
         }
    }
}
