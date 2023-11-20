package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ThreadTest {

  @Test
  public void testReuse() {
    Jsonata expr = jsonata("a");
    Assertions.assertEquals(1, expr.evaluate(Map.of("a", 1)));
    Assertions.assertEquals(1, expr.evaluate(Map.of("a", 1)));
  }

  @Test
  public void testNow() throws InterruptedException {
    Jsonata now = jsonata("$now()");
    Object r1 = now.evaluate(null);
    Thread.sleep(42);
    Object r2 = now.evaluate(null);
    Assertions.assertNotEquals(r1, r2);
  }

  @Test
  public void testReuseWithVariable() throws InterruptedException, ExecutionException {
    Jsonata expr = jsonata("($x := a; $wait(a); $x)");
    expr.registerFunction("wait", (Integer a) -> {
      try {
        Thread.sleep(a);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      return null;
    });

    // start a thread that sets x=100 and waits 100 before returning x
    Future<?> outer =
        Executors.newSingleThreadExecutor().submit(() -> expr.evaluate(Map.of("a", 100)));

    // make sure outer thread is initialized and in $wait
    Thread.sleep(10);

    // this thread uses the same expr and terminates before thread is done
    Assertions.assertEquals(30, expr.evaluate(Map.of("a", 30)));

    // the outer thread is unaffected by the previous operations
    Assertions.assertEquals(100, outer.get());
  }
}
