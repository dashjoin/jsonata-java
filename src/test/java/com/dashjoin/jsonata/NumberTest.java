package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import static java.util.Map.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.dashjoin.jsonata.json.Json;

public class NumberTest {

  /**
   * this case fails, because the double value 1.0 is "untouched"
   */
  @Disabled
  @Test
  public void testDouble() {
    Jsonata expr1 = jsonata("x");
    var res = expr1.evaluate(of("x", 1.0));
    assertEquals(1, res);
  }
  
  /**
   * a computation is applied, and com.dashjoin.jsonata.Utils.convertNumber(Number) casts the double to int
   */
  @Test
  public void testDouble2() {
    Jsonata expr1 = jsonata("x+0");
    var res = expr1.evaluate(of("x", 1.0));
    assertEquals(1, res);
  }

  /**
   * here, the JSON parser immediately converts double 1.0 to int 1
   */
  @Test
  public void testDouble3() {
    Jsonata expr1 = jsonata("x");
    var res = expr1.evaluate(Json.parseJson("{\"x\":1.0}"));
    assertEquals(1, res);
  }
      
  /**
   * "clean" the input using com.dashjoin.jsonata.Utils.convertNumber(Number)
   */
  @Test
  public void testDouble4() {
    Jsonata expr1 = jsonata("x");
    var res = expr1.evaluate(of("x", Utils.convertNumber(1.0)));
    assertEquals(1, res);
  }
      
  /**
   * int 1 is converted to double when divided by 2
   */
  @Test
  public void testInt() {
    Jsonata expr1 = jsonata("$ / 2");
    var res = expr1.evaluate(1);
    assertEquals(0.5, res);
  }
  
  @Test
  public void testConst() {
    Jsonata expr1 = jsonata("1.0");
    var res = expr1.evaluate(null);
    assertEquals(1, res);
  }
}
