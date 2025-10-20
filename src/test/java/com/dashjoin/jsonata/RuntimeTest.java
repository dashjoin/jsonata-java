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

  boolean entered = false;
  boolean exited = false;
  
  @Test
  public void testCallbacks() {
    var expr = jsonata("42");
    var frame = expr.createFrame();
    frame.bind("__evaluate_entry", new Jsonata.EntryCallback() {
      @Override
      public void callback(Symbol expr, Object input, Frame environment) {
        entered = true;
      }
    });
    frame.bind("__evaluate_exit", new Jsonata.ExitCallback() {
      @Override
      public void callback(Symbol expr, Object input, Frame environment, Object result) {
        exited = true;
      }
    });
    expr.evaluate(null, frame);
    Assertions.assertTrue(exited && entered);
  }
}
