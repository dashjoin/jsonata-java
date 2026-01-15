package com.dashjoin.jsonata;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExceptionTest {

  @Test
  public void testError() {
    Jsonata expr = Jsonata.jsonata("$error('message')");
    try {
      expr.evaluate(null);
      Assertions.fail();
    } catch (JException e) {
      Assertions.assertEquals("message", e.getMessage());
    }
  }

  @Test
  public void testDivZero() {
    Jsonata expr = Jsonata.jsonata("1 / 0");
    try {
      expr.evaluate(null);
      Assertions.fail();
    } catch (JException e) {
      Assertions.assertEquals("Number out of range: \"Infinity\"", e.getMessage());
    }
  }
  
  @Test
  public void testAssertCustomMessage() {
    try {
      Jsonata.jsonata("$assert(false, 'message')").evaluate(null);
      Assertions.fail();
    } catch (JException e) {
      Assertions.assertEquals("message", e.getMessage());
    }
  }
}
