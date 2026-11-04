package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.PatternSyntaxException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * see https://docs.jsonata.org/string-functions#string
 */
public class StringTest {

  @Test
  public void stringTest() {
    Assertions.assertEquals("abc", jsonata("$string($)").evaluate("abc"));
    Assertions.assertEquals("100", jsonata("$string(100.0)").evaluate(null));
  }

  @Test
  public void stringExponentTest() {
    Assertions.assertEquals("100", jsonata("$string(x)").evaluate(Map.of("x", 100.0)));
    Assertions.assertEquals("1000000000000000000", jsonata("$string(x)").evaluate(Map.of("x", 1000000000000000000.0)));
    // Assertions.assertEquals("100000000000000000000", jsonata("$string(x)").evaluate(Map.of("x", 100000000000000000000.0)));
    Assertions.assertEquals("1e+21", jsonata("$string(x)").evaluate(Map.of("x", 1000000000000000000000.0)));
  }

  @Test
  public void booleanTest() {
    Assertions.assertEquals("true", jsonata("$string($)").evaluate(true));
  }

  @Test
  public void numberTest() {
    Assertions.assertEquals("5", jsonata("$string(5)").evaluate(null));
  }

  @Test
  public void arrayTest() {
    Assertions.assertEquals(Arrays.asList("1", "2", "3", "4", "5"),
        jsonata("[1..5].$string()").evaluate(null));
  }

  @Test
  public void mapTest() {
    Assertions.assertEquals("{}", jsonata("$string($)").evaluate(Map.of()));
  }

  @Test
  public void map2Test() {
    Assertions.assertEquals("{\"x\":1}", jsonata("$string($)").evaluate(Map.of("x", 1)));
  }

  @Test
  public void escapeTest() {
    Assertions.assertEquals("{\"a\":\"\\\"\"}",
        jsonata("$string($)").evaluate(Map.of("a", "" + '"')));
    Assertions.assertEquals("{\"a\":\"\\\\\"}",
        jsonata("$string($)").evaluate(Map.of("a", "" + '\\')));
    Assertions.assertEquals("{\"a\":\"\\t\"}",
        jsonata("$string($)").evaluate(Map.of("a", "" + '\t')));
    Assertions.assertEquals("{\"a\":\"\\n\"}",
        jsonata("$string($)").evaluate(Map.of("a", "" + '\n')));
    Assertions.assertEquals("{\"a\":\"</\"}",
        jsonata("$string($)").evaluate(Map.of("a", "</")));
  }

  /**
   * Additional $split tests
   */
  @Test
  public void splitTest() {
    Object res;

    // Splitting on an undefined value
    res = jsonata("$split(a, '-')").evaluate(Map.of());
    Assertions.assertNull(res);

    // Splitting on an undefined value, equivalent to above
    res = jsonata("a ~> $split('-')").evaluate(Map.of());
    Assertions.assertNull(res);

    // Splitting empty string with empty separator must return empty list
    res = jsonata("$split('', '')").evaluate(null);
    Assertions.assertEquals(Arrays.asList(), res);

    // Split characters with limit
    res = jsonata("$split('a1b2c3d4', '', 4)").evaluate(null);
    Assertions.assertEquals(Arrays.asList("a", "1", "b", "2"), res);

    // Check string is not treated as regexp
    res = jsonata("$split('this..is.a.test', '.')").evaluate(null);
    //System.out.println( Functions.string(res, false));
    Assertions.assertEquals(Arrays.asList("this","","is","a","test"), res);
    
    // Check trailing empty strings
    res = jsonata("$split('this..is.a.test...', '.')").evaluate(null);
    //System.out.println( Functions.string(res, false));
    Assertions.assertEquals(Arrays.asList("this","","is","a","test","","",""), res);
  
    // Check trailing empty strings
    res = jsonata("$split('this..is.a.test...', /\\./)").evaluate(null);
    Assertions.assertEquals(Arrays.asList("this","","is","a","test","","",""), res);

    // Check string is not treated as regexp, trailing empty strings, and limit
    res = jsonata("$split('this.*.*is.*a.*test.*.*.*.*.*.*', '.*', 8)").evaluate(null);
    Assertions.assertEquals(Arrays.asList("this","","is","a","test","","",""), res);

    // Escaped regexp, trailing empty strings, and limit
    res = jsonata("$split('this.*.*is.*a.*test.*.*.*.*.*.*', /\\.\\*/, 8)").evaluate(null);
    Assertions.assertEquals(Arrays.asList("this","","is","a","test","","",""), res);
  }

