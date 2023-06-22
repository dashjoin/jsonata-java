package com.dashjoin.jsonata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Functions {
    /**
     * Evaluate an input and return a boolean
     * @param {*} arg - Arguments
     * @returns {boolean} Boolean
     */
    public static Boolean toBoolean(Object arg) {
        // cast arg to its effective boolean value
        // boolean: unchanged
        // string: zero-length -> false; otherwise -> true
        // number: 0 -> false; otherwise -> true
        // null -> false
        // array: empty -> false; length > 1 -> true
        // object: empty -> false; non-empty -> true
        // function -> false

        // undefined inputs always return undefined
        if (arg == null) {
            return false; // null; // Uli: Null would need to be handled as false anyway
        }

        var result = false;
        if (arg instanceof List) {
            List l = (List)arg;
            if (l.size() == 1) {
                result = toBoolean(l.get(0));
            } else if (l.size() > 1) {
                long truesLength = l.stream().filter(e -> toBoolean(e)).count();
                result = truesLength > 0;
            }
        } else if (arg instanceof String) {
            String s = (String)arg;
            if (s.length() > 0) {
                result = true;
            }
        } else if (arg instanceof Number) { //isNumeric(arg)) {
            if (((Number)arg).doubleValue() != 0) {
                result = true;
            }
        // FIXME what is the semantic in Java?
        // } else if (arg !== null && typeof arg === 'object') {
        //     if (Object.keys(arg).length > 0) {
        //         result = true;
        //     }
        } else if (arg instanceof Boolean) {
            result = (boolean)arg;
        }
        return result;
    }

    /**
     * returns the Boolean NOT of the arg
     * @param {*} arg - argument
     * @returns {boolean} - NOT arg
     */
    public static Boolean not(Object input, Object arg) {
        // undefined inputs always return undefined
        if (arg == null) {
            return null;
        }

        return !toBoolean(arg);
    }

    /**
     * Join an array of strings
     * @param {Array} strs - array of string
     * @param {String} [separator] - the token that splits the string
     * @returns {String} The concatenated string
     */
    public static String join(List<String> strs, String separator) {
        // undefined inputs always return undefined
        if (strs == null) {
            return null;
        }

        // if separator is not specified, default to empty string
        if (separator == null) {
            separator = "";
        }

        return String.join(separator, strs);
    }

    public static Object join(Object input, Object arg) {
        //System.out.println(((List)arg).get(0)+" "+arg.getClass());
        List l = (List)arg;
        String res = String.join((String)l.get(1), (List)l.get(0));
        //System.out.println("joined = "+res);
        return res;
        //return null;
    }

    public static Object lowercase(Object input, Object arg) {
        List l = (List)arg;
        return ((String)l.get(0)).toLowerCase();
    }

    public static Object uppercase(Object input, Object arg) {
        List l = (List)arg;
        return ((String)l.get(0)).toUpperCase();
    }

    public static Object substring(Object input, Object arg) {
        List l = (List)arg;
        Object o = l.get(0);

        // Handle list case
        if (o instanceof List) {
            List res = Utils.createSequence();
            for (Object s : (List)o) {
                List args = Utils.createSequence(s);
                args.add(l.get(1));
                args.add(l.get(2));
                res.add(substring(input, args));
            }
            return res;
        }
        else if (o instanceof Map) {
            System.err.println("substring Map TODO");
            return null;
        }

        String s = (String)o;
        if (s==null)
            return null;
        int i1 = ((Number)l.get(1)).intValue();
        if (i1<0) {
            i1 = s.length()+i1;
            if (i1<0)
                i1 = 0;
        }
        int i2 = l.size()>2 ? ((Number)l.get(2)).intValue() : s.length()-i1;
        if (i2<0)
            i2 = 0;
        String t = null;
        try {
            t = s.substring(i1, i1+i2);
            //System.out.println("substring "+s+" "+i1+" "+i2+" = "+t+" l1="+s.length()+" l2="+t.length());
        } catch (Exception ignore) {
            //
        }            
        return t;
    }

    /**
     * Append second argument to first
     * @param {Array|Object} arg1 - First argument
     * @param {Array|Object} arg2 - Second argument
     * @returns {*} Appended arguments
     */
    public static Object append(Object arg1, Object arg2) {
        // disregard undefined args
        if (arg1 == null) {
            return arg2;
        }
        if (arg2 == null) {
            return arg1;
        }

        // New Java case: if both args are "string-like"
        if ((arg1 instanceof Character || arg1 instanceof String) &&
        (arg2 instanceof Character || arg2 instanceof String)) {
            return ""+arg1+arg2;
        }

        // if either argument is not an array, make it so
        if (!(arg1 instanceof List)) {
            arg1 = Utils.createSequence(arg1);
        }
        if (!(arg2 instanceof List)) {
            arg2 = new ArrayList<>(Arrays.asList(arg2));
        }
        // else
        //     // Arg2 was a list: add it as a list element (don't flatten)
        //     ((List)arg1).add((List)arg2);
        arg1 = new ArrayList<>((List)arg1); // create a new copy!
        ((List)arg1).addAll((List)arg2);
        return arg1;
    }

    public static boolean isLambda(Object result) {
        return false;
    }

    /**
     * Return value from an object for a given key
     * @param {Object} input - Object/Array
     * @param {String} key - Key in object
     * @returns {*} Value of key in object
     */
    public static Object lookup(Object input, String key) {
        // lookup the 'name' item in the input
        Object result = null;
        if (input instanceof List) {
            List _input = (List)input;
            result = Utils.createSequence();
            for(var ii = 0; ii < _input.size(); ii++) {
                var res = lookup(_input.get(ii), key);
                if (res != null) {
                    if (res instanceof List) {
                        ((List)result).addAll((List)res);
                    } else {
                        ((List)result).add(res);
                    }
                }
            }
        } else if (input instanceof Map) { // && typeof input === 'object') {
            result = ((java.util.Map)input).get(key);
        }
        return result;
    }
}
