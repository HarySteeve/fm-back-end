package servlet;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FrontServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String fullUrl = request.getRequestURL().toString();

        response.setContentType("text/html;charset=UTF-8");

        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head><title>FrontServlet</title></head>");
        out.println("<body>");
        out.println("<h1>URL saisie</h1>");
        out.println("<p style='color:blue; font-weight:bold;'>" + fullUrl + "</p>");
        out.println("</body>");
        out.println("</html>");
    }
}