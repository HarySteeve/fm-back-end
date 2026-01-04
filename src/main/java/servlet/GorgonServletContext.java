package servlet;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import annotations.AnnotationClass;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import util.http.ClassMethod;
import util.scan.ClassScanner;
import util.scan.MethodScanner;

@WebListener
public class GorgonServletContext implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext servletContext = event.getServletContext();
        servletContext.setAttribute("routes", getClassMethodByUrl());
    }

    /**
     * Gets all the url - CM during init()
     * and inserts it in a map
     */
    private Map<String, List<ClassMethod>> getClassMethodByUrl() {
        Map<String, List<ClassMethod>> map = new HashMap<>();
        Set<Class<?>> classes = ClassScanner.getInstance().getClassesAnnotatedWith(AnnotationClass.class, "controller");

        System.out.println("Valid backend URLs: ");
        for (Class<?> c : classes) {
            Map<String, List<Method>> urlMappingPathMap = MethodScanner.getInstance().getAllUrlMappingPathValues(c);

            for (String url : urlMappingPathMap.keySet()) {
                List<Method> m = urlMappingPathMap.get(url);
                List<ClassMethod> classMethod = new ArrayList<>();
                for (Method method : m) {
                    ClassMethod cm = new ClassMethod(c, method);
                    classMethod.add(cm);
                }
                map.put(url, classMethod);
                for (ClassMethod cm : map.get(url)) {
                    System.out.println("url: " + url + " method: "+ cm.method.getName());
                }
            }
        }
        return map;
    }
}
