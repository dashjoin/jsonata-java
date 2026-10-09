package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RuntimeTest {

  @Test
  public void testRuntimeBounds() {
    var expr = jsonata("("
        + "$a := function(){42};"
        + "$b := function(){$a()};"
        + "$c := function(){$b()};"
        + ")");
    var frame = expr.createFrame();
    
    frame.setRuntimeBounds(1000, 2);
    Assertions.assertThrows(JException.class, () -> expr.evaluate(null, frame));
    
    frame.setRuntimeBounds(1000, 3);
    expr.evaluate(null, frame);
  }

  /**
   * https://github.com/dashjoin/jsonata-java/issues/120
   */
  @Test
  public void testRuntimeBoundsArrayConstructorInLoop() {
    var expr = jsonata("$map([1..30], function($i) { [$i, $i] })");
    var frame = expr.createFrame();
    frame.setRuntimeBounds(1000, 10);
    expr.evaluate(null, frame);
  }

  @Test
  public void testRuntimeBoundsNestedArrayConstructorInLoop() {
    var expr = jsonata("$map([1..30], function($i) { [$i, [$i, $i], $i] })");
    var frame = expr.createFrame();
    frame.setRuntimeBounds(1000, 10);
    expr.evaluate(null, frame);
  }

  @Test
  public void testRuntimeBoundsObjectConstructorInLoop() {
    var expr = jsonata("$map([1..30], function($i) { {'a': $i, 'b': $i} })");
    var frame = expr.createFrame();
    frame.setRuntimeBounds(1000, 10);
    expr.evaluate(null, frame);
  }

  /**
   * restoring isParallelCall to false instead of its previous value would let depth go negative,
   * silently loosening the bound without ever overflowing
   */
  @Test
  public void testRuntimeBoundsDepthBalancedAfterConstructors() {
    var expr = jsonata("$map([1..30], function($i) { [$i, [$i, $i], {'a': $i, 'b': [$i, $i]}] })");
    var frame = expr.createFrame();
    var timebox = new Timebox(frame, 1000, 10);
    expr.evaluate(null, frame);
    Assertions.assertEquals(0, timebox.depth);
  }

  @Test
  public void testRuntimeBoundsRecursionWithArrayConstructor() {
    var expr = jsonata("($f := function($n) { $n > 0 ? [$n, $f($n - 1)] : [] }; $f(30))");
    var frame = expr.createFrame();
    frame.setRuntimeBounds(1000, 10);
    Assertions.assertThrows(JException.class, () -> expr.evaluate(null, frame));
  }

  boolean entered = false;
  boolean exited = false;
  
  @Test
  public void testCallbacks() {
    var expr = jsonata("42");
    var frame = expr.createFrame();
    frame.setEvaluateEntryCallback((ast, input, environment) -> {
      entered = true;
    });
    frame.setEvaluateExitCallback((ast, input, environment, result) -> {
      exited = true;
    });
    expr.evaluate(null, frame);
    Assertions.assertTrue(exited && entered);
  }

  @Test
  public void testGuardrails() {
    {
      var options = Jsonata.Options.builder()
        .stack(2)
        .build();
      var expr = jsonata("("
          + "$a := function(){42};"
          + "$b := function(){$a()};"
          + "$c := function(){$b()};"
          + "$a() )", options);

      Assertions.assertThrows(JException.class, () -> expr.evaluate(null));
    }

    {
      var options = Jsonata.Options.builder()
        .stack(3)
        .build();
      var expr = jsonata("("
          + "$a := function(){42};"
          + "$b := function(){$a()};"
          + "$c := function(){$b()};"
          + "$a() )", options);

      Assertions.assertEquals(42, expr.evaluate(null));
    }

  }

  @Test
  public void testGuardrails1a() {
    // stack overflow - infinite recursive function - non-tail call
    var options = Jsonata.Options.builder()
      .timeout(1000L)
      .stack(300)
      .build();
    var expr = jsonata("($inf := function($n){$n+$inf($n-1)};  $inf(5))", options);
    Assertions.assertThrows(JException.class, () -> expr.evaluate(null));
  }

  @Test
  public void testGuardrails1b() {
    // stack overflow - infinite recursive function - tail call (no stack guardrail)
    var options = Jsonata.Options.builder()
      .timeout(1000L)
      .build();
    var expr = jsonata("( $inf := function(){$inf()}; $inf())", options);
    Assertions.assertThrows(JException.class, () -> expr.evaluate(null));
  }

  @Test
  public void testGuardrails2() {
    // guardrails on Ackermann function
    var acker = "( $ack := function($m, $n) { $m = 0 ? $n + 1 : $n = 0 ? $ack($m - 1, 1) : $ack($m - 1, $ack($m, $n - 1)) }; ";

    {
      // should complete for small parameters
      var options = Jsonata.Options.builder()
        .timeout(1000L)
        .stack(300)
        .build();
      var expr = jsonata(acker + " $ack(3,4))", options);
      Assertions.assertEquals(125, expr.evaluate(null));
    }

    {
      // larger inputs cause stack overflow
      var options = Jsonata.Options.builder()
        .stack(500)
        .build();
      var expr = jsonata(acker + " $ack(4,4))", options);
      Assertions.assertThrows(JException.class, () -> expr.evaluate(null));
    }
  }

  @Test
  public void testGuardrails3() {
    // guardrails on sequence length
    var options = Jsonata.Options.builder()
      .sequence(1000)
      .build();
    var expr = jsonata("[1..1001]", options);
    Assertions.assertThrows(JException.class, () -> expr.evaluate(null));

    // prevents large intermediate sequences
    var expr2 = jsonata("[0..100].([0..100]) ~> $count()", options);
    Assertions.assertThrows(JException.class, () -> expr2.evaluate(null));
  }
}
