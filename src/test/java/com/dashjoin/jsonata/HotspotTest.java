package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.dashjoin.jsonata.Jsonata.JFunction;

/**
 * Exercises two hot paths discussed in PR #102:
 * <ul>
 * <li>Signature.validate: called on every function invocation; variadic params ('+') make the
 * matched signature group longer than one char, which hits the per-char split loop</li>
 * <li>Tokenizer number regex: evaluated for every token that is not a string literal</li>
 * </ul>
 * The correctness tests guard the semantics; the loop tests print timings so both code paths can
 * be profiled / compared before and after an optimization.
 */
public class HotspotTest {

  // override with -Dhotspot.iterations=... for a longer run; each loop runs once as warmup first
  static final int ITERATIONS = Integer.getInteger("hotspot.iterations", 20_000);

  // ---------------------------------------------------------------------------------------------
  // Signature.validate
  // ---------------------------------------------------------------------------------------------

  /**
   * $zip has signature &lt;a+&gt;; three array args produce the match "aaa"
   */
  @Test
  public void signatureVariadicArrays() {
    Assertions.assertEquals(
        Arrays.asList(Arrays.asList(1, 3, 5), Arrays.asList(2, 4, 6)),
        jsonata("$zip([1,2],[3,4],[5,6])").evaluate(null));
  }

  /**
   * a non-array arg for an 'a' param must be wrapped into an array (single != 'a' branch)
   */
  @Test
  public void signatureVariadicWrapsNonArrays() {
    Assertions.assertEquals(
        Arrays.asList(Arrays.asList(1, 3, 5)),
        jsonata("$zip(1,[3],5)").evaluate(null));
  }

  /**
   * non-array variadic param (else branch): match "nnnn"
   */
  @Test
  public void signatureVariadicNumbers() {
    Jsonata expr = jsonata("$add(1,2,3,4)");
    expr.registerFunction("add", new JFunction((input, args) -> {
      double sum = 0;
      for (Object o : args)
        sum += ((Number) o).doubleValue();
      return sum;
    }, "<n+:n>"));
    Assertions.assertEquals(10, ((Number) expr.evaluate(null)).intValue());
  }

  /**
   * typed array subtype check: a&lt;n&gt;+
   */
  @Test
  public void signatureVariadicTypedArrays() {
    Jsonata expr = jsonata("$count2([1,2],[3])");
    expr.registerFunction("count2", new JFunction((input, args) -> args.size(), "<a<n>+:n>"));
    Assertions.assertEquals(2, expr.evaluate(null));

    Jsonata bad = jsonata("$count2([1,2],['x'])");
    bad.registerFunction("count2", new JFunction((input, args) -> args.size(), "<a<n>+:n>"));
    Assertions.assertThrows(JException.class, () -> bad.evaluate(null));
  }

  @Test
  public void signatureLoop() {
    Jsonata expr = jsonata("$zip([1,2],[3,4],[5,6],[7,8])");
    for (int i = 0; i < ITERATIONS; i++)
      expr.evaluate(null);
    long start = System.nanoTime();
    for (int i = 0; i < ITERATIONS; i++)
      expr.evaluate(null);
    System.out.println("Signature.validate x" + ITERATIONS + ": "
        + (System.nanoTime() - start) / 1_000_000 + " ms");
  }

  // ---------------------------------------------------------------------------------------------
  // Tokenizer number regex
  // ---------------------------------------------------------------------------------------------

  @Test
  public void tokenizerNumbers() {
    Assertions.assertEquals(0, jsonata("0").evaluate(null));
    Assertions.assertEquals(-42, jsonata("-42").evaluate(null));
    Assertions.assertEquals(3.25, jsonata("3.25").evaluate(null));
    Assertions.assertEquals(1500, jsonata("1.5e3").evaluate(null));
    Assertions.assertEquals(0.015, jsonata("1.5E-2").evaluate(null));
    Assertions.assertEquals(List.of(1, 2.5, 300), jsonata("[1, 2.5, 3e2]").evaluate(null));
    Assertions.assertEquals(9, jsonata("1+2*4").evaluate(null));
  }

  /**
   * builds a long expression so the tokenizer runs the number regex many times per parse
   */
  @Test
  public void tokenizerLoop() {
    StringBuilder sb = new StringBuilder("$sum([");
    for (int i = 0; i < 200; i++) {
      if (i > 0)
        sb.append(',');
      sb.append(i).append(".5e0");
    }
    sb.append("])");
    String src = sb.toString();

    Assertions.assertEquals(20000, jsonata(src).evaluate(null));

    for (int i = 0; i < ITERATIONS / 10; i++)
      jsonata(src);
    long start = System.nanoTime();
    for (int i = 0; i < ITERATIONS / 10; i++)
      jsonata(src);
    System.out.println("Tokenizer numbers x" + (ITERATIONS / 10) + " parses: "
        + (System.nanoTime() - start) / 1_000_000 + " ms");
  }
}
