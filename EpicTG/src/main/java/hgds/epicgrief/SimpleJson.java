package hgds.epicgrief;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SimpleJson {
    private SimpleJson() {
    }

    static Object parse(String json) {
        Parser parser = new Parser(json);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.isAtEnd()) {
            throw new IllegalArgumentException("Unexpected trailing data at " + parser.position());
        }
        return value;
    }

    static String stringify(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return quote((String) value);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map) {
            StringBuilder builder = new StringBuilder();
            builder.append('{');
            Iterator<?> iterator = ((Map<?, ?>) value).entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) iterator.next();
                builder.append(quote(String.valueOf(entry.getKey())));
                builder.append(':');
                builder.append(stringify(entry.getValue()));
                if (iterator.hasNext()) {
                    builder.append(',');
                }
            }
            builder.append('}');
            return builder.toString();
        }
        if (value instanceof Iterable) {
            StringBuilder builder = new StringBuilder();
            builder.append('[');
            Iterator<?> iterator = ((Iterable<?>) value).iterator();
            while (iterator.hasNext()) {
                builder.append(stringify(iterator.next()));
                if (iterator.hasNext()) {
                    builder.append(',');
                }
            }
            builder.append(']');
            return builder.toString();
        }
        return quote(String.valueOf(value));
    }

    private static String quote(String value) {
        StringBuilder builder = new StringBuilder(value.length() + 2);
        builder.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (character < 0x20) {
                        builder.append(String.format("\\u%04x", (int) character));
                    } else {
                        builder.append(character);
                    }
                    break;
            }
        }
        builder.append('"');
        return builder.toString();
    }

    private static final class Parser {
        private final String json;
        private int index;

        private Parser(String json) {
            this.json = json == null ? "" : json;
        }

        private Object parseValue() {
            skipWhitespace();
            if (isAtEnd()) {
                throw error("Unexpected end of JSON");
            }

            char character = current();
            if (character == '{') {
                return parseObject();
            }
            if (character == '[') {
                return parseArray();
            }
            if (character == '"') {
                return parseString();
            }
            if (character == 't') {
                expectLiteral("true");
                return Boolean.TRUE;
            }
            if (character == 'f') {
                expectLiteral("false");
                return Boolean.FALSE;
            }
            if (character == 'n') {
                expectLiteral("null");
                return null;
            }
            if (character == '-' || Character.isDigit(character)) {
                return parseNumber();
            }

            throw error("Unexpected character '" + character + "'");
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> object = new LinkedHashMap<>();
            index++;
            skipWhitespace();
            if (tryConsume('}')) {
                return object;
            }

            while (true) {
                skipWhitespace();
                if (current() != '"') {
                    throw error("Expected object key");
                }
                String key = parseString();
                skipWhitespace();
                consume(':');
                object.put(key, parseValue());
                skipWhitespace();
                if (tryConsume('}')) {
                    return object;
                }
                consume(',');
            }
        }

        private List<Object> parseArray() {
            List<Object> array = new ArrayList<>();
            index++;
            skipWhitespace();
            if (tryConsume(']')) {
                return array;
            }

            while (true) {
                array.add(parseValue());
                skipWhitespace();
                if (tryConsume(']')) {
                    return array;
                }
                consume(',');
            }
        }

        private String parseString() {
            consume('"');
            StringBuilder builder = new StringBuilder();
            while (!isAtEnd()) {
                char character = json.charAt(index++);
                if (character == '"') {
                    return builder.toString();
                }
                if (character != '\\') {
                    builder.append(character);
                    continue;
                }

                if (isAtEnd()) {
                    throw error("Unexpected end of escape sequence");
                }

                char escaped = json.charAt(index++);
                switch (escaped) {
                    case '"':
                    case '\\':
                    case '/':
                        builder.append(escaped);
                        break;
                    case 'b':
                        builder.append('\b');
                        break;
                    case 'f':
                        builder.append('\f');
                        break;
                    case 'n':
                        builder.append('\n');
                        break;
                    case 'r':
                        builder.append('\r');
                        break;
                    case 't':
                        builder.append('\t');
                        break;
                    case 'u':
                        builder.append(parseUnicode());
                        break;
                    default:
                        throw error("Unknown escape sequence '\\" + escaped + "'");
                }
            }

            throw error("Unclosed string");
        }

        private char parseUnicode() {
            if (index + 4 > json.length()) {
                throw error("Invalid unicode escape");
            }

            String hex = json.substring(index, index + 4);
            index += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException exception) {
                throw error("Invalid unicode escape");
            }
        }

        private Number parseNumber() {
            int start = index;
            if (current() == '-') {
                index++;
            }

            while (!isAtEnd() && Character.isDigit(current())) {
                index++;
            }

            boolean floatingPoint = false;
            if (!isAtEnd() && current() == '.') {
                floatingPoint = true;
                index++;
                while (!isAtEnd() && Character.isDigit(current())) {
                    index++;
                }
            }

            if (!isAtEnd() && (current() == 'e' || current() == 'E')) {
                floatingPoint = true;
                index++;
                if (!isAtEnd() && (current() == '+' || current() == '-')) {
                    index++;
                }
                while (!isAtEnd() && Character.isDigit(current())) {
                    index++;
                }
            }

            String number = json.substring(start, index);
            try {
                return floatingPoint ? Double.parseDouble(number) : Long.parseLong(number);
            } catch (NumberFormatException exception) {
                throw error("Invalid number '" + number + "'");
            }
        }

        private void expectLiteral(String literal) {
            if (!json.startsWith(literal, index)) {
                throw error("Expected '" + literal + "'");
            }
            index += literal.length();
        }

        private void skipWhitespace() {
            while (!isAtEnd() && Character.isWhitespace(current())) {
                index++;
            }
        }

        private void consume(char expected) {
            if (isAtEnd() || current() != expected) {
                throw error("Expected '" + expected + "'");
            }
            index++;
        }

        private boolean tryConsume(char expected) {
            if (!isAtEnd() && current() == expected) {
                index++;
                return true;
            }
            return false;
        }

        private char current() {
            return json.charAt(index);
        }

        private boolean isAtEnd() {
            return index >= json.length();
        }

        private int position() {
            return index;
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at " + index);
        }
    }
}
