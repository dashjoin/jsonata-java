package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;

import java.util.List;
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

  @Test
  public void testParserStateClear() {
    Jsonata jsonata = jsonata("items.[name, % .currency]");
    List<Parser.Symbol> ancestry1 = jsonata.parser.ancestry;
    Assertions.assertEquals(1, ancestry1.size());
    Jsonata jsonata2 = jsonata("items.[name, % .currency]");
    List<Parser.Symbol> ancestry2 = jsonata2.parser.ancestry;
    Assertions.assertEquals(1, ancestry2.size());
  }
}
