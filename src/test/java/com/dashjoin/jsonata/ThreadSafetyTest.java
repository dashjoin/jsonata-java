package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Thread safety and instance isolation tests for the Jsonata ThreadLocal fix.
 * Covers GitHub issues #90 and #93, plus high-throughput concurrent scenarios.
 */
public class ThreadSafetyTest {

    @Test
    public void testTwoInstancesSameThread_bindingsDontLeak() {
        Jsonata exprA = jsonata("$test");
        Jsonata.Frame env = exprA.createFrame();
        env.bind("test", "value_from_A");

        // Constructing exprB must NOT corrupt exprA's evaluation context
        Jsonata exprB = jsonata("$test");

        Object resultA = exprA.evaluate("", env);
        Object resultB = exprB.evaluate("");

        assertEquals("value_from_A", resultA, "exprA should see its own binding");
        assertNull(resultB, "exprB should NOT see exprA's binding");
    }

    @Test
    public void testTwoInstancesSameThread_differentExpressions() {
        Jsonata exprA = jsonata("a");
        Jsonata exprB = jsonata("b");

        assertEquals(1, exprA.evaluate(Map.of("a", 1, "b", 99)));
        assertEquals(2, exprB.evaluate(Map.of("a", 99, "b", 2)));
        // Re-evaluate exprA to confirm it still works correctly
        assertEquals(3, exprA.evaluate(Map.of("a", 3, "b", 99)));
    }

    @Test
    public void testManyInstancesSameThread_interleaved() {
        Jsonata add = jsonata("a + b");
        Jsonata mul = jsonata("a * b");
        Jsonata evalExpr = jsonata("$eval('a')");

        for (int i = 1; i <= 500; i++) {
            assertEquals(i + 1, add.evaluate(Map.of("a", i, "b", 1)),
                    "add failed at iteration " + i);
            assertEquals(i * 2, mul.evaluate(Map.of("a", i, "b", 2)),
                    "mul failed at iteration " + i);
            assertEquals(i, evalExpr.evaluate(Map.of("a", i)),
                    "eval failed at iteration " + i);
        }
    }

    @Test
    public void testEvalDeepContext() {
        // This is the exact reproduction from issue #90
        Jsonata expr = jsonata("$eval($.funcs.func)");
        Object input = Map.of(
                "funcs", Map.of("func", "$.a + $.b"),
                "a", 3,
                "b", 4
        );
        assertEquals(7, expr.evaluate(input));
    }

    @Test
    public void testEvalWithSimplePath() {
        Jsonata expr = jsonata("$eval('a')");
        assertEquals(42, expr.evaluate(Map.of("a", 42)));
    }

    @Test
    public void testEvalWithNestedPath() {
        Jsonata expr = jsonata("$eval('a.b.c')");
        assertEquals(99, expr.evaluate(
                Map.of("a", Map.of("b", Map.of("c", 99)))));
    }

    @Test
    public void testNestedEval() {
        Jsonata expr = jsonata("$eval(\"$eval('a')\")");
        assertEquals(7, expr.evaluate(Map.of("a", 7)));
    }

    @Test
    public void testEvalWithinPathStepUsesCurrentItemContext() {
        Jsonata expr = jsonata("items.$eval('a')");
        Object input = Map.of(
                "items", List.of(
                        Map.of("a", 1),
                        Map.of("a", 2)
                )
        );
        assertEquals(List.of(1, 2), expr.evaluate(input));
    }

    @Test
    public void testEvalWithinFilterUsesCurrentItemContext() {
        Jsonata expr = jsonata("items[$eval('a') = 2].a");
        Object input = Map.of(
                "items", List.of(
                        Map.of("a", 1),
                        Map.of("a", 2)
                )
        );
        assertEquals(2, expr.evaluate(input));
    }

