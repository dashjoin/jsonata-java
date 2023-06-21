package com.dashjoin.jsonata;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokenizer { // = function (path) {

static HashMap<String, Integer> operators = new HashMap<String, Integer>() {{
    put(".", 75);
    put("[", 80);
    put("]", 0);
    put("{", 70);
    put("}", 0);
    put("(", 80);
    put(")", 0);
    put(",", 0);
    put("@", 80);
    put("#", 80);
    put(";", 80);
    put(":", 80);
    put("?", 20);
    put("+", 50);
    put("-", 50);
    put("*", 60);
    put("/", 60);
    put("%", 60);
    put("|", 20);
    put("=", 40);
    put("<", 40);
    put(">", 40);
    put("^", 40);
    put("**", 60);
    put("..", 20);
    put(":=", 10);
    put("!=", 40);
    put("<=", 40);
    put(">=", 40);
    put("~>", 40);
    put("and", 30);
    put("or", 25);
    put("in", 40);
    put("&", 50);
    put("!", 0);
    put("~", 0);
}};

static HashMap<String, String> escapes = new HashMap<String, String>() {{
    // JSON string escape sequences - see json.org
    put("\"", "\"");
    put("\\", "\\");
    put("/", "/");
    put("b", "\b");
    put("f", "\f");
    put("n", "\n");
    put("r", "\r");
    put("t", "\t");
}};

// Tokenizer (lexer) - invoked by the parser to return one token at a time
    String path;
    int position = 0;
    int length; // = path.length;

    Tokenizer(String path) {
        this.path = path;
        length = path.length();
    }

    public static class Token {
        String type;
        Object value;
        int position;
        //
        String id;
    }

//        var create = function (type, value) {
//            var obj = {type: type, value: value, position: position};
//            return obj;
//        };
    Token create(String type, Object value) {
        Token t = new Token();
        t.type = type; t.value = value; t.position = position;
        return t;
    }

    boolean isClosingSlash(int position) {
        if (path.charAt(position) == '/' && depth == 0) {
            int backslashCount = 0;
            while (path.charAt(position - (backslashCount + 1)) == '\\') {
                backslashCount++;
            }
            if (backslashCount % 2 == 0) {
                return true;
            }
        }
        return false;
    }

    int depth;

    Pattern scanRegex() throws JException {
        // the prefix '/' will have been previously scanned. Find the end of the regex.
        // search for closing '/' ignoring any that are escaped, or within brackets
        int start = position;
        //int depth = 0;
        String pattern;
        String flags;

        while (position < length) {
            char currentChar = path.charAt(position);
            if (isClosingSlash(position)) {
                // end of regex found
                pattern = path.substring(start, position);
                if (pattern.equals("")) {
                    throw new JException("S0301", position);
                }
                position++;
                currentChar = path.charAt(position);
                // flags
                start = position;
                while (currentChar == 'i' || currentChar == 'm') {
                    position++;
                    currentChar = path.charAt(position);
                }
                flags = path.substring(start, position) + 'g';
                return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            }
            if ((currentChar == '(' || currentChar == '[' || currentChar == '{') && path.charAt(position - 1) != '\\') {
                depth++;
            }
            if ((currentChar == ')' || currentChar == ']' || currentChar == '}') && path.charAt(position - 1) != '\\') {
                depth--;
            }
            position++;
        }
        throw new JException("S0302", position);
    };

    Token next(boolean prefix) throws JException {
        if (position >= length) return null;
        char currentChar = path.charAt(position);
        // skip whitespace
        while (position < length && " \t\n\r".indexOf(currentChar) > -1) { // Uli: removed \v as Java doesn't support it
            position++;
            if (position >= length) return null; // Uli: JS relies on charAt returns null
            currentChar = path.charAt(position);
        }
        // skip comments
        if (currentChar == '/' && path.charAt(position + 1) == '*') {
            var commentStart = position;
            position += 2;
            currentChar = path.charAt(position);
            while (!(currentChar == '*' && path.charAt(position + 1) == '/')) {
                currentChar = path.charAt(++position);
                if (position >= length) {
                    // no closing tag
                    throw new JException("S0106", commentStart);
                }
            }
            position += 2;
            currentChar = path.charAt(position);
            return next(prefix); // need this to swallow any following whitespace
        }
        // test for regex
        if (prefix != true && currentChar == '/') {
            position++;
            return create("regex", scanRegex());
        }
        // handle double-char operators
        boolean haveMore = position < path.length()-1; // Java: position+1 is valid
        if (currentChar == '.' && haveMore && path.charAt(position + 1) == '.') {
            // double-dot .. range operator
            position += 2;
            return create("operator", "..");
        }
        if (currentChar == ':' && haveMore && path.charAt(position + 1) == '=') {
            // := assignment
            position += 2;
            return create("operator", ":=");
        }
        if (currentChar == '!' && haveMore && path.charAt(position + 1) == '=') {
            // !=
            position += 2;
            return create("operator", "!=");
        }
        if (currentChar == '>' && haveMore && path.charAt(position + 1) == '=') {
            // >=
            position += 2;
            return create("operator", ">=");
        }
        if (currentChar == '<' && haveMore && path.charAt(position + 1) == '=') {
            // <=
            position += 2;
            return create("operator", "<=");
        }
        if (currentChar == '*' && haveMore && path.charAt(position + 1) == '*') {
            // **  descendant wildcard
            position += 2;
            return create("operator", "**");
        }
        if (currentChar == '~' && haveMore && path.charAt(position + 1) == '>') {
            // ~>  chain function
            position += 2;
            return create("operator", "~>");
        }
        // test for single char operators
        if (operators.get(""+currentChar)!=null) {
            position++;
            return create("operator", currentChar);
        }
        // test for string literals
        if (currentChar == '"' || currentChar == '\'') {
            char quoteType = currentChar;
            // double quoted string literal - find end of string
            position++;
            var qstr = "";
            while (position < length) {
                currentChar = path.charAt(position);
                if (currentChar == '\\') { // escape sequence
                    position++;
                    currentChar = path.charAt(position);
                    if (escapes.get(""+currentChar)!=null) {
                        qstr += escapes.get(""+currentChar);
                    } else if (currentChar == 'u') {
                        //  u should be followed by 4 hex digits
                        String octets = path.substring(position + 1, (position + 1) + 4);
                        if (octets.matches("^[0-9a-fA-F]+$")) { //  /^[0-9a-fA-F]+$/.test(octets)) {
                            int codepoint = Integer.parseInt(octets, 16);
                            qstr += Character.toString((char) codepoint);
                            position += 4;
                        } else {
                            throw new JException("S0104", position);
                        }
                    } else {
                        // illegal escape sequence
                        throw new JException("S0301", position, currentChar);

                    }
                } else if (currentChar == quoteType) {
                    position++;
                    return create("string", qstr);
                } else {
                    qstr += currentChar;
                }
                position++;
            }
            throw new JException("S0101", position);
        }
        // test for numbers
        Pattern numregex = Pattern.compile("^-?(0|([1-9][0-9]*))(\\.[0-9]+)?([Ee][-+]?[0-9]+)?");
        Matcher match = numregex.matcher(path.substring(position));
        if (match.find()) {
            double num = Double.parseDouble(match.group(0));
            if (!Double.isNaN(num) && Double.isFinite(num)) {
                position += match.group(0).length();
                return create("number", num);
            } else {
                throw new JException("S0102", position); //, match.group[0]);
            }
        }

        // test for quoted names (backticks)
        String name;
        if (currentChar == '`') {
            // scan for closing quote
            position++;
            int end = path.indexOf('`', position);
            if (end != -1) {
                name = path.substring(position, end);
                position = end + 1;
                return create("name", name);
            }
            position = length;
            throw new JException("S0105", position);
        }
        // test for names
        int i = position;
        char ch;
        while (true) {
            //if (i>=length) return null; // Uli: JS relies on charAt returns null

            ch = i<length ? path.charAt(i) : 0;
            if (i == length || " \t\n\r".indexOf(ch) > -1 || operators.containsKey(""+ch)) { // Uli: removed \v
                if (path.charAt(position) == '$') {
                    // variable reference
                    String _name = path.substring(position + 1, i);
                    position = i;
                    return create("variable", _name);
                } else {
                    String _name = path.substring(position, i);
                    position = i;
                    switch (_name) {
                        case "or":
                        case "in":
                        case "and":
                            return create("operator", _name);
                        case "true":
                            return create("value", true);
                        case "false":
                            return create("value", false);
                        case "null":
                            return create("value", null);
                        default:
                            if (position == length && _name.equals("")) {
                                // whitespace at end of input
                                return null;
                            }
                            return create("name", _name);
                    }
                }
            } else {
                i++;
            }
        }
    }

    public static void main(String[] args) throws Throwable {
        Tokenizer t = new Tokenizer("name.value\n/* yo */ {x*y}\n/* comment */");
        Token to;
        while ( (to= t.next(true))!=null ) {
            System.out.println("Token "+to.type+"="+to.value+" pos="+to.position);
        }
    }
}
