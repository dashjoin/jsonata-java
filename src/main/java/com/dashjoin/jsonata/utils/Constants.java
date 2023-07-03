package com.dashjoin.jsonata.utils;

/**
 * Constants required by DateTimeUtils
 */
public class Constants {
    public static final String ERR_MSG_SEQUENCE_UNSUPPORTED = "Formatting or parsing an integer as a sequence starting with %s is not supported by this implementation";
    public static final String ERR_MSG_DIFF_DECIMAL_GROUP = "In a decimal digit pattern, all digits must be from the same decimal group";
    public static final String ERR_MSG_NO_CLOSING_BRACKET = "No matching closing bracket ']' in date/time picture string";
    public static final String ERR_MSG_UNKNOWN_COMPONENT_SPECIFIER = "Unknown component specifier %s in date/time picture string";
    public static final String ERR_MSG_INVALID_NAME_MODIFIER = "The 'name' modifier can only be applied to months and days in the date/time picture string, not %s";
    public static final String ERR_MSG_TIMEZONE_FORMAT = "The timezone integer format specifier cannot have more than four digits";
    public static final String ERR_MSG_MISSING_FORMAT = "The date/time picture string is missing specifiers required to parse the timestamp";
    public static final String ERR_MSG_INVALID_OPTIONS_SINGLE_CHAR = "Argument 3 of function %s is invalid. The value of the %s property must be a single character";
    public static final String ERR_MSG_INVALID_OPTIONS_STRING = "Argument 3 of function %s is invalid. The value of the %s property must be a string";

    /**
    * Collection of decimal format strings that defined by xsl:decimal-format.
    * 
    * <pre>
    *     &lt;!ELEMENT xsl:decimal-format EMPTY&gt;
    *     &lt;!ATTLIST xsl:decimal-format
    *       name %qname; #IMPLIED
    *       decimal-separator %char; "."
    *       grouping-separator %char; ","
    *       infinity CDATA "Infinity"
    *       minus-sign %char; "-"
    *       NaN CDATA "NaN"
    *       percent %char; "%"
    *       per-mille %char; "&#x2030;"
    *       zero-digit %char; "0"
    *       digit %char; "#"
    *       pattern-separator %char; ";"&GT;
    * </pre>
    * 
    * http://www.w3.org/TR/xslt#format-number} to explain format-number in XSLT
    *      Specification xsl.usage advanced
    */
    public static final String SYMBOL_DECIMAL_SEPARATOR = "decimal-separator";
    public static final String SYMBOL_GROUPING_SEPARATOR = "grouping-separator";
    public static final String SYMBOL_INFINITY = "infinity";
    public static final String SYMBOL_MINUS_SIGN = "minus-sign";
    public static final String SYMBOL_NAN = "NaN";
    public static final String SYMBOL_PERCENT = "percent";
    public static final String SYMBOL_PER_MILLE = "per-mille";
    public static final String SYMBOL_ZERO_DIGIT = "zero-digit";
    public static final String SYMBOL_DIGIT = "digit";
    public static final String SYMBOL_PATTERN_SEPARATOR = "pattern-separator";
}
