package util.http;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ResponseHandler {
    private String responseBody;
    private final ServletContext context;

    public ResponseHandler(ServletContext context) {
        this.context = context;
    }

    // Fabrication jerijereo
    public void handleResponse(ClassMethod cm, HttpServletRequest req,HttpServletResponse res) throws IOException {
        Boolean cmExist = cm != null;
        System.out.println(cmExist);

        if(cmExist) {
            invokeControllerMethod(cm, req, res);
        } else {
            handle404(res);
        }

        if(responseBody != null) {
            try {
                PrintWriter out = res.getWriter();
                out.println(responseBody);
            } catch (IOException e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }
    }

    private void invokeControllerMethod(ClassMethod cm, HttpServletRequest req, HttpServletResponse res) {
        try {
            Class<?> c = cm.clazz;
            Method m = cm.method;
            m.setAccessible(true);

            Class<?> returnType = m.getReturnType();
            Object objectController = c.getDeclaredConstructor().newInstance();
            if(returnType.equals(String.class)) {
                res.setContentType("text/plain");
                responseBody = m.invoke(objectController).toString();
            } else if(returnType.equals(ModelAndView.class)){
                ModelAndView mv = (ModelAndView)m.invoke(objectController);
                Map<String, Object> data = mv.getData();
                if(data != null) {
                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        req.setAttribute(entry.getKey(), entry.getValue());
                    }
                }
                String view = mv.getView();
                RequestDispatcher requestDispatcher = context.getRequestDispatcher(view);
                requestDispatcher.forward(req, res);
            } else {
                m.invoke(objectController);
            }
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalArgumentException | InvocationTargetException | IllegalAccessException ex) {
            // TODO: handle exception
            handleError(res, "Error invoking controller method: "+ ex.getMessage());
        } catch (ServletException | IOException ex) { // From requestDispatcher.forward()
            handleError(res, "Error forwarding to view: " + ex.getMessage());
        }
    }

    private void handleError(HttpServletResponse res, String error) {
        res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        responseBody = formattedHtmlResponseBody("Error", "<h1>"+ error +"</h1>");
        res.setContentType("text/html;charset=UTF-8");
    }

    private void handle404(HttpServletResponse res) {
        String htmlBody = "<h1>404 not found</h1>";
        responseBody = formattedHtmlResponseBody("Method not found", htmlBody);
        res.setContentType("text/html;charset=UTF-8");
    }

    private String formattedHtmlResponseBody(String title, String body) {
        return """
            <html>
                <head><title>%s</title></head>
                <body>
                    %s
                </body>
            </html>""".formatted(title, body);
    }
}
