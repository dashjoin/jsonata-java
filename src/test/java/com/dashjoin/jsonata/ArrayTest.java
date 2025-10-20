package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import static java.util.Arrays.asList;
import static java.util.Map.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Arrays;
import java.util.List;
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

  @Test
  public void filterTest() {
    // Frame value not evaluated if used in array filter #45
    Jsonata expr = jsonata("($arr := [{'x':1}, {'x':2}];$arr[x=$number($$.variable.field)])");
    Assertions.assertNotNull(expr.evaluate(Map.of("variable", Map.of("field", "1"))));
  }

  @Test
  public void testIndex() {
    Jsonata expr = jsonata("($x:=['a','b']; $x#$i.$i)");
    Assertions.assertEquals(Arrays.asList(0, 1), expr.evaluate(1));
    Assertions.assertEquals(Arrays.asList(0, 1), expr.evaluate(null));
  }

  @Test
  public void testSort() {
    Jsonata expr = jsonata("$sort([{'x': 2}, {'x': 1}], function($l, $r){$l.x > $r.x})");
    Assertions.assertEquals(Arrays.asList(Map.of("x", 1), Map.of("x", 2)), expr.evaluate(null));
  }

  @Test
  public void testSortNull() {
    Jsonata expr = jsonata("$sort([{'x': 2}, {'x': 1}], function($l, $r){$l.y > $r.y})");
    Assertions.assertEquals(Arrays.asList(Map.of("x", 2), Map.of("x", 1)), expr.evaluate(null));
  }

  @Test
  public void testWildcard() {
    Jsonata expr = jsonata("*");
    Assertions.assertEquals(Map.of("x", 1), expr.evaluate(List.of(Map.of("x", 1))));
  }

  @Test
  public void testWildcardFilter() {
    Object value1 = Map.of("value", Map.of("Name", "Cell1", "Product", "Product1"));
    Object value2 = Map.of("value", Map.of("Name", "Cell2", "Product", "Product2"));
    Object data = List.of(value1, value2);

    // Expecting the first object in the array
    var expression = jsonata("*[value.Product = 'Product1']");
    Assertions.assertEquals(value1, expression.evaluate(data));

    var expression2 = jsonata("**[value.Product = 'Product1']");
    Assertions.assertEquals(value1, expression2.evaluate(data));
  }
}
