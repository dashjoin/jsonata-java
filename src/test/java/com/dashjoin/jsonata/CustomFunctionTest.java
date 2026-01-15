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

  @Test
  public void testEachEmptyArray() {
    var expression = Jsonata.jsonata("[] ~> $each(?, function($v) { $v })");
    Object evaluate = expression.evaluate("{}");
    Assertions.assertNull(evaluate);
  }

  @Test
  public void testEachArrayWithData() {
    var expression = Jsonata.jsonata("[123, 321] ~> $each(?, function($v) { $v })");
    Object evaluate = expression.evaluate("{}");
    Assertions.assertInstanceOf(List.class, evaluate);
    List<Integer> expected = List.of(123, 321);
    Assertions.assertEquals(expected, evaluate);
  }
}
