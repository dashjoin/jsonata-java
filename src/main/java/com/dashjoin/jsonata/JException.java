package com.dashjoin.jsonata;

import java.util.IllegalFormatException;
import java.util.List;
import java.util.regex.Matcher;

public class JException extends Exception {

    String error;
    int location;
    Object current;
    Object expected;

    public JException(String error, int location) {
        super(msg(error, location, null, null));
        this.error = error; this.location = location;
    }
    public JException(String error, int location, Object currentToken) {
        super(msg(error, location, currentToken, null));
        this.error = error; this.location = location;
        this.current = currentToken;
    }
    public JException(String error, int location, Object currentToken, Object expected) {
        super(msg(error, location, currentToken, expected));
        this.error = error; this.location = location;
        this.current = currentToken;
        this.expected = expected;
    }

    /**
     * Generate error message from given error code
     * Codes are defined in Jsonata.errorCodes
     * 
     * Fallback: if error code does not exist, return a generic message
     * 
     * @param error
     * @param location
     * @param arg1
     * @param arg2
     * @return
     */
    public static String msg(String error, int location, Object arg1, Object arg2) {
        String message = Jsonata.errorCodes.get(error);

        if (message==null) {
            // unknown error code
            return "JSonataException "+error+" {code=unknown position="+location+" arg1="+arg1+" arg2="+arg2+"}";
        }

        String formatted = message;
        try {
            // Replace any {{var}} with Java format "%1$s"
            formatted = formatted.replaceFirst("\\{\\{\\w+\\}\\}", Matcher.quoteReplacement("\"%1$s\""));
            formatted = formatted.replaceFirst("\\{\\{\\w+\\}\\}", Matcher.quoteReplacement("\"%2$s\""));

            formatted = String.format(formatted, arg1, arg2);
        } catch (IllegalFormatException ex) {
            ex.printStackTrace();
            // ignore
        }
        formatted = formatted + " {code="+error;
        if (location>=0)
            formatted += " position="+location;
        formatted += "}";
        return formatted;
    }

    // Recover
    String type;
    List<Tokenizer.Token> remaining;
}
