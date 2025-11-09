package servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import annotations.AnnotationClass;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.RouteInfo;
import util.ScanUtils;

/**
 * This is the servlet that takes all incoming requests targeting the app - If
 * the requested resource exists, it delegates to the default dispatcher - else
 * it shows the requested URL
 */
public class FrontServlet extends HttpServlet {

    RequestDispatcher defaultDispatcher;

    @Override
    public void init() {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");
        try {
            String basePackage = "Controller";
            Map<String, RouteInfo> routes = ScanUtils.getInstance().scanUrlMappings(basePackage);
            getServletContext().setAttribute("routes", routes);

            System.out.println("Routes chargées :");
            routes.forEach((url, info) -> {
                System.out.println(url + " -> " 
                    + info.clazz.getName() + "." + info.method.getName());
            });
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
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

        Map<String, RouteInfo> routes = (Map<String, RouteInfo>) getServletContext().getAttribute("routes");

        if(routes.containsKey(path)) {
            invokeRoute(path, routes.get(path), req, res);
            return;
        }

        Boolean resourceExists = getServletContext().getAttribute(path) != null;

        if (resourceExists) {
            defaultServe(req, res);
        } else {
            customServe(req, res);
        }
    }

    private void customServe(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try (PrintWriter out = res.getWriter()) {
            String uri = req.getRequestURI();
            String responseBody = """
                <html>
                    <head><title>Resource Not Found</title></head>
                    <body>
                        <h1>Unknown resource</h1>
                        <p>The requested URL was not found: <strong>%s</strong></p>
                    </body>
                </html>
                """.formatted(uri);

            res.setContentType("text/html;charset=UTF-8");
            out.println(responseBody);
        }
    }

    private void defaultServe(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        defaultDispatcher.forward(req, res);
    }

    protected void invokeRoute(String url, RouteInfo info, 
                            HttpServletRequest req, HttpServletResponse res) throws IOException {
        try {
            res.setContentType("text/html;charset=UTF-8");   // obligatoire pour HTML

            Object controller = info.clazz.getDeclaredConstructor().newInstance();
            info.method.invoke(controller);

            res.getWriter().println("""
                <html>
                <head><title>Route Information</title></head>
                <body>
                    <h1>Route exécutée</h1>
                    <p><strong>URL :</strong> %s</p>
                    <p><strong>Classe :</strong> %s</p>
                    <p><strong>Méthode :</strong> %s</p>
                </body>
                </html>
            """.formatted(
                url,
                info.clazz.getName(),
                info.method.getName()
            ));

        } catch (Exception e) {
            res.setContentType("text/html;charset=UTF-8");
            e.printStackTrace();
            res.getWriter().println("<h1>Erreur dans l'appel de la route : " + url + "</h1>");
        }
    }

}
