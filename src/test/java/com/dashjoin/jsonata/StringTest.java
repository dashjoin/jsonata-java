package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * see https://docs.jsonata.org/string-functions#string
 */
public class StringTest {

  @Test
  public void stringTest() {
    Assertions.assertEquals("abc", jsonata("$string($)").evaluate("abc"));
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
  }
}
