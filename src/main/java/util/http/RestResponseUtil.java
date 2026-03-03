package util.http;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RestResponseUtil {

    private RestResponseUtil() {}

    public static void writeSuccess(HttpServletResponse res, Object payload) throws IOException {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("status", "success");
        envelope.put("code", 200);
        envelope.put("data", wrapCountIfListOrArray(payload));
        envelope.put("error", null);
        writeJson(res, 200, envelope);
    }

    public static void writeError(HttpServletResponse res, int code, String message) throws IOException {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("status", "error");
        envelope.put("code", code);
        envelope.put("data", null);

        Map<String, Object> err = new LinkedHashMap<>();
        err.put("message", message);
        envelope.put("error", err);

        writeJson(res, code, envelope);
    }

    public static Object unwrapModelAndView(Object result) {
        if (result instanceof ModelAndView mv) {
            return mv.getData();
        }
        return result;
    }

    private static Object wrapCountIfListOrArray(Object payload) {
        if (payload == null) return null;

        if (payload instanceof List<?> list) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("count", list.size());
            data.put("items", list);
            return data;
        }

        Class<?> c = payload.getClass();
        if (c.isArray()) {
            int len = Array.getLength(payload);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("count", len);
            data.put("items", payload);
            return data;
        }

        return payload;
    }

    private static void writeJson(HttpServletResponse res, int code, Object body) throws IOException {
        res.setStatus(code);
        res.setCharacterEncoding("UTF-8");
        res.setContentType("application/json; charset=UTF-8");
        res.getWriter().write(toJson(body));
    }

    // -------------------- Minimal JSON serializer --------------------

    private static String toJson(Object value) {
        return toJson(value, new IdentityHashMap<>());
    }

    private static String toJson(Object value, IdentityHashMap<Object, Boolean> seen) {
        if (value == null) return "null";

        if (value instanceof String s) return quote(s);
        if (value instanceof Character c) return quote(String.valueOf(c));
        if (value instanceof Number || value instanceof Boolean) return String.valueOf(value);

        if (value.getClass().isEnum()) return quote(String.valueOf(value));

        if (seen.containsKey(value)) {
            return quote("[Circular]");
        }
        seen.put(value, Boolean.TRUE);

        if (value instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append(quote(String.valueOf(e.getKey())));
                sb.append(':');
                sb.append(toJson(e.getValue(), seen));
            }
            sb.append('}');
            return sb.toString();
        }

        if (value instanceof Iterable<?> it) {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            boolean first = true;
            for (Object o : it) {
                if (!first) sb.append(',');
                first = false;
                sb.append(toJson(o, seen));
            }
            sb.append(']');
            return sb.toString();
        }

        Class<?> cls = value.getClass();
        if (cls.isArray()) {
            int len = Array.getLength(value);
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            for (int i = 0; i < len; i++) {
                if (i > 0) sb.append(',');
                sb.append(toJson(Array.get(value, i), seen));
            }
            sb.append(']');
            return sb.toString();
        }

        // object -> fields
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Field f : getAllFields(cls)) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            if (Modifier.isTransient(f.getModifiers())) continue;

            f.setAccessible(true);
            Object fieldValue;
            try {
                fieldValue = f.get(value);
            } catch (IllegalAccessException ex) {
                continue;
            }

            if (!first) sb.append(',');
            first = false;
            sb.append(quote(f.getName()));
            sb.append(':');
            sb.append(toJson(fieldValue, seen));
        }
        sb.append('}');
        return sb.toString();
    }

    private static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> t = type;
        while (t != null && t != Object.class) {
            fields.addAll(Arrays.asList(t.getDeclaredFields()));
            t = t.getSuperclass();
        }
        return fields;
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
