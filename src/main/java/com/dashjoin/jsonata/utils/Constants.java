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
}
