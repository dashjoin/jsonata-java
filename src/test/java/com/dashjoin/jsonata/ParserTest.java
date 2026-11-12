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
  
  /**
   * The parser is cached per thread (Jsonata.getParser()). Errors collected in recover mode
   * must not leak into subsequent parses: otherwise a later, perfectly valid expression inherits
   * the old errors and evaluate() refuses to run it.
   * see https://github.com/dashjoin/jsonata-java/issues/138
   */
  @Test
  public void testStaleErrorsDoNotLeakIntoNextParse() {
    Parser parser = Jsonata.getParser();
    parser.recover = true;
    Jsonata broken;
    try {
      broken = jsonata("[1,2");
    } finally {
      parser.recover = false;
    }
    Assertions.assertEquals(1, broken.getErrors().size());

    Jsonata valid = jsonata("1+1");
    Assertions.assertNull(valid.getErrors());
    Assertions.assertEquals(2, valid.evaluate(null));

    // the errors of the first expression must not be modified by later parses
    Assertions.assertEquals(1, broken.getErrors().size());
  }
}
