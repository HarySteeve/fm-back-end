package servlet;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.http.ClassMethod;
import util.http.ResponseHandler;

/**
 * This is the servlet that takes all incoming requests targeting the app - If
 * the requested resource exists, it delegates to the default dispatcher - else
 * it shows the requested URL
 */
public class FrontServlet extends HttpServlet {

    RequestDispatcher defaultDispatcher;
    Map<String, ClassMethod> classMethod;

    @Override
    public void init() {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");

        classMethod = (Map<String, ClassMethod>) getServletContext().getAttribute("routes");
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        /**
         * Example: 
         * If URI is /app/folder/file.html 
         * and context path is /app,
         * then path = /folder/file.html
         */
        String path = req.getRequestURI().substring(req.getContextPath().length());

        String realPath = getServletContext().getRealPath(path);
        boolean resourceExists = realPath != null && new File(realPath).exists();

        if (resourceExists) {
            defaultServe(req, res);
        } else {
            customServe(req, res);
        }
    }

    private void customServe(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String pathUrl = req.getRequestURI().substring(req.getContextPath().length());

        ClassMethod c = classMethod.get(pathUrl);
        if(c != null) {
            new ResponseHandler(getServletContext()).handleResponse(c, req, res);
        } else {
            verifyingUrl(pathUrl, req, res);
        }
    }

    private void defaultServe(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        defaultDispatcher.forward(req, res);
    }

    // Fonction qui verifie s'il existe un accolade dans les url d'une classe
    private void verifyingUrl(String pathUrl, HttpServletRequest req, HttpServletResponse res) throws IOException {
        for (Map.Entry<String, ClassMethod> entry : classMethod.entrySet()) {
            String pathInController = entry.getKey();
            if(pathInController.contains("{")) {
                // On construit un regex
                String regex = pathInController
                    .replace("{", "(?<")
                    .replace("}", ">[^/]+)")   // capture group
                    .replace("/", "\\/");

                regex = "^" + regex + "$";
                System.out.println(regex);

                if(pathUrl.matches(regex)) {
                    ClassMethod cm = classMethod.get(pathInController);
                    new ResponseHandler(getServletContext()).handleResponse(cm, req, res);
                    return;
                }
            }
        }
        new ResponseHandler(getServletContext()).handleResponse(null, req, res);
    }
}
