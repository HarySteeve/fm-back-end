package util.http;

import java.lang.reflect.Method;
import java.lang.annotation.Annotation;

public class ClassMethod {
    public Class<?> clazz;
    public Method method;

    public ClassMethod() {}
    
    public ClassMethod(Class<?> clazz, Method method) {
        this.clazz = clazz;
        this.method = method;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public boolean annotationMatchesHttpMethod(Annotation[] anns, String httpMethod) {
        if (httpMethod == null) return false;

        String hm = httpMethod.trim();
        for (Annotation ann : anns) {
            Class<? extends Annotation> annType = ann.annotationType();
            String simpleName = annType.getSimpleName();

            // 1) UrlMapping (compatibilité) : si l'annotation s'appelle UrlMapping,
            //    on tente de lire un attribut 'method'/'methods' ; si pas défini, on accepte.
            if ("UrlMapping".equalsIgnoreCase(simpleName)) {
                try {
                    Method attr = annType.getMethod("method");
                    Object val = attr.invoke(ann);
                    if (val != null && !val.toString().isEmpty()) {
                        if (val.toString().equalsIgnoreCase(hm)) return true;
                        else continue;
                    } else {
                        return true; // UrlMapping sans method -> accepte tous les verbs
                    }
                } catch (NoSuchMethodException ignored) {
                    // pas d'attribut 'method' -> accepte
                    return true;
                } catch (Exception ignored) {}
            }

            // 2) comparaison directe avec le nom de l'annotation (ex: "Get" vs "GET")
            if (simpleName.equalsIgnoreCase(hm)) return true;

            // 3) comparer avec des suffixes usuels (ex: "GetMapping" vs "GET")
            if (simpleName.equalsIgnoreCase(hm + "Mapping")) return true;
            if (simpleName.equalsIgnoreCase(hm + "Request")) return true;

            // 4) vérifier si l'annotation a un attribut 'method' et comparer la valeur
            try {
                Method attr = annType.getMethod("method"); // ex: method()
                Object val = attr.invoke(ann);
                if (val != null) {
                    if (val.getClass().isArray()) {
                        for (Object o : (Object[]) val) {
                            if (o != null && o.toString().equalsIgnoreCase(hm)) return true;
                        }
                    } else {
                        if (val.toString().equalsIgnoreCase(hm)) return true;
                    }
                }
            } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException ignored) {
            }

            // 5) vérifier un attribut 'methods' (tableau) si présent
            try {
                Method attr2 = annType.getMethod("methods"); // ex: String[] methods() or enum[]
                Object val2 = attr2.invoke(ann);
                if (val2 instanceof String[]) {
                    for (String s : (String[]) val2) if (s.equalsIgnoreCase(hm)) return true;
                } else if (val2 instanceof Object[]) {
                    for (Object o : (Object[]) val2) {
                        if (o != null && o.toString().equalsIgnoreCase(hm)) return true;
                    }
                }
            } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException ignored) {
            }
        }
        return false;
}
}
