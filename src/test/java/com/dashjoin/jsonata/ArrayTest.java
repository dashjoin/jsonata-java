package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import static java.util.Arrays.asList;
import static java.util.Map.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ArrayTest {

  @Test
  public void testArray() {
    Jsonata expr1 = jsonata("{'key': $append($.[{'x': 'y'}],$.[{'a': 'b'}])}");
    var res1 = expr1.evaluate(of("key", asList(of("x", "y"), of("a", "b"))));
    Jsonata expr2 = jsonata("{'key': $append($.[{'x': 'y'}],[{'a': 'b'}])}");
    var res2 = expr2.evaluate(of("key", asList(of("x", "y"), of("a", "b"))));
    assertEquals(res1, res2);
  }
  
  @Disabled
  @Test
  public void filterTest() {
    // Frame value not evaluated if used in array filter #45
    Jsonata expr = jsonata("($arr := [{'x':1}, {'x':2}];$arr[x=$number(variable.field)])");
    Assertions.assertNotNull(expr.evaluate(Map.of("variable", Map.of("field", "1"))));
  }
}
