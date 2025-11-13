package util.http;

import java.lang.reflect.Method;

public class ClassMethod {
    public Class<?> clazz;
    public Method method;
    
    public ClassMethod(Class<?> clazz, Method method) {
        this.clazz = clazz;
        this.method = method;
    }
}
