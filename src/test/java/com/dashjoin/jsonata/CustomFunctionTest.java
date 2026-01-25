package com.dashjoin.jsonata;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
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
  public void testEval() {
    var expression = Jsonata.jsonata("$eval('$greet()')");
    expression.registerFunction("greet", () -> "Hello world");
    Assertions.assertEquals("Hello world", expression.evaluate(null));
  }

  @Test
  public void testEvalWithParams() {
    var expression = Jsonata.jsonata("($eval('$greet()'))");
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
        return (String) args.get(0) + (String) args.get(1) + (String) args.get(2);
      }
    }, "<sss:s>"));
    Assertions.assertEquals("abc", expression.evaluate(Map.of("a", "a", "b", "b", "c", "c")));
  }

  @Test
  public void testFunctionWithInputData() {
    JFunction addWithFactor = new JFunction("addWithFactor", (input, args) -> {
      var inputMap = (Map<String, Object>)input;
      String factor = inputMap.get("factor").toString();
      int factorInt = Integer.parseInt(factor);
      return ((Integer) args.get(0) + (Integer) args.get(1)) * factorInt;
    }, null);

    var expression = Jsonata.jsonata("$addWithFactor(1, 2)");
    expression.registerFunction(addWithFactor.functionName, addWithFactor);
    Assertions.assertEquals(9, expression.evaluate(Map.of("factor", "3")));
  }

  @Test
  public void testFunctionWithInputData2() {
    JFunction addWithFactor = new JFunction("multiply", (input, args) -> {
      Object first = args.get(0);
      Object second;
      if (args.size() > 1) {
        second = args.get(1);
      } else {
        var inputMap = (Map<String, Object>)input;
        String factor = inputMap.get("factor").toString();
        second = Integer.parseInt(factor);
      }
      return (Integer) first * (Integer) second;
    }, null, 2);

    var expression = Jsonata.jsonata("$multiply(3) ~> $multiply(?, 2)");
    expression.registerFunction(addWithFactor.functionName, addWithFactor);
    Assertions.assertEquals(18, expression.evaluate(Map.of("factor", "3")));
  }

  @Test
  public void testCustomFunctionPipe() {
    JFunction add = new JFunction("add", (input, args) -> (Integer) args.get(0) + (Integer) args.get(1), null, 2);
    var expression = Jsonata.jsonata("$add(1, 2) ~> $add(?, 3)");
    expression.registerFunction(add.functionName, add);
    Assertions.assertEquals(6, expression.evaluate(null));
  }

  /**
   * Lambdas use no signature - in case of an error, a ClassCastException is thrown
   */
  @Test
  public void testLambdaSignatureError() {
    var expression = Jsonata.jsonata("$append(1, 2)");
    expression.registerFunction("append", (Integer a, Boolean b) -> "" + a + b);
    Assertions.assertThrowsExactly(ClassCastException.class, () -> expression.evaluate(null));
  }

  /**
   * provide signature: number, boolean => string
   */
  @Test
  public void testJFunctionSignatureError() {
    var expression = Jsonata.jsonata("$append(1, 2)");
    expression.registerFunction("append", new JFunction(new JFunctionCallable() {
      @Override
      public Object call(Object input, @SuppressWarnings("rawtypes") List args) throws Throwable {
        return "" + args.get(0) + args.get(1);
      }
    }, "<nb:s>"));
    JException ex =
        Assertions.assertThrowsExactly(JException.class, () -> expression.evaluate(null));
    Assertions.assertEquals("T0410", ex.getError());
    Assertions.assertEquals("append", ex.getExpected());
  }
}
