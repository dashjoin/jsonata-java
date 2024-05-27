package com.dashjoin.jsonata;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class DateTimeTest {

  @Disabled
  @Test
  public void testFormatInteger() {
    Jsonata expr = Jsonata.jsonata("$toMillis('2018th', '[Y0001;o]')");
    Assertions.assertEquals(1514764800000L, expr.evaluate(null));
  }
}
