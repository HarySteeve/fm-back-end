package util.http;

public final class TypeConverter {

    private TypeConverter() {}

    public static Object convert(String value, Class<?> targetType) {
        if (value == null) return null;

        if (targetType == String.class) return value;
        if (targetType == Integer.class || targetType == int.class) return Integer.valueOf(value);
        if (targetType == Long.class || targetType == long.class) return Long.valueOf(value);
        if (targetType == Double.class || targetType == double.class) return Double.valueOf(value);
        if (targetType == Float.class || targetType == float.class) return Float.valueOf(value);
        if (targetType == Boolean.class || targetType == boolean.class) return Boolean.valueOf(value);
        if (targetType == Short.class || targetType == short.class) return Short.valueOf(value);
        if (targetType == Byte.class || targetType == byte.class) return Byte.valueOf(value);
        if (targetType == Character.class || targetType == char.class) {
            return value.isEmpty() ? null : value.charAt(0);
        }

        if (targetType.isEnum()) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Object enumValue = Enum.valueOf((Class<? extends Enum>) targetType, value);
            return enumValue;
        }

        return value;
    }
}
