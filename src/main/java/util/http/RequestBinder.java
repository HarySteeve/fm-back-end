package util.http;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import util.http.PropertyPathParser.Token;
import util.http.PropertyPathParser.TokenType;

public final class RequestBinder {

    private RequestBinder() {}

    @FunctionalInterface
    private interface Setter {
        void set(Object newValue) throws Exception;
    }

    public static <T> T bind(HttpServletRequest request, Class<T> rootType) throws Exception {
        T root = rootType.getDeclaredConstructor().newInstance();
        Map<String, String[]> params = request.getParameterMap();

        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();
            if (values == null || values.length == 0) continue;

            bindPath(root, rootType, key, values[0]);
        }

        return root;
    }

    private static void bindPath(Object root, Type rootType, String path, String value) throws Exception {
        List<Token> tokens = PropertyPathParser.parse(path);
        if (tokens.isEmpty()) return;

        Object currentObj = root;
        Type currentType = rootType;

        // Used to replace the current container in its parent (needed for array resize)
        Setter currentSetter = null;

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            boolean last = (i == tokens.size() - 1);

            if (token.type == TokenType.PROPERTY) {
                Class<?> currentClass = rawClass(currentType);
                Field field = findField(currentClass, token.name);
                if (field == null) return;
                field.setAccessible(true);

                Type fieldType = field.getGenericType();

                if (last) {
                    Object converted = TypeConverter.convert(value, rawClass(fieldType));
                    field.set(currentObj, converted);
                    return;
                }

                Token next = tokens.get(i + 1);
                Object fieldValue = field.get(currentObj);

                if (fieldValue == null) {
                    fieldValue = instantiateForNext(fieldType, next);
                    field.set(currentObj, fieldValue);
                }

                Object parent = currentObj;
                currentSetter = (newValue) -> field.set(parent, newValue);

                currentObj = fieldValue;
                currentType = fieldType;

            } else { // INDEX
                if (currentObj instanceof List<?>) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) currentObj;

                    Type elementType = resolveIndexedElementType(currentType);
                    ensureSize(list, token.index + 1);

                    if (last) {
                        Object converted = TypeConverter.convert(value, rawClass(elementType));
                        list.set(token.index, converted);
                        return;
                    }

                    Token next = tokens.get(i + 1);
                    Object element = list.get(token.index);

                    if (element == null) {
                        element = instantiateForNext(elementType, next);
                        list.set(token.index, element);
                    }

                    int idx = token.index;
                    currentSetter = (newValue) -> list.set(idx, newValue);

                    currentObj = element;
                    currentType = elementType;
                    continue;
                }

                if (currentObj != null && currentObj.getClass().isArray()) {
                    Class<?> arrayClass = currentObj.getClass();
                    if (arrayClass.getComponentType().isArray()) {
                        throw new IllegalStateException("Tableau multi-dimensionnel non supporté: " + arrayClass.getTypeName());
                    }

                    if (!last) {
                        Token next = tokens.get(i + 1);
                        if (next.type == TokenType.INDEX) {
                            throw new IllegalStateException("INDEX imbriqué sur tableau non supporté: " + path);
                        }
                    }

                    Object array = currentObj;
                    Type elementType = resolveIndexedElementType(currentType);

                    int len = Array.getLength(array);
                    if (token.index >= len) {
                        if (currentSetter == null) {
                            throw new IllegalStateException("Resize tableau impossible (pas de parent setter) pour: " + path);
                        }
                        Object resized = resizeArray1D(array, token.index + 1);
                        currentSetter.set(resized);
                        array = resized;
                        currentObj = array;
                    }

                    if (last) {
                        Object converted = TypeConverter.convert(value, rawClass(elementType));
                        Array.set(array, token.index, converted);
                        return;
                    }

                    Token next = tokens.get(i + 1);
                    Object element = Array.get(array, token.index);

                    if (element == null) {
                        element = instantiateForNext(elementType, next);
                        Array.set(array, token.index, element);
                    }

                    Object capturedArray = array;
                    int idx = token.index;
                    currentSetter = (newValue) -> Array.set(capturedArray, idx, newValue);

                    currentObj = element;
                    currentType = elementType;
                    continue;
                }

                throw new IllegalStateException("INDEX sur non-liste et non-tableau: " + path);
            }
        }
    }

    private static Object instantiateForNext(Type targetType, Token next) throws Exception {
        if (next.type == TokenType.INDEX) {
            if (isListType(targetType)) {
                Class<?> raw = rawClass(targetType);
                if (raw == Object.class || raw.isInterface() || Modifier.isAbstract(raw.getModifiers())) {
                    return new ArrayList<Object>();
                }
                if (List.class.isAssignableFrom(raw)) {
                    return raw.getDeclaredConstructor().newInstance();
                }
                return new ArrayList<Object>();
            }

            Class<?> raw = rawClass(targetType);
            if (raw.isArray()) {
                if (raw.getComponentType().isArray()) {
                    throw new IllegalStateException("Création tableau multi-dimensionnel non supportée: " + raw.getTypeName());
                }
                return Array.newInstance(raw.getComponentType(), next.index + 1);
            }

            throw new IllegalStateException("Type attendu List ou tableau 1D mais reçu: " + targetType);
        }

        Class<?> raw = rawClass(targetType);
        if (raw == Object.class) {
            throw new IllegalStateException("Type objet non résolu: " + targetType);
        }
        return raw.getDeclaredConstructor().newInstance();
    }

    private static boolean  isListType(Type type) {
        return List.class.isAssignableFrom(rawClass(type));
    }

    private static Type resolveIndexedElementType(Type containerType) {
        if (containerType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) containerType;
            Class<?> raw = rawClass(pt.getRawType());
            if (List.class.isAssignableFrom(raw)) {
                return pt.getActualTypeArguments()[0];
            }
        }

        if (containerType instanceof Class<?>) {
            Class<?> raw = (Class<?>) containerType;
            if (raw.isArray()) {
                if (raw.getComponentType().isArray()) {
                    throw new IllegalStateException("Tableau multi-dimensionnel non supporté: " + raw.getTypeName());
                }
                return raw.getComponentType();
            }
            if (List.class.isAssignableFrom(raw)) {
                return Object.class;
            }
        }

        throw new IllegalStateException("Pas un type indexable (List ou tableau 1D): " + containerType);
    }

    private static Class<?> rawClass(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            Type raw = pt.getRawType();
            if (raw instanceof Class<?>) {
                return (Class<?>) raw;
            }
        }
        return Object.class;
    }

    private static void ensureSize(List<Object> list, int size) {
        while (list.size() < size) {
            list.add(null);
        }
    }

    private static Object resizeArray1D(Object array, int newLen) {
        Class<?> arrayClass = array.getClass();
        if (!arrayClass.isArray() || arrayClass.getComponentType().isArray()) {
            throw new IllegalStateException("resizeArray1D appelé sur non-tableau 1D: " + arrayClass.getTypeName());
        }

        int oldLen = Array.getLength(array);
        Object resized = Array.newInstance(arrayClass.getComponentType(), newLen);
        System.arraycopy(array, 0, resized, 0, oldLen);
        return resized;
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> t = type;
        while (t != null) {
            try {
                return t.getDeclaredField(name);
            } catch (NoSuchFieldException ex) {
                t = t.getSuperclass();
            }
        }
        return null;
    }
}
