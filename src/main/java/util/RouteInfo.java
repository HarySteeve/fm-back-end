package util;

import java.lang.reflect.Method;

public class RouteInfo {
    public Class<?> clazz;
    public Method method;
    
    public RouteInfo(Class<?> clazz, Method method) {
        this.clazz = clazz;
        this.method = method;
    }
}
