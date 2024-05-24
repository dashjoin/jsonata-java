package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import static java.util.Arrays.asList;
import static java.util.Map.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class NumberTest {

  @Disabled
  @Test
  public void testDouble() {
    Jsonata expr1 = jsonata("x");
    var res = expr1.evaluate(of("x", 1.0));
    System.out.println(res);
    assertEquals(1, res);
  }
  
  @Test
  public void testConst() {
    Jsonata expr1 = jsonata("1.0");
    var res = expr1.evaluate(of("key", asList(of("x", "y"), of("a", "b"))));
    System.out.println(res);
    assertEquals(1, res);
  }
}
