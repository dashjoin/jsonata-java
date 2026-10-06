package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.dashjoin.jsonata.Jsonata.Frame;
import com.dashjoin.jsonata.Parser.Symbol;

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
}
