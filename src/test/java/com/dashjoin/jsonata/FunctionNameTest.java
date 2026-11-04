package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.dashjoin.jsonata.Jsonata.Fn1;

/**
 * https://github.com/dashjoin/jsonata-java/issues/116
 *
 * T0410 signature errors must name the function the way jsonata-js does: by the name used at
 * the call site (registered name or variable name), and never by a Java (lambda) class name.
 */
public class FunctionNameTest {

  JException error(String expr, Object input) {
    Jsonata e = jsonata(expr);
    e.registerFunction("test", Jsonata.function("test", (Fn1<String, String>) (x -> x), "<s:s>"));
    JException ex = Assertions.assertThrows(JException.class, () -> e.evaluate(input));
    Assertions.assertEquals("T0410", ex.getError());
    Assertions.assertFalse(String.valueOf(ex.getMessage()).contains("$$Lambda"), ex.getMessage());
    return ex;
  }

  @Test
  public void registeredFunction() {
    // jsonata-js: token="test"
    Assertions.assertEquals("test", error("$test(1)", null).getExpected());
  }

  @Test
  public void registeredFunctionViaVariable() {
    // jsonata-js: token="f"
    Assertions.assertEquals("f", error("($f := $test; $f(1))", null).getExpected());
  }

  @Test
  public void transformViaVariable() {
    // jsonata-js: token="t"
    Assertions.assertEquals("t", error("($t := |$|{}|; $t(123))", null).getExpected());
  }

  @Test
  public void transformViaApply() {
    // jsonata-js: token=undefined
    Assertions.assertNull(error("$ ~> |$|{}|", 123).getExpected());
  }

  @Test
  public void transformCalledDirectly() {
    // jsonata-js: token=undefined
    Assertions.assertNull(error("(|$|{}|)(123)", null).getExpected());
  }
}
