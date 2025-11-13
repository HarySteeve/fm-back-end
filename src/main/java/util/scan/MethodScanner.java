package util.scan;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import annotations.UrlMapping;

public class MethodScanner {
    private static MethodScanner instance;
    
    public static MethodScanner getInstance() {
        if(instance == null) {
            instance = new MethodScanner();
        }
        return instance;
    }

    /*
     * Gets all the "UrlMapping.path()" values
     * from the methods of clazz
     */
    public Map<String, Method> getAllUrlMappingPathValues(Class<?> clazz) throws SecurityException {
        try {
            Map<String, Method> result = new HashMap<>();

            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(UrlMapping.class)) {
                    UrlMapping annotation = method.getAnnotation(UrlMapping.class);
                    result.put(annotation.value(), method);
                }
            }

            return result;
        } catch (SecurityException se) {
            throw se;
        }   
    }
}
