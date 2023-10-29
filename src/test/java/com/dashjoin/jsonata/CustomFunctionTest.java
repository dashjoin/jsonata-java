package com.dashjoin.jsonata;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.dashjoin.jsonata.Jsonata.JFunction;
import com.dashjoin.jsonata.Jsonata.JFunctionCallable;

public class CustomFunctionTest {

  @Test
  public void testSupplier() {
    var expression = Jsonata.jsonata("$greet()");
    expression.registerFunction("greet", () -> "Hello world");
    Assertions.assertEquals("Hello world", expression.evaluate(null));
  }

  @Test
  public void testUnary() {
    var expression = Jsonata.jsonata("$echo(123)");
    expression.registerFunction("echo", (x) -> x);
    Assertions.assertEquals(123, expression.evaluate(null));
  }

  @Test
  public void testBinary() {
    var expression = Jsonata.jsonata("$add(21, 21)");
    expression.registerFunction("add", (Integer a, Integer b) -> a + b);
    Assertions.assertEquals(42, expression.evaluate(null));
  }

  @Test
  public void testTernary() {
    var expression = Jsonata.jsonata("$abc(a,b,c)");
    expression.registerFunction("abc", new JFunction(new JFunctionCallable() {
      @SuppressWarnings("rawtypes")
      @Override
      public Object call(Object input, List args) throws Throwable {
        return (String)args.get(0) + (String)args.get(1) + (String)args.get(2);
      }
    }, "<sss:s>"));
    Assertions.assertEquals("abc", expression.evaluate(Map.of("a", "a", "b", "b", "c", "c")));
  }
}
