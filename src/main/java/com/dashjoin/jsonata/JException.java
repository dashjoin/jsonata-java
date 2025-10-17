/**
 * jsonata-java is the JSONata Java reference port
 * 
 * Copyright Dashjoin GmbH. https://dashjoin.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dashjoin.jsonata;

import java.util.IllegalFormatException;
import java.util.List;
import java.util.regex.Matcher;

public class JException extends RuntimeException {

    private static final long serialVersionUID = -3354943281127831704L;
    
    String error;
    int location;
    Object current;
    Object expected;

    public JException(String error) {
        this(error, -1, null, null);
    }
    public JException(String error, int location) {
        this(error, location, null, null);
    }
    public JException(String error, int location, Object currentToken) {
        this(error, location, currentToken, null);
    }
    public JException(String error, int location, Object currentToken, Object expected) {
        this(null, error, location, currentToken, expected);
    }
    public JException(Throwable cause, String error, int location, Object currentToken, Object expected) {
        super(msg(error, location, currentToken, expected), cause);
        this.error = error; this.location = location;
        this.current = currentToken;
        this.expected = expected;
    }

    /**
     * Returns the error code, i.e. S0201
     * @return
     */
    public String getError() {
        return error;
    }

    /**
     * Returns the error location (in characters)
     * @return
     */
    public int getLocation() {
        return location;
    }

    /**
     * Returns the current token
     * @return
     */
    public Object getCurrent() {
        return current;
    }

    /**
     * Returns the expected token
     * @return
     */
    public Object getExpected() {
        return expected;
    }

    /**
     * Returns the error message with error details in the text.
     * Example: Syntax error: ")" {code=S0201 position=3}
     * @return
     */
    public String getDetailedErrorMessage() {
        return msg(error, location, current, expected, true);
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
        return msg(error, location, arg1, arg2, false);
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
     * @param details True = add error details as text, false = don't add details (use getters to retrieve details)
     * @return
     */
    public static String msg(String error, int location, Object arg1, Object arg2, boolean details) {
        String message = Jsonata.errorCodes.get(error);

        if (message==null) {
            // unknown error code
            return "JSonataException "+error+
                (details ? " {code=unknown position="+location+" arg1="+arg1+" arg2="+arg2+"}" : "");
        }

        String formatted = message;
        try {
            if ("{{{message}}}".equals(message))
                return ""+arg1;
            // Replace any {{var}} with Java format "%1$s"
            formatted = formatted.replaceFirst("\\{\\{\\w+\\}\\}", Matcher.quoteReplacement("\"%1$s\""));
            formatted = formatted.replaceFirst("\\{\\{\\w+\\}\\}", Matcher.quoteReplacement("\"%2$s\""));

            formatted = String.format(formatted, arg1, arg2);
        } catch (IllegalFormatException ex) {
            ex.printStackTrace();
            // ignore
        }
        if (details) {
            formatted = formatted + " {code="+error;
            if (location>=0)
                formatted += " position="+location;
            formatted += "}";
        }
        return formatted;
    }

    // Recover
    String type;
    List<Tokenizer.Token> remaining;
}
