package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParserTest {

  @Test
  public void testUnsupportedEscapeSequence() {
    Assertions.assertThrows(JException.class, ()->jsonata("$substring('input', '\\a"));
    Assertions.assertThrows(JException.class, ()->jsonata("$substring('input', '\\"));
    Assertions.assertEquals("", jsonata("$substring('\\\\', 1)").evaluate(null));
    
    Assertions.assertThrows(JException.class, ()->jsonata("$substring('\\u"));
    Assertions.assertEquals("", jsonata("$substring('\\uDDDD', 1)").evaluate(null));
  }
}
