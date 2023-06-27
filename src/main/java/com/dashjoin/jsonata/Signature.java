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
    int optArgs;

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
        String args = argTypes.replaceAll("\\(.*\\)", "(")
            .replaceAll("<.>", "");
        int minArgs = args.length();
        int optArgs = 0;
        for (char c : args.toCharArray()) {
            switch (c) {
                case '-': minArgs-=2; break;
                case '?': optArgs++; minArgs-=2; break;
            }
        }
        this.minArgs = minArgs;
        this.optArgs = optArgs;
    }

    @Override
    public Object validate(Object args, Object context) {

        if (!argFromContext || context==null)
            return args;

        List res = new ArrayList((List)args);
        if (res.size()<=(minArgs+optArgs)) {
            // If the signature contains "-" take arg from context
            res.add(0, context);
        }

        return res;
    }

    public int getMinArgs() {
        return minArgs;
    }
}
