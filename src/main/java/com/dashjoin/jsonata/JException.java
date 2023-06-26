package com.dashjoin.jsonata;

import java.util.List;

public class JException extends Exception {

    String error;
    int location;
    Object current;
    Object expected;

    public JException(String error, int location) {
        super("JSonataException "+error+" @"+location);
        this.error = error; this.location = location;
    }
    public JException(String error, int location, Object currentToken) {
        super("JSonataException "+error+" @"+location+" current="+currentToken);
        this.error = error; this.location = location;
        this.current = currentToken;
    }
    public JException(String error, int location, Object currentToken, Object expected) {
        super("JSonataException "+error+" @"+location+" current="+currentToken+" expected="+expected);
        this.error = error; this.location = location;
        this.current = currentToken;
        this.expected = expected;
    }

    // Recover
    String type;
    List<Tokenizer.Token> remaining;
}
