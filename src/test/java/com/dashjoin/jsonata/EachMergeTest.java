package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.dashjoin.jsonata.Jsonata.JFunction;

/**
 * $each / $merge iterate arrays like JS "for (var key in obj)", i.e. with the indexes as string
 * keys. In JSONata, arrays only reach these functions where the signature is not validated (e.g.
 * partial application). The tests call them via $eachRaw / $mergeRaw, which have no signature.
 */
public class EachMergeTest {

  static Object eval(String expr, Object input) {
    var e = jsonata(expr);
    e.registerFunction("eachRaw",
        new JFunction((in, args) -> Functions.each(args.get(0), args.get(1)), null));
    e.registerFunction("mergeRaw",
        new JFunction((in, args) -> Functions.merge((List) args.get(0)), null));
    return e.evaluate(input);
  }

  @Test
  public void eachArrayKeys() {
    Assertions.assertEquals(List.of("0", "1"), eval("$eachRaw([5,6], function($v,$k) { $k })", null));
  }

  @Test
  public void eachArrayValues() {
    Assertions.assertEquals(List.of(5, 6), eval("$eachRaw([5,6], function($v) { $v })", null));
  }

  @Test
  public void eachArrayAllArgs() {
    Assertions.assertEquals(List.of(Map.of("v", 5, "k", "0", "o", List.of(5))),
        eval("[$eachRaw($, function($v,$k,$o) { {'v': $v, 'k': $k, 'o': $o} })]", List.of(5)));
  }

  @Test
  public void eachArraySkipsUndefined() {
    Assertions.assertEquals(List.of(6, 7), eval("$eachRaw([5,6,7], function($v) { $v > 5 ? $v })", null));
  }

  @Test
  public void eachEmptyArray() {
    Assertions.assertNull(eval("$eachRaw([], function($v) { $v })", null));
  }

  @Test
  public void eachUndefined() {
    Assertions.assertNull(eval("$eachRaw(nothing, function($v) { $v })", null));
  }

  @Test
  public void eachMap() {
    Assertions.assertEquals(List.of("a=1", "b=2"),
        eval("$eachRaw({'a': 1, 'b': 2}, function($v, $k) { $k & '=' & $v })", null));
  }

  @Test
  public void mergeArray() {
    Assertions.assertEquals(Map.of("0", 5, "1", 6), eval("$mergeRaw([[5,6]])", null));
  }

  @Test
  public void mergeArrayAndMap() {
    Assertions.assertEquals(Map.of("test", "data"),
        eval("$mergeRaw($)", List.of(List.of(), Map.of("test", "data"))));
  }

  @Test
  public void mergeMaps() {
    Assertions.assertEquals(Map.of("a", 1, "b", 3), eval("$merge([{'a':1,'b':2},{'b':3}])", null));
  }

  @Test
  public void eachMapExpression() {
    Assertions.assertEquals(List.of("a=1", "b=2"),
        jsonata("$each({'a': 1, 'b': 2}, function($v, $k) { $k & '=' & $v })").evaluate(null));
  }

  @Test
  public void eachArrayExpressionRejected() {
    // the signature <o-f?:a> still rejects arrays when called directly
    var ex = Assertions.assertThrows(JException.class,
        () -> jsonata("$each([5,6], function($v,$k) { $k })").evaluate(null));
    Assertions.assertEquals("T0410", ex.getError());
  }
}
