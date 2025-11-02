package util;

import annotations.UrlMapping;
import java.lang.reflect.Method;
import java.util.*;

public class ScanUtils {
    private static ScanUtils instance;

    public static ScanUtils getInstance() {
        if (instance == null)
            instance = new ScanUtils();
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
