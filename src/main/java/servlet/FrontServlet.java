package servlet;

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

        Boolean resourceExists = getServletContext().getAttribute(path) != null;

        if (resourceExists) {
            defaultServe(req, res);
        } else {
            customServe(req, res);
        }
    }

    private void customServe(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String path = req.getRequestURI().substring(req.getContextPath().length());

        ClassMethod c = classMethod.get(path);
        new ResponseHandler(getServletContext()).handleResponse(c, req, res);
    }

    private void defaultServe(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        defaultDispatcher.forward(req, res);
    }
}
