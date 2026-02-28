package util.http;

import java.lang.reflect.Field;
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

                currentObj = fieldValue;
                currentType = fieldType;

            } else { // INDEX
                if (!(currentObj instanceof List<?>)) {
                    throw new IllegalStateException("INDEX sur non-liste: " + path);
                }

                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) currentObj;

                Type elementType = resolveListElementType(currentType);
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

                currentObj = element;
                currentType = elementType;
            }
        }
    }

    private static Object instantiateForNext(Type targetType, Token next) throws Exception {
        if (next.type == TokenType.INDEX) {
            if (!isListType(targetType)) {
                throw new IllegalStateException("Type attendu List mais reçu: " + targetType);
            }
            return new ArrayList<Object>();
        }

        Class<?> raw = rawClass(targetType);
        if (raw == Object.class) {
            throw new IllegalStateException("Type objet non résolu: " + targetType);
        }
        return raw.getDeclaredConstructor().newInstance();
    }

    private static boolean isListType(Type type) {
        return List.class.isAssignableFrom(rawClass(type));
    }

    private static Type resolveListElementType(Type listType) {
        if (listType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) listType;
            Class<?> raw = rawClass(pt.getRawType());
            if (List.class.isAssignableFrom(raw)) {
                return pt.getActualTypeArguments()[0];
            }
        }
        if (listType instanceof Class<?>) {
            Class<?> raw = (Class<?>) listType;
            if (List.class.isAssignableFrom(raw)) {
                return Object.class;
            }
        }
        throw new IllegalStateException("Pas un type List: " + listType);
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
