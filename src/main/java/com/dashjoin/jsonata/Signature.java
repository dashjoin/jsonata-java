package com.dashjoin.jsonata;

import java.util.ArrayList;
import java.util.List;

import com.dashjoin.jsonata.Jsonata.JFunctionSignatureValidation;

public class Signature implements JFunctionSignatureValidation {

    String signature;

    public Signature(String signature) {
        this.signature = signature;
        parseSignature();
    }

    String argTypes;
    boolean argFromContext;
    int minArgs;

    void parseSignature() {
        // FIXME: quick + dirty version here...
        // we let Java do the validation work

        // strip < and >
        assert signature.charAt(0)=='<' && signature.charAt(signature.length()-1)=='>';
        String s = signature.substring(1, signature.length()-2);

        int colon = s.indexOf(':');
        if (colon<0) colon = s.length();

        argTypes = s.substring(0, colon).replace("<a>", "").replace("<f>", "");
        argFromContext = argTypes.contains("-");

        // Calculate min number of function args
        // All args minus optional (?), and ignore context arg (-)
        int minArgs = argTypes.length();
        for (char c : argTypes.toCharArray()) {
            switch (c) {
                case '-': minArgs-=2; break;
                case '?': minArgs-=1; break;
            }
        }
        this.minArgs = minArgs;
    }

    @Override
    public Object validate(Object args, Object context) {

        List res = new ArrayList((List)args);
        if (res.isEmpty() && argFromContext) {
            // If the signature contains "-" take arg from context
            res.add(context);
        }

        return res;
    }

    public int getMinArgs() {
        return minArgs;
    }
}
