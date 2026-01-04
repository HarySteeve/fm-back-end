package util.http;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.ConvertUtils;

import annotations.Param;
import annotations.PathVariable;
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
    public void handleResponse(List<ClassMethod> cm, HttpServletRequest req,HttpServletResponse res) throws IOException {
        Boolean cmExist = cm != null;
        System.out.println("CM-EXISTTTT------ " + cmExist);

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

    private void invokeControllerMethod(List<ClassMethod> cms, HttpServletRequest req, HttpServletResponse res) {
        try {
            Method m = null;
            Class<?> c = null;
            System.out.println("HTTP METHOD: " + req.getMethod());
            for(ClassMethod cm : cms) {
                Method method = cm.getMethod();
                Annotation[] anns = method.getAnnotations();
                for(Annotation ann : anns) {
                    System.out.println("Annotation present: " + ann.annotationType().getSimpleName());
                }

                if(cm.annotationMatchesHttpMethod(anns, req.getMethod())) {
                    c = cm.getClazz();
                    m = method;
                    break;
                }
            }

            if (m == null) {
                // aucune méthode correspondante pour ce verb -> 405
                res.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                responseBody = "Method not allowed";
                return;
            }
            
            m.setAccessible(true);

            Object[] args = getMatchedParams(m, req);
            Class<?> returnType = m.getReturnType();
            Object objectController = c.getDeclaredConstructor().newInstance();
            if(returnType.equals(String.class)) {
                res.setContentType("text/plain");
                responseBody = m.invoke(objectController, args).toString();
            } else if(returnType.equals(ModelAndView.class)){
                ModelAndView mv = (ModelAndView)m.invoke(objectController, args);
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

    private Object[] getMatchedParams(Method method, HttpServletRequest req) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        Map<String, String[]> paramsViaVue = req.getParameterMap();
        paramsViaVue.forEach((key, value) ->
            System.out.println(key + " => " + Arrays.toString(value))
        );

        Map<String, String> pathVariables = Map.of();
        String pathTemplate = (String) req.getAttribute("matchedRoute");
        if(pathTemplate != null) {
            String actualPath = req.getRequestURI().substring(req.getContextPath().length());
            pathVariables = extractPathVariables(pathTemplate, actualPath);
            pathVariables.forEach((key, value) ->
                System.out.println("PATH VARIABLE: " + key + " => " + value)
            );
        }
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter p = parameters[i];
            // @pathVarialble
            PathVariable pathVariable = p.getAnnotation(PathVariable.class);
            if(pathVariable != null) {
                String varName = pathVariable.value().isEmpty() ? p.getName() : pathVariable.value();
                if(pathVariables.containsKey(varName)) {
                    String value = pathVariables.get(varName);
                    Class<?> typeArg = p.getType();
                    Object convertedValue = ConvertUtils.convert(value, typeArg);
                    args[i] = convertedValue;
                } else {
                    args[i] = null;
                }
                continue;
            }
            // Map<String, Object[]>
            Type type = p.getParameterizedType();
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;

                if (pt.getRawType().equals(Map.class)) {
                    Type[] typeArgs = pt.getActualTypeArguments();

                    if (typeArgs.length == 2
                            && typeArgs[0].equals(String.class)
                            && typeArgs[1].equals(Object[].class)) {

                        Map<String, Object[]> paramMapObject = new HashMap<>();

                        req.getParameterMap().forEach((key, values) -> {
                            paramMapObject.put(key, values);
                        });

                        args[i] = paramMapObject;
                        continue;
                    }
                }
            }
            
            // @param
            String paramName;
            Param annotation = p.getAnnotation(Param.class);
            if(annotation != null) {
                paramName = annotation.value();
            } else {
                paramName = p.getName();
            }
            System.out.println("PARAMNAME---- "+paramName);

            if(paramsViaVue.containsKey(paramName)) {
                String value = paramsViaVue.get(paramName)[0];
                Class<?> typeArg = p.getType();

                Object convertedValue = ConvertUtils.convert(value, typeArg);
                args[i] = convertedValue;
            } else {
                args[i] = null;
            }
        }
        return args;
    }

    private Map<String, String> extractPathVariables(String template, String path) {
        String[] tSeg = template.split("/");
        String[] pSeg = path.split("/");

        if(tSeg.length != pSeg.length) return Map.of();

        java.util.Map<String, String> map = new java.util.HashMap<>();
        for (int i = 0; i < tSeg.length; i++) {
            String ts = tSeg[i];
            String ps = pSeg[i];
            if(ts.startsWith("{") && ts.endsWith("}")) {
                String name = ts.substring(1, ts.length()-1);
                map.put(name, ps);
            } else if(!ts.equals(ps)) {
                // mismatch, return empty
                return Map.of();
            }
        }
        return map;
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