  @Test
  public void trimTest() {
    Assertions.assertEquals("", jsonata("$trim(\"\n\")").evaluate(null));
    Assertions.assertEquals("", jsonata("$trim(\" \")").evaluate(null));
    Assertions.assertEquals("", jsonata("$trim(\"\")").evaluate(null));
    Assertions.assertEquals(null, jsonata("$trim(notthere)").evaluate(null));
  }
  
  @Test
  public void evalTest() {
    Assertions.assertEquals("AAA", jsonata("(\n"
        + "  $data := {'Wert1': 'AAA', 'Wert2': 'BBB'};\n"
        + "  $eval('$data.Wert1')\n"
        + ")").evaluate(null));
  }

  @Test
  public void regexTest() {
    // check for IndexOutOfBoundsException
    jsonata("/endsWithSlash-i/i");
  }

  /**
   * https://github.com/dashjoin/jsonata-java/issues/55
   * regexes use Java syntax, so a literal { must be escaped
   */
  @Test
  public void replaceTest() {
    Assertions.assertThrows(PatternSyntaxException.class,
        () -> jsonata("$replace($, /{par}/, '')").evaluate("http://example.org/test{par}"));
    Assertions.assertEquals("http://example.org/test",
        jsonata("$replace($, /\\{par\\}/, '')").evaluate("http://example.org/test{par}"));
  }

  /**
   * https://github.com/dashjoin/jsonata-java/issues/123
   * non-BMP characters (surrogate pairs) must be counted as one code point
   */
  @Test
  public void substringNonBmpTest() {
    // length omitted or larger than the remaining code points
    Assertions.assertEquals("bc", jsonata("$substring('😀bc', 1)").evaluate(null));
    Assertions.assertEquals("bc", jsonata("$substring('𝒳bc', 1)").evaluate(null));
    Assertions.assertEquals("😀bc", jsonata("$substring('😀bc', 0, 10)").evaluate(null));
    Assertions.assertEquals("bc", jsonata("$substring('😀bc', -2)").evaluate(null));
    Assertions.assertEquals("😀", jsonata("$substring('a😀😀', -1)").evaluate(null));

    // length must be counted from start, not from the beginning of the string
    Assertions.assertEquals("bc", jsonata("$substring('😀bc', 1, 2)").evaluate(null));
    Assertions.assertEquals("a", jsonata("$substring('😀abc', 1, 1)").evaluate(null));
    Assertions.assertEquals("b😀", jsonata("$substring('ab😀', 1, 2)").evaluate(null));
    Assertions.assertEquals("b", jsonata("$substring('😀bc', -2, 1)").evaluate(null));
    Assertions.assertEquals("😀", jsonata("$substring('😀😀😀x', 2, 1)").evaluate(null));

    // $pad uses substr internally
    Assertions.assertEquals("a😀😀😀😀", jsonata("$pad('a', 5, '😀')").evaluate(null));
    Assertions.assertEquals("😀😀😀😀a", jsonata("$pad('a', -5, '😀')").evaluate(null));
    Assertions.assertEquals("a😀x😀", jsonata("$pad('a', 4, '😀x')").evaluate(null));
    Assertions.assertEquals("😀---", jsonata("$pad('😀', 4, '-')").evaluate(null));
  }

  @Test
  public void testFieldnameWithSpecialChar() {
    Jsonata expr = jsonata("$ ~> |$|{}|");
    Object o = Map.of("a\nb", "c\nd");
    Assertions.assertEquals(o, expr.evaluate(o));
  }
}
