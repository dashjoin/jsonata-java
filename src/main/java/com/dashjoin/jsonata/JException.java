package com.dashjoin.jsonata;

import java.util.List;

public class JException extends Exception {
    public JException(String error, int location) {
        super("JSonataException "+error+" @"+location);
    }
    public JException(String error, int location, Object currentToken) {
        super("JSonataException "+error+" @"+location+" current="+currentToken);
    }
    public JException(String error, int location, Object currentToken, Object expected) {
        super("JSonataException "+error+" @"+location+" current="+currentToken+" expected="+expected);
    }

    // Recover
    String type;
    List<Tokenizer.Token> remaining;
}
