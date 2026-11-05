package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.dashjoin.jsonata.Jsonata.JFunction;
import com.dashjoin.jsonata.Jsonata.JFunctionCallable;

/**
 * Partial function application, see issue #95
 */
public class PartialTest {

  // #79
  @Test
  public void eachEmptyArray() {
    Assertions.assertNull(jsonata("[] ~> $each(?, function($v) { $v })").evaluate(Map.of()));
  }

  // #86
  @Test
  public void mapAppend() {
    Assertions.assertEquals(Map.of("systems", "asd2"),
        jsonata("{'systems': 'asd2'}.$map($, $append(?))").evaluate(null));
  }

  // missing args are bound to undefined, not left unbound
  @Test
  public void missingArgsBound() {
    Assertions.assertEquals("ello", jsonata("$substring(?, 1)('hello')").evaluate(null));
  }

  // #83
  @Test
  public void customFunction() {
    var expression = jsonata("$add(1, 2) ~> $add(?, 3)");
    expression.registerFunction("add", new JFunction(new JFunctionCallable() {
      @SuppressWarnings("rawtypes")
      @Override
      public Object call(Object input, List args) throws Throwable {
        return (Integer) args.get(0) + (Integer) args.get(1);
      }
    }, "<nn:n>"));
    Assertions.assertEquals(6, expression.evaluate(null));
  }

  // JS iterates array indexes, signature is not checked for partially applied natives
  @Test
  public void eachArray() {
    Assertions.assertEquals(List.of("0", "1"),
        jsonata("[5,6] ~> $each(?, function($v,$k) { $k })").evaluate(null));
  }

  @Test
  public void mergeWithArray() {
    Assertions.assertEquals(Map.of("test", "data"),
        jsonata("$ ~> $merge(?)").evaluate(List.of(List.of(), Map.of("test", "data"))));
  }

  // signature still checked when called directly
  @Test
  public void eachArrayDirect() {
    Assertions.assertThrows(JException.class,
        () -> jsonata("$each([5,6], function($v,$k) { $k })").evaluate(null));
  }

  // forgot the leading $
  @Test
  public void missingDollar() {
    var ex = Assertions.assertThrows(JException.class, () -> jsonata("substring(?, 1)").evaluate(null));
    Assertions.assertEquals("T1007", ex.getError());
  }
}
