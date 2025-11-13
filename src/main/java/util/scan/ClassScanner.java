package util.scan;

import annotations.UrlMapping;
import util.http.ClassMethod;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.lang.annotation.Annotation;
import java.net.URL;

public class ClassScanner {
    private static ClassScanner instance;

    public static ClassScanner getInstance() {
        if (instance == null)
            instance = new ClassScanner();
        return instance;
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
            System.out.println("Scanning path = " + path);
            System.out.println("Resource = " + resource);
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
