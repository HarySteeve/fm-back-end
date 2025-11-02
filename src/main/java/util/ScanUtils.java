package util;

import annotations.UrlMapping;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.lang.annotation.Annotation;
import java.net.URL;

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

    
    public Set<Class<?>> getClassesAnnotatedWith(Class<? extends Annotation> annotation, String basePackage) {
        Set<Class<?>> classes = getClassesInPackage(basePackage);
        
        Set<Class<?>> filtered = new HashSet<>();
        for (Class<?> c : classes) {
            if (c.isAnnotationPresent(annotation)) {
                filtered.add(c);
            }
        }

        return filtered;
    }

    private Set<Class<?>> getClassesInPackage(String basePackage) {
        Set<Class<?>> classes = new HashSet<>();
        
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = basePackage.replace(".", "/");
            URL resource = classLoader.getResource(path);
            if (resource != null) {
                File directory = new File(resource.getFile());
                if (directory.exists()) {
                    scanDirectory(directory, basePackage, classes);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return classes;
    }

    private void scanDirectory(File directory, String basePackage, Set<Class<?>> classes) {
        File[] files = directory.listFiles();

        if (files == null) { 
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                String subPackage = basePackage + "." + file.getName();
                scanDirectory(file, subPackage, classes);
            } else if (file.getName().endsWith(".class")) {
                String className = basePackage + "." + file.getName().replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(className);
                    classes.add(clazz);
                } catch (ClassNotFoundException e) {
                    // ignore
                }
            }
        }
    }
}
