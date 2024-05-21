package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import static java.util.Arrays.asList;
import static java.util.Map.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ArrayTest {

  @Disabled
  @Test
  public void testArray() {
    Jsonata expr1 = jsonata("{'key': $append($.[{'x': 'y'}],$.[{'a': 'b'}])}");
    var res1 = expr1.evaluate(of("key", asList(of("x", "y"), of("a", "b"))));
    Jsonata expr2 = jsonata("{'key': $append($.[{'x': 'y'}],[{'a': 'b'}])}");
    var res2 = expr2.evaluate(of("key", asList(of("x", "y"), of("a", "b"))));
    assertEquals(res1, res2);
  }
}
