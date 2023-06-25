package com.dashjoin.jsonata;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dashjoin.jsonata.Parser.Symbol;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Functions {

    /**
     * Sum function
     * @param {Object} args - Arguments
     * @returns {number} Total value of arguments
     */
    public static Number sum(List<Number> args) {
        // undefined inputs always return undefined
        if (args == null) {
            return null;
        }

        double total = args.stream().mapToDouble(Number::doubleValue).sum();
        // args.forEach(num -> {
        //     total += num.doubleValue();
        // });
        return Utils.convertNumber(total);
    }

    /**
     * Count function
     * @param {Object} args - Arguments
     * @returns {number} Number of elements in the array
     */
    public static Number count(List<Object> args) {
        // undefined inputs always return undefined
        if (args == null) {
            return 0;
        }

        return args.size();
    }

    /**
     * Max function
     * @param {Object} args - Arguments
     * @returns {number} Max element in the array
     */
    public static Number max(List<Number> args) {
        // undefined inputs always return undefined
        if (args == null || args.size() == 0) {
            return null;
        }

        OptionalDouble res = args.stream().mapToDouble(Number::doubleValue).max();
        if (res.isPresent())
            return Utils.convertNumber(res.getAsDouble());
        else
            return null;
    }

    /**
     * Min function
     * @param {Object} args - Arguments
     * @returns {number} Min element in the array
     */
    public static Number min(List<Number> args) {
        // undefined inputs always return undefined
        if (args == null || args.size() == 0) {
            return null;
        }

        OptionalDouble res = args.stream().mapToDouble(Number::doubleValue).min();
        if (res.isPresent())
            return Utils.convertNumber(res.getAsDouble());
        else
            return null;
    }

    /**
     * Average function
     * @param {Object} args - Arguments
     * @returns {number} Average element in the array
     */
    public static Number average(List<Number> args) {
        // undefined inputs always return undefined
        if (args == null || args.size() == 0) {
            return null;
        }

        OptionalDouble res = args.stream().mapToDouble(Number::doubleValue).average();
        if (res.isPresent())
            return Utils.convertNumber(res.getAsDouble());
        else
            return null;
    }

    /**
     * Stringify arguments
     * @param {Object} arg - Arguments
     * @param {boolean} [prettify] - Pretty print the result
     * @returns {String} String from arguments
     */
    public static String string(Object arg, Boolean prettify) {
        try {
            if (prettify!=null && prettify)
                return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(arg);
            else
                return new ObjectMapper().writeValueAsString(arg);
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return arg.toString();
        }
    }

    /**
     * Create substring based on character number and length
     * @param {String} str - String to evaluate
     * @param {Integer} start - Character number to start substring
     * @param {Integer} [length] - Number of characters in substring
     * @returns {string|*} Substring
     */
    public static String substring(String str, Integer start, Integer length) {
        // undefined inputs always return undefined
        if (str == null) {
            return null;
        }

        // not used: var strArray = stringToArray(str);
        var strLength = str.length();

        if (strLength + start < 0) {
            start = 0;
        }

        if (length != null) {
            if (length <= 0) {
                return "";
            }
            var end = start >= 0 ? start + length : strLength + start + length;
            return str.substring(start<0 ? start+strLength : start, end);
        }

        return str.substring(start<0 ? start+strLength : start);
    }

    /**
     * Create substring up until a character
     * @param {String} str - String to evaluate
     * @param {String} chars - Character to define substring boundary
     * @returns {*} Substring
     */
    public static String substringBefore(String str, String chars) {
        // undefined inputs always return undefined
        if (str == null) {
            return null;
        }

        var pos = str.indexOf(chars);
        if (pos > -1) {
            return str.substring(0, pos);
        } else {
            return str;
        }
    }

    /**
     * Create substring after a character
     * @param {String} str - String to evaluate
     * @param {String} chars - Character to define substring boundary
     * @returns {*} Substring
     */
    public static String substringAfter(String str, String chars) {
        // undefined inputs always return undefined
        if (str == null) {
            return null;
        }

        var pos = str.indexOf(chars);
        if (pos > -1) {
            return str.substring(pos + chars.length());
        } else {
            return str;
        }
    }

    /**
     * Lowercase a string
     * @param {String} str - String to evaluate
     * @returns {string} Lowercase string
     */
    public static String lowercase(String str) {
        // undefined inputs always return undefined
        if (str == null) {
            return null;
        }

        return str.toLowerCase();
    }

    /**
     * Uppercase a string
     * @param {String} str - String to evaluate
     * @returns {string} Uppercase string
     */
    public static String uppercase(String str) {
        // undefined inputs always return undefined
        if (str == null) {
            return null;
        }

        return str.toUpperCase();
    }

    /**
     * length of a string
     * @param {String} str - string
     * @returns {Number} The number of characters in the string
     */
    public static Integer length(String str) {
        // undefined inputs always return undefined
        if (str == null) {
            return null;
        }

        return str.length();
    }

    /**
     * Normalize and trim whitespace within a string
     * @param {string} str - string to be trimmed
     * @returns {string} - trimmed string
     */
    public static String trim(String str) {
        // undefined inputs always return undefined
        if (str == null) {
            return null;
        }

        // normalize whitespace
        var result = str.replaceAll("[ \t\n\r]+", " ");
        if (result.charAt(0) == ' ') {
            // strip leading space
            result = result.substring(1);
        }
        if (result.charAt(result.length() - 1) == ' ') {
            // strip trailing space
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * Pad a string to a minimum width by adding characters to the start or end
     * @param {string} str - string to be padded
     * @param {number} width - the minimum width; +ve pads to the right, -ve pads to the left
     * @param {string} [char] - the pad character(s); defaults to ' '
     * @returns {string} - padded string
     */
    public static String pad(String str, Integer width, String _char) {
        // undefined inputs always return undefined
        if (str == null) {
            return null;
        }

        if (_char == null || _char.isEmpty()) {
            _char = " ";
        }

        String result;
        var padLength = Math.abs(width) - str.length();
        if (padLength > 0) {
            var padding = _char; while (padding.length()<padLength+1) padding = padding + padding;
            if (_char.length() > 1) {
                padding = substring(padding, 0, padLength);
            }
            if (width > 0) {
                result = str + padding;
            } else {
                result = padding + str;
            }
        } else {
            result = str;
        }
        return result;
    }

    static class RegexpMatch {
        String match;
        int index;
        List<String> groups;
    }
    /**
     * Evaluate the matcher function against the str arg
     *
     * @param {*} matcher - matching function (native or lambda)
     * @param {string} str - the string to match against
     * @returns {object} - structure that represents the match(es)
     */
    public static List<RegexpMatch> evaluateMatcher(Pattern matcher, String str) {
        List<RegexpMatch> res = new ArrayList<>();
        Matcher m = matcher.matcher(str);
        while (m.find()) {
            RegexpMatch rm = new RegexpMatch();

            System.out.println("grc="+m.groupCount()+" "+m.group(1));

            rm.index = m.start();
            rm.match = m.group();

            List<String> groups = new ArrayList<>();
            // Collect the groups
            for (int g=1; g<=m.groupCount(); g++)
                groups.add(m.group(g));

            rm.groups = groups;
            res.add(rm);
        }
        return res;
    }

    /**
     * Tests if the str contains the token
     * @param {String} str - string to test
     * @param {String} token - substring or regex to find
     * @returns {Boolean} - true if str contains token
     */
    public static Boolean contains(String str, Object token) {
        // undefined inputs always return undefined
        if (str == null) {
            return null;
        }

        boolean result;

        if (token instanceof String) {
            result = (str.indexOf((String)token) != -1);
        } else if (token instanceof Pattern) {
            var matches = evaluateMatcher((Pattern)token, str);
            System.out.println("match = "+matches);
            //result = (typeof matches !== 'undefined');
            //throw new Error("regexp not impl"); //result = false;
            result = !matches.isEmpty();
        } else {
            throw new Error("unknown type to match: "+token);
        }

        return result;
    }

    /**
     * Match a string with a regex returning an array of object containing details of each match
     * @param {String} str - string
     * @param {String} regex - the regex applied to the string
     * @param {Integer} [limit] - max number of matches to return
     * @throws JException
     * @returns {Array} The array of match objects
     */
    public static List<RegexpMatch> match(String str, Pattern regex, Integer limit) throws JException {
        // undefined inputs always return undefined
        if (str == null) {
            return null;
        }

        // limit, if specified, must be a non-negative number
        if (limit!=null && limit < 0) {
            throw new JException("D3040", -1, limit
            );
        }

        var result = Utils.createSequence();
        var matches = evaluateMatcher(regex, str);
        int max = Integer.MAX_VALUE;
        if (limit!=null)
            max = limit;

        for (int i=0; i < matches.size(); i++) {
            Map m = new LinkedHashMap<>();
            RegexpMatch rm = matches.get(i);
            // Convert to JSON map:
            m.put("match", rm.match);
            m.put("index", rm.index);
            m.put("groups", rm.groups);
            result.add(m);
            if (i>=max)
                break;
        }
        return (List)result;
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

    public static String replace(String str, Object pattern, String replacement, Integer limit) {
        // FIXME: limit
        if (pattern instanceof String) {
            return str.replace((String)pattern, replacement);
        } else {
            return str.replaceAll(((Pattern)pattern).pattern(), replacement);
        }
    }

    public static List<String> split(String str, Object pattern, Integer limit) {
        // FIXME: limit
        if (pattern instanceof String) {
            // FIXME: must escape pattern to regexp!
            return Arrays.asList( str.split((String)pattern) );
        } else {
            return Arrays.asList( str.split(((Pattern)pattern).pattern() ) );
        }
    }

    /**
     * Cast argument to number
     * @param {Object} arg - Argument
     * @returns {Number} numeric value of argument
     */
    public static Number number(Object arg) {
        Number result = null;

        // undefined inputs always return undefined
        if (arg == null) {
            return null;
        }

        if (arg instanceof Number)
            result = (Number)arg;
        else if (arg instanceof String) {
            result = Utils.convertNumber( Double.valueOf((String)arg) );
        } else if (arg instanceof Boolean) {
            result = ((boolean)arg) ? 1:0;
        }
        return result;
    }

    /**
     * Absolute value of a number
     * @param {Number} arg - Argument
     * @returns {Number} absolute value of argument
     */
    public static Number abs(Number arg) {

        // undefined inputs always return undefined
        if (arg == null) {
            return null;
        }

        return arg instanceof Double ?
            Math.abs((double)arg) :
            Math.abs((int)arg);
    }

    /**
     * Rounds a number down to integer
     * @param {Number} arg - Argument
     * @returns {Number} rounded integer
     */
    public static Number floor(Number arg) {

        // undefined inputs always return undefined
        if (arg == null) {
            return null;
        }

        return Math.floor(arg.doubleValue());
    }

    /**
     * Rounds a number up to integer
     * @param {Number} arg - Argument
     * @returns {Number} rounded integer
     */
    public static Number ceil(Number arg) {

        // undefined inputs always return undefined
        if (arg == null) {
            return null;
        }

        return Math.ceil(arg.doubleValue());
    }

    /**
     * Round to half even
     * @param {Number} arg - Argument
     * @param {Number} [precision] - number of decimal places
     * @returns {Number} rounded integer
     */
    public static Number round(Number arg, Number precision) {

        // undefined inputs always return undefined
        if (arg == null) {
            return null;
        }

        BigDecimal b = new BigDecimal(arg.doubleValue());
        if (precision==null)
            precision = 0;
        b.setScale(precision.intValue(), RoundingMode.HALF_EVEN);
        
        return Utils.convertNumber( b.doubleValue() );
    }

    /**
     * Square root of number
     * @param {Number} arg - Argument
     * @throws JException
     * @returns {Number} square root
     */
    public static Number sqrt(Number arg) throws JException {

        // undefined inputs always return undefined
        if (arg == null) {
            return null;
        }

        if (arg.doubleValue() < 0) {
            throw new JException("D3060",
                1,
                arg
            );
        }

        return Utils.convertNumber( Math.sqrt(arg.doubleValue()) );
    }

    /**
     * Raises number to the power of the second number
     * @param {Number} arg - the base
     * @param {Number} exp - the exponent
     * @throws JException
     * @returns {Number} rounded integer
     */
    public static Number power(Number arg, Number exp) throws JException {

        // undefined inputs always return undefined
        if (arg == null) {
            return null;
        }

        double result = Math.pow(arg.doubleValue(), exp.doubleValue());

        if (!Double.isFinite(result)) {
            throw new JException("D3061",
                1,
                arg,
                exp
            );
        }

        return result;
    }

    /**
     * Returns a random number 0 <= n < 1
     * @returns {number} random number
     */
    public static Number random() {
        return Math.random();
    }

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
    public static Boolean not(Object arg) {
        // undefined inputs always return undefined
        if (arg == null) {
            return null;
        }

        return !toBoolean(arg);
    }



    ///////
    ///////
    ///////
    ///////



    public static Object _count(Object input, Object arg) {
        Object el = ((List)arg).get(0);
        if (el instanceof List) {
            return ((List)el).size();
        } else
            return 1;
    }

    public static Object _string(Object input, Object arg) { 
        try {
            return new ObjectMapper().writeValueAsString(((List)arg).get(0));
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return input.toString();
        }
        //return input.toString();
    }

    public static Object _join(Object input, Object arg) {
        //System.out.println(((List)arg).get(0)+" "+arg.getClass());
        List l = (List)arg;
        if (l.isEmpty()) return null;
        String sep = l.size()==1 ? "": (String)l.get(1);
        String[] strs;
        if (l.get(0) instanceof String) {
            // single string
            strs = new String[]{(String)l.get(0)};
        } else {
            // list of strings
            List<String> lstr = (List)l.get(0);
            strs = lstr.toArray(new String[lstr.size()]);
        }
        String res = String.join(sep, strs);
        //System.out.println("joined = "+res);
        return res;
        //return null;
    }

    public static Object _lowercase(Object input, Object arg) {
        List l = (List)arg;
        return ((String)l.get(0)).toLowerCase();
    }

    public static Object _uppercase(Object input, Object arg) {
        List l = (List)arg;
        return ((String)l.get(0)).toUpperCase();
    }

    public static Object _substring(Object input, Object arg) {
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
        return (result instanceof Symbol && ((Symbol)result)._jsonata_lambda);
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

    public static String test(String a, String b) {
        return a+b;
    }

    public static Method getFunction(String name) {
        Method[] methods = Functions.class.getMethods();
        for (Method m : methods) {
            // if (m.getModifiers() == (Modifier.STATIC | Modifier.PUBLIC) ) {
            //     System.out.println(m.getName());
            //     System.out.println(m.getParameterTypes());
            // }
            if (m.getName().equals(name)) {
                return m;
            }
        }
        return null;
    }

    public static Object call(String name, List<Object> args) throws Throwable {
        Method[] methods = Functions.class.getMethods();
        for (Method m : methods) {
            // if (m.getModifiers() == (Modifier.STATIC | Modifier.PUBLIC) ) {
            //     System.out.println(m.getName());
            //     System.out.println(m.getParameterTypes());
            // }
            if (m.getName().equals(name)) {
                Class<?>[] types = m.getParameterTypes();
                int nargs = m.getParameterTypes().length;
                List<Object> callArgs = new ArrayList<>(args);
                while (callArgs.size()<nargs) {
                    // Add default arg null if not enough args were provided
                    callArgs.add(null);
                }

                // Special handling of one arg if function requires list:
                // Wrap the single arg in a list with one element
                if (nargs>0 && List.class.isAssignableFrom(types[0]) && !(callArgs.get(0) instanceof List)) {
                    Object arg1 = callArgs.get(0);
                    List wrap = new ArrayList<>(); wrap.add(arg1);
                    callArgs.set(0, wrap);
                    //System.err.println("wrapped "+arg1+" as "+wrap);
                }

                try {
                    return m.invoke(null, callArgs.toArray());
                } catch (IllegalAccessException e) {
                    throw new Exception("Access error calling function "+name, e);
                } catch (IllegalArgumentException e) {
                    throw new Exception("Argument error calling function "+name, e);
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                    throw e.getTargetException();
                }
            }
        }

        throw new Error("Function not found: "+name);
    }

    public static void main(String[] _args) throws Throwable {
        List args = new ArrayList();
        // List arg = new ArrayList();
        // arg.add("5"); arg.add("6");
        // args.add(arg);
        args.add("string1");
        args.add("string2");
        //args.add("string3");
        Object res = Functions.call("test", args);
        System.out.println("Result = "+res);
    }
}