    @Test
    public void testCachedInstanceConcurrentThreads() throws Exception {
        int threads = 10;
        int itersPerThread = 1000;
        Jsonata expr = jsonata("a + b");

        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            futures.add(pool.submit(() -> {
                try {
                    startGate.await();
                } catch (InterruptedException e) { return; }

                for (int i = 0; i < itersPerThread; i++) {
                    Object result = expr.evaluate(Map.of("a", threadId, "b", 1));
                    if (!Integer.valueOf(threadId + 1).equals(result)) {
                        errorCount.incrementAndGet();
                    }
                }
            }));
        }

        startGate.countDown(); // release all threads simultaneously
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(0, errorCount.get(),
                "Concurrent evaluation of cached instance produced wrong results");
    }

    @Test
    public void testHighThroughputWithEval() throws Exception {
        int threads = 16;
        int itersPerThread = 2000;
        Jsonata expr = jsonata("$eval('a') + b");

        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<String> sampleErrors = Collections.synchronizedList(new ArrayList<>());
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            futures.add(pool.submit(() -> {
                try {
                    startGate.await();
                } catch (InterruptedException e) { return; }

                int expected = threadId * 10 + threadId; // threadId * 11
                for (int i = 0; i < itersPerThread; i++) {
                    try {
                        Object result = expr.evaluate(Map.of("a", threadId * 10, "b", threadId));
                        if (!Integer.valueOf(expected).equals(result)) {
                            errorCount.incrementAndGet();
                            if (sampleErrors.size() < 5) {
                                sampleErrors.add("thread-" + threadId + " iter-" + i +
                                        ": expected " + expected + " got " + result);
                            }
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        if (sampleErrors.size() < 5) {
                            sampleErrors.add("thread-" + threadId + " iter-" + i +
                                    ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
                        }
                    }
                }
            }));
        }

        startGate.countDown();
        for (Future<?> f : futures) f.get(60, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(0, errorCount.get(),
                "High-throughput eval errors: " + sampleErrors);
    }

    @Test
    public void testCustomFunctionMultiThread() throws Exception {
        int threads = 10;
        int itersPerThread = 500;
        Jsonata expr = jsonata("$double(a)");
        expr.registerFunction("double", (Integer x) -> x * 2);

        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            final int threadId = t + 1; // 1-based to avoid $double(0)
            futures.add(pool.submit(() -> {
                try {
                    startGate.await();
                } catch (InterruptedException e) { return; }

                int expected = threadId * 2;
                for (int i = 0; i < itersPerThread; i++) {
                    Object result = expr.evaluate(Map.of("a", threadId));
                    if (!Integer.valueOf(expected).equals(result)) {
                        errorCount.incrementAndGet();
                    }
                }
            }));
        }

        startGate.countDown();
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(0, errorCount.get(),
                "Custom function multi-thread evaluation produced wrong results");
    }

    @Test
    public void testCachedInstanceWithBindingsMultiThread() throws Exception {
        int threads = 8;
        int itersPerThread = 1000;
        Jsonata expr = jsonata("$myVar + a");

        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            Jsonata.Frame frame = expr.createFrame();
            frame.bind("myVar", threadId * 100);

            futures.add(pool.submit(() -> {
                try {
                    startGate.await();
                } catch (InterruptedException e) { return; }

                int expected = threadId * 100 + threadId;
                for (int i = 0; i < itersPerThread; i++) {
                    Object result = expr.evaluate(Map.of("a", threadId), frame);
                    if (!Integer.valueOf(expected).equals(result)) {
                        errorCount.incrementAndGet();
                    }
                }
            }));
        }

        startGate.countDown();
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(0, errorCount.get(),
                "Cached instance with per-thread bindings produced wrong results");
    }

    @Test
    public void testNowWithCachedInstance() throws Exception {
        Jsonata expr = jsonata("$now()");
        Object r1 = expr.evaluate(null);
        Thread.sleep(1100); // $now() has second-level precision
        Object r2 = expr.evaluate(null);
        assertNotNull(r1);
        assertNotNull(r2);
        assertNotEquals(r1, r2, "$now() should return different values on different calls");
    }

}
