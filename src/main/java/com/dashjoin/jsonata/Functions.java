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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TimeZone;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static com.dashjoin.jsonata.Jsonata.jsonata;
import com.dashjoin.jsonata.Jsonata.JFunction;
import com.dashjoin.jsonata.Parser.Symbol;
import com.dashjoin.jsonata.Utils.JList;
import com.dashjoin.jsonata.json.Json;
import com.dashjoin.jsonata.utils.Constants;
import com.dashjoin.jsonata.utils.DateTimeUtils;

@SuppressWarnings({"rawtypes", "unchecked"})
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
        return total;
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
            return res.getAsDouble();
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
            return res.getAsDouble();
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
            return res.getAsDouble();
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
      
      if (arg instanceof JList)
        if (((JList)arg).outerWrapper)
          arg = ((JList)arg).get(0);
      
        if (arg == null)
          return null;

        // see https://docs.jsonata.org/string-functions#string: Strings are unchanged
        if (arg instanceof String)
          return (String) arg;
        
        StringBuilder sb = new StringBuilder();
        string(sb, arg, prettify!=null && prettify, "");
        return sb.toString();
    }

    /**
     * Internal recursive string function based on StringBuilder.
     * Avoids creation of intermediate String objects
     */    
    static void string(StringBuilder b, Object arg, boolean prettify, String indent) {
        // if (arg == null)
        //   return null;
        
        if (arg==null || arg == Jsonata.NULL_VALUE) {
          b.append("null"); return;
        }
        
        if (arg instanceof JFunction) {
            return;
        }
      
        if (arg instanceof Symbol) {
            return;
        }

        if (arg instanceof Double) {
          // TODO: this really should be in the jackson serializer
          BigDecimal bd = new BigDecimal((Double)arg, new MathContext(15));
          String res = bd.stripTrailingZeros().toString();
          
          if (res.indexOf("E+") > 0)
            res = res.replace("E+", "e+");
          if (res.indexOf("E-") > 0)
            res = res.replace("E-", "e-");
          
            b.append(res); return;
        }

        if (arg instanceof Number || arg instanceof Boolean) {
            b.append(arg); return;
        }
        
        if (arg instanceof String) {
            // quotes within strings must be escaped
            Utils.quote((String)arg, b); return;
        }

        if (arg instanceof Map) {
            b.append('{');
            if (prettify)
                b.append('\n');
            for (Entry<String, Object> e : ((Map<String, Object>) arg).entrySet()) {
                if (prettify) {
                    b.append(indent);
                    b.append("  ");
                }
                b.append('"');
                b.append(e.getKey());
                b.append('"');
                b.append(':');
                if (prettify)
                    b.append(' ');
                final Object v = e.getValue();
                if (v instanceof String || v instanceof Symbol
                        || v instanceof JFunction) {
                    b.append('"');
                    string(b, v, prettify, indent + "  ");
                    b.append('"');
                } else
                    string(b, v, prettify, indent + "  ");
                b.append(',');
                if (prettify)
                    b.append('\n');
            }
            if (!((Map) arg).isEmpty())
                b.deleteCharAt(b.length() - (prettify ? 2 : 1));
            if (prettify)
                b.append(indent);
            b.append('}');
            return;
        }

        if ((arg instanceof List)) {
            if (((List) arg).isEmpty()) {
                b.append("[]"); return;
            }
            b.append('[');
            if (prettify)
                b.append('\n');
            for (Object v : (List) arg) {
                if (prettify) {
                    b.append(indent);
                    b.append("  ");
                }
                if (v instanceof String || v instanceof Symbol || v instanceof JFunction) {
                    b.append('"');
                    string(b, v, prettify, indent + "  ");
                    b.append('"');
                } else
                    string(b, v, prettify, indent + "  ");
                b.append(',');
                if (prettify)
                    b.append('\n');
            }
            if (!((List) arg).isEmpty())
                b.deleteCharAt(b.length() - (prettify ? 2 : 1));
            if (prettify)
                b.append(indent);
            b.append(']');
            return;
        }

        // Throw error for unknown types
        throw new IllegalArgumentException("Only JSON types (values, Map, List) can be stringified. Unsupported type: "+arg.getClass().getName());
    }


    /**
     * Validate input data types.
     * This will make sure that all input data can be processed.
     * 
     * @param arg
     * @return
     */
    public static void validateInput(Object arg) {
        // if (arg == null)
        //   return null;
        
        if (arg==null || arg == Jsonata.NULL_VALUE) {
            return;
        }
        
        if (arg instanceof JFunction) {
            return;
        }
      
        if (arg instanceof Symbol) {
            return;
        }

        if (arg instanceof Double) {
            return;
        }

        if (arg instanceof Number || arg instanceof Boolean) {
            return;
        }
        
        if (arg instanceof String) {
            return;
        }

        if (arg instanceof Map) {
            for (Entry<String, Object> e : ((Map<String, Object>) arg).entrySet()) {
                validateInput(e.getKey());
                validateInput(e.getValue());
            }
            return;
        }

        if ((arg instanceof List)) {
            for (Object v : (List) arg) {
                validateInput(v);
            }
            return;
        }

        // Throw error for unknown types
        throw new IllegalArgumentException("Only JSON types (values, Map, List) are allowed as input. Unsupported type: "+
            arg.getClass().getCanonicalName());
    }

    /**
     * Create substring based on character number and length
     * @param {String} str - String to evaluate
     * @param {Integer} start - Character number to start substring
     * @param {Integer} [length] - Number of characters in substring
     * @returns {string|*} Substring
     */
    public static String substring(String str, Number _start, Number _length) {
        // undefined inputs always return undefined
        if (str == null) {
            return null;
        }

        Integer start = _start!=null ? _start.intValue() : null;
        Integer length = _length!=null ? _length.intValue() : null;

        // not used: var strArray = stringToArray(str);
        var strLength = str.length();

        if (strLength + start < 0) {
            start = 0;
        }

        if (length != null) {
            if (length <= 0) {
                return "";
            }
            return substr(str, start, length);
        }

        return substr(str, start, strLength);
    }


    /**
     * Source = Jsonata4Java JSONataUtils.substr
     * @param str
     * @param start  Location at which to begin extracting characters. If a negative
     *               number is given, it is treated as strLength - start where
     *               strLength is the length of the string. For example,
     *               str.substr(-3) is treated as str.substr(str.length - 3)
     * @param length The number of characters to extract. If this argument is null,
     *               all the characters from start to the end of the string are
     *               extracted.
     * @return A new string containing the extracted section of the given string. If
     *         length is 0 or a negative number, an empty string is returned.
     */
    static public String substr(String str, Integer start, Integer length) {

        // below has to convert start and length for emojis and unicode
        int origLen = str.length();

        String strData = Objects.requireNonNull(str).intern();
        int strLen = strData.codePointCount(0, strData.length());
        if (start >= strLen) {
            return "";
        }
        // If start is negative, substr() uses it as a character index from the
        // end of the string; the index of the last character is -1.
        start = strData.offsetByCodePoints(0, start >= 0 ? start : ((strLen + start) < 0 ? 0 : strLen + start));
        if (start < 0) {
            start = 0;
        } // If start is negative and abs(start) is larger than the length of the
        // string, substr() uses 0 as the start index.
        // If length is omitted, substr() extracts characters to the end of the
        // string.
        if (length == null) {
            length = strData.length();
        } else if (length < 0) {
            // If length is 0 or negative, substr() returns an empty string.
            return "";
        } else if (length > strData.length()) {
            length = strData.length();
        }

        length = strData.offsetByCodePoints(0, length);

        if (start >= 0) {
            // If start is positive and is greater than or equal to the length of
            // the string, substr() returns an empty string.
            if (start >= origLen) {
                return "";
            }
        }

        // collect length characters (unless it reaches the end of the string
        // first, in which case it will return fewer)
        int end = start + length;
        if (end > origLen) {
            end = origLen;
        }

        return strData.substring(start, end);
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

        if (chars==null)
            return str;

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

        return str.codePointCount(0, str.length());
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

        if (str.isEmpty()) {
            return "";
        }
        
        // normalize whitespace
        var result = str.replaceAll("[ \t\n\r]+", " ");
        if (result.charAt(0) == ' ') {
            // strip leading space
            result = result.substring(1);
        }
        
        if (result.isEmpty()) {
            return "";
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
    public static String pad(String str, Number _width, String _char) {
        // undefined inputs always return undefined
        if (str == null) {
            return null;
        }

        if (_char == null || _char.isEmpty()) {
            _char = " ";
        }

        String result;
        int width = _width.intValue();

        if (width < 0) {
            result = leftPad(str, -width, _char);
        } else {
            result = rightPad(str, width, _char);
        }
        return result;
    }

    // Source: Jsonata4Java PadFunction
    public static String leftPad(final String str, final int size, String padStr) {
        if (str == null) {
            return null;
        }
        if (padStr == null) {
            padStr = " ";
        }

        String strData = Objects.requireNonNull(str).intern();
        int strLen = strData.codePointCount(0, strData.length());

        String padData = Objects.requireNonNull(padStr).intern();
        int padLen = padData.codePointCount(0, padData.length());

        if (padLen == 0) {
            padStr = " ";
        }
        final int pads = size - strLen;
        if (pads <= 0) {
            return str;
        }
        String padding = "";
        for (int i = 0; i < pads + 1; i++) {
            padding += padStr;
        }
        return substr(padding, 0, pads).concat(str);
    }

    // Source: Jsonata4Java PadFunction
    public static String rightPad(final String str, final int size, String padStr) {
        if (str == null) {
            return null;
        }
        if (padStr == null) {
            padStr = " ";
        }

        String strData = Objects.requireNonNull(str).intern();
        int strLen = strData.codePointCount(0, strData.length());

        String padData = Objects.requireNonNull(padStr).intern();
        int padLen = padData.codePointCount(0, padData.length());

        if (padLen == 0) {
            padStr = " ";
        }
        final int pads = size - strLen;
        if (pads <= 0) {
            return str;
        }
        String padding = "";
        for (int i = 0; i < pads + 1; i++) {
            padding += padStr;
        }
        return str.concat(substr(padding, 0, pads));
    }

    static class RegexpMatch {
        String match;
        int index;
        List<String> groups;
        @Override public String toString() {
            return "regexpMatch "+match+" idx="+index+" groups="+groups;
        }
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

            //System.out.println("grc="+m.groupCount()+" "+m.group(1));

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
            //if (dbg) System.out.println("match = "+matches);
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
     * @returns {Array} The array of match objects
     */
    public static List<Map> match(String str, Pattern regex, Integer limit) {
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

    static String safeReplacement(String in) {
        // In JSONata and in Java the $ in the replacement test usually starts the insertion of a capturing group
        // In order to replace a simple $ in Java you have to escape the $ with "\$"
        // in JSONata you do this with a '$$'
        // "\$" followed any character besides '<' and and digit into $ + this character  
        return in.replaceAll("\\$\\$", "\\\\\\$")
            .replaceAll("([^\\\\]|^)\\$([^0-9^<])", "$1\\\\\\$$2")
            .replaceAll("\\$$", "\\\\\\$"); // allow $ at end
    }

    /**
     * Safe replaceAll
     * 
     * In Java, non-existing groups cause an exception.
     * Ignore these non-existing groups (replace with "")
     * 
     * @param s
     * @param pattern
     * @param replacement
     * @return
     */
    static String safeReplaceAll(String s, Pattern pattern, Object _replacement) {

        if (!(_replacement instanceof String))
            return safeReplaceAllFn(s, pattern, _replacement);

        String replacement = (String)_replacement;

        replacement = safeReplacement(replacement);
        Matcher m = pattern.matcher(s);
        String r = null;
        for (int i=0; i<10; i++) {
            try {
                r = m.replaceAll(replacement);
                break;
            } catch (IndexOutOfBoundsException e) {
                String msg = e.getMessage();

                // Message we understand needs to be:
                // No group X
                if (!msg.contains("No group")) throw e;

                // Adjust replacement to remove the non-existing group
                String g = "" + msg.charAt(msg.length()-1);

                replacement = replacement.replace("$"+g, "");
            }
        }
        return r;
    }

    /**
     * Converts Java MatchResult to the Jsonata object format
     * @param mr
     * @return
     */
    static Map toJsonataMatch(MatchResult mr) {
        Map obj = new LinkedHashMap<>();
        obj.put("match", mr.group());

        List groups = new ArrayList<>();
        for (int i=0; i<=mr.groupCount(); i++)
            groups.add(mr.group(i));

        obj.put("groups", groups);

        return obj;
    }

    /**
     * Regexp Replace with replacer function
     * @param s
     * @param pattern
     * @param fn
     * @return
     */
    static String safeReplaceAllFn(String s, Pattern pattern, Object fn) {
        Matcher m = pattern.matcher(s);
        String r = null;
        r = m.replaceAll(t -> { try {
            Object res = funcApply(fn, List.of(toJsonataMatch(t)));
            if (res instanceof String)
                return (String)res;
            else
                return null;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
        });
        return r;
    }

    /**
     * Safe replaceFirst
     * 
     * @param s
     * @param pattern
     * @param replacement
     * @return
     */
    static String safeReplaceFirst(String s, Pattern pattern, String replacement) {
        replacement = safeReplacement(replacement);
        Matcher m = pattern.matcher(s);
        String r = null;
        for (int i=0; i<10; i++) {
            try {
                r = m.replaceFirst(replacement);
                break;
            } catch (IndexOutOfBoundsException e) {
                String msg = e.getMessage();

                // Message we understand needs to be:
                // No group X
                if (!msg.contains("No group")) throw e;

                // Adjust replacement to remove the non-existing group
                String g = "" + msg.charAt(msg.length()-1);

                replacement = replacement.replace("$"+g, "");
            }
        }
        return r;
    }

    public static String replace(String str, Object pattern, Object replacement, Integer limit) {
        if (str == null) {
            return null;
        }
        if (pattern instanceof String)
          if (((String)pattern).isEmpty())
            throw new JException("Second argument of replace function cannot be an empty string", 0);
        if (limit == null) {
          if (pattern instanceof String) {
              return str.replace((String)pattern, (String)replacement);
          } else {
              return safeReplaceAll(str, (Pattern)pattern, replacement);
          }
        } else {
          
          if (limit<0)
            throw new JException("Fourth argument of replace function must evaluate to a positive number", 0);
          
          for (int i=0; i<limit; i++)
            if (pattern instanceof String) {
                str = str.replaceFirst((String)pattern, (String)replacement);
            } else {
                str = safeReplaceFirst(str, (Pattern)pattern, (String)replacement);
            }
          return str;
        }
    }



    /**
     * Base64 encode a string
     * @param {String} str - string
     * @returns {String} Base 64 encoding of the binary data
     */
    public static String base64encode(String str) {
        // undefined inputs always return undefined
        if (str == null) {
            return null;
        }
        try {
            return Base64.getEncoder().encodeToString(str.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Base64 decode a string
     * @param {String} str - string
     * @returns {String} Base 64 encoding of the binary data
     */
    public static String base64decode(String str) {
        // undefined inputs always return undefined
        if (str == null) {
            return null;
        }
        try {
            return new String(Base64.getDecoder().decode(str), "utf-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Encode a string into a component for a url
     * @param {String} str - String to encode
     * @returns {string} Encoded string
     */
    public static String encodeUrlComponent(String str) {
        // undefined inputs always return undefined
        if (str == null) {
            return null;
        }

        Utils.checkUrl(str);
        
        return URLEncoder.encode(str, StandardCharsets.UTF_8)
                            .replaceAll("\\+", "%20")
                            .replaceAll("\\%21", "!")
                            .replaceAll("\\%27", "'")
                            .replaceAll("\\%28", "(")
                            .replaceAll("\\%29", ")")
                            .replaceAll("\\%7E", "~");
    }

    /**
     * Encode a string into a url
     * @param {String} str - String to encode
     * @returns {string} Encoded string
     */
    public static String encodeUrl(String str) {
        // undefined inputs always return undefined
        if (str == null) {
            return null;
        }

        Utils.checkUrl(str);
        
        try {
          // only encode query part: https://docs.jsonata.org/string-functions#encodeurl
          URL url = new URL(str);
          String query = url.getQuery();
          if (query != null) {
              int offset = str.indexOf(query);
              String strResult = str.substring(0, offset);
              return strResult + encodeURI(query);
          }
        } catch (Exception e) {
          // ignore and return default
        }
        return  URLEncoder.encode(str, StandardCharsets.UTF_8);
    }

    static String encodeURI(String uri) {
      String result = null;
      if (uri != null) {
          try {
              // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURI
              // Not encoded: A-Z a-z 0-9 ; , / ? : @ & = + $ - _ . ! ~ * ' ( ) #
              result = URLEncoder.encode(uri, "UTF-8").replaceAll("\\+", "%20")
                  .replaceAll("%20", " ").replaceAll("\\%21", "!")
                  .replaceAll("\\%23", "#").replaceAll("\\%24", "$")
                  .replaceAll("\\%26", "&").replaceAll("\\%27", "'")
                  .replaceAll("\\%28", "(").replaceAll("\\%29", ")")
                  .replaceAll("\\%2A", "*").replaceAll("\\%2B", "+")
                  .replaceAll("\\%2C", ",").replaceAll("\\%2D", "-")
                  .replaceAll("\\%2E", ".").replaceAll("\\%2F", "/")
                  .replaceAll("\\%3A", ":").replaceAll("\\%3B", ";")
                  .replaceAll("\\%3D", "=").replaceAll("\\%3F", "?")
                  .replaceAll("\\%40", "@").replaceAll("\\%5F", "_")
                  .replaceAll("\\%7E", "~");
          } catch (UnsupportedEncodingException e) {
              e.printStackTrace();
          }
      }
      return result;
  }

    /**
     * Decode a string from a component for a url
     * @param {String} str - String to decode
     * @returns {string} Decoded string
     */
    public static String decodeUrlComponent(String str) {
        // undefined inputs always return undefined
        if (str == null) {
            return null;
        }

        return URLDecoder.decode(str, StandardCharsets.UTF_8);
    }

    /**
     * Decode a string from a url
     * @param {String} str - String to decode
     * @returns {string} Decoded string
     */
    public static String decodeUrl(String str) {
        // undefined inputs always return undefined
        if (str == null) {
            return null;
        }

        return URLDecoder.decode(str, StandardCharsets.UTF_8);
    }

    public static List<String> split(String str, Object pattern, Number limit) {
        if (str==null )
            return null;

        if (limit!=null && limit.intValue()<0)
            throw new JException("D3020", -1, str);

        List<String> result = new ArrayList<>();
        if (limit!=null && limit.intValue()==0)
            return result;

        if (pattern instanceof String) {
            String sep = (String)pattern;
            if (sep.isEmpty()) {
                // $split("str", ""): Split string into characters
                int l = limit!=null ? limit.intValue() : Integer.MAX_VALUE;
                for (int i=0; i<str.length() && i<l; i++) {
                    result.add( ""+str.charAt(i) );
                }
            } else {
                // Quote separator string + preserve trailing empty strings (-1)
                result = Arrays.asList( str.split( Pattern.quote(sep), -1) );
            }
        } else {
            result = Arrays.asList( ((Pattern)pattern).split(str, -1) );
        }
        if (limit!=null && limit.intValue()<result.size()) {
            result = result.subList(0, limit.intValue());
        }
        return result;
    }

    /**
     * Formats a number into a decimal string representation using XPath 3.1 F&O fn:format-number spec
     * @param {number} value - number to format
     * @param {String} picture - picture string definition
     * @param {Object} [options] - override locale defaults
     * @returns {String} The formatted string
     */
    public static String formatNumber(Number value, String picture, Map options) {
        // undefined inputs always return undefined
        if (value == null) {
            return null;
        }
        
        if (picture != null) {
          if (picture.contains(",,"))
            throw new RuntimeException("The sub-picture must not contain two adjacent instances of the 'grouping-separator' character");
          if (picture.indexOf('%') >= 0)
            if (picture.indexOf('e') >= 0)
                throw new RuntimeException("A sub-picture that contains a 'percent' or 'per-mille' character must not contain a character treated as an 'exponent-separator");
        }
        
        DecimalFormatSymbols symbols = options==null ? new DecimalFormatSymbols(Locale.US) :
            processOptionsArg(options);

        // Create the formatter and format the number
        DecimalFormat formatter = new DecimalFormat();
        formatter.setDecimalFormatSymbols(symbols);
        String fixedPicture = picture; //picture.replaceAll("9", "0");
        for (char c='1'; c<='9'; c++)
            fixedPicture = fixedPicture.replace(c, '0');

        boolean littleE = false;
        if (fixedPicture.contains("e")) {
            fixedPicture = fixedPicture.replace("e", "E");
            littleE = true;
        }
        //System.out.println("picture "+fixedPicture);
        formatter.applyLocalizedPattern(fixedPicture);
        String result = formatter.format(value);

        if (littleE)
            result = result.replace("E", "e");


        return result;
    }

    // From JSONata4Java FormatNumberFunction
    private static DecimalFormatSymbols processOptionsArg(Map argOptions) {
        // Create the variable return
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US); // (DecimalFormatSymbols) Constants.DEFAULT_DECIMAL_FORMAT_SYMBOLS.clone();

        // Iterate over the formatting character overrides
        Iterator<String> fieldNames = argOptions.keySet().iterator();
        while (fieldNames.hasNext()) {
            var fieldName = fieldNames.next();
            var valueNode = (String)argOptions.get(fieldName);
            // String value = getFormattingCharacter(valueNode);
            switch (fieldName) {
                case Constants.SYMBOL_DECIMAL_SEPARATOR: {
                    String value = getFormattingCharacter(valueNode, Constants.SYMBOL_DECIMAL_SEPARATOR, true);
                    symbols.setDecimalSeparator(value.charAt(0));
                    break;
                }

                case Constants.SYMBOL_GROUPING_SEPARATOR: {
                    String value = getFormattingCharacter(valueNode, Constants.SYMBOL_GROUPING_SEPARATOR, true);
                    symbols.setGroupingSeparator(value.charAt(0));
                    break;
                }

                case Constants.SYMBOL_INFINITY: {
                    String value = getFormattingCharacter(valueNode, Constants.SYMBOL_INFINITY, false);
                    symbols.setInfinity(value);
                    break;
                }

                case Constants.SYMBOL_MINUS_SIGN: {
                    String value = getFormattingCharacter(valueNode, Constants.SYMBOL_MINUS_SIGN, true);
                    symbols.setMinusSign(value.charAt(0));
                    break;
                }

                case Constants.SYMBOL_NAN: {
                    String value = getFormattingCharacter(valueNode, Constants.SYMBOL_NAN, false);
                    symbols.setNaN(value);
                    break;
                }

                case Constants.SYMBOL_PERCENT: {
                    String value = getFormattingCharacter(valueNode, Constants.SYMBOL_PERCENT, true);
                    symbols.setPercent(value.charAt(0));
                    break;
                }

                case Constants.SYMBOL_PER_MILLE: {
                    String value = getFormattingCharacter(valueNode, Constants.SYMBOL_PER_MILLE, false);
                    symbols.setPerMill(value.charAt(0));
                    break;
                }

                case Constants.SYMBOL_ZERO_DIGIT: {
                    String value = getFormattingCharacter(valueNode, Constants.SYMBOL_ZERO_DIGIT, true);
                    symbols.setZeroDigit(value.charAt(0));
                    break;
                }

                case Constants.SYMBOL_DIGIT: {
                    String value = getFormattingCharacter(valueNode, Constants.SYMBOL_DIGIT, true);
                    symbols.setDigit(value.charAt(0));
                    break;
                }

                case Constants.SYMBOL_PATTERN_SEPARATOR: {
                    String value = getFormattingCharacter(valueNode, Constants.SYMBOL_PATTERN_SEPARATOR, true);
                    symbols.setPatternSeparator(value.charAt(0));
                    break;
                }

                default: {
                    //final String msg = String.format(Constants.ERR_MSG_INVALID_OPTIONS_UNKNOWN_PROPERTY,
                    //    Constants.FUNCTION_FORMAT_NUMBER, fieldName);
                    throw new RuntimeException("Error parsing formatNumber format string");
                }
            } // SWITCH
        } // WHILE

        return symbols;
    }

    // From JSONata4Java FormatNumberFunction
    private static String getFormattingCharacter(String value, String propertyName, boolean isChar) {
        // Create the variable to return
        String formattingChar = null;

        // Make sure that we have a valid node and that its content is textual
        //if (valueNode != null && valueNode.isTextual()) {
            // Read the value
            //String value = valueNode.textValue();
            if (value != null && !value.isEmpty()) {

                // If the target property requires a single char, check the length
                if (isChar) {
                    if (value.length() == 1) {
                        formattingChar = value;
                    } else {
                        //final String msg = String.format(Constants.ERR_MSG_INVALID_OPTIONS_SINGLE_CHAR,
                        //    Constants.FUNCTION_FORMAT_NUMBER, propertyName);
                        throw new RuntimeException();
                    }
                } else {
                    formattingChar = value;
                }
            } else {
                final String msgTemplate;
                if (isChar) {
                    msgTemplate = Constants.ERR_MSG_INVALID_OPTIONS_SINGLE_CHAR;
                } else {
                    msgTemplate = Constants.ERR_MSG_INVALID_OPTIONS_STRING;
                }
                //final String msg = String.format(msgTemplate, Constants.FUNCTION_FORMAT_NUMBER, propertyName);
                throw new RuntimeException(msgTemplate);
            }
        //} 
        
        return formattingChar;
    }

    /**
     * Converts a number to a string using a specified number base
     * @param {number} value - the number to convert
     * @param {number} [radix] - the number base; must be between 2 and 36. Defaults to 10
     * @returns {string} - the converted string
     */
    public static String formatBase(Number value, Number _radix) {
        // undefined inputs always return undefined
        if (value == null) {
            return null;
        }

        value = round(value, 0);

        int radix;
        if (_radix == null) {
            radix = 10;
        } else {
            radix = _radix.intValue();
        }

        if (radix < 2 || radix > 36) {
            throw new JException("D3100",
                //stack: (new Error()).stack,
                radix
            );

        }

        var result = Long.toString(value.longValue(), radix);

        return result;
    }

    /**
     * Cast argument to number
     * @param {Object} arg - Argument
     * @throws NumberFormatException
     * @returns {Number} numeric value of argument
     */
    public static Number number(Object arg) throws NumberFormatException, JException {
        Number result = null;

        // undefined inputs always return undefined
        if (arg == null) {
            return null;
        }

        if (arg==Jsonata.NULL_VALUE)
            throw new JException("T0410", -1);

        if (arg instanceof Number)
            result = (Number)arg;
        else if (arg instanceof String) {
            String s = (String)arg;
            if (s.startsWith("0x"))
                result = Long.parseLong(s.substring(2), 16);
            else if (s.startsWith("0B"))
                result = Long.parseLong(s.substring(2), 2);
            else if (s.startsWith("0O"))
                result = Long.parseLong(s.substring(2), 8);
            else
                result = Double.valueOf((String)arg);
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
            Math.abs(arg.doubleValue()) : Math.abs(arg.longValue());
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

        BigDecimal b = new BigDecimal(arg+"");
        if (precision==null)
            precision = 0;
        b = b.setScale(precision.intValue(), RoundingMode.HALF_EVEN);
        
        return b.doubleValue();
    }

    /**
     * Square root of number
     * @param {Number} arg - Argument
     * @returns {Number} square root
     */
    public static Number sqrt(Number arg) {

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

        return Math.sqrt(arg.doubleValue());
    }

    /**
     * Raises number to the power of the second number
     * @param {Number} arg - the base
     * @param {Number} exp - the exponent
     * @returns {Number} rounded integer
     */
    public static Number power(Number arg, Number exp) {

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
            return null; // Uli: Null would need to be handled as false anyway
        }

        var result = false;
        if (arg instanceof List) {
            List l = (List)arg;
            if (l.size() == 1) {
                result = toBoolean(l.get(0));
            } else if (l.size() > 1) {
                long truesLength = l.stream().filter(e -> Jsonata.boolize(e)).count();
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
        } else if (arg instanceof Map) {
            if (!((Map)arg).isEmpty())
                result = true;
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


    public static int getFunctionArity(Object func) {
        if (func instanceof JFunction) {
            return ((JFunction)func).signature.getMinNumberOfArgs();
        } else {
            // Lambda
            return ((Symbol)func).arguments.size();
        }
    }

    /**
     * Helper function to build the arguments to be supplied to the function arg of the
     * HOFs map, filter, each, sift and single
     * @param {function} func - the function to be invoked
     * @param {*} arg1 - the first (required) arg - the value
     * @param {*} arg2 - the second (optional) arg - the position (index or key)
     * @param {*} arg3 - the third (optional) arg - the whole structure (array or object)
     * @returns {*[]} the argument list
     */
    public static List hofFuncArgs(Object func, Object arg1, Object arg2, Object arg3) {
        List func_args = new ArrayList<>(); func_args.add(arg1); // the first arg (the value) is required
        // the other two are optional - only supply it if the function can take it
        var length = getFunctionArity(func);
        if (length >= 2) {
            func_args.add(arg2);
        }
        if (length >= 3) {
            func_args.add(arg3);
        }
        return func_args;
    }

    /**
     * Call helper for Java
     * 
     * @param func
     * @param funcArgs
     * @return
     * @throws Throwable
     */
    public static Object funcApply(Object func, List funcArgs) throws Throwable {
        Object res;
        if (isLambda(func))
            res = Jsonata.current.get().apply(func, funcArgs, null, Jsonata.current.get().environment);
        else
            res = ((JFunction)func).call(null, funcArgs);
        return res;
    }

    /**
     * Create a map from an array of arguments
     * @param {Array} [arr] - array to map over
     * @param {Function} func - function to apply
     * @returns {Array} Map array
     */
    public static List map(List arr, Object func) throws Throwable {

        // undefined inputs always return undefined
        if (arr == null) {
            return null;
        }

        List result = Utils.createSequence();
        // do the map - iterate over the arrays, and invoke func
        for (int i=0; i<arr.size(); i++) {
            Object arg = arr.get(i);
            List funcArgs = hofFuncArgs(func, arg, i, arr);

            Object res = funcApply(func, funcArgs);
            if (res!=null)
                result.add(res);
        }
        return result;
    }

    /**
     * Create a map from an array of arguments
     * @param {Array} [arr] - array to filter
     * @param {Function} func - predicate function
     * @returns {Array} Map array
     */
    public static List filter(List arr, Object func) throws Throwable {
        // undefined inputs always return undefined
        if (arr == null) {
            return null;
        }

        var result = Utils.createSequence();

        for (var i = 0; i < arr.size(); i++) {
            var entry = arr.get(i);
            var func_args = hofFuncArgs(func, entry, i, arr);
            // invoke func
            var res = funcApply(func, func_args);
            var booledValue = toBoolean(res);
            if (booledValue == null ? false : booledValue) {
                result.add(entry);
            }
        }

        return result;
    }

    /**
     * Given an array, find the single element matching a specified condition
     * Throws an exception if the number of matching elements is not exactly one
     * @param {Array} [arr] - array to filter
     * @param {Function} [func] - predicate function
     * @returns {*} Matching element
     */
    public static Object single(List arr, Object func) throws Throwable {
        // undefined inputs always return undefined
        if (arr == null) {
            return null;
        }

        var hasFoundMatch = false;
        Object result = null;

        for (var i = 0; i < arr.size(); i++) {
            var entry = arr.get(i);
            var positiveResult = true;
            if (func != null) {
                var func_args = hofFuncArgs(func, entry, i, arr);
                // invoke func
                var res = funcApply(func, func_args);
                var booledValue = toBoolean(res);
                positiveResult = booledValue == null ? false : booledValue;
            }
            if (positiveResult) {
                if(!hasFoundMatch) {
                    result = entry;
                    hasFoundMatch = true;
                } else {
                    throw new JException("D3138",
                        i
                    );
                }
            }
        }

        if(!hasFoundMatch) {
            throw new JException("D3139", -1
            );
        }

        return result;
    }

    /**
     * Convolves (zips) each value from a set of arrays
     * @param {Array} [args] - arrays to zip
     * @returns {Array} Zipped array
     */
    public static List zip(JList<List> args) {
        var result = new ArrayList<>();
        // length of the shortest array
        int length = Integer.MAX_VALUE;
        int nargs = 0;
        // nargs : the real size of args!=null
        while (nargs < args.size()) {
            if (args.get(nargs)==null) {
                length = 0;
                break;
            }

            length = Math.min(length, args.get(nargs).size());
            nargs++;
        }

        for (var i = 0; i < length; i++) {
            List tuple = new ArrayList<>();
            for (var k=0; k<nargs; k++)
                tuple.add( args.get(k).get(i) );
            result.add(tuple);
        }
        return result;
    }

    /**
     * Fold left function
     * @param {Array} sequence - Sequence
     * @param {Function} func - Function
     * @param {Object} init - Initial value
     * @returns {*} Result
     */
    public static Object foldLeft(List sequence, Object func, Object init) throws Throwable {
        // undefined inputs always return undefined
        if (sequence == null) {
            return null;
        }
        Object result = null;

        var arity = getFunctionArity(func);
        if (arity < 2) {
            throw new JException("D3050",
                1
            );
        }

        int index;
        if (init == null && sequence.size() > 0) {
            result = sequence.get(0);
            index = 1;
        } else {
            result = init;
            index = 0;
        }

        while (index < sequence.size()) {
            List args = new ArrayList<>(); args.add(result); args.add(sequence.get(index));
            if (arity >= 3) {
                args.add(index);
            }
            if (arity >= 4) {
                args.add(sequence);
            }
            result = funcApply(func, args);
            index++;
        }

        return result;
    }

    /**
     * Return keys for an object
     * @param {Object} arg - Object
     * @returns {Array} Array of keys
     */
    public static List keys(Object arg) {
        var result = Utils.createSequence();

        if (arg instanceof List) {
            Set keys = new LinkedHashSet();
            // merge the keys of all of the items in the array
            for (Object el : ((List)arg)) {
                keys.addAll( keys(el) );
            }

            result.addAll(keys);
        } else if (arg instanceof Map) {
            result.addAll( ((Map)arg).keySet() );
        }
        return result;
    }

    // here: append, lookup

    /**
     * Determines if the argument is undefined
     * @param {*} arg - argument
     * @returns {boolean} False if argument undefined, otherwise true
     */
    public static boolean exists(Object arg) {
        if (arg == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Splits an object into an array of object with one property each
     * @param {*} arg - the object to split
     * @returns {*} - the array
     */
    public static Object spread(Object arg) {
        Object result = Utils.createSequence();

        if (arg instanceof List) {
            // spread all of the items in the array
            for (Object item : ((List)arg))
                result = append(result, spread(item));
        } else if (arg instanceof Map) {
            for (Entry entry : ((Map<Object,Object>)arg).entrySet()) {
                var obj = new LinkedHashMap<>();
                obj.put(entry.getKey(), entry.getValue());
                ((List)result).add(obj);
            }
        } else {
            return arg; // result = arg;
        }
        return result;
    }

    /**
     * Merges an array of objects into a single object.  Duplicate properties are
     * overridden by entries later in the array
     * @param {*} arg - the objects to merge
     * @returns {*} - the object
     */
    public static Object merge(List arg) {
        // undefined inputs always return undefined
        if (arg == null) {
            return null;
        }

        var result = new LinkedHashMap<>();

        for (Object obj : arg) {
            for (Entry entry : ((Map<Object,Object>)obj).entrySet()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Reverses the order of items in an array
     * @param {Array} arr - the array to reverse
     * @returns {Array} - the reversed array
     */
    public static List reverse(List arr) {
        // undefined inputs always return undefined
        if (arr == null) {
            return null;
        }

        if (arr.size() <= 1) {
            return arr;
        }

        var result = new ArrayList<>(arr);
        Collections.reverse(result);
        return result;
    }

    /**
     *
     * @param {*} obj - the input object to iterate over
     * @param {*} func - the function to apply to each key/value pair
     * @throws Throwable
     * @returns {Array} - the resultant array
     */
    public static List each(Map obj, Object func) throws Throwable {
        if (obj==null) {
            return null;
        }

        var result = Utils.createSequence();

        for (var key : obj.keySet()) {
            var func_args = hofFuncArgs(func, obj.get(key), key, obj);
            // invoke func
            var val = funcApply(func, func_args);
            if(val != null) {
                result.add(val);
            }
        }

        return result;
    }

    /**
     *
     * @param {string} [message] - the message to attach to the error
     * @throws custom error with code 'D3137'
     */
    public static void error(String message) throws Throwable {
        throw new JException("D3137", -1,
            message!=null ? message : "$error() function evaluated");
    }

    /**
     *
     * @param {boolean} condition - the condition to evaluate
     * @param {string} [message] - the message to attach to the error
     * @throws custom error with code 'D3137'
     * @returns {undefined}
     */
    public static void assertFn(boolean condition, String message) throws Throwable {
        if(!condition) {
            throw new JException("D3141", -1, "$assert() statement failed");
//                message: message || "$assert() statement failed"
        }
    }

    /**
     *
     * @param {*} [value] - the input to which the type will be checked
     * @returns {string} - the type of the input
     */
    public static String type(Object value) {
        if (value == null) {
            return null;
        }

        if (value == Jsonata.NULL_VALUE) {
            return "null";
        }

        if (value instanceof Number) {
            return "number";
        }

        if (value instanceof String) {
            return "string";
        }

        if (value instanceof Boolean) {
            return "boolean";
        }

        if(value instanceof List) {
            return "array";
        }

        if(Utils.isFunction(value) || isLambda(value)) {
            return "function";
        }

        return "object";
    }

    /**
     * Implements the merge sort (stable) with optional comparator function
     *
     * @param {Array} arr - the array to sort
     * @param {*} comparator - comparator function
     * @returns {Array} - sorted array
     */
    public static List sort(List arr, Object comparator) {
        // undefined inputs always return undefined
        if (arr == null) {
            return null;
        }

        if (arr.size() <= 1) {
            return arr;
        }

        List result = new ArrayList<>(arr);

        if (comparator != null) {
            Comparator comp = new Comparator() {

                @Override
                public int compare(Object o1, Object o2) {
                    try {
                        boolean swap = (boolean) funcApply(comparator, Arrays.asList(o1, o2));
                        if (swap)
                          return 1;
                        else
                          return -1;
                    } catch (Throwable e) {
                        // TODO Auto-generated catch block
                        //e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
                
            };
            if (comparator instanceof Comparator)
              result.sort((Comparator)comparator);
            else
              result.sort((Comparator)comp);
        } else {
            result.sort(null);
        }

        return result;
    }

    /**
     * Randomly shuffles the contents of an array
     * @param {Array} arr - the input array
     * @returns {Array} the shuffled array
     */
    public static List shuffle(List arr) {
        // undefined inputs always return undefined
        if (arr == null) {
            return null;
        }

        if (arr.size() <= 1) {
            return arr;
        }

        List result = new ArrayList<>(arr);
        Collections.shuffle(result);
        return result;
    }

    /**
     * Returns the values that appear in a sequence, with duplicates eliminated.
     * @param {Array} arr - An array or sequence of values
     * @returns {Array} - sequence of distinct values
     */
    public static Object distinct(Object _arr) {
        // undefined inputs always return undefined
        if (_arr == null) {
            return null;
        }

        if(!(_arr instanceof List) || ((List)_arr).size() <= 1) {
            return _arr;
        }
        List arr = (List)_arr;

        var results = (arr instanceof JList/*sequence*/) ? Utils.createSequence() : new ArrayList<>();

        // Create distinct list of elements by adding all to a set,
        // and then adding the set to the result
        var set = new LinkedHashSet(arr.size());
        set.addAll(arr);
        results.addAll(set);

        return results;
    }

    /**
     * Applies a predicate function to each key/value pair in an object, and returns an object containing
     * only the key/value pairs that passed the predicate
     *
     * @param {object} arg - the object to be sifted
     * @param {object} func - the predicate function (lambda or native)
     * @throws Throwable
     * @returns {object} - sifted object
     */
    public static Object sift(Map<Object,Object> arg, Object func) throws Throwable {
        if (arg==null) {
            return null;
        }

        var result = new LinkedHashMap<>();

        for (var item : arg.keySet()) {
            var entry = arg.get(item);
            var func_args = hofFuncArgs(func, entry, item, arg);
            // invoke func
            var res = funcApply(func, func_args);
            if (Jsonata.boolize(res)) {
                result.put(item, entry);
            }
        }

        // empty objects should be changed to undefined
        if (result.isEmpty()) {
            result = null;
        }

        return result;
    }

    ///////
    ///////
    ///////
    ///////

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

        // if either argument is not an array, make it so
        if (!(arg1 instanceof List)) {
            arg1 = Utils.createSequence(arg1);
        }
        if (!(arg2 instanceof List)) {
            arg2 = new JList<>(Arrays.asList(arg2));
        }

        // Shortcut:
        if (((List)arg1).isEmpty() && (arg2 instanceof Utils.RangeList))
            return arg2;

        arg1 = new JList<>((List)arg1); // create a new copy!
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
            // Detect the case where the value is null:
            if (result==null && ((java.util.Map)input).containsKey(key))
                result = Jsonata.NULL_VALUE;
        }
        return result;
    }

    public static String test(String a, String b) {
        return a+b;
    }

    public static Method getFunction(Class clz, String name) {
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

    public static Object call(Class clz, Object instance, String name, List<Object> args) throws Throwable {
        return call(instance, getFunction(clz, name), args);
    }

    public static Object call(Object instance, Method m, List<Object> args) throws Throwable {
        Class<?>[] types = m.getParameterTypes();
        int nargs = m.getParameterTypes().length;

        List<Object> callArgs = new ArrayList<>(args);
        while (callArgs.size()<nargs) {
            // Add default arg null if not enough args were provided
            callArgs.add(null);
        }

        // Special handling of one arg if function requires list:
        // Wrap the single arg (if != null) in a list with one element
        if (nargs>0 && List.class.isAssignableFrom(types[0]) && !(callArgs.get(0) instanceof List)) {
            Object arg1 = callArgs.get(0);
            if (arg1!=null) {
                List wrap = new ArrayList<>(); wrap.add(arg1);
                callArgs.set(0, wrap);            
            //System.err.println("wrapped "+arg1+" as "+wrap);
            }
        }

        // If the function receives the args as JList:
        // i.e. a varargs fn like zip can use this
        if (nargs==1 && types[0]==JList.class) {
            JList allArgs = new JList(args);
            callArgs = List.of(allArgs);
        }
        
        try {
            Object res = m.invoke(null, callArgs.toArray());
            if (res instanceof Number)
                res = Utils.convertNumber((Number)res);
            return res;
        } catch (IllegalAccessException e) {
            throw new Exception("Access error calling function "+m.getName(), e);
        } catch (IllegalArgumentException e) {
            throw new Exception("Argument error calling function "+m.getName(), e);
        } catch (InvocationTargetException e) {
            //e.printStackTrace();
            throw e.getTargetException();
        }
    }


    //
    // DateTime
    //

    /**
     * Converts an ISO 8601 timestamp to milliseconds since the epoch
     *
     * @param {string} timestamp - the timestamp to be converted
     * @param {string} [picture] - the picture string defining the format of the timestamp (defaults to ISO 8601)
     * @throws ParseException 
     * @returns {Number} - milliseconds since the epoch
     */
    public static Long dateTimeToMillis(String timestamp, String picture) throws ParseException {
        // undefined inputs always return undefined
        if(timestamp == null) {
            return null;
        }

        if(picture == null) {
          if (/*StringUtils.*/isNumeric(timestamp)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.parse(timestamp).getTime();
          }
          try {
            int len = timestamp.length();
            if (len > 5)
              if (timestamp.charAt(len-5) == '+' || timestamp.charAt(len-5) == '-')
                if (Character.isDigit(timestamp.charAt(len-4)) && Character.isDigit(timestamp.charAt(len-3)) && Character.isDigit(timestamp.charAt(len-2)) && Character.isDigit(timestamp.charAt(len-1)) )
                  timestamp = timestamp.substring(0, len-2) + ':' + timestamp.substring(len-2, len);
            return OffsetDateTime.parse(timestamp).toInstant().toEpochMilli();
          }
          catch (RuntimeException e) {
            try {
              LocalDate ldt = LocalDate.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
              return ldt.atStartOfDay().atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();
            }
            catch (DateTimeParseException noZone) {
              // Implementations usually default to the local time zone when the date string is non-standard
              // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/parse#non-standard_date_strings
              return LocalDateTime.parse(timestamp).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
          }
        } else {
            return DateTimeUtils.parseDateTime(timestamp, picture);
        }
    }
    // Adapted from: org.apache.commons.lang3.StringUtils
    public static boolean isNumeric(final CharSequence cs) {
        if (cs == null || cs.length() == 0) {
            return false;
        }
        final int sz = cs.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isDigit(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts milliseconds since the epoch to an ISO 8601 timestamp
     * @param {Number} millis - milliseconds since the epoch to be converted
     * @param {string} [picture] - the picture string defining the format of the timestamp (defaults to ISO 8601)
     * @param {string} [timezone] - the timezone to format the timestamp in (defaults to UTC)
     * @returns {String} - the formatted timestamp
     */
    public static String dateTimeFromMillis(Number millis, String picture, String timezone) {
        // undefined inputs always return undefined
        if(millis == null) {
            return null;
        }

        return DateTimeUtils.formatDateTime(millis.longValue(), picture, timezone);
    }

    /**
     * Formats an integer as specified by the XPath fn:format-integer function
     * See https://www.w3.org/TR/xpath-functions-31/#func-format-integer
     * @param {number} value - the number to be formatted
     * @param {string} picture - the picture string that specifies the format
     * @returns {string} - the formatted number
     */
    public static String formatInteger(Number value, String picture) {
        if (value == null) {
            return null;
        }
        return DateTimeUtils.formatInteger(value.longValue(), picture);
    }

    /**
     * parse a string containing an integer as specified by the picture string
     * @param {string} value - the string to parse
     * @param {string} picture - the picture string
     * @throws ParseException
     * @returns {number} - the parsed number
     */
    public static Number parseInteger(String value, String picture) throws ParseException, JException {
        if (value == null) {
            return null;
        }

        // const formatSpec = analyseIntegerPicture(picture);
        // const matchSpec = generateRegex(formatSpec);
        // //const fullRegex = '^' + matchSpec.regex + '$';
        // //const matcher = new RegExp(fullRegex);
        // // TODO validate input based on the matcher regex
        // const result = matchSpec.parse(value);
        // return result;
        
        if (picture != null) {
          if (picture.equals("#"))
            throw new ParseException("Formatting or parsing an integer as a sequence starting with \"#\" is not supported by this implementation", 0);
          if (picture.endsWith(";o"))
            picture = picture.substring(0, picture.length()-2);
          if (picture.equals("a"))
            return DateTimeUtils.lettersToDecimal(value, 'a');
          if (picture.equals("A"))
            return DateTimeUtils.lettersToDecimal(value, 'A');
          if (picture.equals("i"))
            return DateTimeUtils.romanToDecimal(value.toUpperCase());
          if (picture.equals("I"))
            return DateTimeUtils.romanToDecimal(value);
          if (picture.equals("w"))
            return DateTimeUtils.wordsToLong(value);
          if (picture.equals("W") || picture.equals("wW") || picture.equals("Ww"))
            return DateTimeUtils.wordsToLong(value.toLowerCase());
          if (picture.indexOf(':') >= 0) {
            value = value.replace(':', ',');
            picture = picture.replace(':', ',');
          }
        }
        
        try {
            DecimalFormat formatter = picture != null ? new DecimalFormat(picture, new DecimalFormatSymbols(Locale.US)) : new DecimalFormat();
            return formatter.parse(value);
        } catch (IllegalArgumentException ill) {
            throw new ParseException("Formatting or parsing an integer as a sequence starting with \""+ picture +"\" is not supported by this implementation", 0);
        } catch (Exception ex) {
            // Ignore the exception, return null
            // System.err.println("Exception in parseInteger (returning null): " + ex);
            // ex.printStackTrace();
            return null;
        }
    }

    /**
     * Clones an object
     * @param {Object} arg - object to clone (deep copy)
     * @returns {*} - the cloned object
     */
    public static Object functionClone(Object arg) {
        // undefined inputs always return undefined
        if(arg == null) {
            return null;
        }

        Object res = Json.parseJson(string(arg, false));
        return res;
    }

    /**
     * parses and evaluates the supplied expression
     * @param {string} expr - expression to evaluate
     * @returns {*} - result of evaluating the expression
     */
    public static Object functionEval(String expr, Object focus) {
        // undefined inputs always return undefined
        if(expr == null) {
            return null;
        }
        Object input = Jsonata.current.get().input; // =  this.input;
        if(focus != null) {
            input = focus;
            // if the input is a JSON array, then wrap it in a singleton sequence so it gets treated as a single input
            if((input instanceof List) && !Utils.isSequence(input)) {
                input = Utils.createSequence(input);
                ((JList)input).outerWrapper = true;
            }
        }

        Jsonata ast;
        Jsonata.Frame env = Jsonata.current.get().environment;
        try {
            ast = jsonata(expr);
        } catch(Throwable err) {
            // error parsing the expression passed to $eval
            //populateMessage(err);
            throw new JException("D3120", -1
            );
        }
        Object result = null;
        try {
            result = Jsonata.current.get().evaluate(ast.ast, input, env);
        } catch(Throwable err) {
            // error evaluating the expression passed to $eval
            //populateMessage(err);
            throw new JException("D3121", -1
            );
        }

        return result;
    }

    //  environment.bind("now", defineFunction(function(picture, timezone) {
    //      return datetime.fromMillis(timestamp.getTime(), picture, timezone);
    //  }, "<s?s?:s>"));
    public static String now(String picture, String timezone) {
        long t = Jsonata.current.get().timestamp;
        return dateTimeFromMillis(t, picture, timezone);
    }

    //  environment.bind("millis", defineFunction(function() {
    //      return timestamp.getTime();
    //  }, "<:n>"));
    public static long millis() {
        long t = Jsonata.current.get().timestamp;
        return t;
    }
}
