package util.scan;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public Map<String, List<Method>> getAllUrlMappingPathValues(Class<?> clazz) throws SecurityException {
        try {
            Map<String, List<Method>> result = new HashMap<>();

            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                Annotation[] annotations = method.getAnnotations();
                if (annotations == null || annotations.length == 0) continue;

                for (Annotation ann : annotations) {
                    Class<? extends Annotation> annType = ann.annotationType();

                    // "value()"
                    String[] paths = null;
                    try {
                        Method valueMethod = annType.getMethod("value");
                        Object val = valueMethod.invoke(ann);
                        if (val instanceof String) {
                            paths = new String[] { (String) val };
                        } else if (val instanceof String[]) {
                            paths = (String[]) val;
                        }
                    } catch (NoSuchMethodException ignored) {
                        // annotation n'a pas value()
                    } catch (Exception ignored) {
                        // ignore other reflection exceptions for this annotation
                    }

                    // si on a trouvé un ou plusieurs chemins, on les ajoute
                    if (paths != null && paths.length > 0) {
                        for (String p : paths) {
                            if (p == null || p.isEmpty()) continue;
                            String key = p.startsWith("/") ? p : "/" + p;
                            result.computeIfAbsent(key, k -> new ArrayList<>()).add(method);
                        }
                    }
                }
            }

            return result;
        } catch (SecurityException se) {
            throw se;
        } catch (Exception ex) {
            throw new SecurityException("Error scanning methods: " + ex.getMessage(), ex);
        }
    }
}
