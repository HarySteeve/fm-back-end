package util.http;

import java.util.ArrayList;
import java.util.List;

public final class PropertyPathParser {

    private PropertyPathParser() {}

    public enum TokenType {
        PROPERTY,
        INDEX
    }

    public static class Token {
        public final TokenType type;
        public final String name;
        public final int index;

        private Token(TokenType type, String name, int index) {
            this.type = type;
            this.name = name;
            this.index = index;
        }

        public static Token property(String name) {
            return new Token(TokenType.PROPERTY, name, -1);
        }

        public static Token index(int index) {
            return new Token(TokenType.INDEX, null, index);
        }
    }

    public static List<Token> parse(String path) {
        List<Token> tokens = new ArrayList<Token>();
        int i = 0;
        int n = path.length();

        while (i < n) {
            int start = i;
            while (i < n && Character.isJavaIdentifierPart(path.charAt(i))) {
                i++;
            }
            if (i > start) {
                tokens.add(Token.property(path.substring(start, i)));
            }

            while (i < n && path.charAt(i) == '[') {
                i++;
                int numStart = i;
                while (i < n && Character.isDigit(path.charAt(i))) {
                    i++;
                }
                if (numStart == i) {
                    throw new IllegalArgumentException("Index vide dans: " + path);
                }

                int idx = Integer.parseInt(path.substring(numStart, i));

                if (i >= n || path.charAt(i) != ']') {
                    throw new IllegalArgumentException("Chemin invalide: " + path);
                }
                i++;

                tokens.add(Token.index(idx));
            }

            if (i < n && path.charAt(i) == '.') {
                i++;
            }
        }

        return tokens;
    }
}
