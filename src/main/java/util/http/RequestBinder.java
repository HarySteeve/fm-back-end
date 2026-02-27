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

    private static void bindPath(Object root, Class<?> rootType, String path, String value) throws Exception {
        List<Token> tokens = PropertyPathParser.parse(path);
        if (tokens.isEmpty()) return;

        Object currentObj = root;
        Class<?> currentClass = rootType;
        Class<?> listElementType = null;

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            boolean last = (i == tokens.size() - 1);

            if (token.type == TokenType.PROPERTY) {
                Field field = findField(currentClass, token.name);
                if (field == null) return;
                field.setAccessible(true);

                if (last) {
                    Object converted = TypeConverter.convert(value, field.getType());
                    field.set(currentObj, converted);
                    return;
                }

                Token next = tokens.get(i + 1);
                Object fieldValue = field.get(currentObj);

                if (next.type == TokenType.INDEX) {
                    if (fieldValue == null) {
                        fieldValue = new ArrayList<Object>();
                        field.set(currentObj, fieldValue);
                    }
                    listElementType = resolveListElementType(field);
                    currentObj = fieldValue;
                    currentClass = field.getType();
                } else {
                    if (fieldValue == null) {
                        fieldValue = field.getType().getDeclaredConstructor().newInstance();
                        field.set(currentObj, fieldValue);
                    }
                    currentObj = fieldValue;
                    currentClass = field.getType();
                }
            } else {
                if (!(currentObj instanceof List<?>)) {
                    throw new IllegalStateException("INDEX sur non-liste: " + path);
                }

                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) currentObj;
                ensureSize(list, token.index + 1);

                if (last) {
                    Class<?> targetType = (listElementType != null ? listElementType : String.class);
                    Object converted = TypeConverter.convert(value, targetType);
                    list.set(token.index, converted);
                    return;
                }

                Object element = list.get(token.index);
                if (element == null) {
                    if (listElementType == null || listElementType == Object.class) {
                        throw new IllegalStateException("Type element de liste introuvable pour: " + path);
                    }
                    element = listElementType.getDeclaredConstructor().newInstance();
                    list.set(token.index, element);
                }

                currentObj = element;
                currentClass = element.getClass();
            }
        }
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

    private static Class<?> resolveListElementType(Field field) {
        Type gt = field.getGenericType();
        if (gt instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) gt;
            Type arg = pt.getActualTypeArguments()[0];
            if (arg instanceof Class<?>) {
                return (Class<?>) arg;
            }
        }
        return Object.class;
    }
}
