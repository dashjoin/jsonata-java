package com.dashjoin.jsonata;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DateTimeTest {
  @Test
  public void testFormatInteger() {
    Jsonata expr = Jsonata.jsonata("$toMillis('2018th', '[Y0001;o]')");
    Assertions.assertEquals(1514764800000L, expr.evaluate(null));
  }
  
  @Test
  public void testToMillis() {
    String noZoneTooPrecise = "2024-08-27T22:43:15.78133";
    Jsonata expr = Jsonata.jsonata("$fromMillis($toMillis($))");
    String timestamp = (String) expr.evaluate(noZoneTooPrecise);
    Assertions.assertTrue(timestamp.startsWith("2024-08-2"));
    Assertions.assertTrue(timestamp.endsWith(":43:15.781Z"));
  }
}
